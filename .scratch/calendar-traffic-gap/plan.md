# 日历交通空隙显示实施计划

**目标：**30 分钟及以上空隙显示可读交通文字，不足 30 分钟隐藏日历交通信息，保留真实耗时。

**结构：**纯 Kotlin CalendarTrafficLayout 判断空隙与文字范围；CalendarTraffic 测量并渲染；CalendarGrid 使用实时 trafficDays.events；CalendarContent 移除所有地点卡交通回退。业务投影与保存不变。

## 任务 1：回归与实现

- [x] 在 CalendarInteractionTest 增加真实 2 分钟步行场景，并执行确认失败：

```kotlin
setup(listOf(item(arrival = "15:00", stay = 60), item("b", "16:30", 60)))
compose.runOnIdle { raw.value = raw.value.map { it.copy(legs = listOf(route("traffic", 120))) } }
traffic().assertIsDisplayed().assertTextContains("步行 · 约2分钟")
compose.onNodeWithTag("calendar-incoming-day-traffic", useUnmergedTree = true).assertDoesNotExist()
val bounds = traffic().fetchSemanticsNode().boundsInRoot
assertTrue(bounds.top >= event().fetchSemanticsNode().boundsInRoot.bottom - 1f)
assertTrue(bounds.bottom <= event("b").fetchSemanticsNode().boundsInRoot.top + 1f)
```

- [x] 增加 CalendarTrafficLayout.kt：输出 `CalendarTrafficLayout(start: Int, intervalEnd: Int, labelStart: Float, end: Float)`；输入 `CalendarTransfer, List<CalendarEvent>, List<CalendarTransfer>, labelMinutes: Float`。缺失正区间或目的地点时返回 null。计算从 blockStart 到目的地点与最早其他卡/交通的连续空隙；小于 30 分钟返回 null。若实际区间足够放字，在区间居中；否则在实际区间末尾后留 1dp 分隔放字，要求整体仍落在空隙内。
- [x] CalendarTraffic 测量字形宽高，超宽返回 null；按上述结果绘制原始实际区间与独立文字。CalendarGrid 将实时 events 与 transfers 交给它，移除 incomingRoutes 及 draftIncoming。CalendarContent 移除 pendingIncoming。
- [x] CalendarTrafficLayoutTest 覆盖 29/30 分钟、2 分钟实际区间、占位/零点/其他交通阻挡、未知时刻、跨日和文字容纳边界。Compose 测试覆盖拖动跨阈值、取消、保存、全程与详情可访问；旧断言按新规则更新。

执行命令：`JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/bytedance/Library/Android/sdk ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug`。仪器测试仅在模拟器执行 `CalendarInteractionTest`，不在实体手机上运行会重置状态的测试。

## 任务 2：设备交付与回滚

- [ ] 保存手机当前 APK 与截图，构建与之同签名的 release APK；验证签名相等后通过 `adb install -r` 覆盖安装，不卸载，不清除用户数据。
- [ ] 回读实际地点 3/4：15:00–16:00、16:30–17:30；文字独立显示、地点 4 内无交通行。回读安装版本与构建元数据。
- [ ] 更新验证记录，提交代码。回滚源码使用本修复提交的 revert；回滚手机使用保存的旧 APK 覆盖安装。按 AGENTS.md 通过 punk-12 通知完成。
