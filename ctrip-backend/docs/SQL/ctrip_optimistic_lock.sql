-- =====================================================
-- 管理端乐观锁：banner / recommendation / blind_box_template
-- 一次性迁移
-- =====================================================

ALTER TABLE `banner`
    ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';

ALTER TABLE `recommendation`
    ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';

ALTER TABLE `blind_box_template`
    ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';
