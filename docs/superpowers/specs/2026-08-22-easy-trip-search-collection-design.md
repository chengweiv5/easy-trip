# Easy Trip 搜索与收藏交互设计

## 1. 背景与目标

当前工作台已具备搜索结果、搜索 Tab、收藏地点、地图 marker、地图范围和级联删除能力，但交互仍存在以下断点：搜索被放在抽屉 Tab 内；工作台搜索框可直接输入；已收藏结果不可从搜索列表取消；高德底图 POI 不能进入统一的地点卡片与收藏流程；marker 的“收藏身份、行程序号、聚焦状态”混在 key 和临时样式中；地点池长列表没有可见滚动位置提示。

本设计在保留当前未提交视觉升级的前提下，完成搜索入口、收藏切换、地图地点卡片、marker 身份和地点池滚动指示。实现不得回退或覆盖工作树中已有的顶部栏、图层菜单、地图范围控件、抽屉层级、卡片时间轴、主题和 adaptive icon 改动。

目标：

1. 抽屉仅保留“地点池”和“每日行程”两个 Tab。
2. 工作台地图右下角保留只读搜索入口，点击进入独立导航搜索页。
3. 选择搜索结果后清空搜索页状态，返回工作台并聚焦、高亮该地点。
4. 搜索结果和高德底图 POI 共用收藏/取消收藏规则与影响确认。
5. marker 同时、稳定表达收藏身份、行程序号和焦点状态。
6. 地点池超出一屏时显示只读纵向 scrollbar thumb。

## 2. 非目标

- 不修改 Room entity、DAO schema、数据库版本或 schema JSON。
- 不改变现有 `deletePlaceAndReferences` 的事务与级联语义。
- 不新增云同步、导航、地点详情网络接口或自定义落点。
- 不替换高德 SDK，不将高德返回类型泄漏到 UI/domain 边界。
- 不重做当前工作树中已有的大量视觉改动。
- 不在工作台入口直接编辑或提交查询。

## 3. 当前实现基线

- `TripWorkspaceScreen` 当前有 `SEARCH/PLACES/ITINERARY` 三个抽屉 Tab，地图底部直接渲染可输入 `PlaceSearchField`。
- `AppNavigation` 当前在工作台组合 `PlaceSearchResults`，搜索点击直接调用 `TripWorkspaceViewModel.focusSearchResult`。
- `PlacePoolViewModel` 已持有搜索、已收藏 POI ID、编辑和删除确认状态。
- `SavedPlaceRepository.usageCount` 与 `deletePlaceAndReferences` 已存在；`RoomSavedPlaceRepository` 已在事务中删除地点出现项并重建受影响路线。
- `MapUiModelMapper` 已支持搜索 marker、范围 marker、同坐标 occurrence 合并和聚焦；但聚焦时会改写 marker key，不能稳定保留收藏身份。
- `RealAmapMapHost` 已有自有 marker 点击回调和 marker bitmap 生成；尚未接入高德底图 POI 点击。
- `SavedPlacesContent` 使用 `LazyColumn`，尚无 scrollbar thumb。

## 4. 信息架构与导航

### 4.1 工作台

工作台抽屉的 Tab 固定为：

```kotlin
enum class WorkspaceTab { PLACES, ITINERARY }
```

删除 `SEARCH` 枚举值、Tab 控件与对应内容分支。已有持久化状态若恢复到字符串 `SEARCH`，迁移为 `PLACES`，不能因 `valueOf` 崩溃。

地图上的搜索入口位于底部右侧：

- 白底；
- 高 40dp；
- 宽屏宽度为可用宽度的 `1/5`；
- 仅显示放大镜图标，无提示文字、query 或占位符；
- 整体可点击，语义为按钮，`contentDescription = "搜索地点"`；
- 工作台入口只读，不获取文本输入焦点，不持有 query。

窄屏仍应保持可点击的最小宽度与 48dp 触控目标；40dp 是可见容器高度，外围语义点击区域可通过父容器满足触控要求。宽屏验收以父宽度的 20% 为准。

### 4.2 独立搜索页

新增导航目的地：

```text
trips/{tripId}/search
```

