# 芙莉莲第 3 集：准备包

本包不代表已经在服务器部署、下载或写库。目标固定为第一季 `animation_id=10941, episode_no=3`；不创建番剧条目，不扩展到第 4 集或第二季。

## 来源与字幕

2026-10-22 读取动漫花园搜索结果时，找到候选发布：

- `[桜都字幕组] 葬送的芙莉莲 / Sousou no Frieren [03][1080p][简体内嵌]`
- 详情页：<https://www.dongmanhuayuan.com/detail/DQ55x.html>

搜索结果同时有第二季、合集和内封版本，所以这里固定单个第一季发布页，不做模糊自动选择。该详情页本次请求超时，尚未核实种子元数据、实际文件大小、存活节点、视频编码或字幕内容。实际采集遇到超时会停止，不能把页面标题当作已验证字幕。换源时需核对作品/季度/第 3 集并优先简中内嵌。

本试跑只接受标题明确标注简中内嵌的发布，不处理内封/外挂字幕，因此转码不会丢失已经烧在画面中的字幕。真正下载后应人工检查画面、字幕和音轨；目前通用采集器不具备内封字幕烧录功能。

## 上传与命名

现有后端接口：

- `POST /api/animation/episode-video/local`：`animationId=10941`、`episodeNo=3`、`title=第3集`、`sourcePath=服务器视频绝对路径`。
- `POST /api/animation/episode-video`：上述三个字段加 multipart `file`。

两者都需要登录，都会同时上传并写入分集表，现有实现还会覆盖同一集，因此本试跑不调用它们。现有 JAR 上传路径是随机 UUID，不能仅靠请求参数指定 ID/集数目录。

`ingest_animation.py` 直接使用 OSS SDK，按现有后端配置上传，不需要重打 JAR 或重启网站。新上传路径统一为：

```text
<aliyun.oss.folder-prefix>/animation-video/10941/003-<uuid>.mp4
例如 anime-reality/animation-video/10941/003-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.mp4
```

UUID 在首次上传前持久化，失败重试复用路径；上传后检查 OSS 对象长度，成功才写入第 3 集。旧的第 1、2 集视频不移动、不改地址。采集状态记录发布标题、文件 SHA-256、UUID 路径及播放 URL，数据库标题统一为 `第3集`。

## 准备部署（尚不要执行批量下载）

将以下文件放到服务器后端的 `scripts` 目录：`ingest_animation.py`、`requirements-animation.txt`、`frieren-ep3.example.json`。后端路径为 `C:\Users\Administrator\Desktop\0721acgn\backend`。它会在服务器读取现有 `src\main\resources\application.properties`，不会把凭证带回本地。

需要安装 qBittorrent 和 FFmpeg。qBittorrent Web UI 只监听本机，示例用 **8081**，避免占用后端常用的 8080。设好用户名/密码后通过当前进程环境变量 `QBIT_USERNAME`、`QBIT_PASSWORD` 传入。脚本不会打开公网端口或修改 Bucket 权限。

复制 `frieren-ep3.example.json` 为 `frieren-ep3.local.json`，将 `playback_base_url` 改为已有可读 OSS/CDN 域名（不含目录前缀），确认 OSS_REGION 与 Bucket 一致。环境配置和运行步骤见 `ANIMATION-INGEST.md`。

预览命令（不连接下载器/OSS/数据库）：

```powershell
python ingest_animation.py --config frieren-ep3.local.json
```

仅在工具、播放域名、源和凭证核对完成后，用 `--apply --watch 300` 进行单集试跑。该配置只含一集；未完成任务时不会继续提交下一集。低于 8 GiB 空闲会暂停本轮，转码使用单线程。原始视频和转码文件仍保留，避免误删；本次一集验证后再安排清理，不要宣称已有自动清理功能。

## 标题修复及 SQL 模板

`frieren_ep3_trial.sql` 提供另一条手工入库路径，必须先有实际上传并验证可播放的 URL。填写 `@ep3_url` 及 `@expected_prefix`，默认 NULL 不会修改记录，结尾默认 `ROLLBACK` 用于演练。核对后才将末行改为 `COMMIT` 正式执行。

如果采集器已经插入第 3 集，模板填入相同 URL 时会跳过插入，只修复第 1、2 集含 U+FFFD 或问号的坏标题，分别改为 `第1集`、`第2集`。如果第 3 集已存在不同 URL，则整组操作不执行。执行前会输出这三集的原始记录供备份核对。

不要在刚通过 HTTP 上传接口入库后，再用不同 URL 重复执行模板；不要运行旧 dump 中的演示 INSERT。SQL 尚未在远程数据库执行验证，模板不会自动修复线上标题。

验收：前端第 3 集能加载、拖动播放、有声音、中文字幕显示正确；第 1、2 集 URL 保持原样；最后确认标题修复成功。再考虑第 4–28 集。
