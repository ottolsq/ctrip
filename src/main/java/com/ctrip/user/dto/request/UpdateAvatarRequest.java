package com.ctrip.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 更新头像 URL 请求 DTO。
 *
 * @param avatarUrl 新头像图片地址（不可为空，最长 512 字符）
 */
public record UpdateAvatarRequest(
        @NotBlank(message = "头像地址不能为空")
        @Size(max = 512, message = "头像地址不能超过 512 个字符")
        String avatarUrl
) {}
