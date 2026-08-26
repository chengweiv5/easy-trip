# Task 8 Report

## Result

Implemented fixed per-place quick-add and more-menu actions and connected `StartAddSingle(placeId)` to the existing workspace add-to-itinerary coordinator. The overlay remains state-driven and opens only after `AddToItineraryViewModel` reaches `SELECT_TARGET_DAY`.

## Behavior

- Each row exposes 40dp quick-add and more touch targets with 22dp drawn icons.
- Place name and address are capped at two lines.
- Both action descriptions include the place name.
- Menu expansion state stays inside `SavedPlaceRow`; the menu contains only edit and error-colored delete actions bound to the current row.
- The public `PlacePoolSheet` hides quick-add and batch-add entry points unless callbacks are supplied.
- Batch add continues dispatching `StartAddToItinerary` and calling `startFromPool()`.
- Single add dispatches only `StartAddSingle(currentPlace.id)` and calls `startForPlace(id)`.
- Invalid places and trips without days leave the add flow idle and do not open the target-day overlay.
- Updated stale tag-input tests to use the shared `newTagInput` plus Add interaction.

## TDD evidence

Before implementation, the new row test failed because the named quick-add semantic node did not exist, and the workspace test failed because `quick-add-place-p` did not exist. After implementation, the new and affected targeted tests passed.

## Verification

- `PlacePoolFlowTest` full class: attempted three times. The first run exceeded 600 seconds after 3/17 tests and was safely terminated after process/log inspection. A later run crashed its instrumentation process after the first test; XML reported `Process crashed`, and logcat showed the app process exited with signal 9. The final run again exceeded 600 seconds at 3/17 with no test failure and was safely terminated. These are runner/emulator failures rather than assertion evidence for the changed behavior.
- Place pool affected tests: 6/6 passed, including row bounds/semantics/current IDs, no-coordinator hiding, both edit-delete flows, shared detail tags, and search-save-edit-filter.
- Workspace full class: instrumentation crashed after 1/19; Gradle reported `Test run failed to complete. Instrumentation run failed due to Process crashed`.
- Workspace affected tests: 3/3 passed, covering valid single add, invalid/no-day guards, and the updated place edit entry.
- Additional delete regression pair: 2/2 passed after aligning the cancellation fixture with the confirmation contract (a referenced place is required; an unreferenced place is deleted immediately).
- `:app:testDebugUnitTest`: passed.
- `:app:compileDebugKotlin`: passed.
- `:app:compileDebugAndroidTestKotlin`: passed.
- `:app:assembleDebug`: passed.
- `git diff --check`: passed.
- `graphify update .`: completed; it reported pre-existing partial AST extraction warnings for `NetworkMonitor.kt` and `RoutePlanner.kt`.

## Diagnostic evidence

- First full-class timeout output: `/private/tmp/claude-501/-Users-bytedance-Code-easy-trip/14258c9c-42f5-40e8-81c4-705321039e1f/tasks/b432zq5kh.output`
- Final full-class timeout output: `/private/tmp/claude-501/-Users-bytedance-Code-easy-trip/14258c9c-42f5-40e8-81c4-705321039e1f/tasks/bavslmjtd.output`
- Android test XML/report directory: `app/build/outputs/androidTest-results/connected/debug/` and `app/build/reports/androidTests/connected/debug/`
- Crash evidence observed in the generated XML (`Process crashed`) and connected-test logcat (`signal 9 (Killed)`).

## Fix round 1

- Bound overlay opening to the explicit Boolean result of the current start command. `startForPlace` now succeeds only from an idle, unlocked draft with a current place and at least one travel day; failed starts preserve old bulk/restored/locked drafts and cannot expose them.
- Disabled automatic opening from `WorkspaceOverlay.None`; the state observer now only advances an already-active add-to-itinerary overlay. Explicit pool, single-place, and selected-day entry points open their initial overlay only when their start command succeeds.
- Added coverage for old bulk target-day state, restored target-day state, repeated single-place starts, locked drafts, invalid places, no-day trips, and the selected-day entry point.
- Added stable place IDs as `LazyColumn` keys so row-local menu state remains attached across reorder, and changed the more icon to horizontal dots.
- Updated workspace/layout tests to use the real more menu rather than removed direct edit/delete tags. A repository-wide test search found no stale `edit-place-*` or `delete-place-*` selectors.
- Delete danger styling was verified in source as `MaterialTheme.colorScheme.error`; no pixel/color assertion was added because Compose semantics do not expose the resolved text color reliably here.

### Fix round 1 TDD and verification

- RED: Boolean start-result test failed to compile while pool/day start APIs returned `Unit`; GREEN after returning explicit results and consuming them in the route.
- RED: menu reorder test lost `menu-delete-place-first`; GREEN after keying lazy rows by place ID.
- RED: selected-day route regression initially found no add button because the test remained on the place-pool section; after entering the itinerary section it passed and verified the `ForDay(day-1)` flow and `SelectAddPlaces` overlay.
- `WorkspaceFlowTest`: 21/21 passed as a full class.
- Workspace affected quick-add/delete group: 7/7 passed.
- `WorkspacePlacePoolLayoutTest` plus affected place-pool tests: 4/4 passed.
- Related JVM classes (`AddToItineraryStateTest`, `TripWorkspaceNavigationStateTest`): passed.
- `:app:compileDebugKotlin`, `:app:compileDebugAndroidTestKotlin`, and `:app:assembleDebug`: passed.
- `PlacePoolFlowTest` full-class rerun again exceeded 600 seconds without reporting an assertion failure and was moved to background task `b6k5ed6iq`; targeted affected tests passed independently.

## Scope

No push was performed. `diagrams/` and the existing design file were not modified or staged.
