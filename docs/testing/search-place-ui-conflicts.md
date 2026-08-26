# Search / Place UI 冲突裁决

## 标签 identity

- 标签的业务 identity 以规范化后的名称为准，不以临时 UI 索引或显示顺序为准。
- 编辑态保留已选标签名称集合；Search 与 Place Pool 都订阅现有 `observeTags` 并把预设名称传入无状态面板。未选预设可添加，已选预设可移除；达到 8 个时禁用新增但仍允许移除。新增标签先 trim，再执行空值、重复、长度 24、最多 8 个校验。
- selector 迁移不替代业务断言：V1 搜索场景仍验证结果行可见且行内文本包含“西湖”。

## 无坐标候选

- 无坐标候选仍进入 repository 收藏流程，由持久化边界返回明确错误。
- 收藏失败不加入 `recentlyCollectedPoiIds`，不发布伪成功返回 payload。
- 地图详情没有合法坐标时不创建地图模型，但非地图业务信息保持可达。

## 双加入入口

- 搜索页只负责收藏，不提供“加入行程”入口。
- 地点池提供主入口与行级快捷入口；两者汇合到同一 Add-to-Itinerary 状态机。无旅行日时行级 `＋` 打开既有 `AddTripDay` 引导；新增日完成后不自动续接单地点意图，也不伪装已进入目标日。
- V1 场景保留“搜索页不显示加入行程入口”的功能断言。

## 删除影响计数

- 删除确认展示精确的行程安排数量与路线段数量，不使用单一 usage count 代替。
- 零影响可直接删除；非零影响必须确认。
- 删除进行中禁止 dismiss、Back 和重复确认；失败后保留确认上下文并允许重试。

## 返回与一次性发布

- 顶部和系统 Back 同优先级：删除确认 → 编辑 → 详情 → 结果 → workspace。
- 详情到结果不发布；只有 Results 请求退出时发布本次会话新增收藏，并只触发一次。
- `SavedStateHandle` payload 由 workspace entry 消费并清空；Activity 重建保留已确认的 entry-scoped UI state，但不重发旧 payload。

## Runner 状态

- Android 12 AVD 存在 instrumentation 进程被 signal 9 和长时间无进度问题；失败 XML 无 assertion body 时不得记为 PASS。
- `PlaceSearchContentTest` 和 Catalog 在重启后整类通过，证明对应首次失败为 runner 环境问题。
- `PlacePoolFlowTest` 按诊断严格串行二分后分组累计 **18/18 PASS**，不声称单次整类通过。Group A 初始卡顿来自长列表测试 fixture：`280.dp + fontScale 2f` 将 weighted list 挤到近零高度；改为真实窄宽 `280.dp`、生产 overlay 高度 `550.dp`、`density 1f / fontScale 2f` 后，该单测 1/1 PASS，仍验证长列表滚动后末日与提交按钮可达。
- Group B 首轮 7/8，唯一失败是旧文案断言；迁移为精确 `1 次行程安排和 0 段路线` 后 B2 4/4，B1 4/4。
- 证据归档位于 `.superpowers/sdd/2026-08-26-easy-trip-search-place-loop-ui-convergence/evidence/place-pool-*`；signal 9 已确认来自并行/后继安装同包时的 package force-stop，没有 low-memory killer 或 OOM 证据。
- V1 Full UI 的 selector 产品性失败已通过稳定 selector 迁移修复，最终 47/47；业务断言未删除。
