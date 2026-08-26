# Easy Trip 搜索与地点闭环 UI 收敛设计

日期：2026-08-26

## 1. 目标

收敛搜索、搜索地图详情、收藏、地点池、地点详情、备注与标签、单地点加入行程和删除影响确认，形成完整且一致的地点闭环。

本阶段将全屏搜索结果与地图详情模式统一在搜索流程内；地点池使用 `＋` 和 `···` 两个固定操作；详情采用地图上半屏 + 地点详情下半屏，并复用现有收藏、编辑、删除和加入行程业务能力。

## 2. 已确认产品规则

### 2.1 搜索结果

- 点击结果整行：进入搜索地图详情模式。
- 点击右侧收藏按钮：收藏或取消收藏。
- 收藏成功后停留在搜索页，可连续收藏。
- 搜索结果不提供加入行程入口。

### 2.2 搜索地图详情模式

- 全屏搜索列表切换为上半地图、下半地点详情面板。
- 地图聚焦并标记当前选中的地点。
- 地图允许缩放和平移。
- 地图上其他 POI 不可点击，不切换当前详情。
- 提供“回到选中地点”按钮。
- 用户平移地图不改变当前地点详情。
- 返回后恢复全屏搜索结果、关键词和列表滚动位置。

### 2.3 地点池行操作

- `＋`：只添加当前地点，直接进入“选择目标日”。
- `···`：编辑地点、删除地点。
- 原有批量加入行程入口继续保留。

### 2.4 地点详情

- 使用底部详情面板，保留地图上下文。
- 默认只读展示名称、地址、收藏状态、备注和标签。
- 详情内不提供加入行程。
- 从搜索进入时可收藏或取消收藏。
- 从地点池进入时可编辑备注、标签和删除地点。
- 搜索候选尚未收藏时没有 SavedPlace 记录，只展示基础信息和收藏操作；收藏成功后才开放备注、标签编辑。

### 2.5 备注与标签

- 详情面板内切换只读和编辑态。
- 预设标签多选。
- 支持输入并创建新标签，新建后自动选中。
- 标签创建前去除首尾空格，并按规范化名称去重。
- 单地点最多选择 8 个标签。
- 单标签最多 12 个中文字符或 24 个普通字符。
- 达到 8 个后仍可取消已有标签，但不能继续选择或新建。
- 保存成功回到只读态。
- 保存失败保留备注、标签和新标签输入。
- 保存中锁定关闭、返回和重复提交。

### 2.6 删除与取消收藏

- 未被行程使用：可直接删除或取消收藏。
- 已被行程使用：显示影响确认。
- 确认层展示将删除的行程项数量和受影响 RouteLeg 数量，并说明收藏记录也会删除。
- 用户确认后执行现有原子删除流程。
- 取消确认不改变数据。

## 3. 范围

### 3.1 本阶段包含

- 搜索初始、加载、结果、无结果和网络失败。
- 搜索结果点击与收藏操作。
- 搜索地图详情模式。
- 搜索模式返回和状态恢复。
- 地点池普通、空态和搜索返回态。
- 地点池 `＋` 单地点加入行程。
- 地点池 `···` 编辑与删除。
- 地点详情只读与面板内编辑态。
- 标签规范化、长度、数量和去重校验。
- 删除/取消收藏影响摘要和确认。
- 提交中、失败和取消恢复。

### 3.2 本阶段不包含

- 搜索结果直接加入行程。
- 点击地图其他 POI 切换详情。
- 独立标签管理页面。
- 修改 SavedPlace 与 ItineraryItem 的身份关系。
- 工作台壳层、行程时间线或旅行设置重做。
- Profile、分享等新产品功能。
- 极端小窗、横屏、分屏和 2× 字体专项适配。
- 每个小改的即时真机验收；真机统一留到候选批次。

## 4. 页面与模式

### 4.1 搜索结果模式

```text
SearchResults
├── 返回工作台
├── 搜索框
├── 初始 / 加载 / 结果 / 无结果 / 失败
└── 结果行
    ├── 点击整行 → SearchMapDetail
    └── 收藏按钮 → 收藏 / 取消收藏
```

### 4.2 搜索地图详情模式

