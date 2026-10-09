# README 图片来源

## 当前介绍：v2.0.0

README 当前引用的所有界面与分享长图于 **2026-10-09** 从 **v2.0.0 / versionCode 14 / Release** 采集，集中保存在 `v2.0.0/`。旧图片保留在原路径作历史材料，README 不再引用旧界面。

- 源码提交：`ebce69e95c494e6840dae3451f7035bdfabac6a0`；APK 内 `SOURCE_STATE=CLEAN`、`DEBUG=false`。
- APK SHA256：`44ec9300610a2a72caa7a5486847f01af3b70225eb010472cd1db3e36e2c2660`。与已完成手机覆盖升级验证的正式包相同，没有为截图改写 App 或加入测试入口。
- 设备：本次新建的专用 Android 12 / API 31 ARM64 模拟器，1080 × 2340、420 dpi、字体比例 1.0。通过正式应用导航打开各页。
- 数据：独立生成的 Room schema9 示例库；杭州三天行程用于地图、日历、地点费用和分享。厦门、成都、大理样例覆盖 2024–2026 年，另有苏州待出行与未定日期旅行。
- **没有读取或公开用户手机数据**。地图是真实高德底图；示例地点名称、坐标、路线连线、交通时长、日期和金额仅用于展示功能，不是真实报价或路线建议。
- 除山岚暮紫对比图外使用湖畔晴空。主题通过正式「设置 → 主题配色」选择并应用；地点编辑只打开与取消，没有为截图保存费用改动。

| 图片（`v2.0.0/` 下） | 内容 |
| --- | --- |
| `expense-years.jpg` | 历年花费，2024–2026 年示例合计 ¥15,495 / 32 笔 |
| `expense-year.jpg` | 2026 年 ¥4,115 / 20 笔，按月回顾 |
| `expense-month.jpg` | 2026 年 10 月 ¥1,421 / 14 笔与分类入口（补充图） |
| `expense-categories.jpg` | 10 月六类金额、笔数、占比与旅行贡献 |
| `place-expense-inline.jpg` | 编辑地点原位单笔输入；零元景点费用也可明确记录 |
| `place-expenses-multiple.jpg` | 同一酒店住宿 ¥680、早餐 ¥128，合计 ¥808 |
| `my-trips.jpg` | 当前首页、旅行／花费导航、待定日期旅行 |
| `place-pool.jpg` | 杭州地图与六个收藏地点 |
| `map-itinerary.jpg` | 地图、当日费用、地点多笔摘要与路段费用 |
| `itinerary-calendar-day.jpg` / `itinerary-calendar-whole.jpg` | 单日日历与全程分页日历 |
| `theme-picker.jpg` / `theme-workspace-violet.jpg` | 当前主题入口及山岚暮紫实际应用 |
| `settings.jpg` | 当前设置页，版本 v2.0.0（补充图） |
| `share-preview.jpg` | 正式全程长图预览，包含备注 |
| `share-long-image-detail.jpg` | 长图摘要与完整第 1 天 |
| `share-long-image.png` | 应用实际生成的完整三天 PNG，1080 × 5304，未裁切或重绘 |

屏幕 JPEG 从原始 1080 × 2340 等比缩小到 591 × 1280，quality 92；未覆盖文字、拼接界面或拉伸。长图细节仅截取原始上部 1080 × 2380（第 2 天标题前的留白），再等比缩小为 581 × 1280；完整原图同时保留。原始 XML、PNG、样例生成脚本、逐图 SHA256、OCR、布局检查和备份位于仓库外本机证据目录：`/Users/bytedance/.codex/artifacts/easy-trip/v2.0.0-release-20261009/`。

## 历史材料：v1.6.0（不代表当前界面）


