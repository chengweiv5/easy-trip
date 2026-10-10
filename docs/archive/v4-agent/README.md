# 原 v4.0 Agent 规划：已废弃独立版本，内容并入 v3.0

2026-10-10 用户确认：将“用户自行发现地点、Agent 帮助录入 App”与原 Agent 规划合并，不再单独推进 v4.0。

**当前入口：**[v3.0 Agent 统一规划：智能录入地点](../../superpowers/specs/2026-10-10-easy-trip-v3-agent-place-intake-design.md)。

本目录是冻结历史资料，不是待开发的另一套正式方案。旧文中的版本号、首期先调整行程、数据库版本和源码基线只描述原方案，当前顺序与范围以上述 v3 文档为准。

## 保留材料

- [原文](2026-09-24-easy-trip-v4-agent-architecture-design.md)
- [总体架构源图](assets/easy-trip-v4-agent/architecture.svg)
- [ChangeSet 生命周期源图](assets/easy-trip-v4-agent/changeset.svg)
- [行程调整时序源图](assets/easy-trip-v4-agent/sequence.svg)

四个文件逐字节来自本地分支 `codex/docs-v4-agent-architecture-20261008` 的 `14c4930914e3cbf6a13e1849cfe3403c7fa3d139`；不改写历史图或冒称它们已经覆盖智能录入的新交互。

## 分支差异与恢复

- 原本地 v4 头：`14c4930914e3cbf6a13e1849cfe3403c7fa3d139`。
- 原远端 v4 头：`81b5c3717d56cdccb4408829349cb1e720423d28`。其正文仍为 v3 命名，代码基线更旧；两者并非同一个提交。
- 对两个头分别建立本地归档引用 `codex/archive-v4-agent-local-20261010` 和 `codex/archive-v4-agent-remote-20261010`，另存已校验 Git bundle。
- 独立 v4 规划停止推进。保留原分支引用作为恢复入口，不把“废弃”误作丢弃历史或覆盖其他工作树。
- 不合并旧分支的整棵代码树；仅迁入以上四个规划文件及本次整合说明。

归档与验证记录见 [v3 / v4 规划合并记录](../../testing/v3-v4-planning-consolidation.md)。
