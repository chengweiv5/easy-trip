# Task 5 报告：标签校验与保存恢复

## 实现

- 新增标签纯函数：trim-only 展示规范化、按 Unicode code point 计数，CJK script 计 2、其他计 1，限制 24 units、8 个标签，并识别空值和重复值。
- Search 与 Place Pool 编辑草稿独立保存 `newTagInput`，通过明确的输入、添加、移除 action 支持逐字输入；逗号不会在重组标签集合时丢失。
- 标签创建成功后自动选中并清空输入；满 8 个标签时仍可移除。
- 保存失败保留 note、已选标签及未提交输入；`CancellationException` 继续抛出。
- 两个 ViewModel 均使用 placeId + generation 忽略旧保存 completion，避免关闭或污染新打开的详情草稿。
- Room 事务在更新 note/cross-ref 前验证非空、24 units、最多 8 个持久化 identity；继续使用 NFKC + Unicode case-fold 去重，并保持 cross-ref 替换和孤儿标签清理在同一事务中。

## RED 证据

- `./gradlew :app:testDebugUnitTest --tests com.yangchengwei.easytrip.place.ui.PlaceTagValidationTest`
  - 失败：`normalizePlaceTagName`、`placeTagUnits`、`validatePlaceTag`、`PlaceTagValidation` 均未实现。
- `./gradlew :app:testDebugUnitTest --tests com.yangchengwei.easytrip.place.ui.PlacePoolViewModelTest --tests com.yangchengwei.easytrip.place.ui.PlaceSearchViewModelTest`
  - 失败：缺少 `UpdateNewTagInput`、`AddNewTag`、`RemoveEditTag` 及 Pool 对应草稿 API。
- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepositoryTest`
  - 失败 3 项：空标签、超 24 units、超过 8 个标签均未被拒绝。

## GREEN 证据

- 指定 JVM 测试：通过。
- `RoomSavedPlaceRepositoryTest`：9 项通过。
- `PlaceDetailPanelTest`：10 项通过，包含逐字输入 `自然,咖啡` 并完整保留逗号的回归。
- `:app:compileDebugKotlin :app:compileDebugAndroidTestKotlin`：通过。
- `graphify update .`：完成；工具报告两个既有源码的部分 AST 解析警告，未涉及本任务文件。
