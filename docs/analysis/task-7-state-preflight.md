# Easy Trip Full UI Task 7 状态预研

## 结论

Task 7 不能直接在当前预研基线 `e303fcf` 上实现。当前基线仅完成 Task 4；Task 5 已存在于提交 `9a7548d`、`20fae8c`、`2be3950`，但不在本分支祖先链上；Task 6 尚未发现实现提交。正确串行入口是：**Task 5 最终提交 → Task 6 最终契约 → Task 7**。

Task 7 的核心建模原则：

1. `SavedPlaceRowUi.scheduled` 表示 Room 中已有行程安排，是持久领域事实；`AddToItineraryUiState.selectedPlaceIds` 表示本次未提交选择，是可恢复 UI 草稿。二者必须分离，不能由一个 Boolean 互推。
2. `selectedPlaceIds`、`targetDayId`、当前编辑目标写入 `SavedStateHandle`；`isSubmitting`、`result`、undo 可用令牌只保存在内存中。
3. 提交前及旅行日流每次更新时校验目标日。目标日失效时清空 `targetDayId`、回到选日态、刷新可选日期，但保留仍存在于地点池中的选择。
4. partial success 只清除已创建项对应的选择，保留失败地点用于重试；所有 `SavedPlace` 均保留。
5. undo 必须只删除该次 `createdItemIds`，不得按 placeId 删除，因此不会误删提交前已存在的同地点安排。

## 调研基线与事实

- 计划：`docs/superpowers/plans/2026-08-23-easy-trip-v1-full-ui-implementation.md` Task 6/7/8。
- 设计状态所有权：`TripWorkspaceViewModel` 协调唯一 overlay，`PlacePoolViewModel` 拥有收藏地点与选择；Route 聚合状态后传给 Content。
- 当前 Workspace：`WorkspaceOverlay` 已有 `SelectPlacesForDay(dayId)`、`SelectTargetDay(placeIds)`；`TripWorkspaceViewModel` 的 overlay 仅为内存 `MutableStateFlow`，并未恢复。
- 当前仓储真实 ID 类型均为 `String`：`TripDay.id`、`SavedPlace.id`、`ItineraryItem.id` 与 `ItineraryRepository` 参数。计划示例中的 `Long` 与现状冲突，Task 6/7 应以现有 `String` 契约为准，禁止 hash 转换。
- 当前 `SelectTargetDay(Set<Long>)` 也被“跨天移动行程项”复用，并通过 `stableWorkspaceOverlayId` 将 String hash 成 Long。这与 Task 7 的“把多个 SavedPlace 加入某日”是不同业务意图，不能继续共用同一 payload。
- Task 5 最终模型已有 `SavedPlaceRowUi(itineraryOccurrenceCount, scheduled, recentlyCollected)`，并明确记录 `scheduled` 与 Task 7 的用户选择分离；usage count 会随 `itinerary_items` 变化更新。
- `RoomItineraryRepository.addItem` 每次调用是独立事务，追加会同步同日 RouteLeg；`deleteItem(createdItemId)` 能精确撤销单次创建并修复邻接。当前接口没有跨多个地点的单一原子批量方法。

## Task 6 依赖契约

Task 7 开工前必须从 Task 6 获得并锁定下列真实契约；计划中的 Long 必须改为 String：

```kotlin
data class AddPlacesRequest(
    val tripId: String,
    val dayId: String,
    val savedPlaceIds: List<String>,
)

sealed interface AddPlacesOutcome {
    data class Success(val dayId: String, val createdItemIds: List<String>) : AddPlacesOutcome
    data class PartialSuccess(
        val dayId: String,
        val createdItemIds: List<String>,
        val failedPlaceIds: List<String>,
    ) : AddPlacesOutcome
    data class TargetDayMissing(val retainedPlaceIds: List<String>) : AddPlacesOutcome
}
```

还需明确：

