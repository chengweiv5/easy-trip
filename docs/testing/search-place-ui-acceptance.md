# Search / Place UI 候选验收

## 自动化结果

| 门禁 | 结果 | 测试数 | 失败 | 备注 |
| --- | --- | ---: | ---: | --- |
| JVM `testDebugUnitTest` | PASS | 404 | 0 | 52 个 suite，0 skipped；含跨 POI 并发收藏回归 |
| `PlaceSearchContentTest` | PASS | 20 | 0 | 含 route callback 替换回归 |
| `PlaceDetailPanelTest` | PASS | 12 | 0 | 含预设标签多选和 8 标签上限可移除 |
| `PlacePoolFlowTest` | PASS | 18/18 | 0 | 本轮独占 AVD 单次整类通过 |
| `RoomSavedPlaceRepositoryTest` | PASS | 12 | 0 | 整类通过 |
| `WorkspaceSearchReturnNavEntryTest` | PASS | 3 | 0 | 整类通过 |
| lint / debug APK / androidTest APK | PASS | 3 tasks | 0 | `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest` |
| V1 Scenario Catalog | PASS | 47/47 | 0 | 首次首测 signal 9；重启模拟器后 47/47 |
| V1 Full UI | PASS | 47/47 | 0 | 保留业务断言，迁移搜索结果 selector 后 47/47 |

## 返回与发布契约

- 顶部返回与系统 Back 都只派发 `PlaceSearchAction.Back`。
- 返回优先级为删除确认 → 编辑 → 详情 → 结果 → workspace；mutation busy 时锁定返回。
- 只有 Results 退出产生单一 `PlaceSearchEffect.ExitDestination`；route 只收集该 effect，没有 bool/effect 双轨。
- 详情返回只回 Results，不发布；Results 退出只发出一次外部返回回调。
- 搜索会话只发布本次新增收藏；恢复的已有收藏不会再次发布。
- workspace `SavedStateHandle` payload 消费后立即清空，重建不重发。
- 无旅行日时地点池单地点 `＋` 打开既有“添加旅行日”引导，不伪装已进入目标日。
- Search / Place Pool 共用无状态详情面板，并展示 repository `observeTags` 的预设标签多选。
- 不同 POI 收藏操作可并发；busy、错误和 completion generation 按 poiId 隔离。
- route effect 收集通过 `rememberUpdatedState` 调用最新 `onBack` callback。

## 真机验收

**DEFERRED**。本候选仅完成 Android 12 模拟器自动化；以下最终物理真机项待统一验收：

- 顶部返回和系统 Back 交互一致。
- edit → detail → results → workspace 层级与动效自然。
- mutation busy 时返回、关闭、重复提交均锁定。
- 收藏后回到地点池的“最近加入”视觉只出现一次。
- 窄屏、大字体、软键盘及地图详情在真实 GPU/系统栏环境下无严重裁切。
