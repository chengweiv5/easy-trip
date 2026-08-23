# Easy Trip v1.0 Pencil 设计落地方案

## 1. 背景

`design/easy-trip-v1.0.pen` 是本轮 Android UI 开发的设计基准。当前应用已经具备旅行列表、创建旅行、旅行工作台、地点搜索与收藏、行程编辑、Room 持久化和高德地图能力，但 Compose 主题与共享视觉组件较薄，页面视觉尚未系统映射到 v1.0 设计稿。

本轮按可运行用户流程分批落地，不按设计稿编号机械逐页生成。第一批覆盖：

```text
我的旅行
→ 创建旅行
→ 旅行工作台
→ 返回旅行列表
→ 再次进入已创建旅行
```

第一批使用真实 Room 数据、现有 ViewModel、现有导航和真实地图能力，不使用仅供展示的假数据替代正式业务链路。

## 2. 决策原则

### 2.1 设计冲突

当 v1.0 设计稿与现有 UI 结构或交互不一致时，以设计稿为优先，可以调整现有页面结构、导航入口和 UI 状态模型。

设计稿未展示但应用已经实现的能力不删除。保留底层与用户能力，并将入口适配到 v1.0 的信息架构和视觉语言中。

### 2.2 工程边界

继续使用现有工程架构：

- Jetpack Compose 与 Material 3；
- Navigation Compose；
- ViewModel、`StateFlow` 与 `SavedStateHandle`；
- Room 本地数据源；
- Repository 与 Service 边界；
- 高德地图 SDK 及现有 Compose/View 互操作；
- 手写 `AppContainer` 依赖组装。

本轮不引入 Hilt、Dagger、Koin 等新的依赖注入框架，不调整 Room schema，不为视觉改版重写 Repository 或数据层。

### 2.3 Pencil MCP 的职责

Pencil MCP 用于：

- 读取 v1.0 页面层级、组件实例、设计变量和节点属性；
- 获取目标 frame 截图作为视觉基准；
- 识别跨页面重复的视觉模式；
- 必要时导出局部 HTML/CSS，辅助理解层级和尺寸。

Pencil 的 HTML/CSS 导出不作为 Android 生产代码。固定画布坐标也不机械翻译为 Compose 绝对布局。最终实现必须遵循 Compose 布局、状态、语义和生命周期模型。

## 3. 目标与非目标

### 3.1 第一批目标

1. 建立与 v1.0 对应的 Compose 设计 token。
2. 统一主按钮、次按钮、筛选 Pill、图标按钮及第一批需要的共享容器。
3. 重做“我的旅行”的默认态、空态、加载态和错误态。
4. 重做“创建旅行”的表单、校验、提交中与失败状态。
5. 重做“旅行工作台”的默认外壳，并保留真实地图和现有业务入口。
6. 通过真实 Room 数据完成创建、进入、返回和再次打开的闭环。
7. 使用 v1.0 默认主流程 frame 作为主验收基准，使用旅程 C/D、成功反馈、编辑完成等画面检查组件对边界状态的表达能力。

### 3.2 第一批非目标

第一批不包含：

- 搜索与收藏全部页面的完整视觉重做；
- 单日及全程行程编辑全部页面的完整视觉重做；
- 一次性实现 v1.0 中约 200 个顶层画面或状态；
- 删除设计稿未展示的现有业务能力；
- 修改 Room schema；
- 更换依赖注入方式；
- 重写 Repository、Service 或高德 SDK 适配层；
- 自行补造设计稿未定义的深色视觉方案。

## 4. 总体架构

### 4.1 数据与事件流

保持现有单向数据流：

```text
Compose Content
    ↓ UI 事件
Route / ViewModel
    ↓
Service / Repository
    ↓
Room / 高德 SDK
```

业务数据的唯一来源仍是现有数据层。视觉改版不建立第二套 UI 专用持久化模型。

### 4.2 Route 与 Content 分层

第一批涉及的主要页面拆成两层：

```kotlin
@Composable
fun TripListRoute(
    viewModel: TripListViewModel,
    onOpenTrip: (Long) -> Unit,
)

@Composable
fun TripListContent(
    state: TripListUiState,
    onAction: (TripListAction) -> Unit,
)
```

职责划分：

- `Route` 获取 ViewModel、收集状态和一次性事件、连接导航及生命周期；
- `Content` 只接收不可变 UI 状态和事件回调，不直接访问 Repository、Room 或导航控制器；
- 创建旅行和旅行工作台遵循同一原则；
- 本批次不强制重构未涉及的页面。

该边界允许使用固定 UI 状态渲染设计稿场景，同时让正式运行继续接入真实数据。

## 5. 设计系统

### 5.1 Token 分层

Pencil 节点读取恢复后，从 v1.0 中提取重复使用的视觉属性，映射为：

