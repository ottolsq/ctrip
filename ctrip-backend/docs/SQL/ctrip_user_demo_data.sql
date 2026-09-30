-- 携程用户中心 - 示例数据
-- 表：users / refresh_tokens / password_reset_tokens

-- ----------------------------
-- 1. users 表（5条测试数据）
-- ----------------------------

-- 没有角色
INSERT INTO users (
    username, email, phone, password_hash, avatar_url,
    gender, birthday, real_name, status,
    email_verified, phone_verified, last_login_at
) VALUES
(
    'zhangsan', 'zhangsan@example.com', '13800138001',
    '$2a$10$E7rTn1VjH2d1aG2bK3cL4mN5oP6qR7sT8uV9wX0yZ1A2bC3dE4fG5',
    'https://img.example.com/avatar/1.jpg',
    1, '1990-05-15', '张三', 1, 1, 1, '2026-04-16 09:30:00'
),
(
    'lisi', 'lisi@example.com', '13800138002',
    '$2a$10$A1bC2dE3fH4jK5lM6nP7qR8sT9uV0wX1yZ2aB3cD4eF5gH6iJ7',
    'https://img.example.com/avatar/2.jpg',
    2, '1992-08-22', '李四', 1, 1, 1, '2026-04-16 10:15:00'
),
(
    'wangwu', 'wangwu@example.com', '13800138003',
    '$2a$10$B2cD3eF4gH5jK6lM7nP8qR9sT0uV1wX2yZ3aB4cD5eF6gH7iJ8',
    'https://img.example.com/avatar/3.jpg',
    0, NULL, NULL, 0, 0, 0, NULL
),
(
    'zhaoliu', 'zhaoliu@example.com', '13800138004',
    '$2a$10$C3dE4fG5hJ6kL7mN8pQ9rS0tU1vW2xY3zA4bC5dE6fG7hI8jK9',
    'https://img.example.com/avatar/4.jpg',
    1, '1988-12-05', '赵六', 2, 1, 1, '2026-04-15 16:40:00'
),
(
    'sunqi', 'sunqi@example.com', '13800138005',
    '$2a$10$D4eF5gH6jK7lM8nP9qR0sT1uV2wX3yZ4aB5cD6eF7gH8iJ9kL0',
    'https://img.example.com/avatar/5.jpg',
    2, '1995-03-10', '孙七', 1, 1, 1, '2026-04-16 11:20:00'
);

-- ----------------------------
-- 2. refresh_tokens 表（3条）
-- ----------------------------
INSERT INTO refresh_tokens (
    user_id, token_hash, device_info, issued_at, expires_at
) VALUES
(
    1,
    'A1B2C3D4E5F67890ABCDEF1234567890A1B2C3D4E5F67890ABCDEF1234567890',
    'iOS 17.5 | iPhone 15 | Safari',
    '2026-04-16 09:30:00',
    '2026-04-30 09:30:00'
),
(
    1,
    'B2C3D4E5F67890ABCDEF1234567890A1B2C3D4E5F67890ABCDEF12345678901',
    'Windows 11 | Chrome 124',
    '2026-04-16 10:00:00',
    '2026-04-30 10:00:00'
),
(
    2,
    'C3D4E5F67890ABCDEF1234567890A1B2C3D4E5F67890ABCDEF123456789012',
    'Android 14 | Xiaomi 14 | App',
    '2026-04-16 10:15:00',
    '2026-04-30 10:15:00'
);

-- ----------------------------
-- 3. password_reset_tokens 表（2条）
-- ----------------------------
INSERT INTO password_reset_tokens (
    user_id, token_hash, channel, expires_at
) VALUES
(
    1,
    'D4E5F67890ABCDEF1234567890A1B2C3D4E5F67890ABCDEF1234567890123',
    'EMAIL',
    '2026-04-17 09:30:00'
),
(
    2,
    'E5F67890ABCDEF1234567890A1B2C3D4E5F67890ABCDEF12345678901234',
    'SMS',
    '2026-04-17 10:15:00'
);



-- 清空测试数据
-- DELETE FROM password_reset_tokens;
-- DELETE FROM refresh_tokens;
-- DELETE FROM users;

-- 重置自增主键
-- ALTER TABLE password_reset_tokens AUTO_INCREMENT = 1;
-- ALTER TABLE refresh_tokens AUTO_INCREMENT = 1;
-- ALTER TABLE users AUTO_INCREMENT = 1;