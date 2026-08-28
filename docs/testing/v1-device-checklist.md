# Easy Trip v1 真机验收清单

> 只有本清单全部通过且完整自动化命令成功后，才能声明 v1 完成。模拟器结果不能替代真机结果。

## 验收环境

- 既有 #1–#12 验收日期：2026-08-22
- 本轮 #13–#17 验收日期：2026-08-28
- 设备型号：Huawei ALN-AL00（序列号不记录）
- 既有 #1–#12 验收 Android 版本：12
- 本轮 #13–#17 真机 Android 版本：12（`ALN-AL00`，1260×2720 @ 520 dpi）
- App commit：基线 `253b8a8`，验收构建包含未提交工作树改动
- 构建类型：debug
- 高德 Key 对应包名：`com.yangchengwei.easytrip`
- 网络环境：已联网，具体类型待填写

## 检查项

| # | 场景 | 操作与预期 | 结果 | 备注 |
|---|---|---|---|---|
| 1 | 地图与生命周期 | 同意隐私协议，进入工作台，确认瓦片显示；切后台/前台、旋转或重建 Activity 后地图可用且无崩溃 | 通过 | 真机验证通过 |
| 2 | 搜索与收藏 | 搜索并收藏至少 10 个真实 POI；搜索结果同时出现在地图和抽屉；重复收藏被阻止 | 通过 | 真机验证通过 |
| 3 | 四类路线 | 分别验证步行、打车、自驾、公交；距离、耗时和轨迹来自高德真实响应 | 通过 | 真机验证通过 |
| 4 | 跨城市公交 | 选择跨城市地点；接受真实可用路线，或显示明确“不支持公交规划/无可用路线” | 通过 | 真机验证通过 |
| 5 | 重复地点 | 三日旅行中同一酒店每天至少出现两次；每次均为独立行程项，同坐标 marker 可查看全部安排 | 通过 | 真机验证通过 |
| 6 | 日内编辑 | 新增、删除、连续拖动、跨日移动；只让变化的邻接路段重算，未变化路线保持缓存 | 通过 | 真机确认新增、长按拖动、删除和跨日移动均正常，路线随后正常更新 |
| 7 | 地图范围 | 切换地点池、单日、全程；单日按顺序编号，全程各日颜色可区分 | 通过 | 真机确认三种范围、单日编号、全程颜色及切换后地图视角保持正常 |
| 8 | 离线恢复 | 飞行模式下继续编辑已有地点；新路线显示等待联网；恢复网络后自动续算 | 通过 | 真机确认飞行模式下可编辑并显示等待联网，恢复网络后自动续算 |
| 9 | 强杀恢复 | 保存旅行、地点、编排及成功路线后强杀并重启；数据与路线仍存在，遗留计算自动恢复 | 通过 | 真机强制停止并重启后，旅行、收藏地点、行程顺序、成功路线及授权状态均保留 |
| 10 | 性能 | 至少 30 个 marker、7 天路线；缩放、拖动地图与切换抽屉无明显卡顿，地图视口不因 sheet/tab 重组重置 | 通过 | 真机验证通过 |
| 11 | 授权撤销 | 撤销地图授权后地图/搜索/路线停止，离线行程编辑仍可用；重新授权后路线继续 | 通过 | 真机验证通过 |
| 12 | 删除确认 | 删除被引用地点前显示引用次数；确认后所有引用和旧路段消失，并生成必要桥接路段 | 通过 | 真机验证通过 |
| 13 | 旅行设置往返 | 从当前旅行工作台“更多”进入设置并返回；保持原 trip、原 section、地点池和行程可用 | 有关注项 | Workspace missing-day race 已有 TDD 根因修复；最终 fresh 真机连续 5 轮均正常返回同一 FreshGate 工作台，未见“无法加载旅行”。历史事件仍保留说明，不删除 concern |
| 14 | 日期增长 | 延长结束日期；新增旅行日连续，原旅行日和内容保持 | 通过 | 专用旅行 8/29..8/31 延长至 9/2，五日连续；原三日保持 |
| 15 | 日期缩短取消与确认 | 核对旅行日、行程项、RouteLeg、SavedPlace 影响；取消后数据不变，再次提交后尾部内容级联删除且日期重编号 | 通过 | 影响框显示删除 2 日、0 item/leg、保留 0 SavedPlace；取消不改数据，再确认缩至三日；非空级联由自动化覆盖 |
| 16 | 单日删除 | 删除非最后一个旅行日后内容级联且日期重编号；最后一个旅行日不可删除 | 通过 | 删除中间日后连续重编号；单日时点击删除不弹确认且数据不变 |
| 17 | 日期提交单飞 | 快速重复点击确认只执行一次；并发快照失效和 Room Flow 异常以自动化测试为权威 | 通过 | 连续点击确认最终只应用一次且无崩溃；不可稳定制造的异常沿用自动化权威结果 |

## 当前 freshness（2026-08-28）

- 本轮新增 Room latest-fact 竞态修复与统一 30 天上限后，表中既有真机结果及此前完整自动化门禁均为 stale 历史证据。
- 新鲜定向证据：相关 trip UI/domain JVM 109/109、设置 Content 13/13、日期 Room 11/11、RoomTripRepository 18/18 PASS。
- 完整 JVM、Catalog、Full UI 与物理真机不在本轮重跑范围，由 controller 后续统一执行。

