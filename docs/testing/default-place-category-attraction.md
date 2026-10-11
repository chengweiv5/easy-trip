# 新收藏地点默认景点

日期：2026-10-11。状态：实现与专项验证完成；未安装手机、未提交、未推送、未发布。

## 范围与实现

- 用户确认：仅新收藏默认“景点”；已有分类保持不变，不升级数据库。
- 当前工作树：`/Users/bytedance/.codex/worktrees/6321/easy-trip`。
- 功能分支：`codex/default-place-category-attraction`，基线 `bbdcb1f`。
- 修改前获取 `origin/main`，确认与当前基线一致；GitHub 待合并 PR 列表为空。
- `RoomSavedPlaceRepository.save` 仅在插入新收藏时显式传入 `PlaceCategory.ATTRACTION.storageKey`。同旅行重复收藏返回已有记录，不重写分类、备注或标签。
- 不改实体/数据库默认值、schema10、9→10迁移、未知分类兼容、历史费用、版本号或已发布资产。已存在的“其他”不会自动变为“景点”。
- 费用沿用既有按地点分类首次预选逻辑；不增加费用联动逻辑、不追改历史费用或恢复草稿。

## 验证

1. 新增默认值回归测试在未修改实现时失败：`expected:<ATTRACTION> but was:<OTHER>`；实现修改后通过。
2. `RoomSavedPlaceRepositoryTest` 与 `PlaceCategoryMigrationTest`：只读模拟器 `emulator-5590` 上共 **17 项通过**。包含五种已存分类及备注/标签在重复收藏后保持一致、同一 POI 在不同旅行中新收藏默认景点、旧数据库迁移后仍为其他。
3. `:app:testDebugUnitTest`：**981 项通过，0 失败、0 错误、0 跳过**。
4. `:app:lintDebug`：**0 错误、55 警告、1 提示**。
5. `:app:assembleDebug`、`:app:assembleDebugAndroidTest` 构建通过；`git diff --check` 通过。
6. 数据库定义、导出 schema、实体默认值、分类读取兼容及应用版本与基线逐文件一致。

验证使用现有 AVD 的 `-read-only -no-snapshot-save` 实例；只向该模拟器安装测试包，没有操作连接的真实手机。完整回归构建日志出现 Gradle 本地 socket 连接异常，但最终 `BUILD SUCCESSFUL`，XML 报告独立确认全部测试通过；不据此宣称运行环境无警告。

本次没有执行 Release 构建、真实地图在线搜索端到端、手机安装或全应用 UI 回归，历史验收缺口保留。

## 正式设计

- 通过 Pencil MCP 回写当前工作树 `design/easy-trip-v2.2.0.pen`，不编辑其他工作树或历史版本源。
- P01 复用既有“景点”选中态，明确“新收藏默认景点”；P01b 保留旧收藏“其他”选中态；I11b 保留“其他”地点的费用预选。
- 更新分类规则与版本状态，明确这是已发布基线上的未发布修订。
- 原生保存、关闭、按当前工作树绝对路径重开及 MCP 回读通过；原生图层名称、context、尺寸属性可编辑。
- 保持 **198 根（197 有效、1 历史）/40,607 唯一节点**；仅改 10 个名称/说明/状态字段，节点、顺序、引用、组件及视觉属性保持。
- 对修改相关规则和两个分类状态进行了结构及截图检查，无新增裁切或布局问题；不是全量界面重新验收。
- 保存后 SHA256：`6da94c437c696114b61071682f00a99f82bdd97a85fedbe4fcc8f381fc186de6`。

## 备份、证据与回滚

证据根：`/Users/bytedance/.codex/artifacts/easy-trip/default-place-category-20261011-6321/`。

- `before/` 与 `before.json`：修改前源码、测试、设计、任务状态备份及 hash。
- `red-test.log` / `green-test.log`：默认值测试红/绿证据。
- `device-regression.log` / `regression-build.log` / `tests.json`：设备、JVM、lint及构建证据。
- `design-diff.json` / `verification.json`：设计最小差异与最终校验结果。
- `completion-readback.json`：punk-12 完成通知回读结果。

回滚仅撤销本次变更：移除新收藏插入时显式传入的 `category` 参数及其注释/导入，撤销对应新增测试与断言；通过 Pencil 将 10 个设计字段按 `design-diff.json` 的 `before` 值恢复，并移除本次新增报告与任务状态段落。恢复前核对文件是否有后续改动，不整文件覆盖后续工作，不使用 `reset --hard` 或强制推送。

没有数据迁移需要回滚；如果之后已使用此修改收藏了地点，回滚代码也不会批量重分类这些地点。