页面包含返回按钮、输入框和搜索结果列表。搜索状态属于该 back stack entry；离开页面即不带 query/results 回工作台。

结果行有两个独立动作：

- 点击结果主体：选择并返回工作台；
- 点击尾部“收藏/已收藏”：切换收藏状态，不触发结果选择。

选择结果的顺序必须是：

1. 将 POI ID、名称、地址、坐标写入前一个工作台 back stack entry 的 `SavedStateHandle`；
2. 调用搜索 ViewModel 的 `clearSearch()`，取消当前请求并清空 query/results/error/searching；
3. `popBackStack()` 返回工作台；
4. 工作台消费一次 selection，聚焦并高亮 marker，再删除导航结果 key，防止重组或二次进入重复聚焦。

缺坐标结果可在列表显示，但不能选择、聚焦或收藏。

## 5. 收藏与取消收藏

### 5.1 统一状态机

搜索结果和地图地点卡片必须调用同一个 ViewModel 入口，避免两套删除规则：

```kotlin
sealed interface CollectionRequestResult {
    data object Saved : CollectionRequestResult
    data object Removed : CollectionRequestResult
    data class ConfirmationRequired(val place: SavedPlace, val usageCount: Int) : CollectionRequestResult
}

fun toggleCollection(candidate: PlaceCandidate)
fun confirmCollectionRemoval()
fun dismissCollectionRemoval()
```

按 `candidate.poiId` 与当前旅行已收藏地点匹配：

- 未收藏：调用现有 `repository.save(tripId, candidate)`；
- 已收藏且 `usageCount == 0`：直接调用现有 `deletePlaceAndReferences(place.id)`，不弹确认；
- 已收藏且 `usageCount > 0`：显示影响确认；
- 用户确认：调用同一 `deletePlaceAndReferences(place.id)`；
- 用户取消：不改变地点、行程项或路线。

确认文案明确给出 `usageCount`，说明会级联删除对应行程项并重建/删除受影响路线。并发点击期间禁用该 POI 的切换动作，避免重复保存或删除。

此处“取消收藏”复用现有删除语义；不引入软删除字段，也不复制级联算法。

### 5.2 搜索结果

已收藏结果的尾部按钮可点击；点击后执行统一状态机。`usageCount == 0` 时列表通过 repository flow 自动变回“收藏”；`usageCount > 0` 时保持当前状态直到用户确认。

### 5.3 高德底图 POI

`AmapMapHost` 新增高德底图 POI 点击回调。SDK 类型在 host 内转换为应用模型：

```kotlin
data class MapPoiUi(
    val poiId: String?,
    val name: String,
    val address: String,
    val point: GeoPoint,
)
```

点击底图 POI 只打开地点卡片，不立即收藏或取消。地点卡片显示名称、地址和当前收藏状态；用户再点击收藏动作。若 SDK 未提供稳定 POI ID，卡片仍可展示和聚焦，但收藏动作禁用并说明无法收藏。

地图上的自有 marker 点击继续展示 marker/occurrence 卡片；底图 POI 与自有 marker 回调必须分开，不能把 route label 当地点。

## 6. Marker 模型与视觉

### 6.1 稳定身份模型

marker key 只用于 overlay identity，不能因聚焦而从 `place-*` 改成 `search-*`。新增显式视觉模型：

```kotlin
enum class MapMarkerKind { UNSAVED_SEARCH, SAVED_PLACE_POOL, SAVED_ITINERARY }

data class MapMarkerUi(
    val key: String,
    val point: GeoPoint,
    val label: String,
    val occurrences: List<OccurrenceUi>,
    val kind: MapMarkerKind,
    val badgeText: String? = null,
    val isFocused: Boolean = false,
)
```

聚焦是正交状态：只增加高亮描边/尺寸，不改变 `kind`、收藏图标、badge 或 key。

### 6.2 各范围样式

- `PLACE_POOL`：已收藏地点使用纯收藏图标，不显示编号；未收藏搜索地点使用普通 marker。
- `SINGLE_DAY`：已收藏地点使用收藏图标，图标内显示该地点当天出现顺序。
- `WHOLE_TRIP`：已收藏地点使用收藏图标，图标内显示跨天展开后的连续顺序。
- 聚焦：在原图标外加高亮描边并适当放大；不得用一个通用圆点覆盖原身份。

