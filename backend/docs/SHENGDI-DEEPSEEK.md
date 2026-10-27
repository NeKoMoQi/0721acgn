# 圣地巡礼 DeepSeek Agent

2026-10-27 v2：从百度千帆 AppBuilder 迁移为站内后端管理的工具 Agent，补齐独立猫娘角色卡、知识检索、账号行程和会话持久化。沿用 Java 17 / Spring Boot 3 和 Vue，无新增生产依赖。继续使用现有 PostgreSQL，在原库中执行增量表迁移，不另建业务数据库。

## v2 的角色卡、知识库和数据库

- 角色卡：`src/main/resources/agent/character.md`。以用户给定的猫娘提示词为基准，称呼「主人」、每段适量使用「喵~」，补充巡礼职责、风险表达、如实汇报与不虚构操作。路线与闲聊都实际加载此文件；工具、访问权限与校验边界仍由后端决定。
- 知识库：`src/main/resources/agent/knowledge/index.json` 加 5 份 Markdown，覆盖行程编排、复拍礼仪、交通边界、地点核验与多作品主题。每篇带 ID、标签、来源和更新时间。根据问题自动检索最多 3 篇，也允许模型调用 `search_knowledge` 补查最多 5 篇；前端显示「本次检索参考」。这些是站内编辑规范，未冒充实时旅行资讯或场所官方资料。
- 原地点数据：继续查询 `acgn0721_gis.tb_xunli` / `tb_xunlipoint`，不重建、清空或复制一套地点库。
- 新增表：在同一个库、同一个 schema 执行 `sql/pilgrimage_agent_v2.sql`。`tb_pilgrimage_alias` 保存作品/城市别名，`tb_pilgrimage_spot_info` 补充访问状态/拍摄备注/来源/核验时间，`tb_pilgrimage_route` 保存账号行程，`tb_pilgrimage_session` 保存登录用户的短期会话。SQL 可重复执行，别名种子不覆盖已有记录。
- 账号行程：前端提供「保存行程」「我的行程」及读取、删除入口。后端从已验证 JWT 取账号 ID，不信任正文中的 owner；游客可规划，保存与读取需登录。重复保存同一快照会更新名称而不是不断新增。列表返回最近 50 条。
- 恢复与校验：保存和读取行程时，都重新读取目录中的地点坐标/图片与访问状态；失效、关闭或禁入点会导致明确失败，不静默删点。读取只恢复预览，不自动导航；继续修改时将精简地点 ID 上下文重新交给 Agent 校验。
- 会话：登录用户最近 4 轮对话与上一版行程会写入原 PostgreSQL；服务重启后，携带原 conversationId 可恢复。匿名设备和旧 bundle 的会话保留在内存，不伪装成账号同步。账号切换会清除前端旧会话与行程展示。数据库会话使用版本检查，拒绝并发静默覆盖。

### 编辑角色卡与知识

默认从打包资源加载，修改后重新构建后端。也可把整个 `agent/` 目录（其中的 `character.md` 和 `knowledge/`）复制到服务器可读目录，然后配置：

```properties
live2d.agent.assets-dir=C:/apps/0721acgn-agent
```

资源每 30 秒重新检查；外部目录必须包含完整角色卡和知识目录。内容有格式错误会明确报错，不悄悄换回旧角色。新增知识时在 `index.json` 登记条目，再放入对应 Markdown；不要将未经核实的营业时间写成确定事实。角色卡示例中的「已保存」不能替代真实的保存接口结果。

### 维护地点核验信息

`tb_pilgrimage_spot_info.point_id` 对应现有 `tb_xunlipoint.id`，不重复保存坐标。`access_status` 支持 `unknown` / `public` / `restricted` / `closed`；默认未知，后两者不会进入可导航候选。填写 `source_url`、`photo_note`、`verified_at` 时应使用实际核验材料，无法确认则保留 unknown 和空核验时间。目录缓存最多 60 秒，不承诺实时状态。

没有自动开放的资料编辑后台；资料由服务器文件和现有数据库维护流程管理。会话 30 分钟到期后不可恢复，过期行可由现有运维流程执行 `DELETE FROM acgn0721_gis.tb_pilgrimage_session WHERE expires_at <= now()` 清理；本次没有创建定时任务。

## 工作逻辑

