# README 图片来源

三张图片均来自 Easy Trip 的模拟器运行截图，使用测试示例数据。不是设计稿或真实用户旅行，原始证据保存在本机 `.scratch/`；本目录 JPEG 为可独立查看的仓库图片。

| 图片 | 运行来源与版本 | 原始尺寸 | 处理 |
| --- | --- | --- | --- |
| `my-trips.jpg` | v1.1.0；`TripListContentTest.otherTripRowKeepsSingleEntrySemanticsWithSeparateMenuAndDecorativeArrow`；已纳入 `97d1001` 的实现 | 1170 × 2532 | 等比缩至 591 × 1280，无裁切 |
| `map-itinerary.jpg` | v1.2.0；`LakesideWorkspaceVisualTest.realMapKeepsLakesideChromeAcrossPoolAndItinerary`；`7026cf8`，真实高德地图 | 1280 × 2856 | 等比缩至 574 × 1280，无裁切 |
| `itinerary-note.jpg` | v1.2.0；`V12ItineraryPolishTest.itineraryNoteExpandsInPlaceAndUpdatedNoteResetsExpansion`；`7026cf8` | 1170 × 2856 | 保留左上角 1170 × 670 区域，仅去掉下方空白，再等比缩至 640 × 366 |

原始证据路径：

- `.scratch/v1.1-color-directions/my-trips.png`
- `.scratch/v1.2.0-itinerary-polish/v11-workspace-itinerary.png`
- `.scratch/v1.2.0-itinerary-polish/v1.2.0-evidence/itinerary-note-expanded.png`

处理方式：Pillow 等比缩放，JPEG quality 88、optimize；没有重绘、替换文案或拉伸界面。图片总大小约 197 KiB。