同坐标重复出现的 badge 文本规则由 `fun formatOccurrenceBadge(orders: List<Int>): String` 表达：

- 1 次：`1`
- 2 次：`1·4`
- 3 次：`1·4·7`
- 超过 3 次：`1 +3`；一般规则为 `"${orders.first()} +${orders.size - 1}"`。

orders 按实际可见行程顺序排序。marker 详情仍保留完整 occurrence，不因 badge 截断而丢数据。

搜索结果与已收藏地点同 POI 或同坐标时只渲染一个 marker，优先保留已收藏 marker 的 key/kind/badge；若该搜索结果被聚焦，则在这个已收藏 marker 上设置 `isFocused = true`。

## 7. 地点池滚动条

`SavedPlacesContent` 持有一个 `LazyListState`，在列表右侧叠加只读 thumb：

- 内容不超过 viewport 时隐藏；
- 内容超过一屏时显示；
- thumb 高度按可见内容占总内容比例计算，并设置最小可读高度；
- thumb offset 随第一个可见 item 及其像素偏移更新；
- 不注册 drag、click 或 scroll 手势，不可拖动；
- 标签筛选行也计入列表 item 数，计算使用 `LazyListLayoutInfo.totalItemsCount`。

几何计算提取为纯函数，Compose 仅负责读取 layout info 和绘制，以便 JVM 边界测试。

## 8. 架构与数据流

### 8.1 组件职责

- `AppNavigation`：声明独立搜索 route；传递一次性导航选择结果；不持有业务收藏规则。
- `PlacePoolViewModel`：搜索 reducer、收藏切换、取消收藏影响确认、搜索清理；搜索页与地点卡片复用。
- `TripWorkspaceViewModel`：工作台 Tab/范围/日期/sheet、一次性搜索 selection 消费、地图聚焦、当前地点卡片。
- `MapUiModelMapper`：从收藏地点、行程 occurrence、搜索焦点生成稳定 marker identity 与 badge。
- `AmapMapHost`：增量渲染 marker、应用聚焦描边、把高德底图 POI 转成 `MapPoiUi`。
- `SavedPlaceRepository`/`PlaceService`：继续提供 usage count 和现有事务删除；不扩展 schema。

### 8.2 搜索选择数据流

```text
工作台只读入口点击
  -> navigate(trips/{tripId}/search)
  -> SearchScreen 输入并展示 PlacePoolViewModel.search
  -> 点击可定位结果
  -> previousBackStackEntry.savedStateHandle 写 SearchSelectionPayload
  -> clearSearch + popBackStack
  -> 工作台消费 payload 并立即 remove key
  -> TripWorkspaceViewModel.focusSearchResult
  -> MapUiModelMapper 标记 isFocused
  -> AmapMapHost 单次移动相机并绘制高亮描边
```

### 8.3 收藏切换数据流

```text
搜索结果按钮 / 底图 POI 卡片按钮
  -> PlacePoolViewModel.toggleCollection(candidate)
  -> 当前 savedPlaces 按 amapPoiId 匹配
  -> 未收藏: repository.save
  -> 已收藏: repository.usageCount
       -> 0: repository.deletePlaceAndReferences
       -> >0: pendingRemoval + confirmation dialog
  -> confirm: repository.deletePlaceAndReferences
  -> Room flows 刷新地点池、搜索按钮、marker、行程和路线
```

## 9. 状态所有权与恢复

- 工作台的日期、Tab、范围、sheet 和聚焦 POI 继续由 `TripWorkspaceViewModel` 的 `SavedStateHandle` 恢复。
- 旧的 `workspace.tab = SEARCH` 恢复为 `PLACES`；未知值同样安全回退。
- 搜索页 query/results/error/searching 不写 Room；页面重建时可恢复 query 并重新搜索，但一旦选中结果或主动离开，调用 `clearSearch()`。
- 导航 selection 是一次性事件。工作台消费后删除 key；进程重建若 key 尚未消费，可消费一次，已消费则不得再次移动相机。
- 聚焦 POI ID 与坐标保留当前恢复语义。若搜索结果消失但该点已收藏，允许通过收藏地点匹配维持焦点；两者都不存在则清除。
- 地图地点卡片是瞬时 UI 状态，可通过 `SavedStateHandle` 恢复基础字段；关闭后清除。
- scrollbar 位置由 `LazyListState` 的 Compose 保存机制恢复，不进入 Room。

