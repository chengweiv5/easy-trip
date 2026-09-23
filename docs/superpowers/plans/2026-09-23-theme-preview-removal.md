# Theme Preview Removal Implementation Plan

**Goal:** 主题配色页不再显示虚构旅行内容，保留整页即时预览。

**Architecture:** 删除展示用 ThemePreviewCard 及上方标题。状态管理、持久化、导航和主题列表不变；五套 Pencil 选择页与说明同步。

**Tech Stack:** Kotlin Compose、Pencil。

## Global Constraints

- 保持功能分支；修改前备份，保留其他任务未追踪文件。
- 低影响删除复用现有测试，不新增镜像断言。
- 不自动推送或发布。

## Task 1: 删除示例区域并验证

- [x] 删除 ThemePickerScreen.kt 中效果预览 Row、ThemePreviewCard() 调用和该私有函数，内容首项为 Text("选择主题", style = MaterialTheme.typography.titleSmall)。
- [x] 同步五套 Pencil 页：WlVUl、uyy9R、QprVu、GAS2e、kCPbS 删除“预览标题”“实时主题预览”节点；导出对应 JPEG。
- [x] 更新 design/v1.6.0-themes.md 和 .scratch/v1.6.0-theme-design/spec.md。
- [x] ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest；在独立只读模拟器执行 ThemePickerTest，检查五套主题和大字截图。
- [x] 保存并回读设计、git diff --check、记录验证与回滚，提交本地变更并通知。