```text
SearchMapDetail
├── Map Region
│   ├── 当前地点唯一标记
│   ├── 缩放 / 平移
│   └── 回到选中地点
└── PlaceDetailPanel
    ├── 名称 / 地址
    ├── 收藏状态
    ├── 备注 / 标签
    └── 收藏、编辑、保存、取消、删除
```

搜索地图详情是搜索流程内的显示模式，不是工作台 Sheet 档位，也不是新的顶层导航目的地。

### 4.3 地点池

```text
[类别图标] 地点名称                    ＋   ⋮
           地址
           标签 · 已排入 N 次
```

- 名称最多两行。
- 地址最多两行。
- 标签与安排状态不能挤压操作区。
- `＋` 和 `···` 各使用独立 40dp 触控区，间距至少 4dp。

## 5. 状态模型

### 5.1 搜索显示模式

```kotlin
sealed interface SearchDisplayMode {
    data object Results : SearchDisplayMode
    data class MapDetail(val poiId: String) : SearchDisplayMode
}
```

只保存稳定 `poiId`，不在模式中复制候选详情或收藏状态。

### 5.2 PlaceSearchUiState 扩展

建议扩展：

```kotlin
data class PlaceSearchUiState(
    val search: PlaceSearchState = PlaceSearchState(),
    val displayMode: SearchDisplayMode = SearchDisplayMode.Results,
    val savedPoiIds: Set<String> = emptySet(),
    val savedPlacesByPoiId: Map<String, SavedPlace> = emptyMap(),
    val collectionBusyPoiIds: Set<String> = emptySet(),
    val pendingCollectionRemoval: PendingCollectionRemoval? = null,
    val collectionError: String? = null,
    val detailDraft: PlaceDetailEditState? = null,
    val shouldNavigateBack: Boolean = false,
)
```

当前候选通过 `poiId` 从 `search.results` 派生；已收藏详情通过 `savedPlacesByPoiId` 派生。

若恢复后 `displayMode` 指向的 `poiId` 不在当前结果中，安全回到 `Results`。

### 5.3 详情编辑状态

```kotlin
data class PlaceDetailEditState(
    val placeId: String,
    val note: String,
    val selectedTagNames: Set<String>,
    val newTagInput: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)
```

编辑状态只用于已收藏地点。`placeId` 绑定 SavedPlace；不能用搜索候选直接创建备注或标签草稿。

### 5.4 纯 UI 状态

Composable 可持有：

- 搜索结果 `LazyListState`。
- 地图相机临时位置。
- `···` 菜单展开 Boolean。
- 输入焦点。

ViewModel 持有：

- 搜索 query/results/phase。
- 当前模式和选中 poiId。
- 收藏状态与 busy/error。
- 编辑草稿、保存和影响确认。

## 6. 导航与返回

### 6.1 进入详情

新增 action：

```kotlin
PlaceSearchAction.OpenDetail(poiId)
```

要求：

- 只允许打开当前结果中的候选。
- 候选必须有可定位坐标；无坐标时展示信息但地图不发起无效聚焦。
- 进入详情不重新搜索、不清空 query、不替换结果列表。

### 6.2 返回优先级

1. 影响确认打开：先关闭确认。
2. 详情编辑态且未提交：先取消编辑；若交互需要放弃提示，提示只针对真实脏草稿。
3. 搜索地图详情模式：回到搜索结果模式。
4. 搜索结果模式：返回工作台。

保存、删除或取消收藏提交中时，系统 Back、顶部 Back、手势关闭和外部点击均锁定。

### 6.3 恢复

- query 继续使用 `SavedStateHandle`。
- `displayMode` 与选中 poiId 写入 `SavedStateHandle`。
- 列表滚动位置由 `LazyListState` 的可保存状态恢复。
- 进程恢复后候选无效时退回 Results。
- 返回工作台仍只发布当前搜索会话的 `recentlyCollectedPoiIds` 一次。

## 7. 地图详情布局

### 7.1 地图区域

- 占详情模式可用高度的上半部分；具体高度基于窗口约束，不固定设备像素。
- 只渲染当前候选标记。
- 允许缩放和平移。
- 禁止地图 POI 点击和其他标记点击切换详情。
- 首次进入时发起一次相机聚焦。
- 后续重组不覆盖用户平移。
- “回到选中地点”显式重新聚焦。

### 7.2 地图 host

复用现有 AMap host/lifecycle/consent 能力，不复制 MapView 生命周期控制器。

搜索详情应构造最小 `MapUiModel`：