| 契约 | Task 7 依赖方式 | Task 6 必须保证 |
|---|---|---|
| 选择顺序 | `savedPlaceIds: List<String>` | 不能只接受 `Set`；按用户选择顺序追加 |
| 重复 placeId | 提交前规范化或由用例定义 | 同一请求不得意外重复创建；规则由 Task 6 测试固定 |
| 部分成功 | 消费 `createdItemIds` 与 `failedPlaceIds` | 每个 ID 分类唯一且与请求可对应 |
| 目标日失效 | 收到 `TargetDayMissing(retainedPlaceIds)` | 不创建项；返回应保留选择 |
| undo | 将 `createdItemIds` 原样交给撤销用例 | 仅删这些 item ID；不按 placeId 全删；保留 SavedPlace 与早先 occurrence |
| 提交并发 | `isSubmitting` 拦截重复事件 | 用例仍应对失败可重复调用，不依赖 UI 唯一性保障 |
| 事务边界 | UI 不猜测回滚情况 | 若沿用逐次 `addItem`，必须准确报告已提交与失败项；不可把已落库项报告为失败 |

### Task 6 当前阻塞

当前树只有 `ItineraryRepository.addItem/deleteItem`，未发现 `AddPlacesToDayUseCase`、`UndoAddedItemsUseCase` 或 `AddPlacesOutcome`。因此 Task 7 可先写纯 reducer/state 测试框架，但提交、partial success 与 undo 集成必须等待 Task 6 最终签名。

## 推荐状态模型与所有权

```kotlin
data class AddToItineraryUiState(
    val selectedPlaceIds: Set<String> = emptySet(),
    val selectionOrder: List<String> = emptyList(),
    val targetDayId: String? = null,
    val editingTarget: AddTarget? = null,
    val isSubmitting: Boolean = false,
    val result: AddPlacesOutcome? = null,
    val undoCreatedItemIds: List<String> = emptyList(),
)
```

计划只列出四个字段，但“按用户选择顺序追加”无法由普通 `Set` 稳健表达；建议以 `selectionOrder` 为提交源，同时保持 `selectedPlaceIds` 用于 O(1) 行选中判断。若团队坚持单字段，可使用具备稳定插入顺序的不可变列表作为唯一真相并派生 Set，不应依赖 `Set` 的遍历顺序。

`editingTarget` 表达当前流程来源，例如 `FromPlacePool` 或 `ForDay(dayId)`；它与 overlay payload 分开持久化，满足设计中的“当前编辑目标”恢复要求。overlay 只表示当前显示步骤，不承载完整业务状态。

### SavedStateHandle 键与恢复

建议由 `TripWorkspaceViewModel` 持有同一流程状态，键使用独立命名空间：

- `workspace.addToItinerary.selectedPlaceIds`
- `workspace.addToItinerary.selectionOrder`
- `workspace.addToItinerary.targetDayId`
- `workspace.addToItinerary.editingTarget`
- `workspace.addToItinerary.overlayStep`（若要求进程恢复后回到原步骤）

恢复顺序：先解码草稿，再等待当前 `days` 与 `SavedPlace` 快照，随后 reconcile：

- 删除已不存在或不属于当前 trip 的 placeId；
- `targetDayId` 不在当前 days 时仅清目标日，不清选择；
- 若选集为空，不能恢复到目标日提交态；
- 不恢复 `isSubmitting=true`，避免进程重建后永久禁用；
- 不恢复 `result`/undo item IDs，避免无法证明原提交仍是最新可撤销动作。

## scheduled 与 selectedPlaceIds 分离

| 维度 | `scheduled` | `selectedPlaceIds` |
|---|---|---|
| 含义 | 地点已有至少一次 itinerary occurrence | 本次加入流程中被用户勾选 |
| 来源 | Room usage count，`count > 0` | UI 事件 + SavedStateHandle |
| 生命周期 | 随数据库变化 | 提交草稿，完成/取消后按规则清理 |
| 是否可同时为 true | 可以；允许同地点再次安排 | 可以 |
| 提交成功后 | usage flow 最终变 true/增加次数 | 对成功项清除 |
| undo 后 | 由剩余 occurrence 决定，可能仍为 true | 不自动重新勾选 |

