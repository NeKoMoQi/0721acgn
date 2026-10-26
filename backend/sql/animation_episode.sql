CREATE TABLE IF NOT EXISTS `tb_animation_episode` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `animation_id` INT NOT NULL,
  `episode_no` INT NOT NULL,
  `title` VARCHAR(128) NOT NULL,
  `video_url` VARCHAR(512) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_animation_episode` (`animation_id`, `episode_no`),
  KEY `idx_animation_episode_animation` (`animation_id`)
);

INSERT INTO `tb_animation_episode` (`animation_id`, `episode_no`, `title`, `video_url`)
VALUES
  (10941, 1, '第 1 集', 'https://acgn0721.oss-cn-hangzhou.aliyuncs.com/anime-reality/animation-video/b3980883-9188-462c-8667-0a8c325f7a82.mp4'),
  (10941, 2, '第 2 集', 'https://acgn0721.oss-cn-hangzhou.aliyuncs.com/anime-reality/animation-video/e257ba33-3d21-4751-ac7c-9a2cee5564ce.mp4')
ON DUPLICATE KEY UPDATE
  `title` = VALUES(`title`),
  `video_url` = VALUES(`video_url`);
