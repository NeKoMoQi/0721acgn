# 0721ACGN 自动编辑部 v2（服务器部署）

这版按“自己逛平台 → 发现热点 → 读内容 → 写稿配图 → 自动更新网站”运行。公开搜索已经改为可选补充，不再充当平台采集。

## 实际运行方式

1. B站：通过 bilibili-api-python 读取热门、动画/游戏分区榜、关键词最新/播放排序；有登录凭证时增加首页推荐。对候选读取视频信息、字幕与一页热门评论。无字幕或登录失效会明确记录；视频简介不会伪装成字幕。
2. 小黑盒：独立 Chromium 用户目录，正常打开网站信息流和配置的话题页，接收网站自身加载的帖子数据，随后打开具体帖子读取正文和评论。不逆向签名，不绕验证码。只有摘要而没有正文时不能冒充已读全文。
3. 每轮保存播放/浏览、评论、点赞快照，后续按实际时间差算每小时增长；综合总互动、增长与分区榜来源排序。首次采集增长显示样本不足；未获得的指标是 null，绝不伪装成 0。
4. 把原始字幕/帖子和采样评论交给 DeepSeek 写作，社区讨论不要求官网证据。可另外配置官网进行公告核实。
5. 正常社区观察和有官网证据或两个独立可靠来源交叉核实的公告默认自动发布。爆料、模型标为有争议/证据不足的稿件、缺原始来源且不能交叉核实的公告进入待审区。标题与来源永久保留，未知日期留空。
6. 每篇生成真实 PNG 封面；有指标时另配统计图。可以按主题自动选择你提供的本地授权图片。默认是程序原创插画，不是调用绘图大模型，也不搬运社区图片。
7. SQLite 持久化，原子生成 published.json。现有 Spring Boot 读取公开快照与 PNG，Vue 在 `/#/news` 展示；发布后不用逐篇重启服务。

默认：每 30 分钟采集一轮，每天最多生成 5 次稿、100 次 DeepSeek API 请求。API 失败/重试也计数；平台只读采集不消耗 DeepSeek 次数。额度不是人民币账单上限。自动发布不代表内容绝对正确，保留审稿与撤稿命令。

## 先安装依赖

需要 Python 3.11+、联网能力和中文字体，**不需要安装桌面 DSH**。本目录应在网站静态根目录之外，建议 Windows `C:\apps\0721acgn-newsroom`。

```powershell
cd C:\apps\0721acgn-newsroom
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m playwright install chromium
Copy-Item config.example.json config.json
```

Linux 用 `python3 -m venv .venv` 和 `.venv/bin/python`；为 Chromium 安装系统依赖，用 `python -m playwright install --with-deps chromium`。中文字体可使用 Noto Sans CJK，或在配置里指定 `font_file`。

**所有后续命令里的 python 都指安装了上述依赖的同一个 Python。** Windows 应使用虚拟环境的完整路径。

创建本地 `secrets.env`：

```dotenv
DEEPSEEK_API_KEY=在服务器本地填写
```

Key 不要发到聊天或提交仓库。B站凭证和小黑盒浏览器目录也不要放在 frontend/dist，不要上传公开备份。进程环境变量中的 Key 优先于文件。系统证书问题应修复 Python/系统 CA，不要关闭 TLS 验证。

## 登录与真实验证（不扣模型费用）

采集器提供独立诊断，**不需要 DeepSeek Key**，也不会自动发评论、私信或投稿：

```powershell
python platform_cli.py probe bilibili --detail
python platform_cli.py probe xiaoheihe --detail
```

输出 items、实际可读字符数、字幕/评论状态和失败原因。items > 0 只表示发现列表成功，detail 出错仍代表正文没读到，不要把两者混为一谈。

B站需要字幕/推荐时，执行扫码登录：

```powershell
python platform_cli.py login bilibili
```

终端显示二维码，同时在凭证目录旁生成 bili-login.png，使用 B站 App 扫码确认。成功后保存到 `bili_credential_file`。如果已经用 DSH 插件登录，可将此配置指向现有 bili_credential.json（格式兼容）；不要直接把凭证贴进配置或聊天。

小黑盒：

```powershell
python platform_cli.py login xiaoheihe
```

