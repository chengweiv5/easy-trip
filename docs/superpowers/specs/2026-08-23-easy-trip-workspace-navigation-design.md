# Easy Trip 工作台导航重构设计

## 1. 目标

重构旅行工作台的信息架构，将当前相互独立的内容 Tab、地图范围和日期选择统一为两层导航：

1. 地图下方的底导航只保留“地点池”和“行程”；
2. 抽屉直接展示底导航当前选中的内容；
3. “行程”内容使用左侧范围导航与右侧行程内容双栏布局；
4. 地图范围始终由底导航和行程范围共同派生，避免地图与抽屉内容不一致。

本设计不改变地点池、每日行程、路线计算和地图渲染的领域规则，只调整工作台导航状态、布局和展示方式。

## 2. 非目标

本次不包含：

- 修改 Room schema 或已有旅行、日期、行程项、路线数据结构；
- 改变地点搜索、收藏或取消收藏流程；
- 在“全程”视图中编辑、排序或跨日拖动行程；
- 修改路线规划、失败重试或网络恢复规则；
- 改变地图图层、缩放控件、顶部栏和悬浮搜索入口的既有视觉；
- 自动生成或优化行程。

## 3. 信息架构

工作台从上到下保持以下区域：

1. 地图区域：地图、悬浮顶部栏、图层与缩放控件、悬浮搜索入口；
2. 底导航栏：“地点池 / 行程”；
3. 抽屉：显示底导航当前内容；
4. 抽屉拖拽短横线：维持收起、半屏、展开三态。

底导航替代现有“地点池 / 单日 / 全程”地图范围控制，也替代抽屉内“地点池 / 每日行程”内容 Tab。工作台不再同时展示两套表达相近的导航。

## 4. 统一导航状态

### 4.1 状态模型

新增工作台底导航：

```kotlin
enum class WorkspaceSection {
    PLACE_POOL,
    ITINERARY,
}
```

新增行程范围：

```kotlin
sealed interface ItineraryScope {
    data object WholeTrip : ItineraryScope
    data class Day(val dayId: String) : ItineraryScope
}
```

`TripWorkspaceUiState` 持有：

```kotlin
val section: WorkspaceSection
val itineraryScope: ItineraryScope
```

地图范围不再作为独立用户状态，而是派生值：

```kotlin
fun WorkspaceSection.toMapScope(itineraryScope: ItineraryScope): MapScope = when (this) {
    WorkspaceSection.PLACE_POOL -> MapScope.PLACE_POOL
    WorkspaceSection.ITINERARY -> when (itineraryScope) {
        ItineraryScope.WholeTrip -> MapScope.WHOLE_TRIP
        is ItineraryScope.Day -> MapScope.SINGLE_DAY
    }
}
```

具体日期由 `ItineraryScope.Day.dayId` 唯一表示，不再维护另一份独立的工作区日期选择状态。

### 4.2 状态不变量

任何时刻必须满足：

- `PLACE_POOL` 对应地点池抽屉和地点池地图；
- `ITINERARY + WholeTrip` 对应全程只读内容和全程地图；
- `ITINERARY + Day(id)` 对应该日可编辑内容和单日地图；
- UI 不允许独立修改地图范围而不改变抽屉内容；
- 行程范围只在 `ITINERARY` 中可见，但切换到地点池时仍保留，以便返回行程后恢复。

## 5. 底导航栏

地图下方只显示两个并排选择项：

- 地点池
- 行程

继续使用现有 `SelectablePill` 视觉语言：

- 选中项为主题色实心背景与反白文字；
- 未选项透明、无描边；
- 两个选项之间不增加额外间距；
- 同时暴露互斥选择语义。

点击“地点池”：

- `section = PLACE_POOL`；
- 地图显示全部收藏地点；
- 抽屉直接显示地点池列表；
- 抽屉内部不再重复显示“地点池”标题或 Tab。

点击“行程”：

- `section = ITINERARY`；
- 抽屉显示双栏行程卡片；
- 地图范围由上次行程范围选择决定。

## 6. 行程双栏卡片

### 6.1 整体布局

“行程”抽屉内容分成左右两栏：

- 左栏固定宽度 `88dp`；
- 右栏使用剩余宽度；
- 两栏各自管理滚动，不让左侧日期选择随右侧长内容消失；
- 抽屉收起、半屏和展开时均保留该结构；
- 半屏空间过小时允许右侧内容纵向滚动，左栏宽度不压缩。

