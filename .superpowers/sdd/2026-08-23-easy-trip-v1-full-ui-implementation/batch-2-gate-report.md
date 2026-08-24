# Batch 2 Gate Report

## Status

**BLOCKED**

Batch 2 自动化 gate 未通过。按“发现失败，准确报告并停止”的要求，已停止后续 APK 安装、生产旅程 B、截图采集和 Pencil 视觉对比；未修改生产代码，未修改 `progress.md`，未 commit。

- 工作目录：`/Users/bytedance/Code/easy-trip/.claude/worktrees/easy-trip-v1-full-ui-run`
- 验证提交：`20fae8ce6208bec43a52dce49a115fc41e1506d6`
- 设备：`emulator-5554`，Android 12，`1220x2700`，density `440`
- 验证日期：2026-08-24

## 执行命令与结果

### 1. Graphify 定向查询

```bash
graphify query "Batch 2 gate search continuous collection place pool detail map controls"
```

结果：成功；定位到 Batch 2 Gate、Task 4/5、`PlaceSearchContentTest`、`PlacePoolViewModelTest`、`PlaceDetailContent`、地图控件等相关节点。Graphify 提示 skill 版本 `0.9.32`、package `0.9.48`，本次只做验证，未更新安装或图谱。

### 2. 全套 JVM、lint、assemble

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

结果：**PASS**，`BUILD SUCCESSFUL in 10s`。

- JVM：222 tests，0 failures，0 errors
- `lintDebug`：PASS
- `assembleDebug`：PASS
- Gradle：58 actionable tasks，3 executed，55 up-to-date

JVM 数量汇总命令：

```bash
find app/build/test-results/testDebugUnitTest -type f -name 'TEST-*.xml' -print0 \
  | xargs -0 grep -h '<testsuite' \
  | perl -ne '$t += /tests="(\d+)"/ ? $1 : 0; $f += /failures="(\d+)"/ ? $1 : 0; $e += /errors="(\d+)"/ ? $1 : 0; END { print "tests=$t failures=$f errors=$e\n" }'
```

输出：`tests=222 failures=0 errors=0`。

### 3. 设备检查

```bash
adb devices -l
adb -s emulator-5554 shell getprop ro.build.version.release
adb -s emulator-5554 shell wm size
adb -s emulator-5554 shell wm density
```

结果：设备在线；Android 12；物理分辨率 `1220x2700`；density `440`。

### 4. Task 4/5 目标 Compose 与相关 Room 测试

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchContentTest,com.yangchengwei.easytrip.place.ui.PlacePoolFlowTest,com.yangchengwei.easytrip.workspace.MapLayerFlowTest,com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepositoryTest
```

结果：**FAIL**，`BUILD FAILED in 29s`。

- 总计：22 tests
- 通过：21
- 失败：1
- 跳过：0
- `PlaceSearchContentTest`：6/6 通过
- `PlacePoolFlowTest`：7/7 通过
- `MapLayerFlowTest`：4/4 通过
- `RoomSavedPlaceRepositoryTest`：4/5 通过

失败用例：

```text
com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepositoryTest
  usageCountsRefreshWhenOnlyItineraryItemsChange
