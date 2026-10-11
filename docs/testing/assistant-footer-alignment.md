# 助手收藏底栏对齐修复与地点行间距检查

2026-10-11，e9e3 工作树。用户确认修复底栏白底及“全选”间距，随后追加检查上方地点行间距。**开发与验证完成；未提交、推送、发布或安装真实手机。**

## 范围与基线

- 功能分支：`codex/assistant-footer-alignment`。
- HEAD 保持 `008ad1ce3b852b6670af0575b26149f06c9805c4`；修改前获取的 origin/main 与 HEAD 一致，开放 PR 查询为空。
- 应用保持 **3.2.0 / build25 / schema11**；没有数据库、搜索城市规则、模型、地图业务或收藏写入逻辑修改。
- 修改前已备份原文件及哈希。证据目录：
  `/Users/bytedance/.codex/artifacts/easy-trip/assistant-footer-e9e3-20261011/`。

## 现场与原因

诊断阶段只读抓取了手机当时的助手界面：紫色主题、四项查询失败、收藏按钮禁用。`phone-current.png` 为现场原图，不是本批代码安装后的效果。

1. **白色底板**：`AssistantSelectionBar` 使用 `Surface(color = MaterialTheme.colorScheme.surface)`。该主题的 Surface 为白色，与面板背景不同；Pencil 底栏组件没有独立填充。
2. **全选文字离选择框太远**：旧实现复用 48dp 宽的选择控件，20dp 可见框靠左，文案从整个热区右侧开始，实际空白为 28dp；整组与收藏按钮仅隔 12dp。
3. **上方地点行也偏松**：代码及 Pencil `OxeSy` 均为 48dp 选择区、20dp 靠左复选框、10dp 行间距、28dp 字母标记、再隔 10dp 到地点文字。因此：
   - 可见复选框右缘到字母标记左缘：**38dp**；
   - 字母标记右缘到地点文字：**10dp**；
   - 复选框右缘到地点文字起点总共 **76dp**，其中包含 28dp 标记本身。
   - 查询失败行不显示选择框，但保留同宽占位。本轮只检查地点行，**未修改其布局、点击区域或设计**；源码自动比对确认结果行及 `AssistantSelectionBox` 与 HEAD 完全一致。

## 修复

仅修改 `AssistantResults.kt` 中的底栏：

- 删除独立 Surface，透明容器直接透出当前面板主题背景；文案显式使用 `onSurface`，避免依赖被移除 Surface 提供的内容颜色。
- “全选”改为同一个三态复选控件：选择框、文字和组内留白共用点击行为。
- 20dp 可见选择框与文字间距 **8dp**；整组最小 **86×48dp**；与收藏按钮间距 **16dp**。
- 按设计恢复普通字号 15sp；大字模式保持 12sp 基础字号随系统字体缩放。
- 收藏按钮及未选、半选、全选、保存中禁用、失败重试等业务行为保持不变。

## 测试与视觉核验

### Red → Green

- `red-background.log`：原实现采样为白色 `-1`，未达到紫色面板背景 `-856074`；修复后 `green-background.log` 显示 `OK (1 test)`。
- `red-spacing.log`：原实现框到文字 **28dp**，断言预期 **8dp** 失败；修复后 `green-spacing.log` 显示 `OK (2 tests)`。
- 首次间距测试曾因测试代码读取不存在的 `DpRect.width` 编译失败，改用边界差后重新建立有效 red 证据。该编译失败不算产品回归证据。

### 最终构建

最终命令使用 JDK 21，完整日志为 `final-validation-v2.log`，结果 `BUILD SUCCESSFUL`：

```sh
./gradlew :app:testDebugUnitTest :app:testReleaseUnitTest \
  :app:assembleDebug :app:assembleDebugAndroidTest \
  :app:lintDebug :app:lintRelease
```