```text
EasyTripTheme
├── MaterialTheme.colorScheme
├── MaterialTheme.typography
├── MaterialTheme.shapes
└── EasyTripTheme.tokens
    ├── spacing
    ├── sizes
    └── elevation
```

规则：

- Material 3 已有语义的颜色、字体和形状接入 `MaterialTheme`；
- 间距、控件高度、图标尺寸等项目属性放入 Easy Trip token；
- 只提取跨页面重复且有明确语义的值，不为每个像素建立 token；
- 页面不得重复散落同一设计色值或组件尺寸；
- 若 v1.0 没有明确深色设计，第一批以浅色视觉为验收目标，不推测深色配色。

### 5.2 组件映射

| Pencil 组件 | Compose 目标 |
|---|---|
| `Primary Button` | 统一主操作按钮 |
| `Secondary Button` | 统一次操作按钮 |
| `Filter Pill` | 改造或替换现有 `SelectablePill` |
| `Icon Button` | 统一图标操作按钮 |

是否沿用现有 Kotlin 组件名，以调用点数量和最小改动为依据。不得同时留下两套视觉和行为重复的公共组件。

第一批按实际页面需要补充：

- 页面顶部栏；
- 旅行卡片；
- 空状态；
- 加载状态；
- 错误提示；
- 表单字段；
- 日期选择入口；
- 对话框容器；
- 工作台浮层容器；
- 地图圆形操作按钮。

不提前建立覆盖全部 v1.0 状态的大型 UI Kit。

### 5.3 响应式转换

- 横向尺寸优先服从父容器约束，不照搬固定画布宽度；
- 列表使用 `LazyColumn`；
- 表单在软键盘出现时仍可滚动和提交；
- 地图占据可用空间，浮层使用 Compose overlay；
- 页面处理状态栏、导航栏和显示切口安全区；
- 支持字体缩放和 TalkBack；
- 小屏优先保证内容可读、操作可达，不以固定空白换取表面像素一致。

## 6. 页面与状态

### 6.1 我的旅行

数据继续来自 `TripListViewModel` 和 `TripRepository`。UI 明确表达以下状态：

```kotlin
sealed interface TripListContentState {
    data object Loading : TripListContentState
    data class Content(val trips: List<TripCardUiModel>) : TripListContentState
    data object Empty : TripListContentState
    data class Error(val message: String) : TripListContentState
}
```

实际实现可以复用或调整现有 `TripListUiState`，无需为了匹配示例类型而建立重复状态。

交互规则：

- 点击旅行卡片打开对应工作台；
- 点击创建入口打开创建旅行界面；
- 创建成功后进入新旅行的工作台；
- 删除等现有能力继续保留，并按 v1.0 视觉安置。

### 6.2 创建旅行

沿用现有 `TripService` 与 Room 写入链路。状态至少包含：

- 旅行名称；
- 开始日期；
- 结束日期；
- 字段校验错误；
- 提交中；
- 提交失败。

行为规则：

- 名称不能为空；
- 结束日期不得早于开始日期；
- 提交期间禁止重复提交；
- 创建失败时保留输入并展示可理解的错误；
- 创建成功只触发一次导航；
- Compose 重组或配置变化不应清空输入；
- 跨进程恢复沿用现有 ViewModel 与 `SavedStateHandle` 能力，不借本轮改版扩展数据库。

### 6.3 旅行工作台

继续由现有 `TripWorkspaceViewModel` 聚合：

- 当前旅行；
- 地点池；
- 行程；
- 路线；
- 地图图层；
- 地图偏好。

第一批重做工作台外壳：

- 顶部信息区；
- 地图区域；
- 浮动操作入口；
- 地点池/行程切换；
- 底部内容容器；
- 加载、错误和空态。

搜索、地点编辑、行程编辑继续可从新工作台进入和使用。第一批不要求这些子页面全部完成 v1.0 视觉重做。

## 7. 真实数据流

### 7.1 创建旅行

```text
创建旅行 UI
→ TripListViewModel / TripService
→ TripRepository
→ Room transaction
→ trips Flow 更新
→ 一次性 OpenTrip 事件
→ AppNavigation 打开工作台
```

### 7.2 打开已有旅行

```text
点击旅行卡片
→ trips/{tripId}
→ 创建工作台相关 ViewModel
→ 订阅 Room Flow
→ 渲染地图和工作台内容
```

### 7.3 返回旅行列表

```text
工作台返回
→ popBackStack()
→ 旅行列表继续订阅 Room Flow
→ 展示更新后的旅行内容
```

导航结果和一次性事件不得因重组重复消费。

## 8. 错误与降级

