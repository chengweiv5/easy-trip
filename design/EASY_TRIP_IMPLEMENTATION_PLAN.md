# Easy Trip 实现计划

本文件记录 `design/easy-trip-v2.0.pen` 到 Android Jetpack Compose 的动态映射和批次进度。每个批次开始前核对设计与现有代码，每个批次结束后更新状态、测试和已知差异。

详细规则见 `design/EASY_TRIP_IMPLEMENTATION_GUIDE.md`。

## 状态定义

| 状态 | 含义 |
|---|---|
| 待分析 | 尚未完成设计到代码映射 |
| 待实现 | 映射完成，尚未编码 |
| 实现中 | 当前批次正在修改 |
| 已实现 | 代码完成但验证未完成 |
| 已测试 | 自动化检查通过 |
| 已验收 | 已完成设备操作和批次验收 |
| 阻塞 | 存在需要用户决策或外部条件的问题 |

## 全局映射原则

- 附录引用不创建独立页面。
- 流程画板和状态组画板不创建路由。
- 同一页面的不同画面由 UiState、弹层状态或一次性反馈表达。
- 优先映射现有 Composable，不为设计编号创建重复组件。
- 具体文件和符号必须在实施前通过 Graphify 和源码核对，不能仅根据名称猜测。

## Batch 0：设计映射与基础设施

目标：建立可持续的设计到代码映射，并收敛后续批次共用的视觉和工作台基础。

| 设计来源 | Frame ID | 现有/目标代码 | 核对内容 | 测试 | 状态 |
|---|---|---|---|---|---|
| Easy Trip UI Kit | `f7rS8` | `EasyTripTokens`、Theme、基础组件 | 颜色、字体、圆角、按钮、状态反馈 | `CreateTripContentTest`、`TripListContentTest`、`WorkspaceChromeTest` 等组件/宿主测试；Huawei 代表性页面验收 | 已测试（含 Batch 9 代表组件抽查；未声明整套 UI Kit 逐组件物理验收） |
| 14 状态规范 | `DxZ2a` | `EmptyState`、InlineStatus、Dialog、Snackbar | 六类通用状态和恢复操作 | `ConfirmationDialogTest`、各业务状态 focused Compose 测试 | 已测试（含 Batch 9 代表状态抽查；整板物理验收 PENDING） |
| 工作台全空 | `BrYVA` | `TripWorkspaceContent` | 地图、Tab、主抽屉结构 | Workspace UI 测试、typed scenario、模拟器证据 | 已测试 |
| 行程全空 | `WFOpg` | `WorkspaceItineraryContent` | Tab 切换和空行程状态 | Workspace UI 测试、typed scenario、模拟器证据 | 已测试 |
| 抽屉收起 | `kCc5z` | `WorkspaceBottomSheet` | `h108`、Tab、真实状态摘要与地图最大空间 | 抽屉状态测试 | 已验收 |
| 抽屉半屏 | `sWTB3` | `WorkspaceBottomSheet` | `h432`、地图与内容并存 | 抽屉状态测试 | 已验收 |
| 抽屉展开 | `f2ieZ6` | `WorkspaceBottomSheet` | `h720`、地图浮层按可用空间降级 | 层级和状态测试 | 已验收 |
| 地图图层 | `shoPV` | 地图图层浮层 | 遮罩、选项和控件层级 | Workspace UI 测试 + Huawei RealAmap 三图层 | 已验收 |

### Batch 0 完成条件

- [x] 设计 frame 与现有核心 Composable 映射完成（由 scenario identity、typed executable 与 production host 映射支撑）。
- [ ] token 与 UI Kit 一致（整套 `f7rS8` 尚未逐组件完成 physical 验收，`DxZ2a` 整板 physical 验收亦尚未完成）。
- [x] 主抽屉三态通过测试。
- [x] Dialog、菜单和操作抽屉不会被地图控件覆盖。
- [x] 地图收藏/已安排标记语义统一。
- [x] 代表性工作台状态已在设备中操作验证。

## Batch 1：我的旅行、创建和设置

| 设计界面 | Frame ID | 现有/目标代码 | 核心状态/交互 | 状态 |
|---|---|---|---|---|
| 我的旅行 | `K9h3r` | `TripListContent` | 当前旅行和少量其他旅行 | 已测试；Huawei 代表路径已验收 |
| 单个旅行 | `wJ51c` | `TripListContent` | 仅一个旅行 | 已测试（controlled Compose；专项物理验收 PENDING） |
| 多个旅行 | `pgrb6` | `TripListContent` | 仅其他旅行区域滚动，创建入口固定 | 已测试（controlled Compose；专项物理验收 PENDING） |
| 空状态 | `zIbEu` | `TripListContent` | 创建第一个旅行 | 已验收（Huawei） |
| 删除后 | `d1sTtb` | `TripListContent` | 删除杭州后川西成为当前旅行 | 已测试（controlled final state + production AppNavigation/Room 删除链；物理验收 PENDING） |
| 创建旅行初始 | `PnhPb` | 创建旅行页面 | 空字段、按钮可点击 | 已测试；Huawei 代表路径已验收 |
| 创建旅行校验 | `yIGiQ` | 创建旅行页面 | 字段错误，按钮形态不变 | 已验收（Huawei） |
| 填写日期 | `YYo6U` | 日期范围选择 | 开始/结束日期和天数 | 已测试；Huawei 代表路径已验收 |
| 创建旅行已填写 | `dzhkC` | 创建旅行页面 | 提交成功进入工作台 | 已测试；Huawei 代表路径已验收 |
| 旅行设置 | `U06l7P` | `TripSettingsContent` | 名称、日期、方式和删除入口 | 已测试（production AppNavigation + in-memory Room；Huawei 新删除闭环 PENDING） |
| 修改出行日期 | `IKTv5` | 日期范围选择 | 与创建日期面板一致，缩短范围提示影响 | 已测试；Huawei 新版范围日历代表路径已验收（严格 fixture 仍 PARTIAL） |
| 删除旅行确认 | `oW9mK` | `ConfirmationDialog` | 影响、保留项和危险操作 | 已测试（production AppNavigation + in-memory Room；物理验收 PENDING） |

### Batch 1 完成条件

- [x] 创建、校验、日期和成功进入工作台闭环可达。
- [x] `wJ51c` 单旅行与 `pgrb6` 多旅行：`TripListContentTest` 13/13（`emulator-5554`，即 `easy_trip_p60pro(AVD) - 12`，含 280dp×2 字体）；标准高度保持 header/primary/create 固定、仅 other-list 滚动，`<500dp` 多旅行时降级为 primary 与 other trips 共用滚动区、header/create 仍固定；Huawei ALN-AL00 production UI 从空列表创建 `SingleTrip` 后，再创建七个实际旅行并实操长列表滚动，header/primary/create 持续可见且创建入口可点（实际数据，非精确 Pencil fixture）。
- [x] `U06l7P`：`TripSettingsContentTest` 36/36（`easy_trip_p60pro(AVD) - 12`，分组、Done/Back、窄宽大字体与危险入口）；Huawei ALN-AL00 production UI 实操分组、完成态及危险区；仅标记实际 physical 操作，不以该安装态数据提升精确 fixture。
- [x] `IKTv5`：新版 `TripDateRangePickerSheet` 的响应式高度、Back/scrim 本地草稿、modal semantics、tiny height、成对范围提交与 footer `navigationBars` inset 已由 `emulator-5554` focused 自动化覆盖；Huawei ALN-AL00 仅以 `adb install -r` 覆盖 production APK，从空安装态创建 `RangeHuawei`（2026-09-08…09），在设置中将范围平移至 2026-09-10…11，确认 Room 回流更新两个连续旅行日并同步列表卡片。此为实际安装态代表路径，不等同严格 Pencil fixture；旧 `DateEditorSheet` 真机记录不再作为新版证据。
- [x] `oW9mK` / `d1sTtb`：`TripSettingsNavigationTest` 5/5（`easy_trip_p60pro(AVD) - 12`；production AppNavigation + in-memory Room）验证精确级联、杭州移除、川西 primary、泉州保留与回栈清理；Huawei ALN-AL00 实际取消删除后 `DateTrip` 保留，确认后 `DateTrip` 消失、`Dali` 成为 primary、其他旅行保留且系统 Back 不回到已删除 settings/workspace。真机数据不能冒充杭州/川西/泉州精确终态。
- [x] Final fix wave：`TripListContentTest` 13/13 + `TripSettingsContentTest` 36/36，合并 connected 49/49；仅更新上述 Batch 1 证据，不提升其他 frame。
- [ ] Batch 1 physical 完成限制：已记录 Huawei ALN-AL00 的实际 production UI 操作；但六帧尚未以严格匹配 Pencil fixture 数据逐项复现，精确终态仍由自动化证明，Device UI 保留 partial/限制说明。

## Batch 2：旅行工作台骨架

| 设计界面 | Frame ID | 现有/目标代码 | 核心状态/交互 | 状态 |
|---|---|---|---|---|
| 地点池工作台 | `A9EKX` | `TripWorkspaceContent`、`PlacePoolSheet` | 地图、地点池、滚动和主抽屉 | 自动化通过；Huawei 自然数据 8→9→10 点、COLD start 初始 fit、手势后 tab 往返保持与集合变化单次 refit 已验收 |
| 行程工作台 | `LFmzR` | `WorkspaceItineraryContent` | 日导航、行程列表和地图 | 已验收（Batch 5 Huawei 行程/路线主链） |
| 全空状态 | `BrYVA` | `TripWorkspaceContent` | 地点池与行程均为空 | 已测试 |
| 行程全空 | `WFOpg` | `WorkspaceItineraryContent` | 所有旅行日无行程项 | 已测试 |
| 工作台设置直达 | `ijpZD` | 工作台顶部设置操作 | 当前生产 UI 直接进入设置，不声明更多菜单 | 已测试（production navigation） |
| 地图图层 | `shoPV` | 地图图层浮层 | 图层选择和正确层级 | 已验收（Huawei RealAmap） |
| 抽屉三态 | `kCc5z` / `sWTB3` / `f2ieZ6` | `WorkspaceBottomSheet` | 收起、半屏、展开、按实时 sheetTop 降级地图浮层 | 已验收（Huawei App 自有 UI） |

### Batch 2 完成条件

自动化与 App 自有 UI 的物理设备验收已完成；2026-09-04 Huawei ALN-AL00 已在授权后的 production workspace / RealAmap 上完成标准、卫星、卫星路网切换。该证据的 state source 为真实安装态工作台数据，permission surface 为已接受 AMap consent；不外推为 A9EKX 八点自然数据验收。

- [x] 地点池/行程切换正确。
- [x] 三态抽屉可拖动且状态可恢复。
- [x] 地图控制、菜单、遮罩和抽屉层级正确。
- [x] 全空和行程全空状态入口可达。
- [x] Huawei ALN-AL00 上完成 App 自有 Batch 2 UI 与偏好持久化验收。
- [x] 已授权环境中的真实 AMap 三图层渲染验证（Huawei ALN-AL00，production workspace，2026-09-04）。

## Batch 3：地点池、搜索和地点详情

| 设计界面 | Frame ID | 现有/目标代码 | 核心状态/交互 | 状态 |
|---|---|---|---|---|
| 地点池长列表 | `A9EKX` | `PlacePoolSheet` | 8 个收藏地点、列表滚动、地图全览 | 已测试（fake map） |
| 地点池短列表 | `jQhXs` | `PlacePoolSheet` | 3 个收藏地点、一屏展示 | 已测试（fake map） |
| 地点池空状态 | `lsr1I` | `PlacePoolSheet` | 搜索第一个地点 | 已验收（Huawei App 自有 UI；map surface 未授权，不声明 RealAmap） |
| 搜索结果 | `ofdn5` | `PlaceSearchContent` | 连续收藏和取消收藏 | 已测试 |
| 搜索加载中 | `s1OvvX` | `PlaceSearchContent` | 保留关键词和加载反馈 | 已测试 |
| 搜索无结果 | `S0psO` | `PlaceSearchContent` | 调整关键词 | 已测试 |
| 搜索网络失败 | `GJo79` | `PlaceSearchContent` | 保留关键词和重试 | 已测试 |
| 已安排地点详情 | `p4G1tS` | 地点详情页面 | 安排日期、次数和实心书签 | 已测试（fake map） |
| 仅收藏地点详情 | `XsGon` | 地点详情页面 | 空心书签和加入行程入口 | 已测试（fake map） |

### Batch 3 完成条件

- [x] 首次进入地点池地图展示全部收藏地点（Batch 7 Huawei RealAmap：8→9→10 个自然收藏地点；COLD start 初始 fit、真实手势保持、集合变化仅单次 refit）。
- [x] 已安排和仅收藏标记与图例一致（自动化 + Huawei 两地点真实数据；非八点验收）。
- [x] 搜索结果可连续收藏/取消收藏并同步地点池。
- [x] 四种搜索状态保留输入和恢复路径（自动化；专项物理状态注入 PENDING）。
- [x] 两种地点详情状态正确（production workspace controlled 自动化；Batch 7 bottom sheet 物理验收 PENDING）。

## Batch 4：加入行程双入口

| 设计界面 | Frame ID | 入口 | 核心状态/交互 | 状态 |
|---|---|---|---|---|
| 单地点选择日期 | `xQfD0` | 地点池单地点加号 | 选择一个或多个旅行日 | 已测试 |
| 长日期列表 | `cRdBn` | 单地点入口且日期较多 | 中间列表滚动，标题和操作固定 | 已测试 |
| 从地点池添加 | `Pqdkf` | 行程页某天添加地点 | 批量选择多个收藏地点 | 已测试 |
| 无旅行日 | `p7U8B` | 单地点入口 | 前往行程添加旅行日 | 已测试 |
| 成功反馈 | `yNKT4` | 两种入口提交成功 | 撤销和查看结果 | 已测试 |
| 部分成功 | `mGhKO` | 批量提交 | 区分成功和失败项 | 已测试 |
| 旅行日已删除 | `D3XZi` | 提交期间状态变化 | 重新选择旅行日 | 已测试 |
| 撤销成功 | `KPBBb` | 成功反馈撤销 | 删除安排，保留收藏 | 已测试 |

### Batch 4 完成条件

自动化与模拟器验证已完成；Huawei 物理设备已完成当前真实数据可达的双入口、成功、查看与撤销闭环，长日期、部分成功和目标日并发删除仍待后续专项设备验收。

- [x] 两个入口都能端到端完成加入行程。
- [x] 单地点入口支持多个旅行日。
- [x] 旅行日入口支持多个地点。
- [x] 无旅行日、部分成功、目标日失效和撤销状态正确。
- [x] 重复加入同一地点不会被 UI 自动去重。
- [x] 聚合结果、异常中断后的已创建项、精确重试及结果/撤销状态可跨 Activity/进程重建恢复。
- [x] Huawei 物理设备完成当前真实数据可达的双入口、成功、查看和撤销验收；长日期、部分成功和目标日并发删除保留为专项设备验收项。

## Batch 5：行程编辑和路线

| 设计界面 | Frame ID | 现有/目标代码 | 核心状态/交互 | 状态 |
|---|---|---|---|---|
| 行程页 | `nAdK8` | `WorkspaceItineraryContent` | 某天添加、地点和路段入口 | 自动化、SwiftShader 模拟器真实 AMap、Huawei 物理设备通过 |
| 行程项编辑 | `K336N` | `EditItineraryItemContent` / `DayItineraryViewModel` | 时间、停留时长和备注；busy 锁定；失败原地保留草稿 | 自动化通过：production UI 三字段编辑、Room 回流与重开 |
| 编辑完成 | `mz2IS` | 行程页状态 | Room/观察流回流后展示最新安排，重新打开读取持久值 | 自动化通过：production AppNavigation + in-memory Room + recording fake map |
| 删除行程项确认 | `l2xCsM` | `ConfirmationDialog` | 删除安排、保留收藏与相邻路线重算 | 自动化通过：production UI 删除、SavedPlace 保留、bridge leg 重建 |
| 交通路段编辑 | `T7aESo` | `EditRouteLegContent` / `DayItineraryViewModel` | 方式、耗时和说明；无地图 coordinator 时 metadata-only 仍可保存 | 自动化、SwiftShader 模拟器、Huawei 15 分钟 override 保存通过 |
| 单日路线 | `eHTX3` | `MapUiModelMapper` / `MapViewportController` / `AmapComposeMap` | selected day markers、合法同日相邻 route、完整 geometry 与四向安全边距 | 自动化、SwiftShader 模拟器和 Huawei 真实路线/安全 viewport 通过 |
| 全程路线 | `FTIOF` | `WholeTripItineraryContent` / `MapUiModelMapper` | 日期连续分组、空日、只读 timeline；按 day 保留 route 颜色且不跨日连接 | 自动化、SwiftShader 模拟器和 Huawei 三天分组/只读/多段真实路线通过 |
| 当天无地点 | `Bcf6A` | `WorkspaceItineraryContent` / `DayItineraryContent` | 保留日期 rail，以当前日 CTA 进入多地点选择；不进入 WFOpg | 自动化通过：production Room-backed 主链 |
| 添加旅行日 | `zvO9Z` | `DayItineraryViewModel` / 工作台 overlay | 服务返回新日 ID 后等待 Room 观察到该 ID；非 Ready 时不消费完成 token；观察失败只重启观察确认而不重复写入 | 自动化通过：production Room-backed 主链 |
| 删除旅行日确认 | `J7PZ7u` | `TripSettingsViewModel` / `TripSettingsContent` | 删除影响、取消、busy、重试、日期范围互斥、重新编号/scope 与 Room 回流 | 自动化及 Huawei 追加第 4 天、取消删除、确认删除、日期/scope 回流通过 |

### Batch 5 完成条件

