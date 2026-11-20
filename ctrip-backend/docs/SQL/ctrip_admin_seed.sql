-- =====================================================
-- 开发环境后台种子账号
-- 仅用于本地开发与答辩演示，禁止在生产环境执行。
--
-- 账号：
--   admin@example.com    / Admin12345    (ADMIN)
--   operator@example.com / Operator12345 (CONTENT_OPERATOR)
--
-- 密码为 BCrypt strength 12 哈希。
-- =====================================================

INSERT IGNORE INTO `users`
(`username`, `email`, `password_hash`, `status`, `role`, `email_verified`, `phone_verified`)
VALUES
('admin', 'admin@example.com',
 '$2a$12$Cwz/0fOA/gAyuO7COz.07ehv3knqjfXrSW0EPmvk/jksyfviwej7W', 1, 1, 1, 1),
('operator', 'operator@example.com',
 '$2a$12$vWafulrHDdye5VMzPtwfkO6JUIyemsMYZ9WgCZ8u.GyDqnDyTWK5C', 1, 2, 1, 1);
