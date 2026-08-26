# SDD ledger — plan: docs/superpowers/plans/2026-08-26-easy-trip-itinerary-ui-convergence.md

## Pre-flight

Base: `f26e257987e16679b4a8c0323285c709719c2553`

| Tasks | Producer / consumer boundary | Finding |
|---|---|---|
| 1 → 2/3/6 | Shared ItineraryPlaceContent is consumed by editable row, drag handle, and whole-trip read-only row | Clean; editable behavior must stay outside the primitive |
| 2 → 3 | Final trailing menu geometry must exist before handle hit-target isolation | Clean; run sequentially |
| 3 → 5 | Day timeline consumes handle-only reorder behavior | Clean; timeline must not reattach drag to row |
| 4 → 5/6 | Explicit RouteLeg four-state model is consumed by day and whole-trip renderers | Clean; domain RouteStatus remains unchanged |
| 5 → 6 | Shared empty illustration and EmptyState slots are consumed by whole-trip empty state | Clean; no duplicated illustration implementation |
| 2 → 6 | Menu actions must establish only existing draft/editor/confirmation state before workspace overlay appears | Clean; no selectedMenuItemId in ViewModel |
| 7 | Full connected gates share device resources | Must run serially and accept at most one infrastructure retry |
| 1 | Existing ItineraryItemRow has card/elevation and permanent action buttons | Internally consistent RED target |
| 2 | Suggested spec signature onOpenMenu is insufficient for direct menu item mapping | Ruling: use row-local Boolean plus `onMenuAction`; this better satisfies the spec's single-source constraint. Cost if wrong: component API differs from the non-binding example signature. |
| 3 | Changing 120f px to 120dp changes high-density physical drag distance | Ruling: follow approved spec; record final hand feel for consolidated device acceptance. Cost if wrong: drag may feel too long on high-density devices until measured-row follow-up. |
| 4 | Domain has five statuses while approved UI has four | Ruling: map PENDING and CALCULATING to UI Calculating; do not alter domain state. Cost if wrong: queueing and active calculation remain visually indistinguishable. |
| 4 | Existing mode action appears for all RouteLeg states | Ruling: approved design restricts mode editing to Ready. Cost if wrong: users lose an intentional offline/pre-compute mode-edit shortcut. |
| 6 | Rail and empty state both expose add-day | Ruling: keep both; both dispatch the same `onAddDay` and create no second flow. Cost if wrong: duplicate entry may feel redundant in final device review. |

## Progress

Task 1: complete (commits f26e257..0e43caf, review clean)
- Tests: ItineraryTimelineContentTest 4/4 PASS; WholeTripItineraryContentTest 4/4 PASS; ItineraryEditingTest 7/7 PASS; unit and Android test compilation PASS.
- Minor (deferred): zero-elevation test does not directly observe elevation; final review should confirm no runtime regression.
- Minor (deferred): editable/read-only geometry test compares primitive instead of rendering ItineraryItemRow; production currently shares the primitive.
- Infrastructure note: combined instrumentation class parameter yielded 0 tests/process crash; split class runs passed.

Task 2: complete (commits 0e43caf..5b209b0, review clean)
- Tests: menu focused 15/15 PASS; related device regression 51/51 PASS; unit/lint/app and test APK assembly PASS.
- Minor (carried to Task 3): drag and more affordances use text glyphs without explicit 20–22dp icon sizing; Task 3 must replace them with fixed-size icons while isolating the handle hit target.
- Minor (deferred final review): ViewModel tests prove state exclusivity but not the full menu click → workspace overlay chain; existing editing flow and source wiring cover it.

Task 3: fix round 1/5 (2 addressed, 0 open; commits 69c8b13..998cc07)
Task 3: complete (commits 5b209b0..998cc07, review clean)
- Tests: Timeline + Editing 22/22 PASS; unit/lint PASS.
- Handle-only drag, 120.dp.toPx threshold, callback freshness and cancel restore are covered.
- Minor carried to final review: more-menu affordance still uses a text glyph without explicit 22dp sizing.

