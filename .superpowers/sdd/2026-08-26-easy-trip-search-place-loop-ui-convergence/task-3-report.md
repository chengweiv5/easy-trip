# Task 3 报告：搜索详情 AMap 与生命周期复用

## 状态

已完成。

## 实现

- 新增 `searchDetailMapModel(candidate, requestId)`：有坐标时只生成一个 `UNSAVED_SEARCH` marker，无路线、无其他 marker，并使用 `SEARCH_FOCUS` viewport；无坐标时不生成地图模型。
- 搜索详情直接复用 `AmapComposeMap`、`AmapMapHost`、既有 consent 与生命周期实现；未复制地图生命周期控制器，未修改 workspace map。
- search destination 从导航边界接收 consent 与 map host factory，因此每个搜索 destination 创建自己的 map host instance。
- 搜索详情 marker 与地图 POI 点击均为 no-op，不改变当前候选。
- `detailMapRequestId` 在普通详情状态变化中保持稳定；仅详情态的 `RecenterDetail` 增加 request id。
- 无坐标、无 consent 或 map host 创建失败时，详情区域及收藏等操作仍可用。
- 地图与详情各占主要区域；有地图时提供“回到选中地点”操作。

## TDD 与验证

先新增地图模型、request id 稳定性、Recenter、地图 click no-op、无坐标/consent 和地图失败测试，并确认因缺失 mapper、状态/action 与地图接入而 RED；随后补最小生产实现并验证 GREEN。

最终命令：

```bash
./gradlew :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleDebug \
  :app:assembleDebugAndroidTest \
  :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchContentTest,com.yangchengwei.easytrip.amap.AmapComposeMapTest

graphify update .
```

结果：JVM tests、lint、Debug APK 与 AndroidTest APK 构建通过；设备 `easy_trip_p60pro(AVD) - 12` 上 28 个 focused instrumentation tests 全部通过；知识图谱已更新。

## 关注点

- 当前默认详情区域提供最小名称、地址与收藏操作；Task 4 可用共享 `PlaceDetailPanel` 替换该 fallback。
- 已通过 Compose instrumentation 在模拟设备上验证关键交互；未做物理真机视觉验收。
- `graphify update .` 报告两个既有源码文件存在可部分抽取的语法错误，并提示 graphify skill/package 版本差异；本次图谱更新仍成功完成。

## Fix round 1

### 修复

- 生产 `SearchDetailFallback` 不再因候选缺少坐标禁用收藏按钮；无坐标详情会 dispatch `ToggleCollection`。
- `PlaceSearchViewModel` 不再提前忽略无坐标候选，而是进入仓储收藏流程；生产仓储继续通过 `SavedPlace.point` 非空约束拒绝持久化，并将“无法收藏缺少坐标的地点”反馈到 `collectionError`。
- 无 consent 与 map host 创建失败时仍渲染并可点击真实默认收藏操作；未增加加入行程入口，未扩展 Task 4 面板。

### TDD 与验证

先将无坐标详情测试改为使用生产 fallback，并新增无坐标 ViewModel 收藏流程测试、map host 创建失败点击真实默认收藏操作测试；确认 instrumentation 因按钮未 dispatch、JVM 因 ViewModel 未调用仓储而 RED。最小移除 UI 与 ViewModel 的坐标前置拦截后 GREEN。

最终命令：

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchContentTest

./gradlew :app:testDebugUnitTest \
  --tests com.yangchengwei.easytrip.place.ui.PlaceSearchViewModelTest

./gradlew :app:assembleDebug :app:assembleDebugAndroidTest

graphify update .
```

结果：设备 `easy_trip_p60pro(AVD) - 12` 上 17 个 `PlaceSearchContentTest` 全部通过；相关 `PlaceSearchViewModelTest` 全部通过；Debug APK 与 AndroidTest APK 编译通过。

### 关注点

- `SavedPlace` 与 Room schema 仍要求非空坐标；本轮只让无坐标点击进入既有收藏流程并展示明确持久化错误，不制造虚假坐标或无效收藏记录。

## Fix round 2

### 修复

- 生产 `SearchDetailFallback` 接收当前候选的收藏 busy 与错误状态；busy 时禁用收藏按钮，避免重复提交。
- `PlaceSearchUiState` 增加 `collectionErrorPoiId`，收藏与取消收藏失败时记录错误所属 POI，清除错误时同步清除归属。
- 详情 fallback 只展示当前候选对应的收藏错误，避免其他搜索结果的错误串到当前详情。
- 未增加加入行程入口，未扩展 Task 4 面板。

### TDD 与验证

先新增生产 fallback Compose 测试，覆盖当前候选 busy 时按钮禁用且错误可见，以及其他候选错误不显示；同时扩展无坐标 ViewModel 测试验证错误归属。确认 Compose 因按钮仍启用而 RED、JVM 因错误归属未记录而 RED，随后补最小状态传递与展示实现并验证 GREEN。

最终命令：

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchContentTest

./gradlew :app:testDebugUnitTest \
  --tests com.yangchengwei.easytrip.place.ui.PlaceSearchViewModelTest

./gradlew :app:assembleDebug :app:assembleDebugAndroidTest

graphify update .
```

结果：设备 `easy_trip_p60pro(AVD) - 12` 上 19 个 `PlaceSearchContentTest` 全部通过；相关 `PlaceSearchViewModelTest` 全部通过。

### 关注点

- 错误归属随现有单一 `collectionError` 一起维护；新一次收藏操作会清除旧错误及其 POI 归属。
