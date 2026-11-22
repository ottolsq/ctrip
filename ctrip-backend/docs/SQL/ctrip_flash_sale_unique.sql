-- =====================================================
-- 秒杀订单：同一用户同一模板最多一条 FLASH_SALE 订单
-- MySQL 8.0+ 生成列 + 唯一索引（普通订单不受影响）
-- 一次性迁移
-- =====================================================

ALTER TABLE `blind_box_order`
    ADD COLUMN `flash_unique_key` VARCHAR(64)
        GENERATED ALWAYS AS (
            CASE WHEN `pay_method` = 'FLASH_SALE'
                 THEN CONCAT(`user_id`, ':', `template_id`)
                 ELSE NULL END
        ) STORED COMMENT '秒杀幂等键，非秒杀订单为 NULL';

CREATE UNIQUE INDEX `uk_flash_user_template`
    ON `blind_box_order` (`flash_unique_key`);
