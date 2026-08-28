# 旅行设置与日期正确性候选验收

日期：2026-08-28

## 状态

**PASS_WITH_CONCERNS（自动化范围）**：Workspace missing-day race 已有 TDD/JVM 覆盖；最终 fresh 自动门禁全部通过。用户取消最后生产变更后的真机复验，因此 Mate 60 Pro 关键路径与 settings→workspace 5/5 仅为 stale 历史证据，不作为当前代码验证；历史“无法加载旅行”、数据恢复事故与非空级联无真机直接证据继续保留。

## 环境

- HEAD：`3f6be10b6d5e396c93d3bd1065ecde26c0b7311a`
- 工作区：dirty，包含 Tasks 1–5 的未提交实现、测试、设计/计划和 Graphify 产物；本 Task 新增本验收记录并补充真机 checklist。
- 自动化设备：`emulator-5554`，AVD `easy_trip_p60pro(AVD) - 12`，ADB 型号 `Android_SDK_built_for_arm64`，Android 12 / API 31。
- 物理设备：Huawei Mate 60 Pro，产品/型号 `ALN-AL00`（ADB `model:ALN_AL00`），Android 12，序列号 `FMR0224725012307`。
- 物理真机验收执行时仅以该 `ALN-AL00` 为目标；此前 connected 自动化仍在 `emulator-5554` 串行执行。
- 屏幕：1260×2720，density 520。
- connected 自动化执行前已检查进程，无其他 instrumentation 任务；所有 suite 严格串行。

## RED 证据

| Task | RED 测试/缺口 | 预期失败 |
|---|---|---|
| 1 | `roomEmissionDoesNotOverwriteDirtyEndDateDraft` | Room emission 不应覆盖 dirty 结束日期草稿 |
| 1 | `applyReturnWaitsForMatchingRoomEmissionAndWritesOnce` | apply 返回后仍应等待匹配 Room emission，且只写一次 |
| 1 | `matchingEmissionBeforeApplyReturnsDoesNotCompleteRequest` | apply 返回前的匹配 emission 不应提前完成请求 |
| 1 | `editingWhileApplyingDoesNotReplaceRequestSnapshot` | Applying 期间编辑不应替换冻结请求快照 |
| 3 | `shrinkRejectsWhenDayIdsChangedAfterPreviewWithoutPartialWrite`、`shrinkRejectsWhenItemCountChangedAfterPreviewWithoutPartialWrite`、`shrinkRejectsWhenLegCountChangedAfterPreviewWithoutPartialWrite`、`shrinkRejectsWhenStartDateChangedAfterPreviewWithoutPartialWrite` | day ID、item/leg 计数或开始日期变化时应抛 `DateRangeSnapshotChangedException` 且无部分写入 |
| 4 | `roomTargetBeforeServiceReturnCompletesWhenServiceReturns`、`exhaustedRoomConfirmationShowsRetryWithoutApplyingAgain`、`repeatedRetryWhileSyncingStartsOneCollector`、`staleCollectorCannotCompleteNewGeneration` | service/Room 双条件、只同步重试、单 collector 与 stale generation 隔离 |
| 5 | `startDateIsReadOnlyAndOnlyEndDateDispatchesDraft`、`applyingLocksDateInputBackDeleteAndDialogDismiss`、`shrinkConfirmationExposesStructuredImpactCounts`、`syncFailureOffersExplicitRetry`、`undatedTripHasNoDateMutationAction` | 设置 UI 的只读、busy 锁定、结构化影响、同步重试和无日期约束 |
| 5 | `deletedTripWhileSettingsOpenReturnsToTripList` | 设置页期间 trip 删除后应回旅行列表并清理无效返回栈 |

Task 1 最终有效 RED 为 14 tests / 4 failures。Task 3 最终有效 RED 见 `task-3-report.md:23-34`；Task 5 设置 UI RED 见 `task-5-report.md:11-25`，删除导航 RED 见 `task-5-report.md:27-35`。

## 历史候选门禁（本轮生产/UI 修改后已过期）

