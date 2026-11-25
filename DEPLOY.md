# 0721ACGN 部署说明（Windows Server 2025 / 10.0.0.7）

## 访问地址

- 站点：**<https://0721acgn.com/>**（`www.0721acgn.com` 同样有效，HTTP 自动 301 跳 HTTPS）
- 公网 IP：`124.220.174.115`（腾讯云），内网 `10.0.0.7`
- 前端静态文件由 Node 服务在 80/443 端口提供，`/api`、`/acgn/live2d` 反向代理到本机 Spring Boot（127.0.0.1:8080，仅本机监听）

## HTTPS / 证书

- 证书：Let's Encrypt（lego 5.5.2，HTTP-01 webroot 验证），覆盖 `0721acgn.com` + `www.0721acgn.com`，90 天有效
- 证书文件：`C:\apps\0721acgn-server\certs\fullchain.pem` + `privkey.pem`（服务启动时自动加载，没有证书就退化为纯 HTTP）
- 续期：计划任务 **`0721ACGN Cert Renew`**，每周日 04:00 跑 `C:\apps\0721acgn-server\renew-cert.ps1`，剩余 30 天内自动续，证书有变化才重启 Web 任务
- ACME 验证目录：`C:\apps\0721acgn-server\acme\.well-known\acme-challenge\`
- 手动续期：`powershell -File C:\apps\0721acgn-server\renew-cert.ps1`

## 运行组件

| 组件 | 版本 | 位置 | 启动方式 |
| --- | --- | --- | --- |
| Node.js | 22.14.0 | `C:\tools\nodejs` | 计划任务 `0721ACGN Web` |
| JDK | 17.0.2 | `C:\tools\jdk17\jdk-17.0.2` | 计划任务 `0721ACGN API` |
| Maven | 3.9.9 | `C:\tools\maven\apache-maven-3.9.9` | 构建用 |
| MySQL | 8.0.29 | `C:\mysql`（数据 `C:\mysql\data`） | 服务 `MySQL0721`（自动） |
| PostgreSQL | 16.4 | `C:\pgsql`（数据 `C:\pgsql\data`） | 服务 `PostgreSQL16`（自动） |
| PostGIS | 3.5 | 装进 `C:\pgsql` | 随 PG |

计划任务均为「开机启动、SYSTEM 账户」，无用户登录也会跑。

## 关键路径

- 前端源码：`C:\Users\Administrator\Desktop\0721acgn\frontend`
- 前端产物：`...\frontend\dist`
- 后端源码 / jar：`...\backend` / `...\backend\target\demo4-0.0.1-SNAPSHOT.jar`
- 后端配置：`...\backend\src\main\resources\application.properties`（打进了 jar）
- 静态服务 + 反代脚本：`C:\apps\0721acgn-server\server.js`
- 启动脚本：`start-web.cmd` / `start-api.cmd`
- 日志：`C:\apps\0721acgn-server\logs\web.log`、`api.log`，PG 日志 `C:\pgsql\pg.log`

## 数据库

- MySQL：库 `0721acgn`，账号 `root` / `<本地配置的密码>`，端口 3306
- PostgreSQL：库 `postgres`，schema `acgn0721_gis`，账号 `postgres` / `<本地配置的密码>`，端口 5432
- 数据来自仓库里的两个 dump，经过修复后导入（修复脚本见 `C:\tools\fix-dump.js`）

## 常用运维

```powershell
# 重启 Web / API
Restart-ScheduledTask -TaskName '0721ACGN Web'
Restart-ScheduledTask -TaskName '0721ACGN API'
# 或
Stop-ScheduledTask -TaskName '0721ACGN API'; Start-ScheduledTask -TaskName '0721ACGN API'

# 重启数据库
Restart-Service MySQL0721
Restart-Service PostgreSQL16

# 前端改动后重新构建
cd C:\Users\Administrator\Desktop\0721acgn\frontend
$env:Path = 'C:\tools\nodejs;' + $env:Path
npm run build-only      # 产物写入 dist，无需重启（静态文件实时生效）

