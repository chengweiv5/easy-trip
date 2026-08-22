# ADR 0001：高德 Android SDK 集成

## 状态

已接受（2026-08-22）。

## 决策

锁定单一合并依赖 `com.amap.api:3dmap-location-search:11.2.100_loc11.2.100_sea9.8.1`，分别包含地图 11.2.100、定位 11.2.100、搜索 9.8.1；不重复引入单独 SDK。API Key 只由 Gradle Properties 从 `local.properties` 读取，并通过 Manifest placeholder `${AMAP_API_KEY}` 注入。

隐私合规采用显式门控：宿主 UI 展示隐私政策时调用 `AmapPrivacyGate.reportPrivacyShown`，拿到用户决定后调用 `reportUserDecision`；只有返回/持有 `consent=true` 才能创建 `MapView`、`PoiSearch` 或 `RouteSearch`。`Application` 启动不调用同意 API，也不创建高德对象。Instrumentation smoke 在 `BeforeClass` 中代表测试用户显式执行 show/agree。

## 已验证能力

在 arm64 模拟器 `Android SDK built for arm64`、Android 12 上执行了联网 smoke：

- MapView：创建成功并执行 `onCreate/onResume/onPause/onDestroy`；未进行物理真机手势缩放检查。
- POI：“故宫博物院”返回 20 条，首条为“故宫博物院”。
- 步行：933 米、746 秒、46 个折线点。
- 驾车：1566 米、540 秒、34 个折线点。
- 公交：SDK 返回成功码 1000 但没有可用路径，适配器转换为结构化 `AmapServiceException(operation=TRANSIT_ROUTE, errorCode=1000)`，没有返回空成功。

## 实际 SDK API 与文档差异

依赖实际编译确认：`RouteSearch(Context)` 和 `PoiSearch(Context, Query)` 构造器会抛 `AMapException`；`WalkRouteQuery(FromAndTo, Int)`、`DriveRouteQuery(FromAndTo, Int, List?, List<List>?, String?)`、`BusRouteQuery(FromAndTo, Int, String, Int)` 均可用，跨城目的地通过 `setCityd(String)` 设置。公交的 HTTP/SDK 成功码不代表存在路线，因此仍必须校验 `paths`、距离、时长和 polyline。

## 限制与风险

- 公交 `originCity` 必填；跨城仅明确支持含火车换乘的方案，不保证任意城市组合可用。
- Key 与 applicationId、签名 SHA1 绑定，配置不匹配会返回 `INVALID_USER_SCODE`。
- 仅完成 arm64 模拟器验证，不得视为物理真机验证；物理真机型号和 Android 版本：未执行/未知。
- 地图对象生命周期与可创建性已自动验证，实际地图瓦片显示、缩放手势和重组行为仍需物理真机 UI 验证。
- 10.x 之后仅支持 armeabi-v7a/arm64-v8a；x86/x86_64 模拟器不适用。

## 资料

- https://lbs.amap.com/api/android-sdk/guide/create-project/android-studio-create-project
- https://lbs.amap.com/api/android-sdk/guide/create-project/get-key
- https://lbs.amap.com/compliance-center/ability/sdk-security-review
- https://lbs.amap.com/api/android-sdk/guide/map-data/poi
- https://lbs.amap.com/api/android-sdk/guide/route-plan/walk
- https://lbs.amap.com/api/android-sdk/guide/route-plan/drive
- https://lbs.amap.com/api/android-sdk/guide/route-plan/bus
