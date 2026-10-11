# 统一默认城市：Git 交付

2026-10-11，用户明确授权“提交并推送”。本轮不发布 Release／标签，不安装或操作真实手机。

## 基线与 rebase

- 工作树：`/Users/bytedance/.codex/worktrees/e9e3/easy-trip`；功能分支：`codex/unified-search-city`。
- 23 个待提交文件与上一轮完成清单 SHA-256 全部一致，初始 HEAD 为 `4eb70523d969af57a31b865ab4f8e31ee1d9c441`；GitHub 开放 PR 为空。
- 首次 fetch 发现 origin/main 已前进到 `1eaa409a9ad94b56d86ee01e2db8334bdd36a424`，新增 `2cad33f` 助手首次城市前缀与脱敏诊断，以及对应交付记录。
- 先提交本批成果为 `5660adb`，再 rebase 到最新 origin/main，形成 **`f51612ee30954f62899063cb072e86f933f14513`**。源码和测试自动合并；仅 AGENTS 与任务状态存在文本冲突，已保留双方记录。
- 上游城市前缀、逐项重试与脱敏诊断保留，新默认城市规则叠加生效；地图逻辑不变。应用仍为 **3.2.0 / build25 / schema11**。

## 集成验证

从 rebase 后的干净功能提交运行：

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home \
ANDROID_HOME=/Users/bytedance/Library/Android/sdk \
./gradlew :app:testDebugUnitTest :app:testReleaseUnitTest \
  :app:assembleDebug :app:assembleDebugAndroidTest \
  :app:lintDebug :app:lintRelease
```

- 构建成功；Debug／Release 各 **1069 tests，0 failures/errors/skipped**。
- Debug lint **0 错误／62 警告／1 Hint**；Release lint **0 错误／59 警告／1 Hint**。
- **12 项模拟器 UI**通过：助手城市 5 项、直接收藏 3 项、搜索明确城市与定位重试 4 项。包含 320dp／双倍字体的 Compose 测试容器场景，不等于全应用系统分辨率或无障碍验收。
- 使用临时只读 `emulator-5582`（`trail_map_api36`，`-read-only -no-snapshot-save`），结束后仅关闭自有实例。没有操作真实手机，没有真实模型或高德网络专项。
- `git diff --check` 通过；本批增加内容的凭据模式检查无候选命中。APK、密钥、签名及本地配置未入库；未禁用或修改宿主 Git hook，提交成功不等于独立证明 hook 的全部扫描结果。
- `git range-diff` 已核对应用变更叠加，上游 `AssistantSearchQuery`／`AssistantSearchDiagnostics` 文件保持一致。

## 设计与安装边界

- Pencil 源沿用上一轮已原生保存重开并回读的 `/Users/bytedance/.codex/worktrees/e9e3/easy-trip/design/easy-trip-v3.2.0.pen`。
- 本轮未改设计输入，仍为 **265 根／43,295 节点**，SHA-256 `eae2044121be599985bb2681cd9f2a5d26c1fc2d7adb02e1b93daaad2537377b`；v3.1 冻结源逐字节不变。
- 设计 context 中“未提交／未推送”记载上一轮开发阶段，不代表当前 Git 交付状态。本轮只交付已验证设计，不为状态说明重新改写画布。
- 手机已有上游 `2cad33f` 的安装记录，但**本批默认城市规则尚未安装到手机**；本轮没有重装、卸载、清数据、创建 Release 或标签。
- 真实模型／定位供应商／相册／TalkBack、280dp 极小窗口及全应用历史未验收边界继续保留，不以 Git 推送代替验收。

## 推送与通知

- 功能提交 `f51612ee30954f62899063cb072e86f933f14513`，提交说明：`feat: 统一搜索框与助手的默认城市来源`。
- 推送前再次 fetch，origin/main 仍为 `1eaa409`；祖先检查通过，执行 `git push origin HEAD:main` 快进推送。
- **2026-10-11 23:20:22（Asia/Shanghai）**独立 `git ls-remote origin refs/heads/main` 回读与本地功能提交 SHA 完全一致，当时工作区干净。
- punk-12 通知 `om_x100b638edbdab4a0c27947760bf8bb4` 已发送，独立回读确认目标 chat、机器人身份、完整正文与 `deleted=false`。
- 本文与状态收尾只记录已经发生的事实，不改应用或设计。收尾提交仍执行 fetch／祖先检查／快进推送／独立远端 SHA 回读，最终结果保存在本机 `final-push-verification.json`，不循环写入提交自身。

## 证据与回滚

本机证据：`/Users/bytedance/.codex/artifacts/easy-trip/city-context-git-e9e3-20261011/`。

- `before-git-delivery.tgz`、`source-manifest.json`、`working-diff-before.patch`：Git 收尾前的全部成果。
- `upstream-commits.txt`、`rebased-feature.diff`、`rebased-source-manifest.json`：上游与 rebase 输入核验。
- `rebase-validation.log`、`rebase-test-results.tgz`、`verified-tests.json`、`rebase-ui.log`：最终集成验证。
- `feature-push-verification.json`、`notification-verification.json`：功能推送与通知回读。
- `before-closeout-docs.tgz`：本轮文档收尾前备份。
- 已推送代码如需回滚，应另行确认后从最新 main 创建新功能分支，生成范围明确的反向提交并复验，再快进推送；不 reset 或强推远端、不覆盖上游他人工作。只撤销文档时从备份逐项比较，保留代码与历史交付记录。
