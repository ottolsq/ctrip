package com.ctrip.content.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.content.dto.request.CreateGuideRequest;
import com.ctrip.content.dto.request.UpdateGuideRequest;
import com.ctrip.content.dto.response.GuideListResponse;
import com.ctrip.content.dto.response.GuideResponse;

/**
 * 攻略服务接口，负责攻略的发布、浏览、编辑和互动。
 */
public interface GuideService {

    /**
     * 分页查询攻略列表（公开接口，仅返回已发布的攻略）。
     *
     * @param page          页码
     * @param limit         每页数量
     * @param destinationId 目的地筛选（可选）
     * @param authorId      作者筛选（可选）
     * @param keyword       关键词搜索（可选，匹配 title）
     * @param sortBy        排序方式：create_time / view_count / like_count
     * @return 分页的攻略列表
     */
    Page<GuideListResponse> listGuides(int page, int limit, Long destinationId, Long authorId, String keyword, String sortBy);

    /**
     * 获取攻略详情（浏览量 +1）。
     *
     * @param id 攻略 ID
     * @return 攻略详情响应
     * @throws com.ctrip.common.exception.ResourceNotFoundException 攻略不存在或未发布时
     */
    GuideResponse getDetail(Long id);

    /**
     * 创建攻略（需 JWT 认证）。
     *
     * @param authorId 作者用户 ID
     * @param request  创建请求
     * @return 创建后的攻略响应
     */
    GuideResponse createGuide(Long authorId, CreateGuideRequest request);

    /**
     * 更新攻略（需 JWT 认证，仅作者或 ADMIN）。
     *
     * @param id      攻略 ID
     * @param userId  当前操作用户 ID
     * @param request 更新请求
     * @return 更新后的攻略响应
     * @throws com.ctrip.common.exception.ResourceNotFoundException 攻略不存在时
     * @throws com.ctrip.common.exception.AuthenticationException 无权操作时
     */
    GuideResponse updateGuide(Long id, Long userId, UpdateGuideRequest request);

    /**
     * 删除攻略（需 JWT 认证，仅作者或 ADMIN）。
     *
     * @param id     攻略 ID
     * @param userId 当前操作用户 ID
     * @throws com.ctrip.common.exception.ResourceNotFoundException 攻略不存在时
     * @throws com.ctrip.common.exception.AuthenticationException 无权操作时
     */
    void deleteGuide(Long id, Long userId);

    /**
     * 点赞攻略（需 JWT 认证）。
     *
     * @param id     攻略 ID
     * @param userId 点赞用户 ID
     * @throws com.ctrip.common.exception.ResourceNotFoundException 攻略不存在时
     */
    void likeGuide(Long id, Long userId);

    /**
     * 取消点赞（需 JWT 认证）。
     *
     * @param id     攻略 ID
     * @param userId 取消点赞用户 ID
     * @throws com.ctrip.common.exception.ResourceNotFoundException 攻略不存在时
     */
    void unlikeGuide(Long id, Long userId);
}
