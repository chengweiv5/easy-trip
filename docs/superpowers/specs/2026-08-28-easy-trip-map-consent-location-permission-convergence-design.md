# Easy Trip 地图授权与定位权限收敛设计

## 1. 目标

本阶段收敛地图服务授权、Android 定位权限、系统设置返回恢复和无地图能力时的本地降级行为。完成后必须保证：

- 地图服务授权只有一个可观察、可持久化的权威状态源；
- 地图服务授权与 Android 运行时定位权限按真实平台语义分别建模；
- 定位权限只在用户主动点击定位后请求；
- 系统权限 launcher 确实发起后才记录“曾请求”；
- 永久拒绝后在地图区域提供页内说明和系统设置入口；
- 用户由该入口返回应用并授予权限后，自动复检并执行一次原待完成定位；
- 地图服务未授权、地图加载失败或定位权限缺失都不阻断 Room 旅行、地点池和行程；
- 用户撤回地图服务授权后立即停止地图、路线和远程地点搜索能力。

## 2. 范围

### 2.1 包含

- 统一高德地图服务授权的持久化决定、运行时 token 和可观察状态；
- 删除未接入生产流程的通用地图服务授权权限分支；
- 收敛定位权限用途说明、系统 launcher、拒绝分类和系统设置返回恢复；
- 将“曾请求”收敛为单一持久化来源；
- 工作台地图、地点搜索和路线能力统一消费地图授权状态；
- 增加地图授权降级、定位永久拒绝和地图加载失败的独立恢复入口；
- 补齐 JVM、Compose、instrumentation 和候选验收证据。

### 2.2 不包含

- 地图视觉重做；
- 395dp 到 396dp 的 Sheet 锚点连续性；
- 横屏、分屏、极窄屏和 2× 字体的完整响应式优化；
- 新增后台定位、持续定位或位置历史；
- 强制用户授予精确位置；
- 扩展路线规划领域模型；
- 复刻 Android 系统权限弹窗；
- 将地图服务授权、定位权限和未来相机等能力抽象成通用 permission engine。

## 3. 已确认产品规则

### 3.1 地图服务授权

- 地图服务授权是全应用决定，跨旅行、跨页面和冷启动保持；
- 同意与拒绝都持久化；
- 尚未决定时，首次进入旅行工作台自动展示一次应用内授权说明；
- 拒绝后不再自动打扰，只在地图降级区域和无授权搜索状态提供重新授权入口；
- 用户可重新打开同一说明并修改决定；
- 已加载地图时改为拒绝，立即停用地图、路线和远程地点搜索；
- 撤回授权不删除 SavedPlace、旅行、旅行日、行程项或 RouteLeg。

### 3.2 定位权限

- 仅在用户点击定位按钮时进入请求流程；
- 请求系统权限前先展示应用内用途说明；
- 用户确认说明后立即发起系统权限 launcher；
- 只有 launcher 确实发起后才持久化“曾请求”；
- 大致位置或精确位置任一获批即可执行定位，不追问精确位置；
- 普通拒绝后再次点击定位可重新说明并请求；
- 永久拒绝后在地图区域显示内联说明和“前往系统设置”；
- 不使用反复阻断用户的永久拒绝弹窗。

### 3.3 系统设置返回

- 只有用户从页内“前往系统设置”入口离开时，才创建一次性恢复请求；
- 应用返回 `RESUMED` 后重新读取系统权限；
- 已授权时清除权限提示并执行一次原待完成定位；
- 仍未授权时保持永久拒绝提示；
- 普通前后台切换不自动请求权限，也不自动定位；
- 用户已切换旅行、离开工作台或 request generation 失效时，不执行旧定位请求。

## 4. 当前问题

当前实现存在以下状态分裂和恢复缺口：

