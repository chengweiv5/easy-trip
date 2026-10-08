# 当前城市重试修复验证

日期：2026-10-08

工作树：`/Users/bytedance/.codex/worktrees/1d7c/easy-trip`

分支：`codex/v1.8.1-current-city-fix`

修复前基线：`d815ce9`

证据目录：`/Users/bytedance/.codex/artifacts/easy-trip/v1.8.1-current-city-20261008/`

## 结果

- 全量 Debug 单测：924 项，0 失败、0 错误、0 跳过。
- UI 专项：7 项通过，包含新增的顶部搜索、键盘搜索及“重试定位”恢复测试，以及原有搜索、加载/失败、收藏、拒绝授权行为回归。
- `lintDebug`：0 Error/Fatal，54 Warning、1 Hint；不是零警告。
- `assembleDebug` 与 `assembleDebugAndroidTest`：成功。
- `git diff --check`：通过。
- GitHub 待合并 PR：0；Codebase 远端返回 `NotFound.Repository`，该远端未能核验。
- 无生产 UI/设计文件变化，没有创建或宣称完成 v1.8.1 设计快照。

## 红绿证据

1. `eachExplicitSearchRetriesFailedLocationWithoutRequiringAppRestart` 在原代码失败：主动点击搜索后定位请求计数仍为 1，而不是期望的 2。修复重试入口后通过。
2. `explicitSearchKeepsSuccessfulLocationAndCityUntilTheyExpire` 在中间实现失败：成功城市被主动搜索重复解析。修复为只清除失败缓存后通过。
3. 补充正在定位时重复点击的去重、坐标成功但城市解析失败的恢复测试，全部通过。

命令：

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

UI 测试仅运行在本任务新建的独立 Android 36 模拟器 `emulator-5562`；结束后已关闭。没有操作其他任务的模拟器。

## 手机与诊断边界

- 正式包未覆盖：仍为 `1.8.0 / 10`，首次安装时间与最后更新时间保持不变。
- 诊断曾复现真实错误 4；随后独立 SDK 请求及原正式包搜索恢复，不能证明底层连接问题永久消失。
- 使用独立包名的临时诊断测试包，结束后已移除；设备原有测试包保留。
- 没有卸载正式应用、清除业务数据、改网络设置或改定位权限。
- 这次代码没有安装到物理手机；真实入口回归使用可控定位失败注入，不代表所有网络环境都能获取定位。

## 主要证据

- `device-retry.json`：真实手机重复失败。
- `real-sdk-before.log`：独立 SDK 诊断通过。
- `device-after-diagnostic.json`：未安装修复时的搜索恢复。
- `red-search-submit.log`、`red-success-cache.log`：两轮红测试。
- `green-search-submit.log`、`green-success-cache.log`：针对性绿测试。
- `full-validation.log`：全量单测、lint 与构建。
- `ui-location-recovery.log`：3 项新增 UI 回归。
- `ui-search-regression.log`：7 项完整专项回归。
- `verification.json`：验证摘要与代码、产物 SHA256。

## 首轮交付与回滚

首轮结束时仅有本地源代码修复，应用版本号仍为 `1.8.0 / 10`；尚未推送、创建发布标签或发布 APK。该状态被下方追加授权的交付流程接续。

修改前文件保存在证据目录 `before/`。本次代码补丁保存为 `code-changes.patch`，可先执行 `git apply --reverse --check` 检查，再反向应用该补丁，仅撤回本次代码修改；如存在后续重叠修改，应停止并手动核对，不强制还原。新增任务文档可独立保留或撤回，不涉及用户业务数据。

## 追加授权：提交、推送、安装

2026-10-08 用户明确授权“提交推送，安装”。目标安装版本为 `1.8.1 / 11`，沿用正式包签名；不创建 GitHub Release 或发布标签。交付前复核 6 个源代码/测试文件 SHA256，与上述已验收版本完全一致；远端 `main` 仍为 `d815ce9`，GitHub 待合并 PR 为 0。

本轮证据目录为前述证据目录下的 `delivery/`。交付前已备份构建配置和任务文档；计划从干净提交执行 Release 单测、lint 和构建，核验签名与版本后快进推送，再用 `adb install -r` 覆盖安装。安装完成前不将本段计划记作交付成功，最终结果另行追加。

推送后源码回滚使用新的 revert 提交，按快进规则交付；已升级手机如需回滚功能，应以相同签名、递增 versionCode 的修复包覆盖安装，不卸载、不清除数据、不强制降级。

### 交付结果

- 功能与版本提交：`d24214bb93dbb3c4919924d2ba25cfdb97855965`，已从功能分支快进推送到 `origin/main`，远端 `refs/heads/main` 回读匹配。后续文档提交不改变安装包源码。
- 从该干净提交执行 `testReleaseUnitTest lintRelease assembleRelease` 成功：924 项 Release 单测通过，0 失败、0 错误、0 跳过；lint 0 Error/Fatal、52 Warning、1 Hint。
- 安装包：`delivery/easy-trip-v1.8.1-release.apk`，62,389,357 字节；版本 `1.8.1 / 11`，`BUILD_TYPE=release`、`DEBUG=false`、`SOURCE_STATE=CLEAN`，内嵌提交为上述功能提交。
- APK SHA256：`134f0053bfa4c584ef4185ba0212cf5c1a6d7073c21f5aa3e0b84eb9f392c32c`。
- 签名证书 SHA256：`a12b60ce83aaacd76899a66d81d78f3fc4cfe3b7424f9a5cf7479732c415048e`，与手机安装前 APK 独立比对一致；16 KB zipalign 校验通过，包内无私有签名配置或密钥文件。
- 已执行 `adb install -r` 覆盖安装到连接的华为 `ALN-AL00` 手机，返回 `Success`。版本回读为 `1.8.1 / 11`，最后更新时间为 `2026-10-08 22:14:01`；UID、首次安装时间、数据目录与升级前一致。
- 安装后回拉 APK，与构建包 SHA256 完全一致。冷启动返回 `Status: ok`，首页原有青州旅行仍可见；本次启动进程日志未发现 `FATAL EXCEPTION` 或 `Fatal signal`。未对所有业务数据逐行比对。
- 真机搜索复查时前台切换到其他应用，立即停止触控；此项未完成，不宣称真机搜索重试已验收。重试逻辑仍以上述 7 项模拟器 UI 回归为证据。
- 未创建发布标签或 GitHub Release，不将设备安装称为公开发布。

交付证据包括 `delivery/artifact.json`、`build-release.log`、`remote-code-main.txt`、`install.txt`、`device-before.json`、`device-after.json`、`launch-after.txt`、`ui-after-home.json` 以及回拉的安装前后 APK。最终文档推送和通知回读另存于同一 `delivery/` 目录。
