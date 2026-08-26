# Task 1 报告：搜索详情状态与统一返回

## 状态

已完成。

## 实现

- 新增 `SearchDisplayMode.Results` / `MapDetail(poiId)`，仅持久化模式与 `poiId`，不持久化 candidate。
- `OpenDetail` 仅接受当前搜索结果中的 `poiId`，保持 query/results 不变。
- 恢复详情时等待搜索进入终态后校验；目标缺失则回退 Results 并清理已保存 `poiId`。
- UI state 同步 `savedPlacesByPoiId`，`recentlyCollectedPoiIds` 保持 ViewModel 会话级状态。
- 统一 Back 优先级：mutation busy 忽略 → 关闭确认 → 取消编辑 → detail 返回 results → 请求 destination exit。
- 系统 Back 与顶部 Back 均 dispatch `PlaceSearchAction.Back`；只有 `shouldNavigateBack` 才调用外部 `onBack`。
- 未实现地图或详情面板 UI。

## TDD 与验证

先新增 9 个模式、恢复和返回行为测试，并确认因缺少 `SearchDisplayMode` / `OpenDetail` / back decision 编译失败；随后补最小实现。mutation busy 场景另以非当前详情 poi 的 busy 集合确认失败，再改为任意 mutation busy 全局锁定。

最终命令：

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.yangchengwei.easytrip.place.ui.PlaceSearchViewModelTest \
  --tests com.yangchengwei.easytrip.place.ui.PlaceSearchReducerTest
```

结果：`BUILD SUCCESSFUL`。

已执行：

```bash
graphify update .
```

结果：图更新为 4394 nodes / 9306 edges / 242 communities；既有 `NetworkMonitor.kt`、`RoutePlanner.kt` AST 解析警告仍存在。

## Fix round 1/5

补充真实行为门禁：

- 使用阻塞 Repository 验证收藏和删除协程实际挂起期间，Back 不改变详情模式且不请求退出。
- 单独验证 `detailDraft.isSaving = true` 时 Back 完全无状态变化。
- 覆盖恢复 MapDetail 后立即发起新 query、旧请求忽略取消并迟到的组合；旧响应不清理 mode/SavedState keys，当前 query 的终态结果负责最终裁决。
- 同时存在 confirmation 与 edit 时，连续 Back 严格按 confirmation → edit → detail → exit 推进。
- Compose Route 覆盖连续 Back 与重组，外部 `onBack` 仅调用一次且 `shouldNavigateBack` 被消费。
- 将统一 Back 状态转换提取为 `reducePlaceSearchBack`，ViewModel 使用同一转换并仅在 mode 改变时同步 SavedState。

验证：

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.yangchengwei.easytrip.place.ui.PlaceSearchViewModelTest \
  --tests com.yangchengwei.easytrip.place.ui.PlaceSearchReducerTest

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchContentTest
```

JVM 聚焦测试与 9 个 Compose instrumentation tests 均通过。

## 关注点

- `PlaceDetailEditState` 仅建立 Task 1 所需状态边界；创建、更新和保存动作由后续详情任务接入。
- 本轮仅验证既有 Route 返回行为，未实现地图或详情面板 UI。