本目录 16 张图片于 **2026-09-23** 从已发布的 [v1.6.0 正式 APK](https://github.com/chengweiv5/easy-trip/releases/tag/v1.6.0) 重新采集或导出：替换原有 10 张，新增主题选择页及五套主题工作台共 6 张。没有沿用旧版截图，也没有修改应用界面后再采集。

运行版本为 **1.6.0 / versionCode 8 / Release**，源码标签 `v1.6.0` 指向 [`a7eddbfbed9afde107f00152403f086b70817e18`](https://github.com/chengweiv5/easy-trip/commit/a7eddbfbed9afde107f00152403f086b70817e18)。模拟器安装包 SHA256 与公开发布包一致：`f65d055d91a982f4bb2c365db3880e63dab84e3fe74f202fbbd03aa22e6a264e`。

所有界面均通过正式应用导航采集，使用本任务专用 Android 36 模拟器 `emulator-5584`，分辨率 1170 × 2532、密度 480 dpi、字体比例 1.0。默认使用湖畔晴空；主题对比通过工作台「更多 → 主题配色」逐一选择、应用，在相同行程和视角下采集。

截图使用独立测试数据库：首页为「杭州 · 湖畔慢游」与「苏州 · 周末散步」，核心流程为杭州 2026-10-01 至 10-03 的三天、九站示例，同一组地点在不同日期重复安排。地图为真实高德底图；地点坐标、路线连线、距离与交通时长为示例数据，不代表实际路线规划结果。没有读取、修改或展示用户手机旅行数据。

| 图片 | 内容 | 入库尺寸 | 处理 |
| --- | --- | --- | --- |
| `my-trips.jpg` | 首页的杭州与苏州两趟示例旅行，右上角为主题入口 | 591 × 1279 | 从 1170 × 2532 等比缩小，无裁切 |
| `place-pool.jpg` | 杭州旅行的地点池、重复排入次数与真实高德底图 | 591 × 1279 | 从 1170 × 2532 等比缩小，无裁切 |
| `map-itinerary.jpg` | 第 1 天的地图、路线、地点与交通列表 | 591 × 1279 | 从 1170 × 2532 等比缩小，无裁切 |
| `itinerary-time-edit.jpg` | 到达时间、停留时长、备注与再次安排入口 | 591 × 1279 | 从 1170 × 2532 等比缩小，无裁切 |
| `itinerary-calendar-day.jpg` | 单日时间日历，三站及两段交通按时间排列 | 591 × 1279 | 从 1170 × 2532 等比缩小，无裁切 |
| `itinerary-calendar-whole.jpg` | 全程日历并列展示第 1、2 天，分页栏与日历列对齐 | 591 × 1279 | 从 1170 × 2532 等比缩小，无裁切 |
| `share-preview.jpg` | 正式分享预览页，包含备注并完成三天地图生成 | 591 × 1279 | 从 1170 × 2532 等比缩小，无裁切 |
| `theme-picker.jpg` | 五套主题选择页，默认湖畔晴空，无虚构旅行预览卡片 | 591 × 1279 | 从 1170 × 2532 等比缩小，无裁切 |
| `theme-workspace-lake.jpg` | 湖畔晴空实际应用后的同一行程 | 591 × 1279 | 从 1170 × 2532 等比缩小，无裁切 |
| `theme-workspace-forest.jpg` | 松林晨光实际应用后的同一行程 | 591 × 1279 | 从 1170 × 2532 等比缩小，无裁切 |
| `theme-workspace-terracotta.jpg` | 落日陶土实际应用后的同一行程 | 591 × 1279 | 从 1170 × 2532 等比缩小，无裁切 |
| `theme-workspace-violet.jpg` | 山岚暮紫实际应用后的同一行程 | 591 × 1279 | 从 1170 × 2532 等比缩小，无裁切 |
| `theme-workspace-rose.jpg` | 玫瑰沙丘实际应用后的同一行程 | 591 × 1279 | 从 1170 × 2532 等比缩小，无裁切 |
| `itinerary-note.jpg` | 西湖天地行程条目的完整展开备注 | 843 × 333 | 按 UI 节点边界截取完整条目，未重绘 |
| `share-long-image-detail.jpg` | 完整长图的旅行摘要与第 1 天，链接指向完整 PNG | 728 × 1800 | 保留顶部 1080 × 2670，止于第 1 天分隔线后，再等比缩放 |
| `share-long-image.png` | 应用实际生成的完整三天 PNG，与应用缓存文件逐字节一致 | 1080 × 7159 | 原始导出文件，无缩放或裁切 |

JPEG 使用 Pillow 等比缩放、quality 90、optimize。未覆盖文字、重绘控件或拉伸截图。长图细节只展示完整内容段，完整 PNG 同时保留，并确认三天、各日地图、交通、备注与页脚均完整。

本机追溯记录位于 `.scratch/readme-v1.6.0/`：

- `raw/`：原始截图、对应 UI 层级及应用原始导出 PNG。
- `make-fixture.py`、`fixture.db`、`ui.py`：示例数据与正式应用导航采集脚本；不会编译进入 App。
- `process-images.py`：图片处理方式及从实际节点测得的备注裁切边界。
- `evidence/provenance.json`、`export-verification.json`：版本、安装包、设备与导出校验。
- `evidence/image-verification.json`：每张图片的来源、尺寸、处理方式与 SHA256；原有 10 张均已变化。
- `evidence/`：OCR、视觉检查及桌面/手机 README 排版验证。
- `before/`：修改前 README、图片及任务状态备份。
