# 助手底栏修复：等待上游、提交推送与手机安装

2026-10-12，用户授权：“等 v3.2 优化交互那个对话推送完成，你也提交推送，并安装手机。”本轮不发布 Release 或标签。

## 顺序与集成

- 通过 Codex `wait_threads` 等待“v3.2 优化交互”对话完成；其最终交付提交为 `657dfad373bb5f7b098911a36a21a7b565426d5a`，包含功能提交 `1eb3d16` 的助手请求节流修复。
- 等待期间只备份、检查本树及安装工具，没有抢先提交、推送或安装。
- 工作树 `/Users/bytedance/.codex/worktrees/e9e3/easy-trip`，功能分支 `codex/assistant-footer-alignment`；修改前开放 PR 为空，七个文件与上一轮开发验证清单一致。
- 本批先提交为 `27a6aef`，随后 rebase 到 `657dfad`，形成 **`d1d7d08cab5a01dc81db0cbff2a9722a065fae1b`**。
- 应用源码和测试自动合并；仅 AGENTS 与任务状态两份文档冲突，保留双方历史记录。本批应用/设计 patch-id 前后一致，8个上游应用及测试文件逐字节一致；共同修改的直接收藏测试同时保留上游等待时长和本批文案点击断言。
- 2026-10-12 07:58:50（Asia/Shanghai）功能提交已显式快进推送 `origin/main`，独立 `git ls-remote` 回读与本地 HEAD 一致。推送前再次 fetch 并验证祖先关系；没有 force push、merge commit 或操作 codebase 远端。

## 本批内容

底栏透明并跟随主题，“全选”共用至少48dp高点击区域，20dp框到文案8dp、整组到收藏按钮16dp。上方地点行38dp/10dp间距仅检查，未调整。需求、Pencil保存及原始 red/green 证据见[开发记录](assistant-footer-alignment.md)。

安装包同时包含先前统一默认城市规则和上游请求节流修复。应用仍为 **3.2.0 / build25 / schema11**。

## 集成验证

全部来自 rebase 后干净提交 `d1d7d08`：

| 检查 | 结果 |
|---|---|
| Debug / Release JVM | 各1079项，0失败、错误、跳过 |
| Debug lint | 0错误、62 Warning、1 Hint |
| Release lint | 0错误、59 Warning、1 Hint |
| Debug / AndroidTest / Release构建 | BUILD SUCCESSFUL |
| 助手底栏、直接收藏、城市与地图城市恢复UI | 21项通过，软件渲染只读模拟器 |
| 真实SDK生产Controller专项 | 青州8地点首次READY，8次请求、0错误 |
| 真实发起间隔 | 704、950、701、702、704、702、702ms，最小701ms |
| 输入一致性 | 581个构建/源码输入在构建、验证与安装后哈希相同 |

- 最终普通/320dp双倍字体容器截图再次目视检查：底栏无白底，全选聚合，按钮未溢出。
- 不把上述21项相关UI解释为全应用验收。上游额外整组UI的两个历史基线失败、历史280dp极小窗口、真实模型/相册/TalkBack边界继续保留，详见[节流交付记录](v3.2.0-assistant-search-pacing-git-delivery.md)。
- 本轮未修改 Pencil 源；开发阶段已保存重开的 v3.2 源哈希仍为 `1f417d3aaedd5fc37053ba48fbb3b920f8d465cb2f8302862c3bb6c1aa7e1f65`，冻结 v3.1 源未变。未把 Git 交付冒充新一轮全页设计验收。

## 安装与回读

- Release APK DEX 独立确认 `GIT_SHA=d1d7d08cab5a01dc81db0cbff2a9722a065fae1b`、`SOURCE_STATE=CLEAN`，非 debuggable，16KB zipalign通过。
- APK SHA256：`ac7e88f225e49f5df0b3cd490cdfa10e226d5f3f633f2fd612069a4a494f13bd`。
- 签名SHA256：`a12b60ce83aaacd76899a66d81d78f3fc4cfe3b7424f9a5cf7479732c415048e`，与手机安装前APK一致。
- **2026-10-12 07:59:07（Asia/Shanghai）**，华为ALN-AL00（`FMR0224725012307`）执行 `adb install -r` 返回 Success；未卸载、清数据或降级。
- 安装后重新从手机拉回 APK，SHA256与目标包完全相同。
- UID10477、首次安装时间 `2026-09-21 08:33:25`、数据目录 `/data/user/0/com.yangchengwei.easytrip` 均保持不变；不等同于对全部用户数据库内容的独立审计。
- 安装前应用在前台，安装后回到桌面；恢复启动 Easy Trip，`Status: ok`，进程存在，捕获日志没有 FATAL EXCEPTION。
- 手机首页可见待出行4、已出行1、青州8个地点/3天行程。未触发手机搜索、模型、收藏或编辑；助手结果页在进程重启后未重建，本轮不声称底栏已完成真机结果页验收。
- 安装包来源固定为功能提交 `d1d7d08`；后续文档收尾提交不会改变这一来源，也不重复安装手机。

## 通知与收尾

- 仅本任务只读模拟器 `emulator-5582` 已退出并从设备清单消失；模拟器、手机归属锁均按本轮 token 核验释放，未操作其他任务设备。
- punk-12通知 `om_x100b63f67fdb3ca0c255179bb0d6461` 已独立回读，目标chat、机器人身份、完整正文和deleted=false全部通过；状态在通知验证后写回。
- 本文件及状态文档作为收尾提交，再次执行 fetch、祖先检查、显式快进推送与远端SHA回读；最终SHA保存在本机 `final-push-verification.json`，不循环将提交自身SHA写入自身。

## 证据与回滚

本机证据目录：
`/Users/bytedance/.codex/artifacts/easy-trip/assistant-footer-delivery-e9e3-20261012/`。

- `before-delivery.tgz`、`before-files.json`、`before-diff.patch`：提交前全部成果备份。
- `rebase-verification.json`、`delivery-safety.json`：双方改动保留、凭据模式和范围检查；APK、签名、本机配置未入库。
- `integration-build.log`、`integration-validation.json`、`integration-ui.log`、`integration-sdk.json`：当前集成验证。
- `target-apk-verification.json`、`phone-before.apk`、`install-verification.json`、`startup-verification.json`：原APK备份、目标签名、安装与启动回读；原APK备份不是数据库备份。
- `feature-push-verification.json`、`cleanup-verification.json`、`notify-verification.json`、`final-push-verification.json`：远端、清理、通知及最终状态提交回读。
- `before-closeout-docs.tgz`：状态收尾前文档备份。

如需撤销代码，另行确认后从最新main创建新功能分支，以范围明确的反向提交回滚本批，复验后快进推送；不reset或强推远端。手机保持schema11，优先同签名更高versionCode的前向修复，不自动降级、卸载或清数据。
