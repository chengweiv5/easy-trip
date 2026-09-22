# 交通空隙规则验证

- 原问题由真实 Compose 回归用例复现：30 分钟空隙、2 分钟步行，独立交通标签不存在，测试失败。
- 修复后：843 项单元测试通过，其中新增 8 项纯布局测试；43 项日历交互测试通过。
- 已覆盖 29/30 分钟、拖动即时改变/取消/保存、单日/全程、大字体、实际 2 分钟标记、占位及零时长遮挡和详情访问。
- Debug lint 无 Error，50 个 Warning、1 个 Hint；debug/release APK 编译成功。
- 手机旧 APK 已备份到 backup/phone-before.apk；新旧签名 SHA-256 一致。
- 回滚：源码 revert 修复提交；手机用 `adb -s FMR0224725012307 install -r .scratch/calendar-traffic-gap/backup/phone-before.apk` 覆盖安装。不得卸载或清除应用数据。
- 真机覆盖安装成功，版本仍为 1.4.0（versionCode 6）；安装包来自干净提交 30a0531b681b26edc6caacf3b9294b23e40ca308。
- 手机实际安装 APK SHA-256 与本地 release APK 一致（详见 evidence/installed-apk-sha256.txt）。
- 真机回读：第 2 天地点 3 二七广场仍为 15:00–16:00，地点 4 郑州二七纪念馆仍为 16:30–17:30；步行约 2 分钟已显示在 16:00–16:30 空隙，地点 4 内不再显示交通行。
- 既有 31 个地点、9 天行程、两项旅行保留；没有修改行程时间，没有卸载或清除数据。
- Release lint 同样无错误；截图保存在 evidence/phone-after.jpg（本地忽略，不纳入版本控制）。
