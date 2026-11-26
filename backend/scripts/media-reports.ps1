param(
    [ValidateSet('Pending', 'Details', 'Resolve')]
    [string]$Action = 'Pending',
    [long]$Id,
    [string]$LoginPath = '0721-reports'
)

$ErrorActionPreference = 'Stop'
if ($Action -ne 'Pending' -and $Id -le 0) {
    throw 'Provide the positive queue id with -Id.'
}

switch ($Action) {
    'Pending' {
        $query = @"
SELECT MAX(id) AS queue_id, media_type, media_id, position_label, reason,
       COUNT(*) AS reports, MAX(created_at) AS last_report
FROM tb_media_report
WHERE resolved_at IS NULL
GROUP BY media_type, media_id, position_label, reason
ORDER BY MAX(created_at) DESC, MAX(id) DESC
LIMIT 100;
"@
    }
    'Details' {
        $query = @"
SELECT r.id, r.created_at, r.detail, r.resolved_at
FROM tb_media_report r
JOIN tb_media_report selected USING (media_type, media_id, position_label, reason)
WHERE selected.id = $Id AND r.id <= $Id
ORDER BY r.id DESC
LIMIT 100;
"@
    }
    'Resolve' {
        $query = @"
UPDATE tb_media_report r
JOIN tb_media_report selected USING (media_type, media_id, position_label, reason)
SET r.resolved_at = CURRENT_TIMESTAMP
WHERE selected.id = $Id AND r.id <= $Id AND r.resolved_at IS NULL;
SELECT ROW_COUNT() AS reports_resolved;
"@
    }
}
& mysql.exe "--login-path=$LoginPath" '--default-character-set=utf8mb4' '--database=0721acgn' "--execute=$query"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
