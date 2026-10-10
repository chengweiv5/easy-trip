# v3.0 地点助手 Implementation Plan

> **For agentic workers:** 按任务逐项执行并验证。通常使用 superpowers:executing-plans；本环境未安装该技能，按下列清单在当前会话原生执行，不安装额外工作流。用户于2026-10-11明确要求完成设计后直接开发、不要等待，故不再增加人工等待门禁。

**执行状态：** 实现和Debug专项已完成；通知回读以实施记录最终交付节为准。Release签名缺失不绕过，不阻塞本轮Debug试用包；全应用验收/20项压力/TalkBack留为明确缺口。

**Goal:** 在当前旅行地图中批量标记用户输入的地点，用户单项或勾选核对后收藏；模型在App全局「助手设置」配置。

**Architecture:** Kotlin/Coroutines StateFlow 管理每项查询与取消；DeepSeek适配器只输出经校验的只读查询计划，真实POI由现有高德接口提供。选择快照与Room批量导入回执绑定，地点/分类/回执原子保存，模型不能写库。设置使用Keystore和noBackupFilesDir保存凭据。

**Tech Stack:** 现有Compose、Room、Coroutines；新增已缓存的OkHttp 4.12.0、kotlinx-serialization-json 1.7.3（仅Json树，无编译插件）。

**Spec:** `docs/superpowers/specs/2026-10-11-easy-trip-v3-first-agent-design.md`，Pencil `design/easy-trip-v3.0.0.pen`。

## Global Constraints

- 只在5ac3工作树、`codex/v3-agent-place-intake`实现；本轮不推送、不发布、不安装物理手机，不改变现有旅行数据。
- 一次最多2000字符/20项；查询词100字符/城市50字符；最多2并发真实POI查询，每项15秒，总任务90秒，模型45秒。只手动重试。
- 标记/勾选/全选不写库；目标旅行、所选POI/分类/版本变化必须重新确认。
- 部分成功保留；未知城市、跨城、多个合理POI、未匹配不自动确认；相同POI去重。
- 配置全局共用；HTTPS限定、拒绝跨源重定向、不记录Key或正文；未配置不影响旧功能。
- 不抓取平台、剪贴板或链接，不新增图片入口/行程工具/费用工具。
- 数据库只增加schema11回执表及10→11迁移，不清库、不降级；旧schema保留。

## Review Focus

- 取消时忽略不可取消数据源的迟到回调；已完成候选和选择不变（Task2）。
- 字段缺失、输出截断、多工具调用、伪造坐标/指令不成为写权限（Task1）。
- 二个输入命中同一POI、提交前已有记录、重复操作ID不得覆盖或重复保存（Task3）。
- 高德撤权、切旅行、改城市/分类使受影响确认失效，未选项后台完成不误清确认（Task2/4）。
- 320dp/大字/键盘/地图面板不能隐藏最终确认；设置返回不丢当前批次（Task4/5）。

## Task 1 — Provider与安全设置

**Files:** 新建 `app/src/main/java/com/yangchengwei/easytrip/assistant/AssistantModels.kt`、`DeepSeekPlaceParser.kt`、`AssistantConfigStore.kt`、`AssistantSettings.kt`；修改 `gradle/libs.versions.toml`、`app/build.gradle.kts`；测试 `app/src/test/java/com/yangchengwei/easytrip/assistant/DeepSeekPlaceParserTest.kt`。

**Interfaces:** `PlaceIntentParser.parse(input:String, defaultCity:String): List<PlaceIntent>`；`AssistantConfigStore.read()/save(config)/clear()`；`ProviderConfig(baseUrl,model,apiKey)`不使用自动暴露Key的toString。

- [x] 测试先失败：`assertThrows(IllegalArgumentException::class.java) { ProviderConfig("http://example.com","deepseek-flash","key").validated() }`；批量JSON、未知工具、非完整finish_reason拒绝。
- [x] 实现一次模型tool call `search_place_batch(items,overflow)`，用Json树校验字段、数量、sourceSpan、城市；不执行写工具。
- [x] 实现OkHttp异步可取消调用、重定向关闭、响应体上限、错误脱敏；固定测试内容不真实搜索POI。
- [x] 实现noBackupFilesDir + Keystore AES/GCM原子配置；配置测试成功才覆盖旧值。
- [x] 跑 `:app:testDebugUnitTest --tests '*DeepSeekPlaceParserTest'`，记录红绿结果。

## Task 2 — 地点助手状态机

**Files:** 新建 `assistant/PlaceAssistantController.kt`、`assistant/AssistantPoiMatching.kt`，测试 `assistant/PlaceAssistantControllerTest.kt`。

