# Task 2 报告：搜索结果详情入口与列表恢复

## 状态

已完成。

## 实现

- 搜索结果整行新增 Button 语义“查看<地点名>详情”，点击 dispatch `OpenDetail(poiId)`。
- 收藏保持独立 48dp Button 操作；点击只 dispatch `ToggleCollection(poiId)`，不会触发行详情。
- `PlaceSearchContent` 按 `SearchDisplayMode.Results` / `MapDetail` 切换，并提供 `(PlaceCandidate) -> Unit` 的 detail content slot。
- 无坐标候选仍可进入 detail slot；本 Task 未提供地图内容或地图聚焦请求。
- `PlaceSearchRoute` 使用 `rememberSaveable(saver = LazyListState.Saver)` 持有结果列表状态，详情往返不重建列表位置。
- 保留搜索初始、加载、空、失败与结果文本语义；结果和详情模式均未加入“加入行程”入口。
- 未修改 ViewModel、Repository 业务，未实现真实地图或 `PlaceDetailPanel`。

## TDD 与验证

先新增结果行/收藏独立操作、Button 语义、详情往返列表位置、无坐标详情和无加入行程入口测试，并确认因缺少 `resultsListState` / `detailContent` 接口而 RED；随后补最小实现。

最终命令：

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchContentTest

./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin

git diff --check
```

结果：13 个 Compose instrumentation tests 通过；JVM tests 与 AndroidTest Kotlin 编译通过；diff 格式检查通过。

## 关注点

- Task 3/4 负责向 detail slot 注入真实地图和 `PlaceDetailPanel`。
- 本轮设备测试使用 `easy_trip_p60pro(AVD) - 12` 串行执行；未做物理真机视觉验收。