禁止项：用 `scheduled` 禁止选择、提交后直接把 `scheduled=false`、undo 时按 placeId 删除所有 occurrence、用 selection 覆盖 Task 5 状态文案。

## 状态机

| 当前状态 | 事件 | Guard | 下一状态 | 效果 |
|---|---|---|---|---|
| Idle | `StartForDay(dayId)` | day 有效 | SelectPlaces，预设 target | 打开 `SelectPlacesForDay` |
| Idle | `StartFromPool` | — | SelectPlaces | 打开选择层 |
| SelectPlaces | `TogglePlace(id)` | id 属于当前地点池；非 submitting | 增删选择并维护顺序 | 写 SavedStateHandle |
| SelectPlaces | `Continue` | 选择非空 | SelectTargetDay | 打开选日层 |
| SelectPlaces | `Continue` | 选择为空 | 不变 | 无；按钮应禁用 |
| SelectTargetDay | `SelectDay(id)` | id 在当前 days | 设置 target | 写 SavedStateHandle |
| SelectTargetDay | `Submit` | 选择非空、目标有效、非 submitting | Submitting | 调用 Task 6 add use case |
| 任意选择态 | `DaysChanged(days)` | target 不存在 | SelectTargetDay；target=null；选择保留 | 更新候选日并提示重选 |
| 任意选择态 | `PlacesChanged(places)` | 有选择已失效 | 删除失效选择 | 写 SavedStateHandle；为空时禁用继续/提交 |
| Submitting | 再次 `Submit/Toggle/SelectDay` | isSubmitting | 不变 | 无，避免重复动作 |
| Submitting | `Success(created)` | — | Completed | 清选择/target；保存一次性 undo token；关闭 overlay；反馈成功 |
| Submitting | `PartialSuccess(created, failed)` | — | SelectTargetDay 或 SelectPlaces | 仅保留 failed；target 若仍有效可保留；提供部分成功反馈与 created 的 undo |
| Submitting | `TargetDayMissing(retained)` | — | SelectTargetDay | 保留 retained；清 target；刷新日期并提示重选 |
| Completed | `Undo` | token 非空且未消费 | Undoing | 调用 Task 6 undo(created IDs) |
| Completed | 重复 `Undo` | token 已消费/正在撤销 | 不变 | 无 |
| 任意非提交态 | `Cancel/Back` | — | Idle | 关闭 overlay；是否清草稿必须由产品事件区分：取消清空，系统重建保留 |

## 事件与效果边界

### 事件

- `StartForDay(dayId)`、`StartFromPool`
- `TogglePlace(placeId)`、`Continue`
- `SelectDay(dayId)`、`Submit`
- `DaysChanged(days)`、`PlacesChanged(placeIds)`
- `AddCompleted(outcome)`、`AddFailed(message)`
- `Undo`、`UndoCompleted`、`UndoFailed(message)`
- `Cancel`、`DismissFeedback`

### 一次性效果

- `OpenOverlay(step)` / `CloseOverlay`
- `ShowFeedback(message, undoAvailable)`
- `InvokeAdd(request)`
- `InvokeUndo(createdItemIds)`

不要把 Snackbar/反馈消费标记写入 SavedStateHandle。不要让 Compose Content 直接调用仓储；事件统一进入拥有状态的 ViewModel/reducer。

## 目标日失效规则

目标日可能在选日页、提交前、提交中被删除。应双重防护：

1. UI reconcile：监听 `TripWorkspaceUiState.days`，发现 `targetDayId` 不存在即清空目标并保留选择。
2. 领域确认：提交仍以 Task 6 的 `TargetDayMissing` 为最终权威，覆盖检查与写入之间的竞态。

