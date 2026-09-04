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
| Easy Trip UI Kit | `f7rS8` | `EasyTripTokens`、Theme、基础组件 | 颜色、字体、圆角、按钮、状态反馈 | 编译、组件 UI 测试 | 待分析 |
| 14 状态规范 | `DxZ2a` | `EmptyState`、InlineStatus、Dialog、Snackbar | 六类通用状态和恢复操作 | 组件 UI 测试 | 待分析 |
| 工作台全空 | `BrYVA` | `TripWorkspaceContent` | 地图、Tab、主抽屉结构 | Workspace UI 测试、typed scenario、模拟器证据 | 已测试 |
| 行程全空 | `WFOpg` | `WorkspaceItineraryContent` | Tab 切换和空行程状态 | Workspace UI 测试、typed scenario、模拟器证据 | 已测试 |
| 抽屉收起 | `kCc5z` | `WorkspaceBottomSheet` | `h108`、Tab、真实状态摘要与地图最大空间 | 抽屉状态测试 | 已验收 |
| 抽屉半屏 | `sWTB3` | `WorkspaceBottomSheet` | `h432`、地图与内容并存 | 抽屉状态测试 | 已验收 |
| 抽屉展开 | `f2ieZ6` | `WorkspaceBottomSheet` | `h720`、地图浮层按可用空间降级 | 层级和状态测试 | 已验收 |
| 地图图层 | `shoPV` | 地图图层浮层 | 遮罩、选项和控件层级 | Workspace UI 测试 | 已测试 |

### Batch 0 完成条件

- [ ] 设计 frame 与现有核心 Composable 映射完成。
- [ ] token 与 UI Kit 一致。
- [ ] 主抽屉三态通过测试。
- [ ] Dialog、菜单和操作抽屉不会被地图控件覆盖。
- [ ] 地图收藏/已安排标记语义统一。
- [ ] 代表性工作台状态已在设备中操作验证。

## Batch 1：我的旅行、创建和设置

| 设计界面 | Frame ID | 现有/目标代码 | 核心状态/交互 | 状态 |
|---|---|---|---|---|
| 我的旅行 | `K9h3r` | `TripListContent` | 当前旅行和少量其他旅行 | 待分析 |
| 单个旅行 | `wJ51c` | `TripListContent` | 仅一个旅行 | 待分析 |
| 多个旅行 | `pgrb6` | `TripListContent` | 仅其他旅行区域滚动，创建入口固定 | 待分析 |
| 空状态 | `zIbEu` | `TripListContent` | 创建第一个旅行 | 待分析 |
| 删除后 | `d1sTtb` | `TripListContent` | 删除杭州后川西成为当前旅行 | 待分析 |
| 创建旅行初始 | `PnhPb` | 创建旅行页面 | 空字段、按钮可点击 | 待分析 |
| 创建旅行校验 | `yIGiQ` | 创建旅行页面 | 字段错误，按钮形态不变 | 待分析 |
| 填写日期 | `YYo6U` | 日期范围选择 | 开始/结束日期和天数 | 待分析 |
| 创建旅行已填写 | `dzhkC` | 创建旅行页面 | 提交成功进入工作台 | 待分析 |
| 旅行设置 | `U06l7P` | `TripSettingsContent` | 名称、日期、方式和删除入口 | 待分析 |
| 修改出行日期 | `IKTv5` | 日期范围选择 | 与创建日期面板一致，缩短范围提示影响 | 待分析 |
| 删除旅行确认 | `oW9mK` | `ConfirmationDialog` | 影响、保留项和危险操作 | 待分析 |

### Batch 1 完成条件

- [ ] 创建、校验、日期和成功进入工作台闭环可达。
- [ ] 单旅行、多旅行、滚动、空状态和删除后状态正确。
- [ ] 修改日期正确处理缩短范围影响。
- [ ] 删除旅行确认和删除后当前旅行切换正确。