以下 checked-in 表格是长期验收摘要。`/tmp/easytrip-task6-*.log` 仅为本会话期原始证据，可能随会话或系统临时目录清理而消失，不作为永久可复核材料；构建目录 XML 同样属于本地生成物。

| 命令 | 测试数 | 失败 | 错误 | 跳过 | 结果 | 本会话期原始证据 |
|---|---:|---:|---:|---:|---|---|
| `./gradlew :app:testDebugUnitTest` | 447 | 0 | 0 | 0 | PASS | `/tmp/easytrip-task6-jvm.log`；`app/build/test-results/testDebugUnitTest/TEST-*.xml`（52 files） |
| `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.TripSettingsContentTest` | 10 | 0 | 0 | 0 | PASS | `/tmp/easytrip-task6-content.log` |
| `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.TripSettingsNavigationTest` | 2 | 0 | 0 | 0 | PASS | `/tmp/easytrip-task6-navigation.log` |
| `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.data.TripDateRangeRoomTest` | 10 | 0 | 0 | 0 | PASS | `/tmp/easytrip-task6-room.log` |
| `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.CascadeDeleteTest` | 2 | 0 | 0 | 0 | PASS | `/tmp/easytrip-task6-cascade.log` |
| `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1ScenarioCatalogTest` | 47 | 0 | 0 | 0 | PASS | `/tmp/easytrip-task6-catalog.log` |
| `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1FullUiAcceptanceTest` | 47 | 0 | 0 | 0 | PASS | `/tmp/easytrip-task6-full-ui.log`；最新 XML 为 `app/build/outputs/androidTest-results/connected/debug/TEST-easy_trip_p60pro(AVD) - 12-_app-.xml` |
| `./gradlew :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest` | N/A | 0 | N/A | N/A | PASS | `/tmp/easytrip-task6-build.log`；`BUILD SUCCESSFUL`，lint 无 error |

上述 447/447 与 118/118 是 final fix wave 之前的历史结果；本轮改动后需要最终 fresh 全门禁重跑。历史执行没有 runner crash、signal 9、设备离线或超时，未使用基础设施重试。

## Final fix wave 新鲜验证

- `TripSettingsViewModelTest`：32/32 PASS。
- trip UI/domain JVM：98/98 PASS。
- `TripSettingsContentTest`：10/10 PASS（物理设备 `FMR0224725012307`）。
- `TripSettingsNavigationTest`：2/2 PASS（物理设备 `FMR0224725012307`）。
- `:app:lintDebug`：PASS。
- `git diff --check`、`graphify update .`：PASS。
- 未重跑全量 JVM、其余 connected suites、Catalog 47/47 或 Full UI 47/47，因此此前全门禁明确过期。

## Mate 60 Pro 真机验收

| # | 路径 | 结果 | 说明 |
|---|---|---|---|
| 1 | 工作台“更多”进入当前旅行设置 | PASS | 在既有“登封”和专用 `Task6Acceptance` 旅行上均进入设置；设置页显示日期与旅行日列表 |
| 2 | 返回工作台，保持原 trip 和原 section | PASS | `Task6Acceptance` 从行程 section 进入设置后返回，仍为同一 trip、行程 section 与第一天 |
| 3 | 延长结束日期，新增连续旅行日 | PASS | `2026-08-29..31` 延长到 `2026-09-02`，出现 8/29、8/30、8/31、9/1、9/2 五个连续旅行日 |
| 4 | 缩短结束日期，核对旅行日、行程项、RouteLeg、SavedPlace 影响 | PASS | 专用空旅行缩到 8/31，确认框准确显示删除 2 日、0 item、0 RouteLeg、保留 0 SavedPlace；非空内容级联仍以已通过 Room 自动化为权威 |
| 5 | 取消确认，数据不变 | PASS | 取消后仍保留五个旅行日，草稿保持 8/31，未发生删除 |
| 6 | 再次提交确认，尾部日期删除、内容级联和重编号 | PASS | 再次应用并确认后仅保留 8/29..8/31；专用旅行无内容，内容级联由 Room 自动化覆盖 |
| 7 | 删除非最后一个旅行日并验证重编号 | PASS | 删除中间 8/30 后变为连续 8/29..8/30，结束日期同步为 8/30 |
| 8 | 最后一个旅行日不可删除 | PASS | 缩到单日后点击唯一“删除”未出现确认、旅行日保持 8/29 |
| 9 | 快速重复确认只执行一次 | PASS | 对缩短确认坐标连续点击两次，最终一次性得到 8/29..8/31，无重复删除/崩溃 |
| 10 | 返回工作台，地点池和行程仍可用 | CONCERN | 返回后曾出现一次“无法加载旅行”；冷启动恢复，专用旅行工作台可用且行程 section/第一天存在，但地点池为空，无法验证已有地点内容 |