# 后端改动后重新打包 + 重启
cd C:\Users\Administrator\Desktop\0721acgn\backend
$env:JAVA_HOME='C:\tools\jdk17\jdk-17.0.2'
C:\tools\maven\apache-maven-3.9.9\bin\mvn.cmd -DskipTests package
Restart-ScheduledTask -TaskName '0721ACGN API'
```

打包前**先停 API**，否则 Windows 会锁住 `target\demo4-0.0.1-SNAPSHOT.jar`，`repackage` 直接失败并留下一个几 MB 的空壳 jar。

## 服务自愈（2026-11-04）

**11/3 那次全站宕机的真实原因**：`0721ACGN Web` / `0721ACGN API` 两个计划任务沿用了 Windows 默认的 `ExecutionTimeLimit = PT72H`，也就是"最多跑 72 小时就停"。10/31 21:16 开机启动，72 小时后 11/3 21:16 被系统按规则终止（`LastTaskResult = 267014`，日志末尾是 `^C`）。同期 `0721ACGN Newsroom` 设的是 `PT0S`，所以只有它没倒。

已做的加固：

1. `0721ACGN Web` / `0721ACGN API` / `0721ACGN qBit` 改为 `ExecutionTimeLimit = PT0S`（不限时长）+ `RestartCount = 999` / `RestartInterval = PT1M`（进程崩了自动重启）。
2. 新增计划任务 **`0721ACGN Watchdog`**：每 5 分钟跑 `C:\apps\0721acgn-server\watchdog.ps1`，443 或 8080 没在监听就把对应任务拉起来，日志写 `logs\watchdog.log`。

```powershell
# 看门狗最近做了什么
Get-Content C:\apps\0721acgn-server\logs\watchdog.log -Tail 20

# 按需暂停/恢复自愈
New-Item -ItemType File C:\apps\0721acgn-server\maintenance.flag   # 维护中，看门狗不插手
Remove-Item C:\apps\0721acgn-server\maintenance.flag              # 恢复
```

**长时间维护（重建后端、换数据库）前先建 `maintenance.flag`**，否则 5 分钟内看门狗会把你停掉的服又拉起来，可能撞上正在写 jar 的 Maven。


## 数据来源（已用 CSV 备份重灌）

`C:\数据库\数据库\*.csv` 是干净的 UTF-8 备份，已全部导入 MySQL，覆盖 14 张表、共 15.35 万行：

| 表 | 行数 | 来源 |
| --- | --- | --- |
| tb_animation | 2159 | CSV |
| tb_cartoonchapters | 5190 | CSV |
| tb_cartoonpage | 145337 | CSV |
| tb_cartoons | 39 | CSV |
| tb_novel | 22 | CSV |
| tb_novelchapter | 404 | CSV |
| tb_acgnuser | 367 | CSV |
| tb_activity / tb_dept / tb_emp / tb_leibie / tb_people / tb_shengdi / tb_user | 10/5/19/8/1/10/1 | CSV |

- CSV 里没有的列（`tb_animation.genre`、`tb_cartoons.genre/releasedate/rating`、`tb_novel.genre/status/releasedate/rating` 等）按 id 从原数据保留。
- `tb_xunli` / `tb_xunlipoint` 属于 PostGIS，来自仓库的 PG dump（本来就是干净的）。
- 导入脚本：`C:\tools\import-csv.js`（可重复执行，先 TRUNCATE 再插入）。
- 部分列因 CSV 内容比原 varchar(254) 长，已放宽为 TEXT：`tb_acgnuser.talent`、`tb_activity.activitydescribe`、`tb_animation.longcomment`、`tb_animation.voicer`、`tb_novel.longcomment`。
- 登录账号随真实数据变回 **`moqi` / `123456`**。

没有 CSV 备份、仍沿用修复后 dump 的表：`tb_acgn_comment`、`tb_acgn_library`、`tb_acgn_progress`、`tb_acgn_subscribe`、`tb_acgn_vote`、`tb_animation_episode`、`tb_related_work`（这几张本来就是用户本地行为数据，量很小）。

## 部署时做过的修改

1. `frontend/src/utils/request.ts`、`live2d-loader.ts`、`view/shengdi/index.vue` 原文件带 GBK/UTF-8 乱码（GitHub 上的版本在转码时被破坏），做了反向还原并补回被吃掉的引号/换行/标签。
2. API 地址从硬编码 `http://localhost:8080` 改为同源相对路径 `/api`、`/acgn/...`。
3. `vite.config.ts`：`target: 'esnext'`、`minify: false`（esbuild 在本机删除临时文件时报 Access denied，关闭压缩规避；vendor 包因此是 5MB 未压缩）。
4. `backend/src/main/resources/application.properties`：按本机 MySQL/PostgreSQL 配置；阿里云 OSS（bucket `acgn0721`）与百度千帆 AppBuilder（应用 ID + API Key）已填入真实密钥并验证通过。
5. **CORS 修复**（公网访问登录报 403 的根因）：
   - `config/WebConfig.java` 原来只放行 `http://localhost:*` / `http://127.0.0.1:*`，浏览器从公网 IP/域名访问时会带 `Origin` 头，Spring 判定为非法 CORS 请求直接返回 `403 Invalid CORS request`。
   - `config/CorsConfig.java` 又用了 `allowedOrigins("*") + allowCredentials(true)`，这在 Spring 6 下是非法组合，会抛 `IllegalArgumentException`（表现为登录 500）。
   - 现在两处 Spring CORS 配置都置空，统一由 `filter/DemoFilter.java` 回显 `Origin` 输出 CORS 头。

