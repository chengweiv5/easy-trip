# v1.6.0 全局主题实现验收

状态：DONE。2026-09-23 用户确认设计后，按五套浅色主题实现。分支 `codex/v1.6.0-theme-design`，实现前 HEAD `c1663fb`。

## 实际结果

- 湖畔晴空、松林晨光、落日陶土、山岚暮紫、玫瑰沙丘五个稳定 ID，完整 Material3 颜色角色。
- 首页调色盘与工作台更多菜单共用主题页；全屏 Dialog 保留底层导航、地图与日历状态。
- 选色仅预览；原子文件写入和磁盘回读成功后发布全局主题。保存中拦截选色/重复提交/所有返回，失败后保留旧主题和当前预览，支持重试。
- 启动前同步恢复单个小型偏好文件，未知值回落湖畔；正式 MainActivity 实际选中玫瑰、回读 `rose`、force-stop 后重启显示「已使用玫瑰沙丘」。见 cold-start.json、theme-restored.xml、cold-start-rose.jpeg。
- Compose 界面、空态双层插画与原生地图标记跟随主题。日期色盘、备注语义、错误色、地图底图和长图导出 renderer 保持原样；分享页面外层控件随主题。
- 版本标记 1.6.0 / versionCode 8；未构建 Release、未推送、未发布。

## 验证证据

- `verified-build.log`：Debug、AndroidTest 构建成功；850 项单元测试，0 failures / 0 errors；lint 0 errors（50 warnings，依赖升级、既有 Compose/Android 建议）。汇总 build-summary.json。
- `final-acceptance.log`：8 项主题模拟器用例全部通过。涵盖五套切换/放弃、保存失败重试、保存中返回拦截、320×568dp / 2倍字体、AtomicFile 重建与中断回滚、30组对比度、原生徽标像素、双入口和工作台位置/地图实例保留。
- `final-themes.log`：此前 7 项主题 + 5 项日历页头对齐，12/12 通过。
- `regression.log`：65 项扩展回归，62 通过；3 个失败在未修改 HEAD c1663fb 的独立临时副本中完全复现，见 baseline-regression.log。
  - 系统 ChooserActivity 存在，但当前模拟器包名为 android，旧测试固定预期 com.android.intentresolver。
  - 两个地点池旧用例预期已不再使用的绿色背景/地址蓝色及备注内联状态，与基线实际布局不符。
- 保存失败测试曾使用异步 accessibility 全局返回，偶发在失败回调后才派发。已改用同步 Espresso.pressBack，最终整组 8/8 通过。
- 视觉检查：screenshots-final/themes/ 的五主题首页/选择页、四主题工作台、失败与大字体截图；cold-start-rose.jpeg 检查系统栏、空态插画和按钮。中文 OCR 结果在 ocr.txt。选择行 >=62dp、返回/应用 >=48dp，小屏内容可滚动，应用可达。
- 模拟器 emulator-5596，easy_trip_p60pro 的只读临时实例，未操作物理设备；地图使用测试 host 验证生命周期、配色传递与相机请求不重复，原生标记独立像素检查。未验证在线高德底图。

## 环境说明

自动审批拒绝从另一工作区复制本地地图密钥，已取消该步骤，采用不依赖密钥的地图测试替身。没有复制或发布凭据。

## 回滚

源码修改前归档在 `before/source.tar`。可从归档逐文件恢复（先对照当前 diff，避免覆盖之后的修改），或撤销本任务的本地提交。新增设计、主题源码/测试按文件移除即可。偏好文件与业务数据库分离，不改表结构；旧版忽略该偏好文件。禁止 reset/force push。
