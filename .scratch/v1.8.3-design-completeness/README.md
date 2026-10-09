# v1.8.3 设计补全审计

状态：**设计补全已保存并完成重开回读；运行验收仍有未通过项**（2026-10-09）。解锁后的保存恢复已完成，不再因锁屏阻塞；不声明“完整版本UI全量验收通过”。

## 当前结果与完成边界

- 只在 `/Users/bytedance/.codex/worktrees/6934/easy-trip` 修改设计，正式源仍是同一份 `design/easy-trip-v1.8.3.pen`。未开发v1.9.0，没有另建正式Pencil文件。
- 开始恢复前已有AGENTS.md、任务状态和审计记录修改，均保留；解锁收尾未再次修改AGENTS.md。
- **已保存并关闭重开**：当前6934精确路径经原生窗口URL及MCP活动路径双重核对。131个有效根、1个隐藏历史容器DRICS；9条路由/49状态组在00A–00C索引中回读一致，125个覆盖矩阵引用画板均有效。
- 顶层重叠0、placeholder 0；131个有效根均有已保存重开说明，旧锁屏context残留0。此前4批有效文字直接父容器裁切检查为0；本次版本说明板诊断为0，不等于所有视口/真机像素验收。
- 原生可编辑性：此前MCP编辑、回读并还原GcyyO索引标题，重开后原值保持；本次在原生Layers选择IQMCC及GcyyO，名称、字号28、行高150%等属性显示可编辑。未声称通过原生UI实际改写文字。组件引用已核对，Palette轴保留lake/forest/sunset/violet/rose。
- **最终落盘：2026-10-09 13:15:24.761924（Asia/Shanghai）**；18,339,285字节，SHA256 `0cb8f48fb14932e8850b426e1f18412b94149a43f5798fb61cfd40f6f90f033a`。最终保存后关闭并重开，MCP确认费用文字、五主题、状态索引、版本说明和context均保留。
- 保存过程没有直接写`.pen` JSON；最后一轮cmd-S未落盘时没有认定成功，改由原生Save流程恢复，依文件哈希与重开内容确认。关闭时曾露出已存在的其他工作树窗口，未对其设计内容做修改；最终停留在6934正式文件。
- **仍未通过的门禁**：窄屏大字体、全程6项、系统分享包名1项运行断言。未逐状态运行验收，未完成真机/实时高德端到端验证；这些边界保留在正式文件、矩阵及下方记录中，不以保存成功代替UI验收。
- 保存/重开结构化证据：`evidence/native-save-reopen.json`；其中MCP和原生UI观察为本轮工具回读结果的人工结构化记录，非原始接口导出。

## 基线

- 分支 `codex/v1.9.0-expense-categories`；HEAD `af212c5fc3cdd251b8db7df11a62325ebce4f85e`。
- 本地origin/main引用 `7dc3692de345e8bf227570de170fc7435035214f`（本轮未同步、未推送）。
- 代码v1.8.3 / versionCode13，`git diff bd370c4f44df8369bfb993bbea881a6396c2a0f5 -- app` 无差异。
- 恢复开始前待合并PR列表检查为空。没有修改应用源码，没有操作物理手机，没有commit/push。
- 与本轮开始的tracked-before.json比较，仅任务状态和目标v1.8.3设计变化；其他已跟踪文件（含历史Pencil快照）哈希保持不变。审计新增文件另计。

## 已保存的修复与补全

1. 首页补回准备度与费用摘要，空值/0元区分；单日/全程费用与未填项一致。
2. 全程页恢复逐站只读时间线、交通和逐日费用；长页面展开展示并说明实际滚动行为。
3. 创建提交中/失败保留输入，工作台加载/不存在/失败，搜索初始/定位/授权异常。
4. 旅行卡菜单、重命名、删除影响失败/同步失败、设置读取失败、旅行日拖柄与排序失败；添加旅行日移除不存在的日期表单。
5. 收藏只读详情/备注标签编辑、搜索地图详情、删除和取消收藏影响；行程菜单/跨日移动、部分加入成功与撤销。
6. 日历修正日期和时间位置，新增交叠成员/详情冲突、保存中/成功/失败重试/撤销反馈；编辑器补上上午下午切换。
7. 分享选日、放大预览、行程加载失败/图片读取失败/地图降级；统一分享样例日期，单独命名避免冒充首页同一旅行。
8. 五主题原始色值和可编辑状态组件；主题预览失败与按钮状态；授权报告失败；更新进度、版本说明和系统安装返回提示。
9. 同一文件内按UI Kit、版本/状态索引及11业务模块重排；每个有效根context指明v1.8.3、发布代码和当前验收边界，清除过时“继承即完整”结论。

覆盖明细：`/Users/bytedance/.codex/worktrees/6934/easy-trip/.scratch/v1.8.3-design-completeness/coverage.md` 与 `coverage.json`。Pencil节点metadata更新未被工具持久回读暴露，因此版本证据使用可见版本板和context，不依赖metadata成功假设。

## 验证证据

独立临时模拟器：easy_trip_v183_audit_6934 / emulator-5598 / API31 arm64。采用已有Compose测试fixture和受控假地图，**不是实时高德网络/SDK端到端或真机验证**。未安装新软件；构建改用已安装JDK17恢复。临时模拟器已停止，并回读adb设备列表确认emulator-5598消失；临时AVD留在备份中以便复核。

