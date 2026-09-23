# Global Themes Implementation Plan

> **For agentic workers:** Implement this plan task-by-task in the current isolated feature worktree. The installed writing-plans skill references superpowers execution skills that are not installed; execute inline with the same verification gates.

**Goal:** Apply the approved five light palettes globally, with local preview, reliable persistence and both entry points.

**Architecture:** ThemePalette owns complete Material roles; ThemePreferenceStore reads an AtomicFile before first composition and writes on IO before publishing its StateFlow. ThemePickerViewModel controls a full-screen dialog over the existing navigation tree, so selecting and returning never recreate the underlying workspace or camera.

**Tech Stack:** Kotlin, Compose Material3, StateFlow, Android AtomicFile, JUnit, Compose instrumentation.

## Global Constraints

- Five stable IDs: lake, forest, sunset, violet, rose; default and unknown IDs resolve to lake.
- Rose primary is #934D63. Fixed date palette, note semantics, errors and export template stay unchanged.
- 62dp minimum option rows, 48dp controls; scroll for large fonts/short screens.
- Preview is temporary; persist before applying. Saving blocks selection/submission/all back actions; failure keeps preview and previous saved theme.
- All trips share one preference; no network dependency. No push or release in this task.
- Source backup: .scratch/v1.6.0-theme-implementation/before/source.tar.

### Task 1: Palette and persistence

Files: core/ui/theme/ThemePalette.kt, ThemePreferenceStore.kt, ThemePickerViewModel.kt; tests under app/src/test/java/com/yangchengwei/easytrip/core/ui/theme/.
Interfaces: ThemePalette.fromId(String?): ThemePalette; ThemePersistence.read(): String? and write(String); ThemePreferenceStore.theme: StateFlow<ThemePalette>; suspend apply(ThemePalette): Result<Unit>.

- [x] Test missing/unknown IDs, restart readback, failed writes retaining saved state, retry and serialized concurrent saves.
- [x] Define all five palettes from design/v1.6.0-themes.md; generate complete ColorScheme without dynamic/system colors.
- [x] Implement AtomicFile write with finishWrite/failWrite; execute IO inside Mutex and NonCancellable so persistence and published state cannot diverge on cancellation.
- [x] Test viewmodel open/select/back, unchanged disabled action, saving guards and retry.
- [x] Run `./gradlew :app:testDebugUnitTest --tests '*Theme*Test'` and require zero failures.

### Task 2: Picker and integration

Files: core/ui/theme/ThemePickerScreen.kt, ThemeHost.kt, Theme.kt, EasyTripApplication.kt, MainActivity.kt, trip/ui/TripListContent.kt, workspace/WorkspaceMoreMenu.kt.
Interfaces: EasyTripTheme(palette: ThemePalette = LAKE, content); LocalThemePicker.current invokes the shared picker; EasyTripThemeHost(store, content) applies saved palette and owns dialog/viewmodel.

- [x] Reproduce approved preview card and five swatch rows. Use selectable radio semantics and selected border/check, bottom fixed action, scrollable content and safe insets.
- [x] Add theme icon in the existing 48dp header target and menu item with current theme + 所有旅行. Preserve four current menu actions.
- [x] Set main/dialog system bars from their palettes; publish success toast after saved state updates. Dialog BackHandler delegates to guarded dismiss.
- [x] Instrument preview/discard/apply/failure/retry/saving and both entry points. Verify workspace state and map host survive.

### Task 3: Color propagation and version

Files: static-color users in core/ui/component, trip/ui, place/ui, itinerary/ui; workspace/AmapComposeMap.kt; share/ItineraryShareScreen.kt; app/build.gradle.kts.
Interfaces: AmapMapHost.setPalette(ThemePalette) invalidates overlay rendering without consuming a new viewport command. Pure helper color parameters default to the existing lakeside palette for compatibility.

- [x] Replace UI static color references with Material roles; pass colors into pure string/drawing helpers.
- [x] Pass selected palette to native marker creation and invalidate overlays only when it changes.
- [x] Keep routeColorForDay and share export renderer unchanged; use theme colors for share preview chrome and unscheduled calendar cards.
- [x] Set v1.6.0/versionCode 8, leaving release signing and publication untouched.

### Task 4: Validation and delivery

- [x] Run `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug` and inspect all failures and lint findings.
- [x] Run targeted emulator tests on emulator-5596 covering picker states, AtomicFile restart, theme semantics and existing navigation/layout.
- [x] Capture actual picker/list/workspace screenshots across palettes and large-font/short-screen case; visually inspect for clipping and preserve fixed date colors.
- [x] Record results in .scratch/v1.6.0-theme-implementation/verification.md; mark design approved and update task state. Review diff and notify via authorized punk-12 bot, then read back message.

Rollback: restore only files changed by this task from the source archive (or revert its local commit). New files can be removed individually. Do not overwrite concurrent edits; do not reset the repository or push.

Validation note: the failure-state instrumented test uses synchronous Espresso back dispatch to avoid a pending global accessibility back action arriving after save completion. Eight final theme instrumentation tests pass. Three existing regression failures were independently reproduced on unchanged HEAD c1663fb; see verification.md.
