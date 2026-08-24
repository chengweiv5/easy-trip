# Batch 2 搜索加载与网络失败可控设备证据方案

## 结论

推荐以 **Compose instrumentation 的测试 Activity 直接渲染生产 `PlaceSearchContent`，并由测试持有可控 `PlaceSearchUiState`** 生成 `s1OvvX` 加载态和 `GJo79` 网络失败态截图；同时保留 reducer JVM 测试作为“状态确由 `PlaceSearchDataSource` 请求挂起/异常产生”的行为证据。两类证据必须分开命名，不能把可控状态渲染截图描述为生产真实网络旅程。

该方案不修改生产代码、不调用真实高德服务、不改变设备网络、不依赖请求时序；只需新增或扩展 `androidTest` 证据测试与证据导出逻辑。预计实现和首轮取证 1.5–2.5 小时，人工对照与归档 0.5–1 小时。

## 调查边界

- 只读检查 `design/easy-trip-v1.0.pen`，未读取或采用 v1.1。
- Pencil frame：
  - `GJo79`：`38 搜索地点 · 网络失败`，390 × 844。
  - `s1OvvX`：`44 搜索地点 · 加载中`，390 × 844。
- 未运行设备、模拟器、instrumentation 或截图命令。
- 未修改生产代码，未运行 `graphify update`。

## 设计基线

### `s1OvvX` 加载态

- 查询词为“西湖”。
- 42 px 主题绿进度指示器。
- 标题“正在搜索地点”。
- 说明“正在查找“西湖”相关结果…”。
- 页面背景 `#F5F3EE`，内容区白色，搜索框 48 高、2 px 主题绿边框。

生产映射已经直接存在：`PlaceSearchPhase.Loading` 渲染 `CircularProgressIndicator`、标题与 query 插值说明。加载态只在 300 ms debounce 结束且 datasource 已取得、`search()` 尚未返回时发布。

### `GJo79` 网络失败态

- 查询词为“西湖”。
- 48 px 红色断网图标。
- 标题“网络连接失败”。
- 说明“无法搜索新的地点。请检查网络连接后重试。”。
- 全宽 48 高“重新搜索”动作。

生产映射已经直接存在：`PlaceSearchPhase.NetworkFailure(message)` 渲染失败图标、标题、message 与 `Retry`。任意非取消异常会清空网络结果并进入该 phase；高德非成功码也会由 datasource 转为异常。

## 已有证据能力

1. `PlaceSearchContent` 是公开、纯状态驱动的生产 Composable；Route 仅收集 ViewModel 状态并转发 action，因此测试 Activity 可直接渲染生产 UI，不需要复制或伪造页面。
2. `PlaceSearchContentTest` 已使用 `createAndroidComposeRule<ComponentActivity>()`，并已直接覆盖 Loading、Empty、NetworkFailure；测试还使用 `captureToImage()` 验证生产 Surface 像素。
3. 失败态已有稳定语义节点：`place-search-network-failure-{body,icon,title,description,action}`。加载态目前可用文本定位；如仅取整页截图，不必增加生产 test tag。
4. `PlaceSearchReducerTest.loadingResultsEmptyAndNetworkFailureAreExclusive` 已用可控 `CompletableDeferred` fake datasource 证明：请求挂起时为 Loading，异常时为 NetworkFailure。
5. `PlaceSearchDataSource` 本身就是可注入边界，`PlaceSearchViewModel` 构造器接受该接口；无需新增生产 DI 或测试开关。

## 证据分类

### A. 生产真实触发

定义：从正式导航进入生产搜索 Route，由正式 ViewModel/Reducer 和 `AmapPlaceDataSource` 发起请求；失败来自真实 SDK/网络条件，加载来自真实请求尚未完成。

可证明：

- 生产装配链可达。
- 真实高德 SDK 异常被映射至失败态。
- 真实设备网络、权限、SDK 与 App 的端到端行为。

局限：