1. 地图授权同时由 `EasyTripApplication` 普通变量、SharedPreferences、进程级 token registry 和 Compose 本地弹层状态表达；
2. 另有一套未接生产流程的 `MAP_SERVICE_CONSENT` 通用权限模型，形成第二种授权语义；
3. 工作台与搜索对注入 token/source 的判断路径不完全一致；
4. 定位“曾请求”同时写入 SharedPreferences 和导航 `SavedStateHandle`；
5. “曾请求”在 launcher 调用前写入，launcher 未实际完成时可能被后续流程误判为永久拒绝；
6. 从系统设置返回后没有来源明确的自动复检与一次性定位恢复；
7. 搜索无地图授权时保留本地地点，但授权恢复入口表达不完整；
8. 真实系统 permission/rationale 和 OEM 行为缺少候选物理设备证据。

## 5. 架构

采用“分离两类授权、统一各自状态源”的方案。

### 5.1 地图服务授权状态源

引入单一权威 `AmapConsentStore`，对外发布可观察状态：

```text
Undecided
Accepted(activeToken)
Declined
```

职责：

- 从 `privacy` SharedPreferences 恢复决定；
- 持久化同意或拒绝；
- 调用高德隐私 shown/decision API；
- 同意成功后创建并发布有效 token；
- 拒绝或撤回后注销旧 token 并发布 `Declined`；
- 使用 generation 隔离旧回调、旧 token 和旧 SDK 会话；
- 在隐私 API 更新失败时保持旧授权事实。

`EasyTripApplication` 只负责创建和暴露 store，不再同时持有多组业务授权变量。现有 token registry 可保留为 store 内部 SDK 实现细节，但不得成为第二个业务状态源。

删除未接生产流程的 `PermissionKind.MAP_SERVICE_CONSENT` 分支。地图服务授权不通过 Android 定位权限 coordinator。

### 5.2 定位权限状态机

`LocationPermissionCoordinator` 继续保持纯逻辑边界，通过状态和 effect 协调：

- 用户点击定位；
- 用途说明展示、确认和取消；
- 系统 launcher 发起；
- launcher 结果；
- 普通拒绝与永久拒绝；
- 打开系统设置；
- 返回应用后的权限复检；
- 一次性定位恢复。

Coordinator 不直接持有 Android `Context`、Activity Result launcher 或生命周期对象。Route/Navigation 执行平台 effect，再将结果和最新 permission snapshot 送回 coordinator。

“曾请求”只保存在 `LocationPermissionRequestStore` 对应的 SharedPreferences 中。导航 `SavedStateHandle` 不再作为并行业务状态源。

### 5.3 UI 消费边界

- 工作台地图、地点搜索和路线协调器统一观察 `AmapConsentStore.state`；
- `Undecided` 只在首次进入工作台时触发自动说明；
- `Declined`、地图加载失败和定位永久拒绝是三种独立 UI 状态；
- 地图与远程搜索状态变化不能重建旅行 ViewModel、清空 Room 内容或重置工作台 Sheet/Tab；
- 本地点池和行程始终由 Room 数据源驱动，不依赖地图授权或定位权限。

## 6. 地图服务授权状态流

### 6.1 首次决定

首次进入旅行工作台时：

1. store 为 `Undecided`；
2. Route 为当前 workspace 创建一次自动说明展示资格；
3. 展示应用内地图服务授权说明；
4. 用户同意或拒绝后消费该资格；
5. 同一持久化决定下，重新组合、返回页面或冷启动都不再次自动展示。

### 6.2 同意

用户同意时：

1. store 开始新的 consent generation；
2. 调用隐私 shown/decision API；
3. 调用成功后持久化 `Accepted`；
4. 创建与当前 generation 绑定的有效 token；
5. 发布 `Accepted(token)`；
6. 工作台地图、路线和远程地点搜索统一恢复。

在 store 发布 `Accepted(token)` 前，调用方不得提前创建地图或路线 SDK 会话。

### 6.3 拒绝与撤回

用户拒绝或从已同意改为拒绝时：

1. 开始新的 consent generation；
2. 更新隐私 decision；
3. 成功后持久化 `Declined`；
4. 注销旧 token；
5. 发布 `Declined`；
6. 销毁当前地图 host 和路线 SDK 会话；
7. 取消或失效未完成的远程搜索；
8. 清除仅属于远程搜索的结果，不清本地 SavedPlace；
9. 显示地图与搜索降级入口。

