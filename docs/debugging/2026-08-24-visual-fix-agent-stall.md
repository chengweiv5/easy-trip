# Visual Fix Agent Stall 诊断

日期：2026-08-24  
调查对象：subagent `aebbf37587d2b0bf1`  
范围：只读分析 transcript、父 session 生命周期事件、仓库状态和遗留验证产物；未修改 app 代码，未向接管 agent `af6fb7609dfbb668f` 发送消息或停止其任务。

## 1. Executive summary

### Confirmed

1. **这不是一个仍在运行或被 Gradle/设备命令挂住的 agent。** 父 session 在两次停止点都收到 `<status>completed</status>`；第一次通知时间为 `2026-08-24T07:27:26.663Z`，第二次为 `2026-08-24T07:28:08.913Z`（UTC，分别为北京时间 15:27:26、15:28:08）。
2. **两次“run”实际共用同一份持续追加的 transcript。** 第一次统计为 121 tool uses、`duration_ms=3037286`；resume 后只新增了 1 次 Bash，累计变为 122 tool uses、`duration_ms=3079546`。第二次不是又独立运行了约 51 分钟，而是同一 agent 从原 transcript 继续约 42 秒；父通知里的 duration/tool 数是累计值。
3. **第一次停止发生在第 121 次工具调用失败后。** 该调用尝试 `adb install`、force-stop、`adb shell monkey`、截图/XML 采集；`monkey` 返回 exit 251。其后没有最终文本响应。
4. **resume 后新增的第 122 次调用成功完成。** 它改用显式启动 `MainActivity`，拉取 `/sdcard/current.xml` 并生成截图；最后一条 transcript 是成功的 tool result（伴随 `Warning: Output file suffix should be png`），随后 agent 再次 completed，仍无最终文本响应。
5. **transcript 中没有本次停止的明确 `context_length_exceeded`、`max_tokens`、`max turns`、tool cap、cancelled、permission/hook denial 或异常堆栈事件。** 唯一出现的 `context_length_exceeded` 是 agent 读取旧进度报告时引用的 Batch 1 历史，不是本次运行的事件。
6. **没有出现正常终局 assistant message。** 105 条 assistant message 的 `stop_reason` 为 `tool_use`；另有若干流式/空占位 message 的 `stop_reason=null`。最后一条 assistant 行仍是 Bash tool use，最后一条记录是 tool result。
7. **任务范围明显过宽且在单一上下文内形成高频修测循环。** 122 次工具调用包括 50 Edit、36 Bash、23 Read、5 Skill、4 Pencil execute、3 Pencil read_skill、1 Pencil get_app_state；其中 16 次为 `connectedDebugAndroidTest`。
8. **遗留代码和测试不是空产物。** HEAD 仍为 `6b911328c9261d8e828391651cebd613e25036c2`；10 个已跟踪文件未提交，合计 139 insertions / 14 deletions；`git diff --check` 通过。transcript 记录 API 36 上最终 7/7 工作台测试及 Batch 2 22/22 测试成功。

### Likely

1. **最高概率是 subagent/harness 在工具调用边界被自动收尾，未获得或未完成生成最终答复的回合。** 证据是两次均紧邻 tool result 结束，父侧标记 completed 而非 failed/cancelled，且 transcript 无模型终止错误。具体是内部空响应、调度器停止条件还是隐藏的预算/生命周期策略，现有日志不能区分。
2. **长上下文和高工具次数很可能提高了上述收尾失败概率。** 单份 transcript 为 1,529,872 bytes、292 行，包含大量 Gradle 输出、完整文件 Edit payload、Pencil skill 文本及反复测试结果；但没有可证明“达到硬性 120/121 工具上限”的事件。
3. **第一次停止可能由失败的第 121 次 adb 采证链触发了一个空/无终局回合；第二次 resume 只完成了该链的替代命令，随即再次在 tool result 后退出。** 这是与事件顺序最一致的解释，但仍不是显式 stop reason。

### Unknown

