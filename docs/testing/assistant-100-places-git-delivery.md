# 100 地点：提交与推送记录

2026-10-14，8bc4 工作树。用户授权“修改完之后提交，推送”；本轮只交付 Git，不安装手机、不发布 Release／标签，不再次修改设计。

## 交付结果

- 功能分支：`codex/assistant-100-places`。
- 功能提交：**`3d1d2fb32520e08b0b07e1030beb551e706e4ef6`**，`feat: 将助手识别地点上限扩展到100个`。
- 43 个 Git 差异文件（旧 UI 测试重命名合并计数；备份清单含 44 路径）均与提交前逐字节备份一致；包括代码／测试、已保存 Pencil、19 图／4 页 PDF 及开发记录。
- 开始与推送前两次 fetch 后，远端 main 均为 `9ee79bd3c62d1c9311e0cba2a2a6fe2c92c41867`，无待合并 PR；无需 rebase。
- `git merge-base --is-ancestor origin/main HEAD` 通过后，执行 `git push origin HEAD:main`；**00:42:41 独立 `git ls-remote` 回读 main 等于功能 SHA**。
- 没有 force、merge 或操作 codebase 远端；没有提交本地配置、密钥、签名文件、APK。
- 本报告及状态索引为后续仅文档收尾提交；最终远端 SHA 见下述证据根的 `final-push-verification.json`，不与 APK 构建 SHA 混淆。

## 干净提交重新验证

在功能提交后的干净工作区执行：

```sh
./gradlew testDebugUnitTest testReleaseUnitTest lintDebug lintRelease \
  assembleDebug assembleDebugAndroidTest assembleRelease \
  --max-workers=2 --console=plain
```

| 项目 | 结果 |
|---|---|
| 构建 | BUILD SUCCESSFUL，1m22s |
| Debug／Release JVM | 各 1135 项，0 失败／错误／跳过 |
| Debug lint | 0 错误；64 Warning、1 Hint |
| Release lint | 0 错误；61 Warning、1 Hint |
| APK | Debug／AndroidTest／Release 均成功 |
| BuildConfig | Debug／Release GIT_SHA 均为功能 SHA，SOURCE_STATE=CLEAN |
| 版本／schema | 3.4.0/build27/schema12，不变 |
| Release 基础校验 | 签名验证、16KB zipalign 通过 |
| 验收源码 | 597 个源码／测试／构建输入与上一轮设备验收逐项 hash 一致 |
| 提交检查 | 已保留并正常执行 Git hooks，`git diff --check` 通过 |

干净包 SHA256：

- Debug：`05d0e04827824c77b6da57adaa50e6e79423e8c66af117baed4d4a75d402c829`
- AndroidTest：`25ba2d19789242e593a901c56a15ad61cc98aa5358e3bf03e6ab5362842ef880`
- Release：`cec0b7b7918d83cf8479849db221255b4de0d3a0473b3497172f1bd45ea9e677`

本轮没有重跑设备或真实模型。前轮 19 项设备回归及 2 项系统 320dp／2 倍字体复测，只在上述源码 hash 一致边界内沿用；不是本轮 CLEAN APK 的独立设备运行。100／101 门禁、编号到 CV、恢复容量和未验收边界详见[开发验证](assistant-100-places.md)。

## Pencil 与安装边界

- 仅提交前轮已原生保存重开、定向回读通过的源字节；未重写 Pencil 或导出预览。
- v3.4：305 根／45,243 节点，SHA256 `fbc1dca80b5f6d8d95b0091b3ed72d7ad31f90645db6798b9f73c9bccf421ddc`。
- 冻结 v3.3：`e55030c5ed4d00cd7d0fcef59d448c525a73c2b933c06776d8252a14d61b8225`，未变。
- 图内“未提交”是开发阶段保存的说明，以本 Git 交付记录为准；不为更新阶段状态再次改稿。
- 未访问或安装真实手机，未创建 Release／标签，未改变正式发布状态。真实模型＋高德的 100 地点链路仍未实测，不沿用旧模型数量作为本批验收。

## 证据、通知与回滚

本机证据根：
`/Users/bytedance/.codex/artifacts/easy-trip/places100-git-8bc4-20261014`

- `before/feature-files.tgz`、`before/worktree.patch`、`feature-files.json`：提交前快照。
- `open-prs.json`、`logs/fetch-initial.log`、`logs/fetch-before-feature-push.log`。
- `logs/feature-commit.log`、`feature-commit-verification.json`、`feature-push-verification.json`。
- `logs/clean-validation.log`、`clean-validation.json`、`clean-test-lint-reports.tgz`、`apks/`。
- `notification-send.json`、`notification-readback.json`：punk-12 通知及完整内容／目标／身份回读。
- `logs/docs-commit.log`、`logs/docs-push.log`、`final-push-verification.json`：最终仅文档收尾与独立远端核验。

撤销时从最新 main 建功能分支，选择性反向修改并验证、快进推送，不 reset／force。**如已产生 100 地点待恢复记录，不直接回退至仅支持 30 地点／256KiB 的旧实现**，应保留记录并做前向兼容修复。schema12 不降至 schema11，不自动给手机装旧包。文档回滚参考 `before/delivery-docs.tgz`，保留真实历史和无关后续修改。
