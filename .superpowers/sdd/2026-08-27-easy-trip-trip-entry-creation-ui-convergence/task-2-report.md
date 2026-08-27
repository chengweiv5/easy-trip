# Task 2 Report

## 状态

已完成旅行列表、卡片菜单和空态收敛。

## 实现

- 页头头像改为纯装饰，移除点击、按钮角色和“个人中心”描述。
- 主旅行卡改为白色内容驱动卡片，仅保留 48dp“继续规划”入口。
- 主卡和其他旅行行统一使用 40×40dp 菜单，菜单仅包含设置和危险色删除。
- 菜单展开状态仅保存 tripId，列表重排后仍绑定原旅行。
- 其他旅行整行进入工作台，菜单操作独立分发。
- 空态使用剩余约束居中，低高度时可滚动，不再使用固定 647dp 高度或固定文案宽度。
- 空态采用确认文案与 108dp 插画。
- Loading/Error 保留页头；Error 同时提供重试和创建。

## TDD 与验证

- RED：先替换 `TripListContentTest`，首次运行因新接口与语义尚未实现而失败。
- GREEN：`TripListContentTest` 共 7 项通过。
- `:app:compileDebugKotlin` 与 `:app:compileDebugAndroidTestKotlin` 通过。
- `graphify update .` 完成；既有 `NetworkMonitor.kt`、`RoutePlanner.kt` AST 语法警告未由本任务引入。
- `git diff --check` 通过。

## 关注点

- 已使用 280dp 宽、2× 字体和 900dp 现实可布局高度验证操作可滚动到达，避免 weighted 列表被压缩为 0。
- 本任务未接入或修改删除确认弹窗业务，仅消费 Task 1 的 `RequestDelete(tripId)` action。

## Fix round 1

- 空态测试改为通过 `LocalDensity` 将像素转换为 dp，并以 300dp 低高度真实触发 compact/滚动，验证剩余高度非固定 647dp 且 CTA 滚动后可达。
- 菜单测试通过稳定语义验证仅有两项，并验证删除项使用 danger 语义 token。
- 280dp/2× 字体测试分别检查主卡名称、主卡菜单、其他旅行名称、其他旅行菜单的左右边界。
- 菜单图标改为横向三点。
- RED：增强测试先因缺少菜单语义和文本测试标签失败；最小实现后 7 项全绿。

## Fix round 2

- 为菜单弹层增加稳定容器 tag，测试直接统计该弹层下具备 click action 的真实菜单项，固定为 2 项；新增任何第三个 `DropdownMenuItem` 都会导致失败，无需实现者主动添加 marker。
- 保留设置/删除行为断言与删除 danger 语义断言。
- RED：菜单容器尚未暴露时真实可点击子节点计数为 0；增加容器 tag 后 7 项全绿。
