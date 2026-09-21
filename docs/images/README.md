# README 图片来源

三张图片均来自 Easy Trip 的模拟器运行截图，统一使用中国地点的测试示例数据：首页为杭州、苏州，地图与备注为杭州西湖周边行程。不是设计稿或真实用户旅行，原始证据保存在本机 `.scratch/`；本目录 JPEG 为可独立查看的仓库图片。

| 图片 | 运行来源与版本 | 原始尺寸 | 处理 |
| --- | --- | --- | --- |
| `my-trips.jpg` | v1.2.0；`TripListContentTest.otherTripRowKeepsSingleEntrySemanticsWithSeparateMenuAndDecorativeArrow`；基于 `650de5f`，仅将首页测试示例改为杭州、苏州后重新运行截图 | 1170 × 2532 | 等比缩至 591 × 1280，无裁切 |
| `map-itinerary.jpg` | v1.2.0；`LakesideWorkspaceVisualTest.realMapKeepsLakesideChromeAcrossPoolAndItinerary`；`7026cf8`，真实高德地图 | 1280 × 2856 | 等比缩至 574 × 1280，无裁切 |
| `itinerary-note.jpg` | v1.2.0；`V12ItineraryPolishTest.itineraryNoteExpandsInPlaceAndUpdatedNoteResetsExpansion`；基于 `d79255d` 加备注一行预览、停留时长行展开收起和暖棕配色修改 | 1170 × 2532 | 保留左上角 1170 × 510 区域，仅去掉下方空白，再等比缩至 640 × 279 |

原始证据路径：

- `.scratch/readme-china-examples-20260921/my-trips.png`
- `.scratch/v1.2.0-itinerary-polish/v11-workspace-itinerary.png`
- `.scratch/note-warm-brown-20260921/evidence/itinerary-note-expanded.png`

处理方式：Pillow 等比缩放，JPEG quality 88、optimize；没有在截图上重绘、覆盖文字或拉伸界面。