旧地图回调、路线结果和搜索响应必须携带或等价校验 consent generation，不能在撤回或重新同意后覆盖当前状态。

### 6.4 修改决定

`Declined` 时：

- 不自动弹出授权说明；
- 地图降级区和搜索无授权状态均提供“查看并授权”；
- 点击后打开与首次相同的应用内说明；
- 同意后走完整新 generation，不复用已注销 token。

`Accepted` 时可通过同一说明查看授权信息并撤回；撤回立即生效。

## 7. 定位权限状态流

### 7.1 点击定位

用户点击定位后，Route 采样最新系统权限事实：

- 大致或精确位置任一已授权：发出一次定位 effect；
- 未授权且未曾请求：展示用途说明；
- 未授权、曾请求且系统允许再次弹窗：展示用途说明；
- 未授权、曾请求且系统不再显示权限弹窗：进入永久拒绝内联状态。

系统权限事实是完成判定的权威，“曾请求”只用于区分首次请求与可能的永久拒绝，不单独证明永久拒绝。

### 7.2 用途说明与 launcher

用户确认用途说明后：

1. coordinator 发出 `LaunchLocationPermissionRequest(requestGeneration)`；
2. Route 实际调用 Activity Result launcher；
3. 调用成功后回传 `PermissionLaunchStarted(requestGeneration)`；
4. coordinator 此时才写入 `hasRequested=true`；
5. launcher 无法发起时回传失败，不写“曾请求”，退出忙碌并允许重试。

用户取消用途说明时不调用 launcher，也不写“曾请求”。

### 7.3 权限结果

launcher 返回后，Route 重新采样 grant 和 rationale：

- 大致或精确位置任一获批：清除拒绝状态并发出一次定位 effect；
- 未获批且仍可再次请求：记录普通拒绝，回到非阻断地图状态；
- 未获批且已请求、系统不再显示 rationale：进入永久拒绝内联状态。

永久拒绝判断是基于最新系统事实和已实际发起过请求的组合，不从 launcher 返回 map 单独推导。

### 7.4 系统设置恢复

用户点击“前往系统设置”时：

1. coordinator 创建 `SettingsRecoveryRequest`，至少包含 workspace identity 和 generation；
2. Route 发起 app details settings Intent；
3. Intent 成功发起后标记等待返回；
4. 当前目的地恢复到 `RESUMED` 时只处理这条有效请求；
5. 重新采样权限；
6. 已授权时清除内联提示，消费 recovery request，并发出一次定位 effect；
7. 未授权时消费本次 resume 检查资格，但保持永久拒绝提示；
8. 连续 `ON_RESUME`、重组或旧生命周期回调不得重复定位。

离开 workspace、切换旅行或新定位 generation 取代旧请求时，旧 recovery request 失效。

## 8. UI 与降级

### 8.1 地图服务说明

说明必须明确：

- 地图服务用途；
- 隐私政策入口；
- 同意后启用地图、路线和在线地点搜索；
- 拒绝不影响本地旅行、地点池和行程。

说明是应用内 UI，不复刻 SDK 或 Android 系统弹窗。

### 8.2 地图未授权

地图区域显示非阻断降级面板：

- 标题：“地图服务未启用”；
- 说明地图、路线和在线地点搜索暂不可用；
- 说明本地地点与行程仍可使用；
- 主动作：“查看并授权”。

### 8.3 地图加载失败

地图加载失败与未授权分开表达：

- 显示“地图加载失败”；
- 提供“重试地图”；
- 重试只重建地图 host，不重置 workspace、Room 内容、Sheet 或 Tab；
- 地图失败不改写授权决定。

### 8.4 定位永久拒绝

地图服务可用但定位永久拒绝时，在地图区域内联显示：

- 标题：“定位权限未开启”；
- 简短用途说明；
- 动作：“前往系统设置”。

再次点击定位应聚焦或保持同一内联提示，不弹出重复阻断弹窗。

