# Task 7 Report

日期：2026-08-27

## 状态

DONE

## 实现

- 将 V1 场景 `01/01v/07/09/13/36/47` 迁移到当前 production selectors 与文案。
- 场景 01 使用唯一 `continue-trip-trip-1` 入口，并验证旅行名称、开始日期、天数、方式及主卡/其他卡菜单。
- 场景 13 从 `trip-menu-trip-1` → `trip-menu-delete-trip-1` 进入确认，精确验证旅行日、收藏地点、标签、行程项、路线段和保留内容。
- 场景 07 验证名称、开始/结束日期、天数、方式及 `create-submit`。
- 场景 09 验证选择的开始日期及按天数推导的结束日期。
- 场景 36 验证新空态标题、说明和创建入口。
- 场景 47 验证名称、日期、天数错误分别出现在字段附近。
- 新增头像仅装饰、无点击/Button semantics、无“个人中心”目的地声明的 catalog 测试。
- 更新 V1 场景矩阵，新增候选验收与冲突记录。

## TDD

- 初次测试代码编译错误，不计有效 RED。
- 修正测试自身后运行 catalog：47 tests，2 failures。失败分别证明场景 01 使用旧入口、场景 36 使用旧空态文案。
- 迁移 executable 后 catalog：47 tests，0 failures。

## 门禁

- JVM：419 tests，0 failures。
- lint + debug APK + androidTest APK：PASS。
- `TripListContentTest`：7 tests，0 failures。
- `CreateTripContentTest`：12 tests，0 failures。
- `V1PencilFlowTest`：2 tests，0 failures。
- `V1ScenarioCatalogTest`：47 tests，0 failures。
- `V1FullUiAcceptanceTest`：47 tests，0 failures。
- 自动化设备：`easy_trip_p60pro(AVD) - 12` / `Android_SDK_built_for_arm64`。
- 物理设备验收：DEFERRED（Mate 60 Pro 未执行）。
- 无 0 tests、超时、signal 9 或 runner crash；未重启 AVD。

## Graphify 与仓库检查

- `graphify update .`：成功，5028 nodes / 10831 edges / 275 communities。
- Graphify 报告两个既有 Kotlin 文件语法解析警告：`NetworkMonitor.kt`、`RoutePlanner.kt`；并提示社区标签需刷新。
- 当前 graphify CLI 0.9.48 不支持 `graphify diff` 与 `graphify status`，两条命令均返回 `unknown command`；以 `git diff --check` 和 `git status --short` 完成对应变更检查。
- `git diff --check`：PASS。
- 未纳入 `diagrams/`、`.pen`、`.kotlin/` 或测试产物。

## Fix round 1

- 场景 09 改为真实交互更新可控 production `CreateTripUiState`，并直接断言 UI 的开始日期 `2026-08-31` 与 inclusive 结束日期 `2026-09-02`；不再以测试侧 `plusDays` 代替 UI 证明。
- 场景 47 复用字段容器祖先 matcher，分别约束名称、日期、天数错误与对应字段空间关联。
- 场景 13 确认后更新可控列表 state，形成 01v 可执行覆盖：目标卡消失、保留旅行“东京秋日”仍在。
- RED：Catalog 47 tests，1 failure（目标卡在确认删除后仍存在）。
- GREEN：Catalog 47 tests，0 failures。
- 更新 matrix、acceptance、conflicts，移除 01v 仅依赖 metadata/父场景的过强表述。

## 关注点

- Mate 60 Pro 物理设备视觉验收仍为 DEFERRED。
- Graphify 的社区标签提示与两个既有解析警告不影响本次自动化门禁，但后续可单独运行支持的标签刷新流程。

## 新授权聚焦修复

- 修复 delete service 已成功、Room Flow 自动 retry(1) 仍耗尽后永久 busy 的缺陷。
- 新增可恢复同步失败状态和“重新同步”动作；该动作仅重订阅 `observeTrips()`，删除调用保持一次。
- 同步中锁定重复动作、取消、Back 与外部 dismiss；再次失败返回可重试状态，成功目标缺失 emission 后进入 Idle。
- collector generation 与原有 delete generation/trip ID 一起阻止旧 collector completion/error/emission 污染新订阅或新目标。
- RED：`TripListViewModelTest` 14 tests / 1 failure（连续两次 observe failure 后仍 busy）；UI compile RED（缺少 `confirmLabel` 覆盖）。
- GREEN：`TripListViewModelTest` 16/16、`ConfirmationDialogTest` 4/4、`TripFlowTest` 6/6 PASS。
