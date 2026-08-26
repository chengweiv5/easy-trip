# 行程内容族 UI 候选验收

日期：2026-08-26
候选基线：`a601f08`
设备：`easy_trip_p60pro(AVD) - 12`

## Automated

- JVM：PASS，338 tests，0 failures，0 skipped。
- Focused Compose：PASS，87 tests，0 failures，0 skipped。覆盖 `ItineraryEditingTest`、`ItineraryTimelineContentTest`、`ItineraryScopeRailTest`、`WholeTripItineraryContentTest`、`TripWorkspaceContentTest`、`WorkspaceFlowTest`。
- Catalog：PASS，47/47，0 failures，0 skipped。首次运行发现状态矩阵仍断言旧文案“等待计算/计算中”；确认生产已按批准设计将 PENDING 与 CALCULATING 合并为“正在计算路线”后，仅迁移 selector，并以精确数量 2 保留两种领域状态的覆盖。
- Full UI：PASS，47/47，0 failures，0 skipped。
- lint / APK：PASS，`lintDebug`、`assembleDebug`、`assembleDebugAndroidTest` 均成功。

## Deferred physical device

状态：DEFERRED（按统一候选批次决定延期，未执行真机验收，不记为 PASS）。

- 半屏单日信息密度。
- 展开长行程滚动。
- 长地点名与长错误。
- handle / menu 误触。
- RouteLeg 四态。
- 全程只读层级。
- 120dp 拖动步长的真机手感。

## Known differences and carried review items

以下项目不影响本次自动化候选门禁，但必须进入 whole-branch review，不得静默忽略：

- zero elevation 测试只间接覆盖，未直接观测运行时 elevation。
- editable/read-only geometry 测试比较共享 primitive，未分别渲染完整 `ItineraryItemRow` 验证。
- `more` 入口仍使用文本 glyph，未以显式 20–22dp 图标尺寸表达。
- RouteLeg 长错误测试验证最大两行，但未直接证明不存在 fixed height。
- RouteLeg 四态文字测试使用全局 selector，未全部限定到各自 leg tag scope。

## Candidate result

自动化候选门禁通过；真机项目保持 DEFERRED。上述 carried review items 和冲突裁决需在 whole-branch review 与统一真机验收中继续跟踪。