以下不可稳定人工制造的异常只采用自动化权威结果，不伪称真机 PASS：并发快照失效、Room emission/service 返回先后顺序、collector 失败/耗尽、stale generation、未知 apply 结果后的只同步恢复。

## 安装、前台与截图

- `ANDROID_SERIAL=FMR0224725012307 ./gradlew :app:installDebug`：PASS，`Installed on 1 device`，`BUILD SUCCESSFUL`。
- 唤醒、解锁、启动 `com.yangchengwei.easytrip/.MainActivity`：PASS，`Status: ok`。
- 最终 `pidof`：`22268`；`mCurrentFocus` / `mFocusedApp` 均为 `com.yangchengwei.easytrip/.MainActivity`。
- 最终 `adb logcat -d -t 300 AndroidRuntime:E '*:S'`：无输出，未发现 AndroidRuntime 崩溃。
- 验收时已实际读取设置页、增长五日、缩短影响确认、缩短结果、非最后日删除、单日限制与返回工作台截图；无黑屏、桌面或严重裁切。
- 当时读取过 `/tmp/easytrip-task6-06-settings-real.png` 至 `/tmp/easytrip-task6-13-dengfeng-roundtrip.png` 及 `/tmp/easytrip-trip-settings-final.png`；这些会话期中间文件现已清理。
- 最终截图记录过一次“无法加载旅行”状态；后续冷启动恢复并可进入 `Task6Acceptance` 工作台。

## 剩余项与结论

- concern：专用验收旅行返回工作台时曾出现一次“无法加载旅行”，冷启动后恢复；需后续定位其瞬态导航/加载失败。
- concern：专用旅行为空，真机只读取到影响计数为 0；非空 item/RouteLeg/SavedPlace 级联仍由已通过自动化覆盖，不伪称真机验证。
- 非阻断视觉项：真机截图未见严重裁切；间距、字体和小幅基线差异留待最终视觉验收。
- 候选结论：**DONE_WITH_CONCERNS**；10 条路径中 9 PASS、1 CONCERN，安装、前台与崩溃检查通过。
- 本记录不代表运行或通过全量无过滤 `connectedDebugAndroidTest`。

## “无法加载旅行”后续调查（2026-08-28）

- 在同一台 `FMR0224725012307` / `ALN-AL00` 上，不清数据、不重装、不修改旅行，执行 `Task6Acceptance` 工作台“更多 → 设置 → 返回”最小循环 10 次；调查截图、UI hierarchy、日志和数据库快照均为会话期中间文件，现已清理。
- 复现：0/10。每轮返回后 250ms 截图和约 750ms UI hierarchy 均为可用工作台；PID 始终 `22266`，Easy Trip task 中既有 3 个 `MainActivity` 实例且循环中未增长。
- 返回立即截图显示的是退出动画中的旧设置页；未捕获“无法加载旅行”。
- 循环前后只读数据库快照 `PRAGMA integrity_check=ok`；3 个旅行及旅行日计数保持不变，`Task6Acceptance` 仍为 1 日。
- 本轮 logcat：`FATAL EXCEPTION=0`、`SQLiteException=0`、`无法加载旅行=0`、应用异常行 0。
- 已排除当前数据库损坏、目标旅行缺失、本轮稳定复现、进程重启、Activity 泄漏式增长及可见 Room/SQLite/AndroidRuntime 崩溃。
- 根因仍未知：错误文案由工作台聚合状态流捕获任一非取消异常后统一产生，当前没有记录原 throwable；不能把既有 3 Activity 实例或任一上游仓库认定为根因。
- 因根因未确认，本轮没有针对该瞬态问题的生产代码修改、失败测试或修复验证；原路径仍保留为 CONCERN。

