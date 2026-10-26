# 本机接入记录（v2，2026-10-23 更新）

本文件记录这台服务器上 newsroom 的实际状态。README.md 是通用的部署说明，两者互补。

## 路径

| 用途 | 路径 |
| --- | --- |
| 服务源码 | `C:\Users\Administrator\Desktop\0721acgn\newsroom` |
| Python 虚拟环境 | `...\newsroom\.venv`（Python 3.13.7，依赖已装） |
| Chromium（Playwright） | `C:\Users\Administrator\AppData\Local\ms-playwright`（已安装并实测可启动） |
| 运行数据 | `...\newsroom\data`（SQLite、published.json、media、小黑盒浏览器目录） |
| 后端读取 | `newsroom.data-dir=C:/Users/Administrator/Desktop/0721acgn/newsroom/data` |
| 前端静态包 | `...\frontend\dist`（上一版备份 `dist-backup-20261023-1638`） |
| 后端 jar | `...\backend\target\demo4-0.0.1-SNAPSHOT.jar` |

## 这一版接了什么

- 平台直读采集器 `platforms.py` + `platform_cli.py`：B站（热门/分区榜/关键词，读信息、字幕、一页评论）、小黑盒（独立 Chromium 会话读信息流与帖子）。
- 稿件分三类：正常社区观察默认自动发布；爆料/争议/未核实的公告进待审区。
- `media.py` 生成真实 PNG 封面与统计图（本机用微软雅黑），也支持 `image_library` 里你授权过的本地图片。
- 后端新增 `GET /api/news/image`（只服务已发布稿件的 cover/stats PNG，待审和撤稿的读不到）；`/api/news`、`/api/news/byid` 继续游客可读。
- 前端 `/#/news` 支持封面图、统计图、图片加载失败回退、三类稿件标签。

## 已在本机验证

| 项目 | 结果 |
| --- | --- |
| `python -m unittest test_worker test_platforms` | 28 通过 |
| 后端 `NewsControllerTests` + `GuestAccessTests` | 4 + 33 通过 |
| 前端 `vitest run` | 36 通过（含 News 5） |
| `vue-tsc` / `vite build` | 通过，已替换线上 dist |
| `GET /api/news`（经 Web 反代） | 200，空快照 |
| `GET /api/news/image` | 未知 ID 404、非法 role 400 |
| B站实测 | 有真实列表与播放/评论/点赞；字幕 `CredentialNoSessdataException`（未登录），详情只有简介 + 3 条评论 |
| 小黑盒实测 | 10 条信息流、标题/日期/评论数可读；正文需登录（返回登录指引，不把摘要当全文） |
| PNG 生成 | 封面 29.9 KB、统计图 41.2 KB，实测成功 |

## 还差三件事（都需要你本人操作）

1. **DeepSeek Key**：写进 `newsroom\secrets.env` 的 `DEEPSEEK_API_KEY=...`，不要发到聊天或提交。
2. **B站登录**（要字幕/首页推荐才需要）：
   ```powershell
   cd C:\Users\Administrator\Desktop\0721acgn\newsroom
   .\.venv\Scripts\python.exe platform_cli.py login bilibili
   ```
   扫二维码确认，凭证写到 `data/bili_credential.json`。
3. **小黑盒登录**（要正文/评论才需要）：
   ```powershell
   .\.venv\Scripts\python.exe platform_cli.py login xiaoheihe
   ```
   弹出浏览器里自己登录，回终端按 Enter 保存会话到 `data/xiaoheihe-browser`。

之后：

```powershell
.\.venv\Scripts\python.exe worker.py check
.\.venv\Scripts\python.exe worker.py once      # 真跑一轮，会产生模型费用
.\.venv\Scripts\python.exe worker.py status
```

## 开机任务（未安装）

`install-windows.ps1` 会先跑 `check`（没 Key 会直接失败），注册时还会要求输入**当前 Windows 账户的密码**，所以这一步必须你自己执行：

```powershell
cd C:\Users\Administrator\Desktop\0721acgn\newsroom
.\install-windows.ps1 -Python 'C:\Users\Administrator\Desktop\0721acgn\newsroom\.venv\Scripts\python.exe'
```

任务名 `0721ACGN Newsroom`，用完成小黑盒登录的同一个账户运行（Chromium Cookie 用 Windows 账户加密，换 SYSTEM 会读不出来）。

## 排障备注

- 在本会话的 shell 里跑 Playwright 会报 `EPERM mkdtemp`，原因是会话临时目录受限，不是部署问题；计划任务用系统正常 TEMP，不受影响。若手工调试遇到，设 `$env:TEMP`、`$env:TMP` 到可写目录即可。
- 修改配置后重启任务；`Running` 只说明进程在，健康状态看 `worker.py status`、`data/health.json`、`data/worker.log`。
- 回滚前端：把 `dist` 改名，再将 `dist-backup-20261023-1638` 改回 `dist`，重启 `0721ACGN Web`。
