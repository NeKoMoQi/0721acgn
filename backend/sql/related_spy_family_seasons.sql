-- 间谍过家家：第一季 ⇄ 第二季（Bangumi 同步脚本里没有 animation→animation，仅靠 seriesKey 时需重启后端新版本）
INSERT IGNORE INTO 0721acgn.tb_related_work (src_type, src_id, dst_type, dst_id) VALUES
  ('animation', 10843, 'animation', 10844),
  ('animation', 10844, 'animation', 10843);
