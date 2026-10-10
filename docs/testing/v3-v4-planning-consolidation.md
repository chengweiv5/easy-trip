# v3.0 / v4.0 Agent 规划合并记录

日期：2026-10-10。

## 请求与范围

用户确认将当前“智能录入地点”与已有 v4.0 Agent 规划合并，并废弃独立 v4.0 分支规划。本轮只做规划整合、历史保护与状态同步，不开发 Agent、不修改 UI 或数据库、不发布 APK。

当前规划入口：[v3.0 Agent 统一规划](../superpowers/specs/2026-10-10-easy-trip-v3-agent-place-intake-design.md)。

## 合并前核验

- 当前工作树：`/Users/bytedance/.codex/worktrees/5ac3/easy-trip`，初始 detached HEAD `2b45968430fd66391a449f53d848f9eebe9e6c19`，工作区干净。
- 获取的最新 `origin/main`：`773387fe27427ad12811f44dd5a2e8d39730676f`；其额外两次提交属于空旅行城市定位修复和交付记录，保留。
- 在该基线创建功能分支 `codex/v3-agent-place-intake`，不直接修改 main。
- GitHub 待合并 PR 查询为空；没有现成 PR 可替代此次整合。
- 原本地 v4 分支位于 `9be0/easy-trip` 工作树，读取时干净，头为 `14c4930914e3cbf6a13e1849cfe3403c7fa3d139`。相对主线只新增一份文档和三张 SVG，没有独有产品实现。
- 原远端同名分支头为 `81b5c3717d56cdccb4408829349cb1e720423d28`，仍处于旧源码基线和 v3 文档命名。仅比较规划内容后确认其差别是版本安排/链接，不能以整树 diff 覆盖主线。
- 原飞书文档 `MhG8dAiFHorRB9xPts1cLx9enuc` 回读版本 8，标题为 v4.0；原有三张画板 token 已备份。

## 保留与改动

1. 创建独立的本地归档引用：
   - `codex/archive-v4-agent-local-20261010`
   - `codex/archive-v4-agent-remote-20261010`
2. 保存并验证 bundle：`/Users/bytedance/.codex/backups/easy-trip/20261010-v3-v4-consolidation/v4-agent-history.bundle`。它包含两个归档头，依赖已存在的主线历史，已用 `git bundle verify` 验证。
3. 原 v4 文档与三张 SVG 逐字节迁入 `docs/archive/v4-agent/`，增加明确的废弃状态与当前规划指针。
4. 新 v3 文档以智能录入为首闭环，复用手机执行/云推理、业务工具、安全提交与恢复原则；行程调整、创建与检查保留为后续阶段，不删除原设计。
5. AGENTS 只增加规划入口，不改变已交付 v2.2 的设计基线。
6. 独立 v4 规划废弃；原 Git 引用保留为恢复入口，不删除其他工作树或其历史。

## 验证范围

- 归档四文件与原提交逐字节一致，原三张 SVG 未重绘。
- 规划中明确用户决定收藏、Agent 执行、来源边界、POI 消歧、授权、幂等、回执、受限撤销和后续能力。
- 检查本次新增本地 Markdown 引用、SVG XML 与 `git diff --check`。
- 限制改动在规划/归档/AGENTS；未修改 `app/`、Gradle、正式 `.pen` 源或已有交付文件。
- 飞书在同一文档更新标题、状态段和历史章节标题，并插入当前统一规划；回读 revision 13，原 64 个未修改顶层内容块序列化一致，三张画板的原 block ID 与 token 全部保留。未编辑画板，不将此核验表述为重新验收画板内容。
- 本轮未运行 App 单元测试或设备测试；文档验证不能等同 Agent 功能验收。

## 外部同步与交付状态

- 飞书沿用同一文档：[Easy Trip v3.0 Agent 统一规划：智能录入地点](https://bytedance.larkoffice.com/docx/MhG8dAiFHorRB9xPts1cLx9enuc)，版本从 8 更新至 13；每次写入后均回读。
- 当前对话更名为“v3.0 Agent 统一规划：智能录入地点”。
- 原对话 `01a0d0fe-5efb-77c3-8f4d-09456dd2a20f` 更名为“已废弃（并入 v3.0）：v4.0 agent 操作调整行程等”，归档工具返回 `archived: true`；没有删除该对话、Git 引用或工作树。
- 旧对话摘要存在“不往 origin/main 推送”的约束；正文读取工具出现游标参数异常，未取得正文作进一步确认。本次保守地只在本地功能分支提交，不推送任何远端，不将规划合并等同 Git 主线合并。
- 完整飞书写入响应、前后回读与对比报告保存在上述备份目录；通知回执在发送后保存同一目录。

## 追加确认：v3.0 推送约束

2026-10-10 用户明确确认：当前分支继续禁止直接推送 `origin/main`，只能推送对应的 v3.0 远程分支。此确认取代上文对旧对话约束的保守推断。

- 本地分支：`codex/v3-agent-place-intake`；唯一允许的远程目标：`origin/codex/v3-agent-place-intake`。
- 本次 `git ls-remote --heads origin refs/heads/codex/v3-agent-place-intake` 查询成功且无匹配，目标尚未创建，当前分支尚无 upstream。本轮未推送、未创建远程分支，也未设置不存在目标的 upstream。
- 仓库共享配置仍有 `remote.origin.push = HEAD:refs/heads/main`，因此规则要求每次显式使用 `HEAD:refs/heads/codex/v3-agent-place-intake`，禁止裸推送；未改动共享 Git 配置或其他工作树。
- 本次仅更新项目规则与本文，不修改 App。修改前备份位于 `/Users/bytedance/.codex/backups/easy-trip/push-policy-20261010-172427/`。
- 撤销本次文件修改可单独反向提交此次规则更新；撤销文件不代表用户已授权恢复向 main 推送，变更推送目标仍须用户另行明确授权。

## 回滚

- 本次源码无功能变化。需要撤销规划整合时，从整合提交按文件恢复本轮新增文档，并恢复备份的 AGENTS；主线已追加其他提交时使用反向提交，不重置或强推。
- 原两个 v4 头可从上述归档引用和 bundle 恢复到新恢复分支，不覆盖任何活动工作树。
- 飞书修改前正文与版本保存于 `/Users/bytedance/.codex/backups/easy-trip/20261010-v3-v4-consolidation/feishu-before.json`；回滚只撤销本轮修改的标题/状态段和新增说明，不整篇覆盖后续编辑，不替换三张画板。
