package com.ctrip.blindbox.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.blindbox.converter.OrderConverter;
import com.ctrip.blindbox.converter.ResultConverter;
import com.ctrip.blindbox.dto.CreateOrderRequest;
import com.ctrip.blindbox.dto.OrderResponse;
import com.ctrip.blindbox.dto.SchemeResponse;
import com.ctrip.blindbox.entity.BlindBoxOrder;
import com.ctrip.blindbox.entity.BlindBoxPreference;
import com.ctrip.blindbox.entity.BlindBoxResult;
import com.ctrip.blindbox.entity.BlindBoxTemplate;
import com.ctrip.blindbox.entity.enums.BlindBoxType;
import com.ctrip.blindbox.entity.enums.OrderStatus;
import com.ctrip.blindbox.entity.enums.TemplateStatus;
import com.ctrip.blindbox.mapper.BlindBoxOrderMapper;
import com.ctrip.blindbox.mapper.BlindBoxPreferenceMapper;
import com.ctrip.blindbox.mapper.BlindBoxResultMapper;
import com.ctrip.blindbox.mapper.BlindBoxTemplateMapper;
import com.ctrip.blindbox.service.BlindBoxOrderService;
import com.ctrip.blindbox.service.BlindBoxPreferenceService;
import com.ctrip.blindbox.service.BlindBoxSchemeService;
import com.ctrip.blindbox.service.FlashSaleService;
import com.ctrip.common.exception.BusinessException;
import com.ctrip.common.exception.ForbiddenException;
import com.ctrip.common.exception.ResourceNotFoundException;
import com.ctrip.messaging.publisher.EventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 盲盒订单服务实现。
 *
 * <p>核心业务逻辑：订单创建、支付、取消、开盒、超时扫描。
 * 限定盲盒使用 Redisson 分布式锁 + MySQL 乐观锁防止超卖。
 */
@Service
public class BlindBoxOrderServiceImpl implements BlindBoxOrderService {

    private static final Logger log = LoggerFactory.getLogger(BlindBoxOrderServiceImpl.class);
    private static final DateTimeFormatter ORDER_NO_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** Redis 订单号计数器 key 前缀，完整 key 为 bb_order_seq:yyyyMMdd。 */
    private static final String ORDER_NO_KEY_PREFIX = "bb_order_seq:";

    private final BlindBoxOrderMapper orderMapper;
    private final BlindBoxTemplateMapper templateMapper;
    private final BlindBoxResultMapper resultMapper;
    private final BlindBoxPreferenceMapper preferenceMapper;
    private final BlindBoxPreferenceService preferenceService;
    private final BlindBoxSchemeService schemeService;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final EventPublisher eventPublisher;
    private final FlashSaleService flashSaleService;

    public BlindBoxOrderServiceImpl(BlindBoxOrderMapper orderMapper,
                                    BlindBoxTemplateMapper templateMapper,
                                    BlindBoxResultMapper resultMapper,
                                    BlindBoxPreferenceMapper preferenceMapper,
                                    BlindBoxPreferenceService preferenceService,
                                    BlindBoxSchemeService schemeService,
                                    RedissonClient redissonClient,
                                    StringRedisTemplate stringRedisTemplate,
                                    ObjectMapper objectMapper,
                                    EventPublisher eventPublisher,
                                    FlashSaleService flashSaleService) {
        this.orderMapper = orderMapper;
        this.templateMapper = templateMapper;
        this.resultMapper = resultMapper;
        this.preferenceMapper = preferenceMapper;
        this.preferenceService = preferenceService;
        this.schemeService = schemeService;
        this.redissonClient = redissonClient;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.flashSaleService = flashSaleService;
    }

    // ========== 订单创建 ==========

    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        // 1. 验证模板
        BlindBoxTemplate template = requireTemplate(request.templateId());
        if (template.getStatus() != TemplateStatus.ACTIVE) {
            throw new BusinessException("盲盒已下架，无法购买");
        }

