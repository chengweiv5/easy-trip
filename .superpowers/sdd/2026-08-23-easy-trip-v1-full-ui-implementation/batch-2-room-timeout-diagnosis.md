# Batch 2 Room Timeout Diagnosis

## 结论

**FIXED / GREEN**

`RoomSavedPlaceRepositoryTest.usageCountsRefreshWhenOnlyItineraryItemsChange` 的失败是测试调度/时序问题，不是生产 `Flow` 缺陷。

单一根因假设：测试在 `runTest` 的虚拟时间调度器上执行 `withTimeout(5_000) { next.await() }`，但 Room 的 invalidation、查询和 Flow 发射由真实后台 executor 推进。测试调度器在真实 Room 工作完成前直接把虚拟时间推进到 5 秒并取消 collector，因此 wall-clock 仅经过约 17–120 ms 就报告“5 秒虚拟时间超时”。测试单独运行时后台工作足够快，故通过；和同类/完整目标测试共同运行时，真实 executor 调度稍慢，稳定触发竞态。

最小修复建议：仅修改测试，把等待区切换到真实 dispatcher 后再使用 timeout，沿用仓库已有 working pattern：

```kotlin
withContext(Dispatchers.Default.limitedParallelism(1)) {
    withTimeout(5_000) { next.await() }
}
```

生产 DAO、repository 和 Room invalidation 查询无需修改。

## Phase 1：失败与复现

### 原始 Batch 2 gate

