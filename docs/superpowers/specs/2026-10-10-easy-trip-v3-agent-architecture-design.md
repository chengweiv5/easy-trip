# Easy Trip v3.0 Agent 技术架构

状态：**本版架构设计已收敛，待评审；未批准实施，未实现。** 2026-10-10 更新 Runtime 选型、Provider 直连和 DeepSeek 图片/工具协议；本版替代同日早先的“必须自建网关”草案。

本稿是 [v3.0 统一规划](2026-10-10-easy-trip-v3-agent-place-intake-design.md)的技术细化，不是第二条版本规划。代码核对基线：`bbdcb1f57a47eac8ae13c8a7423fab97632699bb`（本轮获取的最新 `origin/main`，v2.2.0 / schema 10），当前工作树 `/Users/bytedance/.codex/worktrees/5ac3/easy-trip`。本稿确定这一版的技术设计取舍，供用户评审；设计完成不代表实现获批或能力已接通。输入/确认 UI 和实施计划仍需各自评审。

## 1. 总体决策

**本版选择：原生 Kotlin 手机端编排与执行、直连模型 Provider、复用现有业务能力、本地数据唯一。**

- **Runtime**：Kotlin + Coroutines + Flow/StateFlow，Room 持久化；OkHttp 负责 HTTPS/SSE，kotlinx.serialization 负责 JSON。首期自建有界工具循环，不在手机部署 Python/Node，也不引入重型 Agent 框架。
- **模型**：通过 `ModelPort → ProviderAdapter` 直连；首个适配器选 DeepSeek Chat Completions，模型 ID `deepseek-flash`。只需配置 provider、Base URL、用户自己的 API key 和 model；**不要求用户提供或我们先搭建云端网关**。
- **网关**：未来需要统一密钥、账号、额度或审计时再接入，作为可选 Provider 路由，不进入首期必需链路。
- **图片**：官方文档确认 DeepSeek Flash 支持图片理解和工具调用；首期设计允许截图直接输入，OCR 只作可选降级。2026-10-10 已用用户配置的官方 endpoint/key 实测文本和合成截图各一条工具闭环（4 次请求、thinking 关闭、非流式）；准确率、真实 SSE 和 Android 接入仍未验收。详见[能力调研](../../analysis/2026-10-10-deepseek-flash-capabilities.md)。

Agent 是 App 的第二个操作入口，不是一个通过屏幕点击 App 的机器人，也不是一套独立的旅行数据库。它接收素材、理解指令、请求查询、整理候选和解释结果；正式操作由手机上的受信任业务代码执行。

| 方案 | 优点 | 代价与判断 |
| --- | --- | --- |
| 手机 Kotlin Runtime + Provider 直连 | 直接复用本地地点池与高德会话；确认、事务和恢复在手机内闭环 | 本版选择；接受首期以前台任务为主，不承诺离开 App 后一直运行 |
| 云端 Agent 编排 + 手机工具执行 | 后续适合跨设备和后台任务 | 首期不选；会额外引入设备通道、断连、配对、状态同步和远程写入授权 |
| 外部 Agent / 模拟点击 App | 可沿用外部助手入口 | 首期不选；本应用可提供类型化业务接口，无需将界面控件当作执行协议 |

本次推荐单个 Agent Runtime，不引入多 Agent、向量库、跨旅行长期记忆或独立业务微服务。先让逻辑模块在现有 `:app` 内清晰分层，是否拆 Gradle module 留待依赖和测试规模证明必要。

## 2. 组件图

- [交互式架构图](assets/easy-trip-v3-agent/architecture.html)：可切换明暗主题并导出 SVG。
- [可编辑结构源](assets/easy-trip-v3-agent/architecture.architecture.json)：使用 Archify 重新生成，非 UI 设计稿。

图中的箭头表示调用/数据方向，不是每条网络请求；模型响应和工具结果沿原调用链返回。实线模型主路径为 Runtime → 手机内 ProviderAdapter → DeepSeek；标注“可选”的紫色虚线路径才经过网关；另一路紫色虚线表示任务持久化，红色虚线表示本地确认门控。“任务、草稿、回执”和正式旅行数据属于**同一个 Room 数据库内的不同职责记录**，不是两套互相同步的数据库。

## 3. 当前 App 可以复用什么

以下为上述 main 基线源码实读（rebase 后复核），不是本轮运行验收：

