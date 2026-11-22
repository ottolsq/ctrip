package com.ctrip.blindbox.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ctrip.blindbox.dto.SchemeResponse;
import com.ctrip.blindbox.entity.BlindBoxOrder;
import com.ctrip.blindbox.entity.BlindBoxResult;
import com.ctrip.blindbox.entity.enums.OrderStatus;
import com.ctrip.blindbox.mapper.BlindBoxOrderMapper;
import com.ctrip.blindbox.mapper.BlindBoxResultMapper;
import com.ctrip.blindbox.service.BlindBoxItineraryImportService;
import com.ctrip.common.exception.BusinessException;
import com.ctrip.common.exception.ForbiddenException;
import com.ctrip.common.exception.ResourceNotFoundException;
import com.ctrip.itinerary.dto.request.CreateItineraryItemRequest;
import com.ctrip.itinerary.dto.request.CreateItineraryRequest;
import com.ctrip.itinerary.dto.response.ItineraryDayResponse;
import com.ctrip.itinerary.dto.response.ItineraryResponse;
import com.ctrip.itinerary.entity.enums.ItemType;
import com.ctrip.itinerary.service.ItineraryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 盲盒结果导入行程实现：同一事务创建行程与明细，结果表记录已导入行程。
 */
@Service
public class BlindBoxItineraryImportServiceImpl implements BlindBoxItineraryImportService {

    private static final Logger log = LoggerFactory.getLogger(BlindBoxItineraryImportServiceImpl.class);
    private static final String LOCK_PREFIX = "lock:blindbox:import:";
    private static final Pattern TIME_PATTERN =
            Pattern.compile("(上午|下午|傍晚|清晨|中午|凌晨|晚上)");
    private static final Pattern NAME_PATTERN =
            Pattern.compile("([\\u4e00-\\u9fa5]{2,30})(馆|楼|寺|塔|公园|广场|街|路|海滩|山|岛|码头|大学)");
    private static final String[][] TYPE_KEYWORDS = {
            {"用餐", "FOOD"}, {"午餐", "FOOD"}, {"晚餐", "FOOD"}, {"早餐", "FOOD"},
            {"小吃", "FOOD"}, {"海鲜", "FOOD"}, {"餐厅", "FOOD"},
            {"入住", "HOTEL"}, {"旅馆", "HOTEL"}, {"酒店", "HOTEL"},
            {"高铁", "TRANSPORT"}, {"轮渡", "TRANSPORT"}, {"地铁", "TRANSPORT"},
            {"公交", "TRANSPORT"}, {"骑行", "TRANSPORT"}, {"返程", "TRANSPORT"},
            {"出发", "TRANSPORT"},
            {"参观", "ATTRACTION"}, {"游览", "ATTRACTION"}, {"漫步", "ATTRACTION"},
            {"探访", "ATTRACTION"}, {"打卡", "ATTRACTION"}, {"登", "ATTRACTION"},
            {"逛", "ATTRACTION"},
            {"夜游", "ACTIVITY"}, {"活动", "ACTIVITY"}
    };