## 圣地巡礼 Agent v2（2026-10-27 合并）

从百度千帆 AppBuilder 换成站内后端直连 DeepSeek 的工具 Agent，并补上角色卡、知识检索、账号行程与会话恢复。沿用现有 Java 17 / Spring Boot 3 / Vue，没有新增生产依赖，也没有另建数据库。

- 角色卡：`backend/src/main/resources/agent/character.md`（猫娘口吻、称呼「主人」、适量「喵~」）；路线与闲聊都实际加载。可用 `live2d.agent.assets-dir` 指向外部完整资源目录，30 秒内热加载。
- 知识库：`backend/src/main/resources/agent/knowledge/index.json` + 5 份 Markdown（行程编排、复拍礼仪、交通边界、资料核验、多作品主题），每篇带来源与更新时间；按问题检索，前端显示「本次检索参考」。
- 数据库增量：在**现有 PostgreSQL 业务库**执行 `backend/sql/pilgrimage_agent_v2.sql`，只新增 4 张辅助表（`tb_pilgrimage_alias`、`tb_pilgrimage_spot_info`、`tb_pilgrimage_route`、`tb_pilgrimage_session`），原 `tb_xunli` / `tb_xunlipoint` 不动。脚本可重复执行，不清数据。
- 行程接口：`/api/xunli/agent/routes`（GET 列表 / POST 保存 / GET `{id}` 读取 / DELETE 删除），要求登录 `token`，按账号隔离。
- 流式接口：`POST /acgn/live2d/agent/stream`（SSE）；`server.js` 反代需保持流式，不要缓冲整段响应。
- 配置：设置环境变量 `DEEPSEEK_API_KEY`，可选 `live2d.agent.deepseek.base-url`（默认 `https://api.deepseek.com`）、`live2d.agent.deepseek.model`（默认 `deepseek-flash`）。旧的 `live2d.agent.appbuilder.*` 已不再使用。
- 注意：SSE 依赖 Servlet 异步，`DemoFilter` 必须 `@WebFilter(..., asyncSupported = true)`，否则流式接口会 500。

上线步骤：备份源码与私有配置 → 执行增量 SQL → `mvn -DskipTests package` 并重启 API → `npm run build-only` 刷新 dist → 检查代理未缓冲 SSE。未执行 SQL 时保存/读取行程会明确提示数据库未就绪，不会静默失败。

本地验证（2026-10-27）：后端定向测试 77 项（76 通过 + 1 项默认跳过的 PostgreSQL 集成测试，该测试已在隔离临时库上单独跑通）、前端 43 项单测全通过；类型检查与生产构建通过；Chromium/Edge 1440×900 与 390×844 实际加载、切换模式、发送、停止/新对话检查通过（未配置密钥时如实提示，不返回假路线）。

### 推荐理由补丁（2026-10-28）

- 后端：`validator` 接受 `submit_route` 里按天地点的 `spotReasons[{pointId,reason}]`（每天最多 6 条、正文 1～240 字），校验归属/重复/空/超长后写进 `spots[].reason`；`pilgrimage-tools.json` 同步声明该字段。保存与读取复用同一校验，理由随 JSONB 行程保留，缺字段的旧行程照样能读。
- 前端：路线卡直接显示当天主题与每点推荐理由；收到核验后的行程就自动在地图展示第一天，切换天数即换线；AI 路线隐藏逐步转向说明，保留真实道路与标记；卡片可收起，图片可放大，点名称定位。
- 验证：后端 79 项（78 通过 + 1 跳过）；前端类型检查/构建通过；线上 1440×900 实跑两轮，得到「宇治川东岸·久美子椅与喜撰桥慢拍」一天三点，理由与 `note` 正常显示、地图自动画线；保存后读取 `spotReasons`/`reason`/`note` 均保留。
- 注意：模型偶尔会给出不合规的 `spotReasons`（例如把当天以外的点写进理由），校验器会整版退回让模型重试，5 轮仍不合规则提示「本次规划未能通过校验，请减少城市或天数后重试」。实测约每三次出现一次，重发即可。

## 首屏性能修复（2026-10-28）

线上实测（服务器侧、绕公网 IP）：首屏关键资源 1.08 MB（br），其中 `vendor.js` 一个包就 1.03 MB、下载 2.55 秒，其余资源都在 20 ms 内；`index.html` 里还同步引了 `api.mapbox.com` 的 directions CSS/JS，CSS 阻塞渲染、TTFB 实测 2.6 秒。三者叠加就是「一直都很慢」的来源。改动：