- Claude Code 2.1.241 内部为何把两次运行记为 `completed`，却没有最终 result 文本。
- 是否存在未写入 transcript 的内部 turn/tool budget、空响应重试上限、context compaction/调度策略。
- 模型端最终 response 的底层 stop reason；本地 JSONL 没有对应最终 message。
- 第一次停止是否恰好受一个约 50 分钟任务生命周期限制影响。时长接近但并非精确同值；第二条通知只是累计统计，不能作为“第二次也命中同一超时”的证据。

## 2. 两次 run timeline

所有 transcript 时间均为 UTC；括号内为北京时间 UTC+8。

### Run 1：初始执行

- 开始：`2026-08-24T06:36:49.354Z`（14:36:49）
- 父 session 完成通知：`2026-08-24T07:27:26.663Z`（15:27:26）
- duration：`3,037,286 ms`（50m37.286s）
- tool uses：121
- transcript 在通知前最后工具结果：`2026-08-24T07:27:26.634Z`
- 终点：adb/monkey 采证命令 exit 251；没有最终文本。

Run 1 后段工具动作类别（最后 18 个，按发生顺序）：

1. Edit：工作台生产代码
2. Edit：工作台生产代码
3. Edit：工作台 instrumented test
4. Gradle connected test：单个 sheet 比例测试
5. Gradle connected test：单个搜索返回测试
6. Edit：工作台生产代码
7. Edit：`PlacePoolSheet`
8. Edit：`PlacePoolSheet`
9. Gradle connected test：搜索返回测试
10. Edit：撤销无效 `weight` import
11. Gradle connected test：搜索返回测试
12. Gradle connected test：地点池滚动/inset 测试
13. Edit：测试断言
14. Edit：测试交互
15. Gradle connected test：完整工作台测试（失败）
16. Edit：修正空状态断言
17. Gradle connected test：完整工作台测试（7/7 成功）
18. Gradle connected test：Batch 2 组合套件（22/22 成功）
19. adb：安装、monkey 启动、截图/XML 采集（exit 251；这是第 121 次调用）

### Run 2：resume

- resume 请求：`2026-08-24T07:27:45.317Z`（15:27:45）
- resume 成功确认：`2026-08-24T07:27:45.656Z`
- 新增 assistant tool call：`2026-08-24T07:27:57.274Z`
- tool result：`2026-08-24T07:28:08.899Z`
- 父 session 完成通知：`2026-08-24T07:28:08.913Z`（15:28:08）
- 通知展示 duration：`3,079,546 ms`，tool uses：122
- **解释：统计相对同一 agent 累计；本次 resume 实际约 42.26 秒，仅新增 1 个工具调用。**
- 新增动作：Bash，通过 `am start -n com.yangchengwei.easytrip/.MainActivity` 替代 monkey，完成安装/启动/截图/XML 拉取。
- 终点：成功 tool result 后直接 completed；没有最终文本。

## 3. 停止信号检查

| 信号 | 结论 | 证据 |
|---|---|---|
| `context_length_exceeded` | **未发现本次事件** | transcript 中 2 次字符串命中来自同一条旧 Batch 1 报告引用；不是错误记录或最终 stop reason。 |
| `max_tokens` | **未发现** | transcript 文本计数为 0。 |
| `max turns` / `max_turns` | **未发现** | transcript 文本计数为 0。 |
| tool cap / tool limit | **未发现显式事件** | 121/122 只存在父通知计数；无 cap/limit 文本。 |
| timeout | **未发现 agent 级终止事件** | 命中主要来自测试报告、命令参数和旧 Room timeout 诊断；有一次设备丢失和测试超时内容，但 agent 继续运行。 |
| cancelled/canceled | **未发现 agent 被取消** | 命中来自源码 `CancellationException` 或测试内容；父通知为 completed。 |
| permission denial | **未发现** | 父生命周期事件显示 `permissionMode=bypassPermissions`；无 permission denied。 |
| hook denial | **未发现** | 未见 hook blocked/denied；可见 debug 文件属于 8 月 23 日另一 session，不覆盖本次 agent。 |
| agent stop reason | **没有可用终局 reason** | assistant 的 105 个完整回合均为 `tool_use`；终局 message 缺失。父侧仅有 `<status>completed</status>`。 |
| 异常堆栈 | **没有 agent/harness 异常堆栈** | transcript 中的 exception 是 Gradle/test/device 输出；没有 Claude Code 或 Agent SDK stack trace。 |

