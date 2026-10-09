# Easy Trip 版本设计索引

## 当前目标：v2.0.0

2026-10-09，用户确认花费大改方案，并将原 v1.9.0 设计目标调整为 **v2.0.0**。此为设计版本调整，不表示实现或发布完成。

| 文件 | 角色 |
| --- | --- |
| `easy-trip-v2.0.0.pen` | 当前唯一正式设计源，完整继承已确认的花费大改设计 |
| `easy-trip-v1.9.0.pen` | 改版前冻结设计快照，保留追溯；不代表 v1.9.0 已发布 |
| `easy-trip-v2.0.pen` | 早期历史探索，与当前 v2.0.0 不同；保留原文件，不作为开发基线 |
| `easy-trip-v1.8.3.pen` | 已核验的实现基线设计，原文件保持不变 |

当前 v2.0.0 包括花费一级入口、历年／年度／月度回顾、六类费用、地点多笔与原位录入、地点和费用统一保存，以及既有旅行功能。

- 规格：`../docs/superpowers/specs/2026-10-09-v2.0.0-expense-review-design.md`
- 覆盖与证据：`../.scratch/v2.0.0-expense-review/README.md`
- 开发前以当前工作树实际源码核对实现状态。当前运行代码仍为 v1.8.3/build13；此设计调整不改 APK 版本号。
- `EASY_TRIP_IMPLEMENTATION_GUIDE.md` 和 `EASY_TRIP_IMPLEMENTATION_PLAN.md` 保留旧探索的映射及历史批次，不能直接作为本次 v2.0.0 的实现计划。
