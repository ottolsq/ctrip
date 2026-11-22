package com.ctrip.blindbox.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ctrip.blindbox.dto.FlashSaleMessage;
import com.ctrip.blindbox.dto.FlashSaleRequest;
import com.ctrip.blindbox.entity.BlindBoxOrder;
import com.ctrip.blindbox.entity.BlindBoxTemplate;
import com.ctrip.blindbox.entity.enums.BlindBoxType;
import com.ctrip.blindbox.entity.enums.BudgetLevel;
import com.ctrip.blindbox.entity.enums.TemplateStatus;
import com.ctrip.blindbox.mapper.BlindBoxOrderMapper;
import com.ctrip.blindbox.mapper.BlindBoxTemplateMapper;
import com.ctrip.blindbox.rule.ActivityWindow;
import com.ctrip.blindbox.rule.BlindBoxRuleConfigParser;
import com.ctrip.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 秒杀服务——Redis Lua 原子预扣库存 + RabbitMQ 异步创建订单。
 *
 * <h3>与日常购买的区别</h3>
 * <p>日常购买走 {@code BlindBoxOrderServiceImpl.createOrder()}：
 * 分布式锁 + 乐观锁，同步创建订单，适合 QPS &lt; 100。
 *
 * <p>秒杀走本服务：Lua 脚本在 Redis 内原子执行（~2ms），
 * 扣减成功后通过 RabbitMQ 异步创建订单，立即返回排队状态，
 * 适用于限时抢购等高并发场景（QPS &gt; 1,000）。
 *
 * <h3>完整秒杀链路</h3>
 * <pre>
 * 用户请求 → FlashSaleService.execute()
 *   → Redis Lua 原子预扣库存
 *     ├── 成功 → 发 MQ 消息 → 返回 QUEUED
 *     ├── 库存不足 → 返回 SOLD_OUT
 *     └── 重复购买 → 返回 DUPLICATED
 *            ↓ (MQ 异步)
 *   FlashSaleOrderConsumer → MySQL 扣库存 → 创建订单
 * </pre>
 */
@Service
public class FlashSaleService {

    private static final Logger log = LoggerFactory.getLogger(FlashSaleService.class);

    /** 秒杀库存 Key 前缀：seckill:stock:{templateId} */
    private static final String STOCK_KEY_PREFIX = "seckill:stock:";

    /** 秒杀用户集合 Key 前缀：seckill:users:{templateId} */
    private static final String USERS_KEY_PREFIX = "seckill:users:";

    /** 在途预扣计数 Key 前缀：seckill:pending:{templateId} */
    private static final String PENDING_KEY_PREFIX = "seckill:pending:";

    /** 订单号计数器 Key 前缀：bb_order_seq:yyyyMMdd */
    private static final String ORDER_NO_KEY_PREFIX = "bb_order_seq:";

    /** 订单号日期格式 */
    private static final DateTimeFormatter ORDER_NO_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 秒杀 Exchange 名称（与 RabbitMQConfig 中声明的一致） */
    static final String FLASH_SALE_EXCHANGE = "flash.sale.exchange";

    /** 秒杀订单队列 Routing Key */
    static final String FLASH_SALE_ORDER_RK = "flash.sale.order";

    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final BlindBoxOrderMapper orderMapper;
    private final BlindBoxTemplateMapper templateMapper;
    private final DefaultRedisScript<Long> seckillScript;

    public FlashSaleService(StringRedisTemplate stringRedisTemplate,
                            RabbitTemplate rabbitTemplate,
                            BlindBoxOrderMapper orderMapper,
                            BlindBoxTemplateMapper templateMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.orderMapper = orderMapper;
        this.templateMapper = templateMapper;

        // 从 classpath 加载 Lua 脚本，编译为 DefaultRedisScript<Long>
        this.seckillScript = new DefaultRedisScript<>();
        this.seckillScript.setLocation(new ClassPathResource("lua/seckill.lua"));
        this.seckillScript.setResultType(Long.class);
    }

    // ========================================================================
    // 秒杀入口
    // ========================================================================

    /**
     * 执行秒杀——Lua 原子预扣库存 + 异步发 MQ。
     *
     * <p>核心扣减在 Redis Lua 中完成；请求前会校验模板与活动窗口，
     * 并生成订单号后发送 MQ。
     *
     * @param userId     用户 ID
     * @param templateId 盲盒模板 ID
     * @return 秒杀结果（成功排队 / 库存不足 / 重复购买）
     */
    public FlashSaleResult execute(Long userId, FlashSaleRequest request) {
        Long templateId = request.templateId();
        BlindBoxTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("盲盒模板不存在");
        }
        if (template.getType() != BlindBoxType.LIMITED) {
            throw new BusinessException("仅限定盲盒支持秒杀");
        }
        if (template.getStatus() != TemplateStatus.ACTIVE) {
            throw new BusinessException("盲盒已下架，无法抢购");
        }
        try {
            BudgetLevel.valueOf(request.budgetLevel());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("预算等级不合法");
        }
        LocalDateTime now = LocalDateTime.now();
        BlindBoxRuleConfigParser.ensureActive(template.getRuleConfig(), now);
        ActivityWindow window = BlindBoxRuleConfigParser.parse(template.getRuleConfig());

