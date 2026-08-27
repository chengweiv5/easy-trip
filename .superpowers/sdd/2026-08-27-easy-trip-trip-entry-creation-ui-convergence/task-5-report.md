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

## Fix round 1

- 表单字段顺序调整为名称、日期（开始日期与 inclusive 结束摘要）、天数、出行方式，并增加布局顺序回归测试。
- 新增 `CreateTripDatePickerPalette` 纯颜色契约；生产 `DatePickerDefaults.colors` 与测试共同消费 Forest Sage 映射，任一 token 映射变化都会使契约测试失败。
- 紧凑窗口测试确认输入框真实获得焦点、viewport 存在滚动语义，提交与 Planning Tip 可滚动到达。测试环境未可靠观测软键盘是否实际显示，因此不声称验证了 IME 显示，仅验证 `imePadding` 结构对应的焦点与滚动约束。
- 新增 Route 层测试：idle 系统 Back 触发返回 effect，submitting 时消费 Back，重组替换 callback 后 workspace effect 调用最新 callback。
- Fix round 1 RED：字段顺序测试在旧顺序下失败；颜色契约测试在契约不存在时编译失败。
- Fix round 1 GREEN：`CreateTripContentTest` 与 `CreateTripRouteTest` 共 14 项通过。
