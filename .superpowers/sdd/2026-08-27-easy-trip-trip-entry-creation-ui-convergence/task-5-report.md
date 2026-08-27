# Task 5 报告

## 状态

完成创建旅行页、日期选择器、IME 滚动与返回锁收敛。

## 实现

- 拆出无状态 `CreateTripFormFields`，保留本地 DatePicker overlay 状态。
- 统一 44dp 返回、48dp 提交、52dp 输入框、66dp 日期行。
- 日期模式显示开始日期与 inclusive 结束日期；无法计算时不显示结束日期。
- DatePicker 显式使用 Material 主题中的 Forest Sage 色板；dismiss 仅关闭弹层。
- 名称、天数、日期错误同时提供邻近错误文字、`isError` 与 error semantics。
- submitting 时禁用顶部返回、字段、日期模式、出行方式、日期选择器和提交按钮，并消费系统 Back。
- 页面主体改为带 safe drawing 与 IME padding 的单一滚动区域；Planning Tip 使用内容高度和 12dp × 11dp 内边距。
- Route effect collector 使用 `rememberUpdatedState` 保持回调新鲜度。

## TDD 与验证

- RED：`CreateTripContentTest` 初次运行 9 项中 7 项失败，覆盖缺失尺寸、结束日期、picker 标记、错误 semantics、提交锁与窄屏滚动。
- GREEN：`CreateTripContentTest` 10/10 通过。
- `CreateTripViewModelTest` 通过。
- `compileDebugKotlin` 与 `compileDebugAndroidTestKotlin` 通过。
- `graphify update .` 完成；已更新 `graphify-out`。
- `git diff --check` 通过。

## 关注点

- DatePicker 色彩由显式 `DatePickerDefaults.colors` 绑定主题 token；仪器测试验证弹层存在与 dismiss 草稿保留，颜色值本身由代码审查确认。
- 未改领域模型、日期管理文件、`diagrams/` 或 `.pen`。