| 现状 | 证据 | 接入方式 |
| --- | --- | --- |
| 单一 Android `:app`，Compose UI + ViewModel | `settings.gradle.kts`；`app/build.gradle.kts` | 新增 Agent 页面/面板及 ViewModel，保留已有页面 |
| `AppContainer` 组装业务 Repository 与 Room | `app/src/main/java/com/yangchengwei/easytrip/AppContainer.kt:90-148` | 新增 `AgentContainer` 组装运行时、模型端口和任务存储，注入既有能力 |
| 高德对象通过已同意的 RuntimeSession 创建 | 同文件 `126-146`；[ADR 0001](../../adr/0001-amap-sdk-integration.md) | 用会话提供器按需获取地图查询工具；不由 Agent 启动流程自动同意隐私协议 |
| 收藏在事务内校验坐标，并按旅行 + POI 去重 | `app/src/main/java/com/yangchengwei/easytrip/place/data/RoomSavedPlaceRepository.kt:52-59`；`PlaceEntities.kt:4` | 复用现有业务约束，补充批量导入及回执事务，不另写一套收藏规则 |
| 收藏、分类快捷更新和备注/标签更新是分开的操作 | 同 Repository `28-30`、`52-59`、`69` 起 | 新增一致的提交单元；不能先 save、再 updateDetails、最后写回执来冒充原子提交 |
| 地点池/地图工作台订阅地点 Flow | `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceViewModel.kt:180-207` | 保存后由已有数据流刷新，不让 Agent 自己往地图画一份“已收藏”标记 |
| 搜索的城市参数只是提示，`cityLimit=false` | `app/src/main/java/com/yangchengwei/easytrip/place/amap/AmapPlaceDataSource.kt:64-73` | 新匹配层必须校验目标城市和同名歧义，不能把第一条搜索结果直接当答案 |
| 数据库 schema 为 10，已含地点分类，当前无 Agent 任务表 | `app/src/main/java/com/yangchengwei/easytrip/core/database/EasyTripDatabase.kt:30-35` | 设计增量迁移并保留旧数据；实施时重新确认下一可用 schema 版本 |
| 主 Activity 只有启动 intent，未接收分享 | `app/src/main/AndroidManifest.xml` | 首期输入适配器与后续分享接收分离，不假定现有分享导出等于分享导入 |

## 4. 新增的六组职责

### 4.1 Agent 交互层

负责输入文字/图片、显示任务进度、选择目标旅行、候选消歧、确认和结果回执。

`AgentViewModel` 只调用 `start / observe / answer / confirm / cancel` 等应用接口，不直接调用模型厂商 SDK、地图 SDK 或 DAO。现有触屏页面不依赖 AgentRuntime；模型不可用不应增加原手动操作的依赖。

### 4.2 Agent Runtime：原生 Kotlin 有界循环

| 职责 | 本版选型 | 设计约束 |
| --- | --- | --- |
| 任务编排 | Kotlin Coroutines、结构化并发 | 每个 run 有明确 Job；取消、超时和预算可追踪，不用全局无限循环 |
| 状态输出 | Flow / StateFlow + ViewModel | UI 只订阅状态，网络断开不阻塞现有手动入口 |
| 任务恢复 | Room 任务/草稿/回执 | ViewModel 不是恢复依据；重启从持久化状态和真实回执恢复 |
| 模型连接 | OkHttp HTTPS + SSE | 关闭响应体、取消网络 call；只在完整工具事件后执行 |
| 类型与协议 | kotlinx.serialization + 明确的 schema 校验 | JSON 解析成功不等于业务有效或已授权 |

选择轻量自建循环而不是首期采用 Koog/其他 Agent 框架，是因为当前只有有限工具、单任务状态机与本地事务门控；框架不能替代这些业务约束。Koog 仅保留为后续评估项，不是依赖或已验证兼容选型。实施时锁定并验证 OkHttp/serialization 版本与现有 Android minSdk、Kotlin/AGP 的兼容性，本轮不安装依赖。

负责一轮任务的上下文、模型轮次、工具调度、取消、超时和用量预算：

1. 从指定旅行读取最小必要上下文，预处理用户主动提供的素材。
2. 通过 `ModelPort` 请求一次推理。
3. 模型返回文字、结构化候选或工具请求；Runtime 只接受完整、通过 schema 校验的请求。
4. 调用工具适配器，将结果回传模型；需要用户处理时暂停。
5. 进入待确认状态，或基于真实回执完成结果说明。

