# Stage 1 — Android Technical Foundation

Scope: Yubera Android only. This branch contains Stage 1 foundation work. UI design/navigation, player implementation/testing, notifications, and APK updater are excluded.

## Existing foundation inspected

- Kotlin + Jetpack Compose; application ID `com.yubera.app`; minimum API 26.
- Hilt, Retrofit/OkHttp, Kotlin Serialization, Room, DataStore, WorkManager.
- Native Telegram OIDC callback via `yubera://auth/callback`.
- Native exchange: `POST api/telegram/mobile-auth`; session inspection: `GET api/telegram/init-data`.
- Catalog: `GET api/mobile/catalog`, paginated; snapshot cached in Room with revision metadata.
- Local account data partitioned by account ID and synchronized with the existing account-bound service.
- Existing tests cover session lifecycle, catalog validation, library mutations, and sync conflict policy.

## Changes in this branch

- Catalog refreshes are serialized per app process so concurrent callers cannot download and replace snapshots simultaneously.
- Authentication restoration now requires a token to be associated with the currently selected account (or a specifically marked legacy account awaiting first server identity confirmation).
- Session restoration verifies the token, stored account ID, and active account have not changed while the server request is in flight.
- Offline restoration is limited to a previously account-bound session.
- Profile responses are discarded if the active account changes during the request.
- Logout attempts local credential cleanup even if remote logout fails.
- Failure to remove a stale pending OAuth state after a successfully saved session no longer makes the completed login report as failed.
- Added tests for concurrent catalogue refresh and auth logout/cleanup edge cases.

## Current validation

- [x] CI failure on the default branch investigated: Android resource merging failed because a file-based resource name started with uppercase `Y` (`YUBERA_logo_transparent.webp`). The lowercase-named asset is present in the working branch.
- [x] Initial CI reached compilation and exposed two deprecated coroutine builder calls in the new concurrency test.
- [x] Replaced those calls with structured coroutineScope/async imports.
- [x] Fixed the first CI failure caused by unstructured async calls in the new concurrency test.
- [x] Fixed the next CI assertion: both refresh coroutines are now advanced deterministically in the test scheduler.
- [x] Build and lint tasks completed in CI; all unit tests except the added concurrency test passed.
- [x] Reworked the concurrency regression test to verify the actual cache-revision contract: concurrent refreshes may both request the first page to read its revision, but only one downloads the remaining catalog pages.
- [ ] Run CI on the latest branch head and confirm build, lint, and unit tests pass.

## Out of scope

- Do not change Website/backend.
- Do not implement visual design, navigation-bar structure, player, notifications, or APK updater in Stage 1.
- Do not expose internal infrastructure labels in user-visible copy.
