package com.ctrip.content.service;

import com.ctrip.content.dto.response.AdminContentStatsResponse;

/**
 * 内容运营数据统计服务。
 */
public interface AdminContentService {

    AdminContentStatsResponse getContentStats();
}
