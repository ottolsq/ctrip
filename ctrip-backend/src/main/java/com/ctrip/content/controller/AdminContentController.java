package com.ctrip.content.controller;

import com.ctrip.common.response.ApiResponse;
import com.ctrip.content.dto.response.AdminContentStatsResponse;
import com.ctrip.content.service.AdminContentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端内容数据统计控制器。
 */
@RestController
@RequestMapping("/api/v1/admin/content")
@PreAuthorize("hasAnyRole('ADMIN','CONTENT_OPERATOR')")
public class AdminContentController {

    private final AdminContentService adminContentService;

    public AdminContentController(AdminContentService adminContentService) {
        this.adminContentService = adminContentService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminContentStatsResponse>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(adminContentService.getContentStats()));
    }
}
