# App Settings and Updates Implementation Plan

**Goal:** Implement the approved global settings and safe manual APK update flow.
**Architecture:** Navigation hosts global settings independently of trips; reuse existing theme and AMap consent stores. Pure release policy and a coroutine controller sit behind update source/download seams; Android adapter verifies APK identity and delegates installation to the OS.
**Tech Stack:** Kotlin, Compose, StateFlow, HTTPS, FileProvider, JUnit/Compose tests.
**Spec:** design/app-settings-update/README.md

## Constraints
- Manual only, no auto checking/downloading/installation, no downgrade, preserve existing preferences/data.
- No unrelated dirty worktree changes, no real-device installation requested.
- Release v1.8.0 in mockup is not an actual release; do not change version just for mockup.
- Test public behavior: release decisions, controller cancellation/retry, APK trust boundary, settings navigation and consent UI.

## Review Focus
- Malformed/untrusted metadata and redirect URLs must fail closed.
- Late completion after cancel/retry cannot restore obsolete UI state.
- Same/older versions and incompatible APK signatures cannot install.
- No trips, denied map consent, rotation/back retain independent settings access.
- Small screens/large fonts keep actions visible and scrollable.

## Tasks
- [x] Release policy + tests: semver/official asset selection/digest/size/error cases.
- [x] HTTPS release and download adapter: limits, redirect policy, SHA256 and cancellation cleanup.
- [x] Update controller + tests: manual check/download, progress, retry/cancel, installation failure.
- [x] Android APK verifier + installer: package/version/signature, unknown-source permission handoff, FileProvider.
- [x] Global settings screens and entry migration; reuse theme/consent without resetting.
- [x] Compose integration tests and existing theme/workspace regression adjustments.
- [x] Build, full unit tests, targeted emulator tests/lint, audit diff and report; online check explicitly separate.

GitHub fetch/PR/latest release currently time out; local development proceeds on e95bd83 (parent 7424bbb), no remote push until latest-main gate can pass.

## Completion
Local build, tests and review completed. Online GitHub check/download remains unverified because of network timeouts; no commit/push/device installation in this development turn.
