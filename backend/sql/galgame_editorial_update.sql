-- Editorial ordering and one verified developer correction; no schema changes.
-- Apply after the original seed import. Stable slug keys preserve existing numeric IDs.
SET NAMES utf8mb4;
USE `0721acgn`;
START TRANSACTION;
UPDATE `tb_galgame` SET `sort_order` = CASE `slug`
  WHEN 'atri' THEN 1
  WHEN 'steins-gate' THEN 2
  WHEN 'clannad' THEN 3
  WHEN 'fata-morgana' THEN 4
  WHEN 'gaokao' THEN 5
  WHEN 'mahoyo' THEN 6
  WHEN 'summer-pockets' THEN 7
  WHEN '999' THEN 8
  WHEN 'white-album-2' THEN 9
  WHEN 'narcissu' THEN 10
  WHEN 'rewrite' THEN 11
  WHEN '428' THEN 12
  WHEN 'lucy' THEN 13
  WHEN 'air' THEN 14
  WHEN 'higurashi' THEN 15
  WHEN 'planetarian' THEN 16
  WHEN 'steins-gate-0' THEN 17
  WHEN 'kanon' THEN 18
  WHEN 'umineko' THEN 19
  WHEN 'harmonia' THEN 20
  ELSE `sort_order` END WHERE `slug` IN ('atri', 'steins-gate', 'clannad', 'fata-morgana', 'gaokao', 'mahoyo', 'summer-pockets', '999', 'white-album-2', 'narcissu', 'rewrite', '428', 'lucy', 'air', 'higurashi', 'planetarian', 'steins-gate-0', 'kanon', 'umineko', 'harmonia');
UPDATE `tb_galgame` SET `developer` = 'Frontwing / 枕' WHERE `slug` = 'atri' AND `developer` = 'Frontwing / ANIPLEX.EXE';
COMMIT;
