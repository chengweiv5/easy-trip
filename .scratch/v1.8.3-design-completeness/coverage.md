# v1.8.3 设计覆盖矩阵

状态：已在同一正式 Pencil 文件建立表达与契约，**原生保存、精确路径关闭重开和内容回读已完成**（2026-10-09）。运行验收仍有未通过项，不声明逐状态验收通过。

正式源：`/Users/bytedance/.codex/worktrees/6934/easy-trip/design/easy-trip-v1.8.3.pen`。

9 条路由、49 个功能状态组；131 个有效根画板不是131个独立业务页面。瞬时忙碌/失败/返回行为可复用主画板，在00A–00C索引和画板context描述。外部系统UI只描述交接，不伪造App页面。

代码来源均为当前工作树且与发布代码bd370c4的app/一致。下列状态清单从旧盘点迁移后逐组映射，不继承旧完成结论；未逐状态运行验收。

## N01 我的旅行

- 画板：`K9h3r`、`D03lK`、`zIbEu`、`rd2Iu`、`LoQrS`、`yq8pE`、`oYNJg`
- 状态：加载；无旅行；待出行/已出行筛选及筛选为空；主旅行与其他旅行；加载失败重试；准备度及已记花费。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListContent.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListUiModels.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## N02 创建旅行

- 画板：`PnhPb`、`SKABj`、`dzhkC`、`QDPQR`、`YYo6U`、`yIGiQ`、`mpGO8`、`AC91v`、`PtwjB`、`WWA8d`
- 状态：空表单；名称校验；日期范围未完整/超出30天；灵活/自驾；提交中；提交失败保留输入；成功进入工作台。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripContent.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripUiState.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## N03 旅行工作台外壳

- 画板：`A9EKX`、`LFmzR`、`BrYVA`、`GoxB6`、`U8R5i`、`SwjuL`、`i3yxsc`、`RC3d3`
- 状态：加载；旅行不存在；读取失败重试；就绪；地图未授权/加载/失败/就绪；抽屉收起/半屏/展开；返回与遮罩层顺序；地图平移缩放、定位与恢复正北；控件选中与避让；服务商署名。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceUiModels.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/MapControls.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## N04 地点搜索

- 画板：`ofdn5`、`ny0pT`、`s1OvvX`、`S0psO`、`GJo79`、`RK5rH`、`sT8u9`、`EBQcJ`
- 状态：初始输入及键盘；地图服务未授权；加载；结果；无结果；网络失败；定位权限不足；定位失败；有/无距我的距离；收藏忙碌/失败。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchContent.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchReducer.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchRoute.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## N05 分享行程长图

- 画板：`OSDtB`、`g2NJ9Q`、`vdAvS`、`ZaTMA`、`Pu1Sx`、`bSyPn`
- 状态：加载行程；空行程；加载失败；全程/选一天；包含/不含备注；生成地图和长图；地图缺失降级；生成失败/过长；保存/分享中及失败。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/share/ItineraryShareScreen.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/share/ItineraryShareScreen.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## N06 旅行设置

- 画板：`U06l7P`、`zlRxO`、`hJBDC`、`K2LnC`
- 状态：无权威旅行数据/读取失败；名称；出行日期；灵活/自驾；旅行日列表；添加/排序/删除旅行日；写入中互斥；删除旅行入口。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsContent.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsViewModel.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## N07 App 设置

- 画板：`DUpeg`
- 状态：当前主题；地图授权状态；已安装版本离线显示；存在可更新版本提示；进入主题/授权/更新。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/settings/AppSettingsContent.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## N08 地图授权管理

- 画板：`t7QLVZ`、`ogpD5`、`muIJJ`、`YjnJq`
- 状态：未授权；已授权；阅读政策/已读勾选；展示报告失败重试；允许处理中/失败；撤回入口；不自动申请定位。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/settings/MapConsentContent.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## N09 版本更新

- 画板：`Bl8pI`、`au8lI`、`Tftvp`、`nSd8w`、`TxcuJ`、`O8f2w`、`rg5j1`、`f0TO9`、`S2mZg`
- 状态：Idle；Checking；Current；Ahead；Available；Downloading；Ready；Failed；取消下载；稍后安装；系统返回后的提示；重新下载/检查。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/settings/AppSettingsContent.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/settings/UpdateController.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/settings/UpdateRoute.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## T01 日期范围选择

