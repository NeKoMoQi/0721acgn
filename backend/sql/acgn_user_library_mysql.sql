USE `0721acgn`;

-- 浏览足迹 + 收藏（星标），按 X-Acgn-Client 匿名设备维度
CREATE TABLE IF NOT EXISTS `tb_acgn_library` (
  `client_id` VARCHAR(64) NOT NULL,
  `media_type` VARCHAR(16) NOT NULL COMMENT 'animation|comic|novel',
  `media_id` INT NOT NULL,
  `title` VARCHAR(512) DEFAULT NULL,
  `pic` VARCHAR(1024) DEFAULT NULL,
  `starred` TINYINT NOT NULL DEFAULT 0,
  `starred_at` BIGINT DEFAULT NULL COMMENT 'epoch ms',
  `last_visit_at` BIGINT NOT NULL COMMENT 'epoch ms',
  PRIMARY KEY (`client_id`, `media_type`, `media_id`),
  KEY `idx_cli_visit` (`client_id`, `last_visit_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 「订阅更新」列表
CREATE TABLE IF NOT EXISTS `tb_acgn_subscribe` (
  `client_id` VARCHAR(64) NOT NULL,
  `media_type` VARCHAR(16) NOT NULL,
  `media_id` INT NOT NULL,
  `title` VARCHAR(512) DEFAULT NULL,
  `pic` VARCHAR(1024) DEFAULT NULL,
  `subscribed_at` BIGINT NOT NULL,
  PRIMARY KEY (`client_id`, `media_type`, `media_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 阅读进度（JSON 载荷，适配番剧集数 / 漫画话数+页 / 小说卷）
CREATE TABLE IF NOT EXISTS `tb_acgn_progress` (
  `client_id` VARCHAR(64) NOT NULL,
  `media_type` VARCHAR(16) NOT NULL,
  `media_id` INT NOT NULL,
  `payload_json` MEDIUMTEXT NOT NULL,
  `updated_at` BIGINT NOT NULL,
  PRIMARY KEY (`client_id`, `media_type`, `media_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
