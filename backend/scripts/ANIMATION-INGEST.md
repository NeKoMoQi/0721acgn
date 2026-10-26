# 动漫下载与入库

`ingest_animation.py`：配置页/RSS 匹配单集 → 解析详情页磁力或 `.torrent` → qBittorrent 下载 → FFmpeg 转为 H.264/AAC MP4 → 阿里云 OSS 断点上传 → 写入 `tb_animation_episode`。

仅处理你有权下载及上传的内容。本站提供种子不代表授予再分发权。

## 当前验证范围

2026-10-22 已通过 HTTPS 成功读取动漫花园首页 `https://www.dongmanhuayuan.com/` 和一个 `/detail/` 详情页，并用实际页面验证解析：主页提取到 40 个详情链接，详情页能解析输入框/JSON 文本中的磁力链接。相同 BTIH 的 Base32/十六进制写法会合并。搜索表单地址为 `/dosearch/?key=关键词`，但搜索结果尚未联调；RSS 地址尚未确认。网站间歇性超时，不能视为持续可用。没有绕过验证码或登录限制。支持标准 HTML 的 `a[href]`、RSS 2.0 的 item/link/enclosure，以及直接配置磁力/种子 URL。多个不同种子的详情页会拒绝处理，需明确指定直链。

示例里的番剧 ID、正则和播放域名是待修改占位值，直接运行不会匹配番剧。按番剧和发布组精确匹配，**不要用一个宽泛正则抓整个首页**。集数只接受明确的正整数；合集、多视频种子会拒绝入库，避免把字幕、分辨率或季度当成集数。元数据必须已在 `tb_animation` 存在；缺少分集表时先执行 `backend/sql/animation_episode.sql` 的建表部分（不要执行其中的示例视频 INSERT）。本脚本不创建番剧元数据。

## Windows 准备

在脚本目录打开 PowerShell：

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements-animation.txt
Copy-Item animation-ingest.example.json animation-ingest.local.json
```

安装 qBittorrent 并开启 Web UI，监听本机 `127.0.0.1:8080`，设置用户名和密码。安装 FFmpeg 并将其加入 PATH。**脚本与 qBittorrent 必须在同一台机器，使用相同的本地下载路径**；远程下载器或容器路径映射目前不支持。

## 先补已有动漫

当前工作顺序：先审计数据库已有作品及其分集，再为这些 ID 配置补集订阅，不新增作品。本脚本只写分集表，不创建 `tb_animation` 记录。不同番剧/季的匹配规则需依据数据库名称核对后设置，不根据模糊搜索结果自动认领资源。

有真实连接配置时，先只读审计：

```powershell
.\.venv\Scripts\python.exe audit_animation.py --output ../uploads/animation-ingest/live-audit.json
```

没有连接配置时，可对备份生成参考清单（不会执行备份中的 SQL）：

```powershell
python audit_animation.py --dump ../sql/dumps/mysql_0721acgn_full.sql --output ../uploads/animation-ingest/existing-animation-audit.json
```

报告优先列出零分集作品，其次列出已记录集数之间的空缺。完整集数未知时不会宣称补齐；现有视频 URL 是否有效也需要后续检查。备份有部分历史乱码，报告保留原文，不把它重新导入数据库。

编辑 `animation-ingest.local.json`：

- `animation_id`：数据库中对应的番剧 ID，每季单独配置。
- `source_url`：实际可访问的番剧列表页或 RSS。先在浏览器确认地址，当前不保证首页即可匹配。
- `title_regex`：锁定番剧名、发布组、分辨率与单集格式，必须含 `(?P<episode>\d{1,3})`。JSON 中反斜杠写成 `\\`。例如发布标题为 `许可动画 - 01 [1080p]` 时使用 `^许可动画 - (?P<episode>\\d{2}) \\[1080p\\]$`。
- `playback_base_url`：可供前端访问的 OSS 公网域名或 CDN 域名，**只填写域名，不包含目录前缀**。脚本不更改 Bucket 或对象 ACL；先配置目标视频目录可读及播放器所需 CORS。私有 Bucket 需另接后端签名播放接口，不应把短期签名 URL 永久写入分集表。
- `work_dir`：下载、转码和状态目录；相对路径以配置文件目录为基准。保留原始文件和转码文件，需要预留足够磁盘空间。

已知某一集的种子 URL 时，可将一条 subscription 改为：

```json
{
  "animation_id": 10941,
  "episode_no": 1,
  "title": "你确认过的发布标题",
  "seed_url": "magnet:?xt=urn:btih:替换为真实哈希"
}
```

## 凭证与运行

OSS/MySQL 默认读取现有 `backend/src/main/resources/application.properties` 中的普通 `key=value` 配置；不支持 Spring 的 `${...}` 占位符、转义属性或多行属性。环境变量优先：

```powershell
$env:QBIT_USERNAME = 'admin'
$env:QBIT_PASSWORD = '替换为本机WebUI密码'
$env:OSS_REGION = 'cn-hangzhou'
# 如果 application.properties 已有正确的 OSS/MySQL 配置，不需要重复设置以下变量。
$env:OSS_ENDPOINT = 'https://oss-cn-hangzhou.aliyuncs.com'
$env:OSS_BUCKET = '你的Bucket名'
$env:OSS_ACCESS_KEY_ID = '你的AccessKeyId'
$env:OSS_ACCESS_KEY_SECRET = '你的AccessKeySecret'
$env:MYSQL_URL = 'jdbc:mysql://localhost:3306/0721acgn'
$env:MYSQL_USER = '你的数据库用户名'
$env:MYSQL_PASSWORD = '你的数据库密码'

