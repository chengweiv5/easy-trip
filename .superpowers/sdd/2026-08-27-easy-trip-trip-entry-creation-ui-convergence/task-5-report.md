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

## Fix round 2

- 新增唯一生产渲染边界 `EasyTripDatePicker`，创建旅行日期弹层统一通过该边界消费 Forest Sage `DatePickerDefaults.colors` 映射。
- 新增真实渲染像素测试，验证 DatePicker 容器色和 selected/today 使用的主色像素；临时删除生产 `colors = DatePickerDefaults.colors(...)` 后测试按预期 RED，容器实际呈现默认 Material 紫灰色，恢复映射后 GREEN。
- 新增 idle 系统 Back 的 `onBack` 新鲜度测试；临时让 effect collector 捕获旧 `onBack` 后按预期得到 `back-1` 而非 `back-2`，恢复 `rememberUpdatedState` 后 GREEN，且新 callback 仅调用一次。
- `CreateTripContentTest` 与 `CreateTripRouteTest` 共 16 项通过；`CreateTripViewModelTest`、`compileDebugKotlin` 与 `compileDebugAndroidTestKotlin` 通过。
- IME 验证边界不变：仅确认真实焦点、可滚动 viewport、关键内容可达和 `imePadding` 结构，不声称仪器环境已观测软键盘实际显示。

## Fix round 3

- DatePicker 颜色测试不再统计整屏主色像素；通过 `isSelected()` 精确定位 selected-day 语义节点，仅截取该节点并断言其容器像素为 `EasyTripPrimary`。
- 测试外围将 Material 默认 primary 覆盖为洋红色；临时删除生产 `selectedDayContainerColor` 映射后，选中日期区域实际变为洋红色并按预期 RED，恢复 Forest Sage 映射后 GREEN。
- 容器背景仍通过 DatePicker 根节点局部像素验证。2027-01 固定测试月份不包含执行当天，因此 today 色仅由生产 palette 契约和代码审查覆盖，不声称完成 today 像素验证。
- `CreateTripContentTest` 与 `CreateTripRouteTest` 共 16 项通过；`compileDebugKotlin` 与 `compileDebugAndroidTestKotlin` 通过。
