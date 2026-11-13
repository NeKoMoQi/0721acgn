# 爬虫部署（ops）

两个增量采集器 + 本站入库胶水脚本。生产部署在 `C:\apps\anitabi-server`、`C:\apps\copymanga-server`，
这里存的是**我们写的**那部分（上游给的包在 `C:\anitabi-server-deploy.zip`、`C:\copymanga-server-deploy.zip`，未改动）。

## 文件

- `anitabi/import_to_pg.py` — 导出 CSV → `acgn0721_gis.tb_xunli` / `tb_xunlipoint`，幂等，带 `--dry-run`。
- `anitabi/run-daily.ps1` — 计划任务入口（加锁 → 采集 → 入库 → 日志）。
- `copymanga/run-daily.ps1` — 计划任务入口（加锁 → 预检 → 采集 → 日志）。
- `install-crawler-tasks.ps1` — 注册两个计划任务（SYSTEM，每天 03:30 / 04:30，随机延迟 30 分钟，禁止并发）。

## 部署位置

| 项 | 路径 |
| --- | --- |
| 采集器 | `C:\apps\anitabi-server`、`C:\apps\copymanga-server` |
| 虚拟环境 | 各自 `.venv`（Python 3.13） |
| 数据 / 状态 | 各自 `data\`（SQLite + 导出，必须备份） |
| 日志 | 各自 `logs\<name>-YYYYMMDD.log` |
| 计划任务 | `0721ACGN Anitabi Crawler`、`0721ACGN Copymanga Crawler` |
| 站点库 | PG `acgn0721_gis`（巡礼），MySQL `0721acgn`（漫画，待接） |
| 图片 | OSS `acgn0721`：`xunli-v2/`、`copymanga-v1/` |

部署说明见两个目录里的 `README-DEPLOY.md`（含 2026-11-13 实测的网络问题）。

## 同步方式

改这里的脚本后覆盖到 `C:\apps\...`，再跑一次 `install-crawler-tasks.ps1`（注册是幂等的）。

```powershell
Copy-Item ops\anitabi\*.py,ops\anitabi\*.ps1 C:\apps\anitabi-server\ -Force
Copy-Item ops\copymanga\*.ps1 C:\apps\copymanga-server\ -Force
powershell -File ops\install-crawler-tasks.ps1
```

PowerShell 5.1 读 `.ps1` 依赖 BOM 才能正确识别中文，仓库里的 `.ps1` 是 UTF-8 with BOM。