# 先预览匹配结果：仅读取来源，不连接下载器、OSS或数据库
.\.venv\Scripts\python.exe ingest_animation.py --config animation-ingest.local.json

# 持续处理，每5分钟刷新来源、检查下载、上传已完成文件和重试失败项
.\.venv\Scripts\python.exe ingest_animation.py --config animation-ingest.local.json --apply --watch 300
```

OSS 使用 V4 签名，`OSS_REGION` 必须与 Bucket 地域一致。RAM 用户需要目标目录的上传、分片上传与读取对象元数据权限。凭证不要提交到仓库，也不要贴进聊天。

`--apply` 不带 `--watch` 时只处理一轮：新任务加入下载器后立即退出，下次运行再检查进度。未完成当前单集前不会提交下一集。默认低于 8 GiB 空闲空间暂停本轮，可通过 `min_free_gb` 调整；转码使用单线程。退出码 0 表示本轮没有错误，不代表所有下载已完成；失败返回 1。监听模式可按 Ctrl+C 停止。脚本不会自行注册计划任务或后台服务。

同一工作目录有进程锁，防止重复运行。每个番剧/集数仅保留首次发现的任务；相同集的已有数据库记录会跳过，不覆盖。发现的任务、磁力与上传地址保存到工作目录的 `state.json`，来源列表消失后仍会继续处理；上传成功而入库失败时，下次复用上传地址。新 OSS 路径统一为 `<prefix>/animation-video/<animation_id>/<三位集数>-<uuid>.mp4`，UUID 上传前持久化，SHA-256 保存在状态文件，上传后核对长度再写库。失败项下一轮重试，不删除下载、不删除云对象、不自动停止做种。要更换失败任务的种子，应先停止脚本，处理对应下载器任务和下载目录，再删除 `state.json` 对应条目。

页面匹配结果若有误，先停在预览阶段调整规则。转码会消耗 CPU；当前统一转为 H.264/AAC，不烧录外挂字幕。源视频已有内嵌画面字幕会保留。

## 本地验证

```powershell
python -m unittest discover -s tests -p 'test_ingest_animation.py' -v
```

测试使用伪造页面和下载器响应，不访问种子站、不下载视频、不写远程存储。真实站点、qBittorrent、FFmpeg、OSS 和数据库需要完成配置后再联调。

接口依据：[qBittorrent 官方 Web API](https://github.com/qbittorrent/qBittorrent/wiki/WebUI-API-%28qBittorrent-5.0%29)、[阿里云 OSS 断点上传](https://www.alibabacloud.com/help/en/oss/developer-reference/resumable-upload-1)。
