-- 动漫试看/PV 视频地址（OSS 外链或站内上传后的 URL）
ALTER TABLE 0721acgn.tb_animation
  ADD COLUMN preview_video_url VARCHAR(512) NULL
  COMMENT '试看 PV 视频地址'
  AFTER bcimg;

-- 示例：《间谍过家家》若库中 id 不同，请按需改 WHERE 条件
UPDATE 0721acgn.tb_animation
SET preview_video_url = 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4'
WHERE name LIKE '%间谍过家家%'
LIMIT 1;