- [x] 行程项和交通路段编辑入口可达且保存正确。
- [x] 删除行程项和旅行日保持 SavedPlace。
- [x] 单日路线完整展示并避开所有浮层。
- [x] 全程路线按日期分组且不跨日生成 RouteLeg。

App 自有功能与 recording fake map 自动化门禁已完成；SwiftShader 模拟器和 Huawei ALN-AL00 均已完成真实 AMap、搜索收藏、路线、日期操作与重启持久化验收。真实 `MapView` 专用 attach smoke 在 SwiftShader 环境仍可能在 20 秒内未触发 `OnMapLoaded`，但 production host 已稳定显示底图与路线；不影响当前批次门禁结论。

## Batch 6：异常、权限和最终收敛

| 设计界面 | Frame ID | 核心状态/交互 | 状态 |
|---|---|---|---|
| 等待联网 | `P7k0M` | 保留行程，等待路线计算 | 已测试（focused） |
| 路线计算失败 | `E3EhSv` | 仅失败路段重试 | 已测试（focused） |
| 地图权限说明 | `EHOHC` | 地图服务授权说明 | 已测试（focused） |
| 定位权限说明 | `JFhZ7` | 点击定位后的用途说明 | 已测试（focused） |
| 定位权限前往设置 | `HYCsZ` | 永久拒绝后的系统设置入口 | 已测试（focused） |
| 地图加载中 | `GoxB6` | 保留工作台框架 | 已测试（focused） |
| 地图加载失败 | `U8R5i` | 保留地点/行程操作并重试 | 已测试（focused） |
| 行程保存失败 | `OOEsk` | 保留编辑输入并重试 | 已测试（focused） |

### Batch 6 完成条件

Task 8 已补齐恢复边界：文件型 Room 关闭/重开后，由 production `RoomRouteLegRepository` + `DefaultRouteRefreshCoordinator` 完成 offline→online 路线恢复；SharedPreferences concrete wrapper 跨实例保留 AMap consent 与定位 `hasRequested`；新 `DayItineraryViewModel`、`LocationPermissionCoordinator` 和新 composition 不恢复未提交编辑、保存错误、权限 prompt/generation/settings recovery 或地图失败 attempt。模拟器 installed-app `am force-stop` 门禁已执行；不等价于 Activity recreation、真实 AMap、Android 系统权限或物理设备验收。

- [x] 本地数据在网络、地图和路线失败时保持可用。
- [x] 权限请求时机和永久拒绝恢复正确。
- [x] 字体缩放、长文案、小屏和进程恢复完成核对。
- [x] 代表性实体设备主链验收完成（Huawei ALN-AL00；真实 AMap Loading→Ready、标准/卫星/卫星路网、AndroidSystem 定位说明→拒绝→永久拒绝设置引导→设置授予返回、`am force-stop` COLD start 与已提交 Room 数据保留）。`U8R5i` 真实 SDK failure 专项仍为 PENDING；真实 AMap 故障未自然触发，恢复分支继续由 failure-injecting host 覆盖。

### 2026-09-04 · Batch 6 / Task 71 · 地图失败恢复可达性与定位意图

- 实现：地图失败时按可用空间提供唯一“重试地图”入口：地图 fallback 可完整展示时复用其按钮；280dp 等空间不足场景在收起/半屏的固定 sheet header 槽位或展开内容首行提供 48dp 以上入口。地图未授权在同一紧凑布局提供唯一“查看并授权”入口，避免 fallback 隐藏后无法恢复。各入口条件互斥，不与 Workspace TopBar、返回/更多、Tabs 或 sheet handle 控件交叠。地点/行程内容与当前 section、sheet level 保持不变，不新增页面或 route。
- 定位边界：每个 `AmapComposeMap` attempt 默认不重放旧 attempt；Screen 的 tracker 只把 `onLocateRequestConsumed` 回报的 request 视为已消费。若 request 在 Loading 期间产生但 host 在 ready/消费前失败，或在 Failed 后新产生，则显式点击 retry 会把该 pending intent 交给 replacement attempt，使其首次 ready 后恰好执行一次；重复 retry 不覆盖目标 attempt 的 forwarded baseline。目标 attempt ready/failed 后清除 forwarded 状态，同 attempt subtree 重挂仅使用已记录的 consumed baseline，不推断当前 request 已消费；consent/token replacement 通过复合 attempt identity 以当前 request 建立新 baseline，不重放旧 intent。
- TDD：新增 280dp、2× font scale 下三种 sheet level 的 retry/consent recovery 可达、可点击、最小 48dp 及 TopBar/标题/Back/More/Tabs/handle 非交叠回归。定位补充 JVM tracker 回归，RED 命令 `./gradlew :app:testDebugUnitTest --tests com.yangchengwei.easytrip.workspace.MapLocateRequestBaselineTrackerTest` 先因缺少 attempt API 编译失败，并在 duplicate retry 用例上得到断言失败；GREEN 后 13/13 通过。新增 Screen 集成用例覆盖 Loading 期间 request→timeout failure→显式 retry→replacement ready 后执行一次。
- 自动化验证：全量 JVM `testDebugUnitTest`、`assembleDebug`、`assembleDebugAndroidTest`、`lintDebug`、`git diff --check`、`graphify update .` 通过。恢复 emulator-5554 后，small-window 三 sheet level retry、consent recovery、expanded retry 和 Screen Loading→failure→retry 定位转发均单项通过；`WorkspaceFlowTest` 66/66、`WorkspacePermissionFlowTest` 13/13、`AmapComposeMapTest` 28/28、`RoomRouteLegRepositoryTest` 10/10、`ItineraryTimelineContentTest` 42/42、`ItineraryEditingTest` 10/10、`V1ScenarioCatalogTest` 47/47、`V2AcceptanceTest` 4/4、`Task8PersistenceTest` 4/4 通过。`graphify update` 继续报告未触及的 `NetworkMonitor.kt` / `RoutePlanner.kt` 语法解析 warning；其对新增 Compose 测试的解析 warning 不影响 Kotlin/AndroidTest 编译或 connected 单项结果。
- 环境说明：`TripWorkspaceContentTest` 整类在 emulator-5554 运行时存在既有 instrumentation 进程被 SIGKILL 的不稳定性；本批新增/修改的地图重试、授权恢复与定位转发用例均已隔离串行通过。已使用 `adb install -r` 恢复最终 APK，并以 `am force-stop` 后 COLD start 验证启动成功；未清应用数据。未执行真实 AMap、Android 系统权限或物理设备最终验收。

### 2026-09-04 · Batch 6 / Task 72 · 首次地图挂载前定位意图

- 实现：`TripWorkspaceScreen` 在当前 composition 内记录地图 host 是否曾经挂载。首次 host 尚不存在时，保留 screen 初始 locate baseline，使 consent/token/ready 后首次 `AmapComposeMap` 能消费期间产生的新定位 request；曾挂载后的 consent/token replacement 以 `consent × mapAttempt` 作为新的 tracker attempt identity，并以当前 request 建立 baseline，不重放旧请求。Task 71 失败态显式 pending forwarding 仍通过前移一格的 baseline 执行一次；ready/resumed gate 与非持久化边界不变。
- TDD：新增 production `TripWorkspaceRoute` 回归，覆盖 ConsentRequired/no token 下点击已授权定位、确认没有 host，随后提供 consent/token 创建首个 recording host，并断言 `showCurrentLocation` 恰好一次且普通 recomposition 不重复；另以 `MapLocateRequestBaselineTrackerTest` 覆盖首次 baseline、consent replacement 与默认 replacement 不重放。RED 阶段 focused JVM 明确失败于首次 mount 误用当前 request（expected 0, actual 1），以及新 consent attempt API 缺失；GREEN 后 focused JVM 13/13 通过。
- 自动化验证：全量 JVM `testDebugUnitTest`、`assembleDebug`、`assembleDebugAndroidTest`、`lintDebug`、`git diff --check`、`graphify update .` 通过；focused `MapLocateRequestBaselineTrackerTest`、`MapLayerRenderingPolicyTest` 与 `MapPreferencesTest` 通过。emulator-5554 上的 `WorkspaceFlowTest` 66/66、`WorkspacePermissionFlowTest` 13/13、`AmapComposeMapTest` 28/28 已通过；未运行截图类测试。
- 物理设备验收：Huawei ALN-AL00 使用 `adb install -r` 覆盖安装，未卸载、未清数据。真实 AMap 从工作台加载成功，标准/卫星/卫星路网均可切换；AndroidSystem 定位链完成 JFhZ7 说明、系统拒绝、HYCsZ 设置引导、前往设置授予与返回定位；`am force-stop` 后 COLD start 保留已提交“登封”旅行、地点池和图层偏好，不恢复临时权限弹层。真实 AMap 故障未能自然触发，因此 U8R5i 的真实 SDK 故障证据仍不声明，恢复分支继续由 failure-injecting host 覆盖。验收后已把定位权限恢复为拒绝、地图恢复为标准层。模拟器整类 `TripWorkspaceContentTest` 仍存在环境 SIGKILL，不将该中断冒充通过，相关新增单项已单独串行通过。

## Batch 7：设置删除、地点详情宿主与证据收口

| 设计界面 | Frame ID | Host / state / surface 边界 | 状态 |
|---|---|---|---|
| 旅行设置 | `U06l7P` | production AppNavigation + in-memory Room；recording fake map；未涉及 permission surface | 已测试；Huawei 新闭环 PENDING |
| 删除旅行确认 | `oW9mK` | production AppNavigation + in-memory Room 精确级联；recording fake map；未涉及 permission surface | 已测试；物理验收 PENDING |
| 我的旅行 · 删除后 | `d1sTtb` | controlled final-state TripListContent + 上述 production 删除链；不把 declared path 当 executed navigation | 已测试；物理验收 PENDING |
| 已安排地点详情 | `p4G1tS` | production workspace Compose + controlled repository/UiState + recording/deterministic fake map | saved-place-row flow 已在 AVD 通过；物理验收 PENDING |
| 仅收藏地点详情 | `XsGon` | production workspace Compose + controlled UiState + deterministic fake map；无 Room/navigation 证据 | 已测试；物理验收 PENDING |
| 地点池 8 点 | `A9EKX` | JVM viewport/mapper 自动化；Huawei natural data + RealAmap 裁决最终 viewport | 自动化通过；Huawei 8→9→10 点、COLD start、手势保持与单次集合 refit 已验收 |

### 2026-09-04 · Batch 7 / Task 1 · 设置页整次旅行删除状态机

- 实现：`TripSettingsViewModel` 自持有单旅行 `tripDeletion` 状态，复用既有 `TripDeletionUiState` 与 `ConfirmationUiModel`，未复用 `TripListViewModel`，未修改 Compose UI。请求先加载当前旅行精确影响；取消会失效迟到 impact；确认以 generation 和 busy 守卫确保 service exactly-once。service 成功后必须等待当前 `observeTrip` 新一轮 `null` 事实才发送一次 `ReturnToTripList`；若 `null` 先到，则继续等待 service 返回。删除失败保留同一确认可直接重试；观察同步失败保留确认并只重启 observation，不重发 service。整次旅行删除与日期范围、旅行日删除互斥。
- TDD：focused JVM RED 先因 `tripDeletion` 状态和 request/retry/cancel/confirm/resync API 缺失而编译失败；初始实现阶段的 focused suite 曾记为 51/51。Task 1 报告在补充“旅行日确认阻塞整次旅行删除”等回归后记录 53 tests；review/fix 完成时 progress 记录 54/54，Task 2 后续锁定回归扩展到 58 tests。各数字对应不同提交前阶段，不是同一轮互相矛盾的结果。覆盖精确影响、取消迟到结果、single-flight、两种 service/Room 返回顺序、失败重试、同步重试不重复 service、三类删除互斥及外部删除一次导航。
- 自动化验证：`testDebugUnitTest`、`assembleDebug`、`lintDebug`、`assembleDebugAndroidTest`、`git diff --check` 与 `graphify update .` 通过。`adb devices -l` 无可用设备，未运行 connected Android UI 测试；本 Task 无 Compose UI 改动。
- 已知边界：Graphify 更新继续报告未触及的 `TripWorkspaceContentTest.kt`、`NetworkMonitor.kt`、`RoutePlanner.kt` 语法解析 warning；不影响 Kotlin 编译或本 Task 的 JVM 验证。

### 2026-09-04 · Batch 7 / Task 2 · 设置页旅行删除生产闭环

- 实现：`U06l7P → oW9mK → 旅行列表` 已接入 production UI。设置危险区用 48dp 的“删除这次旅行”入口取代“请回列表操作”提示；`TripSettingsRoute` 转发 Task 1 的 request/retry/cancel/confirm/resync API。列表与设置共同使用 `TripDeletionDialog`，统一 Loading / impact failure / Ready / deleting / sync failure 的互斥文案、操作与 busy 锁。既有 `AppNavigation` 的 `onTripDeleted` 仍以 `popUpTo(TRIP_LIST_ROUTE)` 清除设置/工作台返回栈；不新增 route。
- 设计 frame：`U06l7P`、`oW9mK`，并读取 UI Kit `f7rS8`、状态规范 `DxZ2a`。确认页复用 `ConfirmationDialog` 中“将删除/将保留”的精确影响内容。
- TDD：新增 Compose/production navigation 测试先 RED，`compileDebugAndroidTestKotlin` 按预期因缺少设置删除 callback/API、测试 import 而失败；最小接线后 GREEN。新增测试覆盖危险入口、五种删除状态的互斥恢复路径、删除后 Room 中目标旅行消失且剩余旅行成为 primary、Back 无法回到已删除设置/工作台，以及 280dp×2x 下入口/确认操作 48dp 可达。
- 自动化验证：`git diff --check`、`assembleDebug`、`assembleDebugAndroidTest`、`testDebugUnitTest`、`lintDebug`、`compileDebugAndroidTestKotlin` 均通过；`graphify update .` 成功（6799 nodes / 15823 edges）。
- 设备边界：无连接设备。`connectedDebugAndroidTest` 因 `No connected devices` 未运行；两台本地 AVD 启动均在 macOS Crashpad/NSWorkspace bootstrap 后以 exit 139 退出，因此未进行模拟器/实体机实际操作。未生成截图。
- 已知边界：图谱更新仍报告未触及的 `TripWorkspaceContentTest.kt`、`NetworkMonitor.kt`、`RoutePlanner.kt` 解析 warning；FSEvents 初始化 warning 和 `EmptyState.kt` 既有 Kotlin annotation warning 不影响构建门禁。

### 2026-09-04 · Batch 7 / Task 3 · 工作台地点详情临时 Sheet

- 实现：`p4G1tS` / `XsGon` 继续复用 `WorkspaceOverlay.PlaceDetail`、`PlacePoolUiState`、`PlaceDetailPanel` 与既有 action；仅将 workspace saved-place 的居中 `AlertDialog` 宿主替换为无遮罩、底部贴齐的 `WorkspacePlaceDetailSheet`。正常高度按可用 workspace 的 `490/782` 比例计算，限制为 320–560dp，并保留至少 96dp 上方地图空间；`<416dp` 的极紧凑窗口优先保证 Sheet 可操作性，可占满全部可用高度。查看与编辑在同一 sheet 切换，正文沿用可滚动面板，操作保持至少 48dp。
- 状态边界：地点池行与 `SAVED_PLACE_POOL` marker 均进入同一 sheet；打开、关闭与查看→编辑不改变 map subtree 或 viewport request。保存期间 panel 关闭按钮禁用，workspace Back/关闭均忽略；关闭、删除和加入行程先清旧详情状态，避免 overlay 重新打开。
- TDD：focused JVM 先因缺少响应式高度函数和 `placeDetailSaving` Back 策略编译 RED；随后以临时 mutation 移除两项实现，确认 2/2 行为断言失败，再恢复后 2/2 GREEN。新增 Compose instrumentation 覆盖 row/marker 打开 bottom sheet、无 Dialog 语义、地图仍挂载、host creation=1、viewport 不新增、同 sheet 编辑/保存锁、280dp×2x 下滚动至全部 48dp 操作；设备为空，仅完成 Android test 编译。
- 自动化验证：`compileDebugAndroidTestKotlin`、focused JVM、full JVM、`assembleDebug`、`assembleDebugAndroidTest`、`lintDebug`、`git diff --check` 通过；`graphify update .` 结果见 Task 报告。
- 设备验证：`adb devices -l` 无设备，相关 connected Compose 测试 NOT-RUN；未声明真实 AMap 证据。
- 已知边界：standalone `PlacePoolSheet` 保持现有无生产 route 的 Dialog 宿主，不制造地图宿主；map POI、未收藏 marker、删除/取消收藏确认未扩张。
- Fix Round 1：`TripWorkspaceScreen` 现在用自身单一 `workspace-screen-root` Box 按 Content→Overlay 顺序叠放，不依赖 NavHost/调用方，且未重复传递外部 modifier。极小高度改为 `<416dp` 时 sheet 充满全部可用高度，优先保证 Sheet 可操作性；正常高度保留至少 96dp 地图空间，并保持 490/782 比例与 320–560dp 上下限。`PlaceDetailPanelTest` 的 Kotlin `assert` 已全部替换为 JUnit 断言。工作台 recording hosts 复用 production `viewportRendering` 消费语义记录实际非空 `ViewportCommand` 次数，证明行打开、marker 打开、查看→编辑与关闭均不增加实际 camera apply；因无设备，该 instrumentation 证据已编译但 NOT-RUN。

## 附录引用处理

以下类型只作为视觉和状态验收，不创建额外页面：

- `附录引用 · 01 我的旅行`
- `附录引用 · 36 我的旅行 · 空状态`
- `附录引用 · 03/27/38/44 搜索地点状态`
- `附录引用 · 09/15/31/33/39/42/43 加入行程状态`
- `附录引用 · 17/28/29/37/45/46 路线和地图状态`
- `附录引用 · 30/34/35 权限状态`
- `附录引用 · 10/11/12/13/25/32/40/48 编辑和危险操作`