若提交期间日被删除并返回 partial success，不应由 UI 推断；严格按 Task 6 outcome 展示已创建/失败集合。若 outcome 无法表达“日删除但部分项曾写入”，Task 6 契约需先修订，Task 7 不应猜测。

## partial success 与 undo 语义

- `Success`：全部请求地点产生新 item；清空草稿，undo token 为全部 `createdItemIds`。
- `PartialSuccess`：数据库中已创建项保留；UI 选择仅保留 `failedPlaceIds`，可重试；反馈说明成功数/失败数；undo 仅作用于 `createdItemIds`。
- `TargetDayMissing`：本次不得产生新 item；保留 `retainedPlaceIds`，清空 target。
- 普通异常：若 Task 6 未返回 outcome，Task 7 只能保留原选择并显示失败；Task 6 必须保证异常时没有未报告的部分写入。
- undo 后不恢复原选择，也不删除 SavedPlace。若同一地点提交前已有 occurrence，按新 item ID 撤销后 `scheduled` 仍为 true。
- 新的成功提交应替换旧 undo token；undo token 一次消费。若反馈关闭，是否仍允许 undo 应由产品明确；默认 Snackbar 生命周期内有效。

## 测试矩阵

### JVM 状态/reducer

| 场景 | 断言 |
|---|---|
| 空选择 | Continue/Submit 禁用且无 effect |
| 多选切换 | Set 与 order 同步；取消后顺序稳定 |
| scheduled + selected | 同一行可同时为 true；互不覆盖 |
| 已 scheduled 再选择 | 仍可提交并新增 occurrence |
| 恢复选择 | SavedStateHandle 重建后 IDs 与顺序恢复 |
| 恢复目标日 | 有效 day 恢复；无效 day 清空但选择保留 |
| 恢复编辑目标 | 正确回到来源上下文；非法 payload 降级 Idle |
| 提交互斥 | 连续 Submit 仅发出一个 InvokeAdd |
| 提交时改选/改日 | 被忽略或控件禁用，request 快照不变 |
| Success | 清草稿；记录精确 created IDs；产生反馈 |
| PartialSuccess | 仅失败 place IDs 留在选择；成功 IDs 可撤销 |
| TargetDayMissing | target 清空、选择保留、回到选日 |
| 普通失败 | 原选择/target 保留，isSubmitting 复位 |
| undo 精确性 | 只传 createdItemIds；不传 placeIds |
| undo 重入 | 双击只调用一次 |
| 新成功覆盖旧 token | 只能撤销最新提交 |
| 地点删除 | 从选择及顺序移除；其余选择保留 |
| 全部地点删除 | 继续/提交禁用 |

### Task 6 领域/Room 集成依赖

| 场景 | 断言 |
|---|---|
| 按选择顺序追加 | 新 items 位于末尾且顺序一致 |
| 重复请求 ID | 行为符合固定契约，不重复意外创建 |
| 中途失败 | created/failed 精确，无幽灵写入 |
| 日删除竞态 | 返回 TargetDayMissing 或契约定义的可解释 outcome |
| undo | 仅删除本次 IDs，邻接 RouteLeg 修复 |
| 早先同地点 occurrence | undo 后仍存在 |
| 跨天 | 不生成跨天 RouteLeg |
| SavedPlace | add/partial/undo 全程不删除收藏记录 |

### Compose / Route

| 场景 | 断言 |
|---|---|
| 地点列表 | scheduled 文案与选择控件同时正确显示 |
| Continue | 无选择禁用，有选择启用 |
| 长日期列表 | 内容独立纵向滚动，CTA 可达 |
| 提交中 | 选择、返回策略、提交按钮均防重复 |
| 目标日失效 | 页面仍显示选择，要求重选 |
| partial success | 成功/失败反馈清楚，失败项仍选中 |
| 返回 | 单一 overlay 关闭，不泄漏其他 dialog 状态 |
| 进程恢复 | Route 使用恢复后的单一状态，不二次订阅/重复打开 overlay |
| 280dp/2× 字体 | 行、日期、CTA 不重叠且触控目标至少 48dp |

