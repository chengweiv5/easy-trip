# Easy Trip

本地优先的 Android 旅行规划应用，使用 Jetpack Compose、Room 和高德地图 Android SDK。

## 开发环境

- Android Studio（支持 Android Gradle Plugin 8.9）
- JDK 17
- Android SDK 36 / Build Tools 36.0.0
- 最低系统版本 Android 8.0（API 26）

## 高德地图配置

在项目根目录未跟踪的 `local.properties` 中配置 Android SDK 路径和高德 Key：

```properties
sdk.dir=/path/to/Android/sdk
AMAP_API_KEY=your-amap-android-key
```

高德控制台中的包名必须为 `com.yangchengwei.easytrip`，并应登记对应 debug/release 签名 SHA1。不要提交 `local.properties`、keystore 或密码。

## 构建与测试

```bash
./gradlew test lint assembleDebug
./gradlew connectedDebugAndroidTest
```

安装调试包：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

物理真机发布验收参见 [`docs/testing/v1-device-checklist.md`](docs/testing/v1-device-checklist.md) 和 [`docs/testing/v2-device-checklist.md`](docs/testing/v2-device-checklist.md)。

## v2 工作台

- 内容抽屉分为“搜索 / 地点池 / 每日行程”三个独立 Tab。
- 点击搜索结果会聚焦并高亮地图标记，详情仍保留在搜索 Tab。
- 支持标准、卫星（含路网）两种图层，选择在本机全局保存。
- 进入旅行、地点集合变化或主动切换地图范围时自动适配视野；Tab 和抽屉变化不会重置手动视角。
- 地图固定在顶部，搜索与地点池/单日/全程范围控制固定在抽屉上方。
- 每日行程使用地点卡片与交通连接段组成的时间轴；距离按米或公里显示。
- Launcher 使用地图定位点与路线主题 adaptive icon。

## 数据与隐私

- 旅行、地点、行程及路线缓存只保存在本机 Room 数据库。
- 地图、POI 搜索和路线规划仅在用户同意高德隐私政策后启用。
- 未授权或离线时仍可查看和编辑已保存的本地旅行内容。

## v1 范围外

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