## 批次记录

### 2026-08-30 · Batch 0 / Batch 2 · 工作台抽屉三态

- 实现：以 390×844 设计画布扣除 62dp 状态栏后的 782dp Workspace Body 为基准，精确使用 108/432/720 三态锚点；小窗口按比例缩放并在最小支持窗口保留完整 68dp chrome 和真实摘要；收起态保留拖动入口、44dp Tab 与地点池/行程摘要；半屏和展开态保留完整业务内容；地图浮层按实时 `sheetTop` 空间自动隐藏，顶部栏始终保留；展开态关闭不可见图层菜单状态，地图失败提示在空间不足时不再严重裁切；图层面板在标准半屏中位于圆形控件左侧，且打开期间隐藏地图图例，避免与顶部栏、Sheet 或图例重叠。
- 设计 frame：`kCc5z`、`sWTB3`、`f2ieZ6`。
- 修改文件：`WorkspaceBottomSheet`、`WorkspaceScaffold`、`TripWorkspaceContent`、`TripWorkspaceViewModel` 及对应 Workspace 测试。
- 自动化验证：`WorkspaceSheetSyncTest`、`WorkspaceLayoutMetricsTest`、`TripWorkspaceNavigationStateTest` 通过；`WorkspaceChromeTest` + `TripWorkspaceContentTest` 41/41；`VisualBatch0EvidenceTest` 17/17；`V1ScenarioCatalogTest` 47/47；`V1FullUiAcceptanceTest` 47/47；`lintDebug`、`assembleDebug`、`assembleDebugAndroidTest`、`git diff --check` 通过。
- 设备验证：在 emulator-5554 生成生产 Compose 工作台宿主 + deterministic fake map surface 三态证据；在 Huawei ALN-AL00 上以 `adb install -r` 覆盖安装，保留应用数据并创建临时旅行 `V2Sheet`，实际完成半屏→展开→半屏→收起→半屏以及行程 Tab 收起态；展开态辅助地图 overlay 隐藏、半屏/收起态恢复；图层菜单不会跨展开态残留，其面板在顶部栏与 Sheet 之间完整显示且不再被地图图例遮挡；无崩溃或 ANR。高德隐私说明选择“不同意”，未验证真实高德底图内容。
- 未运行检查及原因：未接受高德隐私条款，因此未验证真实高德地图 Host；三态结构证据使用 deterministic fake map surface，不能替代真实地图证据。
- 已知非阻塞差异：Pencil 使用示意地图，当前验收在未授权状态显示地图服务 fallback；App 自有顶部栏、Tabs、摘要、抽屉状态与 overlay 降级已验证。
- 下一批入口：Batch 2 地图图层 `shoPV` 的遮罩/z-order，以及工作台全空 `BrYVA` / 行程全空 `WFOpg` 的剩余映射。

### 2026-08-31 · Batch 2 · 工作台空状态与地图图层

- 实现：`shoPV` 使用三图层选项、模态遮罩、已选状态、外部关闭与全工作区输入阻断；`BrYVA` 与 `WFOpg` 均由现有 `TripWorkspaceContent` 的 UiState 分支表达，不增加 route。`BrYVA` 仅在地点池与行程均空时出现；`WFOpg` 在所有旅行日没有行程项时出现，隐藏 scope rail 与内容 CTA。
- 设计 frame：`shoPV`、`BrYVA`、`WFOpg`。
- 修改文件：workspace 图层与全空状态实现、对应 JVM/Compose 测试、typed scenario fixtures、visual evidence、scenario matrix。
- 自动化验证：Task 1–3 focused JVM tests 通过；最终相关 connected 套件 222/222 通过（修正 `V2AcceptanceTest` 中与 WFOpg 新语义冲突的旧 rail 断言后，focused 1/1 + 其余相关测试 221/221）；`VisualBatch0EvidenceTest` 22/22、BrYVA/WFOpg typed variants 3/3、`V1ScenarioCatalogTest` 47/47、`V1FullUiAcceptanceTest` 47/47、全量 unit、`lintDebug`、`assembleDebug`、`assembleDebugAndroidTest`、`git diff --check`、`graphify update .` 均通过。未纳入真实 AMap smoke tests；它们在未代用户接受高德隐私条款的模拟器上由 SDK 以 555570 拒绝。
- 设备验证：emulator-5554 已运行 production Compose workspace host 的 `shoPV`/`BrYVA`/`WFOpg` deterministic fake map evidence；稳定证据已导出至 `/tmp/easytrip-v2-batch2-evidence/`。Huawei ALN-AL00（1260×2720、520dpi）使用 `adb install -r` 保留数据覆盖安装后，创建两日空旅行并实际验证：`BrYVA`→`WFOpg` 可达且文案完整、无内容 CTA/行程 scope rail；`shoPV` 三项及选中态完整、scrim 和 picker 无严重裁切、点外与 Back 关闭、底层搜索不穿透、picker 内空白不关闭；STANDARD / SATELLITE / SATELLITE_ROAD 均可选择，SATELLITE_ROAD 在应用强制停止并重启后仍保留；未发现 crash/ANR。
- 未运行检查及原因：真机选择“不同意”高德隐私说明，未代用户接受第三方条款，因此真实 AMap host 的三图层画面切换仍未验证。
- 已知非阻塞差异：本批已通过 Pencil MCP 核对 `shoPV`、`BrYVA`、`WFOpg` 顶层 frame 与状态语义；fake map surface 和未授权真机均不能替代已授权真实地图渲染证据。
- 下一步：在用户自行接受高德隐私条款后补充真实 AMap 三图层渲染验证；App 自有 Batch 2 UI 已具备进入下一批的门禁条件。

### 2026-09-01 · Batch 3 / Task 4 · 搜索四态与连续收藏

- 实现：现有 `PlaceSearchReducer`、`PlaceSearchViewModel` 与 `PlaceSearchContent` 已覆盖本任务；未修改生产代码。新增 reducer 专项测试证明 Loading 保留 query、Empty 经清空回 Initial、Failure 保留 query 且 Retry 同关键词进入 Loading。既有 ViewModel/Compose 测试覆盖结果连续收藏、每个 POI 的 busy/error 隔离，以及结果/搜索详情不出现“加入行程”。
- 设计 frame：`ofdn5`、`s1OvvX`、`S0psO`、`GJo79`；已通过 Pencil MCP 核对可见内容与 context。`S0psO` 保留关键词、引导调整关键词；代码中的“清空搜索”将状态回到 Initial，作为恢复路径。
- 修改文件：`PlaceSearchReducerTest`、本计划。
- 自动化验证：focused JVM `PlaceSearchReducerTest` + `PlaceSearchViewModelTest` 50/50 通过；`assembleDebugAndroidTest` 通过；`git diff --check` 通过。
- 设备验证：未运行 instrumentation；`adb devices` 未发现 emulator/设备。
- 未运行检查及原因：无可用 emulator/设备，未运行 connected instrumentation；未运行全量 lint/assembleDebug（本任务无生产改动，已执行 Android test compile）。
- 已知非阻塞差异：无。
- 下一批入口：Task 5/6 或地点池/地点详情任务，均未在本 Task 4 修改。

### 2026-09-01 · Batch 3 · 地点池、搜索与地点详情

- 实现：地点池支持长、短、空三态；工作台快照原子派生地点逐日安排摘要；地图以实心/空心书签区分已安排与仅收藏；搜索保留结果、加载、无结果和失败四态；地点池行与收藏 marker 汇聚到共享详情，并复用既有加入行程状态机。修复真实设备发现的零基旅行日显示和地点详情双宿主/overlay 清理竞态，避免“第 0 天”和 Dialog 循环闪烁。
- 设计 frame：`A9EKX`、`jQhXs`、`lsr1I`、`ofdn5`、`s1OvvX`、`S0psO`、`GJo79`、`p4G1tS`、`XsGon`。
- 场景与证据：scenario 10 更新为地点池详情可加入行程、搜索详情仍无该入口；`jQhXs`（parent 02）、`XsGon`（parent 10）增加 typed variants，`S0psO` 保持 parent 27；47 个 parent 数量不变。`A9EKX`、`jQhXs`、`p4G1tS`、`XsGon` 使用 production Compose workspace host，并明确标注 deterministic fake map surface。
- 自动化验证：全量 JVM、`lintDebug`、`assembleDebug`、`assembleDebugAndroidTest`、`git diff --check` 通过；`PlacePoolFlowTest` 25/25、`AmapComposeMapTest` 14/14、`VisualBatch0EvidenceTest` 24/24 通过；详情打开与有日/无日加入行程关键流程单项通过。大型 `WorkspaceFlowTest` / 47 项场景聚合运行曾被 emulator 系统或 instrumentation process crash 中断，对应单项未复现产品断言失败。
- 设备验证：Huawei ALN-AL00 上真实 AMap 标准/卫星/卫星路网切换成功；真实搜索 `WestLake` 返回结果，连续收藏两个地点并同步地点池；仅收藏详情、加入第 1 天及已安排摘要完成闭环，修复后确认显示“第 1 天 · 1 次”。后续发现地点详情 Dialog 反复开关并已定位修复；因一次误在 Huawei 启动 instrumentation 后原两地点数据不再存在，未使用旧数据完成修复后的同场景重放。真实设备仅验证两个地点，未宣称设计中的 8 点首次全览已验收。
- 已知非阻塞差异：fake map evidence 不替代真实 AMap；间距、字体和小型视觉差异留到最终物理设备验收。
- 下一批入口：按实现计划进入下一未完成批次；先完成 frame → Composable → UiState → 导航 → 测试映射并等待批准。

### 2026-09-02 · Batch 4 / Task 4 · 多日与长列表日期选择

- 实现：单地点入口复用 `SelectTargetDayContent`，以 checkbox 切换有序多日选择；固定旅行日入口仍使用 radio 且不暴露多选。目标日条目显示第几天、可用日期和现有安排次数；单地点模式显示地点名。确认按钮仅在存在有效选择、且非提交/撤销中可用，并按一日或多日生成准确文案。
- 设计 frame：`xQfD0`、`cRdBn`、`p7U8B`；日期列表为唯一滚动区，固定标题和底部确认/提示；无日状态复用 Task 3 的“前往行程”及“暂不添加”路由。
- 修改文件：`SelectTargetDayContent`、`TripWorkspaceScreen`、`TripWorkspaceViewModel`、`WorkspaceUiModels`、`WorkspaceFlowTest`。
- 自动化验证：新增 Compose RED 用例先失败（缺少日期/次数、确认文案与末日可选择）；GREEN 后 focused 2/2 和 `WorkspaceFlowTest` 39/39 通过；`AddToItineraryStateTest`、`assembleDebug`、`assembleDebugAndroidTest`、`git diff --check` 通过。
- 设备验证：在 `easy_trip_p60pro(AVD) - 12` 完成 focused 与完整工作台 Compose 流程；未做实体设备操作。
- 已知非阻塞差异：未运行 `lintDebug`；`graphify update .` 仍报告未触及的 `NetworkMonitor.kt` 和 `RoutePlanner.kt` 语法解析警告。
- 下一批入口：Task 5 成功、部分成功、目标日失效与撤销的最终反馈 UI。

### 2026-09-02 · Batch 4 / Task 5 · 参数化加入结果

- 实现：新增 `AddToItineraryResultContent`，使用 Task 2 的聚合结果表达成功、部分成功、目标日失效和撤销成功。成功可选择单日或全程行程；部分成功逐日显示已加入/未加入组合并保留收藏；失效目标日明确重新选择且不重投；撤销后仅显示地点池入口。仍复用单一 overlay host、既有导航和 ViewModel。
- 设计 frame：`yNKT4`、`mGhKO`、`D3XZi`、`KPBBb`。
- 修改文件：`AddToItineraryResultContent`、`AddToItineraryViewModel`、`TripWorkspaceScreen`、`TripWorkspaceRoute`、`WorkspaceFlowTest`、`AddToItineraryStateTest`。
- 自动化验证：新增结果 Compose 测试先 RED（旧通用结果体缺少成功/部分/失效/撤销状态文案）；GREEN 后 focused 2/2 与完整 `WorkspaceFlowTest` 43/43 通过；`AddToItineraryStateTest`、`assembleDebug`、`assembleDebugAndroidTest`、`git diff --check` 通过。
- 设备验证：在 `easy_trip_p60pro(AVD) - 12` 执行完整 Compose 工作台流程；未做实体设备验收。
- 已知非阻塞差异：未运行 `lintDebug`；`graphify update .` 报告未触及 `NetworkMonitor.kt` 与 `RoutePlanner.kt` 的语法解析警告。

### 2026-09-02 · Batch 4 / Task 6 · 场景、证据和自动化收口

- 场景：保持 47 个编号 parent 不变；将 `xQfD0`、`cRdBn`、`Pqdkf`、`p7U8B`、`yNKT4`、`mGhKO`、`D3XZi`、`KPBBb` 分别重绑至加入行程的 typed fixture/executable。`xQfD0` 不再复用创建旅行日期表单，`mGhKO` 不再表示路线部分成功。
- 证据边界：typed executable 使用生产 Compose content 与受控 UiState；这些受控状态不是实际 Room 触发，且未声明真实 AMap 证据。所有物理设备状态仍为 `PENDING`。
- 自动化验证：新增 catalog metadata RED 先因 `xQfD0` 仍指向 `dated-trip-form` 失败；修复后 metadata 测试通过。`AddToItineraryStateTest`、`WorkspaceFlowTest` 47/47、`PlacePoolFlowTest` 25/25 均通过（`easy_trip_p60pro(AVD) - 12`）。最终静态门禁结果见 Task 6 report。
- 设备验证：Huawei ALN-AL00 使用 `adb install -r` 覆盖安装并保留现有数据/授权。以真实 3 天旅行“登封”和高德搜索结果完成：连续收藏“少林寺”“中国嵩山少林旅游武术购物城”；单地点勾选第 1、3 天并显示“加入 2 天”；提交后显示“已加入第 1 天、第 3 天”及撤销/查看行程；查看行程后全程视图中第 1、3 天均有新增地点、第 2 天为空；第 1 天入口选择两个收藏地点后直接提交，不出现重复日期选择，并显示“已加入第 1 天”；撤销后显示“已从第 1 天移除，收藏地点仍保留”，返回地点池仍有 2 个收藏地点。过程中无 crash/ANR、无 Dialog 闪烁。当前旅行仅 3 天且真实 repository 未注入失败/并发删除，故长日期、部分成功、目标日删除仅由自动化和 controlled evidence 覆盖。
- 已知非阻塞差异：模拟器日志持续出现 SDK XML v4/工具仅理解至 v3 的环境 warning；不代表产品断言失败。真实 Room 触发、真实 AMap 与物理设备验收尚未完成。

### 2026-09-03 · Batch 5 / Task 7 · Production E2E 与残余回归收口

- 实现：补齐 production AppNavigation + in-memory Room 路径边界；路段编辑在无 AMap coordinator 时允许 metadata-only 原子保存，mode override 仍明确阻断；0 秒 override 在 repository/UI 边界规范化为无 override；选择态采用可访问 selectable semantics，窄屏大字体下方式横向滚动；工作台删除确认补充相邻路线重算。
- 恢复/竞态：COMPLETED 加入结果可从空 overlay 安全恢复但不抢占权限、确认、编辑、地点详情；replacement submission 清除已处理 missing；非 Ready 工作台不消费 append completion token；marker 早点击等待首次 saved-place 快照；新 preview 立即失效旧 commit。最终整分支修复进一步以 Room v3 唯一 `idempotencyKey` 和 SavedState operation ID 支持跨进程续跑逐项加入，已提交 occurrence 不重复；undo 按项持久化剩余 ID 且把已不存在 item 视为完成。
- 设计 frame：`nAdK8`、`K336N`、`mz2IS`、`l2xCsM`、`T7aESo`、`eHTX3`、`FTIOF`、`Bcf6A`、`zvO9Z`、`J7PZ7u`；47 个 parent scenario 保持不变，`nAdK8`/`mz2IS` 作为 parent 04 typed variants。
- 证据边界：controlled production Compose host 与 production AppNavigation + in-memory Room 分开标注；自动化地图证据均为 recording fake map。用户授权后，`easy_trip_p60pro` 以 `-gpu swiftshader_indirect` 启动并实际验证高德标准/卫星/卫星路网、真实 POI 搜索收藏、单日路线、全程分组与重启持久化。默认 Apple/Metal EGL 后端进入真实 MapView 时稳定触发高德 GLThread `createContext failed: EGL_SUCCESS`，切换 SwiftShader 后消失，判定为 AVD 图形后端兼容问题。Huawei ALN-AL00 已使用保留数据的 `adb install -r` 完成真实 AMap、三天分组、多段路线、路段 override、行程项三字段、追加/取消删除/确认删除第 4 天以及重启持久化验收；本轮临时字段已恢复为空，路段 override 已恢复推荐耗时。永久拒绝定位只显示局部设置引导，已 ready 地图、路线和 marker 保持挂载；同坐标已收藏地点按 saved identity 聚焦。

### 2026-09-03 · Batch 5 / Task 6 · 单日/全程地图和安全 viewport

