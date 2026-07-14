-- 演示种子数据：题材 / 评分 / 关联边（id 来自库内真实记录，执行前需已跑 acgn_extend_filters_related.sql）
USE `0721acgn`;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 同 IP 关联（双向边，简介页都能跳到另外两个媒介）
-- 番剧 id: 10836,10837,10838,10839 | 漫画 comicid: 1–4 | 小说 novelid: 1–4
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO `tb_related_work` (`src_type`, `src_id`, `dst_type`, `dst_id`) VALUES
('animation', 10836, 'comic', 1),
('animation', 10836, 'novel', 1),
('comic', 1, 'animation', 10836),
('comic', 1, 'novel', 1),
('novel', 1, 'animation', 10836),
('novel', 1, 'comic', 1),
('animation', 10837, 'comic', 2),
('animation', 10837, 'novel', 2),
('comic', 2, 'animation', 10837),
('comic', 2, 'novel', 2),
('novel', 2, 'animation', 10837),
('novel', 2, 'comic', 2),
('animation', 10838, 'comic', 3),
('animation', 10838, 'novel', 3),
('comic', 3, 'animation', 10838),
('comic', 3, 'novel', 3),
('novel', 3, 'animation', 10838),
('novel', 3, 'comic', 3),
('animation', 10839, 'comic', 4),
('animation', 10839, 'novel', 4),
('comic', 4, 'animation', 10839),
('comic', 4, 'novel', 4),
('novel', 4, 'animation', 10839),
('novel', 4, 'comic', 4);

-- ---------------------------------------------------------------------------
-- 番剧：题材（status 保持库中原值，避免改动原有业务含义）
-- ---------------------------------------------------------------------------
UPDATE `tb_animation` SET `genre` = '战斗' WHERE `id` = 10836;
UPDATE `tb_animation` SET `genre` = '奇幻' WHERE `id` = 10837;
UPDATE `tb_animation` SET `genre` = '日常' WHERE `id` = 10838;
UPDATE `tb_animation` SET `genre` = '科幻' WHERE `id` = 10839;
UPDATE `tb_animation` SET `genre` = '恋爱' WHERE `id` = 10840;
UPDATE `tb_animation` SET `genre` = '悬疑' WHERE `id` = 10841;
UPDATE `tb_animation` SET `genre` = '治愈' WHERE `id` = 10842;
UPDATE `tb_animation` SET `genre` = '校园' WHERE `id` = 10843;

-- ---------------------------------------------------------------------------
-- 漫画：题材、年份串（供年代筛选）、评分、状态统一为「连载/完结」便于前端筛选
-- ---------------------------------------------------------------------------
UPDATE `tb_cartoons` SET `genre` = '恋爱', `releasedate` = '2022-04-10', `rating` = 9.0 WHERE `comicid` = 1;
UPDATE `tb_cartoons` SET `genre` = '奇幻', `releasedate` = '2021-08-20', `rating` = 8.6 WHERE `comicid` = 2;
UPDATE `tb_cartoons` SET `genre` = '战斗', `releasedate` = '2020-11-05', `rating` = 9.2 WHERE `comicid` = 3;
UPDATE `tb_cartoons` SET `genre` = '日常', `releasedate` = '2019-03-15', `rating` = 8.3 WHERE `comicid` = 4;
UPDATE `tb_cartoons` SET `genre` = '科幻', `releasedate` = '2023-01-08', `rating` = 8.7 WHERE `comicid` = 5;
UPDATE `tb_cartoons` SET `genre` = '悬疑', `releasedate` = '2018-07-22', `rating` = 8.5 WHERE `comicid` = 6;
UPDATE `tb_cartoons` SET `genre` = '治愈', `releasedate` = '2024-02-14', `rating` = 9.1 WHERE `comicid` = 7;
UPDATE `tb_cartoons` SET `genre` = '校园', `releasedate` = '2017-09-30', `rating` = 8.8 WHERE `comicid` = 8;

UPDATE `tb_cartoons`
SET `status` = CASE
    WHEN `status` LIKE '%完结%' OR `status` = '完结' OR `status` = '已完结' THEN '完结'
    ELSE '连载'
END
WHERE `comicid` BETWEEN 1 AND 8;

-- ---------------------------------------------------------------------------
-- 轻小说：题材、连载状态、年份、评分
-- ---------------------------------------------------------------------------
UPDATE `tb_novel` SET `genre` = '校园', `status` = '连载', `releasedate` = '2020-05-01', `rating` = 8.9 WHERE `novelid` = 1;
UPDATE `tb_novel` SET `genre` = '恋爱', `status` = '完结', `releasedate` = '2019-11-11', `rating` = 9.0 WHERE `novelid` = 2;
UPDATE `tb_novel` SET `genre` = '奇幻', `status` = '连载', `releasedate` = '2021-02-18', `rating` = 8.7 WHERE `novelid` = 3;
UPDATE `tb_novel` SET `genre` = '日常', `status` = '完结', `releasedate` = '2018-06-06', `rating` = 8.4 WHERE `novelid` = 4;
UPDATE `tb_novel` SET `genre` = '战斗', `status` = '连载', `releasedate` = '2022-09-09', `rating` = 8.8 WHERE `novelid` = 5;
UPDATE `tb_novel` SET `genre` = '科幻', `status` = '完结', `releasedate` = '2017-12-12', `rating` = 9.2 WHERE `novelid` = 6;
UPDATE `tb_novel` SET `genre` = '悬疑', `status` = '连载', `releasedate` = '2023-04-04', `rating` = 8.6 WHERE `novelid` = 7;
UPDATE `tb_novel` SET `genre` = '治愈', `status` = '完结', `releasedate` = '2016-10-10', `rating` = 9.1 WHERE `novelid` = 8;