它会打开独立浏览器。你自己完成登录/必要验证，回终端按 Enter 保存会话，然后重新 probe。登录命令只是保存会话，不会冒称凭证已验证。无图形界面的 Linux 服务器，可在**相同 Linux 用户与同一浏览器目录**下，通过可信的远程桌面/X11 会话完成一次登录，再退出图形界面；不要把 Windows 加密的浏览器 Cookie 目录直接搬去 Linux。

登录或手工审稿前可先暂停任务，避免浏览器用户目录或 worker 锁被占用。遇到 401/403/412、验证码、登录失效时查看 status，按网站正常流程恢复；服务不绕过访问控制。

## 启动编辑与自动发布

```powershell
python worker.py check      # 检查配置、Key、依赖、浏览器和字体；不代表已成功登录
python worker.py once       # 真正采集/写稿一轮，可能产生模型费用
python worker.py status     # 平台状态、稿件状态、请求额度与最近一轮结果
python worker.py run        # 常驻
```

v2 示例配置中 `auto_publish=true`。使用 v1 配置升级时，旧配置里的 false 会被保留；想按本版自动更新，请显式改成 true。`official_domains=[]` 可以正常运行社区资讯，不再是启动报错条件。

| 配置 | 含义 |
| --- | --- |
| platforms | bilibili/xiaoheihe，可单独关闭暂不可用的平台 |
| topics | B站站内搜索关键词 |
| interest_keywords | 热门/首页推荐内容的兴趣过滤词 |
| bili_rank_categories | 默认 douga/game 分区榜 |
| bili_credential_file | B站登录凭证；可指向 DSH 现有凭证 |
| xhh_profile_dir | 小黑盒独立持久浏览器会话目录 |
| xhh_feed_urls | 额外的小黑盒兴趣/话题页面，必须为 www.xiaoheihe.cn/app/ 下的 HTTPS 页面 |
| platform_page_size / max_details_per_round | 限制每个列表和每轮深入读取数量，避免整站抓取 |
| platform_request_delay_seconds | B站列表请求间隔，默认 2 秒 |
| interval_seconds | 默认 1800 秒；候选重试/状态循环每分钟检查 |
| auto_publish | 默认 true；争议/爆料/未核实公告仍进入待审区 |
| official_domains | 可选官网域名列表，用于确认 news 类原始资料 |
| verify_with_web | 是否补查。配置官网时定向搜索；未配置时自动搜索相关公告/可靠报道 |
| web_search_enabled | 默认 false；开启后公开网页搜索只是额外来源 |
| max_drafts_per_day / max_api_calls_per_day | 按北京时间重置的持久化生成/请求限额 |
| report_dir | 可选导入旧 DSH 日报 BV 线索；真正处理时仍通过平台采集器读内容 |
| image_library | 本地授权素材列表，见下面示例 |
| font_file | 可选中文字体绝对/相对路径 |

站点热度分数是本服务的候选排序，不是官方全平台热榜或全体玩家意见。跨平台同一事件通过模型 event_key 去重，仍可能漏掉语义近似稿件；同一 URL 只成稿一次，指标会持续更新。

本地素材示例（添加到 image_library，file 相对 config.json）：

```json
[
  {"file":"art/game-original.png", "topics":["游戏","Steam"], "credit":"你的画师署名", "license":"已获本站使用授权"}
]
```

系统会自动选择匹配主题的图；没有匹配就生成原创书本/手柄插画。原始社区图片不自动下载使用。

## Windows 开机长期运行

**必须用完成小黑盒登录的同一个 Windows 账户运行任务。** Chromium Cookie 使用 Windows 账户加密，不能用 Administrator 登录后改成 SYSTEM 跑，否则会话可能读不出来。

先在同一账户安装依赖、浏览器并完成 probe，再以管理员 PowerShell 执行：

```powershell
.\install-windows.ps1 -Python 'C:\apps\0721acgn-newsroom\.venv\Scripts\python.exe'
```

脚本会要求输入该 Windows 账户的计划任务凭证，由 Windows 任务计划程序保存，脚本不会把密码写进文件或日志。任务开机启动、无需保持登录、无运行时限、进程崩溃后一分钟重启。同名任务会拒绝覆盖；升级旧任务应先停止并显式注销，再安装新任务。

