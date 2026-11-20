-- =====================================================
-- P1 后台管理数据库迁移
-- 适用：MySQL 8.0+ / UTF8MB4
-- 注意：本文件仅执行一次；执行前请备份 ctrip 数据库。
-- 执行顺序：基础 DDL 与示例数据之后执行本文件，再执行 ctrip_admin_seed.sql
-- =====================================================

-- 1. 用户：记录封禁前状态，解封时恢复
ALTER TABLE `users`
    ADD COLUMN `status_before_suspend` TINYINT NULL
        COMMENT '封禁前状态：0=UNVERIFIED 1=ACTIVE 2=SUSPENDED 3=DELETED';

-- 2. 攻略审核字段
ALTER TABLE `guides`
    ADD COLUMN `reject_reason` VARCHAR(500) NULL COMMENT '审核驳回理由',
    ADD COLUMN `reviewed_by` BIGINT NULL COMMENT '审核人用户ID',
    ADD COLUMN `reviewed_at` DATETIME NULL COMMENT '审核时间';

ALTER TABLE `guides`
    MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 0
        COMMENT '0=DRAFT 1=PUBLISHED 2=REJECTED 3=PENDING';

-- 3. 订单退款字段
ALTER TABLE `blind_box_order`
    ADD COLUMN `refund_reason` VARCHAR(500) NULL COMMENT '退款原因',
    ADD COLUMN `refunded_at` DATETIME NULL COMMENT '退款时间';

-- 4. 首页轮播图
CREATE TABLE IF NOT EXISTS `banner` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(100) NOT NULL COMMENT '轮播标题',
    `image_url` VARCHAR(500) NOT NULL COMMENT '图片地址',
    `link_url` VARCHAR(500) DEFAULT NULL COMMENT '跳转地址',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序，升序',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=启用 1=停用',
    `start_at` DATETIME DEFAULT NULL COMMENT '展示开始时间',
    `end_at` DATETIME DEFAULT NULL COMMENT '展示结束时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_banner_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='首页轮播图';

-- 5. 首页推荐位
CREATE TABLE IF NOT EXISTS `recommendation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `slot` VARCHAR(50) NOT NULL COMMENT '位置标识，如 home_featured',
    `title` VARCHAR(100) NOT NULL COMMENT '展示标题',
    `target_type` TINYINT NOT NULL COMMENT '0=攻略 1=目的地 2=盲盒模板',
    `target_id` BIGINT NOT NULL COMMENT '目标ID',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序，升序',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=启用 1=停用',
    `start_at` DATETIME DEFAULT NULL COMMENT '展示开始时间',
    `end_at` DATETIME DEFAULT NULL COMMENT '展示结束时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_recommendation_slot_status` (`slot`, `status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='首页推荐位';
