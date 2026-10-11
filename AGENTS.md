# easy-trip 项目约定

## 当前设计目标

- **v3.2.0 Git交付（2026-10-11，进行中）**：用户已授权“提交，推送”；首次fetch确认origin/main仍为`c1e0a7f`，开放PR为空，29个实施输入／设计／预览hash复核一致。本轮在功能分支提交后显式快进推送origin/main，不发布Release／标签、不重新安装手机。准确交付状态见`docs/testing/v3.2.0-assistant-simplify-git-delivery.md`；下方“未提交／未推送”为开发和安装阶段历史。

- **v3.2.0后续安装（2026-10-11）**：用户单独授权“安装”后，21:15:46同签名3.2.0/build25/schema11开发包已覆盖安装华为ALN-AL00，手机APK回读hash与已验证归档包一致，UID／首次安装时间／数据目录不变；没有卸载或清数据。手机前台为飞书，未打断用户，未进行本轮真机界面或全库核验。准确证据与回滚见`docs/testing/v3.2.0-assistant-simplify-install.md`。未提交、推送、发布；下条“未安装”只记载开发阶段，Pencil不因单独安装重写。

- **v3.2.0助手交互精简（2026-10-11，已确认并实施）**：功能分支`codex/v3.2-assistant-simplify`基于`c1e0a7f`，应用3.2.0/build25/schema11。地图结果页左侧选择／全选并直接收藏，删除核对、二次确认及回执中转；保存中锁定，失败保留选择和幂等操作，候选／分类／单项修正在当前面板展开。唯一累积源`design/easy-trip-v3.2.0.pen`保持265根/43,295节点，已回写实施状态、原生保存关闭重开及MCP回读。Debug/Release各1035 JVM、0错误lint及助手专项通过；未提交、推送、发布或安装真实手机。后续先读`docs/testing/v3.2.0-assistant-simplify-implementation.md`，特别保留既有280dp极小窗口测试缺口与真实模型／相册／TalkBack未验收边界。已发布v3.1.0源冻结不改。

## 当前正式版

- **v3.1.0/build24/schema11已正式发布（2026-10-11）**：基于发布前最新main `f7c31a82a8f0ff36057d93c8e11f3bfae67e1422` 的CLEAN构建，标签固定同一SHA，认证/匿名完整下载与Latest回读通过。本轮Debug/Release各1029 JVM、29项模拟器专项及系统320dp/2倍字体复测通过，Release lint 0错误；未安装真实手机，未实测真实图片模型或系统相册。后续开发、安装或发布前读取`docs/testing/v3.1.0-release.md`，遵守其验证边界与schema11前向修复要求。
- **设计边界**：唯一v3.1累积源`design/easy-trip-v3.1.0.pen`沿用上游已保存的262根/43,451节点，本轮只读校验hash/结构；Pencil多会话原生路径复核未完成，未改稿、未宣称全页重新验收。以下f254安装阻塞、v3.0当前发布基线及未发布说明均为阶段历史，不代表本轮发布状态。

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
