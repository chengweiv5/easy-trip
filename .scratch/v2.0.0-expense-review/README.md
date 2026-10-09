# v2.0.0 花费大改设计覆盖与验证

## 结论

- 当前唯一正式源：`/Users/bytedance/.codex/worktrees/6934/easy-trip/design/easy-trip-v2.0.0.pen`
- 目标状态：用户已确认设计，待实现；运行代码仍为 v1.8.3/build13。
- 本次只是把原 v1.9.0 花费大改改为 v2.0.0，不改变已确认的业务与交互，不增删页面。
- 原 `easy-trip-v1.9.0.pen` 和早期 `easy-trip-v2.0.pen` 都保持原哈希；前者为改版前冻结设计，后者为历史探索。

## 覆盖

- 有效根：185；停用历史根：1（`DRICS`）。
- 完整保留 v1.8.3 的 131 个有效根。
- 11 个设计路由、70 组状态契约。
- 费用能力包括：花费一级入口，历年／年度／月度回顾，住宿／交通／景点／吃饭／购物／其它六类，地点多笔费用，第一笔直接填、更多笔按需加，地点与费用统一原子保存。
- 本轮新增页面：0；删除页面：0。
- 原 v1.8.3 的 8 项 UI 失败断言仍开放。

## 验证

- 当前文件 SHA256：`d31d42703006c980835111b7c60941918d4c44bbfe14f45def85af55598eae3e`。
- v1.9.0 冻结设计 SHA256：`952a4835495afc5698c5d73ea7ccac5fe4d99d9f9d467940ac1450ffd5080a95`。
- 历史 v2.0 探索 SHA256：`1cbe490d0a79e6d9d40433010db8c5cd38ab325aba2469a4f93a50cfe1b0ee04`。
- v1.8.3 SHA256：`0cb8f48fb14932e8850b426e1f18412b94149a43f5798fb61cfd40f6f90f033a`。
- 递归比较确认：除 `name`、`context`、`content` 和原生另存产生的 `fileToken` 外，没有其它字段差异。
- Pencil MCP：有效根版本命名正确；顶层重叠、placeholder、可见文字裁切、断开组件引用、残留“评审稿”均为 0。
- 原生 Layers 已选中 I14 页面并回读编辑属性，确认源文件仍为原生可编辑结构。
- 已在用户解锁后完成原生关闭后的精确路径重开，MCP确认目标路径、185个有效根、单笔/多笔/失败/年度页状态及组件引用；设计改版验收通过。
- punk-12 解锁提醒已发送并逐字回读，发送方和会话一致、消息未删除；message_id `om_x100b63be5d3000b0c33b1fccee3f4b2`。

结构化证据见 `retarget-verification.json`；规格见：

`/Users/bytedance/.codex/worktrees/6934/easy-trip/docs/superpowers/specs/2026-10-09-v2.0.0-expense-review-design.md`

## 变更边界

- 未修改应用代码、数据库、APK 版本号或构建号。
- 未运行应用测试。
- 未提交或推送 Git。
- 未把早期 `easy-trip-v2.0.pen` 当作正式源。

## 回滚

备份：`/Users/bytedance/.codex/artifacts/easy-trip/v2.0.0-retarget-20261009-155611/before/`

回滚前先确认没有后续用户编辑并另行备份；只恢复本轮修改或删除本轮新增文件，禁止整仓库 reset。