- 加载窗口可能短于截图延迟，不稳定。
- 飞行模式、Wi-Fi/蜂窝切换、`adb shell cmd connectivity`、代理或防火墙会改变设备全局状态，且不同 Android/厂商实现权限不一致。
- 断网可能命中 DNS、缓存、SDK 自身错误或超时，失败文案与到达时间不可控。
- 拒绝高德隐私授权会产生 `NetworkFailure` phase，但消息是“请先阅读并同意高德隐私政策”；它不是 `GJo79` 所表达的网络故障，不应作为该 frame 的网络失败证据。

结论：适合作为补充端到端证据，不适合作为本次两个确定视觉 frame 的唯一截图来源。

### B. 可控状态渲染证据

定义：在 instrumentation 的 `ComponentActivity` 中直接挂载生产 `PlaceSearchContent`，测试显式提供 Loading 或 NetworkFailure 状态并截图。

可证明：

- 指定状态在真实 Android 渲染管线、真实 Compose 主题、目标设备尺寸/密度/字体设置下的最终视觉。
- 文案、布局、颜色、图标、按钮和触控节点与 Pencil 的逐屏关系。
- 结果可重复、可审计，不依赖外部服务和偶然时序。

不能单独证明：

- 生产导航和真实高德请求一定到达该状态。

补强方式：与现有 reducer fake datasource 测试和 datasource 错误映射测试共同归档，形成“生产状态机行为证据 + 生产 Composable 设备渲染证据”，但仍明确标记其不是生产真实网络旅程。

## 推荐最小方案

### 方案结构

新增一个专用 instrumentation 证据类，或在 `PlaceSearchContentTest` 增加两个方法：

- `captureLoadingEvidence()`：渲染 `PlaceSearchState(query = "西湖", phase = Loading)`。
- `captureNetworkFailureEvidence()`：渲染 `PlaceSearchState(query = "西湖", phase = NetworkFailure("无法搜索新的地点。请检查网络连接后重试。"))`。

两者均调用生产 `EasyTripTheme` 和生产 `PlaceSearchContent`。测试先断言关键文字/动作存在，再对根 Activity 或 `place-search-surface` 截图。为了覆盖搜索框、背景和系统 Insets，最终证据应取 Activity 整屏；`captureToImage()` 可保留为局部像素/布局断言。

截图导出建议使用 instrumentation 进程写入 app-specific external files，例如：

```text
Android/data/com.yangchengwei.easytrip/files/evidence/batch-2/
  v1-38-GJo79-network-failure-controlled.png
  v1-44-s1OvvX-loading-controlled.png
  manifest.txt
```

`manifest.txt` 至少记录：Git SHA、测试类和方法、frame ID、设备 serial、AVD 名、分辨率、density、font scale、locale、截图文件 SHA-256、证据类型 `controlled-state-render`。拉取后再生成宿主机 SHA-256 清单并与截图一同归档。

### 为什么这是最小方案

- 不需要 production debug menu、BuildConfig 开关、自定义 runner、test manifest Activity 或新增 DI 框架。
- 不需要修改 `PlaceSearchContent` 的参数或状态模型。
- 不需要控制系统网络，因此不会干扰其他进程或污染后续测试。
- 已有测试栈、Activity rule、状态构造方式和 `captureToImage()` 能力可以直接复用。
- fake datasource 行为链已在 JVM reducer 测试存在；若希望同一次 instrumentation 同时证明状态来源，可在测试中构造 `PlaceSearchViewModel` 和挂起/失败 fake，但这不是首选最小截图路径。

## 命令

以下仅为执行方案，本次未运行。

### 1. 无设备的生产状态机回归

```bash
./gradlew testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.place.ui.PlaceSearchReducerTest' \
  --tests 'com.yangchengwei.easytrip.place.ui.PlaceSearchViewModelTest' \
  --tests 'com.yangchengwei.easytrip.place.amap.AmapPlaceDataSourceTest'
```

预期：证明 datasource 请求挂起后进入 Loading、异常后进入 NetworkFailure，且取消异常不被误判为网络失败。

### 2. 明确锁定获准 AVD

