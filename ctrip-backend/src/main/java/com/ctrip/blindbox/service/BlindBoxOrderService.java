package com.ctrip.blindbox.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.blindbox.dto.CreateOrderRequest;
import com.ctrip.blindbox.dto.OrderResponse;

/**
 * 盲盒订单服务接口。
 *
 * 负责订单的创建、查询、取消、开盒等核心业务逻辑。
 * 包含库存扣减、分布式锁、幂等性保证。
 */
public interface BlindBoxOrderService {

    /**
     * 创建盲盒订单。
     *
     * <p>流程：验证模板 → 扣减库存（限定盲盒） → 创建订单 → 保存预选参数
     *
     * @param userId  当前用户ID
     * @param request 创建订单请求
     * @return 订单信息
     */
    OrderResponse createOrder(Long userId, CreateOrderRequest request);

    /**
     * 查询订单详情（通过订单号）。
     *
     * @param userId 当前用户ID
     * @param orderNo 订单编号
     * @return 订单详情
     */
    OrderResponse getOrderDetail(Long userId, String orderNo);

    /**
     * 查询我的订单列表（分页）。
     *
     * @param userId 当前用户ID
     * @param page 页码
     * @param limit 每页条数
     * @param status 筛选状态（可选）
     * @return 分页的订单列表
     */
    Page<OrderResponse> listMyOrders(Long userId, int page, int limit, String status);

    /**
     * 取消订单（仅 PENDING 状态可取消）。
     *
     * <p>限定盲盒取消时自动恢复库存。
     *
     * @param userId  当前用户ID
     * @param orderNo 订单编号
     */
    void cancelOrder(Long userId, String orderNo);

    /**
     * 开盒。
     *
     * <p>流程：验证订单 → 获取分布式锁 → 幂等检查 → 更新状态 → 生成方案 → 保存结果 → 释放锁
     *
     * @param userId  当前用户ID
     * @param orderNo 订单编号
     * @return 开盒结果
     */
    OrderResponse openBox(Long userId, String orderNo);

    /**
     * 模拟支付回调。
     *
     * <p>MVP 阶段使用，不对接真实支付渠道。
     *
     * @param orderNo   订单编号
     * @param payMethod 支付方式（ALIPAY / WECHAT_PAY）
     */
    void payCallback(String orderNo, String payMethod);

    /**
     * 定时任务：扫描并取消过期订单。
     *
     * <p>每分钟执行一次，取消状态为 PENDING 且 expireAt < NOW() 的订单，
     * 限定盲盒自动恢复库存。
     */
    void cancelExpiredOrders();

    /**
     * 查询我的盲盒列表（含开盒状态）。
     *
     * @param userId 当前用户ID
     * @param page 页码
     * @param limit 每页条数
     * @param opened 筛选：true=已开盒，false=未开盒（可选）
     * @return 分页的盲盒列表（含结果信息）
     */
    Page<OrderResponse> listMyBlindBoxes(Long userId, int page, int limit, Boolean opened);
}