- 画板：`YYo6U`、`IKTv5`、`L7z4R`
- 状态：初始月；开始/结束日期；跨月/跨年；30天限制；固定天数改起始日；取消；确认受校验限制。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripDateRangePickerSheet.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsContent.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## T02 旅行卡片更多菜单

- 画板：`oYNJg`
- 状态：标记已出行/待出行；设置；删除；菜单关闭。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListCards.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## T03 删除旅行确认

- 画板：`oW9mK`、`Vl15d`、`APvJ3`
- 状态：Idle不展示；LoadingImpact；ImpactFailure；影响清单确认；删除中；失败；同步失败重试；取消。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripDeletionDialog.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListUiModels.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## T04 重命名旅行

- 画板：`os9Ln`
- 状态：当前名称；编辑；确认；取消恢复；其他写入锁定。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsContent.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## T05 旅行日期变更及影响确认

- 画板：`IKTv5`、`yT6kr`、`Z9abQQ`
- 状态：Idle；Previewing；AwaitingConfirmation；Applying；AwaitingRoom；SyncFailed；移除旅行日影响；取消/重试。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/DateRangeChangeRequest.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsContent.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## T06 旅行日管理

- 画板：`U06l7P`、`K2LnC`、`zvO9Z`、`J7PZ7u`
- 状态：添加一天；拖拽排序；删除候选；忙碌禁用；影响计算/失败；保存失败；长列表滚动。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsContent.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsViewModel.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## W01 地点池

- 画板：`A9EKX`、`lsr1I`、`JCOPS`
- 状态：加载；空池；城市分组及切换；本地关键词；标签；全部/未排入/已排入；无筛选结果；收藏卡片；备注展开；批量安排入口。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolSheet.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolViewModel.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceCityGroups.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## W02 单日行程清单

- 画板：`LFmzR`、`Bcf6A`、`ge7BC`、`WWA8d`
- 状态：默认设备当天（范围外第一天）；手选与返回恢复；加载/空/有行程；行程与相邻交通；拖拽排序；备注展开；当日费用及未填数；增删当天/日历按钮。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/DayItinerarySheet.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceNavigation.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryDayActions.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## W03 全程行程清单

- 画板：`FTIOF`、`kuta1`
- 状态：全空；分日清单；连续重复地点合并；无行程日；相邻/跨日展示交通；各日已记费用；备注展开。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/WholeTripItineraryContent.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryUiModels.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## W04 单日时间日历

- 画板：`W4SNt6`、`T7sn5p`、`LhMcy`
- 状态：空日；未设时间待安排；时间轴地点与交通；重叠；短卡；拖入/移动/调时；保存/撤销/失败；跨午夜信息。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/calendar/CalendarContent.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/calendar/CalendarTraffic.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## W05 全程时间日历

- 画板：`Klkfn`
- 状态：多日列；日期分页首尾；30天浏览；无日期；待安排计数；单日聚焦；跨日/重叠及交通。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/calendar/CalendarContent.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/calendar/CalendarContent.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## W06 搜索结果返回地图

- 画板：`aKaPs`、`PchBt`、`QrBb8`
- 状态：搜索标记；可定位/总结果数量；收起/半屏/展开；保留地点池/行程标签；关键词/列表返回；清除；同城我的位置；无位置/坐标不可用。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceSearchSummary.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceSearchResults.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## W07 地图图层与工作台更多

- 画板：`ijpZD`、`shoPV`
- 状态：标准/卫星/卫星路网；菜单空间不足回退；旅行设置；分享行程长图；返回我的旅行；关闭菜单。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/MapControls.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceMoreMenu.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContent.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## W08 地图 POI 与收藏标记详情

- 画板：`p4G1tS`、`XsGon`、`cmlqS`、`SkMyH`
- 状态：名称/地址缺失；可收藏/不可收藏；收藏/取消收藏中及失败；同地点多次行程；关闭。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## P01 收藏地点详情与编辑

