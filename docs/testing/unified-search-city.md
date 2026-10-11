# 搜索框与助手统一默认城市

日期：2026-10-11。实现与专项验证完成，**未提交、推送、发布或安装真实手机**。

## 范围与基线

- 当前工作树：`/Users/bytedance/.codex/worktrees/e9e3/easy-trip`。
- 功能分支：`codex/unified-search-city`；开始时 HEAD 与已获取的 origin/main 均为 `4eb70523d969af57a31b865ab4f8e31ee1d9c441`，开放 PR 为空。
- 版本保持 **3.2.0 / build25 / schema11**，没有数据库迁移。
- 用户批准修改搜索框与助手的城市来源；地图取景、地点池城市筛选和收藏流程不变。

## 已实现规则

| 情况 | 搜索城市 |
| --- | --- |
| 用户明确指定城市 | 用户指定值优先 |
| 旅行名中只有一个城市 | 始终使用该城市，不等待定位 |
| 旅行名中有多个城市，定位位于其中 | 使用定位城市 |
| 旅行名中有多个城市，定位不在其中或不可用 | 使用旅行名中第一个城市，按标题顺序而非服务返回顺序 |
| 旅行名中没有城市 | 使用定位城市 |
| 旅行名没有城市且定位不可用 | 提示指定城市或开启定位，不退成全国搜索 |

- 搜索框不新增控件，支持明确城市词，例如 `苏州 博物馆`、`苏州的博物馆`、`苏州市博物馆`。`南京路`、`北京路` 等道路名不作为城市覆盖。
- 助手保留城市字段，原文或图片提取内容明确指定城市时，按该地点指定城市查询，不再要求重复确认；模型猜测出的异地城市仍需确认。图片提取结果的准确性仍依赖模型，本轮没有真实模型验收。
- 助手默认值不再读取地点池城市筛选。
- 旅行名或定位上下文变化时可刷新自动默认值；手动填写的城市、已经开始的批次和失败图片重试的原批次城市不被自动覆盖。
- 城市名匹配基于现有高德城市目录，去重并保留标题顺序；定位到区县时支持匹配所属地级市或直辖市。
- 城市目录最多等待 8 秒，超时进入可重试错误；定位最多等待 3 秒。目录失败不等于“标题没有城市”，不得据此悄悄改用定位。协程取消仍按取消处理。

## 实现入口

以下路径均位于 `/Users/bytedance/.codex/worktrees/e9e3/easy-trip`：

- `app/src/main/java/com/yangchengwei/easytrip/place/amap/TripSearchCityResolver.kt`：统一规则和有界等待。
- `app/src/main/java/com/yangchengwei/easytrip/place/amap/TripCitySearchSource.kt`：普通搜索适配，禁止缺城市时全国搜索。
- `app/src/main/java/com/yangchengwei/easytrip/place/domain/ExplicitSearchCity.kt`：搜索框与助手共用的明确城市词判断。
- `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt` 与 `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceRoute.kt`：接入旅行名、定位和上下文刷新。
- `app/src/main/java/com/yangchengwei/easytrip/assistant/PlaceAssistantController.kt`：自动值、手动覆盖、批次与重试边界。
- 城市目录读取复用既有高德 SDK 和隐私同意门控，没有增加依赖、权限或外部服务；地图原有 `firstTripCity` 决策未改。

## 验证结果

证据目录：`/Users/bytedance/.codex/artifacts/easy-trip/city-context-e9e3-20261011/`。

最终构建命令：

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home \
ANDROID_HOME=/Users/bytedance/Library/Android/sdk \
./gradlew :app:testDebugUnitTest :app:testReleaseUnitTest \
  :app:assembleDebug :app:assembleDebugAndroidTest \
  :app:lintDebug :app:lintRelease