- 实现：单日地图只保留选中日 markers 与合法同日相邻 route；mapper 过滤 snapshot 中的伪跨日/非相邻 route，切日不残留旧 polyline geometry。全程持续按 day 保留颜色/标签，所有 itinerary days（含空日）按日期顺序进入只读 timeline。Bcf6A 的当前日空态仍保留 scope rail，并由“从地点池添加”进入固定当前日的多地点选择，不混入 WFOpg。
- viewport：`MapViewportRequest` identity 包含 scope、selected day 与完整 route geometry；普通重组不 fit，scope/day/geometry 各单次触发。Compose render identity 以稳定 `sheetLevel` anchor 注入四向 safeInsets，拖拽过程中不随实时 `dragOffset` refit；工作台 chrome/sheet insets 按可用宽高 clamp 保留正面积。`RealAmapMapHost` 对 bounds 使用 SDK `newLatLngBoundsRect`，对单点使用 deterministic zoom + 正向 center offset scroll。
- 修正：单点 offset 遵循 AMap `scrollBy` 屏幕坐标，使用 `(right-left)/2`、`(bottom-top)/2`；地点池自动 fit 仅使用已保存地点 marker，搜索结果由显式焦点请求控制。
- 设计 frame：`eHTX3`、`FTIOF`、`Bcf6A`；已通过 Pencil MCP 读取顶层可见内容与 context。
- 自动化验证：focused JVM mapper/controller/policy/layout/WholeTrip 通过；全量 JVM、`assembleDebug`、`lintDebug`、`git diff --check`、`graphify update .` 通过。focused connected workspace/WholeTrip 61/61 通过；单独 AMap suite 14/14 通过。
- 未通过 / 未运行：未完成真实多日路线数据下的实体设备/已授权 AMap viewport 验收。
- 边界：real host 已接收实际四向 camera update 与单点 offset scroll；JVM/fake host 不等于真实 AMap 底图 viewport 证据。

### 2026-09-02 · Batch 4 / Task 6 · Fix round 2 · 宿主级场景证据

- 证据：八个 Batch 4 frame 现均由参数化 `TripWorkspaceScreen` + 对应 `WorkspaceOverlay` 的 controlled UiState 通过同一 production workspace host 运行；map 使用 deterministic fake `AmapMapHost`。metadata 明确说明这不是 Room 触发状态，也不是 real AMap 证据。
- 场景交互：`Pqdkf` typed executable 真实点击两个地点并确认“已选 2 个”及 Continue；`cRdBn` typed executable 滚动后选择第 30 天，并验证确认 footer。完整工作台 `WorkspaceFlowTest` 仍提供真实 host 中长列表末日和 footer 可达的证据。
- 自动化验证：新增宿主证据 metadata 测试先编译 RED（证据 catalog 不存在），随后 GREEN；最终命令和结果见 Task 6 report。
- 设备验证：Huawei ALN-AL00 使用 `adb install -r` 保留数据与既有授权覆盖安装。以真实 3 天旅行“登封”和高德搜索结果完成：连续收藏“少林寺”“中国嵩山少林旅游武术购物城”；单地点同时选择第 1、3 天，确认按钮显示“加入 2 天”，成功结果显示“已加入第 1 天、第 3 天”；“查看行程”进入全程视图，第 1、3 天有新增地点，第 2 天为空；第 1 天入口实际多选两个收藏地点并直接提交，无重复日期选择，结果显示“已加入第 1 天”；撤销后显示“已从第 1 天移除，收藏地点仍保留”，返回地点池仍有 2 个收藏地点。未发现 crash、ANR 或 Dialog 闪烁。当前真实旅行仅 3 天，部分失败和目标日并发删除无法自然触发，仍由自动化与 controlled evidence 覆盖。

### 2026-09-03 · Batch 6 / Task 1 · 等待联网与路线计算失败

- 实现：仅 `Ready` 路段可打开交通路段编辑；`Pending`、`Calculating`、`WaitingForNetwork`、`Failed` 均不暴露编辑入口，且 ViewModel 以相同 `Ready` 条件拒绝绕过 UI 的请求。等待联网文案为“等待联网后计算”且无重试；失败路段显示“路线计算失败”与底层说明，仅可重试当前路段。Calculating 的 live region 语义保持不变。
- 设计 frame：`P7k0M`、`E3EhSv`。
- 修改文件：`RouteLegRow`、`DayItinerarySheet`、`DayItineraryViewModel` 及对应 ViewModel/Compose 测试。
- 自动化验证：JVM RED 为 `DayItineraryViewModelTest` 的 Ready-only 守卫、状态降级清理和保存防线断言；Compose RED 为旧 Waiting 文案、缺少失败恢复摘要、非 Ready 编辑入口及宿主 retry 映射。GREEN 后 `DayItineraryViewModelTest` 53/53、`ItineraryTimelineContentTest` 37/37、`OfflineRecoveryTest` 3/3 通过。
- 设备验证：`easy_trip_p60pro(AVD) - 12` 上运行 focused Compose 与 OfflineRecovery instrumentation；未进行实体设备操作。
- 已知非阻塞差异：未运行全量 lint/assemble 或全量 connected suites，本 Task 仅运行要求的 focused suites。

### 2026-09-03 · Batch 6 / Task 3 · 地图加载 watchdog 与失败恢复

- 实现：单次地图 Loading attempt 使用生命周期感知前台 watchdog，生产默认 20_000ms；只有 `RESUMED` 期间消耗时长，pause 冻结、resume 从剩余时长继续。超时以 `MapReadyTimeoutException` 通过既有 `MapHostCallbackGuard` 单次上报并进入当前 `mapAttempt` 的失败态；失败时 map content 退出并 dispose，用户“重试”仅递增 `mapAttempt` 创建新 host，不调用 `TripWorkspaceAction.Retry`，不清理页面、地点、行程或 overlay。旧 host 的迟到 ready/error 仍由既有 guard、composition key 与 `attemptId` 比对隔离。
- ready 裁决：不将 `onCreate` 成功当作 ready。SDK `OnMapLoaded` 仍可 ready；仅 `RealAmapMapHost` 声明可在 callback 前首次 render，首个 render 成功才视为 ready，覆盖真实可见但 attach callback 缺失的已知路径。默认 fake host 仍须显式 ready listener；never-ready fake 因而保持 Loading 并触发 watchdog。
- 设计 frame：`GoxB6`、`U8R5i`。文案为“正在加载地图”“地点和行程仍可继续查看”；失败为“地图暂时无法加载”“地点和行程仍可查看，请稍后重试”“重试”。
- 修改文件：`AmapComposeMap`、`MapReadyWatchdog`、`TripWorkspaceScreen`、`WorkspaceMapFallback`、地图/工作台/权限/场景测试。
- 自动化验证：先新增 RED：缺少 `MapReadyWatchdog` 的 JVM 编译失败；缺少 `readyTimeoutMillis`/`mapReadyTimeoutMillis` 注入 seam 的 Android test 编译失败。GREEN：JVM `MapReadyWatchdogTest` + `MapLifecycleControllerTest` + `MapHostCallbackGuardTest` 通过；`AmapComposeMapTest`、`WorkspaceFlowTest`、`WorkspacePermissionFlowTest` focused connected instrumentation 78/78 通过（`easy_trip_p60pro(AVD) - 12`）。覆盖 never-ready 超时单次、超时后旧 ready 忽略、pause 不消耗、retry 重建与首 host dispose、生产工作台内容保留、精确文案，以及可提前 render host 在无 SDK callback 时正常 ready。
- 设备验证：仅 AVD focused instrumentation；未执行实体设备、未进行真实 AMap attach callback 专项复验。
- 已知限制：真实 `MapView` 的 `OnMapLoaded` 在部分 attach smoke 仍可能不在 20 秒内回调；本 Task 以 `RealAmapMapHost` 首个成功 render 作为有效 ready 兼容路径，尚未以真实设备专门复验该路径。未运行全量 assemble、lint 或全量 connected suites。
- Fix Round1（focused）：`AmapMapHost.setOnReadyListener` 默认改为 no-op，成功 fake 必须显式 ready 或声明 `canRenderBeforeReady`；watchdog 以可注入时钟覆盖多次 pause/resume 累积、pause 即时超时、zero/negative timeout 与 ready/cancel 后抑制超时；timeout 参数进入内外 composition attempt key，改变时一并重建 host、callback guard 与 watchdog；失活 guard 忽略旧 host dispose 错误。RED 为上述 watchdog/guard 行为失败及默认 host/动态 timeout Android 行为失败；GREEN：`MapReadyWatchdogTest`、`MapLifecycleControllerTest`、`MapHostCallbackGuardTest` 与 `AmapComposeMapTest`、`WorkspaceFlowTest`、`WorkspacePermissionFlowTest` focused 通过，connected 82/82。未执行真实 AMap 或实体设备验收。
- Fix Round2（focused）：`canRenderBeforeReady` 仅在 lifecycle `RESUMED` 且 `host.onResume()` 成功后允许首个 render/ready，CREATED/STARTED 不 render、不启动 watchdog；resume 失败只上报一次 error，且没有 ready/render。`MapHostCallbackGuard` 在 ready 后忽略 lifecycle/render/disposal 初始错误，但保留 marker、POI、layer error 的业务 dispatch；ready 前失败仍单次终结。旧 disposal 测试增加 first-created/first-destroyed latch，确认首 host 实际创建并销毁后才断言 replacement 不受污染。RED 为 guard ready 后错误、CREATED/STARTED 提前 render、failed resume 误 render 和 ready 后异常污染；GREEN：规定 JVM tests、`compileDebugAndroidTestKotlin` 与三套 focused connected instrumentation 85/85。未执行真实 AMap 或实体设备验收。
#### Fix Round2 实现落点
- `AmapComposeMap` 以 `lifecycleResumed` gate `AndroidView.update`；`onResume` 成功后才设置状态、启动 watchdog并允许首个 render；pause/destroy/dispose 清除状态。`MapHostCallbackGuard` 的 initial error/disposal 在 `readyReported` 后不再终结，但普通 generation dispatch 不受 ready 影响。
- Round2 自动化仅为 focused AVD/本地 JVM 证据，不代表真实 AMap 或实体设备验收。

### 2026-09-03 · Batch 6 / Task 4 · 地图服务与定位权限恢复

- 实现：地图服务 consent 与设备定位拆分为独立状态链。`LocationPermissionUiState` 以 `NONE`、`EXPLANATION`、`SETTINGS` 表达 prompt，另保留永久拒绝事实、busy 与 error；首次定位仅打开 JFhZ7，确认后才发系统权限 effect，暂不使用只关闭 prompt。永久拒绝打开 HYCsZ；取消/Back 仅关闭 prompt 而保留事实，再点定位可重开；前往设置沿用 workspaceId/generation/launchStarted 恢复保护，授予后仅发一次当前定位。`WorkspaceMapState` 只保留 ConsentRequired/Loading/Ready/Failed，定位永久拒绝改为同一 `WorkspaceOverlay.PermissionExplanation` 的 settings kind，不卸载 ready map、marker 或路线。EHOHC 首次真实 workspace 在 `Undecided` 下无论 privacy shown 是否已上报都展示；Declined 不自动重弹，仅 recovery 入口打开。保留政策链接、shown report、policyRead gate、decide failure retry 和 runtime token 失效链。
- 设计 frame：`EHOHC`、`JFhZ7`、`HYCsZ`。
- 修改文件：`AppNavigation`、定位 permission coordinator/两种内容组件、workspace map state/fallback/content/route/screen/overlay，以及权限、工作台、导航 focused 测试。
- 自动化验证：RED：缺少 prompt enum/settings content、map resolver 仍依赖永久定位拒绝、EHOHC 旧 privacyReported gate 都按预期失败；GREEN：`LocationPermissionCoordinatorTest`、`LocationPermissionSourceTest`、`TripWorkspaceNavigationStateTest`、`TripWorkspaceContentStateTest` 通过；AVD `WorkspacePermissionFlowTest`、`TripWorkspaceContentTest`、production `WorkspaceFlowTest` consent/settings focused、`V1PencilFlowTest` 通过；`compileDebugAndroidTestKotlin` 通过。
- 设备验证：仅 `easy_trip_p60pro(AVD) - 12` 的 focused instrumentation 和 deterministic/recording fake map host；未操作 Android 系统权限页面、未做实体设备验收、未做真实 AMap/MapView 或第三方隐私授权验证。
- 未运行检查及原因：按 Task4 focused 范围未运行全量 lint、assemble、全量 JVM 或全量 connected suites。
- 已知非阻塞差异：统一字体缩放/窗口/IME 矩阵与真实系统设置页面留 Task 7/最终设备验收。
- Fix Round1（focused）：HYCsZ 正文改为 Pencil 原文“请前往系统设置，为 Easy Trip 开启定位权限。地图和行程仍可正常使用。”；`LocationPermissionSettingsContent` 提取同一 production `LocationPermissionSettingsBody`，280dp×600dp 容器以原 density、2x fontScale 直接渲染 body，断言两个按钮均在容器内、互不重叠且各至少 48dp；不宣称真实 Dialog/IME 窗口尺寸已验收。移除无 generation 的 `locationPermissionResults` Flow 注入契约；`AppNavigation` 测试 launcher 改为请求绑定 callback，callback 以 workspace dispose 与 active generation 失效保护。默认 Android Activity Result 路径仍保留 pending generation，并在 launcher 成功后才记录 `hasRequested`；同步 callback 先缓冲、待记录后按同 generation 投递。settings 恢复仅走 `Lifecycle.ON_RESUME` 读取 current snapshot。production root 测试从 trip list 进入 workspace，验证 JFhZ7→launcher callback 拒绝→HYCsZ→settings effect→`STARTED`→`RESUMED` grant→recording host 单次定位，并覆盖 workspace dispose 后和较新请求后旧 callback 均被忽略。完整尺寸矩阵和真实 Dialog/系统字体留 Task 7。
- 下一批入口：Batch 6 剩余最终可访问性、进程恢复与实体设备验收。

### 2026-09-03 · Batch 6 / Task 2 · 行程修改保存失败恢复

- 实现：`DayItineraryContent(showDialogs=true)` 与 `WorkspaceOverlay.EditItineraryItem` 继续使用各自既有的单一 `AlertDialog`；同一 `editDraft.saveError` 改为切换到共享 `ItinerarySaveFailureContent`，未叠加 Dialog、未新增 Overlay 或平行草稿。恢复内容说明到达时间、停留时长和备注仍保留，提示编辑不会自动回滚；“继续编辑”、失败态 Back 与点外 dismiss 都只清 `saveError`，不写 repository、不关闭 overlay；“重新保存”复用 `SaveEdit`。repository 成功后由 ViewModel 关闭 draft，后续 Room Flow 仅刷新 timeline。
- 设计 frame：`OOEsk`。
- 修改文件：`ItinerarySaveFailureContent`、`DayItinerarySheet`、`DayItineraryViewModel`、`TripWorkspaceScreen`、`DayItineraryViewModelTest`、`ItineraryEditingTest`、`ItineraryTimelineContentTest`、`WorkspaceFlowTest`。
- 自动化验证：初始 RED 为缺少 `DismissEditSaveError` 的 ViewModel 业务断言、standalone/workspace 缺少 `itinerary-save-failure` 恢复内容；Fix Round 1 RED 为失败编辑 Back 丢草稿、workspace Back 关闭 overlay、编辑组件裸显错误及受控窄宽操作区不可达。最终 GREEN 见本 Task 的 focused 测试记录。
- 设备验证：仅运行 focused instrumentation；未进行实体设备操作或全量验收。
- 未运行检查及原因：未运行全量 `assemble`、`lint`、全量 unit/connected suites；本任务按 focused 范围验证。
- 已知非阻塞差异：仅完成 2x 字体/受控 280dp 窄宽的共享失败内容基础检查（滚动正文、窄宽纵向操作区、`imePadding`）；真实 Dialog 的 280dp + IME 验证和统一尺寸矩阵留待 Task 7。

### 2026-09-03 · Batch 6 / Task 5 · 生产 AppNavigation E2E 收口

- 实现：在 `V2AcceptanceTest` 增加 OOEsk production 根链。真实导航从旅行列表进入工作台/行程，以合法 SUCCESS 路段进入编辑；测试 repository decorator 仅让首个 `updateDetails` 在写前失败，第二次委托同一 Room repository。断言失败卡出现、Room 保持旧值、`重新保存` 只发起一次第二次请求、成功后失败卡与 editor 关闭；timeline 显示两个可见字段（到达时间、停留时长），Room 与重开编辑器验证到达时间、停留时长、备注三字段均已回流。既有 Batch5 主链 fixture 同步改为真实 route repository 推进 target leg 至 SUCCESS，保持 Ready-only 编辑语义。
- 设计 frame：`OOEsk`；其 context 要求保留输入与原数据，并提供重试或取消修改入口。
- 修改文件：`V2AcceptanceTest`、本计划。
- 自动化验证：新增 root 测试首次运行已直接 GREEN（Task 2 的既有未提交生产实现已满足该行为，因此本 Task 没有可诚实记录的独立业务 RED；未为制造 RED 改动生产代码）。revised root 1/1、`V2AcceptanceTest` 4/4、`WorkspaceFlowTest` 60/60、`V1PencilFlowTest` 2/2 均通过。
- 证据边界：`ProductionAppNavigation + InMemoryRoomNavigation + RecordingFake`。
- 已知非阻塞差异：继续编辑的字段回显由既有 `ItineraryEditingTest` focused 覆盖；本 root 测试选择失败卡的“重新保存”路径以证明精确 call count。
- 下一批入口：Batch 6 最终可访问性、进程恢复与验收收口。

### 2026-09-03 · Batch 6 / Task 6 · Typed scenario、证据矩阵与回归收口

