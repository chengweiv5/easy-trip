<div align="center">

# Easy Trip

### 在找一个顺手好用的行程规划 App？

把每天去哪、几点到、怎么走，放在一张行程里。

**Easy Trip 只帮你做好旅行规划，不干别的。**

[查看 / 下载最新正式版](https://github.com/chengweiv5/easy-trip/releases/latest) · [看看能做什么](#features) · [从源码构建](#development)

**Android 8.0+ · 无需账号 · 旅行数据保存在本机**

</div>

### 同一家酒店，可以是当天的第一站，也是最后一站

有的 App 不让重复添加地点。可早上从酒店出发，晚上还要回酒店，我就是想把这两段都排进行程，时间和路线才算完整。

Easy Trip 支持**同一地点重复加入行程**。酒店、餐厅、景点，想再去一次，就再排一站；每次到访都可以单独设置到达时间和停留时长。

> 酒店 → 西湖 → 餐厅 → 酒店

从出门到回酒店，把当天每一站和往返交通都规划进去。收藏地点、调整顺序、查看路线，专心把自己的旅行安排好。

> 当前正式版为 **[v1.2.0](https://github.com/chengweiv5/easy-trip/releases/tag/v1.2.0)**，可在发布页下载 APK；下方展示 v1.2.0 的运行效果。

## 旅行计划，不用在清单和地图之间来回切换

先把景点、餐厅和酒店收藏起来，再按天安排。地点顺序、到达时间、停留时长和交通信息放在一起，边看地图，边调整下一站。

<div align="center">
  <a href="docs/images/my-trips.jpg"><img src="docs/images/my-trips.jpg" width="280" alt="我的旅行：以杭州、苏州为例，查看待出行旅行、行程准备度并创建新旅行"></a>
  <a href="docs/images/map-itinerary.jpg"><img src="docs/images/map-itinerary.jpg" width="280" alt="地图与每日行程同屏：地点序号对应，路线段显示交通方式、距离与预计耗时"></a>
</div>

**我的旅行**（v1.2.0 源码运行截图） · 管理下一次出发，也保留已经走过的旅程。

**地图行程工作台**（v1.2.0 源码运行截图） · 上方看地图，下方排日程，序号对应每个地点。

*以上为模拟器中的中国地点示例：首页展示杭州、苏州，地图展示杭州西湖周边行程。点击图片可放大查看。*

<a id="features"></a>

## 能帮你做什么

| 规划时的小麻烦 | Easy Trip 的做法 |
| --- | --- |
| 想去的地方很多，还没决定哪天去 | 搜索并收藏到地点池，按城市整理；加入行程时筛选已排入、未排入的地点，支持批量选择。 |
| 看了清单，还是不知道地点在哪 | 地图与行程同屏，地点名称和序号对应；按日期区分颜色，可切换单日或全程。 |
| 临时想换顺序，或给某一站多留点时间 | 拖动调整地点顺序，移到其他日期；紧凑滚轮支持上午／下午快捷切换，以半小时选择到达时间、以小时设置停留时长。 |
| 不确定两站之间怎么走、要多久 | 高德提供路线信息，显示距离与预计耗时，支持步行、打车、驾车和公交方式。 |
| 旅行日期过了，却不代表真的去过 | 待出行与已出行由你手动标记；多趟旅行分别管理。 |
| 旅途中没网络，也想查看计划 | 已保存的旅行、地点和行程可离线查看、编辑；地图、搜索与新路线计算需要网络。 |

### 重要的备注，抬眼就能看见

门票预约、入园提醒、酒店入住安排，都可以留在对应地点旁。行程与地点池用暖棕色区分备注，默认显示一行，长内容可原位展开、收起。行程中的切换入口放在停留时长右侧，备注展开前后保持同宽；地点池的入口与备注同行，都不额外增加按钮行。全程合并连续重复地点时，也会保留各次到访的备注。

<img src="docs/images/itinerary-note.jpg" width="520" alt="v1.2.0 备注展开示例：在西湖行程下直接查看预约、入园、日落和返程提醒">

*运行截图的局部细节，仅裁去了下方空白。*

## 三步，安排一次出发

1. **创建旅行**：起个名字，设置日期或天数，选择灵活出行或自驾。
2. **收藏地点**：搜索想去的景点、餐厅、酒店，放进地点池，按城市整理。
3. **排入每天**：选中地点加入行程，调整顺序、时间和交通方式，在地图上检查安排。

路线规划提供地点之间的交通参考，行程顺序由你决定。若使用中遇到问题，欢迎[提交反馈](https://github.com/chengweiv5/easy-trip/issues)。

<a id="development"></a>

## 开发与构建

使用 Kotlin、Jetpack Compose、Room 和高德地图 Android SDK。

### 开发环境

- Android Studio（支持 Android Gradle Plugin 8.9）
- JDK 17
- Android SDK 36 / Build Tools 36.0.0
- 最低系统版本 Android 8.0（API 26）

### 高德地图配置

在项目根目录未跟踪的 `local.properties` 中配置 Android SDK 路径和高德 Key：

```properties
sdk.dir=/path/to/Android/sdk
AMAP_API_KEY=your-amap-android-key
```

高德控制台中的包名必须为 `com.yangchengwei.easytrip`，并应登记对应 debug/release 签名 SHA1。不要提交 `local.properties`、keystore 或密码。

### 构建与测试

```bash
./gradlew test lint assembleDebug
./gradlew connectedDebugAndroidTest
```

安装调试包：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

物理真机发布验收参见 [`docs/testing/v1-device-checklist.md`](docs/testing/v1-device-checklist.md) 和 [`docs/testing/v2-device-checklist.md`](docs/testing/v2-device-checklist.md)。

### 正式版本构建

正式 APK 与 SHA256 校验文件发布在 [GitHub Releases](https://github.com/chengweiv5/easy-trip/releases)。

构建前，在不受版本控制的 `release-signing.properties` 中配置：

```properties
RELEASE_STORE_FILE=/private/path/easy-trip-release.jks
RELEASE_STORE_PASSWORD=your-private-password
RELEASE_KEY_ALIAS=easy-trip
# 私钥密码与密钥库不同才需要填写
# RELEASE_KEY_PASSWORD=your-private-key-password
```

也可以通过同名环境变量注入配置；环境变量优先。限制该文件的本机读取权限并在仓库外备份签名材料，后续版本沿用同一证书。`local.properties` 中的高德 Key 必须已登记正式签名 SHA1。

```bash
./gradlew :app:assembleRelease :app:testReleaseUnitTest :app:lintRelease
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
```

从已提交且工作区干净的代码构建发布包，并检查 APK 内版本、`debuggable=false`、`GIT_SHA` 和 `SOURCE_STATE=CLEAN`。签名或高德配置缺失时 Release 构建会失败。上传后重新下载 APK 并校验 SHA256。

正式包与 Debug 包签名不同，不能直接覆盖安装；应用数据只保存在本机且尚无导出功能，卸载会丢失旅行数据。已安装 Debug 包的设备请保留原安装，先在其他设备体验正式包。后续正式版可以使用同一签名升级。

## 工作台交互

- 抽屉分为“地点池 / 行程”两区，搜索入口位于地图底部，搜索结果可直接聚焦地图。
- 地点池按城市组织，筛选结果与地图联动；加入行程时可搜索并筛选已排入、未排入的地点。
- 行程支持单日与全程视图，地图同步显示对应日期的地点与路线；全程会合并连续重复地点。
- 支持标准、卫星两种地图图层，图层选择保存在本机；提供定位与指北针。
- 地图支持手势缩放，切换抽屉高度不会重置手动视角；可按需要扩大地图或行程的可见区域。

## 数据与隐私

- 旅行、地点、行程及路线缓存只保存在本机 Room 数据库。
- 地图、POI 搜索和路线规划仅在用户同意高德隐私政策后启用。
- 未授权或离线时仍可查看和编辑已保存的本地旅行内容。

## 暂未支持

- 账号、云同步和多设备恢复
- 多人协作
- 分享和导出
- 自定义地图落点
- 酒店、门票或餐厅预订
- 实时导航
- 费用管理
- iOS 或其他跨平台客户端
- 自动优化地点顺序
- 后台持续定位
