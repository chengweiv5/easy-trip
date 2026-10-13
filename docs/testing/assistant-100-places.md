# 助手识别地点上限 30 → 100

> **Git 已交付（2026-10-14）**：功能提交 `3d1d2fb32520e08b0b07e1030beb551e706e4ef6` 已快进推送 origin/main 并独立 SHA 回读一致；干净提交重新验证 Debug／Release 各 1135 JVM、lint 0 错误及三 APK。详见[提交与推送记录](assistant-100-places-git-delivery.md)。下文保留开发轮事实，“未提交／推送”和 DIRTY APK 均为历史；本轮仍未安装手机或发布。

状态：**开发及限定回归完成，未提交／推送／安装手机／发布**。验证完成于 2026-10-14（Asia/Shanghai）。

## 范围与基线

- 用户要求“把识别地点从 30 个放宽到 100 个”，并确认同时调整两个入口、编号、文案、测试与 Pencil。
- 工作树 `/Users/bytedance/.codex/worktrees/8bc4/easy-trip`，功能分支 `codex/assistant-100-places`，HEAD 保持 `9ee79bd3c62d1c9311e0cba2a2a6fe2c92c41867`；开始时干净，开放 PR 查询为空。
- 旅行内助手支持一批识别／收藏 100 个地点；首页全局助手支持确认后原子创建旅行并保存 100 个地点。不是“默认选择 100 个”，仍需用户选择和确认。
- 最终地点编号为 **A–Z、AA–AZ、BA–BZ、CA–CV**。超过 100 个保留输入、明确提示分批，不截断后查询，不部分写入。
- **20 张图片、1–30 天、700ms 查询发起间隔、并发 2、城市核对、作者证据与语义去重规则不变**。链接整理仍使用既有每份 100／全批 400 的原始候选预算，不将原始候选预算误当最终地点上限。
- 应用仍为 **3.4.0/build27/schema12**。没有数据库迁移、版本变更、Release 或标签操作；不沿用之前手机安装作为本批交付。

## 实现

生产修改限于 assistant 包内五个文件：

| 文件 | 改动与原因 |
|---|---|
| `AssistantModels.kt` | 共享 `MAX_PLACES=100`，所有使用该策略的解析、展示、收藏与创建校验同步；修正二字母编号进位，避免第 53 个及以后产生非字母。 |
| `PlaceAssistantController.kt` | 普通文字批次与查询重试总超时由 90 秒调整至 180 秒；60 秒识别加 100 次 700ms 间隔查询会超过旧预算。显式处理 101 个文字结果，保留原输入并展示“每批最多 100”。 |
| `DeepSeekPlaceParser.kt` | 普通文字／图片请求输出预算由 4096 调整至 8192；攻略材料请求原本已是 8192。严格工具协议、来源核验、overflow 拒绝不放宽。 |
| `CreateTripFromIntake.kt` | 创建恢复记录共享上限由 256KiB 改为 1MiB；100 个合法长名称／地址及来源摘要在测试中确实超过旧上限。命令语义及幂等摘要不变。 |
| `TripCreationJournal.kt` | 读取与写入统一使用 1MiB 有界上限；AtomicFile 行为不变，超大／损坏记录拒绝且保留，不自动清除。 |

图片／链接的动态总超时策略未变。本次没有为提速移除限流，也没有自动重试或新增供应商请求。

## 测试与构建

最终代码构建命令：

```sh
./gradlew testDebugUnitTest testReleaseUnitTest lintDebug lintRelease \
  assembleDebug assembleDebugAndroidTest assembleRelease \
  --max-workers=2 --console=plain
```

| 验证 | 最终结果 |
|---|---|
| Debug JVM | 1135 项，0 失败／错误／跳过，114 suites |
| Release JVM | 1135 项，0 失败／错误／跳过，114 suites |
| Debug lint | 0 Error／Fatal；64 Warning、1 Hint |
| Release lint | 0 Error／Fatal；61 Warning、1 Hint |
| 最终构建 | BUILD SUCCESSFUL，1m55s，Debug／AndroidTest／Release 三 APK |
| 最终精确 APK 设备回归 | 19 项通过，322.829 秒 |
| 系统 320dp／2 倍字体复测 | 2 项通过，148.420 秒；是前述用例复测，不算额外独立用例 |
| Release 包基础检查 | `apksigner verify` 与 16KB `zipalign` 通过 |
| 差异检查 | `git diff --check` 通过；597 个源码／测试／构建输入 hash 留存并在收尾复核 |

关键回归：

- 100 个地点全部 READY、100 个唯一合法字母编号、整批收藏；Z→AA、AZ→BA、BZ→CA、CS–CV 边界。
- 文字、图片、链接最终 101 个均拒绝，没有查询／部分收藏；120 个原始候选整理成 100 个可继续，失败重试复用已有材料。
- 60 秒识别后仍可完成 100 个节流查询，每次发起间隔至少 700ms。
- 文本与图片工具请求 `maxItems=100`／8192 输出预算；严格解码 100 个、拒绝 101 个，不弱化来源证据。
- Room 100 个收藏和 100 个新建均原子／幂等，101 个写入前拒绝；既有失败回滚、删除后不复活、重开恢复继续回归。
- 100 个长名称／地址的恢复记录超过 256KiB 后仍可写入和重开；超过 1MiB 拒绝并保留文件。
- 原 `AssistantThirtyPlacesUiTest` 升级为 `AssistantHundredPlacesUiTest`；可滚动访问末端徽标，文字不换行／溢出，选择热区至少 48dp。全选后实际保存断言为 100。
- 首页→核对→最终确认→Room 新建 100 个；仅识别或选择不创建旅行。常规及窄屏大字均通过。

