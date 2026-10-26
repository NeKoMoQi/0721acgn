-- PREPARATION TEMPLATE ONLY. Default is a no-op followed by ROLLBACK.
-- Use a MySQL client configured for UTF-8. Never execute the historical dump.
-- Fill both variables with the actual verified playable URL and its expected prefix.
SET NAMES utf8mb4;
SET @ep3_url = NULL;
SET @expected_prefix = NULL;

START TRANSACTION;

-- Keep these original values in the execution output before making any change.
SELECT id, animation_id, episode_no, title, video_url
FROM tb_animation_episode
WHERE animation_id = 10941 AND episode_no IN (1, 2, 3)
ORDER BY episode_no FOR UPDATE;

SET @trial_ready = (
    @ep3_url IS NOT NULL AND @expected_prefix IS NOT NULL
    AND LEFT(@ep3_url, 8) = 'https://'
    AND LOCATE('/animation-video/10941/003-', @expected_prefix) > 0
    AND LEFT(@ep3_url, CHAR_LENGTH(@expected_prefix)) = @expected_prefix
    AND CHAR_LENGTH(@ep3_url) <= 512
    AND RIGHT(@ep3_url, 4) = '.mp4'
    AND (SELECT COUNT(*) FROM tb_animation WHERE id = 10941 AND name LIKE '%芙莉莲%') = 1
    AND (SELECT COUNT(*) FROM tb_animation_episode WHERE animation_id = 10941 AND episode_no IN (1, 2)) = 2
    AND NOT EXISTS (
        SELECT 1 FROM tb_animation_episode
        WHERE animation_id = 10941 AND episode_no = 3 AND video_url <> @ep3_url
    )
);

-- Add only episode 3. Never overwrite an existing different URL.
INSERT INTO tb_animation_episode (animation_id, episode_no, title, video_url)
SELECT 10941, 3, '第3集', @ep3_url
WHERE @trial_ready = 1
AND NOT EXISTS (
    SELECT 1 FROM tb_animation_episode WHERE animation_id = 10941 AND episode_no = 3
);

-- Fix only the two known damaged titles, leaving both video URLs untouched.
UPDATE tb_animation_episode
SET title = CASE episode_no WHEN 1 THEN '第1集' WHEN 2 THEN '第2集' END
WHERE @trial_ready = 1 AND animation_id = 10941 AND episode_no IN (1, 2)
AND (LOCATE(CONVERT(0xEFBFBD USING utf8mb4), title) > 0 OR LOCATE('?', title) > 0);

SELECT @trial_ready AS trial_ready;
SELECT id, animation_id, episode_no, title, video_url
FROM tb_animation_episode
WHERE animation_id = 10941 AND episode_no IN (1, 2, 3)
ORDER BY episode_no;

-- Inspect a rehearsal first. Change this line to COMMIT only for the real run.
ROLLBACK;
