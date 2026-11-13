# Anitabi 采集器 — 服务器部署说明（Windows / 0721acgn.com）

部署位置：`C:\apps\anitabi-server`（原 Linux 包的 systemd 单元已弃用，改用 Windows 计划任务）。

## 数据流

```
api.anitabi.cn / image.anitabi.cn
        │  crawler.py --ids ids.txt --mode oss
        ▼
data/state.sqlite3        断点/HTTP 缓存（不要删）
data/export-*/            巡礼番剧.csv + 具体点.csv（CURRENT 指向最新一组）
images → 阿里云 OSS  acgn0721/xunli-v2/<sha256>   （公网可读）
        │  import_to_pg.py
        ▼
PostgreSQL  acgn0721_gis.tb_xunli / tb_xunlipoint
            + tb_anitabi_map / tb_anitabi_point_map（新旧 ID 映射）
```

- 图片走本站已有 OSS（bucket `acgn0721`，前缀 `xunli-v2/`，公网基址 `https://acgn0721.oss-cn-hangzhou.aliyuncs.com`），上传成功后删本地文件。
- 上游作品 ID 是 Bangumi subject id；站内 `xunliid` 是自增编号，**不能直接覆盖**。导入器给新作品分配 `MAX(xunliid)+1`，并写进 `tb_anitabi_map`。
- 站内已有的 369 部作品是旧脚本采的、没有源 ID。导入器会按「作品名/日文名完全相同」认领已有作品（`[adopt]` 日志），再按「点名 + 经纬度（5 位小数）」去重点位，因此不会重复建作品、也不会重复插点。
- 每次导入按作品一个 SAVEPOINT，单部失败只跳过该部；整体一个事务，`--dry-run` 用完整流程后回滚。

## 文件

| 文件 | 作用 |
| --- | --- |
| `crawler.py` | 原包，未改动 |
| `ids.txt` | 要采集的 Bangumi 作品 ID，一行一个，`#` 注释 |
| `.env` | OSS / PG 凭据（ACL 只给 SYSTEM + Administrators 读） |
| `import_to_pg.py` | CSV → PostgreSQL（幂等，可 `--dry-run`） |
| `run-daily.ps1` | 计划任务入口：加锁 → 采集 → 导入 → 写日志 |
| `logs/anitabi-YYYYMMDD.log` | 运行日志（保留 120 天） |
| `data/` | 状态与导出，**必须备份**（不删上游已消失的作品/对象） |

## 计划任务

- 名称：`0721ACGN Anitabi Crawler`，SYSTEM 账户，每天 03:30 + 0–30 分钟随机延迟，无执行时长上限，禁止并发（`IgnoreNew`），错过则补跑。
- 手动跑一次：

```powershell
Start-ScheduledTask -TaskName '0721ACGN Anitabi Crawler'
Get-Content C:\apps\anitabi-server\logs\anitabi-$(Get-Date -Format yyyyMMdd).log -Tail 30
Get-ScheduledTaskInfo -TaskName '0721ACGN Anitabi Crawler'   # LastTaskResult
```

- 停用 / 恢复：`Disable-ScheduledTask` / `Enable-ScheduledTask -TaskName '0721ACGN Anitabi Crawler'`。
- 直接跑（调试，前台）：`C:\apps\anitabi-server\.venv\Scripts\python.exe crawler.py --ids ids.txt --mode oss --data data`。
- 只重导不重采：`import_to_pg.py --data data`（或 `--dry-run` 先看统计）。

## 已知问题（2026-11-13 部署时实测）

**api.anitabi.cn 与 image.anitabi.cn 对本机公网 IP `124.220.174.115` 返回 Cloudflare `403 Sorry, you have been blocked`。**

- 首次请求（10:35）还能拿到 200；随后列表类请求与正常 lite 接口全部 403，11:21 再测仍 403（约 50 分钟冷却未恢复）。返回页明确写出 `Your IP: 124.220.174.115`，属于 Cloudflare 的 IP/ASN 级拦截，不是账号问题。
- 排除客户端因素：PowerShell / curl / Python requests / Node fetch（TLS1.3、HTTP/1.1、浏览器 UA）结果一致都是 403；`https://www.cloudflare.com/cdn-cgi/trace` 显示本机 `ip=124.220.174.115 loc=CN colo=LAX`，即 Cloudflare 本身可达，只是 anitabi 的 WAF 规则拦这个来源。
- 原包说明里也写了「遇到 403 请检查服务器访问情况，不绕过、不盲目重试」，所以程序按设计在 401/403/429 时立即停止本轮并退出 1，不做重试风暴。
- 影响：`crawler.py` 一条作品都取不到 → 导出为空 → 导入器无事可做（日志会显示 `export: 0 works, 0 points` / `nothing to import`）。
- 解决方向（任选其一，均需站主/你这边操作）：
  1. 给 anitabi 站主发邮件，附日志里的 Cloudflare Ray ID，申请放行该 IP / ASN；
  2. 换一个不被拦的出口（住宅宽带、境外小机）跑采集，再把 `data/` 或导出 CSV 同步回本机导入；
  3. 在能访问的网络里先手工跑一轮，把 `export-*` 目录拷过来，用 `import_to_pg.py` 入库。

一旦网络恢复，不用改任何配置：计划任务下一轮会自动采集、入库。

## 离线导入（在别的机器采好再拷回来）

如果这台机器一直被 anitabi 拦，可以在能访问的网络里跑同一份 `crawler.py`，把生成的 `export-xxxx` 目录整个拷回来：

```powershell
cd C:\apps\anitabi-server
# 先把 .env 里的库凭据加载进当前进程（同 run-daily.ps1 的读法）
Get-Content .env | Where-Object { $_ -match '=' -and $_ -notmatch '^\s*#' } | ForEach-Object {
    $k, $v = $_ -split '=', 2; Set-Item -Path ("Env:" + $k.Trim()) -Value $v.Trim()
}
.\.venv\Scripts\python.exe import_to_pg.py --export 'D:\传输\export-abc123' --dry-run   # 先看统计
.\.venv\Scripts\python.exe import_to_pg.py --export 'D:\传输\export-abc123'            # 确认后落库
```

`--export` 直接读目录里的 `巡礼番剧.csv` + `具体点.csv`，不需要 `CURRENT`。导入是幂等的，重复执行只会补新增点位。

## 维护

- 加作品：往 `ids.txt` 追加 Bangumi ID 即可（`#` 注释）。
- 七天内采集成功的作品会跳过（`--days 7`）；要强制刷新加 `--force`（仍复用 HTTP/图片缓存）。
- 换 OSS 桶/目录/模式必须换新的 `--data` 目录（`policy.json` 会拦住混用）。
- 不要开 `--original`：原图体积大，站点展示用 h360 足够。
- 上游 CC BY-NC-SA 4.0：站内展示点位时保留 `origin`/`originURL` 来源说明（当前点位表没有这两列，`具体点.csv` 里保留了，后续加列可直接补）。
