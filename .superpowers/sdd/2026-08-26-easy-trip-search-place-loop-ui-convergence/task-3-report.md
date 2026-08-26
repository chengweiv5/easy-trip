# Task 3 报告：搜索详情 AMap 与生命周期复用

## 状态

已完成。

## 实现

- 新增 `searchDetailMapModel(candidate, requestId)`：有坐标时只生成一个 `UNSAVED_SEARCH` marker，无路线、无其他 marker，并使用 `SEARCH_FOCUS` viewport；无坐标时不生成地图模型。
- 搜索详情直接复用 `AmapComposeMap`、`AmapMapHost`、既有 consent 与生命周期实现；未复制地图生命周期控制器，未修改 workspace map。
- search destination 从导航边界接收 consent 与 map host factory，因此每个搜索 destination 创建自己的 map host instance。
- 搜索详情 marker 与地图 POI 点击均为 no-op，不改变当前候选。
- `detailMapRequestId` 在普通详情状态变化中保持稳定；仅详情态的 `RecenterDetail` 增加 request id。
- 无坐标、无 consent 或 map host 创建失败时，详情区域及收藏等操作仍可用。
- 地图与详情各占主要区域；有地图时提供“回到选中地点”操作。

## TDD 与验证

先新增地图模型、request id 稳定性、Recenter、地图 click no-op、无坐标/consent 和地图失败测试，并确认因缺失 mapper、状态/action 与地图接入而 RED；随后补最小生产实现并验证 GREEN。

最终命令：

```bash
./gradlew :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleDebug \
  :app:assembleDebugAndroidTest \
  :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchContentTest,com.yangchengwei.easytrip.amap.AmapComposeMapTest

graphify update .
```

结果：JVM tests、lint、Debug APK 与 AndroidTest APK 构建通过；设备 `easy_trip_p60pro(AVD) - 12` 上 28 个 focused instrumentation tests 全部通过；知识图谱已更新。

## 关注点

- 当前默认详情区域提供最小名称、地址与收藏操作；Task 4 可用共享 `PlaceDetailPanel` 替换该 fallback。
- 已通过 Compose instrumentation 在模拟设备上验证关键交互；未做物理真机视觉验收。
- `graphify update .` 报告两个既有源码文件存在可部分抽取的语法错误，并提示 graphify skill/package 版本差异；本次图谱更新仍成功完成。
