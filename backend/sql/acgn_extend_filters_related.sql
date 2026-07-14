-- ACGN：筛选字段 + 关联作品表（执行前请备份库）
-- 若某列已存在会报错，可跳过对应 ALTER。

USE `0721acgn`;

-- 番剧：题材、更新时间（热度排序用原有 rating）
ALTER TABLE `tb_animation`
    ADD COLUMN `genre` VARCHAR(64) NULL DEFAULT NULL COMMENT '题材' AFTER `status`;
ALTER TABLE `tb_animation`
    ADD COLUMN `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '条目更新时间';

-- 漫画：题材、开播/发售年份字符串、评分、更新时间
ALTER TABLE `tb_cartoons`
    ADD COLUMN `genre` VARCHAR(64) NULL DEFAULT NULL COMMENT '题材' AFTER `status`;
ALTER TABLE `tb_cartoons`
    ADD COLUMN `releasedate` VARCHAR(32) NULL DEFAULT NULL COMMENT '年份或日期字符串' AFTER `genre`;
ALTER TABLE `tb_cartoons`
    ADD COLUMN `rating` DECIMAL(4, 1) NULL DEFAULT NULL COMMENT '评分' AFTER `releasedate`;
ALTER TABLE `tb_cartoons`
    ADD COLUMN `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 轻小说（若已有 status / genre 等列请删掉对应 ALTER）
ALTER TABLE `tb_novel`
    ADD COLUMN `genre` VARCHAR(64) NULL DEFAULT NULL COMMENT '题材' AFTER `chaptercount`;
ALTER TABLE `tb_novel`
    ADD COLUMN `status` VARCHAR(16) NULL DEFAULT '连载' COMMENT '连载/完结' AFTER `genre`;
ALTER TABLE `tb_novel`
    ADD COLUMN `releasedate` VARCHAR(32) NULL DEFAULT NULL AFTER `status`;
ALTER TABLE `tb_novel`
    ADD COLUMN `rating` DECIMAL(4, 1) NULL DEFAULT NULL AFTER `releasedate`;
ALTER TABLE `tb_novel`
    ADD COLUMN `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `rating`;

-- 关联作品（手工维护 src → dst，演示用）
CREATE TABLE IF NOT EXISTS `tb_related_work` (
    `id`          INT AUTO_INCREMENT PRIMARY KEY,
    `src_type`    VARCHAR(16) NOT NULL COMMENT 'animation|comic|novel',
    `src_id`      INT         NOT NULL,
    `dst_type`    VARCHAR(16) NOT NULL,
    `dst_id`      INT         NOT NULL,
    UNIQUE KEY `uk_rel` (`src_type`, `src_id`, `dst_type`, `dst_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- 演示用题材 + 关联边（已按本机库真实 id 写好）：在同目录执行
--   mysql ... -D 0721acgn < acgn_seed_real_ids.sql
-- 详见文件：acgn_seed_real_ids.sql
