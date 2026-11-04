# YUBERA Android development status

**Last updated:** 2026-11-04  
**Repository:** `makellodima5-cmyk/Yubera-Android-`  
**Branch:** `main`  
**Current verified Android commit:** `76397dd9ddfbcc24448da6e951906790e779b17c`

## Implemented

- Native Kotlin + Jetpack Compose foundation, MVVM/repository layers, Hilt, Room, Retrofit, DataStore and Navigation Compose.
- Original YUBERA assets, launcher resources, startup and bottom navigation.
- R2-backed mobile catalogue API, revision-aware Room snapshot cache, search, genre filters, offline fallback and retry UI.
- Home, catalogue, anime detail and player screens. The Kodik player uses the existing YUBERA server route; provider credentials remain server-side and WebView is confined to the player.
- Local-first favorites, watch-later, last-opened episode history and personal subscriptions.
- Shikimori news feed with Room cache, attribution and category filters.
- Profile theme settings, notification preferences, Android notification permission request and server-session logout.
- Telegram native OIDC with PKCE, one-time code exchange, revocable bearer sessions and Android Keystore-backed token storage. Registration remains disabled until legal documents are approved and published.
- Account-isolated local personal data. Room schema v6 uses composite keys containing `accountId` for favorites/watch-later, progress, subscriptions and episode-notification deduplication. Migration 5→6 preserves existing rows in the `legacy-unassigned` partition instead of assigning them to an arbitrary account.
- The active account ID is stored encrypted alongside the bearer token. Logout clears the active session but preserves each account's local rows. Personal-library flows and all personal writes are scoped to the active account.
- A previously stored session may claim `legacy-unassigned` rows only after the backend confirms the bearer token's account ID. A fresh login does not automatically claim ambiguous legacy rows. Session restoration fails closed if the stored account identity differs from the server identity, or if a stale response arrives after the token has changed.
- Library sync and episode-notification workers use the same account partition as the active session and recheck the token/account before network commits and notification delivery.
- Client-side episode notifications: WorkManager with a six-hour target, local active-subscription filtering, Shikimori GraphQL polling, episode watermark and deduplication, retryable delivery, Android notification channel/permission checks. App-level and per-subscription settings are checked immediately before each notification; favorites and watch-later are not polled.
- Conflict-aware personal-library sync records for favorites, watch-later, subscriptions and last-opened episode progress. Android stores changes in Room first, uses per-item timestamps and deterministic source-ID tie-breaking, retains subscription deletion tombstones, retries failed requests and schedules immediate sync after local changes/login plus a six-hour periodic fallback.
- Website backend endpoint `/api/telegram/library/sync` with bearer/cookie authentication, per-item versions, tombstones, bounded request validation and idempotent conflict resolution. Existing website library and subscription mutations write to the canonical sync state.

## Verified CI

- Android CI #37 passed on `c5e33deb45af53c5078e561c5dbd53e023e2aeae`; notification system/channel/subscription guards compiled and unit tests passed.
- Android CI #41 passed on `ddc98ad83e564158c307d2a84d56ee301f2d7ecc`: SDK verification, debug APK build, unit tests and artifact upload all succeeded.
- Android CI #43 passed on `d9bc83737c6eea4071e5897c404ceec5e495a34d`; the Room v5 migration fix and the full debug build/unit-test workflow passed.
- Android CI #45 passed on `a8c92309412270792e00c8709005969edd932749`; notification delivery now distinguishes a user-disabled app preference from a system channel/permission block and retries the latter.
- Android CI #46 failed at `compileDebugKotlin` because the first account-scoped DAO update left the notification worker calling the old DAO signatures. The worker was updated to use the active account ID in commit `f2be99345f3478619d6a8372065120c992870672`.
- Android CI #47 compiled the production Kotlin sources and assembled the debug APK, but failed at `compileDebugUnitTestKotlin` because a test fake property generated a JVM getter with the same signature as the interface method. The fake was renamed; account-identity mismatch and missing-token tests were added.
- **Android CI #48 passed** on `76397dd9ddfbcc24448da6e951906790e779b17c`: SDK verification, `:app:processDebugResources`, `:app:assembleDebug`, `:app:testDebugUnitTest` and artifact upload all succeeded. Artifact `yubera-debug-apk`, ID `13310689399`, 27,827,345 bytes, expires 2026-11-18.
- Website CI #2568, #2569 and #2570 passed. Run #2570 on `1eafc14f504fc48898eca207f885b305c3ba8ccb` validated automated tests, dependency audit, TypeScript lint, Worker build, clean D1 migrations against local SQLite and dry-run deploy artifacts.
- CI confirms source/build/test validation only. It does not confirm Cloudflare production deployment, live-account API behavior or real Android runtime behavior.