1. **`index.html` 删掉 api.mapbox.com 的 CSS+JS**：`src/view/shengdi/index.vue` 本来就 `import` 了 npm 包 `@mapbox/mapbox-gl-directions` 的 js/css，那两行是从 CDN 重复拉的第二份（且 CSS 阻塞首屏）。删掉后首屏少 2.6 s 外网等待和 287 KB。
2. **`vite.config.ts` 去掉 `manualChunks`**：原来把所有 `node_modules` 塞进单个 `vendor`，导致只在懒加载路由里用的 mapbox-gl / leaflet / epubjs 也被入口静态 import。改成交给 Rollup 按引用图切分后，入口只剩 vue + element-plus，mapbox 单独成 chunk、只在战旗/圣地页加载。首屏关键资源 1.08 MB → **0.39 MB（br）**，关键路径 2554 ms → **~100–240 ms**。
3. **`server.js` 非 hash 静态资源缓存 5 分钟 → 30 天**：`LOGO.png`(53KB)、`behindpic.webp`(84KB)、`live2d-render.bundle.js`(244KB)、`live2dcubismcore.min.js`(202KB)、`models/` 原来都是 `max-age=300`，回访 5 分钟后全量重下。现在是 `max-age=2592000, stale-while-revalidate=86400`。**换图/换模型要改文件名或加 `?v=`**，否则老用户 30 天内看不到新版本。
4. `src/App.vue` 看板娘改为 `requestIdleCallback`（不支持则 1.2 s 定时）后再挂载，不再和首屏抢带宽/主线程。

改动前备份：`.tmp/perf-20261028-115200/`（index.html、vite.config.ts、App.vue、server.js）。回滚就拷回去 + `npm run build-only` + 重启 `0721ACGN Web`。

## 性能优化（2026-10-21）

| 项目 | 优化前 | 优化后 |
| --- | --- | --- |
| 首屏 JS+CSS（brotli） | ~5.6 MB | **826 KB** |
| vendor.js | 5.16 MB | 3.0 MB（terser）→ **790 KB（brotli）** |
| vendor.css | 375 KB | **52 KB（brotli）** |
| 接口 JSON（26 KB 响应） | 26 KB | **11 KB（brotli）** |
| OSS 图片 | 原图 | 按需缩放（列表 700×dpr，阅读页 1600），实测 311 KB→155 KB、156 KB→12.8 KB |
| 本地大图 | PNG/JPG 原图 | 166 张转 WebP（94.4 MB→21.4 MB），服务器按 `Accept` 协商：`wt.png` 7.97 MB→**234 KB**、`yygz.png` 4.49 MB→**324 KB**、`TSG.png` 1.15 MB→**98 KB** |

具体做了：

1. `server.js` 加 brotli/gzip 压缩（带内存缓存），静态文件和 `/api` 代理响应都压。
2. 前端构建从 `minify: false` 改成 **terser**（esbuild 压缩在本机会报 Access denied）。
3. 开启 **HTTP/2**（`start-web.cmd` 里 `set HTTP2=1`，`allowHTTP1: true` 兼容老客户端）。
4. `index.html` 去掉 **Google Fonts**（国内基本拉不到，会阻塞首屏）和 404 的 `SuperMap.Include.js`，mapbox directions 改 `defer`。
5. `src/utils/imageOptimizer.ts`：运行时给 OSS 图片自动加 `x-oss-process=image/resize...`（只处理 `<img>`、只处理图片扩展名，不碰视频）。
6. 缓存策略：`index.html` 用 `no-cache`，`/js`、`/assets` 下带 hash 的文件 `immutable` 一年。
7. **本地图片 WebP**：`C:\tools\webp-convert.js` 把 `frontend/public` 下 166 张 >120KB 的 PNG/JPG 转成同名 `.webp`（跳过 `models/` 里的 Live2D 贴图）；`server.js` 按 `Accept: image/webp` 协商返回，URL 不变、无需改代码。
8. **代码分割（尝试后回退）**：试过按 vue / element-plus / mapbox / live2d / geo 拆 chunk，但 `mapbox-gl` 与 `vendor` 之间因 commonjs helper 形成环形依赖，拆完后 `mapbox` 仍会被首页 `modulepreload`，首屏反而多一个请求（852KB vs 826KB），故保持单 `vendor`。要真正生效需把 `view/WarFlag/composables/useMapbox.ts` 里的 mapbox 改成动态 `import()`。

## 爬虫部署（2026-11-13）

两个增量采集器已装在服务器上，用 Windows 计划任务每天跑（原包是 Linux systemd，已按本机改造）：