上下文构建器仅选择当前旅行、相关收藏和必要素材，不默认发送全部旅行、费用或聊天历史。为模型轮次、工具调用数、总耗时、图片大小和输出长度设置可配置上限；达到上限就保留草稿并返回可解释状态，不无限重试。

流式文本只用于展示。不得在工具参数流尚未结束时执行操作。**原始 SSE 分片不能按 `toolCallId` 或 completion `id` 去重**：同一调用的参数分布在多个分片，首片建立 id/name，后续参数按 `tool_calls[index]` 累加；completion id 也可能在同一响应中重复。

先由 ProviderAdapter 按当前 `attemptId + choiceIndex + toolIndex` 拼接，检查成功终止语义、完整 JSON 和 schema，再产生 `ToolCallComplete`。本版保守要求收到合法成功 `finish_reason` 和 `[DONE]` 才允许工具调度；单有 `[DONE]` 不代表成功，`length / content_filter / insufficient_system_resource / aborted` 或流断开均不可执行半成品。usage-only 事件可没有 choices，不能被误判为工具或错误。工具参数不同但调用 ID 相同应拒绝，不能静默复用旧结果。

只有**已完成且通过校验的工具请求**在 Runtime 内按 `runId + modelTurnId + attemptId + toolCallId` 去重并比较规范化参数摘要。网络重试创建新 attempt；不将新旧响应分片拼接，不假定模型会复用 toolCallId，不自动续传无序号的 SSE。模型调用可能重发、重复计费，这层去重不代替 `operationId` 业务幂等，也不代表云端恰好调用一次。

2026-10-10 离线协议探针已复现错误去重会截断参数，并验证上述拼接/终止处理；这是合成协议测试，不是 Kotlin 适配器或真实 DeepSeek 联调。详见[协议验证记录](../../testing/v3.0-agent-provider-protocol-validation.md)。

### 4.3 工具适配与授权层

将 App 能力包装成有限、类型化的工具。模型只能引用本地生成的对象标识和证据，不能制造可信 POI 坐标、旅行身份或授权。

| 首期工具/动作 | 调用主体 | 约束 |
| --- | --- | --- |
| `read_trip_context` | 模型经 Runtime 请求 | 绑定当前 tripId；白名单字段、数量上限 |
| `search_places` | 模型经 Runtime 请求 | 通过既有高德隐私门控；返回 POI 证据引用、名称、地址和城市 |
| `update_import_draft` | 模型经 Runtime 请求 | 只修改草稿；候选必须关联素材与查询证据；每次修改推进 draftRevision |
| `request_user_input` | 模型经 Runtime 请求 | 展示歧义项或缺失信息，不自行选择收藏 |
| `confirm_import` | 用户界面发出可信事件 | 本地确认绑定 tripId、draftId、revision、选中项、备注/标签/分类和内容摘要 |
| `commit_import` | 本地 ImportCoordinator | 首期不暴露为模型可自行调用的写工具；校验确认记录后调用共享业务能力 |
| `undo_import` | 用户显式操作 | 只撤销回执对应的新建项；必须通过版本和关联使用检查 |

**本版将首期所有正式收藏收敛为结构化确认后提交。** 即使用户输入“帮我收藏”，也先生成可核对的草稿；具体确认 UI 待 Pencil 评审，不开放无确认写入。后续即使支持直接指令，授权也必须来自可信本地策略，不能相信模型说“用户已确认”。

确认记录在手机内部产生，绑定具体草稿与 `operationId`，不发送给模型。草稿/目标/选中项变更使旧确认失效；重启后提交前重新校验状态，必要时再次确认。素材正文、网页、地点描述中的指令只能作为数据，不提升工具权限。首期没有任意 SQL、Shell、通用 HTTP/任意 URL 读取工具。

### 4.4 共享业务能力：不是“给每个按钮套一层”

建议以 `TripCapabilities` 作为职责总称，按领域拆成 `PlaceQuery`、`PlaceImport` 等小接口，而不是建立包含所有操作的巨型类。Agent 工具只做参数适配与受控调用，业务校验仍属于这些接口的实现。

- `PlaceQuery`：搜索、读取已收藏地点、按目标城市核对候选。
- `PlaceImport`：校验草稿、确认后的事务提交、查询回执、受限撤销。
- 后续 `ItineraryEdit`：读取日程、预览变更、提交和撤销；复用原 v4 ChangeSet 思路，首期不实现。

