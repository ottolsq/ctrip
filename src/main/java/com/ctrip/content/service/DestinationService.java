package com.ctrip.content.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.content.dto.request.CreateDestinationRequest;
import com.ctrip.content.dto.request.UpdateDestinationRequest;
import com.ctrip.content.dto.response.DestinationResponse;

/**
 * 目的地服务接口，负责目的地的查询和管理。
 */
public interface DestinationService {

    /**
     * 分页查询目的地列表（公开接口）。
     *
     * @param page     页码
     * @param limit    每页数量
     * @param country  国家筛选（可选）
     * @param province 省份筛选（可选）
     * @param keyword  关键词搜索（可选，匹配 name）
     * @return 分页的目的地列表
     */
    Page<DestinationResponse> listDestinations(int page, int limit, String country, String province, String keyword);

    /**
     * 获取目的地详情（含关联景点列表）。
     *
     * @param id 目的地 ID
     * @return 目的地详情响应
     * @throws com.ctrip.common.exception.ResourceNotFoundException 目的地不存在时
     */
    DestinationResponse getDetail(Long id);

    /**
     * 创建目的地（管理端）。
     *
     * @param request 创建请求
     * @return 创建后的目的地响应
     */
    DestinationResponse createDestination(CreateDestinationRequest request);

    /**
     * 更新目的地（管理端）。
     *
     * @param id      目的地 ID
     * @param request 更新请求（仅传入需要修改的字段）
     * @return 更新后的目的地响应
     * @throws com.ctrip.common.exception.ResourceNotFoundException 目的地不存在时
     */
    DestinationResponse updateDestination(Long id, UpdateDestinationRequest request);

    /**
     * 删除目的地（管理端）。
     *
     * @param id 目的地 ID
     * @throws com.ctrip.common.exception.ResourceNotFoundException 目的地不存在时
     */
    void deleteDestination(Long id);
}
