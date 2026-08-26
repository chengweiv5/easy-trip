# Task 7 报告：单地点加入行程入口

## 实现

- `AddToItineraryViewModel.startForPlace(placeId)` 仅接受最新 validity 中存在的 SavedPlace id，且要求至少存在一个有效旅行日。
- 新流程固定为单地点选择、未选择目标日、`SELECT_TARGET_DAY`、`FromPlacePool`。
- 启动时清理上一流程的 result、error 和 undo batches，避免状态污染，并通过既有 draft 持久化支持 SavedState 恢复。
- 新增 `PlacePoolAction.StartAddSingle(placeId)`；PlacePool 仅定义/转发 action 边界，没有新增 row UI，也没有接入 AddToItineraryViewModel coordinator。
- 保持原批量入口、submit、undo 和 SavedState 行为不变。

## TDD 证据

RED：

```text
./gradlew :app:testDebugUnitTest --tests com.yangchengwei.easytrip.itinerary.ui.AddToItineraryStateTest
FAILED: Unresolved reference 'startForPlace'（5 个新增场景）
```

GREEN：

```text
./gradlew :app:testDebugUnitTest --tests com.yangchengwei.easytrip.itinerary.ui.AddToItineraryStateTest
BUILD SUCCESSFUL
```

新增覆盖：

- 单地点直接进入目标日选择，覆盖选中项、目标日、step、editingTarget。
- 清理旧成功 result、undo token 和 error。
- 拒绝最新 validity 中不存在的地点。
- 无旅行日期不创建流程。
- 全套既有状态测试继续覆盖批量入口、submit、undo、SavedState 恢复。

## 回归验证

```text
./gradlew :app:testDebugUnitTest --tests com.yangchengwei.easytrip.place.ui.PlacePoolViewModelTest
BUILD SUCCESSFUL
```

未新增界面元素，因此无 Task 7 独立浏览器/设备交互路径；真实 coordinator 接线留给 Task 8。