## 地图授权与定位权限候选门禁（2026-08-28）

- 自动化候选门禁：**PASS**，详见 `docs/testing/map-consent-location-permission-acceptance.md`。
- 全量 JVM：`517 / 0 / 0 / 0`；connected：`WorkspacePermissionFlowTest` `7/7`、`TripWorkspaceContentTest` `25/25`、`AmapComposeMapTest` `12/12`、`PlaceSearchContentTest` `22/22`、`WorkspaceFlowTest` `27/27`、`V1ScenarioCatalogTest` `47/47`、`V1FullUiAcceptanceTest` `47/47`，累计 `187 / 0 / 0 / 0`。
- `lintDebug`、`assembleDebug`、`assembleDebugAndroidTest`、更新前后 `git diff --check` 与 `graphify update .` 均 PASS。Catalog 曾出现一次 split APK 安装 infra 失败，唯一重试后 `47/47` PASS；infra retry=1。历史产品/同步失败均已修复并保留于验收记录。
- 本轮未使用物理设备，未清除物理设备数据；地图授权和定位权限的 12 条真机关键路径仍为 `NOT-RUN`，不得以模拟器结果替代。

## 视觉收敛最终候选门禁（2026-08-29）

- 候选结论：**PASS（自动化候选）**；详见 `docs/testing/2026-08-29-visual-convergence-acceptance.md`。
- 执行环境：`emulator-5554` online、boot complete，启动时无 instrumentation。
- 精确计数：全量 JVM XML `521 / 0 failures / 0 errors / 0 skipped`；22 个指定 connected suites XML 合计 `351 / 0 / 0 / 0`，其中 `V1ScenarioCatalogTest` 和 `V1FullUiAcceptanceTest` 各 `47 / 0 / 0 / 0`。
- 构建门禁：`lintDebug`、`assembleDebug`、`assembleDebugAndroidTest`、`compileDebugAndroidTestKotlin` 均 PASS；`git diff --check` 在 `graphify update .` 前后均 PASS。
- Infra：外层 10 分钟命令时限在 `WholeTripItineraryContentTest` 执行时中断一次；设备无残留 instrumentation，按规则唯一重试后该 suite `6 / 0 / 0 / 0`。`infra retry=1`，无产品失败。
- Pencil 对照及截图诊断：基线 `design/easy-trip-v1.0.pen`，frame 覆盖 `U06l7P`、`IKTv5`、`ofdn5`、`p4G1tS`、`A9EKX`、`eHTX3`、`zIbEu`、`lsr1I`、`Bcf6A`、`dzhkC`、`batch5`、`batch6-search`、`batch6-failure`；诊断截图写入模拟器私有路径 `/data/user/0/com.yangchengwei.easytrip/files/evidence/batch-0/`，测试安装清理后未保留可导出副本。历史截图在 `.superpowers/sdd/2026-08-23-easy-trip-v1-full-ui-implementation/task-4-evidence/`。
- 物理真机：**NOT-RUN**；未清除物理真机数据。截图不作像素阻断，严重裁切、关键交互故障、崩溃、不可达流程或数据不一致才阻断。
- 未运行无过滤 474-test connected suite：它包含真实 SDK smoke 的预期 skip，且不属于本次正式归档范围；定向 22 suite 已覆盖本批关键族与 47 场景。

## 自动化基线

- `./gradlew clean test lint assembleDebug connectedDebugAndroidTest`
- 预期：exit 0，debug APK 生成，所有 JVM/Room/UI 测试通过。

## 结论

当前状态：**DONE_WITH_CONCERNS**。已确认根因为删除日时旧 itinerary Room Flow 先于 trip day-list emission 抛 `TargetDayNotFoundException`，并以 focused RED/GREEN 修复。最终 fresh JVM 467/467、connected 122/122 与构建门禁通过；connected 仅在 `emulator-5554` 执行，真机只安装并手工复验。Mate 60 Pro 的 30 天错误、增长、缩短、日删除及 settings→workspace 连续 5 轮均通过。历史“无法加载旅行”事件继续保留说明；非空内容级联仍未在物理设备直接覆盖。

## FreshGate 工作台修复复验补充（2026-08-28）

- [x] 失败现场 UI hierarchy、完整 app PID logcat、dumpsys activity/window/meminfo 已采集。
- [x] `run-as` 只读 DB/WAL/SHM 导出：integrity ok，无 foreign-key orphan，FreshGate 为 1 个合法 day。
- [x] RED：`deletedDayInvalidationBeforeTripEmissionDoesNotFailWorkspace` 修复前按预期失败。
- [x] Fix round 1/5 GREEN：每条 authoritative trip emission 建立 snapshots 代；旧 day missing 后随下一条列表移除则取消旧代，若仍保留则新代立即进入 Error。全部 `workspace.*` JVM 121/121、lint、diff、Graphify PASS；无时间延迟或额外 repository collector。此前真机 `TripDateRangeRoomTest` 11/11 在本次收紧前，现为 stale。
- [x] 同一 Mate 60 Pro 原路径至少 5 轮：用户解锁后完成 5/5，均回到同一 FreshGate 工作台且无“无法加载旅行”。
- [x] 数据事故记录：focused connected 意外清数据；已从测试前完整快照恢复并确认 FreshGate 存在。
