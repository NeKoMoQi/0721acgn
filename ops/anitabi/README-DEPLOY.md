# Anitabi 采集器 — 服务器部署说明（Windows / 0721acgn.com）

部署位置：`C:\apps\anitabi-server`（原 Linux 包的 systemd 单元已弃用，改用 Windows 计划任务）。

**2026-11-18 起，数据源从 API 改成官网静态数据集**：`api.anitabi.cn` 对本机出口 IP 返回 Cloudflare 403（详见证证据见文末），但官网 `www.anitabi.cn` 可访问，且它自己用的地图数据集是公开静态文件，包含全部作品、点位名称、经纬度和图片路径。现在每天从这些文件增量入库，不再依赖被拦的 API。

当前站内：**1574 部作品 / 48268 个点位**（原来 369 部 / 8460 点）。

## 数据流

```
https://www.anitabi.cn/d/g.json      作品表（1571 部）
https://www.anitabi.cn/d/g0..g6.json 点位分片（53599 个，含名称/坐标/图片路径）
        │  collect_dump.py --cache data/site-dump --out data/dump-export
        ▼
data/dump-export/export-*/巡礼番剧.csv + 具体点.csv（CURRENT 指向最新一组）
        │  import_to_pg.py --data data/dump-export
        ▼
PostgreSQL  acgn0721_gis.tb_xunli / tb_xunlipoint
            + tb_anitabi_map / tb_anitabi_point_map（源 ID ↔ 站内 xunliid）
```

- 作品 ID 用上游 Bangumi subject id，站内 `xunliid` 是自增编号。新作品分配 `MAX(xunliid)+1`，且**从 `tb_xunlipoint` 的最大 xunliid 起算**，避免把历史孤儿点位挂到新作品上。
- 已有作品按「中文名/日文名完全相同」认领（日志 `[adopt]`）；同名多部（凉宫春日、点兔等）会记 `[skip] ambiguous` 不猜。
- 点位按「经纬度 4 位小数」去重（旧数据坐标只保留 4 位，按 5 位会全变新点）。4 位小数约 11 米，同作品内两个不同点落在同一格的概率可忽略。
- 每次导入按作品一个 SAVEPOINT；整体一个事务；`--dry-run` 全流程后回滚。

## 字段与已知限制

- **没有 `ep` / `time`**：官网静态数据集里没有集数和时间戳，API 的 `points/detail` 现在也只返回 `id/image/name/origin/originURL`。导入后这两列为空，页面不显示「第 X 话 / 时间」徽标。
- **图片是远程 URL，没有复制到 OSS**：
  - 点位图 `https://image.anitabi.cn/user/...`、封面 `https://image.anitabi.cn/bangumi/...`（1018 部）；
  - 另外 551 部封面来自 `http://bgm-api.anitabi.cn/pic/cover/...`。
  - 这些域名对**访客浏览器**可用（住宅网络），但本机出口 IP 被拦，服务器上没法预览。要落地成本地 OSS 副本，用 GitHub Actions 当采集机（已验证 Actions 出口 200）跑一次搬运，见 `ops/github/`。
- 数据集没有 `origin`/`originURL`（大部分点位有 Google Maps 的 mid，导入器已拼成 originURL 保留在 CSV 里，但表里没有这两列）。

## 文件

| 文件 | 作用 |
| --- | --- |
| `collect_dump.py` | 下载官网静态数据集 → CSV（每天自动跑） |
| `import_to_pg.py` | CSV → PostgreSQL（幂等，可 `--dry-run`） |
| `run-daily.ps1` | 计划任务入口：加锁 → 采集 → 导入 → 写日志 |
| `crawler.py` | 旧的 API 采集器（本机 403，留作换出口后备用） |
| `ids.txt` | API 采集用的作品 ID 清单 |
| `.env` | PG / OSS 凭据（ACL 只给 SYSTEM + Administrators 读） |
| `data/site-dump/` | 官网数据集缓存（约 10 MB） |
| `data/dump-export/` | 导出的 CSV，`CURRENT` 指向最新一组 |
| `logs/anitabi-YYYYMMDD.log` | 运行日志（保留 120 天） |

