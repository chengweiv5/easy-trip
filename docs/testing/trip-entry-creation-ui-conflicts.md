# 旅行入口与创建 UI 冲突记录

日期：2026-08-27

## 已确认决策

- 顶部头像仅为装饰，不可点击，不提供 Profile callback，也不声明 Profile destination。
- 主旅行卡与其他旅行卡都通过各自的 `···` 菜单承载设置和删除；菜单操作绑定稳定 trip ID。
- 主旅行卡只有“继续规划”作为进入工作台的入口，不同时提供整卡点击入口。
- 创建旅行继续保留旅行名称、开始日期、旅行天数和出行方式四类输入；提交使用生产 `create-submit` selector。
- 日期管理、权限流程和工作台内部 UI 不属于本次旅行入口与创建收敛范围。
- 细小间距、字体渲染和轻微圆角差异留待最终物理真机验收，不阻断自动化候选门禁。

## 场景绑定

- `K9h3r`：已有旅行列表与唯一“继续规划”入口。
- `d1sTtb`：删除后列表状态；可控列表闭环确认目标卡消失、保留旅行仍在，删除入口由 `···` 菜单承载。
- `dzhkC`：完整创建字段与生产提交入口。
- `xQfD0`：开始日期选择及结束日期摘要。
- `oW9mK`：菜单删除与精确影响摘要。
- `zIbEu`：新空态标题、用途说明及创建入口。
- `yIGiQ`：名称、日期、天数错误分别显示在对应字段附近。

## Task 3 final fix 裁决

- 删除完成以成功 `observeTrips()` emission 的版本与 trip ID 快照为准，Loading/Error 对 UI 列表的清空不构成完成证据。
- service completion 与 Flow confirmation 是两个独立条件：service 未完成时不关闭；service 返回后可消费删除期间已发生的新 emission，或继续等待后续 emission。
- 删除确认等待中的 collector Error 最多自动重订阅一次；不重复调用删除 service，也不引入第二套删除状态或独立删除操作。
- `LoadingImpact` 属于不可取消的查询阶段；`ImpactFailure` 才恢复取消和重试入口。
- impact/delete cancellation 必须沿协程传播，测试以 job completion cause 直接约束。

## 新授权聚焦修复裁决

- delete service 成功后，自动同步 retry(1) 耗尽不允许继续保持不可恢复 busy；改为保留同一 `Ready` 目标与 confirmation，并显式标记确认同步失败。
- “重新同步”是独立动作，只重订阅旅行 Flow，绝不重新调用 delete service；同步期间复用 busy 的 confirm/dismiss/Back/outside 锁定契约。
- 每次手动同步仍只允许一次自动 retry，不引入无限自动循环；失败后再次返回可点击的“重新同步”。
- collector generation 与 delete generation/trip ID 同时隔离旧结果，旧 collector 的 completion/error/emission 不得污染新目标或新订阅。