### 6.2 左侧范围导航

左侧按顺序显示：

1. 全程
2. 第一天
3. 第二天
4. 依次直到最后一天

规则：

- 左栏独立纵向滚动；
- 选中项使用现有实心选中样式；
- 日期显示旅行日序号，不显示具体日期；
- 首次进入行程且无保存选择时默认第一天；
- 切换至地点池再返回行程时恢复上次选择；
- 天数较多时可滚动到最后一天。

### 6.3 右侧单日内容

选择具体日期时，右侧复用现有每日行程时间轴：

- 地点卡片；
- 地点间交通段；
- 修改时间；
- 修改交通方式；
- 删除地点；
- 同日排序；
- 跨日移动。

单日编辑能力保持不变。左侧选择日期是该工作台唯一的日期选择入口，右侧不再重复显示横向 Day Selector。

### 6.4 右侧全程内容

选择“全程”时，右侧按日期连续分组展示：

```text
第一天
  地点卡片
  交通段
  地点卡片

第二天
  地点卡片
  交通段
  地点卡片
```

规则：

- 从第一天到最后一天依次展示；
- 每组使用明确的“第一天 / 第二天……”标题；
- 地点卡片和交通段复用现有视觉；
- 全程视图只读；
- 不显示拖动、删除、修改时间、交通方式或跨日移动入口；
- 空日期仍显示日期标题与“暂无行程”空态；
- 全程视图不把跨天地点合并为一条无日期时间轴。

## 7. 地图联动

底导航与左侧范围选择同步决定地图内容：

| 底导航 | 行程范围 | 地图内容 |
|---|---|---|
| 地点池 | 保留但不可见 | 全部收藏地点 |
| 行程 | 全程 | 所有日期地点及完整路线 |
| 行程 | 某一天 | 当天地点及完整路线 |

切换行为：

- 点击“地点池”：地图切换地点池范围并适配一次；
- 点击“行程”：地图按记住的行程范围适配一次；
- 点击“全程”：地图适配全部地点和路线 polyline；
- 点击具体日期：地图适配该日地点和路线 polyline；
- 修改当前日期行程后，地图按变化后的地点与路线适配一次；
- 普通重组、抽屉拖动、右侧列表滚动不得重置地图；
- 地图 POI 卡片收藏或取消收藏继续只更新收藏图标，不移动地图。

## 8. 状态恢复与迁移

### 8.1 新状态持久化

通过现有 `SavedStateHandle` 保存：

- `workspace.section`：`PLACE_POOL` 或 `ITINERARY`；
- `workspace.itineraryScope`：`WHOLE_TRIP` 或具体 `dayId`。

### 8.2 默认规则

- 首次进入工作台默认 `PLACE_POOL`；
- 首次进入行程默认第一天；
- 若旅行没有日期，行程范围使用 `WHOLE_TRIP` 空态；
- 返回地点池不会清除上次行程范围。

### 8.3 旧状态迁移

兼容现有保存值：

- 旧 `WorkspaceTab.PLACES` → `PLACE_POOL`；
- 旧 `WorkspaceTab.ITINERARY` → `ITINERARY`；
- 已废弃 `SEARCH` → `PLACE_POOL`；
- 旧 `MapScope.PLACE_POOL` → `PLACE_POOL`；
- 旧 `MapScope.SINGLE_DAY` → `ITINERARY + 当前有效日期`；
- 旧 `MapScope.WHOLE_TRIP` → `ITINERARY + WholeTrip`。

迁移只读取旧 key 并生成新状态，不修改 Room 数据。

### 8.4 日期删除与变化

若当前 `Day(id)` 不再存在：

1. 优先选择被删除日期之后仍存在的相邻日期；
2. 若没有后继，则选择前一个日期；
3. 若旅行已无日期，则切换为 `WholeTrip` 并显示空态。

日期重排后继续按 `dayId` 保持选择，不按旧 index 漂移。

## 9. 组件边界

### 9.1 TripWorkspaceViewModel

职责：

- 持有 `WorkspaceSection` 与 `ItineraryScope`；
- 校正失效日期；
- 派生 `MapScope` 与 `selectedDayId`；
- 保存和迁移工作区状态；
- 向地图视野控制器传递正确的地点与完整路线坐标。

### 9.2 TripWorkspaceScreen

职责：