```bash
export ANDROID_SERIAL=<serial>
AVD_NAME="$(adb -s "$ANDROID_SERIAL" emu avd name | tr -d '\r')"
test "$AVD_NAME" = "easy_trip_p60pro"
adb -s "$ANDROID_SERIAL" shell wm size
adb -s "$ANDROID_SERIAL" shell wm density
adb -s "$ANDROID_SERIAL" shell settings get system font_scale
adb -s "$ANDROID_SERIAL" shell getprop persist.sys.locale
```

预期：只在指定 `easy_trip_p60pro` 上取证，并把环境值写入 manifest。任何名称不匹配都应立即停止。

### 3. 运行专用可控证据测试

若新增独立类：

```bash
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchEvidenceTest
```

若扩展现有类，建议用方法级过滤分别运行，避免其他测试改写证据目录：

```bash
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchContentTest#captureLoadingEvidence

ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchContentTest#captureNetworkFailureEvidence
```

预期：两项均通过，设备目录生成两张 PNG 和 manifest；测试日志包含 frame ID 与输出路径。

### 4. 拉取并校验

```bash
mkdir -p evidence/batch-2
adb -s "$ANDROID_SERIAL" pull \
  /sdcard/Android/data/com.yangchengwei.easytrip/files/evidence/batch-2/. \
  evidence/batch-2/
shasum -a 256 evidence/batch-2/*
```

Android 版本若限制直接访问该路径，应由测试写入可访问媒体目录，或使用 `adb exec-out run-as com.yangchengwei.easytrip` 从 app internal files 导出；不要为了取证给生产 App 增加存储权限。

### 5. 视觉对照

将两张设备图分别只与对应 Pencil frame 对照：

- `v1-38-GJo79-network-failure-controlled.png` ↔ `GJo79`。
- `v1-44-s1OvvX-loading-controlled.png` ↔ `s1OvvX`。

允许差异仅包括系统状态栏、系统字体栅格化、Compose 圆形进度指示器动画帧和设备像素密度取整。查询词、标题、说明、按钮、主体间距、背景/Surface 色和搜索框外观不属于允许差异。

## 备选方案评估

| 方案 | 证据类型 | 生产代码改动 | 稳定性 | 评价 |
|---|---|---:|---:|---|
| 测试 Activity 直接渲染生产 Content + 显式 state | 可控状态渲染 | 无 | 高 | 推荐最小方案 |
| instrumentation 构造生产 ViewModel + 挂起/异常 fake datasource | 可控行为链与渲染 | 无 | 高 | 更强但 setup 较多；用于审计要求更严时 |
| 自定义 test Application/runner 替换 App graph datasource | 接近生产导航的可控旅程 | test-only，通常还需装配改造 | 中 | 当前无 DI 框架，不值得为两个 frame 引入 |
| OkHttp MockWebServer/代理 | 可控网络 | 需生产网络栈可指向代理 | 低 | 高德 SDK 内部请求不受 App 自有 HTTP client 控制，不适用 |
| 模拟器飞行模式/禁网/网络整形 | 生产真实触发 | 无 | 低 | 可补充失败 E2E；无法稳定捕获 Loading，且污染设备全局状态 |
| 拒绝高德隐私授权 | 生产路径 | 无 | 高 | 语义是 consent failure，不是网络失败，禁止冒充 `GJo79` |
| 在生产加入 debug fixture/menu/延迟开关 | 可控生产外壳 | 有 | 高 | 破坏生产表面积，超出最小证据需求，不推荐 |

## 是否需要代码改动

- **生产代码：不需要。** 现有 `PlaceSearchContent(state, onAction)` 和 `PlaceSearchDataSource` 注入边界已足够。
- **测试代码：需要小改。** 增加两个 instrumentation 证据测试，以及一个 test-only PNG/manifest 写出 helper。预计 50–100 行，取决于是否复用已有截图工具。
- **构建配置：通常不需要。** 现有 `androidx.compose.ui.test.junit4` 与 debug test manifest 已配置。只有选择额外截图库时才需依赖变化；最小方案不应新增依赖。
- **文档/证据索引：需要。** 在 Batch 2 scenario matrix 或测试记录中登记受控证据性质、文件 hash 和允许差异。