## Batch 2：旅行工作台骨架

| 设计界面 | Frame ID | 现有/目标代码 | 核心状态/交互 | 状态 |
|---|---|---|---|---|
| 地点池工作台 | `A9EKX` | `TripWorkspaceContent`、`PlacePoolSheet` | 地图、地点池、滚动和主抽屉 | 待分析 |
| 行程工作台 | `LFmzR` | `WorkspaceItineraryContent` | 日导航、行程列表和地图 | 待分析 |
| 全空状态 | `BrYVA` | `TripWorkspaceContent` | 地点池与行程均为空 | 已测试 |
| 行程全空 | `WFOpg` | `WorkspaceItineraryContent` | 所有旅行日无行程项 | 已测试 |
| 更多菜单 | `ijpZD` | 工作台菜单 | 设置和工作台级入口 | 待分析 |
| 地图图层 | `shoPV` | 地图图层浮层 | 图层选择和正确层级 | 已测试 |
| 抽屉三态 | `kCc5z` / `sWTB3` / `f2ieZ6` | `WorkspaceBottomSheet` | 收起、半屏、展开、按实时 sheetTop 降级地图浮层 | 已实现 |

### Batch 2 完成条件

自动化与 App 自有 UI 的物理设备验收已完成；真实 AMap 图层渲染仍因未接受第三方隐私条款而待验证。

- [x] 地点池/行程切换正确。
- [x] 三态抽屉可拖动且状态可恢复。
- [x] 地图控制、菜单、遮罩和抽屉层级正确。
- [x] 全空和行程全空状态入口可达。
- [x] Huawei ALN-AL00 上完成 App 自有 Batch 2 UI 与偏好持久化验收。
- [ ] 已授权环境中的真实 AMap 三图层渲染验证。

## Batch 3：地点池、搜索和地点详情

| 设计界面 | Frame ID | 现有/目标代码 | 核心状态/交互 | 状态 |
|---|---|---|---|---|
| 地点池长列表 | `A9EKX` | `PlacePoolSheet` | 8 个收藏地点、列表滚动、地图全览 | 已测试（fake map） |
| 地点池短列表 | `jQhXs` | `PlacePoolSheet` | 3 个收藏地点、一屏展示 | 已测试（fake map） |
| 地点池空状态 | `lsr1I` | `PlacePoolSheet` | 搜索第一个地点 | 待分析 |
| 搜索结果 | `ofdn5` | `PlaceSearchContent` | 连续收藏和取消收藏 | 已测试 |
| 搜索加载中 | `s1OvvX` | `PlaceSearchContent` | 保留关键词和加载反馈 | 已测试 |
| 搜索无结果 | `S0psO` | `PlaceSearchContent` | 调整关键词 | 已测试 |
| 搜索网络失败 | `GJo79` | `PlaceSearchContent` | 保留关键词和重试 | 已测试 |
| 已安排地点详情 | `p4G1tS` | 地点详情页面 | 安排日期、次数和实心书签 | 已测试（fake map） |
| 仅收藏地点详情 | `XsGon` | 地点详情页面 | 空心书签和加入行程入口 | 已测试（fake map） |

### Batch 3 完成条件

- [ ] 首次进入地点池地图展示全部收藏地点。
- [ ] 已安排和仅收藏标记与图例一致。
- [ ] 搜索结果可连续收藏/取消收藏并同步地点池。
- [ ] 四种搜索状态保留输入和恢复路径。
- [ ] 两种地点详情状态正确。

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
- [x] 最终实体设备验收完成（Huawei ALN-AL00；真实 AMap Loading→Ready、标准/卫星/卫星路网、AndroidSystem 定位说明→拒绝→永久拒绝设置引导→设置授予返回、`am force-stop` COLD start 与已提交 Room 数据保留；真实 AMap 故障未自然触发，继续由 failure-injecting host 覆盖恢复分支）。

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