本预研按要求不运行设备；Compose/Room 设备矩阵仅作为 Task 7 实施验收要求。

## 当前可扩展点

1. `PlacePoolUiState.rows`（Task 5）可在渲染时与 `selectedPlaceIds` 组合；不要把 selection 写回 `SavedPlaceRowUi.scheduled`。
2. `PlacePoolAction` 可新增开始流程/切换选择事件；`PlacePoolViewModel.dispatch` 已是集中入口。
3. `TripWorkspaceViewModel` 已持有 `SavedStateHandle`、days 流及唯一 `WorkspaceOverlay`，适合拥有跨两个 overlay 的 add-flow 状态与目标日 reconcile。
4. `TripWorkspaceRoute` 已集中协调 Place/Itinerary/Workspace action，适合映射一次性 effect；但当前仍允许可选 child ViewModel 或外部 state 两套入口，Task 7 不应再增加第三套状态源。
5. `TripWorkspaceContent` 已接受聚合 state/callback；可增加 add-flow state 与 action，不应在 Content 内 collect Flow。
6. `RoomItineraryRepository.addItem/deleteItem` 已提供单项追加与按 item ID 删除、邻接修复能力，Task 6 可在其上组合，但需明确部分提交契约。

## 与 Task 5 / Task 8 的共享文件冲突地图

| 文件 | Task 5 | Task 7 | Task 8 | 冲突判断 |
|---|---|---|---|---|
| `place/ui/PlacePoolViewModel.kt` | 新增 rows、usage、详情状态 | 加选择入口/映射 | 无计划修改 | **Task 5 → 7 严格串行** |
| `place/ui/PlacePoolSheet.kt` / `SavedPlaceRow.kt` | 重做地点行 | Task 7 选择 UI 很可能接入 | 无 | Task 7 必须基于 Task 5 API；可与 Task 8 并行 |
| `workspace/WorkspaceOverlay.kt` | 使用 LayerMenu/PlaceDetail | 调整 SelectPlaces/SelectTargetDay payload | AddTripDay 已存在并被 Task 8 使用 | **Task 7 与 8 同文件冲突，串行合并** |
| `workspace/TripWorkspaceViewModel.kt` | overlay 协调既有基础 | add-flow、SavedState、日失效 | 计划未列但可能接入 AddTripDay | 逻辑上 7 先稳定；8 不应并行改 coordinator |
| `workspace/TripWorkspaceContent.kt` | 地点池及地图控件改动 | 两个选择 Content 接线 | 日期导航与 AddTripDay 接线 | **高冲突，必须串行或预先切分插槽** |
| `workspace/TripWorkspaceRoute.kt` / `Screen.kt` | Task 5 overlay/detail 接线 | 计划虽漏列但实际必需接入 overlay/effect | 可能接 AddTripDay | 计划文件清单不足；7 完成后 8 再接线 |
| `itinerary/ui/DayItineraryViewModel.kt` | 无 | 消费 Task 6 的方式待定 | 明确修改追加旅行日 | Task 7 避免把 add-flow 塞入该 VM，可降低冲突 |
| `itinerary/ui/*Content.kt` | 无 | 新建两个 Content | 新建两个不同 Content | 可并行创建，但最终 Workspace 接线串行 |
| `place/ui/PlacePoolFlowTest.kt` | Task 5 已扩展 | Task 7 再扩展 | 无 | 7 基于 Task 5 测试串行 |

## 可并行与必须串行边界

### 可并行

