# Easy Trip 视觉收敛最终候选门禁与验收记录

- 日期：2026-08-29
- 工作树：`/Users/bytedance/Code/easy-trip/.claude/worktrees/search-place-ui-convergence`
- 候选结论：**PASS（自动化候选）**
- 执行设备：`emulator-5554`，启动完成（`sys.boot_completed=1`），开始时在线且无 instrumentation 进程。

## 自动化门禁

| 门禁 | 精确结果 | 结论 |
|---|---:|---|
| `testDebugUnitTest` XML | 521 tests / 0 failures / 0 errors / 0 skipped | PASS |
| 22 个指定 connected suite XML | 351 tests / 0 failures / 0 errors / 0 skipped | PASS |
| `V1ScenarioCatalogTest` | 47 / 0 / 0 / 0 | PASS |
| `V1FullUiAcceptanceTest` | 47 / 0 / 0 / 0 | PASS |
| `lintDebug` | exit 0 | PASS |
| `assembleDebug` | exit 0 | PASS |
| `assembleDebugAndroidTest` | exit 0 | PASS |
| `compileDebugAndroidTestKotlin` | exit 0 | PASS |
| `git diff --check`（graphify 前后） | exit 0 | PASS |

指定 connected suites：`ConfirmationDialogTest` 6、`TripSettingsContentTest` 16、`TripSettingsNavigationTest` 3、`WorkspacePermissionFlowTest` 7、`CreateTripContentTest` 17、`TripListContentTest` 8、`PlaceSearchContentTest` 26、`PlaceDetailPanelTest` 12、`WorkspacePlacePoolLayoutTest` 3、`PlacePoolFlowTest` 18、`TripWorkspaceContentTest` 27、`WorkspaceChromeTest` 3、`WorkspaceFlowTest` 27、`ItineraryTimelineContentTest` 30、`ItineraryEditingTest` 7、`WholeTripItineraryContentTest` 6、`ItineraryScopeRailTest` 9、`AmapComposeMapTest` 14、`SearchMapFocusTest` 5、`VisualBatch0EvidenceTest` 13、`V1ScenarioCatalogTest` 47、`V1FullUiAcceptanceTest` 47。

### Infra retry

- `infra retry=1`：外层串行命令在 `WholeTripItineraryContentTest` 执行期间达到 10 分钟时限并被终止（exit 143）；设备随后无残留 instrumentation。该 suite 立即按规则重试，XML 为 `6 / 0 / 0 / 0`。无产品失败、无 skip。

## 视觉对照与截图诊断

- Pencil 基线：`design/easy-trip-v1.0.pen`，390 × 844 canvas。
- 本轮对照 frame：`U06l7P`（设置）、`IKTv5`（日期确认）、`ofdn5`（搜索）、`p4G1tS`（地点详情）、`A9EKX`（地点池）、`eHTX3`（单日行程）、`zIbEu`（旅行空态）、`lsr1I`（地点池空态）、`Bcf6A`（单日空态）、`dzhkC`（创建旅行），以及 `batch5` 路线状态、`batch6-search` / `batch6-failure` 空态组合。
- `VisualBatch0EvidenceTest` 在设备应用私有目录写入诊断截图与 manifest：`/data/user/0/com.yangchengwei.easytrip/files/evidence/batch-0/<name>-<frameId>.png` 和 `.manifest.json`。本次测试完成后的测试安装清理使该临时目录不可再导出；不把不存在的本地副本误记为证据。
- 仍可访问的历史诊断截图：`.superpowers/sdd/2026-08-23-easy-trip-v1-full-ui-implementation/task-4-evidence/{launch,loading,default,failure}.png`。
- 截图仅作诊断，未做像素级阻断。按项目门禁规则，只有功能/状态错误、数据不一致、崩溃、流程不可达、严重裁切或关键交互损坏才会阻断；本批未发现此类阻断项。

## 范围与未执行项

- 未执行无过滤的 474-test connected suite：其中包含真实 SDK smoke 的预期 skip，且不符合本次正式归档限制；本记录的 22 个定向 suite 已覆盖本批关键族，Catalog 与 Full UI 各覆盖 47 个场景。
- 物理真机：**NOT-RUN**。模拟器自动化不得替代物理设备验收。
- 未清除任何物理真机数据；本轮仅在模拟器上运行 Gradle instrumentation，模拟器测试数据按测试安装生命周期处理。

## 收尾

- 已执行 `graphify update .`；工具报告代码图拓扑无变化，但提示 `NetworkMonitor.kt` 与 `RoutePlanner.kt` 存在既有/部分 AST 语法提取告警。
- 最终 `git diff --check` 通过。未修改生产代码，未 commit 或 push。

## 二次视觉收敛复验（2026-08-29）