- Room 读取失败：显示页面级错误和重试入口；
- 创建失败：停留在表单，保留输入，恢复可提交状态；
- 地图未授权或加载失败：地图区域显示符合设计语言的降级态，工作台本地业务内容仍可使用；
- `tripId` 不存在或旅行已删除：显示明确错误并允许返回旅行列表；
- 必须由用户处理的重要错误不只通过短暂 Snackbar 表达；
- 加载、空态和错误态之间由明确状态转换驱动，不通过多个互相矛盾的 Boolean 拼装。

## 9. 实施批次

### 9.1 设计基础

1. 读取 v1.0 的变量、复用组件和默认主流程 frame；
2. 建立 Compose token；
3. 改造公共按钮、Pill、图标按钮和容器；
4. 为第一批页面建立 `Content(state, callbacks)` 边界。

### 9.2 我的旅行

1. 列表、空、加载和错误状态；
2. 旅行卡片和创建入口；
3. 接入现有 Room Flow 和导航；
4. 保留现有删除等能力。

### 9.3 创建旅行

1. 表单、日期选择与校验；
2. 提交中和失败状态；
3. 接入现有 `TripService`；
4. 创建成功进入工作台。

### 9.4 旅行工作台外壳

1. 顶部信息区、地图、浮动按钮和底部内容容器；
2. 地点池/行程切换；
3. 保留搜索、地点池、行程和设置入口；
4. 地图未授权、加载失败和旅行不存在的降级状态。

### 9.5 整体验收

1. 完整操作主流程；
2. 对照 Pencil 和模拟器截图修复视觉差异；
3. 检查可访问性与小屏行为；
4. 运行测试、lint、构建与 instrumentation；
5. 运行 `graphify update .` 更新知识图谱。

## 10. 测试策略

### 10.1 JVM

覆盖：

- 页面状态映射；
- 创建旅行校验；
- 提交防重；
- 创建成功的一次性导航；
- 错误后保留输入；
- 工作台加载、旅行不存在和地图降级状态派生。

### 10.2 Compose instrumentation

覆盖：

- 旅行列表空态、内容态、加载态和错误态；
- 创建入口与表单字段；
- 名称及日期校验；
- 提交中按钮状态；
- 创建成功进入工作台；
- 工作台地图降级时业务入口仍可使用；
- 返回列表后新旅行仍存在；
- 关键控件具备可理解的 TalkBack 语义。

### 10.3 数据与导航集成

使用真实或内存 Room 验证：

- 创建旅行持久化；
- 创建后打开正确 `tripId`；
- 返回列表后 Flow 展示新旅行；
- 再次打开同一旅行；
- 一次性导航结果不重复消费。

### 10.4 验证命令

至少执行：

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew connectedDebugAndroidTest
```

instrumentation 只在可用模拟器名称为 `easy_trip_p60pro` 时运行。若环境不满足，交付报告必须明确写出“未完成设备验证”，不得以 JVM 测试或编译成功替代设备验收。

## 11. 视觉验收

每个默认主流程页面执行：

1. 从 v1.0 获取目标 frame 截图与节点参数；
2. 在模拟器中进入对应真实状态并截图；
3. 对比页面层级、信息顺序、颜色、字体、字重、组件尺寸、圆角和间距；
4. 检查系统栏、软键盘、字体缩放和窄屏行为；
5. 修正 Compose 实现，不修改 v1.0 设计稿迁就代码。

不要求把固定画布逐像素复制到所有设备。任何显著差异必须来自响应式、安全区、可访问性或系统组件约束，而非遗漏设计。

## 12. 验收标准

1. “我的旅行 → 创建旅行 → 旅行工作台 → 返回 → 再次打开”使用真实 Room 数据完整可用；
2. 默认主流程页面的信息架构和视觉语言与 v1.0 一致；
3. Pencil 的四类复用组件在 Compose 中有唯一、统一的实现；
4. 页面共享重复视觉值，不散落复制色值和尺寸；
5. 创建旅行具有完整校验、提交中、防重和失败恢复；
6. 工作台保留真实地图、地点池、搜索、行程和设置能力；
7. 地图未授权或加载失败时，本地工作台功能仍可使用；
8. 设计稿未展示的已有能力没有被删除；
9. 关键页面可用固定 UI state 独立渲染和测试；
10. JVM 测试、lint 和 debug 构建通过；
11. 在 `easy_trip_p60pro` 可用时，instrumentation 与主流程人工操作通过；
12. 代码改动后知识图谱已通过 `graphify update .` 更新。

## 13. 协作边界

开发过程中仅在以下情况需要产品或设计决策：

- 设计稿无法判断点击、返回或状态转换；
- 不同 frame 对同一状态存在矛盾；
- 缺少真实图片、文案或品牌资源；
- 需要启动、解锁或处理指定模拟器；
- 响应式适配与固定画布之间存在必须人工取舍的视觉差异。

其余代码组织、设计参数提取、测试、构建和缺陷修复由实现方完成。
