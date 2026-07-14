-- ============================================================
-- Update tb_kesk_grid.eleclass with coarse Kursk battlefield terrain.
--
-- Keeps the original hex size and fields. This only updates eleclass.
-- A one-time backup is created as:
--   acgn0721_gis.tb_kesk_grid_eleclass_backup
--
-- Terrain classes used by movement algorithm:
--   steppe
--   rolling_steppe
--   ridge_highland
--   forest_steppe
--   river_valley
--   oboyan_open
--   prokhorovka_open
-- ============================================================

BEGIN;

CREATE TABLE IF NOT EXISTS acgn0721_gis.tb_kesk_grid_eleclass_backup AS
SELECT id, q, r, eleclass, elevation, bigmap
FROM acgn0721_gis.tb_kesk_grid;

WITH classified AS (
    SELECT
        id,
        CASE
            -- Low ground and approximate Seym / Tuskar / Psel valley belts.
            WHEN elevation <= 165
              OR (ST_Y(center) BETWEEN 51.55 AND 51.86 AND ST_X(center) BETWEEN 35.35 AND 36.75)
              OR (ST_X(center) BETWEEN 36.05 AND 36.35 AND ST_Y(center) BETWEEN 51.78 AND 52.30)
              OR (ST_Y(center) BETWEEN 50.94 AND 51.24 AND ST_X(center) BETWEEN 35.55 AND 37.55)
                THEN 'river_valley'

            -- Higher rolling ridges / observation ground.
            WHEN elevation >= 225
                THEN 'ridge_highland'

            -- Northern shoulder: Ponyri / Olkhovatka direction, broken forest-steppe.
            WHEN ST_Y(center) >= 52.05
              OR (ST_X(center) BETWEEN 35.70 AND 36.55 AND ST_Y(center) BETWEEN 51.88 AND 52.22)
                THEN 'forest_steppe'

            -- Southern shoulder around Prokhorovka: open tank country.
            WHEN ST_X(center) BETWEEN 36.35 AND 37.25 AND ST_Y(center) BETWEEN 50.88 AND 51.30
                THEN 'prokhorovka_open'

            -- Oboyan axis and southern-western approach.
            WHEN ST_X(center) BETWEEN 35.65 AND 36.45 AND ST_Y(center) BETWEEN 51.05 AND 51.48
                THEN 'oboyan_open'

            -- Medium-high rolling black-earth steppe/farmland.
            WHEN elevation >= 195
                THEN 'rolling_steppe'

            ELSE 'steppe'
        END AS eleclass
    FROM acgn0721_gis.tb_kesk_grid
    WHERE bigmap = 1
)
UPDATE acgn0721_gis.tb_kesk_grid g
SET eleclass = c.eleclass
FROM classified c
WHERE g.id = c.id;

COMMIT;

-- Check:
-- SELECT eleclass, COUNT(*)
-- FROM acgn0721_gis.tb_kesk_grid
-- GROUP BY eleclass
-- ORDER BY COUNT(*) DESC;