### 8.5 地图区域状态优先级

能力依赖从高到低为：

1. 地图服务未授权；
2. 地图加载失败；
3. 地图可用但定位永久拒绝；
4. 正常地图。

定位权限缺失不能覆盖地图服务未授权或地图加载失败的主恢复动作。

### 8.6 地点搜索降级

无地图授权时：

- 保留并展示本地 SavedPlace；
- 禁止远程地点搜索；
- 清除已失效的远程结果；
- 展示地图服务未授权说明和“查看并授权”；
- 不以空白 Spacer 代替恢复入口。

## 9. 错误与并发

### 9.1 地图授权更新失败

- 保持更新前的授权事实；
- 不发布虚假的 `Accepted` 或 `Declined`；
- 不提前创建或销毁 SDK 会话；
- 展示可重试错误；
- 重试创建新 generation。

### 9.2 定位平台动作失败

- launcher 无法发起：不写 `hasRequested`，退出忙碌并允许重试；
- 系统设置 Intent 无法打开：保留永久拒绝提示并展示失败反馈；
- permission callback、resume callback 和定位 effect 都校验 request generation 与 workspace identity；
- `CancellationException` 向上传播，不映射为业务失败。

### 9.3 授权变化隔离

以下旧结果不能覆盖新状态：

- 已撤回 token 对应的地图 host 回调；
- 旧路线计算结果；
- 旧远程地点搜索结果；
- 旧 permission launcher 回调；
- 已失效 workspace 的系统设置返回事件；
- 已消费 recovery request 的重复生命周期回调。

## 10. 组件职责

### 10.1 `AmapConsentStore`

负责：

- 持久化与恢复地图授权决定；
- 发布唯一可观察状态；
- 高德隐私 API 更新；
- token 创建、注销和 generation；
- 授权更新错误。

不负责：

- Compose 弹层显示；
- Android 定位权限；
- Room 数据；
- 地图 host 具体生命周期。

### 10.2 `LocationPermissionCoordinator`

负责：

- 纯权限流程状态；
- 用途说明、launcher、拒绝分类和 settings recovery generation；
- 产生平台 effect 和一次性定位 effect；
- 防止旧回调与重复恢复。

不负责：

- 直接调用 launcher；
- 直接打开系统设置；
- 直接读取 Android 权限；
- 地图服务授权。

### 10.3 Route / Navigation

负责：

- 采样真实 Android permission snapshot；
- 调用 Activity Result launcher；
- 在 launcher 调用成功后回传 started；
- 打开 app details settings；
- 观察目的地生命周期并回传有效 resume；
- 将地图授权说明动作发送给 store。

### 10.4 Workspace

负责：

- 消费地图授权、地图 host 和定位权限 UI 状态；
- 按优先级渲染地图区域；
- 执行一次性定位请求；
- 保持地点池、行程、Sheet 和 Tab 独立。

### 10.5 Place Search / Route

负责：

- 只在 `Accepted(activeToken)` 下创建远程数据源或 SDK 会话；
- 对授权 generation 隔离异步结果；
- 无授权时保持本地数据并提供恢复入口；
- 授权撤回时立即停止新请求并失效旧结果。

## 11. 测试设计

### 11.1 `AmapConsentStore` JVM

必须覆盖：

- 首次无记录为 `Undecided`；
- 同意跨 store 实例恢复；
- 拒绝跨 store 实例恢复；
- 同意成功后生成有效 token；
- 撤回后立即注销旧 token；
- 隐私 API 失败时保持旧事实；
- 旧 generation 完成不能覆盖新决定；
- 拒绝后不重新产生首次自动说明资格。

### 11.2 定位 coordinator JVM

必须覆盖：

- 点击定位后先展示用途说明；
- 取消说明不调用 launcher；
- launcher 实际 started 后才写 `hasRequested`；
- launcher 发起失败不写记录；
- 大致或精确位置任一获批即可定位；
- 普通拒绝允许再次说明和请求；
- 永久拒绝进入地图内联状态；
- 只有系统设置动作创建恢复请求；
- 返回并授权后只定位一次；
- 返回仍未授权时保持提示且不定位；
- 普通 resume 不定位；
- 离开 workspace、切换 trip 或 generation 失效后不定位；
- `CancellationException` 不转为业务错误。

