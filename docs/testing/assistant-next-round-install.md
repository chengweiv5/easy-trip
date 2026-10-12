# 助手自动新一轮：手机安装

日期：2026-10-12。用户单独授权“安装”；仅覆盖安装，不提交推送、发布或修改 Pencil。

## 安装结果

- 手机：华为 ALN-AL00（ADB `FMR0224725012307`）。
- **2026-10-12 22:19:00（Asia/Shanghai）**，`adb install -r` 返回 Success。
- 应用：3.2.0 / build25 / schema11；来源为已推送功能提交 `eda833d60b2a7a55aed7fcce01bff59445a8d5ce` 的已验证 CLEAN Release 构建。当前 HEAD `bdc0a25` 仅追加交付文档，应用与构建输入未变。
- 本次安装包 SHA256：`3cc0d0618aec67f45d84dffca792fbcc8b3f2a7d737746a184872274a336ce9d`。
- 从手机重新拉回安装后的 APK，SHA256 与目标包完全一致。
- 安装前后签名 SHA256 相同：`a12b60ce83aaacd76899a66d81d78f3fc4cfe3b7424f9a5cf7479732c415048e`。目标包非 debuggable、16KB zipalign 校验通过。

## 数据与启动核验

| 字段 | 安装前后 |
|---|---|
| UID | 10477，不变 |
| 首次安装时间 | 2026-09-21 08:33:25，不变 |
| 数据目录 | `/data/user/0/com.yangchengwei.easytrip`，不变 |
| 最近更新时间 | 2026-10-12 07:59:07 → 22:19:00 |

没有卸载、清数据或使用降级参数。原 APK 已备份；这不是数据库备份，也不能据 UID/目录不变宣称全库逐项审计。

安装前后手机在桌面，完成安装后启动 Easy Trip：`Status: ok`，COLD，进程存在；该进程捕获的最近日志没有 FATAL EXCEPTION。未点击旅行、发起搜索、调用模型或执行收藏，本轮不宣称“自动新一轮”完整真机界面验收。

开发与Git交付阶段的验证边界见 [开发报告](assistant-next-round.md) 和 [Git交付](assistant-next-round-git-delivery.md)。本轮没有重新运行JVM或模拟器测试，而是核验并安装已验证包。

## 通知与状态

- punk-12消息 `om_x100b63fa87ba84b0c36201a71ec8823` 已独立回读，chat、机器人身份、完整正文及 deleted=false 全部通过。
- 安装设备锁在操作前独占获取，结束按本轮 token 核验释放；未操作其他设备。
- 本轮仅新增安装记录与本地状态，不提交、推送或发布；不因单独安装重写设计源。

## 证据与回滚

本机证据目录：
`/Users/bytedance/.codex/artifacts/easy-trip/assistant-next-round-install-e9e3-20261012/`。

- `phone-before.apk`：安装前APK备份，SHA256 `ac7e88f225e49f5df0b3cd490cdfa10e226d5f3f633f2fd612069a4a494f13bd`。
- `target-eda833d-release.apk`、`phone-after.apk`、`target-verification.json`：目标签名、版本与安装后APK核验。
- `package-before.txt`、`package-after.txt`、`install.log`、`install-verification.json`：覆盖安装及保留数据标识证据。
- `startup.log`、`startup-verification.json`、`startup-pid-logcat.txt`：启动检查。
- `notify-verification.json`、`cleanup-verification.json`、`before-install-docs.tgz`：通知、归属锁清理与文档备份。

如需回退，先评估当前 schema11 数据兼容性并单独确认；优先同签名、更高 versionCode 的前向修复包，不自动降级、卸载或清数据。原APK备份不等于允许直接覆盖回退。安装记录可从文档备份选择性恢复，保留之后的无关修改。