- Debug / Release JVM：各 **1069** tests，0 failures / errors / skipped。
- Debug lint：0 错误、62 Warning、1 Hint；Release lint：0 错误、59 Warning、1 Hint。
- 最终模拟器安装包和测试包均拉回比对 SHA256，与上述最终构建输出相同：
  - 应用：`a85859ea07c0778ed5b9b0251478e4a78c2df275a15160e2a2acb7646deb99c9`。
  - 测试包：`8dcddcaa9c5ef14d96cfc1817f9ce98093248610cbb7098afc0c9a965ff72930`。

### 最终界面回归

`final-ui-software.log` 明确返回 **`OK (17 tests)`**：

- `AssistantSelectionBarUiTest` 7 项：五主题底色、普通尺寸间距、320dp / 双倍字体、48dp 最小触控高度及禁用状态。
- `AssistantDirectSaveUiTest` 3 项：点击“全选”文字、三态选择、直接收藏、保存锁定、失败重试、内联修改及窄屏大字。
- `AssistantCityUiTest` 5 项。
- `AssistantUiTest` 2 项：选择不自动保存、窄屏双倍字体下收藏可达。

首次硬件渲染模拟器的扩展回归在旧地图 UI 测试中报 `createContext failed: EGL_SUCCESS`，单独重试仍失败；日志保留为 `final-ui.log` 和 `map-ui-retry.log`。仅重启本任务只读模拟器，以 `-gpu swiftshader` 软件渲染运行同一批测试后 17 项全部通过。没有修改应用来规避地图测试，也不声称已解决所有 GPU 环境问题。

最终 `final-normal.jpg`、`final-small.jpg` 已人工查看：白底消失、选择框与“全选”相邻、收藏按钮未溢出。图片是最终 APK 的 Compose 测试容器截图，分别为 390dp / 正常字体与 320dp / 2 倍字体，不是整机系统字体设置或真机验收。

## Pencil 保存与边界

- 唯一累积源：
  `/Users/bytedance/.codex/worktrees/e9e3/easy-trip/design/easy-trip-v3.2.0.pen`。
- 所有 MCP 文档操作均传入上述绝对路径，原生窗口 URL 独立核验。
- 仅更新 `HCnC3`、`l9BEY`、`wOZ2m`、`Kvm6N` 四处 context，记录现有设计约束及实施状态；没有修改布局、节点、颜色或冻结 v3.1 源。
- 原生保存、关闭、按绝对路径重开后，窗口无 Edited 标记；MCP 回读四处内容及 **265 根 / 43,295 节点** 通过。
- 源 SHA256：`1f417d3aaedd5fc37053ba48fbb3b920f8d465cb2f8302862c3bb6c1aa7e1f65`。
- 一次整文档读取参数错误使该调用自动回滚；已按 Pencil 错误返回的 editId 修正为 visitor 读取并成功重试，没有脚本写入或其他设计工具替代。
- 本轮不覆盖历史全应用、280dp 极小窗口、真实模型/相册/TalkBack验证缺口。未安装或操作真实手机，未操作另一任务的模拟器。

## 证据与回滚

- 修改前：`before-footer-fix.tgz`、`before-manifest.json`。
- 测试：上文各 red/green/final 日志、`validation-counts.json`、APK 回读比对 JSON。
- 设计：`design-verification.json` 证明只有四处 context 改动，v3.1 源不变；原生保存及重开路径见本次工具记录。
- 完成收尾：`final-verification.json` 记录范围、输入哈希、模拟器清理及 `git diff --check`；`notify-verification.json` 记录 punk-12 通知独立回读。
- 本任务 `emulator-5582` 已关闭，设备清单回读确认其消失；punk-12 完成消息 `om_x100b638ea5c634a0c3789ffaf6b5b46` 已回读验证目标 chat、机器人身份、完整正文及 `deleted=false`，任务状态最后写回。
- 如需回滚，将备份解压到临时目录逐项比较，仅恢复本轮底栏代码、测试和文档追加；Pencil context 用 MCP 恢复。新增测试文件仅在确认属于本轮且没有后续修改时移除。不要整树 reset、覆盖后续工作或改动远端。
- 手机及数据库未变，无需手机回滚。提交/推送或安装需另行授权，不能把本次开发完成理解为已部署。
