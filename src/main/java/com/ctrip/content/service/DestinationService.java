package com.ctrip.content.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.content.dto.request.CreateDestinationRequest;
import com.ctrip.content.dto.request.UpdateDestinationRequest;
import com.ctrip.content.dto.response.DestinationResponse;

/**
 * 目的地服务接口，负责目的地的查询和管理。
 */
public interface DestinationService {

    Page<DestinationResponse> listDestinations(int page, int limit, String country, String province, String keyword);

    DestinationResponse getDetail(Long id);

    DestinationResponse createDestination(CreateDestinationRequest request);

    DestinationResponse updateDestination(Long id, UpdateDestinationRequest request);

    void deleteDestination(Long id);
}
