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
- 采集脚本：`backend/scripts/collect_galgame.py`，说明见 `backend/scripts/GALGAME-INGEST.md`。

```powershell
# 采集并写入本地 MySQL（使用 backend 自带 venv）
backend\scripts\.venv\Scripts\python.exe backend\scripts\collect_galgame.py --apply --refresh
```

前端构建后会写入 `frontend/dist`；本地 `npm run dev` 已通过 Vite proxy 把 `/api`、`/acgn` 转发到 `127.0.0.1:8080`。页面不会在访客打开时抓取外部站点。

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
