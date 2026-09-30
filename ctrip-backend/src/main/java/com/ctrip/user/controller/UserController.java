package com.ctrip.user.controller;

import com.ctrip.common.response.ApiResponse;
import com.ctrip.user.dto.request.ChangePasswordRequest;
import com.ctrip.user.dto.request.UpdateAvatarRequest;
import com.ctrip.user.dto.request.UpdateProfileRequest;
import com.ctrip.user.dto.response.MessageResponse;
import com.ctrip.user.dto.response.UserProfileResponse;
import com.ctrip.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 用户资料控制器，处理已登录用户的个人信息管理端点（均需 JWT 鉴权）。
 *
 * <p>从 Spring Security 上下文中获取当前用户 ID（由 JwtAuthenticationFilter 注入，
 * 类型为 Long），通过 @AuthenticationPrincipal 注解绑定到方法参数。
 *
 * <p>Controller 层职责：解析请求、校验、调用 Service、包装 ApiResponse。禁止含业务逻辑。
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 查看当前登录用户的个人资料。
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal Long userId) {
        UserProfileResponse profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    /**
     * 更新个人资料（所有字段可选，null 字段不修改）。
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse profile = userService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    /**
     * 修改密码（需提供当前密码进行验证）。
     */
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<MessageResponse>> changePassword(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(new MessageResponse("密码修改成功")));
    }

    /**
     * 更新头像 URL。
     */
    @PutMapping("/me/avatar")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateAvatar(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateAvatarRequest request) {
        UserProfileResponse profile = userService.updateAvatar(userId, request.avatarUrl());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }
}