- 实现：为 `P7k0M`、`E3EhSv`、`EHOHC`、`JFhZ7`、`HYCsZ`、`GoxB6`、`U8R5i`、`OOEsk` 分别建立独立 typed factory/executable identity；30/34/35 不再共用 permission factory，48 运行共享生产保存失败恢复内容，不再仅断言裸错误。所有场景均由生产 Composable + controlled UiState 执行，并精确断言状态文案和恢复入口。
- 证据：新增 `Batch6FrameCheckpoint` / `Batch6FrameEvidence`，分别记录 host、state source、map surface 与 permission surface；每帧至少保留 `ProductionCompose + ControlledUiState` 证据。实际边界为：路线恢复使用 File-backed Room reopen；EHOHC 使用手写 repository fake 且不创建 map host；JFhZ7/HYCsZ 使用 ActivityResult contract、手写 repository fake 与可记录定位调用的 `LocationRecordingHost`；GoxB6 使用记录 host 创建/销毁/重试的 recording fake host；U8R5i 使用 failure-injecting fake host；保存失败使用 Production AppNavigation + in-memory Room + recording fake map。未声明 `AndroidSystem`、`InstalledAppRestart`、`PhysicalDevice` 或 `RealAmap` 证据。
- 矩阵：`docs/testing/v1-full-ui-scenario-matrix.md` 已修复八帧测试入口与此前失效的全矩阵引用；新增 JVM `ScenarioMatrixReferenceTest` 从 `user.dir` 向上定位仓库，扫描全部 Android test Kotlin 源的 `@Test fun`，当前报告 `references=47 missing=0`；47 个 parent scenario 数量不变。复审明确禁止运行会生成 PNG 的 `VisualBatch0EvidenceTest` 全类，本轮仅运行无截图 manifest 方法。
- TDD：metadata/evidence RED 首次 `compileDebugAndroidTestKotlin` 因缺少 Batch6 typed 场景、证据多轴类型和 evidence catalog 失败；最小实现后同一编译任务 GREEN。
- 自动化验证：本 Task 的完整 compile、metadata/catalog/full UI/evidence 及构建门禁结果见本次任务日志；未创建截图，截图不是批次门禁。
- 设备验证：本 Task 未新增实体设备或 Android 系统权限/设置页面操作；所有 PhysicalDevice UI 状态保持 `PENDING`。
- 已知限制：controlled Composable、ActivityResult callback、in-memory/File-backed Room 与 recording fake map 的组合不等价于连续全帧 E2E、真实 AMap、系统权限页、安装后重启或实体设备验收。

### 2026-09-04 · Batch 6 / Task 8 · 文件型恢复与短暂状态边界

- 实现：无生产行为改动。强化 `OfflineRecoveryTest` 为真实文件型 Room close→reopen 后启动 production `RoomRouteLegRepository` + `DefaultRouteRefreshCoordinator`：同日 WAITING_NETWORK / SUCCESS / CALCULATING 三条合法相邻路线中，离线启动只将中断 CALCULATING 恢复为 WAITING_NETWORK 且不调用 planner；联网后只 claim/规划两个待恢复路段，SUCCESS sibling 的状态、版本、距离、耗时、polyline、override 和 note 全部不变。条件等待使用 Room Flow / gate，不使用任意 sleep。
- 持久与短暂状态：新增 concrete SharedPreferences fresh-instance instrumentation，验证 AMap consent `null→accepted→declined` 及定位 `hasRequested false→true→false`。旧 `LocationPermissionCoordinator` 真实进入 SETTINGS，发出 `OpenApplicationSettings` generation，并记录 settings launch；fresh coordinator 复用同一 persisted `hasRequested` store 后初始 prompt=NONE、busy=false、error=null 且普通 resume 不发 effect，再次点击定位后才进入 SETTINGS，证明 prompt、generation 与 settings recovery 不持久。新 `DayItineraryViewModel` 共用 production file-backed Room repositories，确认 VM1 未提交 draft + saveError 在 VM2 中为空，Room 已提交时间/停留/备注不变，重开 editor 从 Room 旧值读取。
- 地图生命周期：新增 production `TripWorkspaceScreen` 新 composition 测试；首 composition 的失败 overlay 不被新 host 复用，新 composition 从 Loading / 新 attempt 开始。此项是新 composition 自动化，不冒充 Activity recreation 或 force-stop。
- TDD：上述新测试针对既有 production 行为首次可执行运行直接 GREEN；没有为了制造 RED 修改生产代码。测试开发中出现的两次失败均为测试假设/同步错误（WAITING leg 在线 claim 不递增版本；`runTest` 虚拟 timeout 与 Room 真实线程不匹配），修正测试后 GREEN。
- 自动化验证：Task 8 focused instrumentation 8/8（OfflineRecovery 3、Task8Persistence 4、new composition 1）通过；full JVM 667/667 通过；`assembleDebug`、`assembleDebugAndroidTest`、`lintDebug`、`git diff --check` 通过。非 PNG instrumentation 按类串行共 629 个唯一测试：623 pass、2 skipped、4 failed；4 个失败均在 `AmapSmokeTest`，其中 3 项明确返回 SDK 555570，transit 项未满足预期 structured-failure assertion且日志未直接保留错误码，因此 instrumentation 总门禁不是全绿。`ItineraryEditingTest` 首轮 1 项偶发失败后单项 1/1、全类 10/10 复跑通过。按要求未运行会生成 PNG 的 `VisualBatch0EvidenceTest`。
- 证据：`P7k0M` 的 `FileBackedRoomReopen` automatedEntry 仍指向 `OfflineRecoveryTest#persistedRoutesRecoverInterruptedWorkWithoutTouchingSuccess`，limitations 已强化为 production repository + coordinator、受控 planner、非导航/非 installed restart。fresh ViewModel/coordinator/composition 分别作为短暂状态边界；Activity recreation：NOT-RUN；RealAmap：NOT-RUN；AndroidSystem：NOT-RUN；PhysicalDevice：NOT-RUN。
- 模拟器 installed-app restart：emulator-5554 在最终检查时 package 未安装（此前 connected 流程已移除安装态），因此 `adb install -r app/build/outputs/apk/debug/app-debug.apk` 是首次安装，不能证明安装前用户数据保留；随后通过 UI 创建并提交旅行 `Task8Review`。在同一工作台接受高德 consent 后实际点击定位并确认 JFhZ7“允许 Easy Trip 获取你的位置”说明可见，再执行真实 `adb shell am force-stop com.yangchengwei.easytrip` 与 `adb shell am start -W -n com.yangchengwei.easytrip/.MainActivity`。启动返回 `Status: ok`、`LaunchState: COLD`、PID 6751；旅行列表及重入工作台均保留 `Task8Review`，JFhZ7 未恢复，未发生 crash/ANR。该门禁只证明首次安装后新提交旅行及已制造的 location prompt 不跨 force-stop 恢复；edit draft/save error 和 map failure 仍仅由 fresh ViewModel/composition 自动化覆盖，settings recovery 未做 force-stop。已接受地图 consent 但未将这次检查声明为 RealAmap evidence；Permission 不是 AndroidSystem；Activity recreation：NOT-RUN；PhysicalDevice：NOT-RUN。此步骤之后未再运行 connected tests。

### 2026-09-04 · Batch 6 / Task 67 · 定位权限恢复链 final-gate fix

- 实现：permission callback 先同时校验 active generation 与当前 in-flight，再清 single-flight 锁，旧 generation 的 failed/result 不再改变当前 busy、prompt、error 或 effect；settings request 以现有 `SettingsRecovery` 作为 single-flight，第二次请求不创建 generation/effect，且从 launcher 成功到 lifecycle resume 期间持续 busy；launch failure、resume 或用户 dismiss 会结束 recovery，恢复可重试，dismiss 后旧 resume 不再触发定位。JFhZ7/HYCsZ 复用原 `LocationPermissionUiState.busy/error`，不增加 overlay/reducer；主操作 busy 时 disabled，launcher 精确错误原地显示并使用 Polite live region，新 attempt 清 error。
- TDD：RED 先分别复现旧 callback 释放新锁、重复 settings 覆盖 generation、permission retry 未清 error、settings launch-started 提前解除 busy、dismiss 后旧 recovery 仍生效，以及两种 production content 缺少 `busy/error` 参数；GREEN 采用 generation/in-flight guard、`settingsRecovery != null` gate、resume/failure/dismiss 收口和 Route→Screen→唯一 `WorkspaceOverlay.PermissionExplanation` 状态透传。完整工作台首次复跑暴露旧“发起 permission 后说明 overlay 应关闭”的协议回归，定位为多余 prompt 保留并撤回；settings busy 仍按新 single-flight 需求保持到 resume/failure/dismiss。
- 自动化验证：`LocationPermissionCoordinatorTest` 30/30、`LocationPermissionSourceTest` 3/3；`WorkspacePermissionFlowTest` 13/13、`WorkspaceFlowTest` 64/64、`Task8PersistenceTest` 4/4 按类串行通过；full JVM 672/672、`compileDebugAndroidTestKotlin`、`assembleDebug`、`assembleDebugAndroidTest`、`lintDebug`、`git diff --check` 通过。按要求未运行会生成 PNG 的 `VisualBatch0EvidenceTest`，未执行 force-stop。
- 边界：本修复在 AVD instrumentation、controlled ActivityResult/settings launcher failure 和 production Compose route 上验证；未操作真实 Android 权限页/系统设置、未做实体设备或真实 AMap 验收，最终安装态留主会话恢复。

### 2026-09-04 · Batch 6 / Task 68 · 失败路段 retry 状态/版本竞态 final-gate fix

- 实现：失败路段 retry action 现在携带 UI 快照的 `expectedVersion`；`DayItineraryViewModel` 在派发前重新验证该路段仍为当前可见 `FAILED` 且版本一致，状态已回流为 `SUCCESS/Ready`、被移出当前相邻可见集或版本变化的旧点击均不调用 coordinator。`RouteLegRepository` 的版本化 retry 继续传给 `DefaultRouteRefreshCoordinator`，最终由 Room 单条 `UPDATE ... WHERE id=:id AND status='FAILED' AND version=:version` 原子裁决；未命中返回 `false`，不会清除已提交的距离、耗时、polyline 或错误以外的路线数据。既有无 version repository/coordinator retry 签名保留为兼容入口，但 production UI 路径只使用版本化重载。
- 回归边界：当前 Failed 重试仍以 online→`PENDING`、offline→`WAITING_NETWORK` 并递增 version；迟到 Ready/旧 Failed 版本不会重置目标路段；Success sibling 的状态、版本和路线数据不受影响。Ready-only 编辑资格未放宽。
- TDD：JVM RED 先证明已回流 Ready 的旧 retry 仍调用 coordinator；GREEN 后覆盖 Ready、旧 version 和当前 Failed 三类 ViewModel 入口。Room regression 加入真实 DAO/repository 的 Failed 正常更新、Ready 和 stale version 均返回 false、以及 sibling 不受影响；因本次无连接设备，Room suite 已编译但未实际执行。
- 自动化验证：`DayItineraryViewModelTest` 56/56、`RouteRefreshCoordinatorTest` 15/15、full JVM 675/675 通过；`compileDebugAndroidTestKotlin`、`assembleDebug`、`assembleDebugAndroidTest`、`lintDebug`、`git diff --check`、`graphify update .` 通过。`RoomRouteLegRepositoryTest` 的 connected 执行因 `adb devices` 无设备未运行；未运行截图类测试。
- 未运行检查及原因：最终门禁交由主会话；无可用 Android 设备，无法运行 Room 和 Compose focused connected suite。跳过所有会生成截图的测试。
- 设备验证：未运行；未 force-stop。
- 已知非阻塞差异：无。

### 2026-09-04 · Batch 6 / Task 70 · 定位权限 in-flight Locate 竞态 final-gate

- 实现：`LocationPermissionCoordinator.onLocateClick()` 在权限系统请求 in-flight 时直接忽略重复 Locate，不推进 `activeGeneration`、不清理 busy/error/prompt，也不发起已授权定位 effect；成功、失败和有效 permission result 仍由原 generation 收口。保持 callback 的 generation/workspace 隔离、bridge 默认/注入 launcher 兼容，以及 settings recovery 逻辑不变。
- TDD：新增 JVM RED 覆盖 in-flight 后重复 denied Locate、in-flight 后重复 granted Locate、旧 generation failure/result 释放锁并恢复状态后 retry 产生新 effect；初始 3 项按预期失败。最小单守卫 GREEN 后 `LocationPermissionCoordinatorTest` 33/33 通过；原 stale callback 测试改用 detach/re-attach 制造真实旧 callback，继续验证 workspace detach 隔离。
- 自动化验证：`LocationPermissionCoordinatorTest` 33/33、`LocationPermissionSourceTest` 3/3、full JVM、`compileDebugAndroidTestKotlin`、`assembleDebug`、`assembleDebugAndroidTest`、`lintDebug`、`git diff --check` 与 `graphify update .` 通过。Graphify 更新报告 6760 nodes / 15774 edges，并继续提示未触及的 `NetworkMonitor.kt`、`RoutePlanner.kt` 既有语法解析 warning。
- 设备验证：`adb devices -l` 当前无设备；因此未运行 `WorkspacePermissionFlowTest`、`WorkspaceFlowTest`、`Task8PersistenceTest` connected tests，仅完成其 Android test 编译门禁。
- 已知边界：未操作真实 Android 系统权限页、系统设置页或实体设备；未运行会生成截图的测试。

### 2026-09-05 · Batch 7 / Task 4 · 场景身份与 declared path

- 实现：保持 47 个编号 parent，不增加 E2E；所有 parent/variant 通过 number、frame、fixture、screen、factory 五维精确 identity 创建 typed executable，错误维度直接拒绝。`d1sTtb` 使用独立 controlled final state：删除“杭州 · 春日慢游”，保留“川西小环线”为 primary，并保留“泉州古城散步”为其他旅行。
- 路径边界：`reachablePath` API 完整收敛为 `declaredPath`，仅表示场景终点 metadata；Catalog 和 Full UI 不宣称真实导航。`U06l7P`、`oW9mK`、`d1sTtb` 的 production AppNavigation + in-memory Room 证据来自 `TripSettingsNavigationTest`；`p4G1tS`、`XsGon` 仅声明 production workspace Compose + controlled state + deterministic/recording fake map，不冒充 Room 触发或真实 AMap。
- TDD：临时弱化 factory identity guard 后，精确 identity 测试按预期因错误 factory 未被拒绝而 RED；恢复 guard 后 GREEN。按 Pencil `d1sTtb` context 增加精确旅行名称与保留卡断言，先因旧占位 fixture 行为 RED，再更新 controlled state 转 GREEN。
- 自动化验证：Task 4 报告记录的先前 focused connected 合并 run 为 142/142，覆盖 `V1ScenarioMetadataTest`、47 parent `V1ScenarioCatalogTest`、47 parent `V1FullUiAcceptanceTest`、Task 2 设置/删除 production navigation 与地点详情 production workspace 流；当前保留 XML 仅为随后 fix-round 的 Metadata + Catalog + Full UI 141/141，不把报告记录的先前合并 run 冒充为当前原始 XML。其余全量构建门禁见 Task 4 报告。
- 证据边界：未修改生产业务，不扩大 47 条参数化 E2E；未声明真实 AMap、Android 系统权限、安装态重启或物理设备证据。
- Fix round 1：fixture-only executable builders 全部收为 factory 私有实现，外部只能以完整 `V1ScenarioIdentity` 创建；返回 executable 现在携带并校验完整 identity，同时再核对 fixture/screen/factory。RED 分别证明缺少 identity/seam 的编译失败，以及错误 fixture、number/frame 的伪造返回值未被旧校验拒绝；GREEN 后 focused identity 4/4 通过。矩阵另明确 `d1sTtb/oW9mK` 的设计声明路径与实际 production E2E 路径不同，后者为旅行列表→继续规划→工作台更多→设置→删除这次旅行→确认→旅行列表。

### 2026-09-05 · Batch 7 / Task 5 · A9EKX 自然数据验收

- 状态：`AUTOMATION_AND_HUAWEI_REAL_AMAP_PASS`。
- 设计：Pencil `A9EKX` context 明确地点池长列表纵向滚动、首次自动 fit 全部 8 个收藏地点、用户手动移动后才允许部分地点离开视野、主抽屉为 432dp 半屏；必要截图中 marker 位于顶部栏、右侧地图控件与半屏 sheet 之外的可见地图区。
- 自动化：最终 focused JVM 29/29、当前 fresh full JVM 727/727 通过；覆盖首次 fit、普通重组、scope/day/visible-set gesture suppression、真实地点集合变化单次 `PLACE_SET_CHANGED`、显式 search focus 以及 callback latest/dispose guard。`AmapComposeMapTest` listener lifecycle seam 已编译。
- 设备：Huawei ALN-AL00 使用 `adb install -r` 覆盖安装，未清数据、未卸载、未安装 test APK、未改权限。COLD start 后 9 点初始 RealAmap fit；真实拖动至西藏/青海后行程→地点池保持；production 搜索收藏“天安门-城楼”为第 10 点后计数更新并单次 fit，5 秒后无二次移动；10 点状态再次拖动/tab 往返仍保持。
- 既有证据：自然搜索收藏至 8 点与列表滚动已在前一轮 Huawei 验收完成；本轮 9→10 点闭环补齐修复后的真实手势保持与集合变化裁决。

### 2026-09-05 · Batch 7 / A9EKX 手势保持修复

- 根因与实现：真实 AMap 使用不消费 `MapView` 触摸事件的 `AMap.OnMapTouchListener`；`ACTION_DOWN` 仅 arm，drag 超过 touch slop 或 pointer down 才单次通知业务。`AmapComposeMap` 用 latest callback 转发同一 host listener，并在 composition dispose 与 host destroy 时清除 listener。`MapViewportController` 在真实手势发生时立即撤销尚未消费的自动 viewport request，之后抑制 section / scope / selected-day / visible-set 变化及普通重组；真实地点集合变化仍只产生一次 `PLACE_SET_CHANGED` 并解除抑制。显式搜索 focus 与定位命令保持独立，不被抑制。
- TDD：新增 controller 用例先在旧 partial 实现上因 `currentRequest` 未清除而 RED；新增 production ViewModel seam 用例同样先 RED。新增 `AmapComposeMapTest#gestureListenerUsesLatestComposeCallbackAndIsClearedOnDispose` 覆盖 host 不重建时读取最新 callback、listener 仅注册一次、dispose 清理及迟到 callback 被 guard 忽略；因无设备仅完成 Android test 编译。
- 自动化验证：focused JVM 29/29、当前 fresh full JVM 727/727、`compileDebugAndroidTestKotlin`、`assembleDebug`、`assembleDebugAndroidTest`、`lintDebug`、`git diff --check` 通过。
- Huawei 复验：ALN-AL00 使用 `adb install -r` 覆盖安装，未清数据、未卸载、未安装 test APK、未改权限。COLD start 后地点池显示 9 个自然收藏并执行初始 RealAmap fit；真实拖动后切换行程→地点池，地图保持在拖动后的西藏/青海视口。随后通过 production 搜索收藏“天安门-城楼”为第 10 点，返回工作台后计数变为 10 并执行一次新 fit；5 秒后画面稳定，无再次自动移动。第 10 点场景再次真实拖动并往返 tab，仍保持拖动视口。

