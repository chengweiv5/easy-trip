# Batch 2 搜索受控状态证据契约

## 范围

`PlaceSearchEvidenceTest` 使用 `createAndroidComposeRule<ComponentActivity>()`、生产主题和生产 `PlaceSearchContent`，分别注入 query“西湖”的 `Loading` 与标准文案 `NetworkFailure`。方法和 PNG：

- `captureLoadingEvidence_s1OvvX_controlledStateRender` → `v1-44-s1OvvX-loading-controlled.png`
- `captureNetworkFailureEvidence_GJo79_controlledStateRender` → `v1-38-GJo79-network-failure-controlled.png`

测试不调用 datasource、网络或高德 SDK，不修改网络，不引入生产 fixture。Instrumentation 会按 Android 测试框架启动既有 target Application；本测试不改变生产启动架构，证据仅覆盖测试 Activity 内生产 Composable 的受控渲染。

## 视觉契约

两种状态均断言查询词、返回按钮、搜索框、Surface 可见，返回区 44dp、搜索框高 48dp。390dp 窗口下 Surface 约为 350dp；其他窗口按左右各 20dp 设计边距适配为约 `windowWidthDp - 40dp`，允许 1dp 布局取整误差。Surface、返回区、搜索框必须完整位于窗口内，返回区与搜索框不得重叠。Loading 的标题和 query 插值说明必须位于 Surface 内且互不重叠；NetworkFailure 的全部关键节点必须位于 Surface 内且相邻内容不重叠，并保持 48dp 图标及 48dp 动作高度。

manifest 同时记录实际 bitmap 像素、实际 window dp 和设计目标 `target_window_width_dp=390`，非 390dp 设备明确属于 responsive adaptation，不伪装为目标尺寸。任何 render/assert 之前，各方法先清理自己的旧 PNG、JSON manifest、complete marker 和临时文件；因此早期断言失败不会留下旧完整证据。

## 构建 provenance

Gradle 配置阶段以显式 `workingDir(rootDir)` 执行 Git：

- `BuildConfig.GIT_SHA`：`git rev-parse HEAD`。
- `BuildConfig.SOURCE_STATE`：状态为空时 `CLEAN`；存在 staged、unstaged 或 untracked 项时为 `DIRTY:<sha256>`，hash 输入是 `git status --porcelain=v1 -z`。
- Git 或 `.git` 不可用时两者为 `UNAVAILABLE`，普通 App 构建仍可继续。

证据测试一律要求 40 位 Git SHA；仅当 instrumentation 参数 `formalEvidenceArchive=true` 时另要求 `SOURCE_STATE == CLEAN`。普通 controlled-state render 可在 `SOURCE_STATE=CLEAN` 或 `DIRTY:<sha256>` 下生成 PNG / manifest / `.complete`；DIRTY manifest 必须如实记录该 provenance，且只能作为受控渲染诊断，不可宣称 clean formal archive。Git 或 `.git` 不可用时普通 App 构建可继续，但 `BuildConfig.GIT_SHA=UNAVAILABLE` 不符合证据测试的 40 位 SHA guard，普通 controlled evidence 与 formal archive 均会在截图前拒绝发布；不能将 `UNAVAILABLE` 写为可发布 evidence provenance。dirty hash 不承担源码内容绑定，避免把只反映路径状态的 hash 当作源码内容证明。设备 provenance 来自 target device 的 `Build.FINGERPRINT`、`MODEL`、SDK；serial 因应用内不可可靠读取而标记 `unavailable`，ADB serial 仅用于外部选机。

## 发布与消费协议

不声称两个文件可获得跨平台文件系统事务。每个 case 的协议为：

1. 清理该 case 所有最终文件、临时文件和 complete marker。
2. PNG 写临时文件，检查 screenshot、compress、fsync、非空，再 rename 为最终 PNG。
3. 计算 PNG SHA-256；以 `JSONObject` 写结构化 JSON manifest 临时文件，再 rename。
4. capture 时冻结 expected snapshot；在 complete 不存在时重新解析 JSON，以 `JSONObject.get` 原始运行时类型严格拒绝字符串/浮点冒充整数等转换，逐项比对 build、device、window、density、font scale、locale、frame、method 和 hash。PNG 另用 `BitmapFactory.decodeBounds` 校验实际宽高。
5. 验证通过后才写 complete 临时文件，其中包含 manifest SHA-256，再 rename 为 `.complete`；它是唯一发布点。发布后再次校验完整链。任何异常都会删除该 case 全部文件。

消费者只认可 `.complete` 存在、其内容等于 manifest hash、manifest 中 PNG hash 自校验通过的 pair。仅有 PNG 或 manifest 均是未发布残留，不得作为证据。

manifest 固定标记 `controlled-state-render` 和 `not_production_real_trigger=true`。它不证明正式导航或真实请求触发状态。状态来源另由 `PlaceSearchReducerTest`、`PlaceSearchViewModelTest`、`AmapPlaceDataSourceTest` 证明；隐私拒绝不能冒充网络失败。

## 2026-09-05 Batch 10 归档核对

- 只读核对 `PlaceSearchEvidenceTest#captureLoadingEvidence_s1OvvX_controlledStateRender` 与 `#captureNetworkFailureEvidence_GJo79_controlledStateRender`：两者分别注入 `Loading` / `NetworkFailure` 到同一个生产 `PlaceSearchContent`；代码固定写入 `evidence_type="controlled-state-render"` 与 `not_production_real_trigger=true`。
- 当前 HEAD `1923ccb45716a5242d6dee13c020e4ba7b7cc91a` 的仓库 tracked 文件和可访问工作树中，均未找到 `s1OvvX` / `GJo79` 的 PNG、manifest、`.complete` 三件套；因此没有可验证的 manifest hash、PNG hash 或 published complete 链。
- 当前工作树含禁止触碰的未跟踪 `.kotlin/`、`diagrams/`，为 DIRTY；可以运行普通 controlled evidence 并让 manifest 如实记录 `DIRTY:<sha256>`，但不得加 `formalEvidenceArchive=true` 或宣称 clean provenance。后续应在同 HEAD 的 clean checkout 以 `formalEvidenceArchive=true` 运行规定 instrumentation、拉取三件套并按本文件发布协议复核。
- 四个搜索 frame 继续是同一 `PlaceSearchContent` 的参数化状态，不新增 route；本次无归档不得把 controlled render 标为 production-real-trigger 或 physical `PASS`。

## 单设备 agent 命令

普通 controlled evidence（允许 manifest 记录 `DIRTY:<sha256>`，不作为 formal archive）：

```bash
export ANDROID_SERIAL=<approved-serial>
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchEvidenceTest
```

正式 clean archive（必须在同 HEAD clean checkout；显式开启 formal gate）：

```bash
export ANDROID_SERIAL=<approved-serial>
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchEvidenceTest \
  -Pandroid.testInstrumentationRunnerArguments.formalEvidenceArchive=true
```

产物目录：

```text
/sdcard/Android/data/com.yangchengwei.easytrip/files/evidence/batch-2/
```

拉取后必须先验证 `.complete` → manifest SHA-256，再验证 manifest → PNG SHA-256；同时保存 instrumentation 日志和外部 ADB 设备信息。允许差异仅限系统栏、字体栅格化、进度动画相位和像素密度取整。
