# 日历交通空隙显示

状态：DONE

用户确认方向：2 分钟步行文字使用地点间空隙；追加约束为不足 30 分钟不显示交通信息。已实现并验证，代码分支 codex/calendar-traffic-gap，修复提交 6206d2c，安装包来自干净提交 30a0531，验证记录提交 c7512fc。尚未推送远端或发布 Release。

843 项单元测试、43 项日历交互测试通过，debug/release lint 无错误。真实手机同签名覆盖安装后，地点 3 15:00–16:00、地点 4 16:30–17:30 保持，步行约 2 分钟显示在间隔，地点卡内无交通行。安装 APK 哈希与本地构建一致，原有 31 个地点、9 天行程保留。

证据：.scratch/calendar-traffic-gap/validation.md；截图 evidence/phone-after.jpg；旧 APK backup/phone-before.apk。恢复旧版使用 adb install -r，不卸载、不清除用户数据。