## 10. 边界与错误处理

- 未授权：地点池/每日行程可用；搜索页显示授权说明，不发请求；底图 POI 不可点击。
- 离线/搜索失败：独立页保留 query 与错误，可重试；返回不污染工作台。
- 搜索结果缺坐标：可显示，不可选择或收藏。
- 底图 POI 缺 POI ID：显示卡片，可聚焦，不可收藏。
- `AlreadySaved`：按已收藏状态收敛，不重复插入。
- usage count 或删除失败：保留收藏和确认目标，显示非阻塞错误，允许重试；不得先乐观移除。
- 删除期间目标已不存在：关闭确认并刷新，不再次删除。
- 搜索结果与收藏地点同坐标：只保留一个收藏 marker；焦点描边叠加其上。
- occurrence 为空时不生成编号；超过三次只压缩 badge，详情完整。
- 空地点池或不足一屏：隐藏 scrollbar thumb。
- 旋转、普通重组、Tab/sheet 变化不得重建 MapView、重复消费导航结果或重复移动相机。

## 11. 测试策略

### JVM

- Tab 只含 `PLACES/ITINERARY`，旧 `SEARCH` 状态恢复为 `PLACES`。
- `clearSearch()` 清空 query/results/error/searching 并取消旧请求。
- 收藏切换：未收藏保存、零引用直接删除、有引用请求确认、取消不删除、确认调用一次级联入口。
- marker kind 与 badge：地点池无编号；单日/全程编号；1/2/3/>3 次；搜索与收藏碰撞；聚焦不改变 identity。
- scrollbar geometry：空、不足一屏、刚好一屏、超一屏、顶部、中部、底部。
- 一次性 selection 消费后 key 被移除。

### Room instrumentation

- 零引用取消只删除地点。
- 有引用确认后复用 `deletePlaceAndReferences`，删除全部 occurrence，并使受影响路线删除/重建；不修改 schema。
- 取消确认后地点、行程项和路线不变。

### Compose instrumentation

- 抽屉只出现地点池/每日行程。
- 地图右下入口无文本输入，点击进入独立页；宽屏约 1/5、高 40dp、白底且有放大镜语义。
- 选择结果后状态清空、返回工作台、只聚焦一次。
- 已收藏结果可取消；有引用时先确认。
- 点击底图 POI 先显示卡片，再由按钮收藏/取消。
- 地点池超屏显示 thumb、不足一屏隐藏，thumb 无点击/拖动行为。

### AMap/设备

所有 instrumentation 只能在 AVD 名严格等于 `easy_trip_p60pro` 的模拟器运行。运行前执行：

```bash
adb -s "$ANDROID_SERIAL" emu avd name
```

输出必须精确为 `easy_trip_p60pro`；否则不运行 instrumentation。验证底图 POI 回调、收藏图标、编号压缩、高亮描边、相机只聚焦一次和不同地图范围下的 identity。

## 12. 验收标准

1. 抽屉无 SEARCH Tab，仅有地点池和每日行程。
2. 工作台地图右下搜索入口符合 1/5、40dp、白底、放大镜、无文字和只读要求。
3. 搜索在独立页面完成；选择后清空搜索状态、返回并聚焦高亮。
4. 搜索结果和底图 POI 均可通过统一规则收藏/取消；零引用直接取消，有引用先确认并复用现有级联删除。
5. 收藏 marker 在三种范围下正确显示收藏身份和编号；重复编号压缩符合规则；聚焦不覆盖身份。
6. 未收藏搜索地点使用普通 marker。
7. 地点池超屏显示不可拖 thumb，不足一屏隐藏。
8. JVM、lint、assemble 通过；instrumentation 仅在 `easy_trip_p60pro` 上通过。
9. Room schema 未变化，无源码回退、无 commit/push、无 key 泄露。