### 2026-09-05 · Batch 7 / Task 6 · 计划与证据账本收口

- Batch 7 结果：`U06l7P/oW9mK/d1sTtb` 已有 production AppNavigation + in-memory Room 的设置进入/返回、确认删除、精确级联、primary 切换与回栈清理证据；`d1sTtb` 另有独立 controlled final-state executable。`p4G1tS` 已在 Task 4 的 `trail_map_api36(AVD) - 16` 合并 run 中通过 production workspace saved-place-row bottom sheet 测试；`XsGon` 只有 production workspace Compose + controlled UiState + deterministic fake map 的 typed/visual 证据。以上均不冒充真实 AMap 或物理设备。
- 路径语义：scenario catalog 的 `declaredPath` 只记录设计声明终点。实际 production navigation 仅在专门的 AppNavigation/Room 测试明确运行时记为 executed；矩阵并列记录两者，不再由 metadata 推断可达。
- 历史回填：`f7rS8` 只提升为代表性页面/组件自动化与 Huawei 验收，不宣称逐组件物理验收；`DxZ2a` 只提升为分散状态 focused 自动化，整板物理验收保持 PENDING。Batch 1 按页面逐项回填 controlled、production navigation/Room 与 Huawei 证据；Batch 2 `shoPV` 已由 Huawei production workspace + RealAmap + 已授权 AMap consent 完成三图层切换；Batch 3 `lsr1I` 已在 Huawei App 自有 UI 路径验收，但当次 map surface 未授权，不能作为 RealAmap 证据。
- `A9EKX`：JVM viewport/mapper 自动化通过；Huawei production installed app 已完成自然搜索收藏 8→9→10 点、列表滚动、COLD start 初始 RealAmap fit、修复后手势/tab 往返保持和地点集合变化单次 fit，状态为 PASS。
- 自动化门禁：当前保留的 Task 4 XML 证实 `trail_map_api36(AVD) - 16` 的 Metadata + Catalog + Full UI 141/141；Task 2 两项 production navigation 与 Task 3 saved-place-row bottom sheet 仅由 Task 4 报告记录的先前 142/142 合并 run 提供可追溯结果，当前没有该 142 项 run 的原始保留 XML。Task 6 运行 full JVM、assemble、AndroidTest assemble、lint；当前无在线设备，未重跑其余 focused connected。
- 证据轴：每个 `PASS` 必须同时写明 Host、state source、map surface、permission surface、设备/构建来源；缺任一真实表面时只声明相应 automated/controlled 范围，不提升物理证据。

### 2026-09-05 · Batch 7 / Task #84 · 地图手势语义接管

- 实现：`MapTouchInteractionDetector` 只在 drag 超过 slop 或 pointer-down 时单次上报；tap/down/up 不上报，cancel/reset，dispose 后不再上报。`RealAmapMapHost` 以不消费触摸的 AMap `OnMapTouchListener` 接入，listener replacement/destroy 会 dispose 并 clear。`+/-` 继续由 `performUserViewportOperation` 先通知用户 viewport 操作、再调用 host zoom。controller 手势时撤销旧 request，抑制自动 scope/day/visible fit；真实地点集合变化单次 refit 并解除 suppression；search focus/locate 不受影响。latest callback 与 deactivate guard 保持。
- 测试宿主：`AmapComposeMapTest` 已恢复为 `createAndroidComposeRule<MainActivity>` 并通过 `scenario = rule.activityRule.scenario` 运行既有 API；28 个基线测试全部保留，新增 gesture seam 与 `zoomButtonsReportViewportOperationExactlyOnceBeforeDelegatingToHost` 后当前共 30 项；WorkspaceFlow zoom 集成测试仍为补充。此前 Huawei 运行整类的 29/30 失败，是错误改为 `ActivityScenarioRule` 后的测试基础设施失败，不作为产品失败；本任务仅编译该 instrumentation。
- 自动化：`MapTouchInteractionDetectorTest` 覆盖 tap、drag、多指、cancel/reset、dispose；`ACTION_DOWN` 仅 arm，drag 超过 slop 或 pointer down 才通知业务。focused JVM、当前 fresh full JVM 727/727、`compileDebugAndroidTestKotlin`、`assembleDebug`、`assembleDebugAndroidTest`、`lintDebug`、`git diff --check`、`graphify update .` 全部通过。根据限制未执行 `connectedDebugAndroidTest`，也未安装 test APK。
- Huawei：ALN-AL00 只通过 `adb install -r` 覆盖 production APK，未清数据、未卸载。创建真实旅行 `GestureVerify`、接受 AMap consent 后，两次不同方向的真实拖动后 行程→地点池均保持手动视口。空数据 production UI 未呈现内部 `+/-` 控件，故不声明按钮真机点击；单击地图后没有可观察的 scope fit target，亦不声明。
- 已知边界：真实 AMap 手势主链已通过；`AmapComposeMapTest` 和 WorkspaceFlow zoom instrumentation 仅编译。Graphify 更新为 6949 nodes / 16196 edges / 362 communities，并继续报告未触及的 `TripWorkspaceContentTest.kt`、`NetworkMonitor.kt`、`RoutePlanner.kt` 解析 warning。

### 2026-09-05 · Batch 9 / Task 5 · Batch 1 文档与证据收口

- 范围：仅回填 `wJ51c`、`pgrb6`、`U06l7P`、`IKTv5`、`oW9mK`、`d1sTtb` 的自动化与设备证据；`f7rS8` / `DxZ2a` 只记录本批代表组件/状态抽查，不外推为逐组件、整板或物理验收。
- 自动化：Task 1 final fix wave 在 `emulator-5554`（即 `easy_trip_p60pro(AVD) - 12`）上 `TripListContentTest` 13/13；标准高度保持 header/primary/create 固定且仅 other-list 滚动，`<500dp` 多旅行启用 primary + other trips 共用滚动区的响应式降级，header/create 仍固定。Task 2 final fix wave 在 `easy_trip_p60pro(AVD) - 12` 上 `TripSettingsContentTest` 36/36，除既有日期 Sheet 的 Back、pointer 防穿透、无障碍 modal 隔离、极小高度、本地草稿事务与 Apply 的 draft→submit exactly-once 外，补充 `DateEditorSheet` footer 的 `navigationBars` inset。合并 Batch 9 connected 49/49 仅为 `TripListContentTest` 13/13 + `TripSettingsContentTest` 36/36。另有 Task 3 `TripSettingsNavigationTest` 5/5，才证明 production AppNavigation + in-memory Room 的设置进入/删除、精确级联与回栈清理；此处数字不外推到其他 frame。
- 证据轴：`wJ51c` / `pgrb6` Host=production `TripListContent` Compose，state source=controlled `TripListUiState`，map=无，permission=未涉及，source=`emulator-5554`（`easy_trip_p60pro(AVD) - 12`）上的 `TripListContentTest`。`U06l7P` / `IKTv5` Host=production `TripSettingsContent` Compose，state source=controlled settings UiState，map=无，permission=未涉及，source=`easy_trip_p60pro(AVD) - 12` 上的 `TripSettingsContentTest`。`oW9mK` / `d1sTtb` Host=production `AppNavigation`（`d1sTtb` 另有 controlled final state），state source=in-memory Room，map=recording fake，permission=未涉及，source=`easy_trip_p60pro(AVD) - 12` 上的 `TripSettingsNavigationTest`。
- Huawei ALN-AL00 actual：production debug APK 仅以 `adb install -r` 覆盖后 cold start，未清数据、未卸载、未安装 test APK。以 production UI 从空列表创建 `SingleTrip`，再创建 `Quanzhou`、`Sichuan`、`Hangzhou`、`Dali`、`Nanjing`、`Qingdao`、`Dunhuang`；长其他旅行列表可滚动，header/primary/create 持续可见且创建入口可点。以 UI 创建 dated 三日 `DateTrip` 后，`IKTv5` Sheet 的 scrim、开始/结束、底部安全区和系统 Back 仅关闭 Sheet 均实测；settings 分组/完成/危险区可达；取消删除保留 `DateTrip`，确认后 `DateTrip` 消失、`Dali` primary、others 保留，Back 不回 deleted settings/workspace。map surface=AMap consent-declined fallback，不声明 RealAmap。上述是真机实际数据，不能冒充精确 Pencil 的杭州删除/川西 primary/泉州 other；该精确终态仍只由 `TripSettingsNavigationTest` E2E 证明。Device UI 记录 Batch 9 actual/partial，严格 fixture 差异/限制保留。
- Final fix wave：`TripListContent` 在 `<500dp` 多旅行场景采用响应式降级，header/create 保持固定，primary 与 other trips 共用滚动区；标准高度仍保持 only-other scroll。`DateEditorSheet` footer 仅应用可注入 `WindowInsets.navigationBars`，避免底部操作落入导航栏安全区，不改变 sheet 贴底或其他内容高度。
- 验证：`TripListContentTest` 13/13、`TripSettingsContentTest` 36/36，合并 connected 49/49（仅两类 content suite）；另有 `TripSettingsNavigationTest` 5/5 证明 production navigation + in-memory Room 的设置/删除/级联/回栈。`assembleDebug`、`lintDebug`、`git diff --check` 通过。既有 Huawei actual physical 记录为 PARTIAL；本轮 final fix 后未再次复验，且当时设备离线。不得声称存在原始归档 XML。
- 约束：未修改其他 frame 的证据等级；未修改 `.pen`、`.kotlin/`、`diagrams/`；Graphify outputs 留主会话统一；未 commit、未 push。

### Batch 7 完成条件

- [x] 设置页可直接完成删除旅行，并具备取消、失败、同步恢复和权威 Room 回流。
- [x] 删除后返回列表且不能返回已删除旅行页面。
- [x] 已安排/仅收藏地点详情使用同一临时 bottom sheet，并保留地图宿主。
- [x] 地点详情全部既有动作和状态迁移无回归：Batch 8 在 `easy_trip_p60pro(AVD) - 12` 实际运行 row、saved marker、仅收藏、查看→编辑、save lock、加入行程及无旅行日 focused connected 13/13；另以 production AppNavigation + in-memory Room 完成 `XsGon` 仅收藏→加入行程→`p4G1tS` 已安排回流。地图均为 recording/fake host，不冒充 RealAmap 或物理设备。
- [x] `d1sTtb` 有独立 executable；47 parent 和全部 variants identity 完整。
- [x] declared path 与 executed production navigation 证据明确分离。
- [x] Batch 0–3 按可追溯证据逐项回填。
- [x] `A9EKX` Huawei 自然数据 / RealAmap 最终验收：8→9 点既有证据与本轮 9→10 点、手势保持、tab 往返及单次集合 refit。
- [x] Task 6 仅修改实现计划、场景矩阵、矩阵引用契约和任务报告；Batch 7 Tasks 1–4 明确包含生产/测试代码改动。整批未修改 `.pen`、`.kotlin/`、`diagrams/`，未 commit、未 push。

### 2026-09-05 · Batch 8 · 地点详情闭环与证据收口

- 实现：`WorkspacePlaceDetailSheet` 顶部圆角由 12dp 收敛为 Pencil `p4G1tS` / `XsGon` 的 20dp；保留现有 490/782 响应式高度、Insets、底部对齐和地图 subtree。focused connected 暴露 `BackHandler` 闭包可能持有上一帧 `places.detailSaving=false`，导致系统 Back 在真实保存中错误关闭详情并调用 `dismissDialogs()`；`TripWorkspaceRoute` 的返回和关闭裁决现从 `placeViewModel.state.value` 读取当前权威地点状态，工作台 Back 与系统 Back 均保持保存锁。
- production 闭环：新增 `V2AcceptanceTest` 的 production `AppNavigation` + in-memory Room instrumentation。先创建一个仅收藏“西湖天地”，经真实工作台导航打开 `XsGon` bottom sheet，断言空心书签、无“已加入行程”块和“加入行程”入口；选择唯一旅行日并提交后等待 Room itinerary item 创建，关闭并重开详情，断言 `p4G1tS` 实心书签与“第 1 天 · 1 次”。
- 设计：Pencil 读取 `XsGon`、`p4G1tS`、`xQfD0` 的 context；`XsGon` 定义仅收藏详情与加入行程，`p4G1tS` 定义日/次数和实心书签，`xQfD0` 定义选择旅行日并确认。
- TDD：20dp 圆角测试先因缺少 design radius seam 编译 RED，最小生产修改后 GREEN。新增 XsGon production test 首次正确 filtered instrumentation 直接 GREEN，未人为制造 RED。详情矩阵首次实际执行稳定暴露 save-lock 系统 Back 回归；通过交换返回顺序、直接断言 UiState/overlay 和等待真实 IME inset，定位为 stale Compose capture 而非测试顺序或 IME 动画；权威 state 修复后原失败单项及 5 项工作台详情集合均 GREEN。
- 自动化：`V2AcceptanceTest#savedOnlyPlaceDetailAddsToTravelDayThroughProductionNavigationAndReopensScheduled` 1/1、完整 `V2AcceptanceTest` 5/5；`WorkspaceFlowTest` 地点详情 row/marker/edit-save-lock/add/no-day 5/5；`PlacePoolFlowTest` / `PlaceDetailPanelTest` 已安排、仅收藏、删除/取消、保存筛选及窄宽 2× 字体 8/8，均在 `easy_trip_p60pro(AVD) - 12` 通过。
- Huawei 设备验收：ALN-AL00 仅以 `adb install -r` 覆盖 production debug APK，未卸载、未清数据、未安装 test APK、未改权限。使用真实安装态旅行 `GestureVerify`，经 production AMap 搜索收藏“杭州索菲特西湖大酒店”，验证 `XsGon` 仅收藏详情：真实 AMap 保持挂载、20dp 顶角 bottom sheet、空心书签、无安排摘要、加入行程/编辑/删除可达；随后加入第 1 天，结果显示“已加入第 1 天”，重开详情显示 `p4G1tS` 的实心书签、“已加入行程”和“第 1 天 · 1 次”。查看→编辑保持同一 sheet，保存备注 `Batch8` 后回流成功；保存完成过快，未在真机上稳定观察 busy 窗口，因此系统 Back/关闭保存锁以 AVD connected 回归为准。
- 证据边界：自动化地图宿主为 `RecordingHost` / `TestMapHost` / `MarkerClickHost` fake/recording host；production 闭环 state source 为 in-memory Room，其余 focused 为 controlled repository/UiState。Huawei 手工路径使用 production installed app + RealAmap + 真实安装态 Room，permission surface 为既有已授权 AMap consent；未运行 `VisualBatch0EvidenceTest`，未生成测试 PNG。
- 产品裁决：按实现指南与用户确认，已安排地点仍允许重复加入行程，不按 `p4G1tS` 静态 disabled 表现新增去重或禁用规则。

- [x] 适用 focused connected 已在 AVD 执行；Huawei production installed app + RealAmap 的 Xs→加入行程→p4、地图保留、同 sheet 编辑与保存回流已验收。保存 busy 窗口的 Back/关闭锁仍由 AVD controlled delayed repository 证明，不冒充真机可观察证据。

### 2026-09-06 · Visual Batch 1 · 我的旅行列表族视觉收敛

- 实现：根据 `K9h3r` / `wJ51c` / `pgrb6` 的可见结构，将主卡收敛为 20dp 圆角深绿“下一站”信息卡，展示真实日期范围、天晚数、地点数、旅行日数和准备度；进度条无障碍描述使用原始计数“已安排 X/Y 个收藏地点”。倒计时统一为“日期待定 / 还有 N 天 / 旅行中 / 已结束”。其他旅行行保留整行进入与独立菜单，并增加无重复点击语义的 36dp 视觉箭头；创建入口为右下 142×52dp 胶囊。
- 数据：旅行列表 Room observable projection 同时统计 `dayCount`、`placeCount`、`scheduledDistinctPlaceCount`；同一收藏地点跨天重复安排只计一次。准备度为已安排去重收藏地点数 / 收藏地点总数，0 地点为 0%；日期、倒计时和百分比只在 UI 层派生，不持久化。
- 响应式：Header 使用 69dp 最小高度；普通高度固定 Header、主卡和创建入口，仅滚动其他旅行。单旅行短高、320×700 与 2×字体进入共享滚动降级，创建入口仍固定可达。
- 自动化验证：fresh `testDebugUnitTest`、`assembleDebug`、`assembleDebugAndroidTest`、`lintDebug`、`git diff --check` 全部通过。仅在 `emulator-5554` 执行指定 connected 集合，`RoomTripRepositoryTest`、`TripListContentTest`、`TripFlowTest`、`TripSettingsNavigationTest`、`V1ScenarioMetadataTest`、`V1ScenarioCatalogTest` 合计 149/149 通过；最终 scoped review 无 Critical / Important。
- 设备验证：AVD production installed app 实际创建 `VisualCheck` 三日旅行并返回列表，确认深绿主卡、日期待定、`3天2晚`、`0 个地点`、`3 天行程`、0% 准备度和右下创建胶囊可见，无严重裁切。Huawei ALN-AL00 仅以 `adb install -r` 覆盖 production debug APK，未安装或运行 test APK、未卸载、未清数据、未改权限；设备当时停留在锁屏且需人脸/指纹解锁，无法完成应用内列表验收，因此 Huawei 视觉与触控仍为 NOT-RUN。
- Graphify：开始前执行 `graphify query` 定位相关代码。早期 Task 1 曾提前运行 `graphify update .` 并改写生成物；最终未再次运行，因为仓库 `.gitignore` 未排除用户要求隔离的 `.kotlin/`、`diagrams/`，不能保证更新不扫描这些目录。当前图谱可能落后于最终修复，作为已知非阻塞维护项保留。
- 约束与边界：未修改 `.pen`、`.kotlin/`、`diagrams/`；未 commit、未 push。AVD 临时旅行保留，不清应用数据。本批不改变证据矩阵 PASS/PARTIAL/PENDING 数量，也不把 Compose controlled state 或 AVD 结果冒充实体设备 evidence。
- 下一批入口：继续按视觉差异优先级收敛创建/设置日期的 range-calendar sheet，或工作台行程编辑/全程路线的信息架构；剩余专项证据继续暂停。

