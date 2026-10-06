# v1.8.0 三项优化：本地开发验证

状态：DONE（本地开发与验证完成，未提交、推送或安装物理手机）  
日期：2026-10-05  
分支：`codex/v1.8.0-default-today`  
HEAD：`474728f8062ad7ce5c0c76a0b36d22cee3e0d423`

## 范围与授权

用户明确要求“不用改设计文件，直接开发即可”。本轮未调用 Pencil，未修改任何设计文件；保留此前已完成但未提交的“进入行程默认当天”修复。不调整搜索排序、版本号、数据库结构，也不发布 Release。

## 完成内容

1. **去掉行程左侧重复竖线**：删除日期栏与内容间额外的 `VerticalDivider`，保留日期按钮边框及 12dp 内容间距。
2. **搜索结果显示距我的距离**：地址下方显示“距我 560 米 · 直线”或“距我 1.1 公里 · 直线”。100 米以内显示“100 米内”，不冒充步行/驾车距离。定位或地点坐标无效时隐藏距离；长名称与收藏按钮不重叠，点击详情与收藏行为不变。
3. **同城地图突出我的位置**：同城搜索结果地图增加蓝色圆点、白色描边、半透明光圈及常显“我的位置”标签；与原搜索标记分开，不计入结果数量，不写入收藏或行程。首次展示将当前位置和搜索结果一起纳入视野；手动拖图、点击定位或展开抽屉后不反复自动拉回。

## 边界处理

- 复用已有前台定位会话，以可观察状态发布坐标及城市；坐标先就绪时可先显示距离，城市未确认时不显示同城标记。
- 同城判断优先使用已有归一化行政区划身份，不以电话区号判断；缺少代码时仅在城市名称一致时回退。城市不一致、城市未知、无可定位结果或坐标无效时不显示同城标记。跨城混合结果只要含当前城市的有效地点，即显示我的位置。
- 手动定位立即清除旧城市，避免新坐标与旧城市混用；晚到旧请求不得覆盖新定位。
- 前台页面恢复/搜索时，超过两分钟的缓存会重新定位；没有后台周期定位。权限撤回或会话关闭后清空对外位置；定位失败不伪造距离。
- 同步更新搜索和工作台的权限说明，明确定位用途包含搜索、距离与我的位置。
- 定位仅存内存会话，不写入搜索快照、收藏或数据库。

## 验证

| 项目 | 结果 | 证据 |
| --- | --- | --- |
| 全量 Debug 单元测试 | 920 项通过，0 失败/错误/跳过 | `evidence/final-build.log`；`app/build/test-results/testDebugUnitTest/` |
| Debug 与 AndroidTest 构建 | 成功 | `evidence/final-build.log` |
| lintDebug | 0 errors、54 warnings、1 hint | `app/build/reports/lint-results-debug.xml` |
| 针对性 UI 回归 | 27 项全部通过 | `evidence/ui-final.log` |
| 真实高德地图三档抽屉 | 相机和摘要坐标不变 | `SearchResultsMapFlowTest.realMapCameraStaysFixedWhenDrawerCoversSearchResults` |
| 日期栏间距 | 12dp，无额外分隔线宽度 | `ItineraryRailSpacingTest` |
| 距离布局/交互 | 更新、隐藏、长名称、收藏/详情独立点击通过 | `SearchDistanceUiTest` |
| 同城标记 | 蓝点/白环/光圈像素、标签、真实导航传递通过 | `CurrentLocationIconTest`、`SearchResultsMapFlowTest` |
| 默认当天与定位权限回归 | 6 项日期测试、2 项跨页面定位测试、13 项权限测试通过 | `evidence/ui-final.log` |
| OCR 与画面检查 | “我的位置”、两种距离、日期按钮均可读；没有重复竖线 | `evidence/ocr.json`、`evidence/images/` |
| 差异与设计保护 | `git diff --check` 通过，设计文件 hash 未变 | `verification.json` |

UI 测试使用独立只读 `trail_map_api36` 模拟器（`emulator-5570`），已关闭。没有安装、卸载或清除物理手机应用。

首次 UI 回归发现旧“两个结果应适配两个点”的断言，现应为“两个结果加我的位置共三个点”；更新与需求直接关联的断言后，最终 27 项全通过。模拟器首次启动出现 System UI 未响应对话框，选择等待后重新采集了无对话框的最终截图。首次安装时设备尚未完成启动，等待启动完成后安装成功。没有将这些中间失败误报为最终通过。

## 主要文件

- `place/domain/LocatedPosition.kt`：有效坐标、直线距离与同城身份规则。
- `place/amap/AppLocationSession.kt`：可观察位置、过期刷新、权限撤销及竞态隔离。
- `place/ui/RememberLocatedPosition.kt` 与 `AppNavigation.kt`：前台生命周期与页面数据接线。
- `place/ui/PlaceSearchContent.kt`：距离展示，保留收藏/详情操作。
- `workspace/WorkspaceSearchResults.kt`、`TripWorkspaceViewModel.kt`：同城位置与首次地图视野。
- `workspace/CurrentLocationIconView.kt`、`AmapComposeMap.kt`：独立定位图层。
- `itinerary/ui/WorkspaceItineraryContent.kt`：去掉重复分隔线。

## 回滚

本轮修改前备份在 `/tmp/easy-trip-location-improvements-20261005/before/`，已包含此前默认当天修复。逐文件比对后恢复本轮差异即可，不会要求撤销前一轮修复；新建文件列表与 hash 见 `verification.json`。恢复前再次备份当前文件，如果之后还有改动需逐段恢复，不覆盖整份新改动。不执行 `reset --hard`、清除应用数据或强制推送。

## 验证命令

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home \
ANDROID_HOME=/Users/bytedance/Library/Android/sdk \
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug

/Users/bytedance/Library/Android/sdk/platform-tools/adb -s emulator-5570 shell am instrument -w \
  -e class 'com.yangchengwei.easytrip.place.ui.SearchDistanceUiTest,com.yangchengwei.easytrip.workspace.CurrentLocationIconTest,com.yangchengwei.easytrip.itinerary.ui.ItineraryRailSpacingTest,com.yangchengwei.easytrip.workspace.SearchResultsMapFlowTest,com.yangchengwei.easytrip.workspace.DefaultItineraryDayTest,com.yangchengwei.easytrip.workspace.AppSessionCitySearchTest,com.yangchengwei.easytrip.workspace.WorkspacePermissionFlowTest' \
  com.yangchengwei.easytrip.test/androidx.test.runner.AndroidJUnitRunner
```
