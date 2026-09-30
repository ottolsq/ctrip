-- 行程模块 DDL
-- 三张核心表 + 收藏表

-- 1. 行程主表
CREATE TABLE `itinerary` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '行程ID',
    `user_id` BIGINT NOT NULL COMMENT '创建者用户ID',
    `title` VARCHAR(100) NOT NULL COMMENT '行程标题',
    `destination_id` BIGINT DEFAULT NULL COMMENT '目的地ID',
    `start_date` DATE NOT NULL COMMENT '开始日期',
    `end_date` DATE NOT NULL COMMENT '结束日期',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-草稿 1-已发布 2-已归档',
    `share_code` VARCHAR(10) DEFAULT NULL COMMENT '分享短码（6位Base62）',
    `share_expires_at` DATETIME DEFAULT NULL COMMENT '分享链接过期时间',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览量',
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_share_code` (`share_code`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_destination_id` (`destination_id`),
    KEY `idx_share_code` (`share_code`, `share_expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行程表';

-- 2. 行程日程表
CREATE TABLE `itinerary_day` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日程ID',
    `itinerary_id` BIGINT NOT NULL COMMENT '所属行程ID',
    `day_number` INT NOT NULL COMMENT '第几天（从1开始）',
    `title` VARCHAR(50) NOT NULL COMMENT '日程标题',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_itinerary_id` (`itinerary_id`),
    KEY `idx_itinerary_sort` (`itinerary_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行程日程表';

-- 3. 行程项表
CREATE TABLE `itinerary_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '行程项ID',
    `itinerary_day_id` BIGINT NOT NULL COMMENT '所属日程ID',
    `type` TINYINT NOT NULL COMMENT '类型：0-酒店 1-景点 2-交通 3-美食 4-活动',
    `name` VARCHAR(100) NOT NULL COMMENT '名称',
    `location` VARCHAR(255) DEFAULT NULL COMMENT '位置/地址',
    `time_slot` VARCHAR(50) DEFAULT NULL COMMENT '时间段，如"09:00-11:00"',
    `description` TEXT DEFAULT NULL COMMENT '描述',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_day_id` (`itinerary_day_id`),
    KEY `idx_day_sort` (`itinerary_day_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行程项表';

-- 4. 收藏表（通用，支持多种目标类型）
CREATE TABLE `collection` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `target_type` TINYINT NOT NULL COMMENT '目标类型：0-行程 1-攻略 2-目的地',
    `target_id` BIGINT NOT NULL COMMENT '目标ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`),
    KEY `idx_user_type` (`user_id`, `target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';