- Task 6 完成稳定接口后，Task 7 的纯 reducer/JVM 测试与两个新 Content 的无状态布局可并行开发。
- Task 8 的 `AddTripDayContent`、`VerticalDayNavigation` 独立新文件可与 Task 7 新 Content 并行，但不能同时接入 Workspace。
- Task 7 的 Compose 布局测试可与 domain undo/add Room 测试并行执行。

### 必须串行

1. Task 5 最终状态模型进入目标分支后，Task 7 才能修改 `PlacePoolViewModel`/地点行。
2. Task 6 的 outcome、ID 类型、partial success 与 undo 语义冻结后，Task 7 才能完成提交状态。
3. `WorkspaceOverlay`、`TripWorkspaceContent`、Route/Screen 接线由 Task 7 先完成并通过回归，再由 Task 8 增加 AddTripDay 导航。
4. 目标日失效测试必须在 Task 7 状态机完成后，Task 8 的新增日行为再复用同一 days reconciliation，不能另建状态。
5. Task 7 不得与正在进行的 Task 5/8 分支直接互相 cherry-pick 部分提交；应在明确基线顺序上整合，避免把同一 sealed `when` 与大参数列表反复手工解冲突。

## 实施前必须裁决的风险

1. **ID 类型严重漂移**：计划是 Long，生产模型是 String；现有 hash-to-Long 会碰撞且不可逆。Task 7 overlay 和 state 必须改用 String，跨天移动应使用独立 overlay 类型或清晰 intent。
2. **Task 6 缺失**：当前无法验证 partial success、TargetDayMissing、undo；不能用 UI 自造近似 outcome。
3. **选择顺序丢失**：`Set` 不足以保证“按用户选择顺序追加”；需显式顺序字段或以 List 为真相。
4. **批量非原子**：现有 `addItem` 每项独立事务。Task 6 必须保证异常与 partial outcome 一致，否则 UI 会保留错误选择或遗漏 undo。
5. **overlay 恢复不完整**：当前 overlay 不进 SavedStateHandle。仅恢复选择而回到 None 会产生隐藏草稿；需决定并测试恢复 overlay step。
6. **状态所有权重叠**：设计称 PlacePoolViewModel 拥有选择，但跨页 target/submit 更适合 Workspace；建议 Workspace 拥有流程，PlacePool 只派发选择事件和渲染派生状态，避免双写。
7. **旧 SelectTargetDay 语义冲突**：当前用于移动 itinerary item；Task 7 不得复用其 `placeIds` 解释。建议拆成 `SelectMoveTargetDay(itemId: String)` 与 `SelectAddTargetDay`。
8. **Task 7 文件清单漏项**：实际 overlay 渲染在 `TripWorkspaceScreen.kt`，事件协调在 `TripWorkspaceRoute.kt`；只改计划列出的文件无法交付完整流程。
9. **Task 5 尚未进入当前基线**：预研引用的是 `2be3950` 最终模型；实施时须先确认最终集成 SHA，防止依据游离提交开发。
10. **undo 生命周期**：若只用 Snackbar，进程死亡后不应恢复旧 token；若产品要求持久 undo，必须由领域层提供提交批次标识，而非恢复 item ID 列表后盲删。

## 建议实施顺序

1. 集成并复核 Task 5 最终提交；确认 `scheduled`/usage flow 契约。
2. 完成 Task 6，冻结 String ID、部分成功和精确 undo 契约。
3. 先写 `AddToItineraryStateTest`，覆盖恢复、目标日失效、partial、undo 精确性。
4. 实现单一 add-flow owner 与 SavedStateHandle reconcile。
5. 拆分 overlay intent，移除加入流程中的 hash Long payload。
6. 实现两个无状态 Content，再在 Route/Screen/WorkspaceContent 单点接线。
7. 跑 JVM、Compose、Room 回归；Task 8 最后基于该状态模型接入共享 Workspace 文件。

本次仅预研和文档提交；未修改生产代码、未运行设备、未派生 agent、未执行 `graphify update`。
