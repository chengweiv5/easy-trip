# Easy Trip 版本设计索引

## 当前目标：v2.2.0

2026-10-10 已确认并实现数字千分位、空旅行首城市定位与旅行列表统一滚动；地点池编辑行为不改。正式累积源 `easy-trip-v2.2.0.pen`：187有效根+1历史根，继承v2.1.0所有根及未变能力，新增旅行列表下滑状态。实现2.2.0/build18/schema9，已提交并快进推送main，同签名Release已覆盖安装到用户手机；未发布Release或标签。965 Debug JVM、40项专项设备测试及965 Release JVM通过，旧全应用UI验收缺口保留。

- 批准的列表效果稿：`explorations/v2.2.0-trip-list-review.pen`（辅助材料）
- 本轮范围、验收证据及回滚：`../docs/testing/v2.2.0-implementation.md`
- 提交、推送、安装结果见`../docs/testing/v2.2.0-delivery.md`；设计源内的开发验收阶段说明保留为历史节点，本次交付未再改写设计文件。
- 正式设计已在954e工作树由Pencil保存、关闭重开并回读；不能从其它工作树的同名文件开发。

## 历史记录：v2.1.0

v2.1.0/build17 已于2026-10-10发布。冻结源为 `easy-trip-v2.1.0.pen`：186有效根+1历史根，包含花费分区、年月控件、连续进度条和编辑地点三区块。

- 规格：`explorations/v2.1.0-expense-sections-review.md`
- 验证与回滚：`../docs/testing/v2.1.0-implementation.md`
- 发布证据：`../docs/testing/v2.1.0-release.md`
- 原 `easy-trip-v2.0.0.pen` 为已发布历史完整源，保持不变。

## 历史记录：v2.0.0

2026-10-09，用户确认花费大改方案，并将原 v1.9.0 设计目标调整为 **v2.0.0**。此为设计版本调整，不表示实现或发布完成。

| 文件 | 角色 |
| --- | --- |
| `easy-trip-v2.0.0.pen` | v2.0.0历史正式设计源，完整继承已确认的花费大改设计 |
| `easy-trip-v1.9.0.pen` | 改版前冻结设计快照，保留追溯；不代表 v1.9.0 已发布 |
| `easy-trip-v2.0.pen` | 早期历史探索，与当前 v2.0.0 不同；保留原文件，不作为开发基线 |
| `easy-trip-v1.8.3.pen` | 已核验的实现基线设计，原文件保持不变 |

当前 v2.0.0 包括花费一级入口、历年／年度／月度回顾、六类费用、地点多笔与原位录入、地点和费用统一保存，以及既有旅行功能。

- 规格：`../docs/superpowers/specs/2026-10-09-v2.0.0-expense-review-design.md`
- 覆盖与证据：`../.scratch/v2.0.0-expense-review/README.md`
- 开发前以当前工作树实际源码核对实现状态。本节记录当时的v2.0设计调整，当前实现版本以上方v2.2.0说明为准。
- `EASY_TRIP_IMPLEMENTATION_GUIDE.md` 和 `EASY_TRIP_IMPLEMENTATION_PLAN.md` 保留旧探索的映射及历史批次，不能直接作为本次 v2.0.0 的实现计划。