### 11.3 Workspace / Search JVM

必须覆盖：

- 地图授权撤回后地图、路线和远程搜索失效；
- 旧 token/generation 的回调被丢弃；
- 无授权和地图失败都保留本地点池与行程；
- 无授权搜索保留 SavedPlace、清远程结果；
- 地图 retry 不重置 workspace 状态。

### 11.4 Compose / instrumentation

必须覆盖：

- 首次进入工作台自动显示地图授权说明；
- 拒绝后地图降级且地点池、行程可操作；
- 再次进入不自动弹出；
- 点击“查看并授权”重新打开说明；
- 同意后地图恢复；
- 已加载地图时撤回授权立即销毁并降级；
- 搜索无授权时保留 SavedPlace 并显示授权入口；
- 点击定位先显示用途说明；
- 永久拒绝显示地图区内联“前往系统设置”；
- 设置返回授权后自动定位一次；
- 连续 resume 不重复定位；
- 普通前后台切换不定位；
- 地图加载失败与未授权显示不同恢复动作。

真实系统权限弹窗不在 Compose 中伪造。测试使用可控 permission snapshot、launcher、settings intent、lifecycle 和 map host seam。

### 11.5 候选门禁

依次执行：

1. 地图授权与定位权限 JVM；
2. workspace/search 相关 JVM；
3. focused Compose；
4. 地图 host 生命周期与失败恢复 instrumentation；
5. `V1ScenarioCatalogTest`；
6. `V1FullUiAcceptanceTest`；
7. 全量 JVM；
8. `lintDebug`；
9. debug APK 与 androidTest APK 构建；
10. `git diff --check` 与 `graphify update .`。

Connected tests 在单个 emulator 上全局互斥并串行执行。基础设施异常最多重试一次，不将 runner crash、设备离线或 emulator 退出直接归因于产品代码。

## 12. 物理设备验收

候选版本冻结后，在物理设备正常竖屏验证：

1. 清晰可控的未决定状态下首次进入工作台，授权说明可达；
2. 拒绝后重新进入不自动打扰；
3. 地图降级时地点池和行程仍可操作；
4. 从降级区重新同意后地图恢复；
5. 撤回授权后地图、路线和远程搜索立即停用；
6. 点击定位先显示用途说明，再出现真实系统权限弹窗；
7. 仅授予大致位置时可定位；
8. 普通拒绝后可再次请求；
9. 永久拒绝后页内系统设置入口可达；
10. 从系统设置返回并授权后自动定位一次；
11. 再次 resume 不重复定位；
12. 地图加载失败重试不影响 Room 数据和工作台上下文。

OEM 对 `shouldShowRequestPermissionRationale`、大致/精确位置组合和 app details settings 返回行为以物理设备事实为准。测试不得清除用户现有旅行数据；需要初始权限状态时使用专用测试安装/fixture 或经明确批准的设备状态准备。

## 13. 完成标准

本批完成必须同时满足：

- 地图服务授权只有一个权威可观察状态源；
- 地图授权与 Android 定位权限分别建模；
- 同意和拒绝均跨重启保持；
- 拒绝后不会重复自动弹出；
- 撤回授权立即停止地图、路线和远程地点搜索；
- 定位“曾请求”只有一个持久化来源；
- launcher 未实际发起时不会写“曾请求”；
- 大致位置可用于定位；
- 永久拒绝使用地图区内联说明和系统设置入口；
- 设置返回恢复按来源执行且最多定位一次；
- 普通 resume 不自动定位；
- 无授权和地图失败不阻断 Room 地点池与行程；
- 自动化候选门禁通过；
- 物理设备关键路径没有功能、状态、数据、崩溃、严重裁切或关键交互阻断。

间距、字体和细小视觉差异不阻断本批，留到最终统一视觉验收。
