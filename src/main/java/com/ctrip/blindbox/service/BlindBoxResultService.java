package com.ctrip.blindbox.service;

import com.ctrip.blindbox.dto.ResultResponse;
import com.ctrip.blindbox.dto.ShareResponse;

/**
 * 盲盒结果服务接口。
 *
 * 负责结果查询、分享链接生成与查看。
 */
public interface BlindBoxResultService {

    /**
     * 获取结果详情（通过订单号）。
     *
     * @param userId  当前用户ID
     * @param orderNo 订单编号
     * @return 结果详情
     */
    ResultResponse getResult(Long userId, String orderNo);

    /**
     * 生成分享链接。
     *
     * <p>若已分享过则复用已有 shareCode，否则生成新的 6 位 Base62 短码。
     * 分享链接默认 7 天过期。
     *
     * @param userId  当前用户ID
     * @param orderNo 订单编号
     * @return 分享信息
     */
    ShareResponse generateShareLink(Long userId, String orderNo);

    /**
     * 通过分享码查看盲盒结果（公开接口，无需认证）。
     *
     * @param shareCode 分享短码
     * @return 结果详情
     */
    ResultResponse getSharedResult(String shareCode);
}
