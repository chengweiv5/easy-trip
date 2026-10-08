# easy-trip 项目约定

## UI 设计

- UI 设计与设计修改必须使用 Pencil；若 Pencil 不可用或执行失败，立即停止并向用户报告具体错误，不得自动改用其他工具或绕过 Pencil 继续设计。

### 版本设计快照

- **一个版本，一份完整快照**：每个版本的正式设计源统一为 `design/easy-trip-v<完整版本号>.pen`（如 `design/easy-trip-v1.8.0.pen`），反映该版本全部功能页面、必要交互状态及共用组件的最新状态，不是本次需求的增量稿。按特性命名的单页稿、探索稿和预览只能作为辅助材料，不能充当版本基线。
- **累积演进**：新版本从已核验的上一版完整快照派生；保留未变页面，更新变化页面，将新增能力纳入原有结构。同版修改回写该版本的正式文件，不为每次需求另建正式源，不以堆叠新旧界面代替更新。已发布历史快照不随新版开发改写。
- **统一组织**：画板按 UI Kit、业务模块、页面及状态分区，采用稳定且可辨识的命名和顺序；明确同一页面的主状态、变体和交互关系，保持组件、样式与命名一致。废弃方案移出当前版本的有效页面集合，保留为明确标识的历史材料。
- **基线可追溯**：版本文件内注明目标版本、继承版本、对应代码基线和设计／实现／验收状态；开发中的新增能力标为待实现，已发布版本应与该版本实际实现一致。未开发的未来版本（如 v2.0）独立保留，不能因文件名较新或旧指南引用而成为当前版本基线。
- **只在当前工作树操作**：先核对当前分支、代码基线及文件内容，再从当前工作树解析设计路径；不因工作树路径而切换到主仓库或其他工作树。文件存在、文件名或打开命令返回成功，都不能代替 Pencil 实际加载与内容回读。
- **缺口先补齐**：上一版完整快照缺失或失真时，先备份，依据该版本代码和实际运行界面核对历史设计素材，在 Pencil 中补齐并验证，再派生新版。无法核验的页面明确列为缺口；仅复制、改名或拼接单页稿不算补齐，不把不完整稿声明为完整版本。
- **完成门禁**：交付前回读目标文件，逐项核对全量页面与必要状态覆盖、版本归属、组件一致性、布局可读性和原生可编辑性；确认没有遗漏旧能力或混入未来方案，记录证据、未通过项及仅撤销本次修改的回滚方式。未通过时不得宣称版本设计完整。

## Git 推送

- 在功能分支开发和提交；本项目始终推送到 `origin/main`，使用 `git push origin HEAD:main`。
- 推送前获取远端最新 `main`，确认 `origin/main` 是当前 `HEAD` 的祖先；仅允许快进推送，禁止强制推送。
- 推送后回读远端 `refs/heads/main`，确认提交哈希与本地 `HEAD` 一致。

## 飞书文档

- 本项目未指定目录时，飞书文档默认放入：
  https://bytedance.larkoffice.com/drive/folder/JIpZfzZMNlZfvNdMwrvc3nf4nxg

## Agent skills

### Issue tracker

Issues are tracked as local Markdown files under `.scratch/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Use the five default canonical triage labels. See `docs/agents/triage-labels.md`.

### Domain docs

Use the single-context domain documentation layout. See `docs/agents/domain.md`.