| 采集器 | 路径 | 计划任务 | 产出 |
| --- | --- | --- | --- |
| anitabi（圣地巡礼） | `C:\apps\anitabi-server` | `0721ACGN Anitabi Crawler`，每天 03:30 + 0–30 min | PG `acgn0721_gis.tb_xunli/tb_xunlipoint`（数据源：官网 `/d/*.json` 静态数据集） |
| 拷贝漫画 | `C:\apps\copymanga-server` | `0721ACGN Copymanga Crawler`，每天 04:30 + 0–30 min | OSS `copymanga-v1/` + `data/comics/*`（尚待接 MySQL） |

- 胶水脚本（入库器、任务入口、安装脚本）在仓库 `ops/`，部署细节见 `C:\apps\anitabi-server\README-DEPLOY.md`、`C:\apps\copymanga-server\README-DEPLOY.md`。
- **2026-11-18 更新**：`api.anitabi.cn` 对本机 IP 返回 Cloudflare 403（排查结论：出口 IP 被站方 WAF 拦，与脚本/频率/UA 无关；GitHub Actions 出口实测 200）。改用官网公开静态数据集 `https://www.anitabi.cn/d/g*.json`（1571 部、53599 点位，含名称/坐标/图片路径），由 `collect_dump.py` 导出 CSV、`import_to_pg.py` 入库。站内已从 369 部 / 8460 点扩到 **1572 部 / 50450 点**（去重后；上游数据集 1571 部、53599 条记录，去重后 44122 个唯一点位，老库另有独有点位）。
- 入库规则：新作品分配 `MAX(xunliid)+1`（并从 `tb_xunlipoint` 的最大 xunliid 起算，避开历史孤儿点位）；已有作品按同名认领；点位按经纬度 4 位小数去重；幂等、可 `--dry-run`。**没有改动旧 `xunliid`**，回滚 SQL 见 `ops/anitabi/README-DEPLOY.md`。
- 已知缺失：静态数据集没有 `ep` / `time`（集数、时间戳）。图片已同步到 OSS（点位图 44,006/48,268、封面 1,148/1,574，其余少量上游 404），方法见 `ops/anitabi/README-DEPLOY.md`。
- 拷贝漫画所有已知域名（`2026copy.com`、`copymanga.*`、`mangacopy.com` 等）在本机不可达，`www.copymanga.cc` 是域名停放页；`comics.txt` 先留空，任务记 `WAITING` 后跳过。

```powershell
# 手动跑一轮 + 看日志
Start-ScheduledTask -TaskName '0721ACGN Anitabi Crawler'
Get-Content C:\apps\anitabi-server\logs\anitabi-$(Get-Date -Format yyyyMMdd).log -Tail 30
Get-ScheduledTaskInfo -TaskName '0721ACGN Anitabi Crawler'
```

## 巡礼图片 CDN（2026-11-19）

自有 OSS 图片对外统一走 `https://img.0721acgn.com`（阿里云 CDN，回源 `acgn0721.oss-cn-hangzhou.aliyuncs.com`）。

- 后端 `util/ImageCdnRewriter.java`：只匹配完整 OSS 主机名，保留路径与查询参数；`image.anitabi.cn` 等外链、已经是 CDN 的链接、带 `Signature`/`OSSAccessKeyId` 的签名链接原样返回。转换点在 `XunliServiceImpl` 的所有返回值（列表/详情/搜索/视野/点位/picker），**数据库仍是原始 OSS 链接**。
- 前端 `utils/imageOptimizer.ts`：识别主机名 `acgn0721.oss-cn-hangzhou.aliyuncs.com` 与 `img.0721acgn.com`；已有 `x-oss-process=` 不重复追加，签名链接不加参数。
- 部署：后端 `mvn -DskipTests package` 后重启 `0721ACGN API`；前端 `npm run build-only`（dist 立即生效，无需重启 Web）。
- 验证（2026-11-19）：`/api/xunli/byid?Xunliid=1742`（蓝色时期）封面返回 CDN；`/api/xunli/point/byid?Xunliid=233` 里 Anitabi 外链（id 51321）保持 `image.anitabi.cn`；列表/视野/picker 接口已无 OSS 残留；CDN 图重复请求 `200` + `X-Cache: HIT`；前端运行时给 CDN 图追加了一次 `x-oss-process`，对已带参数、外链、签名链接不改写。

## 拷贝漫画采集（2026-11-19 重写）

原 v3 API 爬虫对现在的 mangacopy.com 无效（该域名已换成 mangashot 前端：`/api/v3` 全部 404，章节图由混淆 JS 动态加载）。参考 `muzi-xiaoren/kaobei_Crawler` 的做法改为**无头浏览器渲染**：

