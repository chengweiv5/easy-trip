# v3.2 首次批量搜索失败：对照诊断

Status: resolved

## 范围

用户授权继续诊断。物理手机的当前结果保持不变，不安装、不点击、不清日志。
在独占、只读 AVD 上通过真实助手控制器和高德 SDK 验证；无模型调用、收藏及行程写入。
不修改产品查询逻辑，不提交、推送或发布。

基线：`1eaa409a9ad94b56d86ee01e2db8334bdd36a424`。
证据：`/Users/bytedance/.codex/artifacts/easy-trip/f445-search-pacing-20261011/`。
上一轮真实手机证据：`/Users/bytedance/.codex/artifacts/easy-trip/f445-phone-search-failure-20261011-231548/`。

## 已知事实

23:13:53 的 8 项查询在 455ms 内发起。A–C 成功，D–H 均 SDK_ERROR 1800；
失败项均 cityQualified=true，非 NOT_FOUND、CITY_MISMATCH 或 TIMEOUT。
截屏确认 D 偶园、E 昭德古街、F 黄花溪；G/H 名称未显示，不能从截图推定。
测试的 G/H 使用用户此前报告的九龙峪景区、广福寺，是公开测试夹具，不冒充本次截图证据。

## 可证伪假设（按优先级）

1. 请求频率过高：如果是原因，同参数、同并发上限、增加间隔应减少失败；
   单纯串行在响应很快时仍可能失败。
2. SDK 并发不安全：如果是原因，不改变间隔而仅串行应消除失败。
3. 服务/网络瞬时异常：如果是原因，多轮交叉对照中失败不稳定依赖请求间隔或批次位置。
4. 特定地点/参数问题：如果是原因，反转顺序后仍相同地点失败，而不是仍批次后部失败。

1800 是 SDK 的未映射错误，不能直接认定为 QPS 限制，也不能称网络超时。

## 反馈环与步骤

- [x] 检查待合并 PR：无。
- [x] 新建诊断分支，备份重要文档与现有专项测试。
- [x] 增加显式 opt-in 的 emulator-only instrumentation 测试。
- [x] 跑 baseline，断言所有 8 项首次 READY 且 POI 名称符合夹具。5轮均通过，未在此 seam 复现。
- [x] 对比仅串行3轮、并发2加700ms发起间隔5轮；交叉次序、反转地点顺序。
- [x] 单独增加真实 SDK 同步入口突发探针，保留3次红色结果及原始安全错误名称。
- [x] 汇总各请求开始/结束时间与SDK错误码，区分已验证和推断。
- [x] 记录结论与建议；停止且只清理本任务创建的临时模拟器及锁。

调用：

```sh
adb -s emulator-5598 shell am instrument -w -r \
  -e class com.yangchengwei.easytrip.assistant.AssistantSearchPacingDiagnosticTest \
  -e pacingMode baseline -e pacingRun r1-baseline -e pacingReverse false \
  com.yangchengwei.easytrip.test/androidx.test.runner.AndroidJUnitRunner
```

每轮从应用专属 external cache 拉取 `diagnostics/pacing/<run>.json`。
`baseline` 不修改调度；`serial` 仅在适配器外部锁住完整请求；`paced` 仅锁住请求发起，
保持控制器最多2项并发，间隔700ms，无重试。组间休息4秒。
测试 seam 沿用已确认的助手真实搜索公开边界，不 mock 高德成功结果。

## Answer

本次21组、162次真实SDK调用。控制器 baseline 5组、serial 3组、paced 5组全部通过；
模拟器响应较慢，baseline 8次调用发起跨越3.1–4.2秒，未重现手机455ms条件。
SDK同步入口8项突发3组中2组失败，6项突发1组失败，4项突发和另1组8项突发成功，
因此不能推定精确QPS阈值或声称突发必失败。
三个失败均 `1800`，原始安全错误名称均 `CUQPS_HAS_EXCEEDED_THE_LIMIT`。
同SDK同步入口700ms间隔3组24项全部成功，包括反序；控制器700ms间隔5组40项全部成功。

已直接证实SDK1800可以由请求频率超限产生；手机本次根因高度吻合，
但手机缓存没有原始错误名称，故保留归因边界。新增测试默认不带参数跳过，两项门禁均验证。
产品代码未修改；后续建议先做共享发起节流，首次/单项重试共用，保持取消和零隐式写入。
完整结果与建议见 `docs/testing/v3.2.0-assistant-search-pacing-diagnosis.md`。

## 回滚

本轮产品代码零修改。仅删除本轮新增且未交付的测试/诊断文件；
若修改已有文件，仅根据当前 diff 和 evidence/baseline 定点撤销本轮内容，不覆盖他人更改。