```

| 验证 | 最终结果 |
| --- | --- |
| Debug JVM | 1058 项，0 失败 / 错误 / 跳过 |
| Release JVM | 1058 项，0 失败 / 错误 / 跳过 |
| Debug、AndroidTest APK 构建 | 成功 |
| Debug lint | 0 错误，62 警告、1 提示 |
| Release lint | 0 错误，59 警告、1 提示 |
| 模拟器城市专项 UI | 9 项通过 |
| `git diff --check` | 通过 |

- 最终总构建日志为 `final-validation.log`，结果 `BUILD SUCCESSFUL`；UI 最终日志为 `ui-city-final.log`，结果 `OK (9 tests)`。
- JVM 覆盖单城、多城、无城市、失败与慢定位、标题顺序、手动覆盖、明确城市词、行政区匹配、取消、目录超时重试、助手刷新和图片重试。
- Spec / Standards 两轴复审指出的“南京路误识别”和“目录超时停留 Loading”均已修复并补回归；复审无剩余必须修复问题。复审当时只读到了旧 Release XML，随后本轮最终完整构建已重新运行 Release，不能用旧计数替代上表。
- UI 包含助手城市交互 5 项、搜索框明确城市覆盖 1 项、搜索按钮 / IME / 重试按钮定位重试 3 项。
- UI 仅安装到本轮临时只读模拟器 `emulator-5582`（`trail_map_api36`）；使用 `-read-only -no-snapshot-save`，结束后仅关闭该实例，没有改动源 AVD 或操作连接的真实手机。
- 320dp / 2 倍字体为 Compose 测试容器内场景，截图 `assistant-normal.jpg`、`assistant-small.jpg` 已检查文案换行、字段可读与滚动。**不代表全应用系统分辨率、280dp 极小窗口、TalkBack、真实定位供应商、真实模型或系统相册验收**。
- 初次完整构建遇到缺失本地签名配置，补齐被 Git 忽略的本地构建配置后重跑成功；不将失败运行计为通过。没有提交密钥或签名配置。

## Pencil 同步

- 唯一修改源：`/Users/bytedance/.codex/worktrees/e9e3/easy-trip/design/easy-trip-v3.2.0.pen`。
- 使用 Pencil MCP 定向修改；写入前和原生重开后均核验 e9e3 绝对路径，未操作其他工作树设计。
- **265 个根 / 43,295 个节点**保持不变：仅 9 处文字与 9 处 context 修改，没有新增 / 删除节点或布局变更。
- 搜索缺城市、失败提示、助手输入、城市选择、规则 5 个关联画板回读无裁切问题；相关预览已检查。
- 已原生保存、关闭目标文件、重新打开，并通过 MCP 回读最终规则、验证计数与无 placeholder 状态。
- 最终 SHA-256：`eae2044121be599985bb2681cd9f2a5d26c1fc2d7adb02e1b93daaad2537377b`。
- 已发布的 v3.1.0 设计文件与当前 HEAD 内容逐字节一致；没有重写冻结历史源。历史全页验收缺口继续保留。

## 回滚

- 修改前归档：`/Users/bytedance/.codex/artifacts/easy-trip/city-context-e9e3-20261011/before.tgz`，包括原始应用源码、设计、AGENTS 与任务状态；`before-head.txt`、`before-status.txt` 记录干净起点。
- 仅回滚本轮时，先备份后续新增修改，将归档解压到独立临时目录，按最终 `change-manifest.json` 逐文件比较并恢复本轮变更；不要整仓覆盖或重置其他工作。
- 本轮新文件未在原始归档中，回滚时将清单中的本轮新增文件移到独立备份目录；避免误删之后新增的文件。
- `.pen` 应关闭本工作树目标文件后再恢复备份，并通过 Pencil 重新打开核验，避免未保存窗口覆盖磁盘恢复结果。
- 没有线上部署、数据库或手机变更，因此不需要远端或数据回滚。

## 完成通知

- punk-12 完成通知：`om_x100b638e3a2074a0c3bbc1190698893`，2026-10-11 23:12 发送。
- 独立回读核验收件 chat、机器人身份、完整正文及 `deleted=false`；原始发送和回读结果保存在证据目录的 `notification-send.json`、`notification-readback.json`。
