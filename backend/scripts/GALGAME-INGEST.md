# Galgame 资料采集与入库

本目录的数据脚本只使用公开接口，采集与页面展示分离：访客打开页面时读取 MySQL，
不会实时访问 VNDB、萌娘百科或 Bangumi。

## 首批数据

- 作品数：20 部（`galgame_catalog.json` 中的真实作品名单）。
- 结构化字段：VNDB 公共 API（`https://api.vndb.org/kana/vn`）。
  - 原名、发行日期、开发商、支持平台、标签、评分、评分人数。
- 中文名、别名、背景参考：萌娘百科 MediaWiki API（`https://zh.moegirl.org.cn/api.php`）。
- 官方商店页核对：Steam Store API（能匹配到 appid 的作品）。
- Bangumi：当前采集环境访问 `api.bgm.tv` / `bgm.tv` 均连接超时，未使用其条目或评分；
  失败原因记录在 `backend/data/galgame_collection_report.json`，页面不会伪造 Bangumi 评分。
- Key、07th Expansion、AQUAPLUS 等日文官网与 Fandom 专题 Wiki 在当前网络同样连接超时，
  未作为来源；可访问的 Steam 商店页用于官方发行信息核对。
- 露西 -她所期望的一切- 的萌娘百科页面摘要为空，简介依据 VNDB 与公开商店页整理；
  ATRI 的发行日期在 VNDB/Steam 为 2020-06-18、萌娘百科记为 2020-06-19，已在 `release_note` 保留差异。
- 封面：仅下载 VNDB 标记为 `sexual=0`、`violence=0` 的缩略图，保存到
  `frontend/public/galgame/<slug>.jpg`；下载失败时使用
  `frontend/public/galgame/placeholder.svg`。封面版权归原发行方，仅用于非商业作品识别。

评分统一换算为十分制（VNDB API 的 87.3 → 8.7），页面上必须同时显示来源与评分人数，
例如“VNDB 8.7 / 10 · 11,534 人评分”。

## v6 内容与展示更新（2026-10-27）

v6 是最终选定的内容与展示层更新，不新增数据库列，数字 ID 不变：

- `frontend/src/data/galgameGuides.json` / `.ts`：20 部作品的自然中文导读、看点、玩法参考、版本说明和来源。
- `frontend/src/data/galgameMedia.json` / `.ts`：51 条角色介绍、87 张官方 Steam 商品预览、18 张官方 PV 封面、
  6 张人物图、19 部作品的中国区价格/免费状态与试玩入口，通过 `slug` 关联既有接口。
- `frontend/src/data/galgameDiscovery.json` / `.ts`、`GalgameExplore.vue`：4 个专题（中文可玩 14 部、短篇入门 1 部、
  科幻与悬疑 12 部、校园与恋爱 5 部）、系列/同厂商/同专题关联和稳定 slug 路由；真实数据库 ID 由分页接口解析，不写死。
- `frontend/public/galgame/media/`：87 张预览、6 张人物图、18 张 PV 封面；原图版权归权利人，公开来源不等同于新授权。
- 选定背景 `frontend/public/galgame/background-aokana-sky.png`（来源记录在同名 `.source.json`），列表与详情共用；
  桌面两侧全高露出人物，手机隐藏侧边构图层并降低背景对比。
- `frontend/src/view/GalaGame/GalgameIntroduce/GalgameExtras.vue`：版本与价格、角色速览、按“对白与正文/场景/CG与演出/
  玩法与系统”分类的画面预览、PV 观看入口与弹窗。
- 白色相簿2官网限制素材转载，因此不下载其图片，改为官网看图入口；该作评分未核实，`ratingVerified: false`，
  页面显示“评分篇章核对中”，不展示历史评分。
- 价格为中国区 2026-10-27 快照，页面注明“现价以商品页为准”；999、寒蝉、海猫按合集/第一章/出题篇计价，
  不改成全系列价格；寒蝉与白色相簿2没有本次核实的官方商店 PV，不填假链接。
- `backend/sql/galgame_editorial_update.sql`：按 20 个稳定 slug 调整排序并修正 ATRI 开发商。
  本地数据库经核对已处于 v2 更新后的状态（排序 1–20、ATRI 为 `Frontwing / 枕`），本次无需重复变更；
  更新前已将 20 行备份到 `backend/data/galgame_editorial_backup_20261027.json`。

v6 验收：

```powershell
node backend/scripts/verify_galgame_content.mjs .
```

该命令检查 20 部覆盖、51 条角色、87 张本地截图、18 条官方 PV、4 个专题与背景文件、来源、币种、官网回退、
评分保护和 4 个 Vue 组件编译。

## 目录与文件