- `ops/copymanga/crawl_selenium.py`：GitHub Actions 里用 headless Chrome 打开漫画页 → 取章节列表 → 逐章按空格触发懒加载 → 读 `img[data-src]` → 下载图片 → 上传 OSS `copymanga/images/<path_word>/<chapter_uuid>/<页码>.<ext>` → 写 manifest `copymanga/manifests/<path_word>.json`（含每章页码与 OSS URL，可断点续跑）。
- `ops/copymanga/import_mysql.py`：服务器读 manifest → `tb_cartoons/tb_cartoonchapters/tb_cartoonpage`，映射表 `tb_copymanga_map` / `tb_copymanga_chapter_map` 保证幂等；`pageorder` 用四位补零。
- `.github/workflows/copymanga-crawl.yml`：手动输入 path_word 与本次章节数；需要 OSS Secrets（可选 `COPY_USER`/`COPY_PASS` 登录）。
- 实测（2026-11-19）：`HAIBIANZHIYE`《海邊之夜》共 103 章，先抓第 1 章 39 页 → OSS 39 张 → 入库 `comicid=40`、`chapterid=390088`，`/api/cartoon*` 三个接口均正常返回 OSS 图片。

注意：服务器仍访问不了该站，采集只能在 Actions 跑；服务器只做入库。

## 拷贝漫画自动选品/追更（2026-11-19）

流水线（服务器只做状态与入库，采集全部在 Actions）：

1. **04:00 服务器** `0721ACGN Copymanga State` → `run-daily.ps1 -Mode state` → 导出 `copymanga/state.json`（已入库 path_word、队列状态、站内标题别名）。
2. **04:30 Actions** `copymanga-daily`：
   - `scan_rank.py` 扫排行榜（当前 `/rank` 50 条，写 `copymanga/rank/latest.json`）；
   - `pick_daily.py` 从榜首往下，跳过已入库/队列中的作品，按标题别名判断是新建还是补库；每天最多 5 部新作品，另外把队列里「连载中」和「完结未补齐」的作品列为追更；
   - `crawl_plan.py` 逐部调 `crawl_selenium.py`，每部每天最多 20 章（分批），图片直传 OSS，manifest 断点续跑。
3. **07:30 服务器** `0721ACGN Copymanga Import` → `run-daily.ps1 -Mode import` → 读 manifest 入库，更新 `tb_copymanga_queue`、`tb_copymanga_daily`、`tb_cartoons` 的连载状态/章节数，然后刷新 state。

表：

- `tb_copymanga_queue`：path_word、comicid、title、status、rank_today、state(planned/crawling/imported/done)、chapters_total/imported。
- `tb_copymanga_daily`：day、rank_today、path_word、title、action(new/fill/update)、result、chapters_imported、chapters_remaining —— 当天选了谁、补位到谁、还差多少话都在这查。
- 建表脚本 `ops/copymanga/schema_mysql.sql`（导入器会自动建）。

去重：`tb_copymanga_map` 按来源 path_word 唯一；`pick_daily` 还会把候选标题与 `tb_cartoons` 现有标题归一化比对，命中则走补库（fill），不会换个译名重复建条目。

已验证（2026-11-19）：

- 排行榜扫描 50 条；选品跳过已在队列的第 1 名，取第 2 名 + 2 部追更；
- `naoodngxueshenghui`《腦洞學生會》采集第 1 章并入库：comicid=41、status=连载中、1/222 话；
- 《海邊之夜》追更第 2 章并入库（完结作品进入补齐队列，剩 101 话）；
- 两个服务器任务实测 exit 0。

**旧作匹配（2026-11-19 晚）**：站点搜索可用路由是 `/search?q=`（首页搜索框在 headless 下不跳转）。按「搜索结果前 3 个 → 打开作品页比对 h6 标题」匹配，再用章节数做合理性校验：