注意：`completed` 只确认任务生命周期已结束，不等价于业务任务完成；本例明显缺少用户要求的报告、commit、graphify update 和最终状态。

## 4. 行为模式与次数

### 工具分布

| 类别 | 次数 |
|---|---:|
| Edit | 50 |
| Bash | 36 |
| Read | 23 |
| Skill | 5 |
| Pencil execute | 4 |
| Pencil read_skill | 3 |
| Pencil get_app_state | 1 |
| **合计** | **122** |

Bash 的语义分类：

| 行为 | 次数 |
|---|---:|
| Gradle `connectedDebugAndroidTest` | 16 |
| adb/uiautomator/截图采证链 | 3 |
| 其他 Gradle | 1 |
| graphify | 3 |
| 其他 Bash（git、文件/报告查询等） | 13 |

### 循环特征

- `TripWorkspaceContentTest` 整类或单方法 connected tests 被连续反复运行。
- `searchReturnSheetIsRaisedAndHighlightsRecentCollections` 相关单测命令 3 次。
- `halfSheetMatchesDesignProportionAndStaysBelowTopSafeArea` 相关单测命令 2 次。
- 完整工作台测试先失败，改一处断言，再重跑成功；此前已有多轮“测—读失败—改单一断言/实现—再测”。
- 16 次 connected tests 中多次耗时 1.5–5 分钟；工具调用间大于 90 秒的间隔共 9 段，最大约 319 秒，主要对应 Gradle 执行，不是无动作等待。
- Pencil 读取集中在前期，共 8 次；没有证据显示停止点处仍在等待 Pencil。
- 最后两次 adb 采证动作高度相似：第一次使用 monkey 失败，resume 后改为显式 Activity 启动成功。
- 未发现完全相同命令的无限循环；更准确的描述是**围绕相同测试目标的渐进式修测循环**。

## 5. 当前遗留产物与可复用证据

### 工作树

- HEAD：`6b911328c9261d8e828391651cebd613e25036c2`
- 10 个已跟踪文件未提交。
- diff：139 insertions / 14 deletions。
- `git diff --check`：通过。
- 未发现该 agent 创建目标 `batch-2-visual-fix-report.md`；也没有 commit、ledger 更新或 agent 自己完成的 `graphify update` 证据。

改动文件：

- `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContentTest.kt`：76+/5-，新增/调整工作台视觉、搜索返回、滚动与 inset 断言。
- `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt`
- `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolSheet.kt`
- `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolViewModel.kt`
- `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchViewModel.kt`
- `app/src/main/java/com/yangchengwei/easytrip/place/ui/SavedPlaceRow.kt`
- `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContent.kt`
- `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceRoute.kt`
- `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt`
- `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceUiModels.kt`

### 可复用测试证据

transcript 中最后的成功记录：

- API 36 emulator `trail_map_api36(AVD) - 16`
- `TripWorkspaceContentTest`：7 tests，0 failed，`BUILD SUCCESSFUL in 2m 15s`
- Batch 2 组合 suite：22 tests，0 failed，`BUILD SUCCESSFUL in 4m 52s`

这些证据可用于接管判断，但建议接管 agent 在当前代码最终定稿后至少重跑一次关键 gate，因为工作树可能在本诊断期间被共享接管 agent 继续修改。

### 视觉/设备产物

目录 `/tmp/easy-trip-batch2-visual-fix/` 当前存在：

- `00-current.png`
- `00-current.jpg`
- `00-current.xml`
- `create.xml`
- `create2.xml`
- `install.txt`

其中部分文件 mtime 晚于原 agent 停止，可能由接管 agent 继续更新；不能全部归因于 `aebbf37587d2b0bf1`。原 agent transcript 可确认其最后一次调用生成/拉取过 current screenshot/XML。

已有可复用上下文报告：

- `.superpowers/sdd/2026-08-23-easy-trip-v1-full-ui-implementation/batch-2-gate-report.md`
- `.superpowers/sdd/2026-08-23-easy-trip-v1-full-ui-implementation/batch-2-egl-diagnosis.md`
- `.superpowers/sdd/2026-08-23-easy-trip-v1-full-ui-implementation/batch-2-room-timeout-diagnosis.md`
- `.superpowers/sdd/2026-08-23-easy-trip-v1-full-ui-implementation/progress.md`

