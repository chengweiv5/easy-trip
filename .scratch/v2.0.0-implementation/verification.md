# v2.0.0 开发交付验证

2026-10-09；当前工作树 `/Users/bytedance/.codex/worktrees/6934/easy-trip`。

## 实现范围

- 六类费用、地点多笔同类费用、第一笔原位输入；时间/停留/备注/费用统一草稿与原子保存、取消与撤销。
- Room 8→9迁移保留旧待分类/零元，地点费用独立ID；再次安排不复制费用，路段仍单金额交通。
- 旅行/花费一级入口，历年/年度/月度、分类、未定日期、按旅行明细；独立编辑/删除，返回保留筛选与滚动。
- 首页费用摘要、工作台更多、单日/全程摘要接入同一费用页面。
- 日期范围、旅行日插入/重排/删除、地点移日，跨年月/未定日期归属采用事务前后快照、回滚后精确确认；过期确认重读，取消不改数据。
- 版本为2.0.0/build14，schema9；未发布Release，不操作个人手机。

## 验证证据

- 最终Debug与AndroidTest构建成功，952 JVM通过；lint 0错误、54警告、1提示。
- 最终费用包33项设备回归全部通过：见delivery日志03-final-expense-device.log；包含首页费用入口、一级导航、内联编辑及日期确认实际操作。
- 日期与行程事务71项设备回归通过；迁移包含旧库及重开。最后菜单关闭顺序修复后，6项导航/菜单设备复核通过。
- 统一65笔设计fixture：全部23233/65笔/7旅行；2025年12246.50、2024年9800、2026年700、未定日期486.50；厦门移日期后2025年11946.50、2026年1000，合计与ID不变。
- 模拟器emulator-5590，API31，390×844dp；read-only/no-snapshot；生产导航和2倍字体交互已验证。
- Pencil原生保存、关闭重开、MCP回读；185有效根+1历史根、0顶层重叠/placeholder；仅context/content状态变更，布局与历史稿不变。
- Task3/4/5/6独立审查发现均修复复核；Spec代理服务不可用，未冒充两轴独立审查全通过。
- 图更新完成，10个解析警告保留，Kotlin编译通过。没有为图警告安装/升级宿主软件。

日志与备份：`/Users/bytedance/.codex/artifacts/easy-trip/v2.0.0-delivery-20261009`；Task1–5各批证据见相邻JSON。

## 未通过项

完整版本UI验收**未通过**，不以专项测试替代：

1. `TripListContentTest.longNamesAtNarrowWidthAndLargeFontKeepActionsReachable`：旧窄屏大字体1项。
2. `WholeTripItineraryContentTest`：旧6项几何、route节点/语义、间距和文本断言。
3. `ItineraryShareScreenTest.shareOpensSystemChooserAndCancelReturnsToPreview`：API31实际android，期待com.android.intentresolver；失败测试未完成取消返回链。

以上8项在本次23项专项复测中仍失败，15项通过；不自动认定均为过时断言。Task3广泛旧timeline另有22项在未改UI基线878d198同样复现，未声称全量回归通过。实体手机与实时高德端到端未验收。

## 回滚

各批保留独立提交。仅经审阅后revert目标提交或从相应before归档限定恢复；先备份后续改动。禁止force-push、整仓库reset、清库或将schema9用户库降级。设计仅恢复本轮备份，不改v1.8.3/v1.9.0历史快照。Git交付使用功能分支、fetch、祖先检查、HEAD:main快进，完整SHA回读单独记录。
