# 助手抽屉视角修复：手机安装

日期：2026-10-12。用户单独授权“安装”；本轮仅安装与验证，不提交、推送、发布或修改 Pencil。

## 安装结果

- 手机：华为 ALN-AL00（ADB `FMR0224725012307`）。
- **2026-10-12 23:32:18（Asia/Shanghai，手机包管理器回读）**，同签名 `adb install -r` 覆盖安装成功。
- 应用仍为 **3.2.0 / build25 / schema11**。来源为 `codex/assistant-drawer-camera` 的未提交修复，HEAD `bdc0a253e1b5cc9533c731a09faf4381fa6a1c36`；构建标记为 **DIRTY**，不是已提交正式发布物。
- 已验证 APK 内包含新的 `AssistantViewportOwner` 实现，BuildConfig 的基线 SHA 与 DIRTY 标记匹配。
- 目标 APK SHA256：`cada898f830a6644fef2112801e97fd7f02dc5383b83ca0faa38e4659817ae1c`。
- 安装后从手机拉回 APK，SHA256 与目标完全一致。
- 目标包与原包签名 SHA256 一致：`a12b60ce83aaacd76899a66d81d78f3fc4cfe3b7424f9a5cf7479732c415048e`。非 debuggable，16KB zipalign 检查通过。

## 安装前复验

- 567 个源码／构建输入与开发验收清单哈希一致。
- 重新执行 `assembleRelease testReleaseUnitTest lintRelease`，构建成功，Release JVM **1083 项、0 失败／错误／跳过**，Release lint **0 错误**。
- 本轮没有重跑 Debug 单测或模拟器 UI。前轮 9 项相关 UI（含系统 320dp／双倍字体）及额外历史失败边界仍见 [开发验证报告](assistant-drawer-camera.md)，不能当作本轮真机验收。

## 数据与启动

| 字段 | 安装前后 |
|---|---|
| UID | 10477，不变 |
| 首次安装时间 | 2026-09-21 08:33:25，不变 |
| 数据目录 | `/data/user/0/com.yangchengwei.easytrip`，不变 |
| 最近更新时间 | 2026-10-12 22:19:00 → 23:32:18 |

没有卸载、清数据、降级或数据库迁移。已备份原 APK，但这不是数据库备份；上述标识不变不等于逐项核验所有用户数据。

安装前后手机停留桌面，安装后启动应用：`Status: ok`、`COLD`、进程存在，捕获的该进程日志未出现 FATAL EXCEPTION。未打开旅行、调用模型、搜索或收藏；**本轮未实机验收助手抽屉相机行为**，其生产 UI 到相机命令回归证据来自前轮模拟器测试。

## 证据与回滚

收尾：手机独占锁已按本轮 token 核验并释放；567 个输入与 Pencil 哈希及 HEAD 保持不变。punk-12 消息 `om_x100b63fb92ed04a4c3f1849b7e0e02d` 已独立回读，chat、机器人身份、完整正文及 deleted=false 均通过；本地任务状态在通知验证后写回。

本机目录：
`/Users/bytedance/.codex/artifacts/easy-trip/assistant-drawer-camera-install-e9e3-20261012/`。

- `before-install-docs.tgz`、`source.patch`、`validated-inputs.json`：安装前文档备份与源代码快照。
- `release-revalidation.log`、`input-verification.json`：构建及源码一致性证据。
- `phone-before.apk`：原 APK，SHA256 `3cc0d0618aec67f45d84dffca792fbcc8b3f2a7d737746a184872274a336ce9d`。
- `target-camera-fix-release.apk`、`phone-after.apk`、`target-verification.json`：安装目标和回读一致性。
- `package-before.txt`、`package-after.txt`、`install.log`、`install-verification.json`：安装与数据标识。
- `buildconfig-dex.txt`、`viewport-owner-dex.txt`、签名与 zipalign 输出：包内容核验。
- `startup.log`、`startup-verification.json`、`startup-pid-logcat.txt`：启动边界。

如需回退须先单独确认 schema11 数据兼容性，优先同签名、保持 schema11 的前向修复；不自动卸载、清数据或安装旧包。仅撤销本轮文档时从备份选择性恢复，保留修复代码和此前安装记录。
