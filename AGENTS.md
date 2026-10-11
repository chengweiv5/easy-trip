# easy-trip 项目约定

## 当前工作树修复版本

- **f254 rebase集成（2026-10-11）**：用户明确要求“肯定要rebase”，已将本批助手默认值/精简修复rebase到`origin/main 9cb2c99`，应用提交`ca74862`，自然继承v3.1.0/build24/schema11与图文直接发送。Debug/Release各1029 JVM、17项助手UI及0错误Release lint通过；本树v3.1累积设计已补齐为262根/43,451节点。代码/设计提交`c567979`已快进推送main并回读一致；仅安装阻塞：当前ADB未发现手机，尚未执行覆盖安装。准确进度见`.codex/TASK_STATE.md`与`docs/testing/v3.1.0-assistant-defaults-delivery.md`，不再等待版本授权。

- **同分支追加精简（2026-10-11）**：助手内容顶部移除重复的“当前旅行：名称”，保留页面旅行顶栏、默认城市与收藏确认目标；1012 Debug JVM、7项UI及0错误Debug lint复跑通过。Pencil规则同步保存，243根/42,324节点保持，仍未提交、推送或安装手机。证据见下述默认行为修复报告的追加章节。

- **f254 / v3.0.2 助手默认行为追加修复（2026-10-11，未交付手机）**：`codex/v3.0.2-assistant-defaults` 基于 `cc5e50a`，首次进入即按地点池当前城市/旅行名唯一城市填默认城市（徐州旅行填“徐州市”，不取手机定位）；每项默认第一条有效同城候选，可更换，不自动收藏。1012 Debug JVM、7项UI及0错误Debug lint通过，Pencil累积源仍243根/42,324节点并已原生保存。本轮未提交、推送、安装或发布，不能用下条上一轮安装事实替代。详见 `docs/testing/v3.0.2-assistant-defaults.md`。

- **f254 / v3.0.2（2026-10-11用户指定，已交付）**：`codex/v3.0.2-assistant-city` 的应用/设计提交 `0bfd548` 已快进推送origin/main并回读一致，15:38:59同签名覆盖安装华为ALN-AL00，3.0.2/build23/schema11。保留v3.0.1地图卡与地址补全；正式累积设计243根/42,324节点已保存核验，冻结旧版不改。1005 Release JVM、9项城市/地图卡UI、6项窄屏大字及2项真实高德专项通过，Release lint 0错误。手机APK哈希、UID与首次安装时间回读通过；安装后手机切换其他应用，未继续打断，可见旅行数据/助手页复验未完成，不冒称全库核验。准确边界见 `docs/testing/v3.0.2-delivery.md`。没有创建Release或标签，公开正式版仍是v3.0.0。

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