```

失败信息：

```text
kotlinx.coroutines.TimeoutCancellationException:
Timed out after 5s of _virtual_ (kotlinx.coroutines.test) time.
To use the real time, wrap 'withTimeout' in
'withContext(Dispatchers.Default.limitedParallelism(1))'
```

失败位置：`RoomSavedPlaceRepositoryTest.kt:82`。

## 复现与证据

复现：在 `emulator-5554` 在线时，从工作目录运行上述第 4 条 Gradle 命令；测试执行到 Room usage-count invalidation 场景时发生 5 秒虚拟时间超时，Gradle 以 exit code 1 结束。

证据路径：

- Android 测试 XML：`app/build/outputs/androidTest-results/connected/debug/TEST-easy_trip_p60pro(AVD) - 12-_app-.xml`
- 失败 logcat：`app/build/outputs/androidTest-results/connected/debug/easy_trip_p60pro(AVD) - 12/logcat-com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepositoryTest-usageCountsRefreshWhenOnlyItineraryItemsChange.txt`
- HTML 报告：`app/build/reports/androidTests/connected/debug/index.html`

## 功能旅程结果

**未执行。** 自动化 gate 已失败，因此按要求停止，没有安装/启动当前 APK，也没有声称以下生产入口场景通过：

- 搜索 → 连续收藏至少两个 → 主动返回 → 地点池 → 查看/编辑备注和标签
- 空地点池
- 地点详情仅收藏/备注/标签
- 地图图层菜单
- 搜索 Surface

## 截图与视觉对比

**未采集新截图/XML。** 因自动化失败后立即停止，没有读取或分析截图，也没有通过 Pencil MCP 对比以下 frame：

`A9EKX`、`ofdn5`、`p4G1tS`、`shoPV`、`lsr1I`、`S0psO`、`GJo79`、`s1OvvX`、`I62qd5`

这些 frame 的独立视觉分批审查仍全部待办，不能据此宣称视觉 gate 通过。

## 阻塞与后续

1. 修复或确认 `usageCountsRefreshWhenOnlyItineraryItemsChange` 的协程测试时钟/Room invalidation 超时问题。
2. 重新运行完整目标 Compose + Room 命令并取得 0 failures。
3. 自动化通过后，再安装当前 APK并执行完整生产旅程 B及补充场景。
4. 采集缩放后最长边不超过 1280 的 JPEG/WebP 和必要 XML。
5. 按单 agent 图片限制，仅分析最关键 1–2 张，其余交由独立视觉分批审查。
6. 所有自动化、功能旅程和视觉审查全部通过前，Batch 2 gate 保持 blocked/in progress；`progress.md` 保持不变。

---

## 生产功能旅程续跑（2026-08-24）

### 结论

**功能 gate：FAIL（BLOCKED）**

按本轮要求，仅安装并操作生产 `MainActivity`，未使用测试 `Content`，未修改生产代码、未修改 `progress.md`、未 commit。旅程在进入旅行工作台时被模拟器 OpenGL/EGL 崩溃阻断，无法继续验证搜索、连续收藏、地点池、详情编辑、地图图层菜单和搜索 Surface；以下未完成项均不判为通过。

### 设备与构建

- 工作目录：`/Users/bytedance/Code/easy-trip/.claude/worktrees/easy-trip-v1-full-ui-run`
- 设备：`emulator-5554`，Android 12，`1220x2700`，density `440`
- APK：`app/build/outputs/apk/debug/app-debug.apk`
- APK SHA-256：`89c050b2d96697282a9c31e7d06ae8d3ba982eb75caa8053b3511074a30c8352`
- 包：`com.yangchengwei.easytrip`，`versionCode=1`，`versionName=1.0`
- 入口：`com.yangchengwei.easytrip/.MainActivity`

### 实际步骤与结果

1. 执行 Graphify 定向查询，定位 `MainActivity`、`WorkspaceOverlay`、Task 4/5、搜索、地点池及详情相关生产入口：**PASS**。
2. `adb -s emulator-5554 install -r app-debug.apk`：**PASS**。
3. `pm clear com.yangchengwei.easytrip` 后启动生产 `MainActivity`：**PASS**；XML 断言显示“还没有旅行计划”和“创建旅行”。
4. 通过生产 UI 创建 `Batch2Trip`（2 天、日期待定、灵活）：**PASS**；返回旅行列表后 XML 显示旅行卡片。
5. 点击旅行卡片进入生产工作台，勾选“我已阅读高德隐私权政策”并点击“同意并启用搜索”：**FAIL/BLOCKED**。Activity 退出到 Launcher；再次启动并进入同一旅行后稳定出现系统对话框 `Easy Trip keeps stopping`。
6. Logcat 根因：生产进程 GL 线程抛出 `java.lang.RuntimeException: createContext failed: EGL_SUCCESS`，栈位于 `android.opengl.GLSurfaceView$EglHelper.start` / `GLSurfaceView$GLThread.guardedRun`。这是模拟器图形上下文/地图渲染阻塞；本轮没有修改代码规避。
7. 搜索真实地点 → 连续收藏至少两个且留在搜索页 → 主动返回 → 地点池显示收藏：**未执行（被第 5 步阻断）**。
8. 打开详情 → 编辑备注和标签并保存；确认无加入行程入口：**未执行（被第 5 步阻断）**。
9. 空地点池搜索入口：**未执行（工作台无法保持运行）**。
10. 地图图层菜单通过排他 `WorkspaceOverlay`：**未执行（工作台无法保持运行）**。
11. 搜索 Surface 近全宽且不透明：**未执行（工作台无法保持运行）**。

### adb/uiautomator 断言

- 初始空旅行列表：`/tmp/easy-trip-batch2-final/00-launch.xml`
- 创建旅行表单：`/tmp/easy-trip-batch2-final/01-create-trip.xml`
- 创建后的旅行列表（含 `Batch2Trip`、`2 天`）：`/tmp/easy-trip-batch2-final/07-confirm.xml`
- 高德隐私同意界面：`/tmp/easy-trip-batch2-final/08-workspace.xml`
- 重进工作台后的系统崩溃对话框：`/tmp/easy-trip-batch2-final/11-workspace.xml`
- XML 断言结果：`Easy Trip keeps stopping` 存在，断言 **PASS**。

### 截图与日志证据

- 同意高德服务后 Activity 退出到 Launcher：`/tmp/easy-trip-batch2-final/09-workspace-empty.jpg`（`578x1280`）
- 重进工作台后的系统崩溃对话框：`/tmp/easy-trip-batch2-final/11-workspace-crash.jpg`（`578x1280`）
- 同意阶段 logcat：`/tmp/easy-trip-batch2-final/09-consent-logcat.txt`
- 崩溃筛选 logcat：`/tmp/easy-trip-batch2-final/11-crash-logcat.txt`

本轮未读取分析截图；JPEG 均已在读取前缩放至最长边 `1280`。原始 PNG 仅采集留档，未读取分析。

### Concerns

1. `emulator-5554` 上 AMap/`GLSurfaceView` 无法建立 EGL context，导致生产工作台崩溃，完整旅程 B 没有可达入口。
2. 因工作台在地图初始化阶段崩溃，无法对网络搜索是否可用作独立判断；不能把未到达搜索请求等同于网络失败。
3. 本轮功能 gate 必须保持 **FAIL/BLOCKED**；视觉审查仍未完成，不能更新 `progress.md` 或宣称 Batch 2 通过。

---

## API 36 恢复环境生产旅程 B（2026-08-24）

### 结论

**功能 gate：PASS**

**视觉状态：in progress**

旧 API 31 / `easy_trip_p60pro` 的自动化失败与 EGL 阻塞记录保留在上文。本节是在恢复后的独立环境中续跑生产 UI 所得结果，不覆盖历史失败。

- 设备：`emulator-5554`
- AVD：`trail_map_api36`
- Android API：36
- 图形环境：ANGLE + SwiftShader Vulkan，OpenGL ES 3.1
- 应用：`com.yangchengwei.easytrip/.MainActivity`，验证结束时进程存活且 Activity 保持焦点
- 操作方式：仅使用 `adb` / UIAutomator 操作和 XML 断言
- 代码与仓库：未修改生产代码，未修改 `progress.md`，未 commit

### 生产旅程逐步结果

1. 环境与应用存活：**PASS**。API 为 36；SurfaceFlinger 报告 ANGLE、SwiftShader Vulkan 和 OpenGL ES 3.1；生产 Activity 保持焦点。
2. 真实地点搜索 `coffee`：**PASS**。生产搜索返回 20 项。
3. 连续收藏至少两个地点：**PASS**。先后收藏 `Peet's皮爷咖啡(灵境胡同店)` 与 `星巴克(老佛爷百货店)`；第一次收藏后搜索页仍存在“返回地点池”且目标动作变为“取消收藏”，第二次收藏后同一搜索页同时出现两个“取消收藏”。
4. 主动返回地点池：**PASS**。展开后的地点池包含本轮收藏的上述两个地点。该旅行此前已有其他收藏，因此结论是“包含本轮两项”，不是“池中只有两项”。
5. 打开地点详情：**PASS**。通过生产地点池打开 `Peet's皮爷咖啡(灵境胡同店)` 的详情编辑界面。
6. 修改并保存备注、标签：**PASS**。实际保存值为备注 `NoteB2`、标签 `coffe`。原计划输入更长标签，但 adb 输入被截断；本报告仅记录 XML 确认的实际值。
7. 关闭并重新打开详情确认持久化：**PASS**。重新打开后的两个 `EditText` 分别为 `NoteB2` 和 `coffe`。
8. 地点详情无“加入行程”入口：**PASS**。首次打开及持久化重开 XML 均无“加入行程”，可用动作包括“取消收藏”和“保存”。
9. 地点池 empty state 与搜索入口：**PASS**。新建独立空旅行 `EmptyB2` 后出现“还没有收藏地点”及“搜索地点”；点击真实按钮进入生产搜索页。创建表单中的旅行天数最终为 `18`，不影响空地点池验证。
10. 地图图层排他 overlay：**PASS**。打开状态出现“地图图层”“关闭”“✓ 标准”“卫星”等菜单语义；关闭后菜单专属语义“关闭”“✓ 标准”“道路、建筑和地点信息”“卫星影像叠加道路”“选择将应用到所有旅行”全部消失。Graphify 定位该生产状态为 `WorkspaceOverlay.LayerMenu`。
11. 搜索 Surface：**PASS（功能/结构）**。搜索框 bounds 为 `[222,108][1220,252]`，位于返回按钮右侧并占据标题区域绝大部分宽度；实际截图显示搜索主体为不透明白色 Surface。完整 Pencil 像素级审查仍未完成。
12. 真实搜索无结果：**PASS**。不存在的查询实际落入输入框的文本为 `zzzzznonexi`，页面出现“没有找到相关地点”“试试更短的关键词，或检查地点名称是否正确。”及“清空搜索”。