## 第二轮最终修复后的 freshness（2026-08-28）

- 已修复复审残留的 day-delete impact late completion 与 active date phase 并发：日期流程独占写权限，删除 pending/retry/error 被失效清理，冲突删除弹层不渲染。
- 本轮新鲜定向验证：ViewModel 34/34、相关 trip UI/domain JVM 100/100、Content 11/11、Navigation 2/2、lint、diff check、Graphify update PASS。
- 因生产状态机与 Compose UI 再次变化，上述历史 447/447 JVM、118/118 connected 及 Mate 60 Pro 结果仍 stale；下一步必须 fresh rerun 完整门禁与物理真机验收。

## Residual fix round 2/5 freshness（2026-08-28）

- 已补齐反向互斥：单日删除执行期间，日期请求保持 Idle 且不会 preview/apply；active date phase 下的删除 guards 继续生效。
- 新鲜定向验证：ViewModel 35/35、相关 trip UI/domain JVM 101/101、Content 12/12、Navigation 2/2、lint、diff check、Graphify update PASS。
- 生产 ViewModel 再次变化，历史 447/447 JVM、118/118 connected 与 Mate 60 Pro 结果继续 stale；下一步必须 fresh rerun。

## 最终两个 Important 修复后的 freshness（2026-08-28）

- 产品规则：用户于 2026-08-28 明确选择旅行日期范围最多 30 天；创建和设置日期范围共享单一领域常量。
- Room 完成采用最新事实；matching emission 后出现 eligible fresh mismatch 会撤销确认，service 返回后继续等待，不清 dirty、不误报成功。
- 30 天通过；31 天与 `9999-12-31` 显示“旅行最多 30 天”，并在 deletionCounts/apply 或持久化海量日之前拒绝。
- 新鲜定向结果：新增 JVM 8/8；四个重点 JVM 类 67/67；相关 trip UI/domain JVM 109/109；`TripSettingsContentTest` 13/13；`TripDateRangeRoomTest` 11/11；`RoomTripRepositoryTest` 18/18，均 0 failures/errors/skipped。
- Boundary follow-up：service append RED 1/1、Room 30→31 RED 1/1 均按预期失败；GREEN 后 `TripSettingsViewModelTest` 39/39、`TripServiceTest` 11/11、相关 trip UI/domain JVM 112/112、`RoomTripRepositoryTest` 19/19。29→30 成功，30→31 在写入前拒绝且 Room 状态不变。
- Matching 覆盖已纠正为真实 same-size wrong/reordered，以及 shrink retained-ID wrong/reordered；增长前缀反例和 latest-fact 竞态覆盖继续保留。
- Date representability residual：`LocalDate.MAX` 的 1 日 dated trip 在 Validator/Service/Room 均通过；2 日 dated trip 显示“日期范围超出支持范围”，Service 不下传，Room 拒绝后列表与既有旅行不变。新鲜 Validator 12/12、Service 13/13、相关 trip JVM 117/117、RoomTripRepository 21/21 PASS。
- `setStartDate` residual：单日 trip 设置 `LocalDate.MAX` 通过；多日 trip 在 Service 层不调用 repository，Room 层事务拒绝且 `TripWithDays`/positions 不变。新鲜 Service 14/14、相关 trip JVM 118/118、RoomTripRepository 22/22 PASS。
- 未重跑全量 JVM、Catalog 47/47、Full UI 47/47 或完整真机验收；此前全门禁和真机结论继续 stale，由 controller 后续统一刷新。
- 既有 concerns 不变：瞬态“无法加载旅行”根因未知；非空内容级联未在物理设备直接覆盖。

## 最终 fresh 门禁与真机复验（2026-08-28）

### 自动门禁

