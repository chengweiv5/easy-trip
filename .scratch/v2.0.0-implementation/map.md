# v2.0.0 实现决策

## Decisions so far

- [Task1](issues/01-expense-domain.md)：六类、旅行日归属、稳定来源身份和原位草稿纯领域规则。
- [Task2](issues/02-expense-storage.md)：地点费用独立ID且归具体行程项；路段维持单金额，事务一致读取和原子保存。UI及年月入口尚待Task3–6。

- [Task3](issues/03-inline-expenses.md)：地点原位多笔费用、整页原子草稿和统一摘要；年月独立入口待Task4。

- [Task4](issues/04-expense-review.md)：一级花费入口、年月分类、单笔编辑及范围/滚动保留。

- [Task5](issues/05-period-consistency.md)：精确年月影响、回滚后确认、过期快照、取消与删除一致性。