### 证据映射

证据根目录：`/tmp/easy-trip-batch2-journey-b/`

| 状态 / frameId | 证据 | 断言 |
|---|---|---|
| 环境 | `00-device.txt`、`00-focus.txt` | API/设备与生产 Activity 状态 |
| `ofdn5` 搜索与首次收藏 | `01-ofdn5-first-favorite.xml`、`01-ofdn5-first-favorite.jpg` | 收藏后仍在搜索页 |
| `ofdn5` 连续收藏 | `02-ofdn5-two-favorites.xml`、`02-ofdn5-two-favorites.jpg` | 同页出现两个“取消收藏” |
| `A9EKX` 地点池 | `03-A9EKX-place-pool.xml`、`03-A9EKX-place-pool.jpg`、`04b-pool-expanded.xml` | 返回地点池并包含本轮两个收藏 |
| `I62qd5` 搜索返回地点池 | `03-I62qd5-place-pool-search-return.xml`、`03-I62qd5-place-pool-search-return.jpg` | Pencil 中该 frame 名为“02 地点池 · 搜索返回”；复用同一次真实返回状态证据 |
| `p4G1tS` 详情打开 | `04-p4G1tS-detail-open.xml`、`04-p4G1tS-detail-open.jpg` | 打开生产详情 |
| 保存后地点池 | `06-after-save.xml` | 条目显示 `NoteB2` 与 `coffe` |
| `p4G1tS` 持久化重开 | `07-p4G1tS-detail-persisted.xml`、`07-p4G1tS-detail-persisted.jpg` | 备注和标签持久化，且无“加入行程” |
| `shoPV` 图层菜单 | `08-shoPV-map-layers.xml`、`08-shoPV-map-layers.jpg` | LayerMenu 打开 |
| `shoPV` 菜单关闭 | `09-shoPV-map-layers-closed.xml` | 菜单专属语义消失 |
| `lsr1I` 空地点池 | `13-lsr1I-empty-place-pool.xml`、`13-lsr1I-empty-place-pool.jpg` | 空状态与搜索入口 |
| `ofdn5` 空状态搜索入口 | `14-ofdn5-empty-search-entry.xml` | 从空地点池进入搜索页；搜索框近全宽 |
| `S0psO` 无结果 | `15-S0psO-no-results.xml`、`15-S0psO-no-results.jpg` | 真实无结果状态及不透明搜索 Surface |

