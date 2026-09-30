-- 内容与目的地模块数据库表
-- 适用于 MySQL 8.0+ / UTF8MB4

-- 目的地表
CREATE TABLE IF NOT EXISTS `destinations` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(100) NOT NULL COMMENT '目的地名称',
    `country`     VARCHAR(60)  NOT NULL COMMENT '所属国家',
    `province`    VARCHAR(60)  DEFAULT NULL COMMENT '所属省份/州',
    `description` TEXT         DEFAULT NULL COMMENT '目的地简介',
    `best_season` TINYINT      NOT NULL DEFAULT 0 COMMENT '最佳季节: 1春 2夏 3秋 4冬 5全年',
    `cover_url`   VARCHAR(500) DEFAULT NULL COMMENT '封面图 URL',
    `image_urls`  JSON         DEFAULT NULL COMMENT '多图 JSON 数组',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_destination_country` (`country`),
    INDEX `idx_destination_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='目的地';

-- 景点表
CREATE TABLE IF NOT EXISTS `attractions` (
    `id`            BIGINT         NOT NULL AUTO_INCREMENT,
    `destination_id` BIGINT        NOT NULL COMMENT '所属目的地 ID',
    `name`          VARCHAR(200)   NOT NULL COMMENT '景点名称',
    `description`   TEXT           DEFAULT NULL COMMENT '景点简介',
    `location`      VARCHAR(300)   DEFAULT NULL COMMENT '详细地址/坐标',
    `ticket_price`  DECIMAL(10, 2) DEFAULT NULL COMMENT '门票价格，NULL 表示免费',
    `cover_url`     VARCHAR(500)   DEFAULT NULL COMMENT '封面图 URL',
    `image_urls`    JSON           DEFAULT NULL COMMENT '多图 JSON 数组',
    `created_at`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_attraction_destination` (`destination_id`),
    INDEX `idx_attraction_name` (`name`),
    CONSTRAINT `fk_attraction_destination`
        FOREIGN KEY (`destination_id`) REFERENCES `destinations`(`id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='景点';

-- 攻略表
CREATE TABLE IF NOT EXISTS `guides` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `author_id`       BIGINT       NOT NULL COMMENT '作者用户 ID',
    `title`           VARCHAR(200) NOT NULL COMMENT '攻略标题',
    `content`         LONGTEXT     NOT NULL COMMENT '攻略正文（Markdown）',
    `destination_id`  BIGINT       DEFAULT NULL COMMENT '关联目的地 ID',
    `cover_url`       VARCHAR(500) DEFAULT NULL COMMENT '封面图 URL',
    `image_urls`      JSON         DEFAULT NULL COMMENT '多图 JSON 数组',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0草稿 1已发布 2驳回',
    `view_count`      INT          NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `like_count`      INT          NOT NULL DEFAULT 0 COMMENT '点赞次数',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_guide_author` (`author_id`),
    INDEX `idx_guide_destination` (`destination_id`),
    INDEX `idx_guide_status` (`status`),
    INDEX `idx_guide_created` (`created_at`),
    CONSTRAINT `fk_guide_author`
        FOREIGN KEY (`author_id`) REFERENCES `users`(`id`)
        ON DELETE CASCADE,
    CONSTRAINT `fk_guide_destination`
        FOREIGN KEY (`destination_id`) REFERENCES `destinations`(`id`)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='攻略';

-- 评论表
CREATE TABLE IF NOT EXISTS `comments` (
    `id`         BIGINT     NOT NULL AUTO_INCREMENT,
    `guide_id`   BIGINT     NOT NULL COMMENT '所属攻略 ID',
    `user_id`    BIGINT     NOT NULL COMMENT '评论者用户 ID',
    `parent_id`  BIGINT     DEFAULT NULL COMMENT '父评论 ID，NULL 为顶级评论',
    `content`    TEXT       NOT NULL COMMENT '评论内容',
    `like_count` INT        NOT NULL DEFAULT 0 COMMENT '点赞次数',
    `created_at` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_comment_guide` (`guide_id`),
    INDEX `idx_comment_user` (`user_id`),
    INDEX `idx_comment_parent` (`parent_id`),
    CONSTRAINT `fk_comment_guide`
        FOREIGN KEY (`guide_id`) REFERENCES `guides`(`id`)
        ON DELETE CASCADE,
    CONSTRAINT `fk_comment_user`
        FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论';
