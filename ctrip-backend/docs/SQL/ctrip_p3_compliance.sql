-- =====================================================
-- P3 盲盒合规：抽取记录留存
-- MySQL 8.0+ / UTF8MB4
-- 可重复执行
-- =====================================================

CREATE TABLE IF NOT EXISTS `blind_box_draw_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_id` BIGINT NOT NULL COMMENT '订单ID，幂等唯一键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `template_id` BIGINT NOT NULL COMMENT '盲盒模板ID',
    `tier` TINYINT NOT NULL COMMENT '0=COMMON 1=RARE 2=LEGENDARY',
    `destination_id` BIGINT DEFAULT NULL COMMENT '目的地ID',
    `destination_name` VARCHAR(100) NOT NULL COMMENT '目的地名称快照',
    `guaranteed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否保底触发',
    `probability_version` VARCHAR(32) NOT NULL COMMENT '概率配置版本',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '抽取时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_draw_order` (`order_id`),
    KEY `idx_draw_user_created` (`user_id`, `created_at`),
    KEY `idx_draw_template_created` (`template_id`, `created_at`),
    KEY `idx_draw_tier` (`tier`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='盲盒抽取记录';
