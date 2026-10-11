# easy-trip 项目约定

## 当前发布基线

- **v3.0.0/build21/schema11已正式发布（2026-10-11）**，标签固定`bebaf82ca0dba4188f09fe31dba2724c4d594cdc`。唯一正式累积源`design/easy-trip-v3.0.0.pen`，234根；已通过Pencil同步v2.2.1两行标题与发布状态。后续开发、安装或发布前读取`docs/testing/v3.0.0-release.md`，遵守其中的验证边界和schema11前向修复要求。下方“当前v2.2/草案/未发布/标题待同步”均为阶段历史，由本段及发布记录取代；旧PDF不代表最终源，历史全页验收缺口保留。

## UI 设计

- UI 设计与设计修改必须使用 Pencil；若 Pencil 不可用或执行失败，立即停止并向用户报告具体错误，不得自动改用其他工具或绕过 Pencil 继续设计。

### Pencil 多会话与工作树隔离

- **固定目标**：本会话只操作当前工作树内已确认的 `.pen` 文件；所有支持 `filePath` 的文档操作均显式传入该文件的绝对路径，不使用相对路径或依赖当前窗口。
- **分别打开，保持打开**：多会话处理不同文件时，各目标文件须分别在 Pencil 中打开并保持打开；磁盘文件存在或打开命令成功不代表已正确加载。后续通过绝对路径定向操作，不反复打开或聚焦窗口来切换目标。
- **写入前核验**：先确认目标文件已打开，再以同一绝对路径只读回查目标内容。Pen 1.2.15 在路径缺失或未匹配已打开文件时会静默回退到最后聚焦窗口，因此调用成功或读到节点本身不能证明目标正确；无法确认时停止写入，不退而操作活动画布。
- **活动窗口不是会话绑定**：无 `filePath` 参数的 `get_app_state()` 仅反映活动画布，不是会话绑定或锁；不得因其显示其他文件而争抢焦点，也不得仅凭其结果判定带路径调用的目标。
- **无法定向时串行**：操作不支持指定目标且无法安全确认归属时，暂停该操作并协调串行执行；保留已有内容，不直接修改 `.pen` 文件或改用其他工具绕过隔离与核验。

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