| 文件 | 作用 |
| --- | --- |
| `galgame_catalog.json` | 作品名单、中文名、别名、题材、无剧透简介、版本说明、官网/Steam 链接 |
| `collect_galgame.py` | 采集、清洗、生成 JSON/SQL、可选写入 MySQL |
| `verify_galgame_content.mjs` | 校验 20 部导读、51 条角色、87 张本地截图、18 条 PV、专题、背景、来源与 Vue 组件编译 |
| `../data/galgame_seed.json` | 清洗后的 20 条数据（审计用） |
| `../data/galgame_collection_report.json` | 采集报告：来源状态、缺失字段、错误 |
| `../data/galgame_editorial_backup_20261027.json` | v3 SQL 前的 20 行排序/开发商备份 |
| `../sql/galgame.sql` | 表结构 |
| `../sql/galgame_seed.sql` | 表结构 + 20 条 `INSERT ... ON DUPLICATE KEY UPDATE` |
| `../sql/galgame_editorial_update.sql` | 排序与 ATRI 开发商增量 SQL |
| `../.cache/galgame/*.json` | 7 天响应缓存（不入库、不展示） |
| `../../frontend/src/data/galgameGuides.*` | 20 部编辑导读 |
| `../../frontend/src/data/galgameMedia.*` | 角色、截图、分类、PV、价格与商店资料 |
| `../../frontend/src/data/galgameDiscovery.*` | 专题、关联关系与稳定 slug 解析 |
| `../../frontend/public/galgame/media/` | 本地角色图、分类预览与 PV 封面 |
| `../../frontend/public/galgame/background-aokana-sky.*` | 列表/详情背景及来源说明 |

## 运行

在项目根目录执行（使用 backend 自带 Python 虚拟环境，已安装 requests / PyMySQL）：

```powershell
# 1. 只采集，生成 data/galgame_seed.json 与 sql/galgame_seed.sql
backend\scripts\.venv\Scripts\python.exe backend\scripts\collect_galgame.py --refresh

# 2. 采集并写入本地 MySQL（默认读取 backend/src/main/resources/application.properties）
backend\scripts\.venv\Scripts\python.exe backend\scripts\collect_galgame.py --apply --refresh

# 3. 不重新联网，使用 7 天缓存刷新数据库
backend\scripts\.venv\Scripts\python.exe backend\scripts\collect_galgame.py --apply
```

可用参数：

- `--refresh`：忽略缓存，重新请求来源。
- `--apply`：写入 MySQL；不传时只生成文件。
- `--no-covers`：不下载封面，保留 VNDB 远程地址。
- `--limit 5`：只处理前 5 部，调试用。
- `--mysql-user` / `--mysql-pass` / `--mysql-db`：覆盖数据库连接；也可用
  `BISHE_MYSQL_USER`、`BISHE_MYSQL_PASSWORD`、`BISHE_MYSQL_DB` 环境变量。

访问规则：每次请求间隔 0.8 秒、20 秒超时、最多 2 次重试、缓存 7 天；只调用公开 API，
不登录、不绕过验证码或站点限制。

## 去重与版本

- `slug` 是稳定唯一键，`tb_galgame.slug` 建唯一索引；重复执行脚本是 upsert，不会产生重复行。
- 同一作品的重制版、移植版、Fan disc 不合并：
  - `release_date` 记录该条目的首发日期；
  - `release_note` 记录 PC / 主机 / Steam 等版本差异；
  - 例如《高考恋爱100天》首发实体版 2014-07-20，Steam 版 2015-05-30，Switch 版 2018；
  - 《白色相簿2》以 Introductory Chapter 2010-03-26 为首发，Closing Chapter 2011-12-22。
- 多来源冲突：以 VNDB 的结构化字段为基础，中文名/别名参考萌娘百科，Steam 商店页仅作核对；
  冲突与取舍写入 `release_note`、`source_note` 和 `sources_json`，不拼接无法核实的字段。

## 数据表

`tb_galgame` 主要字段：

- `name_zh` / `name_original` / `aliases`
- `short_intro` / `intro`（无剧透，只介绍题材、背景和故事开端）
- `cover` / `developer` / `release_date` / `release_year` / `release_note`
- `platforms` / `genres`
- `rating` / `rating_count` / `rating_source` / `rating_source_url`
- `moegirl_url` / `bangumi_url` / `vndb_url` / `official_url` / `steam_url`
- `sources_json` / `source_note` / `fetched_at`

接口：

- `GET /api/galgame?page=1&genre=科幻&yearMin=2009&yearMax=2015&sortBy=rating`
- `GET /api/galgame/hot`
- `GET /api/galgame/byid?id=1`

## 许可与署名

- 萌娘百科内容适用 CC BY-NC-SA 3.0 CN；详情页“资料来源”保留页面链接、采集时间和许可说明。
- VNDB 仅作为资料与评分来源，详情页保留条目链接；评分注明 VNDB 与评分人数。
- 封面版权归原发行方，页面仅作作品识别；如权利方有异议，替换为默认封面即可。
- 简介由项目根据来源资料重新整理，不是百科段落照搬；不采集或展示露骨成人内容、成人图片或盗版下载链接。
