# Easy Trip 版本设计索引

## 当前评审目标：v3.0.0 地点助手

**用户已授权并完成首版实现，仍待体验评审（2026-10-11）**：唯一累积源 [easy-trip-v3.0.0.pen](easy-trip-v3.0.0.pen)，从本工作树已核验的 v2.2.0 完整源原生派生；保留198根，UI Kit + A00–A34共36新增根，合计234根（233有效、1历史）。17个地图状态新增地点助手入口，App全局设置新增「助手设置」，原有功能与历史验收缺口保留。

- [第一版功能/UI规格](../docs/superpowers/specs/2026-10-11-easy-trip-v3-first-agent-design.md)：已回写一次多地点全部标记、待核对项独立处理、单项或勾选后确认收藏；图片/分享/其他App操作后续扩展，等待体验微调。批量Kotlin/真实模型/高德/Room已通过专项验证，见[实施记录](../docs/testing/v3.0-agent-implementation.md)；不将小批验证外推为全量验收。
- [14页评审PDF](previews/v3.0.0-agent/export.pdf)：含地图入口、批量主流程与异常、App设置入口和助手设置，仅为Pencil导出预览，不是第二份正式源。
- [真实地图闭环与设计验证](../docs/testing/v3.0-agent-place-map-loop-validation.md)：真实POI/Room/图钉/重启已过；不等于生产Agent已实现。
- 源文件已经原生保存/关闭/从5ac3路径重开，MCP回读与原生可编辑属性核对。全量继承/节点/引用/hash见验证记录；已发布 v2.2 源不变。
- **设置命名澄清（2026-10-11）**：用户确认全局入口与页面名统一为「助手设置」，当前「模型服务」作为页内分区；所有旅行共用，后续其他助手设置在此扩展。已同步 A11、App 设置入口与相关引导文案，不新增未来功能。

首版实现期间未改动本设计源/PDF；保留234根和设计评审状态，作为明早对照稿。当前未发布，不能以此称v3正式验收完成。

## 当前正式版：v2.2.0

**已发布（2026-10-10）**：2.2.0/build19/schema10，发布提交 `3c59ed1f4343a40da33a2ab4e87e21363a92b568`。唯一正式源 `easy-trip-v2.2.0.pen` 已通过Pencil回写已发布状态，再次原生保存、关闭后按776a路径重开、MCP结构回读及原生可编辑性核验。累计198根、40,607唯一节点，无断引用或placeholder；最终SHA256 `c01e3b26e6eb5bcd5ca6fad96eb347cc3cce7f558977a9a3e68c054755801a44`。相对发布提交仅7处版本元数据更新，页面布局不变；历史版本源冻结。[发布页](https://github.com/chengweiv5/easy-trip/releases/tag/v2.2.0)及[发布报告](../docs/testing/v2.2.0-release.md)记录构建、下载校验与已知限制。

### v2.2 开发阶段历史

以下“等待Mac解锁／未提交／未发布”均为对应阶段记录，现已由上方正式发布结果取代。手机此前安装的是build18，本次发布未再次安装build19。

**分类图标精简与快捷修改（2026-10-10）**：基于 `f595f3c` 已实现地点池去重、行程／日历／长图名称前图标、灰色空心图钉及点击即改分类。正式源视觉修改已保存，累计198根；最后4个实现状态说明及关闭重开门禁等待Mac解锁，尚不宣称设计交付完整。代码验证与准确缺口见 [本轮验证报告](../docs/testing/v2.2.0-category-shortcuts.md)。本轮未安装手机、未提交推送、未发布。

**后续手机安装**：用户单独授权后，2026-10-10 21:30 已完成本工作区同签名 Release 覆盖安装和回读；未提交推送、未发布，设计最终保存／重开仍待完成。见 [安装记录](../docs/testing/v2.2.0-category-shortcuts-install.md)。

2026-10-10 已确认并实现数字千分位、空旅行首城市定位与旅行列表统一滚动。实现2.2.0/build18/schema9，已提交并快进推送main，同签名Release已覆盖安装到用户手机；未发布Release或标签。965 Debug JVM、40项专项设备测试及965 Release JVM通过，旧全应用UI验收缺口保留。

**同版地点分类已实现、安装手机并推送main**：776a 工作树基于 `773387f` 完成收藏五分类、统一图标颜色、三模块编辑与新增费用预选。旧收藏／未设置分类统一为“其他”，没有独立未分类；历史费用不追改。前三项能力及空旅行修复保留，版本仍2.2.0/build18，数据库增量升级为schema10。用户另行授权后已于2026-10-10完成同签名Release覆盖安装；978项Release JVM通过，Release lint为0错误/52警告/1提示。功能提交 `fed43b7` 已快进推送origin/main并回读一致，详细记录见下方链接；未发布Release或标签。

**正式设计已回写并验证保存**：`easy-trip-v2.2.0.pen` 保留195根（194有效、1历史）、40,158唯一节点。本次只更新190个实现状态字段；Pencil原生保存、关闭重开及MCP回读通过，SHA256 `ab37e3271a536da556b621d311caf9ba6701ebbbc75de5ea5274b0fe6eb5a39c`。978 JVM与最终68项设备专项通过，lint 0错误/54警告/1提示；全应用历史失败单列，不称全绿。

- [地点分类与费用联动评审说明](explorations/v2.2.0-place-category-review.md)
- [三屏概览](previews/v2.2.0-place-category/overview.jpeg)
- [本批实现验证与回滚](../docs/testing/v2.2.0-place-category-implementation.md)
- [本批手机安装与Git交付](../docs/testing/v2.2.0-place-category-delivery.md)
- [设计确认阶段历史验证](../docs/testing/v2.2.0-place-category-design.md)
- [实现计划](../docs/superpowers/plans/2026-10-10-v2.2.0-place-categories.md)

- 批准的列表效果稿：`explorations/v2.2.0-trip-list-review.pen`（辅助材料）
- 本轮范围、验收证据及回滚：`../docs/testing/v2.2.0-implementation.md`
- 既有提交、推送、安装结果见`../docs/testing/v2.2.0-delivery.md`；属于前三项实现，不是分类功能的开发证据。
- 本批正式源仅操作776a工作树，通过Pencil修改、保存、关闭重开及回读；不能从其它工作树的同名文件开发。

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