- 渲染地图和两个底导航项；
- 根据 `section` 选择地点池或行程抽屉内容；
- 不再渲染旧抽屉 Tab 行；
- 保持现有悬浮顶部栏、搜索入口、图层、缩放和抽屉手势。

### 9.3 ItineraryScopeRail

新增独立组件：

```kotlin
@Composable
fun ItineraryScopeRail(
    days: List<TripDay>,
    selected: ItineraryScope,
    onSelect: (ItineraryScope) -> Unit,
    modifier: Modifier = Modifier,
)
```

只负责左侧范围导航及其滚动、选择语义。

### 9.4 DayItinerarySheet

调整为只渲染单日右侧内容：

- 不再包含横向 `DaySelector`；
- 仍保留所有单日编辑能力；
- 日期选择由 `ItineraryScopeRail` 负责。

### 9.5 WholeTripItineraryContent

新增只读全程内容组件：

```kotlin
@Composable
fun WholeTripItineraryContent(
    days: List<WholeTripDayUi>,
    modifier: Modifier = Modifier,
)
```

它只消费只读 UI 模型，不直接调用 repository 或编辑 ViewModel。

## 10. 数据模型

全程只读内容使用独立 UI 模型：

```kotlin
data class WholeTripDayUi(
    val dayId: String,
    val dayNumber: Int,
    val items: List<ItineraryItemUi>,
    val legs: List<RouteLegUi>,
)
```

由现有 itinerary/route flow 映射生成，保持：

- 日期顺序稳定；
- 每日地点顺序稳定；
- 交通段仅出现在相邻地点之间；
- 路线状态、距离和时间格式复用现有逻辑。

## 11. 空态与错误处理

- 地点池为空：显示现有地点池空态；
- 行程没有日期：左侧只显示“全程”，右侧显示“暂无旅行日”；
- 某天无地点：右侧显示“暂无行程”；
- 全程所有日期为空：每个日期保留分组标题与空态；
- 路线失败或等待网络：沿用现有交通段状态，不影响其他日期展示；
- 选中日期失效时自动回退，不闪退、不显示旧日期内容。

## 12. 可访问性

- 底导航使用互斥选择语义，名称为“地点池”“行程”；
- 左侧“全程 / 第 N 天”使用 `Role.Tab` 或 `Role.RadioButton` 与 selected 状态；
- 全程只读内容中的日期分组标题使用 heading 语义；
- 左栏和右栏均可通过 TalkBack 独立遍历；
- 单日编辑按钮保留现有描述和自定义排序动作；
- 不依赖颜色单独表达底导航或行程范围选择。

## 13. 测试策略

### JVM

- 旧 Tab/MapScope 到新状态的迁移；
- 底导航与行程范围到 `MapScope` 的派生；
- 默认第一天、记住上次范围；
- 日期删除后的后继/前驱回退；
- 全程按天分组模型顺序；
- 全程只读模型的地点与路线配对；
- 单日/全程 viewport 包含 marker 和完整 polyline。

### Compose instrumentation

- 底导航仅显示“地点池 / 行程”；
- 地点池选中时抽屉直接显示地点列表；
- 行程选中时显示左侧范围栏与右侧内容；
- 左侧范围栏可独立纵向滚动；
- 选择“全程 / 第 N 天”同步地图范围；
- 全程内容按日分组且无编辑动作；
- 单日内容保留拖动、时间、方式、删除和跨日移动；
- 抽屉三态、悬浮顶部栏、搜索入口不回归。

### 真机

- 窄屏下 `88dp` 左栏与右侧卡片均可读；
- 7 天以上时左栏滚动顺畅；
- 单日和全程切换时地图完整展示路线且只适配一次；
- 全程长列表滚动无明显卡顿；
- 抽屉拖动不误触左侧范围选择。

## 14. 验收标准

1. 地图下方只显示“地点池 / 行程”两个底导航项；
2. 地点池直接展示地点池列表；
3. 行程抽屉左侧显示“全程 / 第一日到最后一日”，右侧显示所选内容；
4. 左侧选择与地图范围始终同步；
5. 全程按天分组展示且不可编辑；
6. 单日保留全部既有编辑能力；
7. 选择和抽屉状态可恢复，日期失效可安全回退；
8. 地图自动视野包含当前范围的地点和完整路线；
9. 原有悬浮顶部栏、搜索、收藏、图层、缩放和抽屉手势均不回归；
10. 不修改 Room schema，现有数据可直接使用。
