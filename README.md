# 0721ACGN

ACGN WebGIS graduation project, organized for GitHub upload.

## Structure

- `frontend/` - Vue 3 + Vite frontend.
- `backend/` - Spring Boot backend and database scripts.
- `backend/scripts/` - Python data synchronization scripts.

## Local setup

Frontend:

```bash
cd frontend
npm install
npm run dev
```

Backend:

```bash
cd backend
copy src\main\resources\application.example.properties src\main\resources\application.properties
mvnw.cmd spring-boot:run
```

Fill `backend/src/main/resources/application.properties` with your local database, OSS, and agent credentials before running the backend.

## Galgame 板块

- 页面：`/#/Galagame`（列表与推荐轮播）、`/#/GalgameIntroduce?galgameid=1`（详情）。
- 接口：`GET /api/galgame`、`GET /api/galgame/hot`、`GET /api/galgame/byid?id=`。
- 数据表：`tb_galgame`，应用启动时由 `GalgameMysqlSchemaInitializer` 自动建表。
- 首批 20 部真实作品资料，结构化字段来自 VNDB，中文名/背景参考萌娘百科；Bangumi 在采集环境连接超时，未使用其数据。
- v6 内容更新：20 篇中文导读、51 条角色介绍、87 张官方 Steam 画面预览、18 条官方 PV 入口、6 张人物图、4 个专题、19 部作品的中国区价格快照（2026-10-27）与试玩入口；列表/详情使用选定蓝天四人背景；白色相簿2改用官网看图入口并隐藏未核实评分。
- 采集脚本：`backend/scripts/collect_galgame.py`，说明见 `backend/scripts/GALGAME-INGEST.md`。
- v6 内容校验：`node backend/scripts/verify_galgame_content.mjs .`

```powershell
# 采集并写入本地 MySQL（使用 backend 自带 venv）
backend\scripts\.venv\Scripts\python.exe backend\scripts\collect_galgame.py --apply --refresh
```

前端构建后会写入 `frontend/dist`；本地 `npm run dev` 已通过 Vite proxy 把 `/api`、`/acgn` 转发到 `127.0.0.1:8080`。页面不会在访客打开时抓取外部站点。

## 圣地巡礼 Agent（v2）

- 页面：`/#/shengdi`，地图巡礼 + 猫娘向导。路线查询与日常闲聊两种模式，回复按天预览，确认后才把当天交给地图算路径。
- 角色卡：`backend/src/main/resources/agent/character.md`；知识库：`backend/src/main/resources/agent/knowledge/`（5 份，带来源与更新时间）。
- 接口：`POST /acgn/live2d/agent/stream`（SSE）、`GET|POST /api/xunli/agent/routes`（账号行程，需登录 token）、`POST /acgn/live2d/agent/chat`（旧 Live2D bundle 兼容）。
- 数据：沿用现有 PostgreSQL，在原库执行 `backend/sql/pilgrimage_agent_v2.sql` 新增 4 张辅助表；`tb_xunli` / `tb_xunlipoint` 不变。
- 配置：环境变量 `DEEPSEEK_API_KEY`，可选 `live2d.agent.assets-dir` 外置角色卡与知识目录。详见 [部署说明](DEPLOY.md)。
- 前端商户搜索使用 `VITE_FOURSQUARE_API_KEY`，放在未提交的 `frontend/.env.local`，构建时注入；未配置时地图与站内巡礼仍可用，商户搜索降级。

## 游客与登录

- 游客可访问首页、搜索、作品列表与详情、观看与阅读页面、历史与收藏，以及巡礼浏览入口。
- 消息、个人中心、我的动态、投稿、添加巡礼点和战棋页面需要登录。站内点击时保留原页，登录成功后进入目标页；取消后继续浏览。直接打开受保护链接时先显示首页和登录框。
- 保留原有登录界面的布局、图片、样式以及扫码、手机登录入口。密码登录连接现有接口；尚未接通的手机登录操作仅提示暂未开放。
- 刷新时读取本地 token 并恢复用户资料；后端明确返回未登录时才清理凭证。网络故障不会清除登录态，退出登录即使遇到断网也会清理本机凭证。
- 现有历史、收藏、进度及匿名互动仍按浏览器设备标识保存；本次没有新增账号间或跨设备同步。
- 后端 Filter 与 MVC 拦截器共用 `PublicAccessPolicy`。公开浏览接口按路径和 HTTP 方法放行；上传、导入和战局修改仍需有效 token。未登录统一返回 HTTP 401 与 `NOT_LOGIN`。

回归验证（无需启动数据库）：

```bash
cd frontend
npm run test:unit -- --run
npm run build
cd ../backend
mvnw.cmd -Dtest=GuestAccessTests test
```

圣地巡礼 v2 的相关测试：

```powershell
cd backend
$env:JAVA_HOME='C:\tools\jdk17\jdk-17.0.2'
C:\tools\maven\apache-maven-3.9.9\bin\mvn.cmd -o -Dtest="GuestAccessTests,NewsControllerTests,PilgrimageAgentTests,DeepSeekClientTests,AgentAssetsTests,PilgrimageLibraryTests,PilgrimageDatabaseTests,Live2dAgentControllerTests,PilgrimageRouteControllerTests" test
# 可选：在隔离的临时 PostgreSQL 库上跑迁移 + MyBatis 读写
# mvn -Dtest=PilgrimageDatabaseTests -Dpilgrimage.test.jdbc=jdbc:postgresql://127.0.0.1:5432/pilgrimage_it -Dpilgrimage.test.user=postgres -Dpilgrimage.test.password=*** test
```