- 一个搜索候选 marker。
- 一个初始 viewport request。
- 无路线。
- 无其他地点池 marker。

地图失败时详情面板仍可操作收藏和已收藏地点编辑；地图失败不阻断本地数据操作。

### 7.3 详情面板

- 使用 24dp 上圆角、统一阴影和 20dp 水平 padding。
- 默认半屏；内容过长可滚动。
- 面板不复用工作台三档状态机，避免两个导航域互相污染。
- IME 出现时保存和取消保持可达。

## 8. PlaceDetailPanel

### 8.1 来源

```kotlin
enum class PlaceDetailSource {
    SEARCH,
    PLACE_POOL,
}
```

`SEARCH`：

- 未收藏：基础信息 + 收藏。
- 已收藏：基础信息、备注、标签、收藏/取消收藏、编辑。

`PLACE_POOL`：

- 基础信息、备注、标签、编辑、删除。
- 不显示收藏按钮的重复语义；地点池中的对象必然已收藏。

两种来源均不显示加入行程。

### 8.2 只读态

展示：

- 名称。
- 地址；空地址显示“地址暂不可用”。
- 收藏状态。
- 备注；空备注显示弱提示。
- 标签；无标签显示弱提示。

### 8.3 编辑态

- 备注输入框。
- 预设标签列表。
- 新标签输入和创建按钮。
- 保存与取消。
- 错误信息。

保存中：

- 禁用标签、输入、保存和取消。
- 锁定详情关闭与返回。
- 保存按钮显示明确进行中状态。

## 9. 标签规则

### 9.1 规范化

```kotlin
internal fun normalizePlaceTagName(raw: String): String = raw.trim()
```

去重按规范化名称执行。中文和英文大小写规则保持现有数据语义；本阶段不引入大小写折叠，避免改变已有标签身份。

### 9.2 长度

定义用户可理解长度：

```kotlin
internal fun placeTagUnits(value: String): Int =
    value.codePoints().sumOf { codePoint ->
        if (Character.UnicodeScript.of(codePoint) in cjkScripts) 2 else 1
    }
```

限制为 24 units：

- 中文、日文汉字、韩文等 CJK 字符按 2 units，最多 12 个。
- 普通拉丁字符按 1 unit，最多 24 个。
- Emoji 和其他扩展字符按 Unicode code point 计 1 unit，避免 UTF-16 surrogate 被算两次。

### 9.3 选择数量

- 最多 8 个规范化后非空且互异的标签。
- 达到 8 个后可取消已有标签。
- 达到上限后选择或创建新标签返回明确错误，不改变当前选择。

### 9.4 校验结果

```kotlin
sealed interface PlaceTagValidation {
    data class Valid(val normalized: String) : PlaceTagValidation
    data object Empty : PlaceTagValidation
    data object Duplicate : PlaceTagValidation
    data object TooLong : PlaceTagValidation
    data object SelectionLimitReached : PlaceTagValidation
}
```

校验使用纯函数，并由 ViewModel 转为中文提示。

### 9.5 持久化

继续调用现有：

```kotlin
repository.updateDetails(placeId, note, tagNames)
```

Repository/DAO 继续负责标签创建、关联与去重。UI/ViewModel 在调用前保证规范化和数量限制，但不改变数据库 schema。

## 10. 地点池行

### 10.1 组件接口

```kotlin
@Composable
fun SavedPlaceRow(
    place: SavedPlaceRowUi,
    onQuickAdd: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
)
```

行内菜单仅持有展开 Boolean。菜单 action 直接绑定当前 `place.id`，不在 ViewModel 增加 selected menu place。

### 10.2 `＋` 单地点加入

新增地点池 action：

```kotlin
PlacePoolAction.StartAddSingle(placeId)
```

工作台收到后复用现有 `AddToItineraryViewModel`：

1. 初始化有效 days/places。
2. 选中当前 placeId。
3. 直接进入 `SELECT_TARGET_DAY`。
4. 打开现有目标日选择 overlay。

建议新增明确入口：

```kotlin
fun startForPlace(placeId: String)
```

其状态必须满足：

```kotlin
selectedPlaceIds = setOf(placeId)
editingTarget = AddToItineraryEditingTarget.FromPlacePool
step = AddToItineraryStep.SELECT_TARGET_DAY
```

没有旅行日时复用现有新增旅行日引导，不创建隐式 day。