| 本轮运行 | 结果 | 证据 |
|---|---|---|
| assembleDebug + assembleDebugAndroidTest | 成功，69 tasks | evidence/gradle-build-jdk17.log |
| testDebugUnitTest | 928通过，0失败/错误/跳过 | evidence/gradle-final-verification.log；本地XML |
| lintDebug | 0错误，54 Warning、1 Hint | 同上；app/build/reports/lint-results-debug.xml |
| 7个VisualBatch0EvidenceTest | 7通过，7张界面截图 | evidence/runtime-visual-tests.log；临时备份下runtime/files/evidence/batch-0 |
| 首批UI回归 | 152项，147通过、5失败 | evidence/runtime-regression.log |
| 正确390×844dp重试 | 11项，10通过、1失败 | evidence/runtime-recheck.log |
| 地点/全程/分享/日历追加验证 | 43项，36通过、7失败 | evidence/runtime-final-modules.log |

首批配置为390×844物理像素/density160；复核与追加配置为780×1688物理像素/density320，逻辑390×844dp。首批4个失败在后者通过，不能简单累加为互不重复测试数。

### 尚未通过的UI项目（不修产品代码）

- `TripListContentTest.longNamesAtNarrowWidthAndLargeFontKeepActionsReachable`：大字体窄宽场景 `other-trip-trip-other` 不可见；复核仍失败。
- `WholeTripItineraryContentTest` 6项：axis/route节点、宽度340vs324dp、route语义、header高度36vs44dp、间距8vs0dp、旧时间摘要文本断言。当前代码未改，但尚未证明每个失败均属历史基线或过时测试，**不自动归因或算通过**。
- `ItineraryShareScreenTest.shareOpensSystemChooserAndCancelReturnsToPreview`：API31实际包`android`，测试期待`com.android.intentresolver`，属于所见包名断言差异；最终取消返回链未在此失败测试中验收。
- 其余追加：PlaceDetailPanel 22/22，全程6/12，分享4/5，ShareNavigation 1/1，CalendarResizeWorkspace 3/3。

7张fixture截图已逐张查看：home、route-editor、whole-trip、settings、search、item-editor、scheduled-place-detail。fixture中的准备度和费用有人工样例，不能替代领域数据正确性；设计按源码语义核对。未宣称逐画板真机像素一致。

## 工具恢复记录

- 原中断读取已恢复，使用小批次和局部回读。
- 本轮component override错误 `Eh806`：按同editId修补，复用原组件子节点，成功回读。
- 大范围诊断超时 `Amf5K` / `rbphP`：按同editId缩小批次，随后4批文字诊断与3批引用诊断通过。
- fit-content/旧布局缓存告警：回读bounds，针对有效内容固定所需高度并重验。隐藏历史子树不计入有效页面裁切。
- 历史阻塞：Mac锁屏曾阻止原生保存，报错 `The Mac is locked and automatic unlock could not unlock it`，当时未关闭未保存文档。用户解锁后于13:08首次保存全量修复，随后更新验收状态并于13:15完成最终保存/重开。
- 解锁后的保存恢复：一次cmd-S后文件哈希未变且关闭出现Save确认，继续通过原生Save保存；回读新哈希并重开核验，无需CLI登录或JSON绕过。

## 保护、通知与后续操作

- 保存恢复完成通知：punk-12消息 `om_x100b63b3da4e88a4de7eecf882469d8`，发送与回读ok=true，发件机器人、目标chat及正文一致；证据在`evidence/completion-notification.json`。

- 原始备份：`/var/folders/2w/1f00699j5n5f4jp09txy9n2c0000gn/T/easy-trip-v183-design-repair-20261009-7xgcsypp`，含目标pen、任务状态、AGENTS、原审计与tracked-before.json。
- 原始pen SHA256：`e000566287d0941e8db20a1d0a0fdb23a6e43e16ab1b75c0c0f1a9a7af333b54`。
- 文档收尾前另备份：`/var/folders/2w/1f00699j5n5f4jp09txy9n2c0000gn/T/easy-trip-v183-design-repair-20261009-7xgcsypp/checkpoint-before-final-docs`。
- 解锁提醒消息 `om_x100b63b2baba4ca4c11370e9f8e4413`，bot app `cli_aaa104e4de399cca`、目标chat `oc_b5dc30707aff7eb5628922703c9b3814`，发送/回读均ok=true且原文一致；回读在临时备份。
- 解锁后状态更新前备份：B/unlocked-save-20261009，含13:08首次全量保存的设计（SHA256 `07ff22667afa21357737a0532c00a3af1bd1e86b0d2330c5caa6ca23372e10f4`）及当时审计/状态文件。仅撤销本次解锁后的说明更新可使用此备份；不要回退之前的设计补全。
- 保存、关闭重开及回读现已完成；运行失败需另行定位，不能直接据此把v1.8.3声明为全部验收通过后派生v1.9。
- 仅撤销此次设计补全：先备份当前内存/后续编辑并获得回滚确认，再将B/design/easy-trip-v1.8.3.pen恢复到同名目标；保留AGENTS规则更新和原有未提交改动。任务状态和审计可从B对应路径恢复。不要全仓reset/clean或修改历史版本。