触屏收藏与 Agent 收藏使用同一组地点校验和写入原语；Agent 专属的任务状态、模型上下文不进入既有领域 Repository。只迁移首期需要的操作，不为接入 Agent 重构所有现有页面。

建议契约形状：

```text
PlaceQuery.search(query, cityScope) -> List<PoiEvidence>
PlaceImport.preview(draftId, draftRevision, selectedIds) -> ImportPreview
PlaceImport.commit(confirmedOperationId) -> ImportReceipt
PlaceImport.receipt(operationId) -> ImportReceipt?
PlaceImport.undo(receiptId, userConfirmation) -> UndoReceipt
```

`commit` 内部加载可信确认记录与草稿，不接受模型直接传入的任意实体/坐标作为最终写入依据。查询证据包含供应商、POI ID、坐标系、坐标、城市、地址和查询时间；沿用当前高德坐标语义，不从截图上的位置估算正式坐标。

### 4.5 任务与数据存储

同一个 `EasyTripDatabase` 新增辅助记录，继续让 Room 正式数据驱动界面。

| 记录 | 主要内容 | 是否正式地点 |
| --- | --- | --- |
| `AgentRun / RunEvent` | runId、目标旅行、状态、预算、请求关联、恢复点 | 否 |
| `SourceEvidence / AttachmentRef` | 用户输入片段、来源链接、私有文件引用、内容哈希 | 否 |
| `ImportDraft / DraftItem` | 原名称、候选证据、用户选择、备注/标签/分类、草稿版本 | 否 |
| `ConfirmedOperation / ImportReceipt` | 确认范围、operationId、摘要、逐项结果、撤销依据 | 否 |
| 现有 `saved_places`（含 category）/ `tags` + 来源关联 | 用户确认后的真实收藏 | 是，地图与地点池只从这里展示正式结果 |

草稿和未匹配项不会伪装成已收藏。来源采用关联记录，保留短片段/链接即可，不默认永久保存完整攻略或视频。图片保存在应用私有目录；接收时校验类型、大小和数量，先复制/验证可读权限再确认接收成功。发送云端前裁剪/压缩并去掉不必要元数据，显式说明发送内容；关闭云端识别仍可保留手动录入。

原图留存期限、用户“保留原素材”选项及历史任务清理周期需产品/隐私评审；自动清理不得删除尚待恢复任务的必需素材或仍用于幂等防重的操作记录。

### 4.6 ModelPort、Provider 直连与可选网关

#### 配置和边界

```text
Android App
  AgentRuntime → ModelPort → DeepSeekProviderAdapter → HTTPS → 用户配置的 DeepSeek endpoint
                              ↑
                     ProviderConfig + CredentialStore

可选替代路由（未来）：兼容网关 Adapter → 自有模型网关 → 模型 Provider
```

`ProviderConfig` 包含 `providerType`、`baseUrl`、`modelId`、能力声明与 thinking 设置；API key 通过 `credentialRef` 引用，不进入普通配置导出或模型上下文。第一版仅承诺 DeepSeek 适配器，不把“OpenAI-compatible”视为所有供应商完全同构的保证。若用户的 endpoint 是第三方兼容服务，须单独验证模型存在性、图片、tools、SSE、思考字段及错误格式。

官方调用示例的 Base URL 为 `https://api.deepseek.com`；具体请求由适配器形成 Chat Completions 路径。配置校验应识别 base URL 与完整 completion URL 的误填，避免重复追加路径。连接检查由用户显式触发，标明是否会发送测试文本/图片并产生费用；不在每次打开设置时自动执行有计费的推理。

#### 手机端 key 管理

个人自用/BYOK 模式由用户在设备上填写自己的 key。使用 Android Keystore 管理加密密钥，再加密保存 provider key；**不能把 provider key 本身硬编码进 APK**，也不写入 Room 业务日志、崩溃报告、备份或导出。密文和 Keystore 绑定，换设备/密钥失效后提示重新配置。网络请求时 key 仍会短暂出现在进程内存；不承诺能抵抗已被控制的设备。

只通过 HTTPS 向用户明确配置的 endpoint 发送凭据；校验 URL，禁止把 Authorization 转发到任意重定向目标。修改 endpoint 后要求重新确认凭据用途，不把旧 key 自动发送到新主机。工具结果和素材中的 URL 不能改变 provider 配置。清除凭据或撤回云端授权后停止后续调用，保留可继续手动处理的草稿。