命令：

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchContentTest,com.yangchengwei.easytrip.place.ui.PlacePoolFlowTest,com.yangchengwei.easytrip.workspace.MapLayerFlowTest,com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepositoryTest
```

结果：22 个测试中 1 个失败；失败用例为目标测试。完整异常明确写明：

```text
kotlinx.coroutines.TimeoutCancellationException:
Timed out after 5s of _virtual_ (kotlinx.coroutines.test) time.
To use the real time, wrap 'withTimeout' in
'withContext(Dispatchers.Default.limitedParallelism(1))'
```

堆栈从 `TimeoutCoroutine.run` 进入 `TestDispatcher.processEvent` 和 `TestCoroutineScheduler.tryRunNextTaskUnless`，最终映射到表达式体测试入口 `RoomSavedPlaceRepositoryTest.kt:82`。源码中的实际超时边界是 `RoomSavedPlaceRepositoryTest.kt:96` 的 `withTimeout { next.await() }`，不是 `insertItem`：插入调用位于 `RoomSavedPlaceRepositoryTest.kt:90-94`，只有它返回后才会进入该等待断言。

原始 XML 记录 wall-clock 测试耗时仅 `0.017s`，logcat 中开始到失败约 `12ms`，与“5 秒虚拟时间被立即推进”一致，不是实际等待 5 秒。

### 设备

- adb serial：`emulator-5554`
- Android serial property：`EMULATOR37X1X11X0`
- model：`Android SDK built for arm64`
- Android：12
- AVD 报告名：`easy_trip_p60pro(AVD) - 12`

### 单独重复运行

两次均使用：

```bash
./gradlew connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepositoryTest#usageCountsRefreshWhenOnlyItineraryItemsChange'
```

| 运行 | 结果 | XML testcase 时间 | Gradle |
|---|---|---:|---:|
| isolated run 1 | PASS | 0.066s | BUILD SUCCESSFUL in 7s |
| isolated run 2 | PASS | 0.071s | BUILD SUCCESSFUL in 6s |

单独复现率：**0/2（0%）**。因此不是隔离环境下稳定失败。

### 最小环境敏感性验证

重新运行原 22-test Batch 2 命令：**FAIL，1/1**；目标测试 XML wall-clock `0.120s`，仍为同一虚拟时间异常。

另运行整个测试类：

```bash
./gradlew connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepositoryTest'
```

结果：**FAIL，1/1**；5 个测试中仅目标测试失败，异常相同。

结合原始 gate：

- isolated：0/2 失败；
- 整类/目标 suite：3/3 失败（原始 gate、重跑 gate、整类）；
- 已观察总计：3/5 失败（60%），且失败完全由测试运行上下文决定。

这表明问题是稳定的 suite-order/load-sensitive 竞态，而非随机生产查询错误。

## Phase 2：模式对比

### 同文件 working tests

`RoomSavedPlaceRepositoryTest.kt:42-80` 的四个测试都使用 `runTest`，但只在写入完成后调用 Room Flow 的 `first()` 获取当前快照：

- 没有预先启动的长生命周期 collector；
- 没有跨 Room executor 等待 invalidation；
- 没有测试虚拟时钟上的 `withTimeout`；
- 因此均通过，包括目标 suite 中的 4/4。

失败测试独有组合位于 `RoomSavedPlaceRepositoryTest.kt:82-96`：

1. `runTest` 使用测试调度器；
2. `async(start = CoroutineStart.UNDISPATCHED)` 仅同步启动 collector 到首次挂起；
3. insert 显式切到 `Dispatchers.IO`；
4. collector 后续依赖 Room 自己的真实 query/invalidation executor；
5. `next.await()` 外层 timeout 却仍使用 `runTest` 虚拟时间。

`UNDISPATCHED` 只能保证订阅开始，不能让后续 Room invalidation 查询改用测试调度器。

### 仓库内 working Room Flow 模式

- `RoomTripRepositoryTest.kt:259-284` 使用 `produceIn(this)` 和 channel `receive` 串行消费 mutation 发射。
- 其 `receiveUntil` helper 在 `RoomTripRepositoryTest.kt:343-355` 明确先进入 `Dispatchers.Default.limitedParallelism(1)`，再调用 `withTimeout(5_000)`，从而使用真实时间。
- `ItineraryTransactionTest.kt:122-136` 等待 Room observation 结束时采用同一真实 dispatcher 包裹方式。
- `ItineraryTransactionTest.kt:106-119` 的 working invalidation 测试使用 `produceIn`，先接收初始值，再 mutation，再接收更新值，不把真实 Room executor 与虚拟 timeout 混用。

失败测试与这些 working 测试的关键差异只有 timeout 所在 dispatcher，而不是 Room 查询或 repository mapping 结构。

## 数据流跟踪

1. `RoomSavedPlaceRepositoryTest.kt:91-93` 调用 `SchemaItineraryDao.insertItem`，写入 `itinerary_items`。
2. `EasyTripDatabase.kt:10-12` 声明该 DAO insert；Room 完成事务后通知 `InvalidationTracker`。
3. `PlaceDao.kt:55-62` 的 `observeUsageCounts` SQL 同时读取 `saved_places` 和 `itinerary_items`。Room 根据查询表集合订阅两张表，因此 `itinerary_items` 写入会使 Flow 失效并重查。
4. SQL `LEFT JOIN itinerary_items ...` 并 `COUNT(i.id)`，重查后目标 place 的 count 为 1。
5. `RoomSavedPlaceRepository.kt:42-43` 只做纯同步 `associate` mapping，不切 dispatcher、不缓存、不抑制发射。
6. `RoomSavedPlaceRepositoryTest.kt:86-88` 的 collector 等待 map 中目标 id 的值变为 1。

链路完整且在 isolated run 1/2 中均实际走通，直接支持生产 invalidation 和 repository Flow 正常。

## Phase 3：单一假设验证

### 假设

根因是虚拟 `withTimeout` 与真实 Room executor 的调度域不一致；suite 负载下，测试调度器先执行未来的虚拟 timeout，而 Room 后台 executor 尚未来得及完成 invalidation 重查和恢复 `next`。

### 支持证据

1. 异常文本由 coroutines-test 直接标记 `_virtual_`，并明确给出切到 `Dispatchers.Default.limitedParallelism(1)` 的真实时间建议。
2. 堆栈包含 `TestDispatcher.processEvent` / `TestCoroutineScheduler.tryRunNextTaskUnless`，不是 SQLite、Room query 或 assertion 错误。
3. 所谓 5 秒超时在 wall-clock 约 12–120 ms 内发生，证明不是 Room 真正卡住 5 秒。
4. isolated 2/2 通过，证明 insert → invalidation → DAO Flow → repository Flow → collector 可以正确完成。
5. 同一 suite 3/3 失败，说明真实 executor 获得运行机会的时序受前序测试负载影响。
6. 仓库两个 working Room Flow 等待点已使用真实 dispatcher 包裹 timeout，恰好规避同类问题。
7. DAO 查询显式引用 `itinerary_items`，repository 只做无状态 map，未发现生产侧漏订阅或吞发射点。

### 反驳证据与边界

- 不能从 2 次 isolated pass 证明所有设备/Room 版本永不出现生产 invalidation 缺陷；但 suite 失败时的虚拟时间堆栈、亚秒 wall time、以及真实 dispatcher 的既有模式足以定位当前 gate 失败。
- logcat 没有应用级事件日志，无法逐条观察 invalidation callback；本调查按限制未加 instrumentation。但 isolated pass 已是端到端观察，覆盖完整链路。
- 未修改测试进行修复后 A/B 验证；这是用户限定 Phase 1-3 且禁止修改测试文件的结果。最小假设验证采用运行上下文变化：同一二进制 isolated 2/2 pass，suite/class 3/3 fail。

## GREEN 修复与验证

仅修改 `RoomSavedPlaceRepositoryTest.kt`：把 `next.await()` 的 `withTimeout(5_000)` 包在 `withContext(Dispatchers.Default.limitedParallelism(1))` 中，使 timeout 使用真实时间并与 Room 真实后台 executor 协作。生产 DAO、repository 与 Flow 均未修改。

RED 沿用本报告 Phase 1–3 的既有证据，不为制造 RED 再改测试：isolated 0/2 失败，而完整类/Batch suite 3/3 失败，异常均为虚拟时间 timeout。

修复后在 `emulator-5554`（`easy_trip_p60pro(AVD) - 12`）连续验证：

- 完整 `RoomSavedPlaceRepositoryTest`：**3/3 次通过**，每次 5/5。
- Batch 2 目标 instrumentation suite：**2/2 次通过**，每次 22/22。套件包含 `PlaceSearchContentTest`、`PlacePoolFlowTest`、`MapLayerFlowTest`、`RoomSavedPlaceRepositoryTest`。
- `./gradlew testDebugUnitTest lintDebug assembleDebug`：**BUILD SUCCESSFUL**。

这组 GREEN 结果覆盖了此前稳定失败的整类和 Batch suite 调度上下文，支持竞态已由最小测试调度修复消除。

## 判定

- 状态：**FIXED / GREEN**
- 修复前总观察复现率：**3/5（60%）**
- 修复前 isolated 复现率：**0/2（0%）**
- 修复前 suite/class 复现率：**3/3（100%）**
- 修复后完整类连续通过：**3/3**
- 修复后 Batch 2 目标 suite 连续通过：**2/2**
- 分类：**测试调度/时序缺陷**
- 生产 Flow：**未修改，现有证据反对生产缺陷**
- 最小修复：只让 timeout 使用真实 dispatcher。

## 证据路径

- 目标测试：`app/src/androidTest/java/com/yangchengwei/easytrip/place/data/RoomSavedPlaceRepositoryTest.kt:82-96`
- DAO Flow 查询：`app/src/main/java/com/yangchengwei/easytrip/place/data/PlaceDao.kt:55-62`
- repository mapping：`app/src/main/java/com/yangchengwei/easytrip/place/data/RoomSavedPlaceRepository.kt:42-43`
- insert DAO：`app/src/main/java/com/yangchengwei/easytrip/core/database/EasyTripDatabase.kt:9-17`
- working 真实 timeout 模式：`app/src/androidTest/java/com/yangchengwei/easytrip/trip/data/RoomTripRepositoryTest.kt:343-355`
- 另一 working 真实 timeout：`app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/data/ItineraryTransactionTest.kt:122-136`
- 当前失败 XML（整类运行后）：`app/build/outputs/androidTest-results/connected/debug/TEST-easy_trip_p60pro(AVD) - 12-_app-.xml`
- 当前失败 logcat（整类运行后）：`app/build/outputs/androidTest-results/connected/debug/easy_trip_p60pro(AVD) - 12/logcat-com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepositoryTest-usageCountsRefreshWhenOnlyItineraryItemsChange.txt`
- 原始 gate 汇总：`.superpowers/sdd/2026-08-23-easy-trip-v1-full-ui-implementation/batch-2-gate-report.md`
- isolated run 1 XML：`/tmp/room-usage-run1.xml`
- isolated run 1 logcat：`/tmp/room-usage-run1-logcat.txt`
- isolated run 2 XML：`/tmp/room-usage-run2.xml`
- isolated run 2 logcat：`/tmp/room-usage-run2-logcat.txt`

诊断阶段未修改生产文件或测试文件。修复阶段仅修改目标测试的 timeout 调度，不修改生产文件；全程未派生 subagent。
