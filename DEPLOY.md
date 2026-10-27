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

## 已知问题

1. **`frontend/src/view/shengdi/index.vue` 占位页已替换**：2026-10-27 合并圣地巡礼 v2 更新包后，该路径已恢复为完整的地图巡礼页（含 DeepSeek Agent、按天预览、保存行程）。原占位页与受损版本的备份仍在同目录：`index.vue.original-from-github`、`index.vue.repaired-attempt`。
2. **域名 `0721acgn.com` 未完成腾讯云 ICP 备案**：大陆地域对未备案域名在 80/443 上有网关拦截，表现为域名打不开、直接 IP 访问正常。备案通过后即可用域名访问（HTTPS 证书已就绪）。
3. **服务器已启用 HTTPS**（Let's Encrypt，lego 自动续期），无需再前置 Nginx。
