# v1.6.0 真实高德地图补验

状态：DONE（2026-09-23）。用户明确回复「授权」后完成本地地图配置复制和真实地图补验。

- 来源 `/Users/bytedance/Code/easy-trip/local.properties`，目标当前工作区 `local.properties`。复制前目标不存在，复制后逐字节一致；权限 0600，Git ignored / untracked。仅回报非空与匹配状态，未输出密钥。
- Debug 与 AndroidTest 构建通过，合并 Manifest 的高德 Key 与本地配置匹配，见 config-verification.json 和 build.log。
- emulator-5596（此前建立的只读临时 AVD），运行真实 `RealAmapMapHost` / 高德 SDK，地图隐私 API 先于 MapView 构建执行。
- 1 项真实地图验收通过，包含五主题地点池与五主题行程，共 10 个画面；工作台更多菜单 → 主题预览 → 应用全流程，真实持久写入。
- 每次切换比较中心经纬度（1e-6 容差）、缩放（1e-4 容差）；MapView 仅创建 1 次，切换时销毁 0 次。见 instrumentation.log。
- 10 张实际屏幕截图位于 screenshots/；最终两组缩略图和 OCR 已检查，高德底图显示西湖、街道、公园等真实图层，收藏标记和界面随主题变色。
- 五张行程图的地图区域中固定日期蓝色 #2766AA 均为 15431 像素，路线及序号颜色未随主题改变。见 pixel-verification.json。
- 验收内容为杭州示例数据，未读取/修改业务旅行数据库；未操作物理手机，未推送或发布。
- 临时截图用例已从 app/src/androidTest 移至此目录归档；应用源码无改动，不给常规测试增加在线地图依赖。

回滚：移除本次新建、未追踪的 local.properties 即恢复先前无地图 Key 的构建状态；主仓库配置未修改。验证文档可撤销本次本地文档提交。APK SHA 仅记录本地已验证构建，不表示发布。
