# v1.5.0 正式发布

状态：DONE（2026-09-23，推送、发布及公开产物回下载验证完成）

已发布 https://github.com/chengweiv5/easy-trip/releases/tag/v1.5.0 并设为 Latest。发布提交及标签指向 a37877a50293917007ac6e9150276a896d895ae1，APK 为 1.5.0 / versionCode 7、SOURCE_STATE=CLEAN。包括全程日历页头对齐、日期操作按钮、地图视角修复及最新 README 全套截图。

845 项 Release 单元测试全部通过，lint 无错误；同签名从官方 v1.4.0 覆盖升级，2 个旅行、6 个旅行日、3 个地点、9 条行程测试数据逐行一致。正式包首页、地点池、行程及全程日历启动验证通过。本次未操作物理手机。

GitHub 两项资产大小和 SHA256 验证通过；正式发布后重新下载的 APK 与校验文件和本地发布产物逐字节一致，签名、版本复验通过。APK SHA256：06a353f16f35331850d7da3c3c597e42b9cea9c74ff7b91f55a11061bb5ace2d。

发布验证及回滚说明：docs/testing/v1.5.0-release.md。原始构建、升级、公开下载、远端及通知证据：.scratch/release-v1.5.0/evidence/。发布后的文档补充提交不改变 v1.5.0 标签及 APK。

---

## 之前的任务记录

# 全程日历页头与网格对齐

状态：DONE（2026-09-23，修复与截图验证完成）

分页栏排除时间刻度，与下方日历列等宽、同中心；每一天标题和换行日期按各自日历列逐行居中。修复提交 46af47efd95674bdd5bc48ef95c735567af8f0cb，分支 codex/calendar-grid-header-alignment。

7 项模拟器验证通过，包括 5 项新回归、30 天翻页横滑和真实导航截图。实际分页/日历边界 x=387..1122px 完全重合，分页文本及两列标题、日期的中心误差均为 0px。已从修复版本重新采集 README 全部图片，OCR、14 个链接、桌面和手机排版验证通过。未操作物理手机。

证据及回滚：.scratch/calendar-grid-header-alignment/verification.md。最终远端交付与通知见同目录 evidence/。临时采集入口已归档，正式回归测试保留。

---

## 之前的任务记录

# 全程日历分页文字居中

状态：DONE（2026-09-23，App 修复与 README 截图更新验证完成）

全程日历「1–2 / 3 天」默认左对齐，现改为在左右翻页按钮间居中。修复提交 855fbe2359c13a9421f698c8c6f5c92c2de6a6ba。保留现有按钮尺寸、日期列与翻页行为。

Debug / AndroidTest 构建通过，4 项模拟器验证通过；普通及 1.3 倍字体、首末页和两位数页码共 6 次实际文字中心测量与分页区域中心一致，误差 0；30 天翻页/横滑正常。修复版本已重新采集全套 README 图片，完整长图、OCR、14 个链接及桌面/手机排版验证通过。未操作物理手机。

分支 codex/calendar-pager-center；验证与回滚：.scratch/calendar-pager-center/verification.md；原始证据、远端交付及通知记录：.scratch/calendar-pager-center/evidence/。临时验证入口已归档移除。

---

## 之前的任务记录

# README 全部截图刷新

状态：DONE（2026-09-23，全部截图重新采集与文档验证完成）

已从最新 origin/main d267f453a94ca836ce854c162bcb6113334d686f 构建并在专用只读模拟器 emulator-5586 运行 App，替换 README 全部 10 张图片。包含正式版 v1.4.0 之后的日期按钮等距、红色删除图标及地图视角修复。所有界面来自同一次生产 AppNavigation 流程与同一份杭州三天内存示例。

验证：Debug 与 AndroidTest 构建成功；整套导航截图用例通过；9 份原始图片的采集 SHA 均为 d267f45，全部 10 张入库图片与旧版哈希不同；完整分享 PNG 与应用本次导出字节一致；OCR 关键文字、14 个本地链接、1024px / 390px 排版和图片比例检查通过。已逐张检查最新日期操作按钮、日历、备注、时间编辑、分享与完整三天页脚。临时采集入口已归档移除，无应用代码改动。

分支：codex/readme-latest-all-screenshots。原始图片、构建来源、采集入口、OCR、排版与回滚说明：.scratch/readme-latest-screenshots/verification.md。最终提交与远端交付结果以 .scratch/readme-latest-screenshots/evidence/latest/push-verification.json 为准。备份：.scratch/readme-latest-screenshots/before/。

---

## 之前的任务记录

# README 主要功能界面截图

状态：DONE（2026-09-23，内容与截图验证完成）

README 已补充地点池、单日/全程日历、分享预览、长图效果与时间编辑截图，保留原首页、地图行程及备注图。新增 6 张 JPEG、1 张完整三天 PNG；来源说明已同步到 docs/images/README.md。

验证：debug/AndroidTest 构建成功，3 项既有截图用例和 2 项临时截图采集通过；13 个本地链接存在；GitHub Markdown 渲染以及 1024px/390px 图片加载、比例、无横向溢出检查通过。完整 PNG 与原始导出字节一致，末日与页脚完整。无应用源码改动、未操作真机。

分支：codex/readme-feature-screenshots；基线 57ea6cf。原始素材、临时采集入口、验证与回滚记录：.scratch/readme-feature-screenshots/verification.md。修改前文档备份：.scratch/readme-feature-screenshots/before/。提交与远端回读记录：.scratch/readme-feature-screenshots/evidence/push-verification.json。

---

## 之前的任务记录

# 日历交通空隙显示

状态：DONE

当前补充需求：隐藏交通时，地点里不提示“交通可能来不及”。已完成卡片、详情独立警告、拖动预览及警告颜色跟随交通实际可见性。真实日程重叠和路线描述/入口保留。

分支 codex/calendar-traffic-gap；已 rebase 到 origin/main e953835。最新行为修复 2d76da2，已同签名覆盖安装到真机。

用户已授权继续安装最终包、推送 origin/main 并发布 v1.4.0。最终发布提交、安装包哈希和远端回读记录保存在 .scratch/calendar-traffic-gap/evidence/release-v1.4.0/verification.json。

843 项 release 单元测试，56 项日历交互测试、3 项浮动提示测试通过；release lint 无错误。真机地点 3 14:30–16:00、地点 4 16:00–17:00，零间隙隐藏交通及独立交通警告。原有两项旅行、31 个地点、9 天行程保留，APK 回读哈希一致。

证据：.scratch/calendar-traffic-gap/evidence/hide-warning/verification.md。
回滚：.scratch/calendar-traffic-gap/backup/hide-warning/phone-before.apk，用 adb install -r 覆盖安装，不卸载或清除数据。
