package com.ctrip.user.security;

/**
 * 用户封禁 Redis key。
 */
public final class UserBanKeys {

    public static final String PREFIX = "auth:deny:user:";

    private UserBanKeys() {
    }

    public static String key(Long userId) {
        return PREFIX + userId;
    }
}