| 命令 / suite | 测试数 | 失败 | 错误 | 跳过 | 结果 |
|---|---:|---:|---:|---:|---|
| `:app:testDebugUnitTest`（52 个 XML） | 465 | 0 | 0 | 0 | PASS |
| `TripSettingsContentTest` | 13 | 0 | 0 | 0 | PASS |
| `TripSettingsNavigationTest` | 2 | 0 | 0 | 0 | PASS |
| `TripDateRangeRoomTest` | 11 | 0 | 0 | 0 | PASS |
| `CascadeDeleteTest` | 2 | 0 | 0 | 0 | PASS |
| `V1ScenarioCatalogTest` | 47 | 0 | 0 | 0 | PASS |
| `V1FullUiAcceptanceTest` | 47 | 0 | 0 | 0 | PASS |

- connected 合计 122/122 PASS，0 failures/errors/skipped；运行前未发现其他 `am instrument` 或 `connectedDebugAndroidTest`，六个 suite 在唯一在线的 `FMR0224725012307 / ALN-AL00` 上严格串行执行。
- 未发生 runner crash、signal 9、设备离线或超时；未使用基础设施重试。
- `:app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest`：PASS，`BUILD SUCCESSFUL`。
- `ANDROID_SERIAL=FMR0224725012307 :app:installDebug`：PASS，`Installed on 1 device`。

### 真机关键路径

- 新建 `FreshGate` 三日旅行作为空内容验收 fixture。
- 30 天限制：输入 38 天后明确显示“旅行最多 30 天”，未进入创建确认、未写入超限旅行，PASS。
- 设置页进入与读取：PASS；截图 `/tmp/easytrip-final-settings.png`。
- 正常增长：3 日增长到 5 日，日期连续，PASS。
- 正常缩短：影响框显示删除 2 日、0 行程项、0 路线段、保留 0 收藏地点；取消后仍为 5 日，再次确认后为 3 日，PASS。
- 重复确认：确认坐标连续点击两次，仅完成一次缩短，无崩溃，PASS。
- 删除互斥基本路径：删除中间日后 3 日变 2 日并连续重编号；再次删除成单日；单日删除点击不弹确认且数据不变，PASS。
- 最终返回工作台：FAIL。首次从设置返回后显示“无法加载旅行 / 重试”；因此要求的额外 5 轮往返在第 1 轮即失败，完成 0/5，未继续伪造结果。

### 截图、前台与日志

- 已实际读取：`/tmp/easytrip-final-settings.png`、`/tmp/easytrip-final-overlimit.png`、`/tmp/easytrip-final-workspace.png`、`/tmp/easytrip-final-workspace-error.png`；均非黑屏或桌面，错误页明确可见。
- 失败时 PID `19641`；`mCurrentFocus` 与 `mFocusedApp` 均为 `com.yangchengwei.easytrip/.MainActivity`。
- `adb logcat -d -t 300 AndroidRuntime:E '*:S'` 无输出，未见 AndroidRuntime crash。
- 失败已追加到持续开发飞书诊断日志。

### 结论

- **BLOCKED**：自动门禁全部新鲜通过，但关键真机返回工作台路径失败。
- 历史“无法加载旅行” concern 本轮再次复现，仍保留“根因未知”，不写已修复。
- 专用旅行为空；非空 item/RouteLeg/SavedPlace 级联仍没有物理设备直接证据，继续保留为边界。
- 按用户要求，验证失败后未修改生产代码。

## Workspace missing-day race 修复后最终 fresh 复验（2026-08-28）