**Interfaces:** controller暴露`StateFlow<PlaceAssistantState>`、`submit/cancel/retry/editItem/choosePoi/toggle/selectAll/review/confirm`；只通过`PlaceSearchDataSource`查询；保存通过Task3的`PlaceImport`。

- [x] 写失败测试：四项里前两项成功、第三项超时、第四项歧义时前两项仍可选；`assertEquals(0, importer.calls)`在review前后均成立。
- [x] 实现稳定itemId、逐项generation、信任真实POI、城市核对、确定性保守匹配和重复POI归并。
- [x] 实现取消只停止剩余任务、失败项重试、缺城市补充、0选择不确认、分类改变使确认失效。
- [x] 测试保存仅可信UI明确confirm触发、旧确认拒绝、迟到结果不覆写、配置/隐私撤销取消后续任务。
- [x] 跑 `:app:testDebugUnitTest --tests '*PlaceAssistantControllerTest'`。

## Task 3 — Room原子导入与持久回执

**Files:** 新建 `assistant/PlaceImport.kt`、`assistant/data/ImportReceiptEntity.kt`、`assistant/data/RoomPlaceImport.kt`；修改 `core/database/EasyTripDatabase.kt`、`AppContainer.kt`；自动生成 `app/schemas/.../11.json`；测试 `app/src/androidTest/java/.../assistant/RoomPlaceImportTest.kt`。

**Interfaces:** `PlaceImport.commit(ImportConfirmation): ImportReceipt`；`receipt(operationId): ImportReceipt?`；确认含tripId/选中真实POI/分类/digest；全部新增同事务。

- [x] 写失败测试：2项确认保存后观察地点仓库得到2项；同operationId重放不增加，换digest拒绝。
- [x] 新建回执表，以operationId主键、tripId外键级联；10→11只建新表。事务核验trip存在和合法point，所选POI去重，已存在只记AlreadySaved。
- [x] 注入第二项写失败验证0新增及0回执，测试已有分类/备注不覆盖。
- [x] 测试迁移保存原旅行/地点/费用；版本3.0.0/build20。

## Task 4 — Compose地图与全局设置接入

**Files:** 新建 `assistant/PlaceAssistantPanel.kt`、`assistant/PlaceAssistantViewModel.kt`、`assistant/AssistantMap.kt`；修改 `TripWorkspaceRoute.kt`、`TripWorkspaceScreen.kt`、`TripWorkspaceContent.kt`、`AppNavigation.kt`、`AppSettingsContent.kt`、`AppContainer.kt`。

**Interfaces:** 当前trip backstack持有VM，映射候选到现有`MapMarkerUi`搜索层，不持有MapView；确认门禁由真实MapHostState回调提供。

- [x] 按Pencil加入地图「地点助手」入口及可收起底部面板；原搜索/地点池继续可用。
- [x] 输入、多项结果、歧义选择、勾选、分类核对、确认与回执逐态实现；关闭保留，清空/离开有草稿提示。
- [x] 地图显示候选字母；批量视野覆盖候选，点击标记打开对应项；保存后由既有Room Flow显示正式分类图钉。
- [x] App设置加入助手设置路由；返回原旅行保留批次。Key默认掩码，设置画面禁止系统截图。
- [x] 无配置/无地图同意/错误时提供恢复入口，不自动授权或上传。

## Task 5 — 集成验收与可检查交付

**Files:** 新建 `app/src/androidTest/java/.../assistant/PlaceAssistantUiTest.kt`、`docs/testing/v3.0-agent-implementation.md`；更新设计状态与`.codex/TASK_STATE.md`。

- [x] `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin :app:lintDebug`，修复新增失败，旧问题单列。
- [x] 专用可追踪模拟器上测批量UI/Room/地图/设置；不连接物理手机，不重用他人设备锁。
- [x] 对真实Provider可用时最多4个新请求独立记账，仅固定杭州样例；不复制本地Key到源码/日志或APK。无法测通如实报告，不把fake当真实通过。
- [x] 独立审查规格和实现，修复安全/数据一致性缺陷，再复跑针对性测试。
- [x] 提供本地Debug APK、设计对比截图、通过项/缺口/微调清单和精确回滚范围；通知punk-12并回读。

## 执行与回滚

用户已明确无需等待继续开发，采用当前会话原生执行。不为自动化而安装工具或创建新聊天。代码审查按code-review技能使用独立只读审查者。

修改前备份：`/Users/bytedance/.codex/artifacts/easy-trip/v3-implementation-20261011-014035/before-implementation.tgz`及diff/status。回滚只恢复本次改动路径；若已有后续编辑则差异撤销。不得整库reset/clean或覆盖原有未提交设计/探针。