    private final BlindBoxOrderMapper orderMapper;
    private final BlindBoxResultMapper resultMapper;
    private final ItineraryService itineraryService;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    public BlindBoxItineraryImportServiceImpl(BlindBoxOrderMapper orderMapper,
                                              BlindBoxResultMapper resultMapper,
                                              ItineraryService itineraryService,
                                              RedissonClient redissonClient,
                                              ObjectMapper objectMapper) {
        this.orderMapper = orderMapper;
        this.resultMapper = resultMapper;
        this.itineraryService = itineraryService;
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ItineraryResponse importItinerary(Long userId, String orderNo) {
        BlindBoxOrder order = requireOrder(orderNo, userId);
        if (order.getStatus() != OrderStatus.OPENED) {
            throw new ForbiddenException("订单尚未开盒，无法导入行程");
        }
        BlindBoxResult result = requireResult(order.getId());
        if (result.getImportedItineraryId() != null) {
            ItineraryResponse existing = existingItinerary(
                    result.getImportedItineraryId(), userId);
            if (existing != null) {
                return existing;
            }
            clearImportedMarker(result.getId(), result.getImportedItineraryId());
            result.setImportedItineraryId(null);
        }

        RLock lock = redissonClient.getLock(LOCK_PREFIX + result.getId());
        try {
            // 不指定 lease，交由 Redisson watchdog 续期
            if (!lock.tryLock(3, TimeUnit.SECONDS)) {
                throw new BusinessException("系统繁忙，请稍后重试");
            }
            try {
                // 加锁读最新数据，避免 REPEATABLE READ 快照看到旧的 imported_itinerary_id
                BlindBoxResult latest = resultMapper.selectOne(
                        new LambdaQueryWrapper<BlindBoxResult>()
                                .eq(BlindBoxResult::getId, result.getId())
                                .last("FOR UPDATE"));
                if (latest != null && latest.getImportedItineraryId() != null) {
                    ItineraryResponse existing = existingItinerary(
                            latest.getImportedItineraryId(), userId);
                    if (existing != null) {
                        return existing;
                    }
                    clearImportedMarker(latest.getId(), latest.getImportedItineraryId());
                    latest.setImportedItineraryId(null);
                }
                ItineraryResponse itinerary = doImport(userId, latest != null ? latest : result);
                int updated = resultMapper.update(null, new LambdaUpdateWrapper<BlindBoxResult>()
                        .eq(BlindBoxResult::getId, result.getId())
                        .isNull(BlindBoxResult::getImportedItineraryId)
                        .set(BlindBoxResult::getImportedItineraryId, itinerary.id()));
                if (updated == 0) {
                    throw new BusinessException("行程已导入，请刷新后重试");
                }
                return itineraryService.getDetail(itinerary.id(), userId);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("导入被中断");
        }
    }

    private ItineraryResponse doImport(Long userId, BlindBoxResult result) {
        SchemeResponse scheme;
        try {
            scheme = objectMapper.readValue(result.getResultText(), SchemeResponse.class);
        } catch (Exception e) {
            throw new BusinessException("盲盒结果格式错误，无法导入");
        }
        if (scheme == null) {
            throw new BusinessException("盲盒结果不存在，无法导入");
        }

        int days = resolveDays(scheme);
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(days - 1L);
        String title = buildTitle(scheme, days);

        ItineraryResponse created = itineraryService.createItinerary(
                userId, new CreateItineraryRequest(title, scheme.destinationId(), startDate, endDate));
        List<ItineraryDayResponse> dayResponses = created.days();

        Map<String, Object> itinerary = scheme.itinerary();
        if (itinerary != null && !dayResponses.isEmpty()) {
            int count = Math.min(days, dayResponses.size());
            for (int i = 1; i <= count; i++) {
                Object dayObject = itinerary.get("day" + i);
                if (!(dayObject instanceof Map<?, ?> dayMap)) {
                    continue;
                }
                Object items = dayMap.get("items");
                if (!(items instanceof List<?> itemList)) {
                    continue;
                }
                int sortOrder = 1;
                for (Object rawItem : itemList) {
                    CreateItineraryItemRequest request = toItemRequest(rawItem, sortOrder++);
                    if (request != null) {
                        itineraryService.addItem(
                                created.id(), dayResponses.get(i - 1).id(), userId, request);
                    }
                }
            }
        }

        addTransport(userId, created.id(), dayResponses, scheme.transport());
        addHotel(userId, created.id(), dayResponses, scheme.hotel());
        log.info("盲盒结果导入行程: orderId={}, itineraryId={}", result.getOrderId(), created.id());
        return created;
    }

    private void addTransport(Long userId, Long itineraryId,
                              List<ItineraryDayResponse> days, Map<String, Object> transport) {
        if (days.isEmpty() || transport == null) {
            return;
        }
        String type = text(transport.get("type"));
        String description = text(transport.get("description"));
        if (type.isBlank() && description.isBlank()) {
            return;
        }
        String destination = text(transport.get("destination"));
        String name = (type.isBlank() ? "交通" : type) + (destination.isBlank() ? "" : ": " + destination);
        itineraryService.addItem(itineraryId, days.getFirst().id(), userId,
                new CreateItineraryItemRequest(
                        ItemType.TRANSPORT, limit(name, 30), "",
                        limit(text(transport.get("departureTime")), 50),
                        description, 1));
    }

    private void addHotel(Long userId, Long itineraryId,
                          List<ItineraryDayResponse> days, Map<String, Object> hotel) {
        if (days.isEmpty() || hotel == null) {
            return;
        }
        String name = text(hotel.get("name"));
        if (name.isBlank()) {
            return;
        }
        String rating = text(hotel.get("rating"));
        itineraryService.addItem(itineraryId, days.getLast().id(), userId,
                new CreateItineraryItemRequest(
                        ItemType.HOTEL, limit(name, 30), limit(text(hotel.get("address")), 255), "",
                        rating.isBlank() ? "" : "评分: " + rating, 99));
    }

    private CreateItineraryItemRequest toItemRequest(Object rawItem, int sortOrder) {
        if (rawItem instanceof Map<?, ?> map) {
            String name = text(map.get("name"));
            String description = text(map.get("description"));
            if (name.isBlank() && description.isBlank()) {
                return null;
            }
            String typeText = text(map.get("type"));
            ItemType type = parseType(typeText);
            String timeSlot = text(map.get("timeSlot"));
            if (timeSlot.isBlank()) {
                timeSlot = text(map.get("time"));
            }
            return new CreateItineraryItemRequest(
                    type, limit(name.isBlank() ? description : name, 30),
                    limit(text(map.get("location")), 255), limit(timeSlot, 50),
                    description, sortOrder);
        }

        String text = text(rawItem);
        if (text.isBlank()) {
            return null;
        }
        String timeSlot = "";
        Matcher timeMatcher = TIME_PATTERN.matcher(text);
        if (timeMatcher.find()) {
            timeSlot = timeMatcher.group(1);
        }
        String name = text;
        Matcher nameMatcher = NAME_PATTERN.matcher(text);
        if (nameMatcher.find()) {
            name = nameMatcher.group();
        }
        return new CreateItineraryItemRequest(
                inferType(text), limit(name, 30), "", limit(timeSlot, 50), text, sortOrder);
    }

    private ItemType inferType(String text) {
        for (String[] keyword : TYPE_KEYWORDS) {
            if (text.contains(keyword[0])) {
                return ItemType.valueOf(keyword[1]);
            }
        }
        return ItemType.ATTRACTION;
    }

    private ItemType parseType(String type) {
        if (type == null || type.isBlank()) {
            return ItemType.ATTRACTION;
        }
        try {
            return ItemType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return ItemType.ATTRACTION;
        }
    }

    /**
     * 天数取 days 字段与 itinerary dayN 键数量的较大值，避免静默丢天。
     */
    private int resolveDays(SchemeResponse scheme) {
        int declared = scheme.days() != null && scheme.days() > 0 ? scheme.days() : 0;
        int keys = 0;
        if (scheme.itinerary() != null) {
            for (String key : scheme.itinerary().keySet()) {
                if (key != null && key.startsWith("day")) {
                    try {
                        keys = Math.max(keys, Integer.parseInt(key.substring(3)));
                    } catch (NumberFormatException ignored) {
                        // 非 dayN 键忽略
                    }
                }
            }
        }
        int days = Math.max(declared, keys);
        return days > 0 ? Math.min(days, 30) : 3;
    }

    private String buildTitle(SchemeResponse scheme, int days) {
        String destination = scheme.destination() == null || scheme.destination().isBlank()
                ? "盲盒" : scheme.destination();
        String theme = scheme.theme() == null || scheme.theme().isBlank()
                ? "旅行" : scheme.theme();
        return limit(destination + "·" + theme + "·" + days + "天", 100);
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String limit(String value, int maxLength) {
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    /**
     * 已导入行程被删除时返回 null，允许重新导入。
     */
    private ItineraryResponse existingItinerary(Long itineraryId, Long userId) {
        try {
            return itineraryService.getDetail(itineraryId, userId);
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }

    private void clearImportedMarker(Long resultId, Long itineraryId) {
        resultMapper.update(null, new LambdaUpdateWrapper<BlindBoxResult>()
                .eq(BlindBoxResult::getId, resultId)
                .eq(BlindBoxResult::getImportedItineraryId, itineraryId)
                .set(BlindBoxResult::getImportedItineraryId, null));
    }

    private BlindBoxOrder requireOrder(String orderNo, Long userId) {
        BlindBoxOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<BlindBoxOrder>()
                        .eq(BlindBoxOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new ResourceNotFoundException("订单不存在：" + orderNo);
        }
        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException("无权导入他人订单");
        }
        return order;
    }

    private BlindBoxResult requireResult(Long orderId) {
        BlindBoxResult result = resultMapper.selectOne(
                new LambdaQueryWrapper<BlindBoxResult>()
                        .eq(BlindBoxResult::getOrderId, orderId));
        if (result == null) {
            throw new ResourceNotFoundException("盲盒结果不存在");
        }
        return result;
    }
}