## 6. 根因候选（按证据强弱排序）

### 1. Harness 在 tool-result 边界自动结束，最终答复回合缺失

**强度：高。**

支持：

- 两次停止都发生在工具结果之后。
- 父侧均标记 completed，不是 failed/cancelled。
- 没有最终 assistant text、没有业务 result。
- resume 只增加一次成功工具调用，又以同样模式停止。

反证/限制：

- 没有内部 scheduler 日志或明确 stop reason，不能指出具体代码路径。

### 2. 超大单任务上下文与高频工具链导致空终局/自动收尾概率升高

**强度：中。**

支持：

- 1.53 MB JSONL、292 行、122 tools。
- 50 次 Edit 会在 tool result 中携带大段文件内容。
- 16 次 connected tests 携带大量 Gradle 输出。
- 同一 agent 同时承担设计读取、TDD、设备恢复、视觉采证、整套 gate、报告、graphify、commit。

反证/限制：

- 没有本次 `context_length_exceeded`、compaction failure 或 token usage 终止事件。
- 模型为 1M context 配置，单凭文件大小不能证明越界。

### 3. 存在未记录的内部运行时/turn/tool 预算

**强度：中低。**

支持：

- 第一次持续约 50m37s，且恰在第 121 个工具结果后结束，形态类似生命周期预算。
- 父任务通知不提供详细停止原因。

反证：

- resume 后累计 122 工具仍可继续，否定“硬性 121 tool cap”。
- 第二次通知的 51m19s 是累计值，不是第二次又运行 51 分钟。
- 没有 max turns/tool cap/timeout 文本。

### 4. adb/monkey 失败直接导致第一次停止

**强度：中低，只能解释第一次。**

支持：

- 第一次停止紧接 exit 251。
- resume 明确改用 `am start`，工具成功。

反证：

- Bash 非零通常会返回模型继续处理，不应自动结束 agent。
- 第二次工具成功后仍无最终响应。

### 5. Permission/hook/Pencil/设备等待导致停止

**强度：低。**

支持：

- 任务涉及外部 MCP、emulator、adb 和 hooks，理论上存在外部不稳定性。

反证：

- 无 denial；父事件为 `bypassPermissions`。
- 停止点不是 Pencil 调用。
- 第二次最终 adb 工具正常返回。
- 没有仍运行的目标 agent 进程证据；emulator 持续运行不代表 agent 卡住。

## 7. 最小复现、进一步检查与避免再次浪费

### 最小复现

不要在共享生产 worktree 重跑整个视觉修复。若要复现 harness 行为，使用隔离临时目录/无业务修改的小任务：

1. 启动一个 background agent，要求连续执行若干只读短命令，最后必须输出固定 token `FINAL_SENTINEL`。
2. 在第 N 次工具调用后让最后一条命令成功返回，观察 JSONL 是否产生终局 assistant text。
3. 另做一组：倒数第二条命令返回非零，resume 后执行一条成功命令，观察是否复现“tool result 后 completed、无 sentinel”。
4. 从 20、60、100、120 次工具调用逐级测试；每组控制 transcript 体积，以区分 tool 数和上下文体积。
5. 分别记录父 task notification 的 `duration_ms/tool_uses/status` 与子 transcript 最后一条 message 的 `stop_reason/content`。

### 进一步检查

1. 若能取得 Claude Code 2.1.241 对应 session debug 日志，查 `2026-08-24 15:27:20–15:28:09 +0800`，关键词：agent id、response id、empty response、turn budget、task timeout、abort、compact、stop reason。
2. 检查 CLI/SDK 是否有“background agent 无 live children 时，在 tool result 后提前 completed”的已知问题；本地现有 debug 文件是 8 月 23 日另一 session，不能用于定论。
3. 对比一个正常完成的同类 subagent transcript：确认正常末尾应有 `assistant stop_reason=end_turn` 和文本 result，而本例缺失。
4. 记录 task runner 的原始结构化完成对象；当前父 JSONL 仅保存 status/usage，没有隐藏 reason。

### 避免再次浪费