- 画板：`p4G1tS`、`XsGon`、`n2oJu`
- 状态：只读详情；未安排/按天重复安排/安排信息未知；收藏状态；备注和标签编辑；预设/新标签/校验；保存中/失败；取消；加入行程；删除。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceDetailPanel.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/place/ui/WorkspacePlaceDetailSheet.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## P02 搜索结果地图详情与编辑

- 画板：`cmlqS`、`SkMyH`、`n2oJu`
- 状态：地图加载/失败；未收藏/已收藏；收藏切换；地址及安排信息；已收藏地点备注/标签编辑；返回结果列表。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchContent.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchViewModel.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## P03 删除收藏/取消收藏影响确认

- 画板：`pfaVY`、`yXOEq`
- 状态：受影响行程/路线清单；计算中/失败；确认；处理中；失败保留；取消。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchRoute.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## I01 从地点池选地点

- 画板：`Pqdkf`、`xwaRW`、`zDqcV`、`JCOPS`
- 状态：无候选；未选/多选；关键词/城市筛选；选择计数；固定目标日失效；继续/取消。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/SelectPlacesContent.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## I02 选择安排日期

- 画板：`xQfD0`、`cRdBn`、`p7U8B`、`D3XZi`
- 状态：多日选择；已有安排次数；无旅行日；添加一天入口；提交中；日期失效；取消。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/SelectTargetDayContent.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/AddToItineraryUiState.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## I03 安排异常结果与恢复

- 画板：`mGhKO`、`KPBBb`、`D3XZi`
- 状态：部分成功；失败地点重试；目标日失效重选；撤销中/失败；已撤销查看地点池；成功自动收起而不弹成功对话框。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/AddToItineraryResultContent.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## I04 添加旅行日

- 画板：`zvO9Z`
- 状态：追加末尾说明；确认；添加中；失败重试；取消。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/AddTripDayContent.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## I05 删除旅行日

- 画板：`J7PZ7u`
- 状态：影响数量；保留收藏说明；忙碌；错误；取消；确认删除；后续日期/范围变化。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/WorkspaceDayDeletionDialog.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsContent.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## I06 编辑单次行程

- 画板：`K336N`、`c0vRa`
- 状态：到达时间滚轮及分时段；停留时长；未设时间；备注；花费空/0/小数/无效/溢出；再次安排；保存中；取消保留原值。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/EditItineraryItemContent.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryTimingPickers.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditDraft.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## I07 编辑交通路段

- 画板：`T7aESo`、`c0vRa`、`OOEsk`
- 状态：起终点/距离；步行/打车/驾车/公交；恢复推荐；覆盖/恢复用时；备注；金额及无效输入；保存中/失败；取消。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/EditRouteLegContent.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditDraft.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## I08 行程项菜单与跨日移动

- 画板：`btWFB`、`dGRLD`
- 状态：编辑；再次安排；移动到其他日期；删除；目标日期列表排除当天；移动中/失败；取消。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryItemMenu.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## I09 移出单次行程确认

- 画板：`l2xCsM`
- 状态：仅移除本次安排；收藏保留；相邻路线重算；确认/取消；删除中/失败。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditDraft.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## I10 编辑保存失败恢复

- 画板：`OOEsk`
- 状态：原因；继续编辑；重试保存；关闭错误后回草稿。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItinerarySaveFailureContent.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## C01 日历详情与交叠列表

- 画板：`qsgyR`、`Z1PFU`
- 状态：交叠成员列表；单次日程日期/次序/到达/结束/停留；长备注；时间冲突；交通来不及；入站/出站交通；转编辑；条目已删除。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/calendar/CalendarContent.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/calendar/CalendarDetail.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## C02 日历拖动与调时反馈

- 画板：`LhMcy`、`IIQHg`、`dpJ4s`、`EF8ik`、`aBMG6`
- 状态：未触发/长按门槛；拖动草稿；调时提示避手指；大字体/顶部边界；保存中；成功撤销；失败重试；撤销失败。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/calendar/CalendarResizeHint.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/calendar/CalendarTimingController.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/calendar/CalendarContent.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## F01 现有费用录入与总额展示

