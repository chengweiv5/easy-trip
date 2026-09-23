# README 图片来源

图片来自 Easy Trip 的模拟器运行截图或应用实际导出的 PNG，统一使用中国地点的测试示例数据：首页为杭州、苏州，其余为杭州行程。不是设计稿或真实用户旅行。地图使用真实高德底图；地点安排、路线连线和交通时长为示例数据，不代表实际路线规划结果。

原始截图、采集入口与验证记录保存在本机 `.scratch/`；本目录图片均可独立查看。日历截图使用生产工作台组件，展开日历后的上方地图区域未挂载地图；分享截图则实际加载高德 SDK。

| 图片 | 运行来源与版本 | 原始尺寸 | 处理 |
| --- | --- | --- | --- |
| `my-trips.jpg` | v1.2.0；`TripListContentTest.otherTripRowKeepsSingleEntrySemanticsWithSeparateMenuAndDecorativeArrow`；基于 `650de5f`，仅将首页测试示例改为杭州、苏州后重新运行截图 | 1170 × 2532 | 等比缩至 591 × 1280，无裁切 |
| `map-itinerary.jpg` | v1.2.0；`LakesideWorkspaceVisualTest.realMapKeepsLakesideChromeAcrossPoolAndItinerary`；`7026cf8`，真实高德地图 | 1280 × 2856 | 等比缩至 574 × 1280，无裁切 |
| `itinerary-note.jpg` | v1.2.0；`V12ItineraryPolishTest.itineraryNoteExpandsInPlaceAndUpdatedNoteResetsExpansion`；基于 `d79255d` 加备注一行预览、停留时长行展开收起和暖棕配色修改 | 1170 × 2532 | 保留左上角 1170 × 510 区域，仅去掉下方空白，再等比缩至 640 × 279 |
| `place-pool.jpg` | v1.4.0；2026-09-23 在 emulator-5588 运行 `LakesideWorkspaceVisualTest.realMapKeepsLakesideChromeAcrossPoolAndItinerary`，真实高德地图 | 1073 × 2321 | 等比缩至 592 × 1280，无裁切 |
| `itinerary-calendar-day.jpg` | v1.4.0；基于 `57ea6cf`，临时 `ReadmeScreenshotCaptureTest.calendarScreens`，生产 `TripWorkspaceContent` 与三天内存示例 | 975 × 2110 | 等比缩至 591 × 1280，无裁切 |
| `itinerary-calendar-whole.jpg` | 同上，切换到全程后采集；首屏并列展示第 1、2 天 | 975 × 2110 | 等比缩至 591 × 1280，无裁切 |
| `share-preview.jpg` | v1.4.0；基于 `57ea6cf`，临时 `ReadmeScreenshotCaptureTest.sharePreviewWithRealMap`；生产 `ItineraryShareScreen`、高德 SDK 与 `shareFixture()` | 1073 × 2123 | 等比缩至 647 × 1280，无裁切 |
| `share-long-image.png` | v1.4.0；2026-09-23 运行 `ShareMapCaptureTest.realSdkCaptureUnderOpaquePreviewRetainsMapAndCleansUpView`；生产 `ShareImageRenderer`、高德 SDK 与三天 `shareFixture()` | 1080 × 7159 | 原始完整 PNG，保留三天、地图、交通、备注及页脚，无缩放或裁切 |
| `share-long-image-detail.jpg` | 上述完整长图的摘要与第 1 天，用于 README 内联展示 | 1080 × 7159 | 保留顶部 1080 × 2650 区域，止于第 1 天分隔线之后，等比缩至 734 × 1800；点击链接查看完整 PNG |
| `itinerary-time-edit.jpg` | v1.4.0 时间编辑组件；复用 emulator-5588 留存的 `itinerary-time-wheel-am.png`，杭州西湖天地示例 | 990 × 1431 | 等比缩至 886 × 1280，无裁切 |

原始证据路径：

- `.scratch/readme-china-examples-20260921/my-trips.png`
- `.scratch/v1.2.0-itinerary-polish/v11-workspace-itinerary.png`
- `.scratch/note-warm-brown-20260921/evidence/itinerary-note-expanded.png`
- `.scratch/readme-feature-screenshots/evidence/place-pool.png`
- `.scratch/readme-feature-screenshots/evidence/itinerary-calendar-day.png`
- `.scratch/readme-feature-screenshots/evidence/itinerary-calendar-whole.png`
- `.scratch/readme-feature-screenshots/evidence/share-preview-with-map.png`
- `.scratch/readme-feature-screenshots/evidence/share-long-image.png`
- `.scratch/readme-feature-screenshots/evidence/itinerary-time-edit.png`

处理方式：Pillow 等比缩放，JPEG quality 88–90、optimize；没有在截图上重绘、覆盖文字或拉伸界面。长图细节只截取完整内容段，完整导出 PNG 同时入库。临时截图入口仅采集内存示例，已从应用源码目录移除，保留在 `.scratch/readme-feature-screenshots/ReadmeScreenshotCaptureTest.kt` 供追溯；没有改动应用功能。
