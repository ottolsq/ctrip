package com.ctrip.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.common.response.ApiResponse;
import com.ctrip.user.dto.response.AdminUserDetailResponse;
import com.ctrip.user.dto.response.AdminUserResponse;
import com.ctrip.user.entity.enums.UserRole;
import com.ctrip.user.entity.enums.UserStatus;
import com.ctrip.user.service.AdminUserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 管理端用户管理控制器。仅 ADMIN 可访问。
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Page<AdminUserResponse> result = adminUserService.listUsers(
                page, size, status, role, keyword, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(adminUserService.getUserDetail(id)));
    }

    @PostMapping("/{id}/ban")
    public ResponseEntity<ApiResponse<Void>> ban(@PathVariable Long id,
                                                 @AuthenticationPrincipal Long operatorId) {
        adminUserService.banUser(operatorId, id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{id}/unban")
    public ResponseEntity<ApiResponse<Void>> unban(@PathVariable Long id) {
        adminUserService.unbanUser(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
