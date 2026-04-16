package com.ctrip.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改密码请求 DTO（步骤 15 UserServiceImpl 使用）。
 *
 * <p>需要提供当前密码以验证身份，防止会话被劫持时密码被静默修改。
 */
public record ChangePasswordRequest(

        /** 当前密码（用于身份二次确认） */
        @NotBlank
        String currentPassword,

        /** 新密码，8~64 位 */
        @NotBlank @Size(min = 8, max = 64)
        String newPassword

) {}
