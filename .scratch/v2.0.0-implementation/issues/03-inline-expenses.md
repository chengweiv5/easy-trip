# 地点原位多笔费用闭环

Status: resolved (Task3 only)

## Context

复用地点编辑器和整页草稿；第一笔原位录入，按需增加，同类可重复。时间、停留、地点备注与费用统一原子保存。

## Answer

原位六类选择、默认三笔/原位展开、移除撤销、脏草稿退出确认、保存失败保留、来源失效保护、金额与笔数统一汇总已实现。新增7项Compose用户操作测试和2项ViewModel测试；最终41项设备定向测试、943 JVM、Debug/AndroidTest构建与lint通过。

广泛旧UI回归22项失败在未改UI基线同样复现，不视为本批通过；原8项验收缺口继续开放。两项独立审查问题已修复复核。详情见../task3-verification.json。

## 回滚

本批独立提交保留可逆边界；修改前源码归档在证据目录before/source-state.tgz。仅经审阅后revert本批，不清空或降级用户数据库。