Task 4: complete (commits 998cc07..ce2b67e, review clean)
- Tests: focused mapper PASS; Timeline + Editing 27/27 PASS; lint/app and test APK assembly PASS.
- Minor (deferred final review): long-error test checks max lines but does not prove absence of a fixed height.
- Minor (deferred final review): four-state text tests use global selectors rather than scoping each assertion under its leg tag.

Task 5: fix round 1/5 (1 addressed, 0 open; commits ee80815..f840f71)
Task 5: complete (commits ce2b67e..f840f71, review clean)
- Tests: Timeline + Editing 33/33 PASS; unit/lint/app and test APK assembly PASS.
- Empty day with non-empty savedPlaces exposes only AddPlaces; non-empty itinerary retains direct AddPlace shortcuts.

Task 6: Ruling: retain the small `DayItineraryViewModel` Ready-state guard and `TripWorkspaceRoute` overlay derivation changes despite the task brief's narrower production-file list. The approved spec requires only Ready mode editing and business-state-derived overlays; reverting would violate those requirements. Cost if wrong: Task 6 carries integration changes that could have been isolated in a later task and broadens its review surface.
Task 6: fix round 1/5 (5 addressed, 0 open; commits 5d26701..a601f08)
Task 6: complete (commits f840f71..a601f08, review clean)
- Tests: targeted JVM PASS; Task 6 device suite 29/29 PASS; unit/lint/app and test APK assembly PASS.
- Confirm-before-mutation, menu dismiss, separate add-day entries, and whole-trip forbidden menu/drag tags are covered.

Task 7: whole-branch fix wave complete (commits 245a2b2..9491879, scoped re-review clean)
Task 7: complete (commits a601f08..9491879, review clean)
- Tests: JVM 338/338 PASS; focused Compose 87/87 PASS; Catalog 47/47 PASS; Full UI 47/47 PASS; lint/app and test APK assembly PASS; 0 failed/skipped.
- Accessibility fixes: Calculating RouteLeg uses Polite live region; more-menu icon is fixed 22dp inside a 40dp touch target.
- Minor (deferred): handle itself has no explicit 40x40 test, though production uses size(40.dp).
- Minor (deferred): zero-elevation, full editable/read-only geometry, RouteLeg adaptive-height, and per-leg selector scoping tests remain weaker than ideal; production inspection and complementary tests found no blocking defect.
- Physical-device acceptance: DEFERRED by user; not recorded as PASS.

Task 7: complete (candidate gates PASS; commit recorded in task report)
- Tests: JVM 338/338 PASS; focused device suite 87/87 PASS; Catalog 47/47 PASS; Full UI 47/47 PASS; all runs 0 failed and 0 skipped.
- Static gates: lintDebug, assembleDebug, and assembleDebugAndroidTest PASS.
- Selector migration: scenario 14 now asserts two “正在计算路线” nodes for PENDING/CALCULATING, preserving both domain-state fixtures and strengthening the merged UI-state assertion.
- Physical-device acceptance: DEFERRED by decision; not recorded as PASS.
- Whole-branch fix wave: strict RED confirmed missing `more-icon-i1` and missing Calculating `LiveRegionMode.Polite`; production now uses a fixed 22dp Canvas icon inside the unchanged 40dp menu target and exposes polite live-region semantics on the calculating leg.
- Bounds coverage now verifies the 40dp menu target, 22dp icon, and non-overlap with the drag handle.
- Post-fix gates: JVM 338/338 PASS; focused device suite 87/87 PASS; Catalog 47/47 PASS; Full UI 47/47 PASS; all runs 0 failed and 0 skipped. lintDebug, assembleDebug, and assembleDebugAndroidTest PASS.
- Remaining whole-branch review items: zero-elevation test strength; editable/read-only geometry test strength; RouteLeg fixed-height and selector-scope test strength. The more text glyph item is closed.
- Acceptance and conflict records: docs/testing/itinerary-ui-acceptance.md and docs/testing/itinerary-ui-conflicts.md.