- **拆阶段**：
  1. Pencil 基线读取与差异清单；
  2. 单一视觉问题 TDD；
  3. API36 关键测试；
  4. 生产截图/XML 采证；
  5. 全 gate；
  6. report/graphify/commit。
- **每阶段写 checkpoint report**：至少记录当前 SHA、dirty files、测试命令/结果、证据路径、下一步。即使 agent 无终局回复，也能直接接管。
- **明确停止条件**：如“同一测试最多修测 3 轮；仍失败则写 BLOCKED checkpoint 并停止”“设备丢失只恢复一次”。
- **限制单 agent 范围**：避免一次包含 TDD + emulator 运维 + 视觉采证 + 全套 gate + graphify + commit。
- **压缩工具输出**：Gradle 结果只保存最终摘要和报告路径；不要把重复完整 build log 回灌上下文。
- **减少 Edit 次数**：先形成小型变更计划，再按文件成组修改；避免几十次单行 Edit 将完整文件状态反复写入 transcript。
- **先产出再扩展**：完成代码和关键测试后先写 report/checkpoint，再执行全量验证和视觉证据。
- **resume 指令应先要求立即总结当前状态**，而不是直接继续所有未完成链路；确认拿到 checkpoint 后再派下一阶段。

## 8. 附录：路径与安全查询命令

### 相关路径

- 子 agent transcript：  
  `/Users/bytedance/.claude/projects/-Users-bytedance-Code-easy-trip--claude-worktrees-easy-trip-v1-full-ui-run/14258c9c-42f5-40e8-81c4-705321039e1f/subagents/agent-aebbf37587d2b0bf1.jsonl`
- task output symlink：  
  `/private/tmp/claude-501/-Users-bytedance-Code-easy-trip/14258c9c-42f5-40e8-81c4-705321039e1f/tasks/aebbf37587d2b0bf1.output`
- 父 session transcript：  
  `/Users/bytedance/.claude/projects/-Users-bytedance-Code-easy-trip--claude-worktrees-easy-trip-v1-full-ui-run/14258c9c-42f5-40e8-81c4-705321039e1f.jsonl`
- 当前视觉证据目录：  
  `/tmp/easy-trip-batch2-visual-fix/`
- 工作目录：  
  `/Users/bytedance/Code/easy-trip/.claude/worktrees/easy-trip-v1-full-ui-run`

### 安全查询原则

- 不直接 Read/打印完整 JSONL。
- 用 Python 流式逐行解析，只输出计数、时间戳、工具名和截断片段。
- 对 tool input/result 设定每段上限（建议 200–500 字符）和总行数上限。
- 不读取 `.pen` 原始文件；设计信息只通过 Pencil MCP。

### 示例命令

统计 JSONL 类型和时间范围：

```bash
python3 - <<'PY'
import json, collections
p = '/path/to/agent.jsonl'
types = collections.Counter()
first = last = None
with open(p, encoding='utf-8') as f:
    for line in f:
        x = json.loads(line)
        types[x.get('type')] += 1
        ts = x.get('timestamp')
        first = first or ts
        last = ts or last
print(first, last, types)
PY
```

只列最后 20 个 tool use 的名称与时间：

```bash
python3 - <<'PY'
import json
from collections import deque
p = '/path/to/agent.jsonl'
out = deque(maxlen=20)
with open(p, encoding='utf-8') as f:
    for n, line in enumerate(f, 1):
        x = json.loads(line)
        for b in (x.get('message') or {}).get('content') or []:
            if isinstance(b, dict) and b.get('type') == 'tool_use':
                out.append((n, x.get('timestamp'), b.get('name')))
for row in out:
    print(row)
PY
```

查明确停止标记但不输出整行：

```bash
python3 - <<'PY'
import re
p = '/path/to/agent.jsonl'
pat = re.compile(r'context_length_exceeded|max_tokens|max.?turn|timeout|cancel|permission|hook.{0,20}denied|tool.{0,20}(cap|limit)', re.I)
with open(p, encoding='utf-8', errors='replace') as f:
    for n, line in enumerate(f, 1):
        if pat.search(line):
            print(n, pat.search(line).group(0))
PY
```

仓库遗留状态：

```bash
git status --short
git rev-parse HEAD
git diff --stat
git diff --check
```
