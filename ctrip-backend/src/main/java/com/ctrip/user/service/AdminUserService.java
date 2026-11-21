package com.ctrip.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.user.dto.response.AdminUserDetailResponse;
import com.ctrip.user.dto.response.AdminUserResponse;
import com.ctrip.user.entity.enums.UserRole;
import com.ctrip.user.entity.enums.UserStatus;

import java.time.LocalDate;

/**
 * 管理端用户服务。
 */
public interface AdminUserService {

    Page<AdminUserResponse> listUsers(int page, int size, UserStatus status, UserRole role,
                                      String keyword, LocalDate startDate, LocalDate endDate);

    AdminUserDetailResponse getUserDetail(Long userId);

    void banUser(Long operatorId, Long userId);

    void unbanUser(Long userId);
}