        // 2. 限定盲盒：检查库存 + 分布式锁扣减
        if (template.getType() == BlindBoxType.LIMITED) {
            if (template.getStock() == null || template.getStock() <= 0) {
                throw new BusinessException("库存不足");
            }
            decrementStockWithLock(template.getId());
        }

        // 3. 创建订单
        String orderNo = generateOrderNo();
        LocalDateTime now = LocalDateTime.now();
        BlindBoxOrder order = BlindBoxOrder.builder()
                .userId(userId)
                .templateId(template.getId())
                .orderNo(orderNo)
                .status(OrderStatus.PENDING)
                .payAmount(template.getPrice())
                .expireAt(now.plusMinutes(15))
                .build();
        orderMapper.insert(order);

        // 4. 保存预选参数
        preferenceService.savePreference(
                order.getId(),
                request.departureCity(),
                request.budgetLevel(),
                request.theme()
        );

        log.info("创建盲盒订单: orderNo={}, userId={}, templateId={}", orderNo, userId, template.getId());

        // 发布订单创建事件
        eventPublisher.publishOrderStatusEvent(
                "ORDER_CREATED", order.getId(), orderNo, userId,
                template.getId(), template.getPrice(), null, null);

        return OrderConverter.toResponse(order, template);
    }

    // ========== 订单查询 ==========

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetail(Long userId, String orderNo) {
        BlindBoxOrder order = requireOrder(orderNo, userId);
        BlindBoxTemplate template = templateMapper.selectById(order.getTemplateId());
        return OrderConverter.toResponse(order, template);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> listMyOrders(Long userId, int page, int limit, String status) {
        Page<BlindBoxOrder> pageObj = new Page<>(page, limit);

        LambdaQueryWrapper<BlindBoxOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlindBoxOrder::getUserId, userId);

        if (status != null && !status.isBlank()) {
            wrapper.eq(BlindBoxOrder::getStatus, OrderStatus.valueOf(status));
        }
        wrapper.orderByDesc(BlindBoxOrder::getCreatedAt);

        Page<BlindBoxOrder> result = orderMapper.selectPage(pageObj, wrapper);

        Page<OrderResponse> responsePage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        responsePage.setRecords(
                result.getRecords().stream()
                        .map(order -> {
                            BlindBoxTemplate template = templateMapper.selectById(order.getTemplateId());
                            return OrderConverter.toResponse(order, template);
                        })
                        .toList()
        );
        return responsePage;
    }

    // ========== 取消订单 ==========

    @Override
    @Transactional
    public void cancelOrder(Long userId, String orderNo) {
        BlindBoxOrder order = requireOrder(orderNo, userId);

        // 仅待支付可取消
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("仅待支付订单可取消");
        }

        // 更新状态
        orderMapper.update(null, new LambdaUpdateWrapper<BlindBoxOrder>()
                .eq(BlindBoxOrder::getId, order.getId())
                .set(BlindBoxOrder::getStatus, OrderStatus.CANCELLED));

        // 限定盲盒恢复库存
        BlindBoxTemplate template = templateMapper.selectById(order.getTemplateId());
        if (template != null && template.getType() == BlindBoxType.LIMITED) {
            templateMapper.incrementStock(template.getId());
            log.info("取消订单恢复库存: orderNo={}, templateId={}", orderNo, template.getId());
        }

        log.info("取消订单: orderNo={}", orderNo);

        // 发布订单取消事件
        eventPublisher.publishOrderStatusEvent(
                "ORDER_CANCELLED", order.getId(), orderNo, order.getUserId(),
                order.getTemplateId(), order.getPayAmount(), order.getPayMethod(), null);
    }

    // ========== 开盒 ==========

    @Override
    @Transactional
    public OrderResponse openBox(Long userId, String orderNo) {
        BlindBoxOrder order = requireOrder(orderNo, userId);

        // 仅已支付可开盒
        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException("仅已支付订单可开盒");
        }

        // 获取分布式锁防止重复开盒
        String lockKey = "lock:blindbox:open:" + order.getId();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                throw new BusinessException("系统繁忙，请稍后重试");
            }

            // 幂等检查：若已开盒，直接返回
            BlindBoxOrder refreshed = orderMapper.selectById(order.getId());
            if (refreshed.getStatus() == OrderStatus.OPENED) {
                log.info("开盒幂等返回: orderNo={}", orderNo);
                BlindBoxTemplate template = templateMapper.selectById(refreshed.getTemplateId());
                return OrderConverter.toResponse(refreshed, template);
            }

            // 更新订单状态为 OPENED
            LocalDateTime openedAt = LocalDateTime.now();
            orderMapper.update(null, new LambdaUpdateWrapper<BlindBoxOrder>()
                    .eq(BlindBoxOrder::getId, order.getId())
                    .set(BlindBoxOrder::getStatus, OrderStatus.OPENED));

            // 生成盲盒方案
            BlindBoxPreference preference = preferenceService.getByOrderId(order.getId());
            SchemeResponse scheme = schemeService.generateScheme(preference);

            // 将方案序列化为 JSON
            String resultText;
            try {
                resultText = objectMapper.writeValueAsString(scheme);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("序列化方案失败", e);
            }

            // 保存结果
            BlindBoxResult result = BlindBoxResult.builder()
                    .orderId(order.getId())
                    .destination(scheme.destination())
                    .destinationId(scheme.destinationId())
                    .theme(scheme.theme())
                    .resultText(resultText)
                    .openedAt(openedAt)
                    .build();
            resultMapper.insert(result);

            log.info("开盒成功: orderNo={}, destination={}", orderNo, scheme.destination());

            // 发布盲盒结果生成事件
            eventPublisher.publishBlindBoxOpenEvent(
                    "BLINDBOX_RESULT_GENERATED", order.getId(), userId,
                    result.getId(), Map.of(
                            "destination", scheme.destination(),
                            "destinationId", scheme.destinationId(),
                            "theme", scheme.theme(),
                            "days", scheme.days()
                    ));

            // 发布订单开盒状态事件
            eventPublisher.publishOrderStatusEvent(
                    "ORDER_OPENED", order.getId(), orderNo, userId,
                    order.getTemplateId(), order.getPayAmount(), order.getPayMethod(), null);

            // 返回更新后的订单
            BlindBoxTemplate template = templateMapper.selectById(order.getTemplateId());
            return OrderConverter.toResponse(orderMapper.selectById(order.getId()), template);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("开盒操作被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // ========== 模拟支付回调 ==========

    @Override
    @Transactional
    public void payCallback(String orderNo, String payMethod) {
        LambdaQueryWrapper<BlindBoxOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlindBoxOrder::getOrderNo, orderNo);
        BlindBoxOrder order = orderMapper.selectOne(wrapper);

        if (order == null) {
            throw new ResourceNotFoundException("订单不存在：orderNo=" + orderNo);
        }

        // 幂等检查：已支付直接返回
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.OPENED) {
            log.info("支付回调幂等: orderNo={}, status={}", orderNo, order.getStatus());
            return;
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("订单状态不支持支付：status=" + order.getStatus());
        }

        orderMapper.update(null, new LambdaUpdateWrapper<BlindBoxOrder>()
                .eq(BlindBoxOrder::getId, order.getId())
                .set(BlindBoxOrder::getStatus, OrderStatus.PAID)
                .set(BlindBoxOrder::getPayTime, LocalDateTime.now())
                .set(BlindBoxOrder::getPayMethod, payMethod));

        log.info("支付成功: orderNo={}, payMethod={}", orderNo, payMethod);

        // 发布订单支付事件
        eventPublisher.publishOrderStatusEvent(
                "ORDER_PAID", order.getId(), orderNo, order.getUserId(),
                order.getTemplateId(), order.getPayAmount(), payMethod,
                Map.of("payTime", order.getPayTime() != null ? order.getPayTime().toString() : ""));
    }

    // ========== 定时任务：过期订单扫描 ==========

    @Override
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    @Transactional
    public void cancelExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();

        // 查询过期且待支付的订单
        LambdaQueryWrapper<BlindBoxOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlindBoxOrder::getStatus, OrderStatus.PENDING)
                .lt(BlindBoxOrder::getExpireAt, now);

        Page<BlindBoxOrder> pageObj = new Page<>(1, 100);
        Page<BlindBoxOrder> expiredOrders = orderMapper.selectPage(pageObj, wrapper);

        if (expiredOrders.getRecords().isEmpty()) {
            return;
        }

        for (BlindBoxOrder order : expiredOrders.getRecords()) {
            // 更新状态为 CANCELLED
            orderMapper.update(null, new LambdaUpdateWrapper<BlindBoxOrder>()
                    .eq(BlindBoxOrder::getId, order.getId())
                    .set(BlindBoxOrder::getStatus, OrderStatus.CANCELLED));

            // 限定盲盒恢复库存
            BlindBoxTemplate template = templateMapper.selectById(order.getTemplateId());
            if (template != null && template.getType() == BlindBoxType.LIMITED) {
                templateMapper.incrementStock(template.getId());
            }

            log.info("自动取消过期订单: orderNo={}, templateId={}", order.getOrderNo(), order.getTemplateId());
        }
    }

    // ========== 我的盲盒列表 ==========

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> listMyBlindBoxes(Long userId, int page, int limit, Boolean opened) {
        Page<BlindBoxOrder> pageObj = new Page<>(page, limit);

        LambdaQueryWrapper<BlindBoxOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlindBoxOrder::getUserId, userId);

        // 按开盒状态筛选
        if (opened != null) {
            if (opened) {
                wrapper.eq(BlindBoxOrder::getStatus, OrderStatus.OPENED);
            } else {
                wrapper.ne(BlindBoxOrder::getStatus, OrderStatus.OPENED);
            }
        }
        wrapper.orderByDesc(BlindBoxOrder::getCreatedAt);

        Page<BlindBoxOrder> result = orderMapper.selectPage(pageObj, wrapper);

        Page<OrderResponse> responsePage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        responsePage.setRecords(
                result.getRecords().stream()
                        .map(order -> {
                            BlindBoxTemplate template = templateMapper.selectById(order.getTemplateId());
                            return OrderConverter.toResponse(order, template);
                        })
                        .toList()
        );
        return responsePage;
    }

    // ========== 私有辅助 ==========

    /**
     * 通过分布式锁 + MySQL 乐观锁扣减库存。
     */
    private void decrementStockWithLock(Long templateId) {
        String lockKey = "lock:blindbox:stock:" + templateId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                throw new BusinessException("系统繁忙，请稍后重试");
            }

            int rows = templateMapper.decrementStock(templateId);
            if (rows == 0) {
                throw new BusinessException("库存不足");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("库存扣减被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 生成订单号：BByyyyMMddXXXX。
     *
     * <p>使用 Redis INCR 保证跨重启的序号唯一性，生成后校验 DB 确保不重复。
     * Key 格式：bb_order_seq:yyyyMMdd，首次使用时从数据库同步最大序号。
     */
    private String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(ORDER_NO_DATE_FMT);
        String key = ORDER_NO_KEY_PREFIX + dateStr;

        // 首次使用时：从数据库同步当前最大序号到 Redis
        Long maxSeq = getMaxSeqFromDb(dateStr);
        stringRedisTemplate.opsForValue()
                .setIfAbsent(key, String.valueOf(maxSeq), Duration.ofDays(2));

        // 循环递增直到获取一个数据库中不存在的订单号
        while (true) {
            Long seq = stringRedisTemplate.opsForValue().increment(key);
            long currentSeq = seq != null ? seq : maxSeq + 1;
            String orderNo = String.format("BB%s%04d", dateStr, currentSeq);

            // 校验：确保该订单号在数据库中不存在（兜底保障，处理历史数据和极端并发）
            if (!orderNoExists(orderNo)) {
                return orderNo;
            }
            log.warn("订单号已存在，跳过: orderNo={}", orderNo);
        }
    }

    /**
     * 检查订单号是否已存在于数据库。
     */
    private boolean orderNoExists(String orderNo) {
        LambdaQueryWrapper<BlindBoxOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlindBoxOrder::getOrderNo, orderNo);
        return orderMapper.selectCount(wrapper) > 0;
    }

    /**
     * 从数据库查询当日已有的最大订单号序号。
     *
     * @param dateStr 日期字符串 yyyyMMdd
     * @return 当日最大序号，若无记录则返回 0
     */
    private Long getMaxSeqFromDb(String dateStr) {
        String prefix = "BB" + dateStr;
        LambdaQueryWrapper<BlindBoxOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(BlindBoxOrder::getOrderNo, prefix)
                .orderByDesc(BlindBoxOrder::getOrderNo)
                .last("LIMIT 1");
        BlindBoxOrder lastOrder = orderMapper.selectOne(wrapper);
        if (lastOrder == null) {
            return 0L;
        }
        // 从订单号末尾提取 4 位序号
        String lastOrderNo = lastOrder.getOrderNo();
        try {
            return Long.parseLong(lastOrderNo.substring(lastOrderNo.length() - 4));
        } catch (NumberFormatException e) {
            log.warn("解析订单号序号失败: orderNo={}", lastOrderNo);
            return 0L;
        }
    }

    private BlindBoxTemplate requireTemplate(Long id) {
        BlindBoxTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new ResourceNotFoundException("盲盒模板不存在：id=" + id);
        }
        return template;
    }

    private BlindBoxOrder requireOrder(String orderNo, Long userId) {
        LambdaQueryWrapper<BlindBoxOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlindBoxOrder::getOrderNo, orderNo);
        BlindBoxOrder order = orderMapper.selectOne(wrapper);

        if (order == null) {
            throw new ResourceNotFoundException("订单不存在：orderNo=" + orderNo);
        }
        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException("无权查看他人订单");
        }
        return order;
    }

    // ========================================================================
    // 定时任务：Redis ↔ MySQL 库存对账
    // ========================================================================

    /**
     * 每小时对比 Redis 秒杀库存与 MySQL 库存，发现差异时以 MySQL 为准修正。
     *
     * <p>此任务在秒杀活动中起最终一致性兜底作用：
     * <ul>
     *   <li>Redis 库存因消费者失败回滚不完整等原因与 MySQL 偏差</li>
     *   <li>定时对账自动发现并修正，避免超卖或库存残留</li>
     * </ul>
     *
     * <p>仅检查活跃状态的限定盲盒模板。
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional(readOnly = true)
    public void reconcileStock() {
        // 查询所有活跃的限定盲盒模板
        LambdaQueryWrapper<BlindBoxTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlindBoxTemplate::getType, BlindBoxType.LIMITED);
        wrapper.eq(BlindBoxTemplate::getStatus, TemplateStatus.ACTIVE);
        var templates = templateMapper.selectList(wrapper);

        int reconciled = 0;
        for (BlindBoxTemplate template : templates) {
            // 获取 Redis 中的剩余库存（未预热的跳过）
            int redisStock = flashSaleService.getRemainingStock(template.getId());
            if (redisStock < 0) {
                continue; // 未预热，跳过
            }

            // 获取 MySQL 中的库存
            Integer mysqlStock = template.getStock();
            if (mysqlStock == null) {
                continue;
            }

            // 对比并修正
            if (redisStock != mysqlStock) {
                log.warn("库存不一致: templateId={}, redis={}, mysql={}",
                        template.getId(), redisStock, mysqlStock);
                // 以 MySQL 为准——数据库是最终真相源
                flashSaleService.prewarmStock(
                        template.getId(), mysqlStock, Duration.ofHours(48));
                reconciled++;
            }
        }

        if (reconciled > 0) {
            log.info("库存对账完成，修正 {} 个模板", reconciled);
        }
    }
}