1. 前端 POST `/acgn/live2d/agent/stream`，携带 `query`、`mode`（`route` / `chat`）和可选的 `conversationId`。
2. 后端负责可信提示词和会话，直接调用 DeepSeek Chat Completions，开启流式、明确关闭深度思考。客户端传来的 `systemHint` 不会成为系统指令。
3. 路线模式下模型调用 `search_spots`：把自然语言拆成作品、城市与地点短词，检索站内具体取景点；不知道收录情况时先查目录。关键词为或匹配，城市额外过滤。没有结果不塞随机景点。
4. 模型调用 `submit_route`，提交完整的分天地点 ID、主题、推荐理由及出行方式。模型不生成图片链接或坐标。
5. 后端检查候选范围、坐标、每天点数、重复地点、明显不合理的长距离移动；失败则把错误返回模型修正。校验成功后从站内数据补齐名称、图片和坐标。
6. 前端展示按天卡片。切换天数只改变预览；点击导入后，仅把选中当天交给现有 Mapbox 服务算实际路径。单地点行程只定位，不伪造路径。
7. 用户可继续说“第二天少走一点”。后端传入上一版已验证行程供修改，重新校验完整的新版本。路线和闲聊拥有不同会话。

通常是“检索一次 → 提交一次”两次模型请求，无额外长篇总结请求。复杂检索或校验修正会增加轮次，最多 5 次模型请求。速度需要部署后测量，未给出实际首字延迟或加速倍数。

## 配置

在运行后端的进程环境设置 `DEEPSEEK_API_KEY`，然后重启后端。密钥不放前端、不提交源码。

可选配置（见 `application.example.properties`，只合并以下设置，不覆盖数据库/OSS 配置）：

```properties
live2d.agent.deepseek.api-key=${DEEPSEEK_API_KEY:}
live2d.agent.deepseek.base-url=https://api.deepseek.com
live2d.agent.deepseek.model=deepseek-flash
live2d.agent.assets-dir=
```

`deepseek-flash` 为本次核对官方文档后的默认模型，可改为账号可用的其他 DeepSeek 模型。base-url 不含 `/chat/completions`，代码会追加。旧的 `live2d.agent.appbuilder.*` 不再使用，可从私有配置中移除。

Windows 计划任务部署时，环境变量必须对运行后端的任务账号/进程有效；在另一个终端临时设置不会自动传给已有任务。也可以在服务器私有 `application.properties` 设置 API key，勿加入更新包。