## Known integration gaps and constraints

1. The Android sync worker stores last-opened episode data as `progress` records in `yume_library_sync_items`. The current website sync handler writes favorites, watch-later and subscriptions through to the legacy website tables, but does not project `progress` into `yume_view_history`. The existing history endpoint accepts an anime ID and watched seconds, not an episode number or playback position. Website history parity for Android episode progress is therefore not implemented/verified and requires a separate backend contract/design. No website code was changed for this work.
2. The existing Kodik WebView does not expose a verified playback-position callback. The app can sync the last opened episode, but does not yet capture a reliable resume position or watch percentage.
3. Production rollout is unverified. The current website Worker config points at the clean `yumedb` database and `migrations-clean`; CI validates this schema locally, but no available evidence confirms the production Worker deployment or application of production D1 migration `0002_library_sync_items.sql`. No production deployment or migration was initiated.
4. Legacy rows migrated as `legacy-unassigned` remain preserved but are not assigned to a fresh account. They can be claimed only when an existing bearer session is successfully validated and the backend identifies its owner. If the app starts offline after migration, these rows remain local and do not sync until a later successful session validation; there is no manual legacy-data recovery UI yet.
5. Real-account API checks are still required for GET/POST sync, favorite/watch-later add and removal, subscription add/removal, conflict resolution, expired sessions, account switching and retry behavior.
6. Device/emulator QA is still required for Telegram deep-link login, cold start, navigation, catalogue, anime detail, player, theme, Room 5→6 migration and notification delivery. The successful CI is not a substitute for this runtime testing.
7. Release signing, final QA and distribution are not configured.

## Milestones

| Milestone | Status |
|---|---|
| 0. Foundation and CI | Implemented; Android CI #48 green |
| 1. Assets | Implemented |
| 2. Architecture | Implemented; local-first data and account-scoped sync path present |
| 3. Startup and navigation | Implemented; runtime verification pending |
| 4. Native authentication | Implemented; CI tests pass; live/runtime verification pending |
| 5. Catalogue | Implemented; production/runtime verification pending |
| 6. Home | Implemented; runtime verification pending |
| 7. Anime detail and player | Implemented; player/device QA pending |
| 8. Personal library and sync | Account-scoped Android storage and client implemented; production migration, live conflict/retry checks and progress-to-website-history projection pending |
| 9. News | Implemented; live API verification pending |
| 10. Profile | Implemented; runtime verification pending |
| 11. Notifications | Implemented; account-scoped worker and delivery guards pass CI; live Shikimori and device-delivery verification pending |
| 12. QA and release | Pending |

## Change log

- 2026-11-03: Android CI #37 and #43 passed; notification worker checks system/channel/subscription state and rechecks global settings immediately before delivery.
- 2026-11-03: Website personal-library sync endpoint, version/tombstone D1 state, legacy write-through and origin/timestamp validation passed website CI #2568-#2570.
- 2026-11-03: Android local-first sync implemented for favorites, watch-later, subscriptions and last-opened episode progress. Room v5 migration, per-item conflict ordering, subscription tombstones, WorkManager scheduling and auth-triggered sync passed Android CI #41 and #43.
- 2026-11-04: Account partitioning added to Room schema v6, repository flows, library sync and episode-notification workers. Existing rows are preserved in `legacy-unassigned` until an existing session is verified.
- 2026-11-04: Native session restoration hardened against a missing token, a stale response after logout/account switch and mismatch between the persisted account ID and backend `user.id`. Account isolation and authentication edge-case unit tests passed Android CI #48.
- 2026-11-04: Audited the current website sync handler and clean D1 schema. Recorded the missing progress-to-legacy-history projection; no website code, production deployment or production D1 data was changed.