## 计划任务

- 名称：`0721ACGN Anitabi Crawler`，SYSTEM 账户，每天 03:30 + 0–30 分钟随机延迟，无执行时长上限，禁止并发（`IgnoreNew`），错过则补跑。
- 手动跑一次：

```powershell
Start-ScheduledTask -TaskName '0721ACGN Anitabi Crawler'
Get-Content C:\apps\anitabi-server\logs\anitabi-$(Get-Date -Format yyyyMMdd).log -Tail 30
Get-ScheduledTaskInfo -TaskName '0721ACGN Anitabi Crawler'   # LastTaskResult
```

- 手动导入某个导出目录（先看再落库）：

```powershell
cd C:\apps\anitabi-server
.\.venv\Scripts\python.exe import_to_pg.py --export 'D:\path\export-xxxx' --dry-run
.\.venv\Scripts\python.exe import_to_pg.py --export 'D:\path\export-xxxx'
```

## 图片同步（2026-11-18）

服务器上行只有 **0.06 MiB/s**（实测），自己下载/上传图片不现实；`image.anitabi.cn` 又只对访客 IP 放行。做法：

1. GitHub Actions（6 个分片 runner，`ops/anitabi/push_images.py`）下载 50,702 个图片 URL（点位图 + 封面），直传 OSS
   `xunli-v2/images/<sha256(url)>.<ext>`，每个分片把自己的映射写到 `xunli-v2/map-shards/shard-<i>.json`。
2. 服务器 `merge_maps.py` 合并成 `xunli-v2/image-map-final.json`（URL → OSS URL）。
3. 服务器 `rewrite_images.py` 按映射把 PG 里的远程 URL 换成 OSS URL（幂等，只换非 OSS 的）。
4. 每日任务已串起来：采集 → 入库 → merge_maps → rewrite_images。

当前覆盖：

| 项 | 总数 | 已在 OSS | 仍远程 |
| --- | --- | --- | --- |
| 点位图 | 48,268 | 44,006 | 2 |
| 封面 | 1,574 | 1,148 | 426（420 个 image.anitabi + 6 个 bgm.tv） |

- 少部分图片上游返回 404/403，Actions 里记为 fail，不影响其余对象；下次运行会自动重试（分片 map 只记成功的）。
- 首次单 runner 直传在 2 万张后会被限速到 ~2/s，所以改成 6 分片；实测 6 分片约 32 张/秒。

## 回滚

导入产生的作品/点位都能靠映射表精确回滚（不影响原有 369 部）：

```sql
BEGIN;
DELETE FROM acgn0721_gis.tb_xunlipoint WHERE xunliid IN (SELECT xunliid FROM acgn0721_gis.tb_anitabi_map);
DELETE FROM acgn0721_gis.tb_xunli     WHERE xunliid IN (SELECT xunliid FROM acgn0721_gis.tb_anitabi_map);
TRUNCATE acgn0721_gis.tb_anitabi_point_map;
TRUNCATE acgn0721_gis.tb_anitabi_map;
COMMIT;
```

## 为什么不用 API（2026-11-18 排查结论）

- `api.anitabi.cn` / `image.anitabi.cn` 对本机 IP `124.220.174.115` 返回 Cloudflare `403 Sorry, you have been blocked`（页面写明该 IP）。
- 停任务静默 11 分钟后、同作品 10 秒间隔单发仍 403；PowerShell / curl / Python / Node / 真 Chromium（headless Edge）结果一致；主站 `www.anitabi.cn` 同期 200。→ 与脚本、频率、UA 无关，是出口 IP 被站方 WAF 拦。
- 同一时段 GitHub Actions 出口（Azure）访问 API 全部 200，说明采集能力没问题，换出口即可。
- 官网 API 的 `points/detail` 数据也变了：现在只有 `id/image/name/origin/originURL`，没有 `geo/ep/s`。
