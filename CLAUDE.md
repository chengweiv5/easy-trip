## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).

## Batch gates

- Batch gates do not require per-screen screenshots or pixel-by-pixel comparison.
- Block a batch only for functional or state errors, data inconsistency, crashes, unreachable flows, severe clipping, or broken key interactions.
- Treat spacing, typography, small visual differences, and minor design-baseline inconsistencies as non-blocking final physical-device acceptance items.
- Screenshots may be retained as diagnostic evidence, but they do not block batch progress.

## Easy Trip 设计实现

设计事实来源：

- `design/easy-trip-v2.0.pen`
- `design/EASY_TRIP_IMPLEMENTATION_GUIDE.md`
- `design/EASY_TRIP_IMPLEMENTATION_PLAN.md`

规则：

- 开始 UI 开发前，先阅读实现指南和实现计划。
- 修改前检查 `git status` 和目标文件 diff，不覆盖无关未提交改动。
- 有 `graphify-out/graph.json` 时，先用 `graphify query` 定位相关代码；修改代码后运行 `graphify update .`。命令不可用时明确说明。
- 使用 Pencil MCP 读取本批涉及的 UI Kit、状态规范、顶层 frame、`context` 和必要截图，不直接全量解析 `.pen` 文件。
- 顶层 frame 的可见内容定义视觉；`context` 定义入口、交互和状态语义。
- 附录引用、流程画板和状态组画板不作为独立页面重复实现。
- 同一页面的多个设计状态由同一个 Composable 和参数化 UiState 表达。
- 复用现有架构、数据模型和组件，不按截图建立平行实现。
- 每批完成端到端业务闭环，不只实现静态布局。
- 每批结束更新 `design/EASY_TRIP_IMPLEMENTATION_PLAN.md`。
- UI 改动必须在模拟器或实体设备实际操作验证；无法验证时明确说明。
- 完成前运行适用的 assemble、unit test、lint 和 Android UI 测试，并如实报告失败或未运行项。
- 未经用户明确要求，不 commit、不 push。