所有最终 JPEG 均已检查为 `573x1280`，最长边不超过 1280。仅分析了两张截图：`04-p4G1tS-detail-open.jpg` 与 `15-S0psO-no-results.jpg`；其余截图只采集和记录。`05-p4G1tS-detail-filled.*` 实际捕获的是地点池，不作为详情填写成功证据。

### 视觉缺口与 concerns

1. `GJo79`（搜索网络失败）未采证：未为了画面伪造失败状态，也未在本轮破坏已恢复的网络环境。
2. `s1OvvX`（搜索加载中）未稳定捕获：真实请求完成过快，未伪造 loading 状态。
3. `I62qd5` 已通过 Pencil MCP 确认为“02 地点池 · 搜索返回”，并映射到同一次真实返回地点池证据；尚未做像素级视觉比较。
4. 功能旅程和补充状态均通过，但目标 frame 的独立视觉对照尚未完成，因此总视觉状态保持 **in progress**，不能据此宣称完整 Batch 2 视觉 gate 通过。

---

## 地点池视觉修复复核（2026-08-24）

### 结论

**视觉子项：PASS；Batch 2 总 gate：in progress**

仅以 `design/easy-trip-v1.0.pen` 为设计基线，确认 A9EKX 的 sheet 为 y=448/844、高 396，I62qd5 为 y=432/844、高 412。生产实现现使用普通态 396dp、搜索返回态 412dp，并通过 safe drawing inset 避让系统栏。

