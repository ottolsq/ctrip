-- =====================================================
-- 盲盒模块数据库表结构
-- 基于 docs/blindbox_module.md 设计
-- =====================================================

-- 1. 盲盒模板表
CREATE TABLE IF NOT EXISTS `blind_box_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `name` VARCHAR(100) NOT NULL COMMENT '盲盒名称',
    `type` TINYINT NOT NULL DEFAULT 0 COMMENT '类型：0=日常盲盒(DAILY), 1=限定盲盒(LIMITED)',
    `price` DECIMAL(10, 2) NOT NULL COMMENT '价格（元）',
    `stock` INT NOT NULL DEFAULT -1 COMMENT '库存：-1=无限，>=0=限量',
    `rule_config` JSON DEFAULT NULL COMMENT '规则配置JSON（活动起止时间、折扣等）',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0=上架(ACTIVE), 1=下架(INACTIVE)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_type_status` (`type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='盲盒模板表';

-- 2. 盲盒订单表
CREATE TABLE IF NOT EXISTS `blind_box_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID（FK → users）',
    `template_id` BIGINT NOT NULL COMMENT '盲盒模板ID（FK → blind_box_template）',
    `order_no` VARCHAR(30) NOT NULL COMMENT '订单编号（业务主键，格式：BB + 日期 + 序号）',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0=待支付, 1=已支付, 2=已开盒, 3=已退款, 4=已取消',
    `pay_amount` DECIMAL(10, 2) NOT NULL COMMENT '实际支付金额',
    `pay_method` VARCHAR(20) DEFAULT NULL COMMENT '支付方式：ALIPAY / WECHAT_PAY',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `expire_at` DATETIME NOT NULL COMMENT '支付超时时间（创建后15分钟）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_template_id` (`template_id`),
    KEY `idx_status_expire` (`status`, `expire_at`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_blindbox_order_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_blindbox_order_template` FOREIGN KEY (`template_id`) REFERENCES `blind_box_template` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='盲盒订单表';

-- 3. 盲盒结果表
CREATE TABLE IF NOT EXISTS `blind_box_result` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `order_id` BIGINT NOT NULL COMMENT '关联的订单ID（FK → blind_box_order）',
    `destination` VARCHAR(100) NOT NULL COMMENT '目的地名称',
    `destination_id` BIGINT DEFAULT NULL COMMENT '目的地ID（FK → destination）',
    `theme` VARCHAR(50) DEFAULT NULL COMMENT '旅行主题（美食/海滨/古镇/滑雪/亲子等）',
    `result_text` LONGTEXT NOT NULL COMMENT '完整旅行方案 JSON',
    `result_image_url` VARCHAR(500) DEFAULT NULL COMMENT 'AI生成结果图URL（暂不使用）',
    `share_code` VARCHAR(10) DEFAULT NULL COMMENT '分享链接短码（6位Base62）',
    `share_expires_at` DATETIME DEFAULT NULL COMMENT '分享链接过期时间',
    `opened_at` DATETIME DEFAULT NULL COMMENT '开盒时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_id` (`order_id`),
    UNIQUE KEY `uk_share_code` (`share_code`),
    CONSTRAINT `fk_blindbox_result_order` FOREIGN KEY (`order_id`) REFERENCES `blind_box_order` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='盲盒结果表';

-- 4. 盲盒预选参数表
CREATE TABLE IF NOT EXISTS `blind_box_preference` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `order_id` BIGINT NOT NULL COMMENT '关联的订单ID（FK → blind_box_order）',
    `departure_city` VARCHAR(50) NOT NULL COMMENT '出发城市',
    `budget_level` TINYINT NOT NULL DEFAULT 1 COMMENT '预算等级：0=经济型, 1=标准型, 2=豪华型',
    `theme` VARCHAR(50) DEFAULT NULL COMMENT '旅行主题（可选）',
    `image_tags` JSON DEFAULT NULL COMMENT 'AI分析图片特征标签（暂不使用）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_id` (`order_id`),
    CONSTRAINT `fk_blindbox_preference_order` FOREIGN KEY (`order_id`) REFERENCES `blind_box_order` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='盲盒预选参数表';
