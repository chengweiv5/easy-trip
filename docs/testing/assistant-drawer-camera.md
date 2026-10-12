# 助手抽屉保持地图视角

日期：2026-10-12。用户确认后实施；本轮只开发与验证，未提交、推送、发布或安装真实手机。

## 范围与结果

- 分支 `codex/assistant-drawer-camera`，基线与当前 HEAD 均为 `bdc0a253e1b5cc9533c731a09faf4381fa6a1c36`。
- 打开、关闭、重开助手及在收起／半展开／全展开间拖动，不重置地图中心或缩放；地图 host 不重建，地图底层尺寸不变。
- 新的可见搜索结果、主动聚焦与更换候选仍定位。普通搜索可重新接管地图；勾选／全选不定位。
- 隐藏期间到达的结果不移动相机，重开也不补发旧定位。清空草稿不重放旧工作区请求。
- 地点池及行程自身抽屉规则保留：行程从全展开重新展示时仍适配一次；助手覆盖在行程上时不触发该恢复。
- 不改城市来源、收藏持久化、页面布局、版本或数据库。仍为 3.2.0 / build25 / schema11。
- 前轮手机安装记录 `assistant-next-round-install.md` 及原有 AGENTS/TASK_STATE 修改保留，不冒充本轮安装。

## 红测与根因

测试使用真实 `TripWorkspaceRoute → TripWorkspaceScreen → AmapComposeMap`，只把 SDK map host 换成相机命令记录器；助手使用合成 parser/search、隔离配置及测试仓储，不使用真实模型或用户旅行。

1. 原代码运行 `WorkspaceFlowTest#reopeningAssistantWithCandidatesDoesNotMoveMap`，关闭已有候选的助手时失败：`Closing the assistant must retain the camera expected:<2> but was:<3>`。助手 `open` 变化生成／移除视野请求，移除后 owner 退回已消费的工作区请求；重开又会创建新的 request id。
2. 第一处修复后，该测试通过（2.618 秒）。追加真实拖动，`assistantDrawerOverItineraryDoesNotMoveMap` 在 `ITINERARY / HALF` 失败：`expected:<3> but was:<4>`。底层行程的全展开恢复逻辑错误地响应了助手抽屉。
3. 修正助手拖动的保留视野标记后，两项新测试及原有行程恢复测试一起通过。

修复：

- `TripWorkspaceScreen`：请求键只包含助手实例、候选坐标与焦点，不再包含 open；可见性单独参与请求接收判断。
- `AssistantViewportOwner`：只接收新的相机意图；观察隐藏请求但不执行、不因重开重放；退出旧草稿不回退旧请求。
- `TripWorkspaceRoute / TripWorkspaceViewModel`：助手的 `SetSheetLevel` 显式保持视角；普通调用默认保留原有行程行为。没有修改通用 `MapViewportController` 或 SDK renderer。

## 验证

| 检查 | 结果 |
|---|---|
| Debug JVM | 1083 项，0 失败／错误／跳过 |
| Release JVM | 1083 项，0 失败／错误／跳过 |
| Debug lint | 0 错误，62 Warning / 1 Hint |
| Release lint | 0 错误，59 Warning / 1 Hint |
| Debug、AndroidTest、Release APK | 构建成功；本地未提交开发包，不是 CLEAN 发布物 |
| 最终普通尺寸 UI | 6 项通过：两项助手相机、原有行程恢复、3 项助手直接收藏 UI |
| 系统 320dp / 2 倍字体 | 3 项相机与行程恢复 UI 通过；960×1704px / 480dpi |
| 源码静态检查 | `git diff --check` 通过；验证输入哈希回读 |

新增 owner 单测 3 项，加原有 2 项共 5 项，覆盖隐藏更新、开关、清空、手势释放与普通搜索接管。
新增 UI 两项分别以地点池、行程为底层，点击真实助手入口及关闭按钮、拖动真实抽屉把手，断言相机命令次数／目标点、host 挂载数及尺寸；并检查新结果、聚焦、候选替换、普通搜索及清空草稿。

### 额外历史失败边界

扩大测试时，已有 `manualMapGestureClearsViewportAndSuppressesSectionFitsWithoutZoomButtons` 失败：手势后切换普通标签期望相机总次数 1，实际 3。

本轮用 `git archive bdc0a25` 在独立临时目录构建完全未修改的产品和测试，再在同一自有模拟器运行该精确方法，得到相同 `expected:<1> but was:<3>`。它也已见于 `v2.2.0-place-category-implementation.md` 的历史失败清单。本轮不修改此旧测试或普通标签切换规则，不声称全应用 UI 全绿。原始 7 项运行是 6 通过、1 失败；最后精确 6 项通过不能覆盖这个事实。

## Pencil 与审查

- 唯一目标为当前 e9e3 工作树 `design/easy-trip-v3.2.0.pen`。
- 先原生窗口核验绝对路径，再同路径 MCP 只读回查；通过 Pencil 更新 `WPYC3`、`kIRND` 的 context，未改可见布局。
- 原生 File → Save、关闭并重开同一路径，MCP 回读两处规则及截图通过。
- 保存后仍 265 根 / 43,305 节点，结构差异仅两处 context；SHA256 `12b145e0b1674d4e4a58b7251ec7a6686b57e0875aefdbc9085a915cbbd984f2`。冻结 v3.1 源未变。
- 原生应用初次以“Pencil”名称绑定失败，按应用清单改用真实 bundle id `dev.pencil.desktop`；不是画布执行失败，没有绕过 Pencil 写文件。
- Standards / Spec 两个只读审查未发现必须修复项；主代理以本轮用户确认核对范围，子代理源码检查不代替设备运行证据。

## 验收限制

自有 read-only 软件渲染模拟器 `emulator-5582`，相机 host 为记录器。验证的是生产 UI 到相机命令边界，不是高德真 SDK 瓦片／实际地理投影或真机视觉验收。本轮没有操作真实手机、调用真实模型、系统相册或 TalkBack；没有全应用验收。

## 证据与回滚

收尾：自有模拟器已退出，归属锁核验后释放，独立基线临时目录已删除；567 个源码／构建输入哈希未变，前轮安装报告及冻结 v3.1 源保持不变。punk-12 完成消息 `om_x100b63fb8e967ca0c4f7191fd6eab21` 已独立回读并核验 chat、机器人、完整正文与 deleted=false。本地任务状态在通知验证后写回。

本机目录：
`/Users/bytedance/.codex/artifacts/easy-trip/assistant-drawer-camera-e9e3-20261012/`

- `before.tgz`、`before-files.json`、`prior-diff.patch`：修改前备份，保留前轮安装记录。
- `red-instrument.log`、`green-instrument-1.log`、`anchor-instrument.log`、`anchor-green-instrument.log`：红绿链。
- `full-validation.log`、`jvm-counts.json`、lint XML、测试 XML 归档：构建与单测。
- `ui-final.log`、`ui-final-passing.log`、`ui-narrow.log`、`narrow-device.txt`：扩大检查失败及最终专项通过。
- `baseline-build.log`、`baseline-existing-failure.log`：未修改基线的旧问题独立复现。
- `pencil-diff-verification.json`、`validated-inputs.json`：设计范围及源码验证输入。

回滚仅需选择性撤销本轮 4 个产品文件、2 个测试文件及两处 Pencil context；设计恢复通过 Pencil 实施。源文件可参考修改前归档或基线 `bdc0a25`，不要 reset 整个工作树，更不要覆盖前轮未提交的安装文档。无数据库迁移、远端或手机状态需要回滚。
