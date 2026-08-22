# Easy Trip v2 真机验收清单

> 自动化和模拟器验证完成后再执行。开发阶段不向真机安装测试 APK。

## 验收环境

- 日期：2026-08-22
- 设备型号：ALN-AL00（序列号 FMR0224725012307）
- Android 版本：12（API 31）
- App commit / 工作树状态：基线 79fb989，V2 未提交改动
- 构建类型：debug

## 检查项

| # | 场景 | 操作与预期 | 结果 | 备注 |
|---|---|---|---|---|
| 1 | 状态视觉 | 检查内容 Tab、地图范围、地图图层、标签；未选透明无边框，选中为主题色实心反白 | 通过 | 真机确认 C 方案：实际高度 32dp、8dp 圆角；创建旅行、设置及全 App 紧凑按钮风格符合预期 |
| 2 | 工作台内容 Tab | 抽屉仅显示地点池、每日行程；地图右下白色放大镜入口打开独立搜索页，入口不可编辑 | 通过 | 搜索与收藏改版真机验收通过 |
| 3 | 搜索聚焦 | 独立搜索页选择结果后清空查询并返回工作台；地图只移动一次，收藏 marker 身份与编号不被高亮覆盖 | 通过 | 搜索与收藏改版真机验收通过 |
| 4 | 地图图层 | 点击图层图标展开标准、卫星（含路网），选择后菜单折叠；MapView 不闪退或重建 | 通过 | 第 4、5、7 组真机验收 |
| 5 | 图层持久化 | 选择图层后切换旅行并强杀重启，仍保持上次图层 | 通过 | 第 4、7 组真机验收 |
| 6 | 初始视野 | 进入旅行后自动展示全部地点池；单地点使用合理缩放 | 通过 | 第 5 组真机验收 |
| 7 | 地点变化视野 | 收藏或删除地点后只适配一次新范围 | 通过 | 第 5 组真机验收 |
| 8 | 手动视角保持 | 手动缩放/拖动后切换内容 Tab、收起/展开抽屉，地图视角不重置 | 通过 | 第 5 组真机验收 |
| 9 | 固定控制区 | 地图在顶部；40dp 圆角顶栏与白色搜索框悬浮于地图内；地图范围控制始终可见 | 通过 | 最新视觉优化真机复验通过 |
| 10 | 抽屉手势 | 不显示收起/半屏/展开按钮；通过抽屉卡片顶部短横线向上滑展开、向下滑收起，三态均可达 | 通过 | 收起稳定不回弹；短横线避开系统底部手势区，真机复验通过 |
| 11 | 行程时间轴 | 地点为独立卡片，交通段位于卡片之间；拖动整卡时视觉清晰，路线段不参与拖动；全程地图地点连续编号且每日路线显示“第一天/第二天”等标签 | 通过 | 第 5、6 组真机验收 |
| 12 | 距离格式 | 小于 1km 显示米，大于等于 1km 显示最多一位小数的公里 | 通过 | 第 6 组真机验收 |
| 13 | 应用图标 | Launcher 普通/圆形 mask 下定位点+路线清晰；Android 13+ 主题图标显示 monochrome | 通过 | 普通图标在 API 31 真机通过；主题图标由自动化覆盖，真机系统不支持 |
| 14 | 性能 | 30 个 marker、7 天路线下缩放、拖动、图层和 Tab 切换无明显卡顿 | 通过 | 第 8 组真机验收 |
| 15 | 收藏切换 | 搜索结果和底图 POI 卡片均可收藏；零引用直接取消，有行程引用时取消收藏先展示影响数量，取消不改数据、确认级联删除 | 通过 | 搜索页与底图 POI 真机验收通过；地图卡片切换收藏时视角保持不变 |
| 16 | Marker 身份 | 地点池显示收藏图标；单日/全程显示准确编号，重复地点按 `1·4·7` / `1 +3` 压缩；聚焦仅增加描边 | 通过 | 真机验收通过；收藏图标即时增删，路线使用高对比冷色/红紫色调色板 |
| 17 | 地点池滚动条 | 短列表隐藏，长列表显示只读 thumb；滚动时跟随，直接拖动 thumb 不驱动列表 | 通过 | 真机验收通过 |

## 自动化基线

```bash
./gradlew clean test lint assembleDebug
ANDROID_SERIAL=<easy_trip_p60pro serial> ./gradlew connectedDebugAndroidTest
```

运行 instrumentation 前必须通过：

```bash
adb -s <serial> emu avd name
```

确认输出严格为 `easy_trip_p60pro`，不得使用其他模拟器或物理真机。

## 搜索与收藏增量自动化

```bash
./gradlew testDebugUnitTest \
  --tests '*WorkspaceTabRestorationTest' \
  --tests '*PlaceSearchReducerTest' \
  --tests '*SearchSelectionConsumptionTest' \
  --tests '*CollectionTogglePolicyTest' \
  --tests '*OccurrenceBadgeFormatterTest' \
  --tests '*MapUiModelMapperTest' \
  --tests '*LazyScrollbarGeometryTest'

AVD_NAME="$(adb -s "$ANDROID_SERIAL" emu avd name | tr -d '\r')"
test "$AVD_NAME" = "easy_trip_p60pro"
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest
```

## 结论

当前状态：**V2 原验收及搜索与收藏增量的自动化、指定模拟器和物理真机验收全部通过。**
