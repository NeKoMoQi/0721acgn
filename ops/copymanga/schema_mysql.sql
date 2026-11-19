-- 拷贝漫画自动追更/补库用的辅助表（可重复执行）
CREATE TABLE IF NOT EXISTS tb_copymanga_queue(
  path_word VARCHAR(64) PRIMARY KEY,
  comicid INT NULL,
  title VARCHAR(255) NULL,
  status VARCHAR(32) NULL COMMENT '连载中/完结/未知',
  rank_today INT NULL,
  state VARCHAR(32) NOT NULL DEFAULT 'planned' COMMENT 'planned/crawling/imported/done/failed/skipped',
  chapters_total INT NOT NULL DEFAULT 0,
  chapters_imported INT NOT NULL DEFAULT 0,
  note VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tb_copymanga_daily(
  id INT AUTO_INCREMENT PRIMARY KEY,
  day DATE NOT NULL,
  rank_today INT NULL,
  path_word VARCHAR(64) NOT NULL,
  title VARCHAR(255) NULL,
  action VARCHAR(32) NOT NULL COMMENT 'new_work/fill/update/queued/skipped',
  result VARCHAR(32) NULL COMMENT 'queued/imported/failed/skipped',
  chapters_imported INT NOT NULL DEFAULT 0,
  chapters_remaining INT NOT NULL DEFAULT 0,
  note VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_day_pw(day, path_word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