        // 未预热时从 MySQL 懒加载库存到 Redis，TTL 覆盖活动结束
        if (getRemainingStock(templateId) == -1) {
            Duration ttl = Duration.between(now, window.end()).plusHours(1);
            prewarmStock(templateId, template.getStock(), ttl);
        }

        // 1. Lua 脚本原子预扣库存
        long userSetTtl = Duration.between(now, window.end()).plusHours(1).toSeconds();
        Long result = stringRedisTemplate.execute(
                seckillScript,
                List.of(templateId.toString()),
                userId.toString(),
                String.valueOf(Math.max(userSetTtl, 60))
        );

        if (result == null) {
            log.error("秒杀 Lua 脚本返回 null: userId={}, templateId={}", userId, templateId);
            return FlashSaleResult.error("系统繁忙，请稍后重试");
        }

        // 2. 根据返回值判断结果
        return switch (result.intValue()) {
            case 1 -> {
                try {
                    yield handleSuccess(userId, request);
                } catch (RuntimeException e) {
                    rollbackReservation(userId, templateId);
                    log.error("秒杀后续处理失败，已回滚库存: userId={}, templateId={}",
                            userId, templateId, e);
                    throw new BusinessException("抢购失败，请稍后重试");
                }
            }
            case 0 -> {
                log.debug("秒杀库存不足: userId={}, templateId={}", userId, templateId);
                yield FlashSaleResult.soldOut();
            }
            case -1 -> {
                log.debug("秒杀重复购买: userId={}, templateId={}", userId, templateId);
                yield FlashSaleResult.duplicated();
            }
            default -> {
                log.error("秒杀 Lua 脚本返回未知值: {}", result);
                yield FlashSaleResult.error("系统异常");
            }
        };
    }

    /**
     * 秒杀成功：生成订单号 → 发送 MQ 消息 → 返回排队结果。
     */
    private void rollbackReservation(Long userId, Long templateId) {
        stringRedisTemplate.opsForValue().increment(STOCK_KEY_PREFIX + templateId);
        stringRedisTemplate.opsForSet().remove(USERS_KEY_PREFIX + templateId, userId.toString());
        stringRedisTemplate.opsForValue().decrement(PENDING_KEY_PREFIX + templateId);
    }

    private FlashSaleResult handleSuccess(Long userId, FlashSaleRequest request) {
        String orderNo = generateOrderNo();

        FlashSaleMessage message = FlashSaleMessage.of(
                userId, request.templateId(), orderNo,
                request.departureCity(), request.budgetLevel(), request.theme());
        rabbitTemplate.convertAndSend(FLASH_SALE_EXCHANGE, FLASH_SALE_ORDER_RK, message);

        log.info("秒杀成功: userId={}, templateId={}, orderNo={}",
                userId, request.templateId(), orderNo);
        return FlashSaleResult.queued(orderNo);
    }

    // ========================================================================
    // 库存管理（管理员后台调用）
    // ========================================================================

    /**
     * 活动预热：将库存从 MySQL 加载到 Redis。
     *
     * <p>管理员在活动开始前调用此方法，将库存同步到 Redis。
     * 参数中的 stock 数量应与 MySQL 中 blind_box_template.stock 一致。
     *
     * @param templateId       盲盒模板 ID
     * @param stock            库存数量
     * @param activityDuration 活动持续时间（Redis key 的 TTL）
     */
    public boolean prewarmStock(Long templateId, int stock, Duration activityDuration) {
        String stockKey = STOCK_KEY_PREFIX + templateId;
        Boolean initialized = stringRedisTemplate.opsForValue().setIfAbsent(
                stockKey, String.valueOf(stock), activityDuration);
        if (Boolean.TRUE.equals(initialized)) {
            log.info("秒杀库存预热完成: templateId={}, stock={}, ttl={}s",
                    templateId, stock, activityDuration.toSeconds());
        }
        return Boolean.TRUE.equals(initialized);
    }

    /**
     * 以 MySQL 库存与在途预扣为准重置 Redis 库存（对账/管理员改库存）。
     */
    public void resetStock(Long templateId, int dbStock) {
        int pending = getPendingCount(templateId);
        int target = Math.max(dbStock - pending, 0);
        stringRedisTemplate.opsForValue().set(
                STOCK_KEY_PREFIX + templateId, String.valueOf(target), Duration.ofHours(48));
        log.info("秒杀库存重置: templateId={}, db={}, pending={}, redis={}",
                templateId, dbStock, pending, target);
    }

    /**
     * 管理员修改 MySQL 库存后，仅在活动已预热时同步 Redis。
     */
    public void onAdminStockChanged(Long templateId, int dbStock) {
        if (getRemainingStock(templateId) != -1) {
            resetStock(templateId, dbStock);
        }
    }

    /**
     * 正常下单扣减 MySQL 库存后，同步扣减已预热的 Redis 库存。
     */
    public void onNormalPurchase(Long templateId) {
        if (getRemainingStock(templateId) == -1) {
            return;
        }
        Long remaining = stringRedisTemplate.opsForValue()
                .decrement(STOCK_KEY_PREFIX + templateId);
        if (remaining != null && remaining < 0) {
            stringRedisTemplate.opsForValue().increment(STOCK_KEY_PREFIX + templateId);
        }
    }

    /**
     * 取消/退款恢复 MySQL 库存后，同步恢复 Redis（仅当库存 key 存在）。
     */
    public void onStockRestored(Long templateId) {
        if (getRemainingStock(templateId) != -1) {
            stringRedisTemplate.opsForValue().increment(STOCK_KEY_PREFIX + templateId);
        }
    }

    /**
     * 结束活动：清理 Redis 中的秒杀数据。
     *
     * @param templateId 盲盒模板 ID
     */
    public void endActivity(Long templateId) {
        String stockKey = STOCK_KEY_PREFIX + templateId;
        String usersKey = USERS_KEY_PREFIX + templateId;
        stringRedisTemplate.delete(List.of(stockKey, usersKey));
        log.info("秒杀活动已结束，Redis 数据已清理: templateId={}", templateId);
    }

    /**
     * 获取 Redis 中当前剩余库存。
     *
     * @param templateId 盲盒模板 ID
     * @return 剩余库存；未预热则返回 -1
     */
    public int getRemainingStock(Long templateId) {
        String stockKey = STOCK_KEY_PREFIX + templateId;
        String stock = stringRedisTemplate.opsForValue().get(stockKey);
        return stock != null ? Integer.parseInt(stock) : -1;
    }

    public int getPendingCount(Long templateId) {
        String pending = stringRedisTemplate.opsForValue()
                .get(PENDING_KEY_PREFIX + templateId);
        return pending != null ? Integer.parseInt(pending) : 0;
    }

    // ========================================================================
    // 内部工具：订单号生成（与 BlindBoxOrderServiceImpl 逻辑一致）
    // ========================================================================

    /**
     * 生成订单号：BB + yyyyMMdd + 4 位 Redis 自增序号。
     *
     * <p>与 {@code BlindBoxOrderServiceImpl.generateOrderNo()} 使用相同的
     * Redis key 前缀 {@code bb_order_seq:}，保证全局唯一。
     */
    private String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(ORDER_NO_DATE_FMT);
        String key = ORDER_NO_KEY_PREFIX + dateStr;

        String maxSeq = getMaxSeqFromDb(dateStr);
        stringRedisTemplate.opsForValue()
                .setIfAbsent(key, maxSeq, Duration.ofDays(2));

        while (true) {
            Long seq = stringRedisTemplate.opsForValue().increment(key);
            long currentSeq = seq != null ? seq : Long.parseLong(maxSeq) + 1;
            String orderNo = String.format("BB%s%04d", dateStr, currentSeq);

            if (!orderNoExists(orderNo)) {
                return orderNo;
            }
            log.warn("订单号已存在，跳过: orderNo={}", orderNo);
        }
    }

    private String getMaxSeqFromDb(String dateStr) {
        String prefix = "BB" + dateStr;
        LambdaQueryWrapper<BlindBoxOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(BlindBoxOrder::getOrderNo, prefix)
                .orderByDesc(BlindBoxOrder::getOrderNo)
                .last("LIMIT 1");
        BlindBoxOrder lastOrder = orderMapper.selectOne(wrapper);
        if (lastOrder == null) {
            return "0";
        }
        String lastOrderNo = lastOrder.getOrderNo();
        try {
            return String.valueOf(Long.parseLong(
                    lastOrderNo.substring(lastOrderNo.length() - 4)));
        } catch (NumberFormatException e) {
            log.warn("解析订单号序号失败: orderNo={}", lastOrderNo);
            return "0";
        }
    }

    private boolean orderNoExists(String orderNo) {
        LambdaQueryWrapper<BlindBoxOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlindBoxOrder::getOrderNo, orderNo);
        return orderMapper.selectCount(wrapper) > 0;
    }

    // ========================================================================
    // 秒杀结果 Record
    // ========================================================================

    /**
     * 秒杀执行结果——由 Controller 据此决定返回给前端的 HTTP 响应。
     *
     * <p>不影响现有接口返回格式，Controller 可将此 Record 的字段
     * 映射到统一的 {@code ApiResponse} 信封中。
     */
    public record FlashSaleResult(boolean success, String status, String orderNo, String message) {

        /** 成功排队，等待 MQ 异步创建订单 */
        public static FlashSaleResult queued(String orderNo) {
            return new FlashSaleResult(true, "QUEUED", orderNo, "排队中，请等待结果");
        }

        /** 库存已售罄 */
        public static FlashSaleResult soldOut() {
            return new FlashSaleResult(false, "SOLD_OUT", null, "已售罄");
        }

        /** 该用户已购买过此盲盒 */
        public static FlashSaleResult duplicated() {
            return new FlashSaleResult(false, "DUPLICATED", null, "已购买，不可重复");
        }

        /** 系统异常 */
        public static FlashSaleResult error(String message) {
            return new FlashSaleResult(false, "ERROR", null, message);
        }
    }
}