提交、PartialSuccess、TargetDayMissing、Undo 与 generation 逻辑继续复用现有实现。

### 10.3 批量加入

原 `StartAddToItinerary` 继续进入选择多个地点步骤，不因单地点入口改变。

## 11. 收藏、取消收藏和删除

### 11.1 收藏

- 只锁定当前 poiId 的收藏按钮。
- 其他搜索结果仍可操作。
- 收藏成功后保持 SearchMapDetail。
- `savedPlacesByPoiId` 更新后，详情从 candidate-only 自动升级为 saved detail。
- 收藏失败保持详情和错误信息。

### 11.2 取消收藏与删除

统一影响摘要：

```kotlin
data class PlaceDeletionImpact(
    val itineraryItemCount: Int,
    val routeLegCount: Int,
)
```

现有 `usageCount(placeId)` 只能提供行程项使用数量，不能精确提供 RouteLeg 数量。为了满足已确认确认文案，本阶段增加只读影响查询：

```kotlin
suspend fun deletionImpact(placeId: String): PlaceDeletionImpact
```

Room 实现使用事务一致快照统计：

- 该 SavedPlace 的 ItineraryItem 数量。
- 删除这些 item 将影响的 RouteLeg 数量，按实际将被删除/重建的唯一 leg id 去重。

删除写路径仍使用现有原子 `deletePlaceAndReferences(placeId)`，不创建第二删除流程。

### 11.3 确认规则

```kotlin
impact.itineraryItemCount == 0 && impact.routeLegCount == 0
```

可直接执行。

任一数量大于 0：

- 展示影响确认。
- 明确收藏记录也会删除。
- 确认前不调用删除。
- 取消不修改数据。
- 提交中锁定关闭和重复确认。
- 失败保留确认状态和错误信息。

## 12. 搜索状态视觉

### 12.1 初始

- 搜索提示。
- 最近查询区域仅在已有数据能力时展示；本阶段不新增搜索历史存储。

### 12.2 加载

- 保留搜索框和 query。
- 显示“正在搜索地点”。
- 使用 live region 或等效状态播报。

### 12.3 结果

- 结果列表可滚动。
- 整行可点击打开详情。
- 右侧收藏按钮有 saved/busy/disabled 语义。
- 当前行 busy 不锁定其他行。

### 12.4 无结果

- 简洁插画。
- “没有找到相关地点”。
- 修改关键词或清空搜索操作。

### 12.5 网络失败

- 错误图标、说明和重试。
- 不用隐私未授权、地图失败或空结果冒充网络失败。
- 重试保留 query。

## 13. 数据流

### 13.1 搜索详情

```text
PlaceSearchViewModel
  ├─ PlaceSearchReducer → query/results/phase
  ├─ SavedPlaceRepository → saved places/tags
  ├─ SearchDisplayMode → selected poiId
  ├─ PlaceDetailEditState → saved detail draft
  └─ PendingCollectionRemoval → impact confirmation
          ↓
PlaceSearchContent
  ├─ Results
  └─ SearchMapDetail
       ├─ search map host
       └─ PlaceDetailPanel
```

### 13.2 地点池单地点加入

```text
SavedPlaceRow ＋
  ↓ StartAddSingle(placeId)
TripWorkspaceRoute
  ↓ AddToItineraryViewModel.startForPlace(placeId)
SELECT_TARGET_DAY
  ↓ existing submit
AddPlacesToDayUseCase
```

### 13.3 删除

```text
request remove/delete
  ↓ deletionImpact(placeId)
zero impact → existing atomic delete
non-zero → Pending confirmation
  ↓ confirm
existing atomic deletePlaceAndReferences(placeId)
```

## 14. 错误与并发

- 搜索请求继续使用 reducer 的 cancellation/generation 规则。
- 收藏、取消收藏按 poiId single-flight。
- 详情保存按 placeId + generation 约束旧 completion，切换详情后旧保存不得关闭新详情。
- 删除影响准备可取消；新地点请求不得被旧影响结果覆盖。
- 保存、删除、取消收藏的 `CancellationException` 必须重抛。
- 失败状态保留当前模式、候选、草稿和确认上下文。

## 15. 可访问性

