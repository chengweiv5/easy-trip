# SDD ledger — plan: docs/superpowers/plans/2026-08-25-easy-trip-workspace-ui-convergence.md

## Pre-flight

Base: `c04a0819fe01bef3f55d16d71b02e2655371614f`

| Tasks | Producer / consumer boundary | Finding |
|---|---|---|
| 1 → 3/4 | Workspace visual tokens and icons are consumed by scaffold chrome | Clean; Tasks 3/4 must not duplicate sizes or colors |
| 2 → 3 | Sheet anchors and drag functions are consumed by WorkspaceScaffold | Clean; Task 3 must use Task 2 interfaces |
| 3 → 4/5/6 | Scaffold exposes shared sheet boundary and slots | Clean; later tasks must not reintroduce local inset calculations |
| 4 → 5/6 | Chrome fixes the sheet content origin and horizontal inset | Clean; content tasks consume, not override, shell padding |
| 5 ↔ 6 | Both connect through TripWorkspaceContent.kt | Sequential final wiring required; no parallel edit to shared file |
| 1 | Tests require icon semantics before replacing placeholder text | Internally consistent |
| 2 | Existing 280dp/2× test conflicts with deferred extreme-scenario gate | Ruling: keep as non-regression observation; do not add production branches solely for it; if incompatible, weaken to no-crash/key-action reachability. Cost if wrong: a pre-existing extreme case may regress until the later responsive pass. |
| 3 | Overlay positions must follow current visible sheet boundary | Internally consistent |
| 4 | Spec requires date subtitle but current ready state lacks it | Ruling: derive `dateLabel` from existing `TripWithDays.startDate` and day count in UI state only; no repository/domain change. Cost if wrong: date formatting may need later product adjustment. |
| 5 | Place row action style is unresolved | Ruling: preserve existing edit/delete actions; only constrain layout and typography. Cost if wrong: visual weight remains until joint review. |
| 6 | RouteLeg/card density is deferred by spec | Ruling: only fix shell-induced clipping and padding. Cost if wrong: local visual density remains inconsistent until a later batch. |
| 7 | Device and connected tests are globally shared | Run serially only after confirming availability |

## Progress

Task 1: complete (commits c04a081..fd27ac0, review clean)
- Tests: WorkspaceChromeTest + MapLayerFlowTest 5/5 PASS; testDebugUnitTest and compileDebugAndroidTestKotlin PASS.
- Non-blocking: full physical-device visual review remains in Task 7; existing Graphify warnings in NetworkMonitor.kt and RoutePlanner.kt.

Task 2: fix round 1/5 (3 addressed, 0 open; commits 5195594..8c0c0f7)
Task 2: complete (commits fd27ac0..8c0c0f7, review clean)
- Tests: WorkspaceSheetSyncTest PASS; compileDebugAndroidTestKotlin PASS; TripWorkspaceContentTest 11/13 PASS.
- Deferred to Task 4: workspaceTabsUseIndicatorAndTabSemantics.
- Deferred to Task 5: placePoolListScrollsAndKeepsSecondCardAboveBottomInset.

Task 3: fix round 1/5 (1 addressed, 1 open — active drag synchronized; drag-end controlled-state split remained; commits fba6057..79cd497)
Task 3: fix round 2/5 (1 addressed, 2 open — atomic offset fixed; delayed acceptance and callback freshness remained; commits 79cd497..d549dd0)
Task 3: fix round 3/5 (2 addressed, 0 open; commits d549dd0..7bd9ec5)
Task 3: complete (commits 8c0c0f7..7bd9ec5, review clean)
- Tests: unit suite PASS; six drag tests PASS; TripWorkspaceContentTest 20/22 PASS.
- Deferred to Task 4/5: only the known tab-indicator and place-pool bottom-inset tests.

