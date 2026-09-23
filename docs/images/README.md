# README 图片来源

本目录全部 10 张图片于 **2026-09-23** 重新采集或生成，未沿用旧版截图。运行来源为最新 `origin/main` 提交 [`46af47e`](https://github.com/chengweiv5/easy-trip/commit/46af47efd95674bdd5bc48ef95c735567af8f0cb) 的 Debug 构建，应用版本号为 1.4.0，包含 v1.4.0 正式发布之后的工作台修复（全程日历分页栏与日历列等宽对齐、日期标题两行居中、日期操作按钮等距、红色删除图标、抽屉切换保持地图视角等）。

所有界面来自专用只读模拟器 `emulator-5586` 的同一次应用运行，分辨率 1170 × 2532、密度 480 dpi、字体比例 1.0。临时采集入口 `LatestReadmeCaptureTest.allReadmeScreensFromProductionNavigation` 使用生产 `AppNavigation`、Room 仓库、真实高德地图 SDK 和分享渲染器，通过点击、滑动依次进入各功能界面。

示例保存在内存数据库：首页为杭州、苏州两趟旅行，其余均为同一份「杭州 · 湖畔慢游」三天、九站行程。图片不是设计稿或真实用户旅行；地图使用真实高德底图，地点坐标、路线连线、距离与交通时长为测试数据，不代表实际路线规划结果。

| 图片 | 内容与采集方式 | 原始尺寸 | 入库处理 |
| --- | --- | --- | --- |
| `my-trips.jpg` | 应用首页，展示两趟旅行与杭州行程准备度 | 1170 × 2532 | 等比缩至 591 × 1280，无裁切 |
| `place-pool.jpg` | 从首页打开杭州旅行后的地点池与真实地图 | 1170 × 2532 | 等比缩至 591 × 1280，无裁切 |
| `map-itinerary.jpg` | 切换行程与第 1 天，展示地图、行程及最新日期操作按钮 | 1170 × 2532 | 等比缩至 591 × 1280，无裁切 |
| `itinerary-note.jpg` | 展开工作台备注，采集完整的西湖天地行程条目 | 843 × 384 | 原尺寸 JPEG，无裁切 |
| `itinerary-time-edit.jpg` | 从地点菜单打开时间编辑面板，保留工作台背景及全部操作 | 1170 × 2532 | 等比缩至 591 × 1280，无裁切 |
| `itinerary-calendar-day.jpg` | 工作台切换单日时间日历，地图保持挂载 | 1170 × 2532 | 等比缩至 591 × 1280，无裁切 |
| `itinerary-calendar-whole.jpg` | 切换全程日历，首屏并列展示第 1、2 天 | 1170 × 2532 | 等比缩至 591 × 1280，无裁切 |
| `share-preview.jpg` | 从工作台菜单进入分享预览，包含备注并完成地图生成 | 1170 × 2532 | 等比缩至 591 × 1280，无裁切 |
| `share-long-image.png` | 上述分享预览实际生成的完整 PNG，直接复制应用缓存文件 | 1080 × 6444 | 原文件，保留全部三天、地图、交通、备注及页脚，无缩放或裁切 |
| `share-long-image-detail.jpg` | 本次完整长图的旅行摘要与第 1 天，用于 README 内联展示 | 1080 × 6444 | 保留顶部 1080 × 2510 区域，止于第 1 天分隔线之后、第 2 天标题之前，等比缩至 775 × 1800；链接指向完整 PNG |

处理方式：Pillow 等比缩放，JPEG quality 90、optimize。未重绘或覆盖界面文字，未拉伸截图。长图细节只截取完整内容段；完整导出 PNG 同时入库，并校验与应用原始输出字节一致。

页头对齐验证：分页栏排除左侧时间刻度，与实际日历列左右边界完全重合；分页文字及每一天标题、日期的逐行中心偏差均为 0px。已验证双列首末页、窄屏、1.3 倍字体、两位数天数与未设置日期的场景。

本机追溯记录：

- `.scratch/calendar-grid-header-alignment/LatestReadmeCaptureTest.kt`：临时采集入口归档，已从应用源码目录移除。
- `.scratch/calendar-grid-header-alignment/evidence/raw/`：本次全部原始 PNG 与采集清单，每张记录运行版本、提交 SHA、尺寸和时间。
- `.scratch/calendar-grid-header-alignment/evidence/build-provenance.json`：构建基线、模拟器与 APK SHA256。
- `.scratch/calendar-grid-header-alignment/evidence/image-verification.json`：本次全部图片的来源、SHA256 与入库尺寸。
- `.scratch/calendar-grid-header-alignment/evidence/`：构建、截图用例、OCR、README 排版及推送回读记录。

本次先修复全程日历页头与网格对齐，再运行修复版本重新采集全部图片；采集过程使用专用模拟器与内存示例，未操作物理设备。