### 2026-09-06 · Batch 2 / Task 2 · 创建旅行范围驱动

- 实现：`CreateTripUiState` 改持有 `startDate/endDate`，以闭区间派生 `dayCount`；生产 UI 移除独立天数输入与“日期待定/指定日期”切换。创建必须确认完整范围，`DateRangeChanged` 一次原子提交开始/结束日期，validator 映射既有 `CreateTrip(startDate, dayCount)`。`SavedStateHandle` 持久化两端日期；旧 `startDate + dayCount` 草稿兼容推导结束日期，不完整旧草稿必须重选范围，且会清除旧 requestId；完整、语义等价的范围仍保持 requestId 重试语义。
- Sheet：创建页接入共享 `TripDateRangePickerModalHost` / `TripDateRangePickerSheet`，背景语义在 modal 打开时隔离；取消、Back 和 scrim 仅关闭本地 sheet 草稿，确认一次才 dispatch `DateRangeChanged`。
- 测试：更新 CreateTrip validator/ViewModel、Content、Route、TripFlow、V1 scenario、Pencil flow、Room replay 与视觉 fixture，覆盖必填完整范围、闭区间映射、保存恢复/旧草稿迁移、requestId、确认一次和取消不提交。
- 自动化验证：`CreateTripValidatorTest` + `CreateTripViewModelTest` JVM 通过；`compileDebugAndroidTestKotlin` 通过；`CreateTripContentTest` 7/7、`CreateTripRouteTest`、`TripFlowTest` 6/6、`V1ScenarioCatalogTest` 47/47 均在 `emulator-5554`（`easy_trip_p60pro(AVD) - 12`）通过。
- 设备验证：仅完成上述 AVD instrumentation；尚未进行 Huawei 生产 UI 或真实 AMap 验收。
- 约束：未修改 `.pen`、`.kotlin/`、`diagrams/`，未 commit、未 push。Graphify outputs 留待安全隔离扫描后统一维护。

### 2026-09-06 · Batch 2 / Task 3 · 设置日期范围原子平移

- 实现：`DateRangeChangeRequest` 扩展为快照 `baselineStartDate`（可空）和非空 `targetStartDate/targetEndDate`；`TripDateRangeService` 始终按目标闭区间计算 1–30 天的 ordinal prefix，预览/提交仍携带开始日期及有序 day ID 快照。`DateRangeApply.expectedStartDate` 同样可空，Room 单事务先校验旧开始日期、day ID 与待删除内容快照，再写目标开始日期并扩缩 `trip_days`；不增加 `endDate` 列。相同长度平移保留所有 day ID 与内容，扩展保留前缀并追加，缩短级联尾部 content，SavedPlace 保留；未定日期可原子转已定。
- TDD：JVM 先以新的目标范围和 nullable snapshot API 编译 RED；增加平移范围完成判定后得到 RED（仍按 baseline 校验 startDate），最小修复转 GREEN。Room instrumentation 覆盖同长度平移内容保留、平移扩展、平移缩短级联及收藏保留、未定→已定、ID factory 失败 rollback；domain 覆盖 LocalDate MIN/MAX 与 30 天边界。`SchemaTest` 断言 `trips` 有 `startDate` 且无 `endDate`。
- 自动化验证：`TripDateRangeServiceTest` 与平移 completion focused JVM 通过。Task 3 当时曾在 emulator-5554 和 Huawei ALN-AL00 运行 `TripDateRangeRoomTest` / `SchemaTest`；Huawei instrumentation 违反本批最终设备约束，记录标记 **INVALID**，不计入任何自动化或物理验收证据。有效 Room/Schema 证据以 Task 6 在 `emulator-5554` 的最终重跑为准。
- 设备验证：本任务仅运行 in-memory Room instrumentation，不作生产 UI/真实 AMap 手工验收；未提前实现 Task 4 日期范围 UI 状态机。
- 约束：未修改 `.pen`、`.kotlin/`、`diagrams/`，未 commit、未 push；Graphify 更新待安全隔离扫描统一执行。

### 2026-09-06 · Visual Batch 2 / Task 4 · 设置范围选择状态机

- 实现：设置页改用与创建页共享的 `TripDateRangePickerSheet`，`DateRangeChangeUiState` 同时保存 baseline/draft 的开始与结束日期；已定旅行选择完整闭区间。未定旅行已有 N 个旅行日时只选择开始日，Sheet 自动形成同长度结束日；`fixedDayCount` 进入 remember key，运行中长度变化会重新推导 selection，提交时使用当前长度，domain 再次拒绝不同长度请求并原样呈现保留天数提示，确保 day IDs/content 不被扩缩。日期行显示完整 range 与 `N天M晚`；范围中间日期除背景色外还有连续 2dp 连接条。取消、系统 Back 与 scrim 只关闭本地 Sheet 草稿，paired range 只提交一次；平移、扩展、缩短继续复用 preview/impact/apply/Room 回读及 ViewModel single-flight/互斥状态机。
- 自动化验证：focused `TripSettingsViewModelTest`、`TripDateRangePickerSheetTest`、`TripSettingsContentTest` 均在 `emulator-5554` 通过；`compileDebugKotlin`、`compileDebugAndroidTestKotlin`、`assembleDebug`、`lintDebug`、`git diff --check` 通过。所有 connected 命令均显式 `ANDROID_SERIAL=emulator-5554`，未在 Huawei 运行 instrumentation。最终 fresh 数量以 Task 6 报告为准，不沿用 Task 4 中途测试计数。
- 证据边界：本 Task 为 controlled state / production content 自动化，不单独证明 Room 持久化；Room 与 production AppNavigation 联合证据由 Task 6 提供。IKTv5 旧 Huawei 手工记录来自已被替换的文本日期 Sheet，已标记 INVALID，不能作为新版范围日历证据；新版 Huawei 必须 fresh 验收。未修改 `.pen`、`.kotlin/`、`diagrams/`，未运行 `graphify update .`，未 commit、未 push。

### 2026-09-06 · Visual Batch 2 / Task 6 · Production 导航与 Room 集成收口

- 自动化集成：`TripSettingsNavigationTest.productionRangeTripShiftShrinkCancelConfirmAndGrowthPersistExactlyOnce` 使用 production `AppNavigation`、`TripService`、`TripSettingsViewModel`、共享范围 Sheet和 in-memory `EasyTripDatabase`。测试从空列表经生产创建入口创建三日 `Range 旅行`，为首日、中间日和末日写入可识别的 SavedPlace、ItineraryItem 完整字段及相邻 RouteLeg。
- 平移：在设置页把 2026-10-01…03 同长度平移至 2026-10-08…10，断言旧 day ID 顺序和首/末日可识别 content 实体原样保留。
- 缩短：先选择两日范围并取消，断言数据库零变化；再次选择并确认，只级联删除尾日 Item/RouteLeg，旧 day ID 前缀、前两日完整 content 和全部 SavedPlace 保留。
- 扩展：再扩为四日，断言旧 day ID 前缀保持、新增尾日 ID 由 Room 生成且新增日 Item/RouteLeg 为空。每次范围操作只点击一次确认；真正的 exactly-once 写入由既有 ViewModel single-flight 单元测试证明，本 integration 只证明单次 UI 操作链。最终 Settings → Workspace → Trip List 回栈正确，navigation observer 仅记录 create、workspace、settings 三次生产导航。
- 既有 Room 契约：`TripDateRangeRoomTest` 继续单独覆盖 snapshot 变化拒绝、rollback、LocalDate 边界、未定→已定同长度保持、同长度平移、扩缩级联与 SavedPlace 保留；删除尾部快照使用有序完整 `ItineraryItemEntity` / `RouteLegEntity` typed lists，data-class 全字段比较使等量替换同样拒绝并回滚。Task 6 integration 只补 production AppNavigation 与真实 Room 串接的最小缺口。LocalDate MIN/MAX 月历与边界月份导航另由 picker state/Compose 测试证明。
- Task 3 证据纠正：Task 3 早期曾在 Huawei ALN-AL00 运行 `TripDateRangeRoomTest` / `SchemaTest` instrumentation；该运行违反最终 connected 设备约束，明确不作为 Visual Batch 2 验收证据，也不冒充 production UI/手工证据。最终所有 Task 6 connected suites 均显式固定 `ANDROID_SERIAL=emulator-5554`。
- 自动化门禁：`./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug` fresh 通过（736 JVM tests）；`ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<TripDateRangePickerSheetTest,CreateTripContentTest,CreateTripRouteTest,TripSettingsContentTest,TripDateRangeRoomTest,TripSettingsNavigationTest>` 80/80；同设备 `<TripFlowTest,V1PencilFlowTest,V1ScenarioCatalogTest,SchemaTest>` 64/64；最终 residual `<CreateTripContentTest,TripSettingsContentTest#undatedThreeDayTripSelectsStartAndKeepsExistingLength,TripDateRangePickerSheetTest>` 19/19；最终 V1 catalog + metadata 94/94；`git diff --check` 通过。以上命令与计数直接记录于计划，不依赖 gitignored task report。
- 设备边界：Task 6 自动化仅执行 emulator-5554 instrumentation；随后主会话已完成 Huawei production UI fresh 验收，详见下方完成条件。未读取或修改 `.pen`，未触及 `.kotlin/`、`diagrams/`；未 commit、未 push。

### Visual Batch 2 完成条件

- [x] 共享 1–30 天闭区间选择器及创建页范围驱动完成。
- [x] 设置页支持同长度平移、扩展、缩短影响确认、取消和单次提交。
- [x] Room 原子应用保留 ordinal day ID 前缀，扩展只追加空日，缩短只删除尾日内容且保留 SavedPlace。
- [x] production AppNavigation + in-memory Room 自动化闭环覆盖创建、首/末日内容、平移、缩短取消/确认、扩展、持久化与回栈。
- [x] 最终 connected 自动化仅使用 `emulator-5554`；Task 3 Huawei instrumentation 已降级为无效验收证据。
- [x] Huawei production UI fresh 验收：仅 `adb install -r` 覆盖 production debug APK；在空安装态从 `PnhPb` 创建 `RangeHuawei`，打开 `YYo6U` 共享范围日历，选择 2026-09-08…09，确认摘要 `2天1晚` 后创建进入工作台；由工作台进入设置，`U06l7P` 显示完整范围与天晚数，打开 `IKTv5` 新版范围日历并将同长度范围平移至 2026-09-10…11，Room 回流后两个原旅行日连续更新，返回列表卡片同步显示新范围。未运行 test APK、未卸载、未清数据、未改权限；新建旅行保留。

### 2026-09-03 · Batch 6 / Task 7 · 响应式、IME 与无障碍矩阵

- `P7k0M` / `E3EhSv`：PASS。`ItineraryTimelineContentTest` 以 production `RouteLegContent`、280dp 容器、设备原 density 与 2x fontScale 验证等待联网文案和离线 contentDescription、等待态无编辑/重试、失败标题/详情/唯一重试、Ready sibling 可编辑；Waiting/Failed 均为 Polite live region。retry 使用同一 Compose root 的 `getUnclippedBoundsInRoot` 验证至少 48dp、位于容器和失败路段内，且不与标题、详情或相邻编辑操作重叠。父状态容器不再设置重复 contentDescription；失败 error 为空时只呈现一次“路线计算失败”。
- `GoxB6` / `U8R5i`：PASS。`WorkspacePermissionFlowTest` 直接渲染 production `WorkspaceMapFallback` 于 280dp、原 density 与 2x fontScale；Loading/Failed 文案和 action 排他，spinner contentDescription 为“地图正在加载”，两个动态状态均为 Polite live region；Failed retry 至少 48dp 且位于同一 root 的本地容器内。父状态容器不设置重复摘要 contentDescription，由 live region、子文本及加载图标语义表达状态。
- `JFhZ7`：PASS。production `PermissionExplanationContent` 复用新提取的 `PermissionExplanationBody`；直接 body 测试覆盖 280dp/2x、标题 heading、声明/视觉顺序、两个 action 至少 48dp、容器内、不重叠以及 confirm/dismiss callback 分离。未使用独立 Dialog window bounds。
- `HYCsZ`：PASS。保留并通过既有 `locationSettingsBodyActionsRemainReachableAt280dpWithTwoTimesFontScale`，直接 production body 验证标题 heading、两个 action 的 48dp、本地容器边界和非重叠；未重复建立平行测试。
- `EHOHC`：PASS。production `AmapConsentDialog` 与测试共享新提取的 `AmapConsentBody`。直接 body 在 280dp、2x、420dp 短高下验证标题 heading、正文/政策/单一 checkbox 语义、滚动正文区和固定 footer；允许/暂不允许均至少 48dp、在本地容器内且不重叠，callback 分离。`WorkspaceFlowTest` 另从 production `AppNavigation` 验证确认阅读 gate、允许决策 `true`、dialog 关闭和 runtime session 创建。未断言 Dialog window bounds，未打开外部政策页面。
- `OOEsk`：PASS。`ItineraryTimelineContentTest` 直接渲染 production `ItinerarySaveFailureContent` 于 280dp/2x，验证标题 heading、声明/视觉顺序、两个 48dp action 位于本地容器内且不重叠、callback 分离和 Polite live region；另以 300dp 短高模拟 IME 后受限可用空间，正文滚动区不覆盖固定 footer。独立 AlertDialog 的 280dp window bounds 仍为 NOT-RUN。
- IME：PASS（仅 AVD instrumentation 边界）。`ItineraryEditingTest` 在真实 `DayItinerarySheet` flow 聚焦备注输入，除输入事件外明确等待 `WindowInsetsCompat.Type.ime()` 可见；触发保存失败后再明确等待 IME inset 不可见，确认失败恢复内容的两个操作可达，随后继续编辑并验证备注草稿和保存/取消操作可达。失败态 Back 测试只发送一次 `pressBack()`，不再用条件式第二次 Back 掩盖残留 IME，验证一次 Back 即清除 `saveError` 并返回原草稿。该结果只证明 `easy_trip_p60pro(AVD) - 12` 的系统 IME inset，不外推为实体设备结论。
- TDD：路线 Waiting/Failed、地图 Loading/Failed、JFhZ7 body API、EHOHC body API/整行单一 checkbox 语义、OOEsk live region/2x 短高 footer、失败状态重复播报和四类标题 heading 均先得到预期 RED，再以最小生产修改转 GREEN。
- 自动化验证：四类合并曾通过 121/121；最终语义变更后的合并运行在 109/121 时遭遇 instrumentation process SIGKILL，失败点 `itemMenuActionsOpenBusinessOverlaysForSameItem` 单独复跑通过。随后按类稳定复跑 `WorkspaceFlowTest` 61/61，以及 `ItineraryTimelineContentTest` + `ItineraryEditingTest` + `WorkspacePermissionFlowTest` 60/60。审查收紧后的真实 IME 与单次 Back focused 分别复跑 1/1；两项首次合并复跑在第一项已通过后 instrumentation process crash，拆分类复跑均通过。末轮新增的短高 OOEsk、默认失败不重复播报、EHOHC shared body、heading 与 checkbox focused 矩阵 8/8 通过。
- 证据边界：PASS 仅指 production Composable / production AppNavigation + controlled state / AVD instrumentation。PhysicalDevice：NOT-RUN；真实 AMap/MapView：NOT-RUN；系统定位权限与设置页面：NOT-RUN；独立 Dialog 280dp window bounds：NOT-RUN。未运行会生成 PNG 的 `VisualBatch0EvidenceTest` 全类。

### 2026-09-06 · Visual Batch 3 · 工作台行程信息架构