### 验证结果

- `TripWorkspaceContentTest`：7/7 PASS；覆盖固定 sheet 高度、顶部安全区、搜索返回态、离屏节点滚动与底部 24dp 避让。
- Batch 2 目标 suite：22/22 PASS。
- `testDebugUnitTest lintDebug assembleDebug`：BUILD SUCCESSFUL。
- 生产 APK 安装到 API 36 `trail_map_api36`；Emulator 启动参数包含 `-gpu swiftshader_indirect`。

### 生产证据

证据目录：`/tmp/easy-trip-batch2-visual-fix/`。

- `A9EKX-place-pool.xml/.jpg`：普通地点池，两项状态均为“仅收藏”。
- `I62qd5-search-return.xml/.jpg`：真实搜索连续收藏两项并返回，两项状态均为“刚刚收藏 · 待安排行程”。
- `lsr1I-empty-place-pool.xml/.jpg`：空地点池与搜索入口。

三张 JPEG 均为 573×1280；仅读取 A9EKX、I62qd5 两张。XML 显示顶部栏 y=115，普通态首卡 y=1897、返回态首卡 y=1849，返回态相对上移；卡片底部位于系统导航区之上。UIAutomator 未把 Compose LazyColumn 标记为 `scrollable=true`，因此滚动结论来自 instrumentation 的 `performScrollToNode`，不是伪造 XML 断言。

### 剩余项

`GJo79` 网络失败与 `s1OvvX` 加载中仍待生产证据；Batch 2 总 gate 保持 **in progress**。

详见 `batch-2-visual-fix-report.md`。