- 基线提交：`00043b7`；本节记录其上的未提交视觉候选，尚未 commit/push。
- 自动化：`testDebugUnitTest`、`lintDebug`、`assembleDebug`、`assembleDebugAndroidTest` PASS；冷启动 AVD 后 `V1ScenarioCatalogTest` 47/47、`V1FullUiAcceptanceTest` 47/47 PASS。
- 定向门禁：最终视觉细节 70/70、工作台空态 35/35、行程空态 50/50；`VisualBatch0EvidenceTest` 14/14、`ConfirmationDialogTest` + `PlaceSearchContentTest` 37/37 PASS。
- 证据改进：截图支持 387–390dp 目标手机宽度，manifest 记录 390dp 基线和实际宽度；工作台证据明确为 production Compose workspace host + deterministic fake map surface，不等同于真实 AMap Host 证明。
- 模拟器截图：`/tmp/easytrip-emulator-visual-v4/`；最终空地点池诊断图：`/tmp/easytrip-emulator-visual-final/empty-place-pool-lsr1I.png`。
- 物理设备：Huawei ALN-AL00，Android 12，1260×2720。使用 `adb install -r` 覆盖安装，未主动清数据。
- 真机代表路径：旅行空态、创建表单及校验错误、创建临时旅行 `FinalVisual`、地图隐私拒绝恢复、地点池空态、行程空态、设置页均已操作验证；无 crash/ANR、流程不可达或状态错误。
- 真机中发现并修复两项严重裁切：地点池空态与单日行程空态的底部按钮被 Sheet/导航栏压缩。修复方式为只对 Sheet 内空态使用 16dp 垂直 padding；相关模拟器门禁与最终真机截图通过。
- 最终真机证据：`/tmp/easytrip-physical-acceptance-v3/17-final-workspace-empty.png`、`21-final-itinerary-fixed.png`。
- 用户未授权代为接受高德隐私条款，本轮选择“不同意”验证本地恢复路径；真实地图瓦片和已授权在线搜索不属于本次最终真机结论。
- 本轮创建的临时旅行 `FinalVisual` 暂时保留，未擅自删除用户设备数据。
- `graphify update .` 完成；仍有既有的 `NetworkMonitor.kt`、`RoutePlanner.kt` AST 部分提取警告。

## v2 工作台抽屉最终验收（2026-08-30）

- 事实来源：`design/easy-trip-v2.0.pen` 的 `kCc5z` / `sWTB3` / `f2ieZ6`，以及 `design/EASY_TRIP_IMPLEMENTATION_GUIDE.md` 的 108/432/720dp 三态规范。
- 自动化：Workspace JVM 定向测试通过；`WorkspaceChromeTest` + `TripWorkspaceContentTest` 41/41；`VisualBatch0EvidenceTest` 17/17；`V1ScenarioCatalogTest` 47/47；`V1FullUiAcceptanceTest` 47/47；`lintDebug`、`assembleDebug`、`assembleDebugAndroidTest`、`git diff --check` 通过。
- 独立复审后修复：小窗收起态为真实摘要保留空间，并新增 92dp 极限窗口结构测试；紧凑锚点使用自适应拖动阈值；地图辅助 overlay 避开顶部栏；展开态和空间不足时关闭不可见图层菜单；展开态不再裁切地图失败/授权 fallback；地点池摘要使用收藏总数而非筛选结果数；图层面板改为位于圆形控件左侧，打开期间隐藏地图图例，避免顶部栏、Sheet 和图例重叠。
- 模拟器最终证据：`/tmp/easytrip-v2-sheet-evidence-final/`；manifest 明确记录 `production Compose workspace host` 和 `deterministic fake map surface; not a real map host`。
- 真机：Huawei ALN-AL00，使用 `adb install -r` 覆盖安装，不清数据；创建临时旅行 `V2Sheet`，选择“不同意”高德隐私授权，实际完成半屏→展开→半屏→收起→半屏，以及地点池/行程两个收起摘要；图层菜单打开后进入展开态再返回半屏不会残留，标准半屏面板完整显示且图例隐藏；无 crash/ANR。
- 真机诊断截图：`/tmp/easytrip-v2-physical-half2.png`、`/tmp/easytrip-v2-fixed-expanded.png`、`/tmp/easytrip-v2-physical-collapsed2.png`、`/tmp/easytrip-v2-itinerary-collapsed.png`、`/tmp/easytrip-v2-layer-return.png`。
- 限制：未接受高德隐私条款，因此没有将真实高德底图内容纳入本轮结论；本轮只验收 App 自有顶部栏、Tabs、摘要、Sheet、overlay 降级和本地恢复路径。
- 本轮临时旅行 `V2Sheet` 保留，未擅自删除设备数据；未 commit、未 push。
