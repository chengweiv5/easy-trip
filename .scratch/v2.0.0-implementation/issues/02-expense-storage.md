# 多笔费用权威存储与升级迁移

Type: task
Status: resolved
Label: ready-for-agent

## 范围

实施计划Task2，用户已确认Repository公共边界。以place_expenses独立记录替代旧地点金额列；Room事务读写、来源与expected检查、零元迁移、精确删除影响。

## Answer

旧NULL/零元/金额与路段迁移、重开、三笔原子保存、来源/ID/过期请求拒绝、插入冲突回滚、独立编辑删除、日期归属和级联均通过模拟器Repository验证。8项新增instrumentation，连同旧行程/日期/费用/迁移共79项通过；941 JVM通过，lint无错误。UI尚未接入，版本号未升级；不声称Task3–6完成。详见../task2-verification.json。

## 回滚

源码按本批独立提交revert；备份见task2-verification.json。已迁移用户库不可降级到schema8或删除重建；仅使用保留新记录的前向兼容修复。本次只修改专用read-only模拟器的临时数据。
