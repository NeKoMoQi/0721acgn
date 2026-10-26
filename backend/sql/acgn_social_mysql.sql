USE `0721acgn`;

-- 多端匿名设备点赞（每台浏览器 X-Acgn-Client）
CREATE TABLE IF NOT EXISTS `tb_acgn_vote` (
  `client_id` VARCHAR(64) NOT NULL,
  `media_type` VARCHAR(16) NOT NULL COMMENT 'animation|comic|novel',
  `media_id` INT NOT NULL,
  `vote` TINYINT NOT NULL COMMENT '1赞 -1踩',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`client_id`, `media_type`, `media_id`),
  KEY `idx_media` (`media_type`, `media_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 短评评论
CREATE TABLE IF NOT EXISTS `tb_acgn_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `media_type` VARCHAR(16) NOT NULL,
  `media_id` INT NOT NULL,
  `avatar` VARCHAR(1024) NOT NULL DEFAULT '',
  `nickname` VARCHAR(64) NOT NULL DEFAULT '访客',
  `body` VARCHAR(900) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_media_time` (`media_type`, `media_id`, `id` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `tb_acgn_comment`
  ADD COLUMN IF NOT EXISTS `avatar` VARCHAR(1024) NOT NULL DEFAULT '' AFTER `nickname`;
