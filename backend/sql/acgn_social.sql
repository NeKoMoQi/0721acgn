-- ACGN：点赞计数 + 访客投票 + 评论（访客 ID 来自前端 Header X-Visitor-Id）

USE `0721acgn`;

CREATE TABLE IF NOT EXISTS `tb_acgn_engagement` (
  `target_type` VARCHAR(16) NOT NULL COMMENT 'animation|comic|novel',
  `target_id` INT NOT NULL,
  `likes` INT NOT NULL DEFAULT 0,
  `dislikes` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`target_type`, `target_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `tb_acgn_vote` (
  `visitor` VARCHAR(64) NOT NULL,
  `target_type` VARCHAR(16) NOT NULL,
  `target_id` INT NOT NULL,
  `vote` TINYINT NOT NULL COMMENT '1 up, -1 down',
  UNIQUE KEY `uk_visitor_target` (`visitor`, `target_type`, `target_id`),
  INDEX `idx_target` (`target_type`, `target_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `tb_acgn_comment` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `target_type` VARCHAR(16) NOT NULL,
  `target_id` INT NOT NULL,
  `nick` VARCHAR(32) NOT NULL,
  `body` VARCHAR(512) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_target_created` (`target_type`, `target_id`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
