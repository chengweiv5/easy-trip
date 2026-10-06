# 默认选中当天：本地修复验证

日期：2026-10-05  
分支：`codex/v1.8.0-default-today`  
基线：`474728f8062ad7ce5c0c76a0b36d22cee3e0d423`  
交付范围：本地实现与验证；本轮没有提交、推送或安装到物理手机，不更改版本号。

## 原因

上一轮搜索返回地图的实现未包含最初提出的日期默认选择需求。原 `reconcileItineraryScope()` 不接收旅行开始日期或当前日期，没有有效选中值时始终选第一天；因此不是新包安装失败或设备日期判断错误。

## 修复

- 按设备本地时区日期，将当前日期与 `startDate + TripDay.index` 匹配；当天在行程中时默认选当天。
- 未开始、已结束或未设开始日期时默认第一天；没有旅行日时保持全程。
- 有效的手选日期、全程、同一工作台状态恢复、切换地点池与搜索返回不会被覆盖。返回旅行列表再进入时重新应用默认值。
- 删除所选日期后保留原来的相邻日期回退规则。
- 长行程自动将选中日期滚入日期栏可见区域；先等待首次布局，防止第一天选中时把“全程”入口意外滚出。用户主动滚动不会被反复拉回。
- 不修改地图或抽屉外观、旅行日期数据、数据库结构、搜索排序或版本号。

## 验证结果

| 验证 | 结果 | 证据 |
| --- | --- | --- |
| 修复前真实 ViewModel 复现 | 原代码选择第一天，新测试失败 | `evidence/repro-red.log` |
| 修复后针对性单测 | 通过 | `evidence/targeted-green.log` |
| 全量 Debug 单元测试 | 909 项通过，0 失败/错误/跳过 | `evidence/final-build.log`、`app/build/test-results/testDebugUnitTest/` |
| Debug / AndroidTest 构建 | 成功 | `evidence/final-build.log` |
| lintDebug | 0 errors、54 warnings、1 hint | `app/build/reports/lint-results-debug.xml` |
| 新增 UI 测试 | 6 项全部通过 | `evidence/targeted-ui-green.log` |
| 搜索地图真实导航回归 | 1 项通过 | `evidence/targeted-ui-green.log` |
| 扩展 UI 回归 | 18 项中 13 通过、5 个已知基线失败 | `evidence/navigation-ui-verified.log` |
| 旧日期栏基线复现 | 11 项中同样 5 项失败，失败名与原因一致 | `evidence/rail-baseline-ui.log` |
| 差异检查 | `git diff --check` 通过 | 完成本轮时执行 |

固定时钟使用 `2026-10-04T16:30:00Z` 和 `Asia/Shanghai`，验证按 2026-10-05 选择第二天，而不是按 UTC 选择第一天。日期纯逻辑覆盖首日、中间日、末日、范围外、跨年、闰年、无日期、无旅行日、有效手选及删除回退。

UI 测试使用真实 `AppNavigation` 与隔离的内存 Room，覆盖进行中/未来/过去/未设日期旅行、退出重进、手动切日后切换 Tab、长行程第 20 天自动可见、手动滚动第 30 天不被拉回、第一天与“全程”同时可见。

## 已有失败，不属于本次改动

基线 APK 来自 `c6e06fc` 的独立目录。该提交至当前 HEAD 的日期栏实现、日期栏测试及相关行程内容源码无差异。以下 5 项在基线和修复后均失败，未顺便修改旧测试：

1. `railOrdersWholeTripIndexedDaysThenNonScopeAddAction`：仍查找旧文案“第一天”。
2. `railHasFixedWidthLabelsAndSelectableScopes`：旧断言 88dp，现为 64dp。
3. `scopeRailAndDayContentStayInsideSheetBounds`：旧断言 32dp，实际 36dp。
4. `scrollingRailRevealsLastDayWithoutMovingRightContent`：仍查找旧文案“第七天”。
5. `railWholeTripAndBothScopeHeadersShareTheSameTopEdge`：旧顶部对齐断言与现布局不一致。

## 验证命令

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home \
ANDROID_HOME=/Users/bytedance/Library/Android/sdk \
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug

/Users/bytedance/Library/Android/sdk/platform-tools/adb -s emulator-5570 shell am instrument -w \
  -e class 'com.yangchengwei.easytrip.workspace.DefaultItineraryDayTest,com.yangchengwei.easytrip.workspace.SearchResultsMapFlowTest#submitListAndClearUseRealNavigationWithoutSavingSearchResults' \
  com.yangchengwei.easytrip.test/androidx.test.runner.AndroidJUnitRunner
```

独立只读模拟器 `emulator-5570` 已关闭，物理手机未安装本次修复。

## 回滚

修改前备份位于 `/tmp/easy-trip-default-today-20261005/before/`，已核对备份与本轮开始时 HEAD 一致。

仅撤销本轮时，先保存当前文件，再从备份恢复以下 5 个原有源码/测试文件及 `.codex/TASK_STATE.md`；移走本轮新增的 `DefaultItineraryDayTest.kt` 与 `.scratch/v1.8.0-default-today/`。若其后还有新修改，应按差异逐段恢复，不整文件覆盖。不要执行 `reset --hard`、删除应用数据、卸载手机应用或强制推送。

- `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceNavigation.kt`
- `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceViewModel.kt`
- `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryScopeRail.kt`
- `app/src/test/java/com/yangchengwei/easytrip/workspace/WorkspaceNavigationTest.kt`
- `app/src/test/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContentStateTest.kt`