Task 4: Ruling: “普通文字 Tab”指非胶囊/非填充容器，并非禁用选中态字重；规格明确要求激活态由主色、字重和 3dp 指示线共同表达，因此保留选中加粗。若裁决错误，成本是选中切换存在轻微字宽变化，后续可单点调整。
Task 4: complete (commits 7bd9ec5..336776b, review clean with 1 adjudicated non-blocking interpretation)
- Tests: WorkspaceChromeTest + WorkspaceSearchTabsTest 4/4 PASS; testDebugUnitTest and lintDebug PASS; related device suite 25/26 PASS.
- Deferred to Task 5: placePoolListScrollsAndKeepsSecondCardAboveBottomInset.

Task 5: complete (commits 336776b..96301c1, review clean)
- Tests: WorkspacePlacePoolLayoutTest 2/2 PASS; target bottom-inset test 1/1 PASS; testDebugUnitTest and lintDebug PASS.
- Full 38-test device run did not complete: first run stalled, sole retry exited by signal 9. Ruling: evidence indicates infrastructure failure without product assertion/crash; Task 5 may proceed, but Task 7 must obtain a valid complete device result. Cost if wrong: a cross-scenario regression may remain hidden until Task 7.

Task 6: complete (commits 96301c1..e13546f, review clean)
- Tests: focused device tests 35/35 PASS; testDebugUnitTest and lintDebug PASS; ItineraryEditingTest 5/7 PASS.
- Ruling: failedRouteShowsErrorAndRetryAction is a direct DayItinerarySheet viewport assertion unrelated to the workspace shell; Task 7 may correct the test to scroll to the RouteLeg before asserting visibility. Cost if wrong: a genuine initial-viewport requirement could be weakened.
- Ruling: waitingForNetworkKeepsAllPlaceActions incorrectly expects the itinerary row root to expose OnClick although production exposes reorder custom actions and explicit timing/move/delete buttons; Task 7 may align the assertion with actual accessibility semantics, not make the whole card clickable. Cost if wrong: a desired whole-card interaction would remain absent pending product confirmation.

Task 7: complete pending physical-device acceptance.
- Added real NavHost navigation regression plus long-title physical hit-target and map-failure reachability coverage; no production reducer, SavedState, repository, map lifecycle, or permission-flow changes.
- Corrected the two adjudicated itinerary contracts and stale scenario selectors/gesture injection; `ItineraryEditingTest` 7/7 PASS, workspace navigation focus 13/13 PASS, workspace device suite 53/53 PASS, `V1ScenarioCatalogTest` 47/47 PASS, and `V1FullUiAcceptanceTest` 47/47 PASS on `easy_trip_p60pro(AVD)`.
- Static gate: 330 JVM tests, 0 failed/skipped; lint and app/test APK assembly PASS.
- Mate 60 Pro physical-device acceptance: PENDING/BLOCKED because only emulator-5554 was connected.

Task 7: fix round 1/5 (2 addressed, 0 open).
- Restored real system `BackHandler` coverage and retained top-back coverage. RED proved the callback was enabled and Activity dispatch worked; the route read a stale collected overlay snapshot. The minimal route fix reads `viewModel.state.value.overlay`, so system and top back both close the overlay before leaving.
- Restored the failed-route explanation contract. RED plus merged/unmerged semantics bounds proved there was no clipping: the UI mapper discarded legacy `errorCode` when typed `errorKind` was absent. The mapper now prefers typed summaries and falls back to `errorCode`, with JVM coverage; the device test scrolls to the RouteLeg and verifies both `no route` and retry.
- Long-title back/more/search controls are physically sized, actually clicked, and their callbacks verified.
- Final gate: 331/331 JVM tests, lint and app/test APK assembly PASS; `ItineraryEditingTest` 7/7, workspace device suite 53/53, catalog 47/47, and full UI acceptance 47/47 PASS with zero skipped/failed on `easy_trip_p60pro(AVD)`.
- Mate 60 Pro physical-device acceptance remains PENDING/BLOCKED because only emulator-5554 was connected.

Task 7 fix round 1/5 commit: `2956c6b`.
check: `git diff --check` PASS.
report: `.superpowers/sdd/2026-08-25-easy-trip-workspace-ui-convergence/task-7-report.md`.