- 实现：单日行程在同一 `DayItineraryContent` 内增加“第 N 天 · M 站”摘要；全程行程增加“全程 · N 天 · M 站”摘要，并在已知旅行开始日期时将每个分组标为连续日期与旅行日。全程仍仅复用只读地点/路段 primitive，未暴露拖动、菜单、编辑、删除或失败重试。编辑器增加 `imePadding` 与稳定 host test tag，未改变 ViewModel、Room 事务、地图 scope、权限或路线算法。
- 生产 fixture：`V2AcceptanceTest` 新增三日、第二日空、八站、SUCCESS/FAILED/WAITING_NETWORK 路段与可识别 metadata 的 Room fixture；production `AppNavigation` + recording fake map 在同一 fixture 下明确验证 day → whole → day 的 `MapScope`、selectedDayId、marker 4→8→4 与合法同日 polyline 1→1→1，并覆盖全程只读、Room 编辑回流、重开持久值、删除后 SavedPlace 保留及 bridge route。
- TDD：新增全程摘要/连续日期、单日摘要、编辑器 host root、Room fixture、production 单日/全程同步与编辑删除回流用例；先以缺少 `startDate` API 得到预期编译 RED，再最小接线转绿。fixture 初始将 PENDING leg 直接传给 `waitForNetworkIfVersionMatches` 失败；已定位 DAO 该操作仅接受 `PENDING`，在 fixture 中保持由离线 repository 产生的 `WAITING_NETWORK` 初始状态，不修改生产路线状态机。
- 自动化：全程尾部空间改为可定位的 24dp `whole-trip-bottom-spacer`，测试滚动至 spacer 后验证末项边距；zoom 超时定位为测试 fixture 的 HALF bottom sheet 覆盖控件坐标、点击误命中行程 tab，fixture 改为 COLLAPSED 后恢复真实 `performClick()`。`V2AcceptanceTest` 的 Room closed 竞态定位为 teardown 先关数据库、后卸载 Compose，现改为先 `setContent {}`、再取消 observation scope、最后关闭 Room。最终 fresh `ANDROID_SERIAL=emulator-5554` 目标 connected 集合 80/80；`./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug` 通过；`git diff --check` 通过。最终 scoped review 未发现 Critical/Important。
- Huawei production：仅以 `adb install -r` 覆盖 production debug APK，未安装 test APK、未卸载、未清数据、未直接操作数据库、未改变 Android 系统权限。覆盖后列表为空，旧 `RangeHuawei` 不可见，原因未知；通过 production UI 新建并保留 `Batch3Huawei`（2026-09-08…09），进入工作台后选择“暂不允许”应用内高德地图隐私同意，验证地图未启用 fallback、本地点/行程仍可用、地点池与行程 tab 可切换、空行程状态无崩溃或严重裁切。自然数据不足，未在 Huawei 验证单日/全程多站路线、地点编辑、路段编辑、删除与真实 AMap；不以 fake map 或自动化 fixture 冒充该证据。
- Graphify：开始前已 query。由于仓库未隔离 `.kotlin/` 与 `diagrams/`，本批未执行最终 `graphify update .`，图谱可能落后于改动；当前 git status 未出现 Graphify 生成物变更。
- 约束：未读取、修改或清理 `.kotlin/`、`diagrams/`、`design/easy-trip-v2.0.pen`；未 commit、未 push。高德地图隐私同意为应用内 gate，本批未改动 Android 系统权限。

### 2026-09-06 · Visual Batch 4 · 地点发现与加入行程

- 设计映射：`A9EKX/jQhXs/lsr1I` → `PlacePoolContent` / `SavedPlaceRow` / `PlacePoolUiState` / 工作台 `PLACE_POOL`；`ofdn5/s1OvvX/S0psO/GJo79` → `PlaceSearchContent` / `PlaceSearchUiState` / 独立 search destination；`p4G1tS/XsGon` → `WorkspacePlaceDetailSheet` + `PlaceDetailPanel` / `WorkspaceOverlay.PlaceDetail`；`xQfD0/cRdBn/p7U8B/yNKT4/mGhKO/D3XZi` → `SelectTargetDayContent` / `AddToItineraryUiState` / 工作台互斥 overlay。
- 批次裁决：搜索四态、连续收藏、搜索详情地图模式和加入行程多日/结果状态已有生产闭环及自动化，本轮不重写 reducer、Room、地图 host 或提交状态机。视觉改动集中在地点池行的信息层级和地点详情入口语义：地点池行补通用地点图标，名称/地址允许两行并保持 `＋`/更多固定 48dp；已安排地点详情不重复提供加入行程，仅收藏地点详情保留该入口；安排摘要未知时不猜测可加入能力。单地点入口继续使用语义更准确的 `editingTarget = ForPlace(placeId)`，不退回旧记录中的 `FromPlacePool`。
- 测试映射：`WorkspacePlacePoolLayoutTest` 覆盖长文本、窄宽、2×字体和固定操作区；`PlaceDetailPanelTest` 覆盖搜索/已安排/仅收藏/未知安排能力矩阵及保存锁；`PlacePoolFlowTest`、`WorkspaceFlowTest` 覆盖整行详情、行级单地点加入、目标日选择、结果/撤销和 Back。所有 connected 命令仅允许显式 `ANDROID_SERIAL=emulator-5554`。
- 实现结果：`SavedPlaceRow` 增加不伪造类别数据的通用地点图标，名称和地址各最多两行，安排状态/备注/标签合并为底部辅助信息；右侧 `＋` 与更多操作保持独立 48dp。地点池详情仅在安排状态已知且次数为 0 时显示“加入行程”，已安排、状态未知和搜索详情不显示。搜索结果头标题可收缩、数量徽章保持可见；无坐标候选不再被 UI 层阻断收藏，由 repository 返回明确持久化错误，详情地图仍安全降级。
- 自动化验证：TDD 首轮 `PlaceDetailPanelTest` 18 项中 3 项按预期 RED；最终 `ANDROID_SERIAL=emulator-5554` 执行 `PlaceDetailPanelTest`、`PlaceSearchContentTest`、`WorkspacePlacePoolLayoutTest` 合计 50/50。`compileDebugAndroidTestKotlin`、`assembleDebugAndroidTest`、`git diff --check` 通过；最终 scoped review 无 Critical/Important。
- Huawei production：仅以 `adb install -r` 覆盖 production debug APK；现有自然旅行“登封”及 6 个收藏地点可见。地点池新行结构显示通用图标、名称、地址和安排状态，右侧 `＋`/更多固定可达；打开已安排“少林寺”详情后显示“已加入行程 / 第 1 天 · 1 次 / 编辑 / 删除”，且没有重复“加入行程”入口。未安装 test APK、未卸载、未清数据、未直接操作数据库、未确认删除或改变系统权限。仅收藏详情因当前可见列表区域未找到可确认的仅收藏自然地点，未作真机结论。
- Graphify：实现 Agent 运行了 `graphify update .`，当前生成物已更新；报告 4 个未触及文件的解析 warning：`TripDateRangePickerSheetTest.kt`、`TripWorkspaceContentTest.kt`、`NetworkMonitor.kt`、`RoutePlanner.kt`。未读取或修改 `.kotlin/`、`diagrams/`，未修改 `.pen`，未 commit、未 push。

### 2026-09-06 · Visual Batch 5 · 工作台更多菜单

- 设计映射：`ijpZD` → `WorkspaceTopBar` + 新工作台更多菜单 surface；`TripWorkspaceUiState.overlay` / `WorkspaceOverlay.MoreMenu` 作为唯一互斥状态；三项操作分别连接 `TripWorkspaceAction.OpenSettings`、现有高德隐私授权入口和 `TripWorkspaceAction.Back`，不建立局部 Boolean 或平行导航状态。
- Pencil 事实：顶部三点按钮打开右上菜单，包含“旅行设置 / 修改名称、日期和旅行日”“地图授权 / 管理高德地图权限”“返回我的旅行 / 回到旅行列表”；菜单浮于地图和行程 Sheet 上方，点击外部或 Back 关闭。
- 实现边界：保留现有设置 route、地图授权 gate、返回列表导航与 `WorkspaceOverlay.LayerMenu`；只增加菜单 surface、overlay 排他、scrim/Back 和动作接线，不改变 Room、地图 SDK、权限系统或工作台业务状态。
- 测试映射：`WorkspaceChromeTest` 覆盖菜单几何、三项内容、scrim 与已有 modal 排他；`WorkspaceFlowTest` 覆盖 overlay/Back/授权行为；`TripSettingsNavigationTest` 验证经菜单进入设置并正常回栈；场景 16 从“直达设置”更新为 production 更多菜单路径。所有 connected 命令仅允许显式 `ANDROID_SERIAL=emulator-5554`。
- 实现：新增 `WorkspaceOverlay.MoreMenu` 和 `WorkspaceMoreMenu`，顶部三点只在没有其他 overlay 时打开菜单；点击外部或 Back 关闭，三个菜单动作均先关闭 overlay，再复用现有设置、隐私授权和返回列表动作。菜单 fit 不足时自动关闭，不与 LayerMenu 或保存/确认 modal 共存。菜单采用最大 260dp 的可滚动容器，确保 2×字体时第三项仍可达。为修复既有 280×280dp、2×字体下地图恢复按钮与 tabs 约 2dp 重叠，将常规 workspace sheet header 从 68dp 调整为 72dp；声明支持的 92dp 极小窗口继续使用 86dp 折叠锚点和 68dp compact header，为摘要保留 18dp 可见空间。
- 自动化验证：`compileDebugKotlin`、`compileDebugAndroidTestKotlin` 通过；首次 `WorkspaceChromeTest + TripWorkspaceContentTest` 55 项暴露 4 项测试/小窗问题，修复旧直达设置预期、controlled overlay 驱动和恢复按钮边界后，最终 fresh 55/55 通过；菜单导航联合 `TripSettingsNavigationTest + WorkspaceChromeTest + TripWorkspaceContentTest` 61/61 通过。Visual Batch 4–6 合并目标集 `PlaceDetailPanelTest + PlaceSearchContentTest + WorkspacePlacePoolLayoutTest + WorkspaceChromeTest + TripWorkspaceContentTest + TripSettingsContentTest + TripSettingsNavigationTest` 148/148；场景 16/25、V2 主链和已安排详情旧契约修正后 49/49。完整 connected 741 项曾运行至 703 项通过、2 项跳过、36 项失败：其中 34 项是未注入隐私同意的真实 AMap smoke/生命周期环境失败，3 个与本次改动相关的旧契约已定向修复并通过（计数存在重叠）；不把该次全量运行记为绿。full JVM 739/739、assembleDebug、assembleDebugAndroidTest、lintDebug、`git diff --check` 通过。最终 scoped re-review 提出的动作关闭时序、菜单高度、大字体第三项可达性和极小窗口摘要问题已修复；修复后复审无 Critical/Important。
- 设备验证：仅以 `adb install -r` 覆盖 Huawei production debug APK，现有自然旅行“登封”及 6 个收藏地点可见。`ijpZD` 三点菜单实机显示三项图标、标题和说明，无裁切；“旅行设置”正确关闭菜单并进入 `U06l7P`，系统 Back 返回工作台时菜单不残留；“地图授权”正确关闭菜单并打开应用内高德隐私说明，系统 Back 回到工作台且菜单不残留；“返回我的旅行”正确回到列表。`J7PZ7u` 在真实 4 日/6 收藏/3 行程项/2 路线段状态下显示结构化“将删除 3 个行程项、2 个路线段 / 将保留 6 个收藏地点”，仅取消未确认删除。未安装 test APK、未卸载、未清数据、未直接操作数据库、未确认授权或删除、未改变系统权限；未修改 `.pen`、`.kotlin/`、`diagrams/`，未 commit、未 push。

### 2026-09-06 · Visual Batch 6 · 设置与危险操作复核

- Pencil/代码核对：`U06l7P` 的分组设置结构、`IKTv5` 的范围日历与固定操作区、`oW9mK` 的旅行删除影响卡均无 Critical/Important 结构差异。`IKTv5` 22dp 与设计约 20dp 顶角属于非阻塞微差。
- 实现：修复 `J7PZ7u` 单日删除确认。原实现把行程项、路线段和保留收藏数量拼入单段 message，导致共享 `ConfirmationDialog` 不展示设计中的“将删除 / 将保留”摘要卡；现将行程项和路线段计数放入 `deletedItems`，收藏地点计数放入 `retainedItems`，保留不可撤销和后续日期编号/路线变化说明。未修改状态机、Room、导航或共享 Dialog。
- 自动化验证：TDD 先修改结构断言得到预期 RED；最终 `ANDROID_SERIAL=emulator-5554` 的 `TripSettingsContentTest` 37/37，`assembleDebugAndroidTest`、`git diff --check` 通过。
- 设备与证据边界：Agent 在 RED 阶段误执行一次未限定 serial 的 connected 命令，instrumentation 同时启动到 emulator 与 Huawei；该 Huawei 运行违反设备约束，明确无效且不作任何证据。后续 connected 均固定 `emulator-5554`。新版 `IKTv5` Huawei fresh 验收已记录在 Visual Batch 2；场景矩阵中的旧 PENDING/INVALID 条目需最终账本统一，不代表生产 UI 未实现。
- 约束：未修改 `.pen`，未读取或修改 `.kotlin/`、`diagrams/`，未 commit、未 push。

### 2026-09-06 · Visual Batch 7 · 旅行日管理入口

- 审计发现：`U06l7P` 设计要求旅行日添加、排序和更多操作，现有设置页仅提供删除；领域层已有 `TripService` / Room 的新增和移动能力。实施须以既有日期范围连续性和 day ID/content 契约为约束，不能为对齐截图引入会破坏日期语义的平行状态。
- 实现：设置页增加“添加一天”、48dp 旅行日操作区和更多菜单；追加复用 `TripService.appendTripDay`，保持 startDate 与既有 day ID/content。未定日期旅行可重排；已定日期旅行按连续日期顺序固定，并在 UI、ViewModel、TripService 三层拒绝重排。dayManagement 与日期/删除写入互斥，进行中锁定顶部返回、完成和系统 Back。
- 自动化：`TripSettingsViewModelTest` 与 `TripServiceTest` focused JVM 通过；`TripSettingsContentTest + TripSettingsNavigationTest` 44/44。新增区域使 220dp 测试容器中的日期行需要滚动后才能点击，两个 Sheet 几何测试修正为先 `performScrollTo()`，随后 2/2 及整组通过。`compileDebugKotlin`、`compileDebugAndroidTestKotlin`、静态全量门禁和 `git diff --check` 通过。最终 scoped review 无 Critical/Important。
- Huawei fresh：Huawei ALN-AL00 仅以 `adb install -r` 覆盖 production debug APK，保留自然旅行“登封”（2026-09-30…10-03，4 天、6 个收藏、3 个当日站点），未安装 test APK、未卸载、未清数据、未改权限。工作台更多→旅行设置实际显示“添加一天”、4 个旅行日操作区和“已设置日期的旅行日按日期连续排列”；已定日期旅行未暴露重排操作。为避免破坏自然数据，未点击“添加一天”或删除；追加事务、未定日期重排和写入锁仍由 JVM/AVD 自动化证明，不冒充真机写入证据。

### 2026-09-06 · Visual Batch 8 · 行程编辑入口

- 审计发现：`K336N` 缺“再次安排这个地点”直达入口；`T7aESo` 缺路段起终点、状态/距离/预计耗时上下文及合法重算入口。实施须复用既有 Add-to-Itinerary、RouteRefreshCoordinator/失败重试和 metadata-only 规则，不能复制 Room 实体或伪造成功路段的重算能力。
- 实现：`ItineraryItemUi/EditDraft` 保留 SavedPlace ID/name；生产 workspace 明确开启“再次安排”菜单与编辑器入口，复用 `AddToItineraryViewModel.startForPlace` 进入多目标日选择。独立 `DayItinerarySheet` 默认不暴露无法完成的动作。RouteLeg 行和编辑浮层展示起终点、当前状态、距离、预计耗时；切换方式时按钮明确“保存并重新计算路线”，仍调用现有 coordinator，metadata-only 保存保持原边界。
- 一致性修复：valid place reconcile 改用 `PlacePoolUiState.savedPlaceIds` 的全量 SavedPlace ID 集合，不再被当前标签筛选 rows 截断；ScheduleAgain 只有在 `startForPlace` 成功后才关闭当前编辑草稿并打开目标日 overlay，失败时保留草稿。
- 自动化：`ItineraryTimelineContentTest + WorkspaceFlowTest#itineraryItemScheduleAgainReusesSinglePlaceTargetDaySelection` 48/48；首次再次安排测试超时的根因是 controlled `PlacePoolUiState` 未包含有效 SavedPlace，补真实全量 `savedPlaceIds` 语义后通过。focused 编译、静态全量门禁和 `git diff --check` 通过。最终 scoped review 无 Critical/Important。
- Huawei fresh：在上述 production installed “登封”自然数据中进入第 1 天 3 站行程，打开“少林寺”行程项菜单，确认“再次安排这个地点”可达，并实际进入单地点目标日选择；页面显示第 1 天已安排 1 次、第 2–4 天尚未安排，随后用系统 Back 取消，未提交重复安排。打开“少林寺 → 少林寺-碑林”路段编辑，确认展示“已规划 · 141 米 · 预计 1 分钟”、步行/打车/驾车/公交/推荐方式、预计耗时和备注，未保存修改。当前地图因应用内 AMap consent 未启用而显示 fallback，因此本次只证明 production UI + 真实安装态 Room 的入口与上下文，不声明 RealAmap；`startForPlace` 失败保留草稿仍由 AVD 自动化证明。

### 2026-09-06 · Visual Batch 7–8 · 最终合并门禁

- `ANDROID_SERIAL=emulator-5554` 合并执行 `TripSettingsContentTest`、`TripSettingsNavigationTest`、`ItineraryTimelineContentTest` 与 `WorkspaceFlowTest#itineraryItemScheduleAgainReusesSinglePlaceTargetDaySelection`，92/92 通过。
- fresh `./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug` 成功；`git diff --check` 通过。全量 connected 的既有真实 AMap consent 环境失败不在本轮重复运行，也不被定向绿色集合覆盖或改写。
- `graphify update .` 已在 `.git/info/exclude` 本地排除 `.kotlin/`、`diagrams/` 后执行，生成 7098 nodes / 16653 edges / 386 communities。仍报告 4 个部分解析 warning：`TripDateRangePickerSheetTest.kt`、`TripWorkspaceContentTest.kt`、`NetworkMonitor.kt`、`RoutePlanner.kt`；Kotlin 编译、AndroidTest 编译和目标 connected 均通过。该本地 exclude 不进入仓库提交。
- 未修改 `.pen`，未读取或修改 `.kotlin/`、`diagrams/`，未 commit、未 push。

每次完成一批后追加：

```markdown
### YYYY-MM-DD · Batch N

- 实现：
- 设计 frame：
- 修改文件：
- 自动化验证：
- 设备验证：
- 未运行检查及原因：
- 已知非阻塞差异：
- 下一批入口：
```