- 用户确认设备已解锁；复验前 `FMR0224725012307 / ALN-AL00` 在线，`showing=false`、`mWakefulness=Awake`。
- 全量 JVM XML：467/467 PASS，52 files，0 failures/errors/skipped；其中 `TripWorkspaceContentStateTest` 为 JVM suite，定向 13/13 PASS，已包含删除日失效先于 trip emission 的 race 覆盖。
- connected 仅运行于 `emulator-5554`，未在真机运行会清数据的 instrumentation；运行前无其他 instrumentation。Content 13/13、Navigation 2/2、DateRangeRoom 11/11、CascadeDelete 2/2、Catalog 47/47、Full UI 47/47，合计 122/122 PASS，0 failures/skipped，无 infra retry。
- `lintDebug + assembleDebug + assembleDebugAndroidTest`：PASS；`installDebug` 仅安装到 `ALN-AL00`，未清 app 数据。
- 保留并使用此前 `FreshGate` 数据。真机正常增长 1→4 日、缩短 4→2 日、删除至单日与最终日不可删除路径通过；30 天错误沿用本轮同一代码安装前已实际读取的“旅行最多 30 天”截图证据，且本次未创建超限内容。
- settings→workspace 原错误路径连续 5 轮均未出现“无法加载旅行”；前四轮使用页面返回控件，第五轮因旋转/坐标失配改用系统返回后确认工作台，均读取 UI hierarchy。历史 concern 仍保留，但本轮 0/5 未复现。
- 已实际读取最终设置页 `/tmp/easytrip-fresh2-final-settings-portrait.png` 与工作台 `/tmp/easytrip-fresh2-final-workspace-portrait.png`；非黑屏、非桌面、无严重裁切。
- 最终 PID `12744`；`mCurrentFocus` / `mFocusedApp` 均为 `.MainActivity`；最近 300 行 `AndroidRuntime:E` 无输出。
- 本轮未修改生产代码。历史 accidental data clear + restore 事实保持原记录，不改写；专用旅行为空，非空级联仍保持物理设备证据边界。
- 最新结论：**DONE_WITH_CONCERNS**。Workspace missing-day race 已有 TDD 根因修复；物理设备 5 轮复验未复现历史错误，但不把历史 concern 删除或笼统写成已修复。

## FreshGate 工作台根因修复候选（2026-08-28）

- 状态：**ROOT_CAUSE_FIXED_DEVICE_REVALIDATION_BLOCKED_BY_LOCK_SCREEN**；尚未完成要求的 5 轮真机路径，不升级为 DONE。
- 现场证据：错误 UI hierarchy 已保存；PID `19641`、前台 MainActivity、窗口与进程正常；完整 app PID logcat 无异常。只读 DB/WAL/SHM 快照 `integrity_check=ok`、无 foreign-key orphan；FreshGate 仅余 1 个合法 day。错误页点击“重试”立即恢复工作台。
- 根因：日删除 Room transaction 同时 invalidates trip 与 itinerary 查询；旧 day 的 itinerary JOIN 可能先发空结果并抛 `TargetDayNotFoundException`，早于新 `TripWithDays` emission 取消旧 snapshots，导致整个 workspace combine 终止。重试后只订阅现存 day，所以恢复。
- 初始 RED：确定性 JVM 用例 `deletedDayInvalidationBeforeTripEmissionDoesNotFailWorkspace` 修复前 1/1 按预期失败。
- Fix round 1/5 RED：新增 `missingDayStillPresentInLatestTripFailsWorkspace`；两条定向用例首次为 1 PASS / 1 expected failure，暴露无条件收敛会掩盖当前合法 day 的真实错误。
- GREEN：每条 authoritative trip emission 建立独立 snapshots 代。旧 day 已有 snapshot 后 typed missing 会等待下一条 trip emission；day 被移除时旧代取消并重建，仍被保留时新代立即传播 typed 异常到 Error。首次订阅即 missing 也传播。无延时猜测、无额外 repository collector；其他上游异常仍进入 Error。
- 全部 `workspace.*` JVM、`lintDebug`、diff check、Graphify update PASS。真机 `TripDateRangeRoomTest` 11/11 属本轮生产收紧前证据，已 stale；full gates 和物理设备复验仍待刷新。
- 数据事故：执行 focused connected 时 instrumentation 意外清除 app 数据；已使用测试前只读导出的 DB/WAL/SHM 原样恢复，并确认 FreshGate 单日记录重新出现。此操作违反“不修改现有用户旅行”约束，明确披露。
- 当前阻塞：修复版已安装，设备进入安全密码锁屏，ADB 无法解锁；需人工解锁后完成同真机至少 5 轮“工作台 → 设置 → 返回工作台”，每轮确认无错误页。
- 未 commit、未 push。