- 画板：`K9h3r`、`LFmzR`、`FTIOF`、`K336N`、`T7aESo`、`c0vRa`
- 状态：未记金额与0元区分；两位小数；已记总额；未填项数量；非法输入；合计溢出拒绝；编辑保存后刷新。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/expense/Expense.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/expense/ExpenseField.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListUiModels.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryUiModels.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## F02 已记费用丢失二次确认

- 画板：`AnBuD`
- 状态：受影响笔数/金额；交通会重新生成且费用不转移；删除花费并继续；取消调整无错误提示。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/expense/ExpenseRemovalDialog.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## A01 主题选择与预览

- 画板：`WBSL0`、`PDyHG`、`iNBUq`
- 状态：湖畔晴空；松林晨光；落日陶土；山岚暮紫；玫瑰沙丘；当前项禁用应用；本页预览；应用中/失败；返回放弃；成功 Toast。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/core/ui/theme/ThemeHost.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/core/ui/theme/ThemePickerScreen.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/core/ui/theme/ThemePalette.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## A02 首次地图服务授权

- 画板：`tymNg`、`ogpD5`、`muIJJ`
- 状态：政策入口；已读勾选；展示报告失败重试；允许/暂不允许；忙碌/失败；拒绝仍可用离线内容。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## A03 撤回地图授权确认

- 画板：`YjnJq`、`t7QLVZ`
- 状态：暂停在线地图搜索路线；保留已有旅行收藏；确认/取消；撤回中/失败。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/settings/MapConsentContent.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## A04 工作台定位权限

- 画板：`JFhZ7`、`HYCsZ`
- 状态：用途解释；允许/取消；系统请求中；永久拒绝转设置；打开设置失败；返回后重新读取权限。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/permission/PermissionExplanationContent.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/permission/LocationPermissionSettingsContent.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## A05 搜索定位权限

- 画板：`y3Gew`、`KyU0S`、`RK5rH`、`sT8u9`
- 状态：用途包含当前城市/距我/我的位置；允许定位/暂不；永久拒绝打开设置；忙碌/错误；恢复搜索。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/SearchLocationPermission.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/SearchLocationPermission.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## H01 分享选择日期与放大预览

- 画板：`L1ww5`、`ryMk5`、`Pu1Sx`
- 状态：旅行日选择/关闭；当前日期；放大长图滚动；返回分享页；图片读取失败。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/share/ItineraryShareScreen.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/share/ItineraryShareScreen.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## H02 每日地图行程长图

- 画板：`fSrZV`、`OSDtB`、`g2NJ9Q`、`bSyPn`、`GUKFC`
- 状态：全程/单日；旅行摘要；每日地图/地图不可用；地点/交通/备注；长备注；重复地点；空日；过长失败。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/share/ShareImageRenderer.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/share/ItineraryShareModels.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## G01 共用组件、主题与全局反馈

- 画板：`f7rS8`、`iNBUq`、`vdAvS`
- 状态：默认五主题色值；标题/正文/金额；主次危险按钮；禁用/加载；输入错误；空态；网络/同步失败；可展开备注；Toast/Snackbar；键盘/系统栏/大字体。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/core/ui/theme/ThemePalette.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/core/ui/component/ConfirmationDialog.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/core/ui/component/FeedbackState.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/core/ui/component/ExpandableNote.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。

## X01 Android / 外部界面交接

- 画板：`JFhZ7`、`HYCsZ`、`y3Gew`、`KyU0S`、`rg5j1`、`OSDtB`
- 状态：系统定位授权与应用设置；浏览器隐私政策；Android 8–9 相册权限；系统分享选择器及返回；未知来源授权；安装确认及返回。
- 源码：`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/SearchLocationPermission.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/share/ItineraryShareScreen.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/settings/ApkInstaller.kt`、`/Users/bytedance/.codex/worktrees/6934/easy-trip/app/src/main/java/com/yangchengwei/easytrip/settings/UpdateRoute.kt`
- 门禁：保存与重开已验证；逐状态运行验收未声明通过，已知UI测试失败见 README。
