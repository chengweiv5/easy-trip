# 主动搜索没有重试失败的当前城市请求

Status: ready-for-agent
Completion: done

## 背景与原因

定位失败已在手机读取并复现；业务回归测试进一步确认：失败请求保存在会话中，`PlaceSearchReducer.submit()` 未通知数据源清除失败状态。

## 改动

- `PlaceSearchDataSource.retryFailedLocation()` 表达主动搜索的重试意图，普通数据源默认无操作。
- `LocatedCitySearchSource` 将该意图传给共享定位会话。
- `PlaceSearchReducer` 只在非防抖搜索时触发重试。
- `AppLocationSession.retry()` 仅清除已完成的失败，保留成功与正在执行的请求。

## 验证

见 `../verification.md`。4 项新增单测和 3 项新增 UI 测试覆盖恢复、复用与去重。

## Comments

- 2026-10-08：用户明确每次主动点击搜索应重新尝试；采用显式重试，不采用失败缓存过期或 SDK 自动重试方案。
- 2026-10-08：本地代码及验收完成，尚未推送或发布。
- 2026-10-08：用户追加授权提交、推送和安装；版本递增为 `1.8.1 / 11`，交付过程与最终回读见验证记录。GitHub Release 不在本次范围内。