多用户公开发行若由开发者统一付费，不能把共享厂商 key 放进 APK；届时另行建设可选网关的用户/设备鉴权、额度、密钥轮换和滥用保护。网关始终没有手机业务写入权，也不是旅行数据库。用户自带 key 的直连路径并不以该服务为前置条件。

#### 模型协议与 DeepSeek 首期配置

`ModelPort` 暴露版本化请求与 `TextDelta / ToolCallComplete / Usage / Error / TurnComplete` 等领域无关事件。ProviderAdapter 负责厂商 JSON、SSE 拼接、错误分类和能力探测；工具调用包含稳定 `toolCallId`、工具名及完整参数。多个工具可由 Runtime 有界串行调度，不因模型并行提议而并发写入业务库。

| 项目 | 本版设计 | 核验与边界 |
| --- | --- | --- |
| 模型 | `deepseek-flash` | 官方当前能力表映射 DeepSeek-V4.1-Flash；不把第三方同名 endpoint 当作已验证 |
| 图片 | user 消息内容块，文字 + `image_url`；首期优先裁剪后的 Base64 data URL | 官方支持 JPEG/PNG/GIF/WebP；应用应设更低的尺寸/数量/总字节预算；不上传整库或自动抓取网页图片 |
| 工具 | `tools` + assistant `tool_calls`；执行后用 `role=tool`、对应 `tool_call_id` 回传文本/JSON结果 | 参数先解析/schema 校验，再做 trip/POI/授权校验；模型不执行本地函数 |
| 思考模式 | 简单地点录入初始配置显式 `thinking.type=disabled`，`tool_choice=auto` | 官方默认开启思考，不能靠默认值推测；以后可单独启用 |
| 思考 + 工具 | 启用时完整保留并按协议回传 assistant 的 `reasoning_content`、`content` 与 `tool_calls` | thinking 模式不支持 `required` / 强制指定工具；不能归一化时丢弃厂商续接字段 |
| 协议差异 | 首期只在 user 消息放图，工具回传文本/JSON | 图片指南与 tool 消息 schema 口径差异另测，不依赖工具图片回传 |

厂商续接字段是**不透明的敏感协议状态**，不能变成授权依据、业务字段或用户可见的“解释”。运行内保留，确需恢复时加密保存最小字段并执行清理策略；无法安全恢复完整协议时从已验证草稿开启新模型会话，不重放写操作、不伪造推理字段。日志只记录关联 ID、状态、耗时和脱敏错误。

官方文档支持不等于 App 已接通。2026-10-10 23:13（Asia/Shanghai）已完成独立探针的真实基础 smoke：“文本 → tool_calls → Fake 工具结果 → 最终输出”和“截图 + tools”各一条，4/4 HTTP 200；严格检查工具参数、tool_call_id、工具独有随机码及尚未收藏状态。使用官方 endpoint、`deepseek-flash`、thinking disabled、非流式；无真实地图/Room 写入，详见[协议验证记录](../../testing/v3.0-agent-provider-protocol-validation.md)。这支持首期 Provider 直连选型，但不证明普遍准确率或手机接入。thinking 开启、真实 SSE/中断、无效 JSON、鉴权/限流/超时和取消仍需后续验证；费用未查询账单，耗时只代表本轮样本。供应商留存、地域和服务条款仍需核对；“本地不留正文”不代表供应商不留存。

## 5. 一次“把这些地方收藏起来”的完整调用

1. 用户配置 provider/key、同意本任务必要云端发送范围，在目标旅行交付文字/截图，创建本地 `runId` 与素材引用。
2. Runtime 读取必要旅行信息，模型提取地点名称及其来源片段。
3. Runtime 根据模型请求，经 `PlaceQuery` 查询高德，保存可信 POI 证据。
4. 匹配层检查城市、同名、重复和坐标；模型可以辅助解释，但置信分数不能单独决定地点身份。
5. 草稿卡片展示名称、地址、地图定位、来源、备注/标签/分类及“待澄清 / 已收藏 / 可新增”；用户修改或勾选。
6. 用户点击确认，本地建立绑定草稿版本的 `operationId` 与确认记录。
7. `PlaceImport.commit` 在同一 Room 事务中校验并写入地点、元数据、来源关联和逐项回执。
8. 原有 Repository Flow 刷新地点池与地图。结果 UI 读取回执，不依赖模型的一句“完成了”；模型总结失败不影响真实保存状态。
9. 用户撤销时调用受限逆操作；取消 Agent 对话本身不等于撤销已提交收藏。