```powershell
Get-ScheduledTask -TaskName '0721ACGN Newsroom'
Stop-ScheduledTask -TaskName '0721ACGN Newsroom'
Start-ScheduledTask -TaskName '0721ACGN Newsroom'
```

修改配置后重启任务。Running 仅代表进程在，不代表平台/模型都正常：检查 `worker.py status`、data/health.json 和 data/worker.log。

## 接到现有网站

必须更新前后端源码并构建，不是只复制 Python 文件。此次新增图片接口 `/api/news/image`，只允许访问已发布文章的 cover/stats PNG；待审或撤稿的图片不能通过此接口读出。

后端启动命令追加：

```text
--newsroom.data-dir=C:/apps/0721acgn-newsroom/data
```

按现有部署流程构建 Vue dist 和 Java jar、重启 Java 服务，然后访问 `/#/news`。服务账户要能读取 data/published.json 和 data/media。不要把整个 data 暴露为静态目录。

```powershell
cd 你的项目\frontend
npm run build
cd ..\backend
mvnw.cmd "-Dtest=NewsControllerTests,GuestAccessTests" test
mvnw.cmd -DskipTests package
```

## Linux systemd

示例路径 `/opt/0721acgn/newsroom`，服务用户 acgn。用该用户安装虚拟环境和浏览器，设置统一浏览器缓存路径；同一个用户完成登录后运行服务。

```bash
sudo mkdir -p /opt/0721acgn/newsroom/data /opt/0721acgn/newsroom/browser-cache
sudo chown -R acgn:acgn /opt/0721acgn/newsroom/data /opt/0721acgn/newsroom/browser-cache
# newsroom.service 默认使用虚拟环境 Python：
# /opt/0721acgn/newsroom/.venv/bin/python
# 浏览器安装和服务使用同一个位置：
export PLAYWRIGHT_BROWSERS_PATH=/opt/0721acgn/newsroom/browser-cache
python -m playwright install chromium
sudo cp newsroom.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now newsroom
```

需要先创建 acgn 用户、安装 Chromium 系统依赖与中文字体；上述目录必须存在。Key 文件仅对运行账户可读。Java 账户需要读取发布文件的权限。worker 不开放任何监听端口。

## 审阅、撤稿、备份

```powershell
python worker.py review             # 生成 data/review.html，含本地封面和待审原因
python worker.py approve 稿件ID
python worker.py reject 稿件ID      # 已发布也可撤下
python worker.py retry 候选ID       # 重试 failed/skipped
python worker.py export             # 从数据库重建快照
```

review.html 与 media 一起复制到本机可离线查看，不要把审稿文件放进网站。暂无网页正文编辑器。执行命令若提示 worker busy，等当前采集完成重试，或暂停任务。

数据库自动兼容 v1，添加 score/last_seen 字段与 samples 表。升级前停任务并备份整个 data（包含 SQLite/WAL）、config.json、secrets.env 和平台会话。源码升级不能覆盖这些文件。B站凭证、小黑盒会话、Key 均只保存在服务器本地。

日志轮转最多约 12 MB。候选正文/指标默认保留 90 天，URL 和已生成稿件保留用于去重；定期检查数据库体积和磁盘。公开快照仅展示最近 500 篇。API 失败最多重试三次，候选指数退避；平台整轮失败五分钟后再试，其他平台和已有公开文章继续可用。

## 验证与已知边界

```text
python -m unittest -v test_worker test_platforms
```

这些是模拟 API/平台的自动测试，不替代真实登录与线上长期验证。最近实测、尚未验证项见 VALIDATION.md。

接口库和网站会变化，任何“永久无需维护”的承诺都不可靠。服务保留每个平台的错误与读取状态，不会把未获取到的字幕/正文当成已读，也不会偷偷用公开搜索替换故障的平台。

参考：[dsh-bili-scroll](https://github.com/c1overzz/dsh-bili-scroll)、[bilibili-api-python](https://pypi.org/project/bilibili-api-python/)、[better-XiaoHeiHe](https://github.com/k1m0206/better-XiaoHeiHe)。采集适配器为本项目自写，未复制上述插件代码。

