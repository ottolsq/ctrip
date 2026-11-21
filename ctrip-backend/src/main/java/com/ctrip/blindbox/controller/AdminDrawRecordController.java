package com.ctrip.blindbox.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.blindbox.dto.DrawRecordResponse;
import com.ctrip.blindbox.prize.PrizeTier;
import com.ctrip.blindbox.service.DrawRecordService;
import com.ctrip.common.response.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 管理端盲盒抽取记录控制器。仅 ADMIN 可访问。
 */
@RestController
@RequestMapping("/api/v1/admin/blind-box/draw-records")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDrawRecordController {

    private final DrawRecordService drawRecordService;

    public AdminDrawRecordController(DrawRecordService drawRecordService) {
        this.drawRecordService = drawRecordService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DrawRecordResponse>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) PrizeTier tier,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.ok(
                drawRecordService.listAll(
                        page, size, userId, templateId, tier, startDate, endDate)));
    }
}