100 地点／编号、慢识别超时、输出预算、恢复容量和清晰超限文案均保留先红后绿证据。过程中修正过测试编写错误（方法位置、严格协议 fixture 字段及默认城市）；这些不作为产品故障证据。

### 设备与截图边界

- 仅使用本轮自有 `emulator-5586`、API36 arm64、SwiftShader、read-only／不保存快照。无真实模型与高德搜索调用，供应商解析和 POI 使用确定性测试替身；持久化测试使用隔离内存库／临时库／临时恢复目录。
- 先前 19 项运行对应最后一处错误文案改动之前的 APK，不计为最终源码验收；本轮重新安装最终两 APK 后完整重跑 19 项。
- 系统复测实际设置为 960×1800px、480dpi、font_scale=2.0；分别执行 100 地点旅行内收藏和全局新建。之后恢复 1280×2856px、480dpi、font_scale=1.0，再退出自有模拟器。
- 最终末端 CS–CV、收藏按钮及“100 个地点只加入地点池”确认页截图已缩放到最长边 1280 的 JPEG 复核。截图中的部分选择数量 9／10 是逐个徽标点击过程，不是容量上限；最后全选保存由断言核实。
- 滚动列表在容器边缘会裁切未滚入的部分内容；这里只验收目标徽标、操作可达及本次容量闭环，不宣称所有页面／设备均无裁切。
- 未触碰已连接真实手机，未替换手机 APK，未验证手机数据、TalkBack、真实 20 图帖子或真实模型＋高德的 100 地点端到端结果。旧版真实模型结果不冒充本批验收。

## Pencil 与预览

- 唯一修改目标：`design/easy-trip-v3.4.0.pen`，所有支持 `filePath` 的调用均使用本工作树绝对路径；原生窗口 URL 与同路径 MCP 回读一致。
- **305 根／45,243 唯一节点，19 根变化**，无新增／删除节点、重复 ID、断裂引用或 placeholder。主要为容量文案和 context、G25 的 97–100 项 CS–CV 示例；首页布局及其余不相关节点保持。
- 同步修正旧 overflow 页残留 20 地点说明和流程总览“1–30个”；当前可见 30 地点上限文案为 0，1–30 天未改。
- 已原生保存、关闭本工作树窗口、重开并回读；G25 根属性与末项徽标的原生图层／字体属性可访问、可编辑。不是截图替代源，也不宣称全页手动编辑验收。
- 当前 SHA256：`fbc1dca80b5f6d8d95b0091b3ed72d7ad31f90645db6798b9f73c9bccf421ddc`。
- 冻结 v3.3 源保持 `e55030c5ed4d00cd7d0fcef59d448c525a73c2b933c06776d8252a14d61b8225`。
- 最新增量预览为 [`design/previews/assistant-100-places/`](../../design/previews/assistant-100-places/) 的 **19 JPEG＋4 页 PDF**；PDF 已逐页渲染、OCR 和视觉复查。旧 v3.4 预览中有关 30 地点的内容为历史，不代表本轮容量；旧文件未覆盖。
- 修改文字与 G25 徽标的定向布局检查通过。旧示意底图存在既有路径裁切，不据此宣称整个累积画布零裁切。

## 产物与追溯

证据根：
`/Users/bytedance/.codex/artifacts/easy-trip/places100-8bc4-20261013`

- `before/changed-area.tgz`、`before/v34-previews.tgz`：修改前备份。
- `logs/final-exact-build.log`、`final-test-lint-reports.tgz`、`final-build-verification.json`。
- `logs/device-capacity-final.log`、`logs/device-system320-final.log`。
- `final-source-hashes.json`、`design-verification.json`、`cleanup-verification.json`。
- `device-final/`、`device-system320-final/`：设备截图；`visual/`：设计检查与 PDF 渲染／OCR。
- `apks/`：最终已验证三 APK，均为未提交开发构建，不是发布包。

| APK | SHA256 |
|---|---|
| Debug | `16235cf6db568be282cba0a3230f2008fb9ce53327ff8b07a4f0f863a267c611` |
| AndroidTest | `c85c9424e4af66b27a9b2d79fd7840433882ae97ea7b52c625603369bf09ec4c` |
| Release | `0ff42ebc4665e1386fa455250ecd552c6cd906d6c2cc516cb2b23fbb85050c2c` |

BuildConfig 记录 HEAD `9ee79bd3…` 和 `DIRTY:c9e1bf9044488b42c240d6f0f32f698654e8efa20045a892ec6c4871c1e7d6a2`，不能称为 CLEAN。之后只更新设计／预览／文档，生产源码未再改。

## 风险与回滚

- 100 个候选会增加模型输出、地图查询总时长和可能的费用；8192 只是输出预算，不保证真实供应商必定完整返回 100 个。失败／超限仍保留输入，不能以截断冒充成功。
- 如需撤销，应先备份当前工作树并检查后续编辑，再在差异中仅反向撤销本批五个生产文件、相关测试及文档。不要 `reset --hard`、清用户数据或覆盖其他工作树。
- Pencil 通过原生打开备份源核对后，仅恢复本次 19 根的改动；不得直接脚本修改 `.pen` 或覆盖冻结旧版。
- **已有 100 地点待恢复命令时，不应直接退回只允许 30／256KiB 的旧代码**；先在当前兼容版本处理或保留恢复记录，再决定前向兼容修复。schema12 不回退 schema11，不自动安装旧包。
- 本轮没有 commit／push／安装手机／Release／标签，后续这些交付需单独授权和重新核验。