- 40 部里 26 部自动匹配成功（另手工确认《香格里拉边境》1 部，共 27 部写入 `tb_copymanga_map` 与补库队列）；
- 3 部因源章节数远小于站内（出包王女 Darkness 4 vs 77、租借女友 16 vs 301、总之就是非常可爱 42 vs 313）判为疑似错配，未入库；
- 10 部搜索无结果（异世界舅舅、堀与宫村、转生史莱姆、狂赌之渊、别当欧尼酱了、谭雅战记、关于我在无意间被隔壁的天使变成废柴、转生成为魔剑了、孤独摇滚、无职转生），需人工给 path_word。
- 已匹配的 27 部合计缺约 990 话，进入「完结未补齐 / 连载中」队列，每日最多 12 部 × 每部 20 话补库。
- 2026-11-19 首轮补库后：**完结作品全部移出每日队列**（state=done，不再自动更新）。
- 2026-11-19 16:xx 一次性补齐 3 部完结作品：迷宮干 149/149、海邊之夜 103/103、我獨自升級 197/201（132、188、189、190 四话上游章节页取不到图，重试两次仍失败，按上游缺失处理）。导入器的跳过规则从「话数 ≤ 已导入最大值」改成「按话数集合判断」，以后中间缺章也能补。
- 每日追更改为按 `updated_at` 轮转（最久没更新的优先），避免固定 12 部长期占位。
- **匹配补全（2026-11-19 晚）**：加简繁/异体变体搜索 + `ops/copymanga/aliases.tsv` 别名表后，39/40 部已匹配。只剩《出包王女 Darkness》未定（源站搜到的是 4 话同名短篇，需人工给正确链接）。
  - 用户点名的三部：堀与宫村 → `kuyugongcun`、异世界舅舅 → `yishijieshushu`（繁体「異世界叔叔」）、史莱姆 → `gywzshcwslmnjs`（繁体「關於我轉生後成為史萊姆的那件事」）。
  - 另外补上：狂赌之渊、别当欧尼酱了、转生成为魔剑了、租借女友（源 452 话）、孤独摇滚、无职转生。
  - 完结作品在 seed 时直接 `state=done`，不进每日追更队列。

## 已知问题


1. **`frontend/src/view/shengdi/index.vue` 占位页已替换**：2026-10-27 合并圣地巡礼 v2 更新包后，该路径已恢复为完整的地图巡礼页（含 DeepSeek Agent、按天预览、保存行程）。原占位页与受损版本的备份仍在同目录：`index.vue.original-from-github`、`index.vue.repaired-attempt`。
2. **域名 `0721acgn.com` 未完成腾讯云 ICP 备案**：大陆地域对未备案域名在 80/443 上有网关拦截，表现为域名打不开、直接 IP 访问正常。备案通过后即可用域名访问（HTTPS 证书已就绪）。
3. **服务器已启用 HTTPS**（Let's Encrypt，lego 自动续期），无需再前置 Nginx。

## 前端自动发布（2026-11-20）

`scripts\auto-deploy-frontend.ps1` 定时检查 GitHub 上 `frontend/` 的变化，有更新就自动拉取、构建并替换 `dist`（静态文件，不需要重启 Web）。

- 计划任务：**`0721ACGN AutoDeploy Frontend`**，开机启动 + 每 5 分钟一次，以 Administrator（交互式）运行
- 状态与日志：`.deploy\frontend-state.json`、`.deploy\deploy.log`（都在 .gitignore 里）
- 行为：
  - 只认 `frontend/` 下的改动；只有 `backend/`、`newsroom/` 或采集数据变化时只记日志并提示手动部署
  - 工作区不干净或有未推送提交时直接跳过，不会 reset / 覆盖本地改动
  - `package-lock.json` 变了会先 `npm install`
  - 构建前备份 `dist`，构建失败或没有 `dist/index.html` 时自动还原；`dist-backup-*` 只留最近 3 份
- 手动跑一次：`powershell -NoProfile -ExecutionPolicy Bypass -File scripts\auto-deploy-frontend.ps1`
- 只看会做什么：加 `-DryRun`；强制重建：加 `-Force`
- 停用：`Disable-ScheduledTask -TaskName '0721ACGN AutoDeploy Frontend'`

## 轻之国度榜单爬虫（2026-11-25）

从 `lightnovel.fun` 的「综合热度」榜按名次顺序检查，每天最多新增 5 本、已有书继续补卷；整卷正文打成纯文字 EPUB 传 OSS，再写 `tb_novel` / `tb_novelchapter`。北京时间跨天。

| 项目 | 值 |
| --- | --- |
| 代码 | `C:\apps\0721acgn-server\novel-crawler\app`（原包 + 2026-11-25 卷数补丁；旧版留 `crawl_lk_rank.py.bak-20261125-volcount`） |
| venv | `C:\apps\0721acgn-server\novel-crawler\.venv`（Python 3.13.7 x64） |
| 榜单配置 | `C:\apps\0721acgn-server\novel-crawler\lk-rank.json` |
| 受限凭据 | `C:\apps\0721acgn-server\config\novel-crawler.properties.local`（ACL 只留 SYSTEM / Administrators 可读） |
| 日志 | `C:\apps\0721acgn-server\novel-crawler\logs\crawler.log`（轮转 2 MB × 5） |
| EPUB 暂存 | `C:\apps\0721acgn-server\novel-crawler\epubs` |
| 计划任务 | **`0721ACGN LightNovel Ranking`**，每天 03:30（服务器本地时间），SYSTEM，`IgnoreNew`，时限 4 h |

