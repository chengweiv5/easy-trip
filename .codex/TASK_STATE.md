# 日历交通空隙显示

状态：DONE

当前补充需求：隐藏交通时，地点里不提示“交通可能来不及”。已完成卡片、详情独立警告、拖动预览及警告颜色跟随交通实际可见性。真实日程重叠和路线描述/入口保留。

分支 codex/calendar-traffic-gap；已 rebase 到 origin/main e953835。最新行为修复 2d76da2，已同签名覆盖安装到真机。

用户已授权继续安装最终包、推送 origin/main 并发布 v1.4.0。最终发布提交、安装包哈希和远端回读记录保存在 .scratch/calendar-traffic-gap/evidence/release-v1.4.0/verification.json。

843 项 release 单元测试，56 项日历交互测试、3 项浮动提示测试通过；release lint 无错误。真机地点 3 14:30–16:00、地点 4 16:00–17:00，零间隙隐藏交通及独立交通警告。原有两项旅行、31 个地点、9 天行程保留，APK 回读哈希一致。

证据：.scratch/calendar-traffic-gap/evidence/hide-warning/verification.md。
回滚：.scratch/calendar-traffic-gap/backup/hide-warning/phone-before.apk，用 adb install -r 覆盖安装，不卸载或清除数据。
