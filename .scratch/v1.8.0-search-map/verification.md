# v1.8.0 搜索返回地图：开发验证

日期：2026-10-05（Asia/Shanghai）  
状态：DONE，本地实现与验证完成；未提交、未推送、未发布。  
分支：`codex/v1.8.0-search-map`；基线：`c6e06fcb4f972c9a5f9fbd7f421f2e33bf00f7eb`。

## 实现范围

- 当前搜索结果展示后，明确点击搜索按钮或键盘搜索才返回地图；自动联想不退出。
- 返回时隐藏键盘、收起抽屉，保持地点池/行程及选中日期。
- 临时搜索图层展示放大镜标记和名称，不修改 Room 收藏或行程。无效坐标过滤，摘要提示可定位数量。
- 首次返回按搜索结果适配一次；随后抽屉半屏、全屏及拖动仅覆盖地图，不改地图容器、适配区域或摘要坐标。
- 列表恢复原查询与结果；清除仅撤销搜索图层，保留当前相机。
- 搜索结果用 SavedStateHandle 支持的数据传递；不直接从搜索页构造工作台 ViewModel，兼容后退栈重建。工作台快照恢复及实例隔离有单测覆盖。
- 未扩展行程日期默认选中、搜索排序、版本号或发布流程。保留此前设计产物，未继续绘图。

## 验证结果

| 验证 | 结果 | 证据 |
| --- | --- | --- |
| 全量 Debug 单元测试 | 903 通过，0 失败/错误/跳过 | `verification.json`、`evidence/build.log` |
| Debug APK / AndroidTest APK | 构建成功 | `evidence/build.log` |
| lintDebug | 0 errors，54 warnings，1 hint | `evidence/lint.txt` |
| 新功能 UI 测试 | 3 项通过：真实导航/Room、固定抽屉布局、真实高德地图相机 | `evidence/ui-regression.log` |
| 扩展 UI 回归 | 42 项中 39 通过，3 项既有失败 | `evidence/ui-regression.log` |
| 修改前基线对照 | 相同 3 项失败均在 HEAD 独立临时副本复现 | `evidence/baseline-ui.log` |
| 真实地图视觉检查 | 收起/半屏/展开；名称可读、无摘要遮挡；展开后地图相机与摘要坐标不变 | `evidence/collapsed.png`、`half.png`、`expanded.png` |
| OCR | 断桥残雪、雷峰塔、列表、地点池、行程均识别；收起态两地名称均在摘要上方 | `evidence/*-ocr.json` |
| Git diff | `git diff --check` 通过 | `verification.json` |

构建命令：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home \
ANDROID_HOME=/Users/bytedance/Library/Android/sdk \
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
```

使用专用只读 Android 36 模拟器 `emulator-5570`；没有操作物理手机或修改远端。
真实地图测试使用确定性的杭州示例地点，并验证实际高德相机回调；不是假地图截图。

## 基线已有失败

1. `PlaceSearchContentTest.systemBackPublishesCurrentSessionCollectionsThroughSameCallback`：系统返回后回调等待超时。
2. `PlaceSearchContentTest.controlsUseSpecifiedVisualAndTouchBounds`：历史断言期望 46dp，实际 48dp。
3. `WorkspaceSearchTabsTest.workspaceHasTwoTabsAndReadOnlySearchLauncher`：测试以 consent=null 初始化，搜索入口未显示。

以上未混入本次功能修改。首次扩展回归另外发现键盘隐藏时机导致空结果清除按钮点击竞态，已把隐藏键盘和清焦点移到真正返回地图的 effect，并在最终回归验证通过。

## 审查与限制

- 手工审查并修复了跨导航 ViewModel 获取、搜索期间切换日期后清除导致误移相机、底部名称被摘要遮挡等问题。
- 独立标准/规格子审查多次因运行时断流未返回结果；不将其标为通过。最终结论依据源码审查、自动化回归和真实地图检查。
- 高密度结果的原生地图标签仍可能相互邻近；本次未实现聚合、重新排序或自定义标签避让。
- 这是开发交付，不代表 v1.8.0 正式发布或物理真机验收。

## 备份与回滚

修改前源码备份：`/tmp/easy-trip-v1.8.0-search-map-dev/before/app/src/`；任务状态补充备份：同目录上一级的 `TASK_STATE.before-completion.md`。

回滚时先另存当前工作区，再仅对本次修改的既有源码从 before 目录逐文件恢复，将新增 `WorkspaceSearchResults.kt`、`WorkspaceSearchSummary.kt` 及对应新增测试移到工作区外的备份目录。不要执行整仓 reset/clean，不覆盖此前 Pencil 设计或其他工作。可通过 `verification.json` 中的改动文件及哈希确认范围。重新运行上述构建验证。

通知及临时进程清理结果记录于 `verification.json`。

## 后续提交、推送与安装

2026-10-05，用户授权提交、推送和安装。上述“未提交/未推送”及模拟器记录是开发验收时的快照，不代表后续交付状态。

- 沿用 1.7.0/code9，不创建 GitHub Release 或标签。
- 获取远端最新 main，以 rebase 同步，确认祖先关系后仅快进推送到 `origin/main`，再回读提交哈希。
- 使用正式签名构建并验证 Release APK，与设备旧包证书核对后仅执行 `adb install -r`；不卸载、不清除用户数据。
- 设备原 APK、源码提交前备份位于 `/tmp/easy-trip-search-map-delivery-20261005/before/`。同版本且同签名的旧 APK 可用于覆盖回滚；不使用强制降级或卸载。
- 交付结果、Release 测试、APK 哈希、设备回读及通知保存在本地忽略目录 `delivery/`，最终汇总为 `delivery/verification.json`。设备详情与聊天通知原始回执不提交到公开仓库。