## 最终 fresh 自动化验收（用户取消最后生产变更后的真机复验，2026-08-28）

- 结论：**PASS（自动化范围）**。
- `:app:testDebugUnitTest`：52 个 XML，467/467 PASS，0 failures/errors/skipped；`TripWorkspaceContentStateTest` 13/13，包含 Workspace missing-day race 覆盖。
- 七个 connected suite 均显式使用 `ANDROID_SERIAL=emulator-5554` 且全局串行；运行前无其他 instrumentation：Content 13/13、Navigation 3/3、TripDateRangeRoom 11/11、RoomTripRepository 19/19、CascadeDelete 2/2、Catalog 47/47、Full UI 47/47，合计 142/142 PASS，最终完整运行 0 failures/errors/skipped。
- `V1FullUiAcceptanceTest` 首次运行期间 emulator-5554 退出，收到 22/47；重启同一 AVD 后按 infra 上限重试一次并 47/47 PASS。基础设施失败 1、重试 1。
- 自动化合计 609/609 PASS。
- `:app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest`、`git diff --check`、`graphify update .`：PASS。
- 用户取消最后生产变更后的真机手工复验。本轮没有执行物理设备验证；最近 Mate 60 Pro 的 5/5 历史复验仍保留，但早于后续 Route 系统 Back 生产修复，属于 stale，不能作为当前最新代码验证。
- 历史“无法加载旅行”与 accidental data clear + restore 继续保留；非空 item/RouteLeg/SavedPlace 级联仍无真机直接证据。
- 完整记录：`.superpowers/sdd/2026-08-27-easy-trip-trip-settings-date-correctness-convergence/final-gates-report.md`。
- 本轮仅更新验证文档与 Graphify 产物，未修改生产代码，未 commit、未 push。

## 最终审查系统返回修复 freshness（2026-08-28）

- 修复设置增长处于 `Applying` / `AwaitingRoom` 时 Android 系统 Back 可绕过禁用顶部返回的问题；Route busy 同时覆盖 `dayDeleteInProgress`，`SyncFailed` 和完成后的非 busy 返回保持可用。
- Route instrumentation 使用真实挂起 apply：RED 为 Navigation 3 tests / 1 expected failure；GREEN 后 Applying、AwaitingRoom 均不离开设置页，匹配 Room 完成后系统 Back 返回 workspace，Navigation 3/3 PASS。
- 新鲜定向门禁：相关 trip UI/domain JVM 113/113、Content 13/13、Navigation 3/3、lint、diff check PASS；connected 仅指定 `emulator-5554` 串行执行，未触碰 Mate 60 Pro 数据。
- 既有 ConfirmationDialog busy 返回锁未修改，Content 回归通过。
- 本轮生产 Route 已变化，此前 467/467 JVM、122/122 connected、构建和物理真机结果全部标记 stale；controller 随后复验，当前不得恢复完整候选结论。

## 终审 Important 定向验收（2026-08-28）

- 状态：**TARGETED_PASS_FULL_GATES_STALE**。
- `TripSettingsViewModelTest`：42/42 PASS；普通观察异常独立显示设置页加载错误，retry 仅重订阅并由新 emission 恢复；CancellationException 与 date commit SyncFailed 回归通过；删除执行中重复 request 不增加 impact 调用。
- 相关 `trip.ui.*` + `trip.domain.*` JVM：120/120 PASS，0 failures/errors/skipped。
- `TripSettingsContentTest`：14/14 PASS；错误文案和重试按钮可达，初始空白正文不再永久显示。
- `RoomTripRepositoryTest`：23/23 PASS；`LocalDate.MAX` 单日 no-op 合法，2 日 apply 写前拒绝且 trip/positions 不变。
- `lintDebug`、`git diff --check` PASS；connected 均在 `emulator-5554` 串行执行。
- 完整 JVM、其余 connected、Catalog、Full UI、APK 构建与物理真机验收未运行，旧结果均 stale。
