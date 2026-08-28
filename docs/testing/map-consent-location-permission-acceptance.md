# 地图授权与定位权限候选验收记录

- 日期：2026-08-28
- 最终结论：**PASS（自动化候选门禁）**
- 工作树：`/Users/bytedance/Code/easy-trip/.claude/worktrees/search-place-ui-convergence`
- 分支：`worktree-trip-entry-creation-ui`
- 基线 HEAD：`aca6f11fc590218d1992b37ecd9ca21dd4304ab0`
- 执行边界：仅使用专用 emulator 自动化验证；未使用物理设备，未清除物理设备数据。

## 设备与互斥检查

- 专用设备：`emulator-5554`，`Android_SDK_built_for_arm64` / `sdk_phone64_arm64`，Android 12。
- 最终阶段执行前：设备状态 `device`，`sys.boot_completed=1`。
- `androidx.test.services` 与 `com.yangchengwei.easytrip.test` 均无 PID；无其他 instrumentation。
- Connected suite 严格串行。

## 自动化门禁与 XML 实际计数

| 阶段 | XML tests/failures/errors/skipped | 结果 |
|---|---:|---|
| Focused JVM | 未单独保存 XML；Gradle 成功 | PASS |
| Full JVM | `517 / 0 / 0 / 0`（54 XML） | PASS |
| `WorkspacePermissionFlowTest` | `7 / 0 / 0 / 0` | PASS |
| `TripWorkspaceContentTest` | `25 / 0 / 0 / 0` | PASS |
| `AmapComposeMapTest` | `12 / 0 / 0 / 0` | PASS |
| `PlaceSearchContentTest` | `22 / 0 / 0 / 0` | PASS |
| `WorkspaceFlowTest` | `27 / 0 / 0 / 0` | PASS |
| `V1ScenarioCatalogTest` | `47 / 0 / 0 / 0` | PASS（唯一 infra 重试后） |
| `V1FullUiAcceptanceTest` | `47 / 0 / 0 / 0` | PASS |
| Connected 累计 | `187 / 0 / 0 / 0` | PASS |

## 构建与静态门禁

- `./gradlew :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest`：`BUILD SUCCESSFUL`；lint 无 error，debug APK 与 androidTest APK 已生成。
- `git diff --check`：Graphify 更新前后均 PASS。
- `graphify update .`：PASS，图谱为 `5572 nodes / 12217 edges / 309 communities`。
- Graphify 报告既有解析告警：`NetworkMonitor.kt`（line 1）与 `RoutePlanner.kt`（line 7）有 syntax error、仅部分提取；未影响本次测试或候选门禁结论。

## Infra 重试

- `V1ScenarioCatalogTest` 首次发生 split APK 安装基础设施失败；按规则仅重试一次，重试后完整 `47 / 0 / 0 / 0` PASS。
- infra retry：**1**。
- 没有产品断言失败重试。

## 历史失败时间线

- `AmapComposeMapTest` 初次两项失败已确认为 listener 注册及 fake-host destroy 的测试同步/可见性竞争；最新完整 suite `12 / 0 / 0 / 0` 替代历史结果。
- `PlaceSearchContentTest` 初次授权区 tag/布局问题已修复；最新完整 suite `22 / 0 / 0 / 0` 替代历史结果。
- `WorkspaceFlowTest` 初次 fixture 缺少旅行且使用过期 tag 导致六项失败；补齐 fixture 并迁移入口 tag 后，最新完整 suite `27 / 0 / 0 / 0` 替代历史结果。
- `V1ScenarioCatalogTest` 初次场景 46“地图加载失败”重试入口不可达问题已修复；最终完整 suite `47 / 0 / 0 / 0`。其后一次 split APK 安装失败仅按 infra 规则重试一次。

## 物理设备

**NOT-RUN**。本轮未连接或使用物理设备，未执行专用测试安装/fixture，未清除任何物理设备上的旅行数据。以下关键路径待后续候选物理设备验收：首次说明、拒绝后不自动弹出、本地 Room 降级可操作、重新授权/撤回、真实定位授权（含大致位置、普通/永久拒绝）、设置返回一次性定位及地图失败重试保持工作台上下文。

## 结论边界

本记录证明本批自动化候选门禁通过；不将 emulator 结果表述为物理设备通过。物理设备项保持 `NOT-RUN`。
