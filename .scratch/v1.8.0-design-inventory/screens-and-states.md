# v1.8.0 页面与必要状态清单

返回 [盘点结论](/Users/bytedance/.codex/worktrees/6934/easy-trip/.scratch/v1.8.0-design-inventory/README.md)。基线 `d815ce903bd64e350a108e0a103ba9453994db27`；源于当前工作树，不代表本轮运行验收。

**计数口径：49 个盘点组，不是 49 个独立页面。** 只有 N01–N09 是导航页面；同一页面的状态仍归属于同一个页面。所有组在正式 v1.8.0 文件中的 frame 映射均待补齐；材料栏只是候选/参考，不能视为已覆盖。

## 导航页面（9）

### N01 我的旅行

- 类型：导航页面。入口：应用启动 / 返回我的旅行。
- 路由：`TRIP_LIST_ROUTE`。
- 必要状态：加载；无旅行；待出行/已出行筛选及筛选为空；主旅行与其他旅行；加载失败重试；准备度及已记花费。
- 候选材料：D12, D16, H16, H18。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：首页右上角必须是 App 设置入口，不能复用旧主题入口；需补当前费用摘要。
- 源码：[AppNavigation.kt:405](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt#L405)；[TripListContent.kt:45](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListContent.kt#L45)；[TripListUiModels.kt:30](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListUiModels.kt#L30)。
- 测试源码（本轮未运行）：[TripListContentTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripListContentTest.kt)。

### N02 创建旅行

- 类型：导航页面。入口：首页创建按钮。
- 路由：`CREATE_TRIP_ROUTE`。
- 必要状态：空表单；名称校验；日期范围未完整/超出30天；灵活/自驾；提交中；提交失败保留输入；成功进入工作台。
- 候选材料：D12。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：按当前表单、日期和出行方式核验，旧主画板仅作候选。
- 源码：[CreateTripContent.kt:47](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripContent.kt#L47)；[CreateTripUiState.kt:7](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripUiState.kt#L7)。
- 测试源码（本轮未运行）：[CreateTripContentTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/CreateTripContentTest.kt)；[CreateTripRouteTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/CreateTripRouteTest.kt)。

### N03 旅行工作台外壳

- 类型：导航页面。入口：首页打开旅行 / 创建成功。
- 路由：`TRIP_WORKSPACE_ROUTE`。
- 必要状态：加载；旅行不存在；读取失败重试；就绪；地图未授权/加载/失败/就绪；抽屉收起/半屏/展开；返回与遮罩层顺序；地图平移缩放、定位与恢复正北；控件选中与避让；服务商署名。
- 候选材料：D12, D16, D18, H18L。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：需要完整外壳及降级状态；搜索单页不能代表全工作台。
- 源码：[AppNavigation.kt:458](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt#L458)；[WorkspaceUiModels.kt:86](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceUiModels.kt#L86)；[TripWorkspaceScreen.kt:126](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt#L126)；[MapControls.kt:39](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/MapControls.kt#L39)。
- 测试源码（本轮未运行）：[TripWorkspaceContentTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContentTest.kt)；[WorkspaceFlowTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceFlowTest.kt)。

### N04 地点搜索

- 类型：导航页面。入口：工作台搜索按钮 / 地图搜索摘要中的列表。
- 路由：`TRIP_SEARCH_ROUTE`。
- 必要状态：初始输入及键盘；地图服务未授权；加载；结果；无结果；网络失败；定位权限不足；定位失败；有/无距我的距离；收藏忙碌/失败。
- 候选材料：D12, H18L。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：v1.8.0 单页不含搜索列表；需补距离有/无、权限和明确提交返回地图。
- 源码：[PlaceSearchContent.kt:79](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchContent.kt#L79)；[PlaceSearchReducer.kt:18](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchReducer.kt#L18)；[PlaceSearchRoute.kt:25](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchRoute.kt#L25)。
- 测试源码（本轮未运行）：[PlaceSearchContentTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/PlaceSearchContentTest.kt)；[SearchDistanceUiTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/SearchDistanceUiTest.kt)。

### N05 分享行程长图

- 类型：导航页面。入口：工作台更多 / 分享行程长图。
- 路由：`TRIP_SHARE_ROUTE`。
- 必要状态：加载行程；空行程；加载失败；全程/选一天；包含/不含备注；生成地图和长图；地图缺失降级；生成失败/过长；保存/分享中及失败。
- 候选材料：D14, H16。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：采用 R4 每日地图+清单，不混入已取消的分享日历；补各种生成状态。
- 源码：[ItineraryShareScreen.kt:50](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/share/ItineraryShareScreen.kt#L50)；[ItineraryShareScreen.kt:42](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/share/ItineraryShareScreen.kt#L42)。
- 测试源码（本轮未运行）：[ItineraryShareScreenTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/share/ItineraryShareScreenTest.kt)；[ShareNavigationTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/share/ShareNavigationTest.kt)。

### N06 旅行设置

- 类型：导航页面。入口：首页旅行菜单 / 工作台更多。
- 路由：`TRIP_SETTINGS_ROUTE`。
- 必要状态：无权威旅行数据/读取失败；名称；出行日期；灵活/自驾；旅行日列表；添加/排序/删除旅行日；写入中互斥；删除旅行入口。
- 候选材料：D12, DSET。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：只放旅行级设置；与 App 设置严格区分，核验日期影响确认及危险操作。
- 源码：[TripSettingsContent.kt:83](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsContent.kt#L83)；[TripSettingsViewModel.kt:60](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsViewModel.kt#L60)。
- 测试源码（本轮未运行）：[TripSettingsContentTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripSettingsContentTest.kt)；[TripSettingsNavigationTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripSettingsNavigationTest.kt)。

### N07 App 设置

- 类型：导航页面。入口：我的旅行右上角齿轮（空旅行也可进入）。
- 路由：`APP_SETTINGS_ROUTE`。
- 必要状态：当前主题；地图授权状态；已安装版本离线显示；存在可更新版本提示；进入主题/授权/更新。
- 候选材料：DSET, H18。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：现存对应材料为 HTML/JPG，不是可用的版本 Pencil 正式页面。
- 源码：[AppSettingsContent.kt:45](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/settings/AppSettingsContent.kt#L45)；[AppNavigation.kt:420](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt#L420)。
- 测试源码（本轮未运行）：[AppSettingsTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/settings/AppSettingsTest.kt)。

### N08 地图授权管理

- 类型：导航页面。入口：App 设置 / 地图授权。
- 路由：`APP_MAP_CONSENT_ROUTE`。
- 必要状态：未授权；已授权；阅读政策/已读勾选；展示报告失败重试；允许处理中/失败；撤回入口；不自动申请定位。
- 候选材料：DSET。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：需在完整 Pencil 中补原生页面；与首次授权弹窗、系统定位权限分开。
- 源码：[MapConsentContent.kt:18](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/settings/MapConsentContent.kt#L18)。
- 测试源码（本轮未运行）：[AppSettingsTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/settings/AppSettingsTest.kt)。

### N09 版本更新

- 类型：导航页面。入口：App 设置 / 检查更新。
- 路由：`APP_UPDATE_ROUTE`。
- 必要状态：Idle；Checking；Current；Ahead；Available；Downloading；Ready；Failed；取消下载；稍后安装；系统返回后的提示；重新下载/检查。
- 候选材料：DSET。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：需补全原生更新页面和状态，不能把未发布 main 当正式更新或模拟已安装成功。
- 源码：[AppSettingsContent.kt:102](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/settings/AppSettingsContent.kt#L102)；[UpdateController.kt:17](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/settings/UpdateController.kt#L17)；[UpdateRoute.kt:14](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/settings/UpdateRoute.kt#L14)。
- 测试源码（本轮未运行）：[AppSettingsTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/settings/AppSettingsTest.kt)。

## 旅行管理附属状态（6）

### T01 日期范围选择

- 类型：共享弹层。入口：创建旅行 / 旅行设置日期编辑。
- 必要状态：初始月；开始/结束日期；跨月/跨年；30天限制；固定天数改起始日；取消；确认受校验限制。
- 候选材料：D12。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：同一选择器的创建/设置变体归组，不重复当新页面。
- 源码：[TripDateRangePickerSheet.kt:79](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripDateRangePickerSheet.kt#L79)；[TripSettingsContent.kt:287](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsContent.kt#L287)。
- 测试源码（本轮未运行）：[TripDateRangePickerSheetTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripDateRangePickerSheetTest.kt)。

### T02 旅行卡片更多菜单

- 类型：菜单。入口：我的旅行 / 单个旅行更多。
- 必要状态：标记已出行/待出行；设置；删除；菜单关闭。
- 候选材料：D12, H16。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：需核验当前手动出行状态菜单，不把日期自动状态作为事实。
- 源码：[TripListCards.kt:243](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListCards.kt#L243)。
- 测试源码（本轮未运行）：[TripListContentTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripListContentTest.kt)。

### T03 删除旅行确认

- 类型：共享弹层。入口：首页菜单 / 旅行设置。
- 必要状态：Idle不展示；LoadingImpact；ImpactFailure；影响清单确认；删除中；失败；同步失败重试；取消。
- 候选材料：D12。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：记录删除/保留内容及不可撤销语义，不能复用泛化确认文案。
- 源码：[TripDeletionDialog.kt:20](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripDeletionDialog.kt#L20)；[TripListUiModels.kt:62](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListUiModels.kt#L62)。
- 测试源码（本轮未运行）：[TripDeletionDialogTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripDeletionDialogTest.kt)。

### T04 重命名旅行

- 类型：弹层。入口：旅行设置 / 修改旅行名称。
- 必要状态：当前名称；编辑；确认；取消恢复；其他写入锁定。
- 候选材料：D12。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：旧画板的命名、按钮与当前弹层对照后再复用。
- 源码：[TripSettingsContent.kt:457](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsContent.kt#L457)。

### T05 旅行日期变更及影响确认

- 类型：流程状态组。入口：旅行设置 / 提交日期范围。
- 必要状态：Idle；Previewing；AwaitingConfirmation；Applying；AwaitingRoom；SyncFailed；移除旅行日影响；取消/重试。
- 候选材料：D12。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：需覆盖非破坏性改期与缩短日期范围的影响确认，不以一个日期框代替整条流程。
- 源码：[DateRangeChangeRequest.kt:15](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/DateRangeChangeRequest.kt#L15)；[TripSettingsContent.kt:137](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsContent.kt#L137)。
- 测试源码（本轮未运行）：[TripSettingsContentTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripSettingsContentTest.kt)。

### T06 旅行日管理

- 类型：页面内状态组。入口：旅行设置 / 旅行日列表。
- 必要状态：添加一天；拖拽排序；删除候选；忙碌禁用；影响计算/失败；保存失败；长列表滚动。
- 候选材料：D12。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：按实际实现核验排序和日期映射；删除确认与 I05 共用语义。
- 源码：[TripSettingsContent.kt:111](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsContent.kt#L111)；[TripSettingsViewModel.kt:60](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsViewModel.kt#L60)。

## 工作台子视图（8）

### W01 地点池

- 类型：工作台子视图。入口：工作台 / 地点池标签。
- 必要状态：加载；空池；城市分组及切换；本地关键词；标签；全部/未排入/已排入；无筛选结果；收藏卡片；备注展开；批量安排入口。
- 候选材料：D12, D16, H16。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：补当前城市/排入状态/本地搜索，旧池内在线搜索不另画为正式页面。
- 源码：[PlacePoolSheet.kt:98](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolSheet.kt#L98)；[PlacePoolViewModel.kt:47](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolViewModel.kt#L47)；[PlaceCityGroups.kt:30](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceCityGroups.kt#L30)。
- 测试源码（本轮未运行）：[PlaceCityGroupingTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/PlaceCityGroupingTest.kt)；[PlacePoolFlowTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/PlacePoolFlowTest.kt)。

### W02 单日行程清单

- 类型：工作台子视图。入口：行程标签 / 选旅行日。
- 必要状态：默认设备当天（范围外第一天）；手选与返回恢复；加载/空/有行程；行程与相邻交通；拖拽排序；备注展开；当日费用及未填数；增删当天/日历按钮。
- 候选材料：D12, D13, H16, H18L。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：补默认当天和无重复竖线的日期栏；费用是总额而非六分类。
- 源码：[DayItinerarySheet.kt:122](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/DayItinerarySheet.kt#L122)；[WorkspaceNavigation.kt:72](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceNavigation.kt#L72)；[ItineraryDayActions.kt:22](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryDayActions.kt#L22)。
- 测试源码（本轮未运行）：[DefaultItineraryDayTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/workspace/DefaultItineraryDayTest.kt)；[ItineraryRailSpacingTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryRailSpacingTest.kt)。

### W03 全程行程清单

- 类型：工作台子视图。入口：行程 / 全程。
- 必要状态：全空；分日清单；连续重复地点合并；无行程日；相邻/跨日展示交通；各日已记费用；备注展开。
- 候选材料：D12, D13, H16。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：按当前全程投影核验，不把合并显示误当删除实际费用。
- 源码：[WholeTripItineraryContent.kt:27](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/WholeTripItineraryContent.kt#L27)；[ItineraryUiModels.kt:99](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryUiModels.kt#L99)。
- 测试源码（本轮未运行）：[WholeTripItineraryContentTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/WholeTripItineraryContentTest.kt)。

### W04 单日时间日历

- 类型：工作台子视图。入口：单日行程 / 日历切换。
- 必要状态：空日；未设时间待安排；时间轴地点与交通；重叠；短卡；拖入/移动/调时；保存/撤销/失败；跨午夜信息。
- 候选材料：D13, D14, H16。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：复用候选需包含后续调时提示、交通同列和短时长表现。
- 源码：[CalendarContent.kt:52](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/calendar/CalendarContent.kt#L52)；[CalendarTraffic.kt:98](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/calendar/CalendarTraffic.kt#L98)。
- 测试源码（本轮未运行）：[CalendarInteractionTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/calendar/CalendarInteractionTest.kt)；[CalendarResizeWorkspaceTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/calendar/CalendarResizeWorkspaceTest.kt)。

### W05 全程时间日历

- 类型：工作台子视图。入口：全程 / 日历切换。
- 必要状态：多日列；日期分页首尾；30天浏览；无日期；待安排计数；单日聚焦；跨日/重叠及交通。
- 候选材料：D13, D14, H16。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：补后续页头对齐与日期分页；不继承旧重复内容。
- 源码：[CalendarContent.kt:252](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/calendar/CalendarContent.kt#L252)；[CalendarContent.kt:74](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/calendar/CalendarContent.kt#L74)。
- 测试源码（本轮未运行）：[CalendarHeaderAlignmentTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/calendar/CalendarHeaderAlignmentTest.kt)。

### W06 搜索结果返回地图

- 类型：工作台子视图。入口：明确提交搜索 / 返回工作台。
- 必要状态：搜索标记；可定位/总结果数量；收起/半屏/展开；保留地点池/行程标签；关键词/列表返回；清除；同城我的位置；无位置/坐标不可用。
- 候选材料：D18, H18L。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：当前仅收起态单页有原生回读；需补其余状态、结果计数及当前位置样式核验。
- 源码：[WorkspaceSearchSummary.kt:33](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceSearchSummary.kt#L33)；[WorkspaceSearchResults.kt:10](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceSearchResults.kt#L10)；[AppNavigation.kt:754](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt#L754)。
- 测试源码（本轮未运行）：[SearchResultsMapFlowTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/workspace/SearchResultsMapFlowTest.kt)；[SearchMapFocusTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/workspace/SearchMapFocusTest.kt)。

### W07 地图图层与工作台更多

- 类型：菜单组。入口：地图图层工具 / 工作台右上角更多。
- 必要状态：标准/卫星/卫星路网；菜单空间不足回退；旅行设置；分享行程长图；返回我的旅行；关闭菜单。
- 候选材料：D12, D16, DSET。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：旧主题、地图授权菜单入口应移除；两个菜单分别归组，不混为同一画板。
- 源码：[MapControls.kt:90](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/MapControls.kt#L90)；[WorkspaceMoreMenu.kt:30](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceMoreMenu.kt#L30)；[TripWorkspaceContent.kt:315](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContent.kt#L315)。
- 测试源码（本轮未运行）：[WorkspaceChromeTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceChromeTest.kt)。

### W08 地图 POI 与收藏标记详情

- 类型：弹层组。入口：点击地图原生地点 / 收藏或行程标记。
- 必要状态：名称/地址缺失；可收藏/不可收藏；收藏/取消收藏中及失败；同地点多次行程；关闭。
- 候选材料：D12。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：分别保留地图 POI 和收藏标记两种容器，不能与完整收藏地点详情混为一页。
- 源码：[TripWorkspaceScreen.kt:728](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt#L728)；[TripWorkspaceScreen.kt:753](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt#L753)。

## 地点详情与编辑（3）

### P01 收藏地点详情与编辑

- 类型：弹层状态组。入口：地点池卡片 / 收藏地点。
- 必要状态：只读详情；未安排/按天重复安排/安排信息未知；收藏状态；备注和标签编辑；预设/新标签/校验；保存中/失败；取消；加入行程；删除。
- 候选材料：D12, H16。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：保留只读与编辑状态；地点评注不是行程某次安排的备注或费用。
- 源码：[PlaceDetailPanel.kt:58](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceDetailPanel.kt#L58)；[WorkspacePlaceDetailSheet.kt:23](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/place/ui/WorkspacePlaceDetailSheet.kt#L23)。
- 测试源码（本轮未运行）：[PlaceDetailPanelTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/PlaceDetailPanelTest.kt)。

### P02 搜索结果地图详情与编辑

- 类型：搜索子视图。入口：搜索列表 / 点结果。
- 必要状态：地图加载/失败；未收藏/已收藏；收藏切换；地址及安排信息；已收藏地点备注/标签编辑；返回结果列表。
- 候选材料：D12。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：与 W08 原生 POI 弹窗、W06 搜索返回地图区分；当前生产调用 detailContent 默认路径。
- 源码：[PlaceSearchContent.kt:136](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchContent.kt#L136)；[PlaceSearchViewModel.kt:29](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchViewModel.kt#L29)。
- 测试源码（本轮未运行）：[PlaceDetailPanelTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/PlaceDetailPanelTest.kt)；[PlaceSearchContentTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/PlaceSearchContentTest.kt)。

### P03 删除收藏/取消收藏影响确认

- 类型：共享弹层组。入口：地点池删除 / 搜索或地图取消已安排行程的收藏。
- 必要状态：受影响行程/路线清单；计算中/失败；确认；处理中；失败保留；取消。
- 候选材料：D12。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：必须区分删除收藏连带安排与仅移除一次行程；费用删除另由 F02 确认。
- 源码：[TripWorkspaceScreen.kt:641](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt#L641)；[PlaceSearchRoute.kt:83](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchRoute.kt#L83)。
- 测试源码（本轮未运行）：[PlacePoolFlowTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/PlacePoolFlowTest.kt)；[ExpenseCancellationUiTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/expense/ExpenseCancellationUiTest.kt)。

## 行程安排与编辑（10）

### I01 从地点池选地点

- 类型：编辑弹层。入口：当天添加 / 地点池批量安排。
- 必要状态：无候选；未选/多选；关键词/城市筛选；选择计数；固定目标日失效；继续/取消。
- 候选材料：D12。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：按当前固定日期与批量入口分别表达；不把测试宿主中的屏幕当额外导航。
- 源码：[SelectPlacesContent.kt:51](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/SelectPlacesContent.kt#L51)；[TripWorkspaceScreen.kt:555](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt#L555)。
- 测试源码（本轮未运行）：[PlacePickerRefinementTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/PlacePickerRefinementTest.kt)。

### I02 选择安排日期

- 类型：编辑弹层。入口：选地点后继续 / 地点详情加入 / 再次安排。
- 必要状态：多日选择；已有安排次数；无旅行日；添加一天入口；提交中；日期失效；取消。
- 候选材料：D12。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：需要覆盖单地点多次安排，不把已排入状态作为禁用理由。
- 源码：[SelectTargetDayContent.kt:32](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/SelectTargetDayContent.kt#L32)；[AddToItineraryUiState.kt:31](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/AddToItineraryUiState.kt#L31)。

### I03 安排异常结果与恢复

- 类型：结果弹层。入口：批量安排部分失败 / 日期失效 / 撤销。
- 必要状态：部分成功；失败地点重试；目标日失效重选；撤销中/失败；已撤销查看地点池；成功自动收起而不弹成功对话框。
- 候选材料：D12。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：不得照搬旧成功结果弹窗；当前全成功会关闭。
- 源码：[AddToItineraryResultContent.kt:11](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/AddToItineraryResultContent.kt#L11)；[TripWorkspaceScreen.kt:572](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt#L572)。
- 测试源码（本轮未运行）：[AddPlacesRoomIntegrationTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/AddPlacesRoomIntegrationTest.kt)。

### I04 添加旅行日

- 类型：弹层。入口：工作台日期栏新增 / 选日期无日可用。
- 必要状态：追加末尾说明；确认；添加中；失败重试；取消。
- 候选材料：D12。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：同一能力多个入口共用状态，不凭旧版本另画不同流程。
- 源码：[AddTripDayContent.kt:13](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/AddTripDayContent.kt#L13)；[TripWorkspaceScreen.kt:697](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt#L697)。

### I05 删除旅行日

- 类型：共享确认流程。入口：单日头部删除 / 旅行设置删日。
- 必要状态：影响数量；保留收藏说明；忙碌；错误；取消；确认删除；后续日期/范围变化。
- 候选材料：D12。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：核对两个宿主的实际确认容器和按钮，不覆盖费用损失确认。
- 源码：[WorkspaceDayDeletionDialog.kt:9](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/WorkspaceDayDeletionDialog.kt#L9)；[TripSettingsContent.kt:97](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsContent.kt#L97)。
- 测试源码（本轮未运行）：[ItineraryDayActionsTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryDayActionsTest.kt)；[TripSettingsContentTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripSettingsContentTest.kt)。

### I06 编辑单次行程

- 类型：编辑弹层。入口：行程项编辑 / 日历详情编辑。
- 必要状态：到达时间滚轮及分时段；停留时长；未设时间；备注；花费空/0/小数/无效/溢出；再次安排；保存中；取消保留原值。
- 候选材料：D12, D13, H16。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：旧 v1.6 截图无费用输入；补现有金额，不加入 v1.9 分类。
- 源码：[EditItineraryItemContent.kt:21](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/EditItineraryItemContent.kt#L21)；[ItineraryTimingPickers.kt:55](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryTimingPickers.kt#L55)；[ItineraryEditDraft.kt:7](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditDraft.kt#L7)。
- 测试源码（本轮未运行）：[ItineraryEditingTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt)；[ItineraryTimingPickerTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryTimingPickerTest.kt)；[ExpenseCancellationUiTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/expense/ExpenseCancellationUiTest.kt)。

### I07 编辑交通路段

- 类型：编辑弹层。入口：点交通 / 日历详情查看交通。
- 必要状态：起终点/距离；步行/打车/驾车/公交；恢复推荐；覆盖/恢复用时；备注；金额及无效输入；保存中/失败；取消。
- 候选材料：D12, D13。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：按当前推荐与手动覆盖表达，并补费用字段。
- 源码：[EditRouteLegContent.kt:34](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/EditRouteLegContent.kt#L34)；[ItineraryEditDraft.kt:41](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditDraft.kt#L41)；[TripWorkspaceScreen.kt:618](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt#L618)。
- 测试源码（本轮未运行）：[ItineraryEditingTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt)；[ExpensePersistenceTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/expense/ExpensePersistenceTest.kt)。

### I08 行程项菜单与跨日移动

- 类型：菜单/弹层组。入口：行程项更多。
- 必要状态：编辑；再次安排；移动到其他日期；删除；目标日期列表排除当天；移动中/失败；取消。
- 候选材料：D12, D13。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：菜单与目标日弹层分状态；跨日移动后原费用保持在该次安排。
- 源码：[ItineraryItemMenu.kt:28](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryItemMenu.kt#L28)；[TripWorkspaceScreen.kt:532](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt#L532)。
- 测试源码（本轮未运行）：[ItineraryEditingTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt)。

### I09 移出单次行程确认

- 类型：弹层。入口：行程项菜单 / 删除行程项。
- 必要状态：仅移除本次安排；收藏保留；相邻路线重算；确认/取消；删除中/失败。
- 候选材料：D12。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：与删除收藏、删除旅行日、费用删除四种风险文案分别归属。
- 源码：[TripWorkspaceScreen.kt:651](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt#L651)；[ItineraryEditDraft.kt:67](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditDraft.kt#L67)。
- 测试源码（本轮未运行）：[ExpenseCancellationUiTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/expense/ExpenseCancellationUiTest.kt)。

### I10 编辑保存失败恢复

- 类型：编辑弹层状态。入口：行程编辑保存失败。
- 必要状态：原因；继续编辑；重试保存；关闭错误后回草稿。
- 候选材料：D12。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：属于 I06 的错误状态，不是独立导航；交通编辑错误由 I07 内联展示。
- 源码：[ItinerarySaveFailureContent.kt:24](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItinerarySaveFailureContent.kt#L24)；[TripWorkspaceScreen.kt:512](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt#L512)。

## 日历交互（2）

### C01 日历详情与交叠列表

- 类型：底部弹层组。入口：点日历卡片 / 交叠组。
- 必要状态：交叠成员列表；单次日程日期/次序/到达/结束/停留；长备注；时间冲突；交通来不及；入站/出站交通；转编辑；条目已删除。
- 候选材料：D13, D14。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：交叠列表和单条详情分画板；没有源码的费用/分类字段不要补入详情。
- 源码：[CalendarContent.kt:341](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/calendar/CalendarContent.kt#L341)；[CalendarDetail.kt:17](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/calendar/CalendarDetail.kt#L17)。
- 测试源码（本轮未运行）：[CalendarInteractionTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/calendar/CalendarInteractionTest.kt)。

### C02 日历拖动与调时反馈

- 类型：页面内状态组。入口：单日长按移动 / 调整起止。
- 必要状态：未触发/长按门槛；拖动草稿；调时提示避手指；大字体/顶部边界；保存中；成功撤销；失败重试；撤销失败。
- 候选材料：D13, D14, H16。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：采用后续调时修订，不复制原始日历设计中的旧提示位置。
- 源码：[CalendarResizeHint.kt:34](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/calendar/CalendarResizeHint.kt#L34)；[CalendarTimingController.kt:8](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/calendar/CalendarTimingController.kt#L8)；[CalendarContent.kt:237](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/calendar/CalendarContent.kt#L237)。
- 测试源码（本轮未运行）：[CalendarResizeWorkspaceTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/calendar/CalendarResizeWorkspaceTest.kt)。

## 现有费用能力（2）

### F01 现有费用录入与总额展示

- 类型：跨页面能力。入口：首页卡片 / 日行程 / 全程各日 / 行程及交通编辑。
- 必要状态：未记金额与0元区分；两位小数；已记总额；未填项数量；非法输入；合计溢出拒绝；编辑保存后刷新。
- 候选材料：D12。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：未找到 v1.7.0 正式 Pencil；金额能力需映射进各现有页面，当前无独立费用统计页和六分类。
- 源码：[Expense.kt:15](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/expense/Expense.kt#L15)；[ExpenseField.kt:13](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/expense/ExpenseField.kt#L13)；[TripListUiModels.kt:105](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListUiModels.kt#L105)；[ItineraryUiModels.kt:127](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryUiModels.kt#L127)。
- 测试源码（本轮未运行）：[ExpensePersistenceTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/expense/ExpensePersistenceTest.kt)；[V170UiTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/v170/V170UiTest.kt)。

### F02 已记费用丢失二次确认

- 类型：全局弹层。入口：调整/删除引起已记花费丢失。
- 必要状态：受影响笔数/金额；交通会重新生成且费用不转移；删除花费并继续；取消调整无错误提示。
- 候选材料：D12。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：补独立风险确认，不与删除旅行/收藏的内容确认合并。
- 源码：[ExpenseRemovalDialog.kt:11](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/expense/ExpenseRemovalDialog.kt#L11)；[AppNavigation.kt:345](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt#L345)。
- 测试源码（本轮未运行）：[ExpenseCancellationUiTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/expense/ExpenseCancellationUiTest.kt)。

## 主题与授权（5）

### A01 主题选择与预览

- 类型：全局全屏弹层。入口：App 设置 / 主题配色。
- 必要状态：湖畔晴空；松林晨光；落日陶土；山岚暮紫；玫瑰沙丘；当前项禁用应用；本页预览；应用中/失败；返回放弃；成功 Toast。
- 候选材料：D16, H16。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：旧双入口失效；保留五主题与预览规则，没有虚构旅行预览卡片。
- 源码：[ThemeHost.kt:20](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/core/ui/theme/ThemeHost.kt#L20)；[ThemePickerScreen.kt:30](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/core/ui/theme/ThemePickerScreen.kt#L30)；[ThemePalette.kt:7](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/core/ui/theme/ThemePalette.kt#L7)。
- 测试源码（本轮未运行）：[ThemePickerTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/core/ui/theme/ThemePickerTest.kt)；[ThemeNavigationTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/core/ui/theme/ThemeNavigationTest.kt)。

### A02 首次地图服务授权

- 类型：应用弹层。入口：首次工作台 / 地图未授权恢复。
- 必要状态：政策入口；已读勾选；展示报告失败重试；允许/暂不允许；忙碌/失败；拒绝仍可用离线内容。
- 候选材料：D12, DSET。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：不等同 N08 管理页，不凭权限弹窗猜测地图服务授权。
- 源码：[AppNavigation.kt:170](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt#L170)；[AppNavigation.kt:234](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt#L234)。
- 测试源码（本轮未运行）：[WorkspacePermissionFlowTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspacePermissionFlowTest.kt)。

### A03 撤回地图授权确认

- 类型：弹层。入口：地图授权管理 / 撤回。
- 必要状态：暂停在线地图搜索路线；保留已有旅行收藏；确认/取消；撤回中/失败。
- 候选材料：DSET。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：补原生确认及数据保留说明。
- 源码：[MapConsentContent.kt:57](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/settings/MapConsentContent.kt#L57)。
- 测试源码（本轮未运行）：[AppSettingsTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/settings/AppSettingsTest.kt)。

### A04 工作台定位权限

- 类型：应用弹层组。入口：点击地图定位工具。
- 必要状态：用途解释；允许/取消；系统请求中；永久拒绝转设置；打开设置失败；返回后重新读取权限。
- 候选材料：D12, H18L。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：仅画应用自有解释页；系统弹窗单列边界。
- 源码：[PermissionExplanationContent.kt:23](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/permission/PermissionExplanationContent.kt#L23)；[LocationPermissionSettingsContent.kt:23](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/permission/LocationPermissionSettingsContent.kt#L23)；[TripWorkspaceScreen.kt:710](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt#L710)。
- 测试源码（本轮未运行）：[WorkspacePermissionFlowTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspacePermissionFlowTest.kt)。

### A05 搜索定位权限

- 类型：应用弹层组。入口：搜索当前城市需要定位。
- 必要状态：用途包含当前城市/距我/我的位置；允许定位/暂不；永久拒绝打开设置；忙碌/错误；恢复搜索。
- 候选材料：H18L。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：当前为独立 AlertDialog，不能强行套用工作台弹窗布局。
- 源码：[SearchLocationPermission.kt:25](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/SearchLocationPermission.kt#L25)；[SearchLocationPermission.kt:94](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/SearchLocationPermission.kt#L94)。
- 测试源码（本轮未运行）：[AppSessionCitySearchTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/workspace/AppSessionCitySearchTest.kt)。

## 分享附属界面与导出产物（2）

### H01 分享选择日期与放大预览

- 类型：子视图/弹层组。入口：分享页 / 选一天或放大。
- 必要状态：旅行日选择/关闭；当前日期；放大长图滚动；返回分享页；图片读取失败。
- 候选材料：D14, H16。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：两个附属界面不能遗漏，放大预览不是第二份导出内容。
- 源码：[ItineraryShareScreen.kt:131](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/share/ItineraryShareScreen.kt#L131)；[ItineraryShareScreen.kt:193](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/share/ItineraryShareScreen.kt#L193)。
- 测试源码（本轮未运行）：[ItineraryShareScreenTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/share/ItineraryShareScreenTest.kt)。

### H02 每日地图行程长图

- 类型：导出视觉产物。入口：分享页生成。
- 必要状态：全程/单日；旅行摘要；每日地图/地图不可用；地点/交通/备注；长备注；重复地点；空日；过长失败。
- 候选材料：D14, H16。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：采用 R4；不含分享日历。独立产物分区，不能冒充 App 页面。
- 源码：[ShareImageRenderer.kt:35](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/share/ShareImageRenderer.kt#L35)；[ItineraryShareModels.kt:15](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/share/ItineraryShareModels.kt#L15)。
- 测试源码（本轮未运行）：[FullTripExportTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/share/FullTripExportTest.kt)；[ShareImageExportTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/share/ShareImageExportTest.kt)。

## 共用组件（1）

### G01 共用组件、主题与全局反馈

- 类型：组件/状态规范。入口：全部页面。
- 必要状态：默认五主题色值；标题/正文/金额；主次危险按钮；禁用/加载；输入错误；空态；网络/同步失败；可展开备注；Toast/Snackbar；键盘/系统栏/大字体。
- 候选材料：D12, D16。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：完整版本需要自身 UI Kit；当前 D18 无 reusable 组件。系统样式属于运行验证，不应伪造设计控件。
- 源码：[ThemePalette.kt:7](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/core/ui/theme/ThemePalette.kt#L7)；[ConfirmationDialog.kt:29](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/core/ui/component/ConfirmationDialog.kt#L29)；[FeedbackState.kt:20](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/core/ui/component/FeedbackState.kt#L20)；[ExpandableNote.kt:31](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/core/ui/component/ExpandableNote.kt#L31)。
- 测试源码（本轮未运行）：[ConfirmationDialogTest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/androidTest/java/com/yangchengwei/easytrip/core/ui/component/ConfirmationDialogTest.kt)。

## 系统边界（1）

### X01 Android / 外部界面交接

- 类型：系统边界。入口：授权/隐私链接/保存/分享/安装。
- 必要状态：系统定位授权与应用设置；浏览器隐私政策；Android 8–9 相册权限；系统分享选择器及返回；未知来源授权；安装确认及返回。
- 候选材料：无；仅记录系统交接边界。正式 v1.8.0 映射：**待补齐**。
- 缺口/核对点：记录调用和返回状态，不重绘为应用自有页面；本轮未操作系统授权或安装。
- 源码：[SearchLocationPermission.kt:25](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/SearchLocationPermission.kt#L25)；[ItineraryShareScreen.kt:120](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/share/ItineraryShareScreen.kt#L120)；[ApkInstaller.kt:54](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/settings/ApkInstaller.kt#L54)；[UpdateRoute.kt:14](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/settings/UpdateRoute.kt#L14)。

## 状态类型对照（17 个选定模型 / 82 个变体）

校验脚本逐一比对下列模型声明与映射；不穷举整个代码库的类型，也不把每个类型变体直接当一个画板。

| 模型 | 声明来源 | 变体 → 盘点组 |
|---|---|---|
| TripListPageState | [TripListUiModels.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListUiModels.kt) | Loading → N01；Empty → N01；Content → N01；Error → N01 |
| TripDeletionUiState | [TripListUiModels.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListUiModels.kt) | Idle → T03；LoadingImpact → T03；ImpactFailure → T03；Ready → T03 |
| TripWorkspacePageState | [WorkspaceUiModels.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceUiModels.kt) | Loading → N03；NotFound → N03；Error → N03；Ready → N03 |
| WorkspaceMapState | [WorkspaceUiModels.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceUiModels.kt) | Loading → N03；Ready → N03；ConsentRequired → N03；Failed → N03 |
| MapHostState | [WorkspaceUiModels.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceUiModels.kt) | Loading → N03；Ready → N03；Failed → N03 |
| WorkspaceOverlay | [WorkspaceOverlay.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceOverlay.kt) | None → N03；PlaceDetail → W08, P01；SelectAddPlaces → I01；SelectAddTargetDay → I02；AddToItineraryResult → I03；SelectMoveTargetDay → I08；AddTripDay → I04；EditItineraryItem → I06, I10；EditRouteLeg → I07；LayerMenu → W07；MoreMenu → W07；Confirmation → P03, I09；PermissionExplanation → A04；Feedback → EX01 |
| PlaceSearchPhase | [PlaceSearchReducer.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchReducer.kt) | Initial → N04；ConsentRequired → N04；Loading → N04；Results → N04；Empty → N04；NetworkFailure → N04；LocationPermissionRequired → N04；LocationFailure → N04 |
| SearchDisplayMode | [PlaceSearchViewModel.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchViewModel.kt) | Results → N04；MapDetail → P02 |
| DateRangeChangePhase | [DateRangeChangeRequest.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/DateRangeChangeRequest.kt) | Idle → T05；Previewing → T05；AwaitingConfirmation → T05；Applying → T05；AwaitingRoom → T05；SyncFailed → T05 |
| UpdateState | [UpdateController.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/settings/UpdateController.kt) | Idle → N09；Checking → N09；Current → N09；Ahead → N09；Available → N09；Downloading → N09；Ready → N09；Failed → N09 |
| AddToItineraryStep | [AddToItineraryUiState.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/AddToItineraryUiState.kt) | IDLE → N03；SELECT_PLACES → I01；SELECT_TARGET_DAY → I02；COMPLETED → I03 |
| RouteStatus | [RouteStatus.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/core/model/RouteStatus.kt) | WAITING_NETWORK → W02, W03, I07；PENDING → W02, W03, I07；CALCULATING → W02, W03, I07；SUCCESS → W02, W03, I07；FAILED → W02, W03, I07 |
| ThemePalette | [ThemePalette.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/core/ui/theme/ThemePalette.kt) | LAKE → A01, G01；FOREST → A01, G01；SUNSET → A01, G01；VIOLET → A01, G01；ROSE → A01, G01 |
| WorkspaceSheetLevel | [WorkspaceBottomSheet.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceBottomSheet.kt) | COLLAPSED → N03, W06；HALF → N03, W06；EXPANDED → N03, W06 |
| PermissionKind | [WorkspaceOverlay.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceOverlay.kt) | DEVICE_LOCATION → A04；DEVICE_LOCATION_SETTINGS → A04 |
| LocationPermissionPrompt | [LocationPermissionCoordinator.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/permission/LocationPermissionCoordinator.kt) | NONE → A04, A05；EXPLANATION → A04, A05；SETTINGS → A04, A05 |
| ShareGeneration | [ItineraryShareScreen.kt](/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/share/ItineraryShareScreen.kt) | Working → N05；Ready → N05；Failed → N05 |

## 不应机械计入页面的源码

### EX01 WorkspaceOverlay.Feedback

有类型和渲染分支，但当前生产源未找到构造调用；不作为已证实可达页面。

保留可达性待核查，不为凑齐 enum 单独画页面。

### EX02 EditTimingDialog / PlaceDetailDialog / SavedPlacesContent

当前 main source 仅找到定义，无生产调用；现用行程编辑与共享地点详情已有对应组。

不按源码文件名机械生成页面；若后续发现实际入口，再补清单。

### EX03 PlacePoolContent(showSearch=true, showDialogs=true) / DayItineraryContent(showDialogs=true)

保留 standalone/测试宿主分支；当前工作台调用显式关闭内置在线搜索和重复 dialogs，由 WorkspaceOverlay 统一承载。

复用流程语义，不把重复宿主画成新的正式入口。

### EX04 旧 v2.0 / V1Scenario 画板映射

现有测试场景目录包含旧设计映射，不证明未来 v2.0 整体已实现。

每个入口以当前 AppNavigation 和实际宿主核验；不继承固定旧 frame 清单。