- MySQL 用专用账号 `novel_crawler`@`localhost`，只对 `tb_novel`、`tb_novelchapter`、`tb_novel_lk_source` 有 `SELECT/INSERT/UPDATE`；表由 `app\lk-rank-schema.sql` 建（`tb_novel_lk_source` 记源站卷 → 站内卷映射、指纹和当天新增日期）。
- OSS 走 bucket `acgn0721` 的 `novel-crawler/` 前缀，写完会自己按站内匿名地址回读校验（需该前缀可匿名读，与 `novel/` 一致）；阅读仍走 `/api/novel/content?id=&no=`。
- 榜单页 `/rank` 只渲染 30 条、无公开分页（前端 `items.slice(0,30)`），所以 `additional_rank_pages` 留空。
- `existing_book_ids` 需要显式绑定，因为源站作者名和库里不一致（位置保护按名次对齐，库里的旧卷不动）：
  `1338→3`（败犬女主太多了，源站作者 `雨森たきび` vs 库里 `雨森焚火`）、`5143→16`（义妹生活，`三河 ごーすと` vs `三河ごーすと`）、`2409→1`（欢迎来到实力至上主义的教室，`衣笠彰梧×トモセシュンサク` vs `衣笠彰梧`）。
- 首轮实跑（2026-11-25 10:56–11:54）：榜 30 条全部检查，新增 5 本（`不迷途的羔羊`、`请别来管我…`、`奇幻探险者`、`我想成為影之強者！`、`自稱賢者弟子的賢者`），更新 3 本已有作品（novelid 1/3/16 补卷），`tb_novel` 22→27、`tb_novelchapter` 404→454。用同一任务以 SYSTEM 重跑一次，全部 `no readable changes`、退出码 0，确认幂等。
- 已知问题：
  1. **`tb_novel.chaptercount` 已修（2026-11-25）**：原来爬虫给新书写的是「源站章节总数」（`自稱賢者弟子的賢者` 24 卷写成 522），站点旧数据里这字段是卷数。已按修复包把 `publish_book` 改成 `len(history)`，并用 `lk-rank-backfill-volume-count.sql` 回填（脚本可重复执行，重复跑 0 行）。
     - 第一遍按包里的 `WHERE s.imported_on IS NOT NULL` 只动了爬虫建的作品：23→10、24→3、25→4、26→4、27→24（5 行）。
     - 然后把该条件去掉，改为「凡 `tb_novel_lk_source` 收录的作品都按 `tb_novelchapter` 实际卷数对齐」再跑一次：只影响 1 行，`败犬女主太多了`（novelid 3）8→**13**，详情页「现已更新至13章」与 13 条卷记录一致。原始 SQL 留在同目录 `lk-rank-backfill-volume-count.sql.orig`。
     - 注意 **代码只给爬虫新建的作品自动同步这个字段**，旧作补卷后不会自己变（旧的 `imported_on` 判断是对的：`len(history)` 只数源站卷，像 novelid 1 源站 24 卷、站内 32 卷，无脑覆盖会把 32 改成 24）。所以旧作新增卷之后，重跑一次上面这条幂等 SQL 即可对齐。
  2. `败犬女主太多了`（novelid 3）补到的第 11 卷标题也是「SSS短篇集」，与库里原有第 8 卷重复 —— 库里的旧卷是「第一~七卷 + SSS短篇集」，按名次对齐后源站「8卷/8.5卷/作者访谈/第九卷」都补上了，但短篇集多出一份。删卷会破坏章节号连续，爬虫下一轮会判整本异常，故保留（修复包也要求保留）。
  3. 源站受限/空正文的卷会跳过并记 `WARNING`：本次 `1207`（理想的圣女）第 42254 卷空正文、`3457`（我想成為影之強者！）第 7630 卷含受限章节。
  4. `歡迎來到實力至上主義的教室 2年級篇`（源站 1166）内容与 novelid 1 已有的二年级篇重复，但 `tb_novel_lk_source.novelid` 唯一键只允许绑一条，已把主系列 2409 绑到 1。若哪天队列排到 1166，它会作为新作品再建一份，需要人工处理。

```powershell
# 手动跑一轮 / 看任务状态
Start-ScheduledTask -TaskName '0721ACGN LightNovel Ranking'
Get-Content C:\apps\0721acgn-server\novel-crawler\logs\crawler.log -Tail 30
Get-ScheduledTaskInfo -TaskName '0721ACGN LightNovel Ranking'

# 停用 / 长期停用
Disable-ScheduledTask -TaskName '0721ACGN LightNovel Ranking'
```