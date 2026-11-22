-- =====================================================
-- 盲盒结果一键导入行程
-- MySQL 8.0+ / UTF8MB4
-- 仅执行一次
-- =====================================================

ALTER TABLE `blind_box_result`
    ADD COLUMN `imported_itinerary_id` BIGINT NULL COMMENT '已导入的个人行程ID';
