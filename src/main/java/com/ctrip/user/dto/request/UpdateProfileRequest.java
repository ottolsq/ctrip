package com.ctrip.user.dto.request;

import com.ctrip.user.entity.enums.Gender;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 更新用户资料请求 DTO（步骤 15 UserServiceImpl 使用）。
 *
 * <p>所有字段均为可选，仅传入需要修改的字段，null 表示不修改。
 */
public record UpdateProfileRequest(

        /** 用户名，2~50 位；null 表示不修改 */
        @Size(min = 2, max = 50)
        String username,

        /** 头像 URL，最长 512 位；null 表示不修改 */
        @Size(max = 512)
        String avatarUrl,

        /** 性别；null 表示不修改 */
        Gender gender,

        /** 生日；null 表示不修改 */
        LocalDate birthday,

        /** 实名认证姓名，最长 50 位；null 表示不修改 */
        @Size(max = 50)
        String realName

) {}