- 搜索结果整行使用 Button 语义和“查看地点详情”。
- 收藏按钮使用独立描述，不与整行点击合并。
- 地图标记包含地点名称；装饰地图元素清除语义。
- “回到选中地点”使用明确描述。
- 详情面板标题具备 heading 语义。
- `＋` 描述为“将 <地点名> 添加到行程”。
- `···` 描述为“更多 <地点名> 操作”。
- 标签使用 selected 语义；达到上限的未选标签使用 disabled 语义。
- 加载、保存、删除和错误反馈使用 live region 或等效播报。

## 16. 自动化验收

### 16.1 搜索与模式恢复

- 结果行点击进入 `MapDetail(poiId)`。
- 收藏按钮点击不进入详情。
- 进入详情不清空 query/results。
- 返回恢复 Results 和列表位置。
- 无效 poiId 恢复到 Results。
- 系统 Back 和顶部 Back 遵循同一优先级。
- 返回工作台只发布一次本次会话收藏集合。

### 16.2 地图详情

- 只渲染当前候选 marker。
- 其他 POI 点击不改变详情。
- 用户平移后普通重组不重置相机。
- 点击重新定位恢复当前候选 viewport。
- 地图失败不阻断收藏和已收藏地点编辑。

### 16.3 收藏与删除

- 收藏后保持详情，saved 状态同步。
- 仅当前结果 busy。
- 零影响可直接删除。
- 非零影响显示精确 item/leg 数量。
- 确认前 repository 无删除调用。
- 取消确认无副作用。
- 失败保留上下文。

### 16.4 标签

- trim 后空值失败。
- 规范化名称去重。
- CJK 12 字允许，13 字拒绝。
- 普通字符 24 个允许，25 个拒绝。
- 最多选 8 个。
- 达到上限仍可取消已有标签。
- 新标签成功创建并自动选中。
- 保存失败保留全部草稿。

### 16.5 地点池

- `＋` 只选择当前 placeId 并进入目标日。
- 不进入多选页。
- 无旅行日进入现有新增日引导。
- `···` 仅包含编辑和删除。
- 批量加入入口仍进入 SELECT_PLACES。
- 长文本不挤出两个 40dp 操作区。

### 16.6 门禁

每个实施批次：

- 搜索 reducer/ViewModel JVM tests。
- 地点池 ViewModel/Room tests。
- 标签 validator JVM tests。
- AddToItinerary JVM tests。
- focused Compose tests。
- `lintDebug`、`assembleDebug`、`assembleDebugAndroidTest`。

候选批次：

- `V1ScenarioCatalogTest` 47/47。
- `V1FullUiAcceptanceTest` 47/47。

## 17. 人工验收

统一留到后续 Mate 60 Pro 候选批次：

- 全屏搜索结果到地图半屏详情。
- 地图缩放、平移、重新定位。
- 连续收藏和返回地点池。
- 长地点名、长地址、8 个标签。
- 地点池 `＋` 单地点加入。
- `···` 编辑、删除和影响确认。
- 网络失败、保存失败和取消收藏失败。

## 18. 实施批次建议

### P1：搜索模式与状态恢复

- SearchDisplayMode。
- 结果点击和返回。
- query/results/list position 恢复。

### P2：搜索地图详情

- 地图 host、当前 marker、viewport。
- 地图 + 半屏详情布局。
- 地图失败降级。

### P3：统一详情与标签编辑

- PlaceDetailPanel。
- 只读/编辑态。
- 标签 validator 和保存恢复。

### P4：收藏与删除影响

- 收藏/取消收藏。
- PlaceDeletionImpact 精确统计。
- 确认、失败和 single-flight。

### P5：地点池 `＋/···`

- 单地点加入目标日。
- 编辑/删除菜单。
- 批量入口回归。

### P6：候选回归

- focused tests。
- Catalog/Full UI。
- 候选真机清单。

## 19. 完成定义

本阶段完成需满足：

- 搜索列表和地图半屏详情模式可稳定切换与恢复。
- 地图只聚焦当前地点，其他 POI 不改变详情。
- 收藏、取消收藏、编辑、删除状态一致且失败可恢复。
- 标签数量、长度和规范化规则落实。
- 地点池 `＋` 单地点直达目标日，`···` 只含编辑/删除。
- 批量加入流程保持。
- 精确影响确认在删除前展示 item/leg 数量。
- 自动化和 47 场景回归通过。
- 真机验收未执行时明确记录为 DEFERRED。
