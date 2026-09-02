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
| 行程页 | `nAdK8` | `WorkspaceItineraryContent` | 某天添加、地点和路段入口 | 待分析 |
| 行程项编辑 | `K336N` | 行程项编辑页面 | 时间、停留时长和备注 | 待分析 |
| 编辑完成 | `mz2IS` | 行程页状态 | 保存后展示最新安排 | 待分析 |
| 删除行程项确认 | `l2xCsM` | `ConfirmationDialog` | 删除安排、保留收藏 | 待分析 |
| 交通路段编辑 | `T7aESo` | 路段编辑页面 | 方式、耗时和说明 | 待分析 |
| 单日路线 | `eHTX3` | 单日地图和行程 | 完整路线、安全边距和交通方式 | 待分析 |
| 全程路线 | `FTIOF` | `WholeTripItineraryContent` | 所有日期分组和全部路线 | 待分析 |
| 当天无地点 | `Bcf6A` | `WorkspaceItineraryContent` | 保留导航并引导添加 | 待分析 |
| 添加旅行日 | `zvO9Z` | 旅行日操作 | 追加旅行日 | 待分析 |
| 删除旅行日确认 | `J7PZ7u` | `ConfirmationDialog` | 删除日期和相关行程项 | 待分析 |

### Batch 5 完成条件

- [ ] 行程项和交通路段编辑入口可达且保存正确。
- [ ] 删除行程项和旅行日保持 SavedPlace。
- [ ] 单日路线完整展示并避开所有浮层。
- [ ] 全程路线按日期分组且不跨日生成 RouteLeg。

## Batch 6：异常、权限和最终收敛

| 设计界面 | Frame ID | 核心状态/交互 | 状态 |
|---|---|---|---|
| 等待联网 | `P7k0M` | 保留行程，等待路线计算 | 待分析 |
| 路线计算失败 | `E3EhSv` | 仅失败路段重试 | 待分析 |
| 地图权限说明 | `EHOHC` | 地图服务授权说明 | 待分析 |
| 定位权限说明 | `JFhZ7` | 点击定位后的用途说明 | 待分析 |
| 定位权限前往设置 | `HYCsZ` | 永久拒绝后的系统设置入口 | 待分析 |
| 地图加载中 | `GoxB6` | 保留工作台框架 | 待分析 |
| 地图加载失败 | `U8R5i` | 保留地点/行程操作并重试 | 待分析 |
| 行程保存失败 | `OOEsk` | 保留编辑输入并重试 | 待分析 |

### Batch 6 完成条件

- [ ] 本地数据在网络、地图和路线失败时保持可用。
- [ ] 权限请求时机和永久拒绝恢复正确。
- [ ] 字体缩放、长文案、小屏和进程恢复完成核对。
- [ ] 最终实体设备验收完成。

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

### 2026-09-02 · Batch 4 / Task 6 · Fix round 2 · 宿主级场景证据

- 证据：八个 Batch 4 frame 现均由参数化 `TripWorkspaceScreen` + 对应 `WorkspaceOverlay` 的 controlled UiState 通过同一 production workspace host 运行；map 使用 deterministic fake `AmapMapHost`。metadata 明确说明这不是 Room 触发状态，也不是 real AMap 证据。
- 场景交互：`Pqdkf` typed executable 真实点击两个地点并确认“已选 2 个”及 Continue；`cRdBn` typed executable 滚动后选择第 30 天，并验证确认 footer。完整工作台 `WorkspaceFlowTest` 仍提供真实 host 中长列表末日和 footer 可达的证据。
- 自动化验证：新增宿主证据 metadata 测试先编译 RED（证据 catalog 不存在），随后 GREEN；最终命令和结果见 Task 6 report。
- 设备验证：Huawei ALN-AL00 使用 `adb install -r` 保留数据与既有授权覆盖安装。以真实 3 天旅行“登封”和高德搜索结果完成：连续收藏“少林寺”“中国嵩山少林旅游武术购物城”；单地点同时选择第 1、3 天，确认按钮显示“加入 2 天”，成功结果显示“已加入第 1 天、第 3 天”；“查看行程”进入全程视图，第 1、3 天有新增地点，第 2 天为空；第 1 天入口实际多选两个收藏地点并直接提交，无重复日期选择，结果显示“已加入第 1 天”；撤销后显示“已从第 1 天移除，收藏地点仍保留”，返回地点池仍有 2 个收藏地点。未发现 crash、ANR 或 Dialog 闪烁。当前真实旅行仅 3 天，部分失败和目标日并发删除无法自然触发，仍由自动化与 controlled evidence 覆盖。

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