### 本版首期批量语义

用户可以只选择已消歧的部分提交，其余留在草稿。**对本次确认选中的集合采用一个事务：新建项全部成功或全部回滚；已存在项记为 `ALREADY_SAVED`，不修改原备注、标签、分类或来源。**

数据库异常或任一选中项的校验失败时，不产生部分新增；回执准确区分已提交新建、原有重复、未选/待处理和本次失败。不要把“事务外查询部分失败”和“事务内部分落库”混为一谈。后续若需要逐项事务和部分成功，必须显式重新设计 UI 与回执语义。

## 6. 幂等、并发、撤销与生命周期

- **幂等**：operationId 唯一并绑定确认内容摘要；同 ID 同摘要返回原回执，同 ID 异摘要拒绝。双击确认复用待处理操作；用户重新发起的导入使用新 ID。回执与新建地点在同一个事务里提交。旧操作已撤销后重试仍返回其历史状态，不重新创建。
- **确认失效**：提交前重新检查目标旅行存在、草稿版本、候选证据及当前重复项。不能凭旧预览覆盖后来编辑的地点。
- **撤销版本**：建议为 saved place 增加 `editRevision` 并在所有相关写入路径推进，包括手动备注/标签/分类及城市补全；回执记录新建后版本。撤销要求版本未变、无行程/费用等关联，检查与删除在同一事务。任何一项冲突则整次撤销不落库并解释原因。
- **覆盖全部编辑入口**：地点分类快捷操作 `updateCategory`、`updateDetails`、备注/标签及城市补全等语义编辑均需推进地点 `editRevision`；仅 Agent 路径递增不足以保护撤销。重复项保留原分类和元数据；新建项沿用现有分类默认值，用户明确选定的分类才进入确认摘要。
- **不用现有级联删除作撤销**：`deletePlaceAndReferences` 会处理关联数据，语义过宽。导入撤销必须使用专用受限操作，只删除该回执真正创建的地点；原本已收藏的重复项不在撤销集合中。
- **恢复**：任务状态建议为 `RUNNING / WAITING_INPUT / WAITING_CONFIRMATION / COMMITTING / PAUSED / COMPLETED / FAILED / CANCELLED`。进程重启发现 `COMMITTING`，先查询 operationId 回执；不存在时按草稿和授权重新校验，不直接重放模型请求或自动写入。
- **取消**：停止尚未发生的工具调用；事务已提交就展示回执和撤销入口，不声称取消使已保存数据消失。
- **前后台**：首期以前台交互为主，任务状态持久化；切后台不承诺持续推理，恢复后显示真实状态。适合延后执行的清理/恢复检查才考虑 WorkManager，不把它当作永久运行 Agent 的守护进程。
- **串行写入**：同一旅行的 Agent 提交串行化，Room 事务和数据库约束仍是最终防线；手动写入也遵守共享校验和版本递增，不能只依赖 Agent 内部互斥锁。

## 7. 代码组织建议

在当前 `app/src/main/java/com/yangchengwei/easytrip/` 下按职责新增：

```text
agent/
  ui/          AgentViewModel、任务/候选/确认/结果状态
  runtime/     AgentRuntime、ContextBuilder、预算与恢复
  model/       ModelPort、统一事件、ProviderAdapter、DeepSeekProviderAdapter
  config/      ProviderConfig、CredentialStore、Keystore 加密实现
  tools/       ToolRegistry、schema 校验、工具适配、权限策略
  data/        Run、Evidence、Draft、Receipt 的 Room 实现
  AgentContainer
place/domain/
  PlaceQuery、PlaceImport 与共享校验
place/data/
  导入提交/受限撤销的 Room 事务实现
```

目录是逻辑职责，不要求每个框独立成 Gradle 模块。纯运行状态机、校验器和模型端口不依赖 Compose 或高德实现；Android、高德、Room、网络代码位于适配器。`AppContainer` 只做组装，Agent 通过按需会话接口获取地图能力，不绕过 ADR 0001。

首期没有必需的独立服务端部署。可选网关未来另行选型；ProviderAdapter 隔离厂商消息与认证细节，领域层不接触 API key、厂商 SDK 或 reasoning 字段。

