# 百度网盘分集流水线

本包用于已有网盘账号、可用 BaiduPCS-Go 和明确文件清单，不负责搜分享、转存、获取 Cookie、解决验证码或伪造设备认证。`ingest_from_netdisk.py` 从 manifest 指定文件逐集处理，不扫描整个网盘，不新增番剧条目。

## 当前完成与限制

- 默认只预览清单，无网络/数据库操作。
- `--apply --download-only` 只下载、校验和生成检查帧，不连接 MySQL 或 OSS。
- `--apply` 先查现有番剧 ID/名称及集数，存在即跳过，绝不覆盖。
- 下载器必须事先在服务器本地授权。这里不收集、不读取 `.baidu-cookie`，也不将 Cookie 传进命令行。
- 下载器失败会退出；即使客户端错误地返回 0，也必须通过精确字节数及媒体校验。失败停止整轮，不反复自动登录。没有对所有认证失效响应做专门分类，可能显示为外部工具失败或文件缺失，不能宣称已自动诊断所有 403 原因。
- 单进程锁、逐集串行；上传前持久化 UUID，上传成功后保存 URL，入库失败重试复用同一对象。没有数据库和 OSS 的分布式事务，进程竞争或失败时可能遗留未引用对象，但不会覆盖他人已入库的集数。
- 不自动判断字幕语言。manifest 支持 `zh-Hans`（简体）/`zh-Hant`（繁体），仅自动处理已烧入画面的字幕；内封/外挂版本拒绝自动发布，需要单独设计字幕处理。
- 全量解码检查、ffprobe 媒体报告和两张 JPG 检查帧是辅助证据，不代表完整内容人工验收。
- MP4、H.264、8-bit yuv420p、视频不超过 1920×1080、单 AAC 音轨、且 moov 在 mdat 前：直接上传，跳过转码。相同编码而 moov 在后：无损重封装。其它单视频/单音轨文件：单线程转 H.264/AAC、最多 1080p。多音轨/多视频拒绝，避免自动选错配音。
- 上传后核对 OSS 对象大小，再验证公网 HEAD 类型/大小和 206 Range，才写库。这仍不替代站内实际播放检查。
- 脚本不修改 Bucket ACL。源文件与转码文件默认保留。`cleanup_local=true` 时，只在校验及入库成功后删除工作目录内跟踪的这两个媒体文件；不删网盘文件、不递归删目录、不删外部文件。默认先保留到人工播放验收。
- 剩余空间须至少为 `min_free_gb` 加源文件大小的三倍；这是预留估算，无法保证任意异常高码率转码都不会耗尽空间。

## 部署文件

将包内 `scripts` 的文件放入：

```text
C:\Users\Administrator\Desktop\0721acgn\backend\scripts
```

依赖与当前环境相同：requests、oss2、PyMySQL[rsa]，外部需要 ffmpeg、ffprobe、已授权的 BaiduPCS-Go。不要为了这个脚本升级已正常运行的依赖。安装其它包如遇缓存权限问题，沿用服务器已验证的 `PIP_CACHE_DIR=C:\tools\pipcache` 并为 pip 添加 `--no-cache-dir`。

脚本读取服务器 `backend/src/main/resources/application.properties` 的 MySQL/OSS 配置；不需重打 JAR 或重启后端。支持简单单行等号属性和整个值为 `${ENV}` / `${ENV:default}` 的占位符，不支持复杂 Spring 配置合并、Java properties 转义或续行。复杂配置请用既有脚本支持的 `MYSQL_*`、`OSS_*` 环境变量显式提供。

复制 `netdisk-manifest.example.json` 为 `netdisk-manifest.local.json`，修改 `pcs_executable` 为实际程序路径。该客户端是否支持你的账号和下载加速，需要先在同一账号下验证；官方客户端下载成功不代表命令行客户端已经授权。

第 3 集按用户已确认结果填写：繁体、462656944 字节、1470 秒。正式模式会跳过已存在的第 3 集，不重下、不重传。第 4 集默认禁用：必须核实路径、精确字节数、字幕及内容后再启用，不要使用估算的 MB 数或虚构路径。优先简体，缺少简体再选繁体，选择结果需明确写进每个 job。

## 运行

在服务器 PowerShell 中：

```powershell
Set-Location 'C:\Users\Administrator\Desktop\0721acgn\backend\scripts'

# 只预览清单
.\.venv\Scripts\python.exe ingest_from_netdisk.py --manifest netdisk-manifest.local.json

# 正式模式：示例仅含已上线第3集，应只打印 skipped，不产生上传
.\.venv\Scripts\python.exe ingest_from_netdisk.py --manifest netdisk-manifest.local.json --apply

# 对新启用的集数先下载和检查（不连数据库/OSS）
.\.venv\Scripts\python.exe ingest_from_netdisk.py --manifest netdisk-manifest.local.json --apply --download-only
```

`--download-only` 不检查数据库，因此不要对已上线集数使用这个模式来验证跳过。新集的工作目录在 `backend/uploads/netdisk-ingest/<番剧ID>-<集数>/`，包含 `media.json`、`preview-start.jpg`、`preview-middle.jpg`。核对画面/字幕/音轨后，将该 job 的 `subtitles_verified` 改为 true，再执行 `--apply`。

默认不接受某一在途 job 的来源路径/大小/集数等映射变化；只允许人工字幕确认从 false 改为 true。换源需先审查旧状态和文件，不能随便清空整个状态目录。已有同名但大小不符的文件不会自动覆盖或删除，需核实原因后人工处理。

完整入库使用唯一键保护，不覆盖不同 URL，数据库中已有记录比本地 completed 标记优先。改过清单但仍指向已有分集时同样跳过。如果数据库入库成功但进程在记账前退出，下次按已有记录跳过。若仅上传成功，状态保留 URL，下次重试公网验证及入库。

## Windows 计划任务

先手工验证新集一次，再创建任务：

- 运行身份选择 **Administrator**，与本机网盘授权账号相同；不要照搬 qBittorrent 的 SYSTEM 身份。
- 程序填写 `C:\Users\Administrator\Desktop\0721acgn\backend\scripts\.venv\Scripts\python.exe`。
- 参数填写 `"C:\Users\Administrator\Desktop\0721acgn\backend\scripts\ingest_from_netdisk.py" --manifest "C:\Users\Administrator\Desktop\0721acgn\backend\scripts\netdisk-manifest.local.json" --apply`。
- 起始位置填写 scripts 目录，先每 10 分钟触发，并设置已有任务运行时不启动新实例。
- 首次试运行后如有认证或下载失败，禁用后续触发，人工修复后再启用。脚本失败返回 1，但计划任务下一次定时触发仍会再运行；这与“失败立即禁用任务”不同，当前没有自动禁用任务功能。

这份包没有创建服务器计划任务，也没有在服务器执行下载/上传/写库。远程联调由服务器实际输出确认。

## 收尾 SQL

`sql/frieren_titles_only.sql` 仅修复第1/2集的已知乱码，前后列出记录；默认 ROLLBACK，用于先查看演练。确认两集视频 URL 不变后，将最后一行改为 COMMIT 才正式保存。第3集不会重复插入，也不会改动其记录。

## 验证依据

- BaiduPCS-Go 命令：https://github.com/qjfoidnh/BaiduPCS-Go#下载文件目录
- FFmpeg MP4 快速起播：https://ffmpeg.org/ffmpeg-formats.html#mov_002c-mp4_002c-ismv
- 本地测试：`python -m unittest discover -s tests -p "test*netdisk*.py" -v`。