## 对生产语义的保证

1. 截图调用的是生产 `PlaceSearchContent`，不是测试重写的页面。
2. 状态对象使用生产 `PlaceSearchUiState`、`PlaceSearchState` 和 `PlaceSearchPhase`，不会形成第二套测试模型。
3. 不向生产 source set 加 fixture、开关、延迟或 fake，不改变 release/debug 行为。
4. reducer 测试通过真实生产 reducer 与 fake `PlaceSearchDataSource` 证明状态转换；截图测试只负责证明对应状态的设备渲染。
5. 证据标签明确写 `controlled-state-render`，不声称走过正式导航、真实网络或高德服务。
6. 若另做真实断网验证，必须独立标记 `production-real-trigger`，记录网络操作、SDK 错误、开始/结束时间及恢复网络步骤，不与受控截图混用。

## 预期证据包

最小可审计包：

- `v1-38-GJo79-network-failure-controlled.png`。
- `v1-44-s1OvvX-loading-controlled.png`。
- 两个 instrumentation 测试的通过日志。
- `PlaceSearchReducerTest`、`PlaceSearchViewModelTest`、`AmapPlaceDataSourceTest` 的通过日志。
- 环境与 SHA-256 manifest。
- 两行 scenario matrix：frame、fixture、测试方法、设备参数、Pencil/设备图路径、允许差异、结论。

建议可选补充：一次真实生产断网失败录像或截图，仅用于证明生产装配链；不要求与 `GJo79` 文案完全一致，也不承担加载态证据。

## 时间估计

| 工作 | 估计 |
|---|---:|
| 新增 test-only 证据方法和写出 helper | 45–75 分钟 |
| 本地编译/JVM 回归 | 15–30 分钟 |
| 指定 AVD 两次 instrumentation 与拉取校验 | 20–40 分钟 |
| Pencil 并排对照、manifest/scenario matrix 归档 | 30–60 分钟 |
| 合计 | 1.5–2.5 小时；含人工归档最多约 3 小时 |

若增加“生产 ViewModel + fake datasource”的同进程链路测试，再加 30–60 分钟；若要求真实网络断开端到端证据，再加 1–2 小时且仍存在 SDK 超时与设备网络控制的不确定性。

## 风险与控制

1. **截图 API 仅捕获 Compose 节点而非整屏。** 控制：局部 `captureToImage()` 做断言，最终证据通过 Activity/window 或 instrumentation screenshot 获取；记录是否包含系统栏。
2. **进度指示器动画导致像素差异。** 控制：比较结构、颜色、尺寸和位置，不做整图逐像素门禁；允许动画相位差。
3. **设备目录不可直接 pull。** 控制：优先 app-specific external files；受限时使用 `run-as`，不增加生产存储权限。
4. **受控 state 被误报为真实旅程。** 控制：文件名、manifest、scenario matrix 都强制写 `controlled`；真实触发另列。
5. **测试 state 与 reducer 漂移。** 控制：同一证据包必须包含 reducer/source 回归；若 phase 或文案模型改变，截图测试和行为测试一起失败/更新。
6. **Pencil 与 Compose 系统 Insets 不同。** 控制：忽略系统栏差异，只比较 App 内容区域；记录设备尺寸、density、font scale。
7. **真实网络控制影响当前设备其他任务。** 控制：本推荐方案不修改设备网络；若做可选真实断网验证，必须使用专用 AVD、先记录原状态、结束后恢复并验证。

## 最终建议

先实施两张 **可控状态渲染证据**，与既有 reducer/fake datasource 测试组成 Batch 2 的最低风险审计闭环。若门禁额外要求“正式导航 + 真实 SDK”证明，再单独追加一次专用 AVD 的真实断网失败验证；不要用拒绝隐私授权替代网络失败，也不要为捕获瞬时 Loading 向生产代码加入延迟或 fixture。