## 8. 分阶段实现与测试门禁

1. **业务执行层先可独立测试**：Fake 模型和 Fake 地图验证草稿、确认、提交、回执、撤销。真实业务规则无需云端即可验收。
2. **接入模型协议**：小样本验证文本/图片识别、工具循环、歧义和失败；工具结果伪造、素材提示注入、超限和流中断必须有测试。
3. **接入 App 交互**：评审后的 Pencil UI 对接任务状态，验证 Repository Flow、数据库迁移、权限撤回及前后台恢复。
4. **后续扩展行程能力**：增加 ItineraryEdit/ChangeSet，而不是再建立一个“v4 Agent”。

业务测试至少覆盖：确认后草稿被改、同名跨城、无坐标、重复地点、重复请求不同参数、并发收藏、地点/元数据/回执事务回滚、提交后响应丢失、已撤销请求重放、后续编辑/关联导致撤销拒绝。已有数据迁移与触屏回归均通过后才能宣称接入完成。

当前仅评审技术方案。基础 Provider smoke 已通过；产品入口、确认卡片、素材留存及其余协议/Android 门禁仍需后续专项评审；不创建实施计划或开始开发，直到本架构稿获批。

## 9. 本次设计验证、依据与回滚

- 已查询当前知识图谱定位模块，再回读当前源码核实；图谱中包含历史和测试节点，不把其“Agent skills”节点当作已实现 Agent。
- 当前 GitHub 待合并 PR 查询为空；没有现成 PR 可直接采用。
- 图源通过 Archify schema、坐标/连线与 SVG 检查；明暗主题和文字可读性另见本工作树 `.codex/TASK_STATE.md` 的本次验证记录。结构源可编辑；HTML 不是可拖拽的 Pencil UI 稿。
- 未改 App 代码、数据库、Gradle、正式 `.pen` 或冻结 v4 源图；不运行 App 测试；独立 Provider smoke 通过不代表 App 接通或功能验收。
- 参考的官方资料已实际读取：Android [数据层与单一事实来源](https://developer.android.com/topic/architecture/data-layer)、[Room 异步查询](https://developer.android.com/training/data-storage/room/async-queries)、[后台任务调度](https://developer.android.com/develop/background-work/background-tasks/persistent)。外部资料解释机制，不证明本方案已实现；本地源码是现状依据。
- 写入前备份：`/Users/bytedance/.codex/backups/easy-trip/v3-agent-architecture-20261010-214549/`。需要撤销本次文档设计时，单独反向提交架构稿/图源及统一规划链接修改；不恢复整个仓库，不改旧 v4 原件。`.codex/TASK_STATE.md` 仅撤销本次新增区段，保留历史记录。
- 本轮用户已要求提交、rebase 最新 `origin/main` 并推送；交付状态以[本版记录](../../testing/v3.0-agent-architecture-delivery.md)为准。目标仅为 `origin/codex/v3-agent-place-intake`，禁止向 main 推送。用户随后明确“允许这一次 force push”；本次仅使用绑定已核对旧 SHA 的精确 `--force-with-lease`，不解除日后禁强推约束，不能将本地成功说成远端交付。

### 本版补充依据与验证范围

- DeepSeek 官方：[模型能力表](https://api-docs.deepseek.com/zh-cn/quick_start/pricing)、[图像理解](https://api-docs.deepseek.com/zh-cn/guides/vision)、[工具调用](https://api-docs.deepseek.com/zh-cn/guides/tool_calls)、[思考模式](https://api-docs.deepseek.com/zh-cn/guides/thinking_mode)、[Chat Completions](https://api-docs.deepseek.com/zh-cn/api/create-chat-completion)；证据、差异与未实测边界见[能力调研](../../analysis/2026-10-10-deepseek-flash-capabilities.md)。
- Runtime 依据：[Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)、[Kotlin serialization](https://kotlinlang.org/docs/serialization.html)、[OkHttp 官方仓库](https://github.com/square/okhttp)、[Android Keystore](https://developer.android.com/privacy-and-security/keystore)。这些资料证明技术机制，不代表本项目依赖兼容或端到端测试通过。
- 本版修改前文件及 refs 备份：`/Users/bytedance/.codex/backups/easy-trip/v3-provider-design-20261010-223722/`。精确文件清单、图形校验、rebase 结果、远端状态与回滚边界记在交付记录中。