官方协议依据：[Chat Completions](https://api-docs.deepseek.com/api/create-chat-completion/)、[工具调用](https://api-docs.deepseek.com/guides/tool_calls/)。

## 接口与前端

流式事件均是 SSE 命名事件，`data` 为 JSON：

| 事件 | 内容 |
| --- | --- |
| `meta` | `conversationId` |
| `status` | `text`：理解需求、检索、校验、修正的实际阶段 |
| `delta` | `text`：可显示的回复片段，不包含工具参数和推理内容 |
| `done` | `reply`、`conversationId`、`knowledgeSources`、可选 `routePlan` |
| `error` | `message`：失败原因；前端保留上一版行程，不导入半成品 |

每 10 秒心跳注释。前端支持停止生成、连接中断检测和 100 秒等待上限。后端连接超时 8 秒、单次读取超时 20 秒、任务检查期限 90 秒、SSE 超时 95 秒。取消会关闭正在使用的上游连接；数据库检索设置 10 秒语句超时。

代理需保持流式转发，不能将整个响应读完再返回。后端发送 `X-Accel-Buffering: no` 和 `Cache-Control: no-cache, no-transform`。现有 Node 反代若使用 `upstream.pipe(response)` 可直接转发；若使用 Nginx，对本路径关闭 `proxy_buffering`，读取超时建议大于 100 秒，并避免 gzip 合并输出。旧 `/acgn/live2d/agent/chat` 仍返回原 `Result` 包装，供 Live2D bundle 兼容调用，同样走 DeepSeek。

账号行程接口为 `/api/xunli/agent/routes`：GET 列表、POST `{title,plan}` 保存、GET `/{id}` 读取、DELETE `/{id}` 删除；均需原站点的有效 `token` 请求头，并按账号隔离。流式对话通过 `token` 或 `X-Acgn-Client` 绑定会话归属；身份只由服务器识别，不进入模型上下文。

`routePlan.version = 2`，核心结构：

```json
{
  "intent": "shengdi_route_plan",
  "version": 2,
  "travelMode": "walking",
  "days": [{
    "day": 1,
    "title": "当天主题",
    "note": "推荐理由",
    "pointIds": ["数据库地点ID"],
    "spots": [{"id": "数据库地点ID", "name": "真实名称", "coordinates": [135.8, 34.9], "imageUrl": "站内图片"}],
    "straightLineKm": 0,
    "warnings": ["实际路程以地图结果为准"]
  }]
}
```

前端不再从聊天正文提取 JSON、按模糊地名猜点或把作品 ID 扩展成若干随机点；也不再合并多天、静默截断为前 25 点。完整手动导入列表只在打开其面板时加载，AI 不依赖前端下载全站点位。

## 规则与范围

- 只允许已检索/上一版仍存在的具体点 ID。不把缺少具体名称的作品级替代名称当作地点。数据库记录真实性仍取决于站内资料质量。
- 每版 1～10 天，每天 1～6 点；同一天重复坐标会被拒绝，不同天可重复到访。
- 相邻点直线距离超过步行 20 km / 骑行 80 km / 驾车 600 km 时拒绝该版并请求修正；当天直线合计较长时显示提示。这是异常筛查，不是路线优化器或体力保证。
- 支持步行、骑行、驾车。尚未实现电车/公交实时规划，也不把直线距离当实际路程/耗时。不调用联网搜索核实营业时间。
- 尚未接入任意酒店/车站地理编码，行程可导航点以站内收录为限；地图已有手动起终点导航仍可使用。
- 地点目录及别名/核验信息缓存 60 秒；每次检索最多返回 32 点，单次任务最多保留 128 个候选；目录最多展示 100 个作品/城市组合。全站源查询沿用现有 28000 点上限，缺失资料需完善录入或查询策略。
- 会话保留最近 4 轮对话及上一版行程，30 分钟空闲过期，进程内上限 256 个。账号会话可从数据库恢复；匿名会话重启会丢失。多实例同时修改同一会话会触发版本冲突，尚未实现分布式排队锁。
- 同一会话不并发处理。全局最多 4 个模型任务；流式入口另有 8 个排队位置，过载会明确提示。未启用自动重试，避免重复花费和进一步加长等待。
- 未配置密钥、上游失败或无候选时均明确报错/澄清，不返回假路线。上游错误体和密钥不会返回客户端。

## 验证与部署

本地使用模拟 DeepSeek、模拟巡礼点和模拟地图服务验证，不会调用真实付费 API：

- 后端：地点检索/别名/访问状态、角色卡实际加载、外部知识文件与资料来源、账号行程重新校验与隔离、会话重启恢复，以及原有的 ID/重复坐标/跨城步行校验、按天保留、取消、流式和接口权限。
- 隔离 PostgreSQL：迁移重复执行、真实 MyBatis 读写、行程去重、账号隔离、会话版本冲突均验证；没有连接业务数据库。
- 前端：SSE 分段/UTF-8、错误/截断/错误类型、取消信号；桌面和 390px 手机实际浏览器操作检查按天切换、仅导入当天、停止保留原路线、模式隔离及新对话。
- 前端类型检查、生产构建通过。地图与真实 DeepSeek 的外部服务质量及耗时仍需部署后联调。

部署步骤：备份当前源码及私有配置；在现有 PostgreSQL 业务库执行 `backend/sql/pilgrimage_agent_v2.sql`；合并更新包；设置模型密钥和可选资源目录；后端正常构建并重启；前端正常构建并更新整份 dist；检查代理流式转发。不要只替换某个有 hash 的前端 JS 文件，也不要用示例配置覆盖真实配置。无需重建或导入一套地点数据库；未执行新增表迁移时会明确提示数据库未就绪。

建议联调输入：

1. “宇治一天步行，想看上低音号相关取景点”。应检索真实点、返回一天预览，确认导入后才请求路径。
2. “第二天少走一点，第一天不变”。先提供两天行程再测试，应保留第一天，并返回完整的新版本。
3. 站内未收录的作品。应澄清/说明无结果，不能拼进热门无关点。
4. 生成中点击停止。原行程应保留，输入恢复可重试。

测试使用系统默认 JDK 25 时，当前项目 Mockito/Byte Buddy 版本需用测试 JVM 的 Java agent 和 `-Dnet.bytebuddy.experimental=true`；生产目标仍为 Java 17，这个开关不属于生产配置。
