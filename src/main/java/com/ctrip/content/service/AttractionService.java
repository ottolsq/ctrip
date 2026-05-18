package com.ctrip.content.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.content.dto.request.CreateAttractionRequest;
import com.ctrip.content.dto.response.AttractionResponse;

/**
 * 景点服务接口，负责景点的查询和管理。
 */
public interface AttractionService {

    /**
     * 分页查询景点列表（公开接口）。
     *
     * @param page          页码
     * @param limit         每页数量
     * @param destinationId 目的地筛选（可选）
     * @param keyword       关键词搜索（可选，匹配 name）
     * @return 分页的景点列表
     */
    Page<AttractionResponse> listAttractions(int page, int limit, Long destinationId, String keyword);

    /**
     * 获取景点详情。
     *
     * @param id 景点 ID
     * @return 景点详情响应
     * @throws com.ctrip.common.exception.ResourceNotFoundException 景点不存在时
     */
    AttractionResponse getDetail(Long id);

    /**
     * 创建景点（管理端）。
     *
     * @param request 创建请求
     * @return 创建后的景点响应
     */
    AttractionResponse createAttraction(CreateAttractionRequest request);

    /**
     * 更新景点（管理端）。
     *
     * @param id      景点 ID
     * @param request 更新请求（仅传入需要修改的字段）
     * @return 更新后的景点响应
     * @throws com.ctrip.common.exception.ResourceNotFoundException 景点不存在时
     */
    void updateAttraction(Long id, com.ctrip.content.dto.request.UpdateAttractionRequest request);

    /**
     * 删除景点（管理端）。
     *
     * @param id 景点 ID
     * @throws com.ctrip.common.exception.ResourceNotFoundException 景点不存在时
     */
    void deleteAttraction(Long id);
}
