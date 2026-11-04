# YUBERA Android development status

**Last updated:** 2026-11-04  
**Repository:** `makellodima5-cmyk/Yubera-Android-`  
**Branch:** `main`  
**Current source commit:** `ff8edc6313ed8a04f9a808d8058734226c065a2f`  
**Last verified CI commit:** `ebe3e260e568ffd3ecd6b5a714af2df56c90de1c` (Android CI #52 passed; CI #53 is running and CI #54 will cover the subsequent news-search change)

## Implemented

- Native Kotlin + Jetpack Compose foundation, MVVM/repository layers, Hilt, Room, Retrofit, DataStore and Navigation Compose.
- Original YUBERA assets, launcher resources, startup and bottom navigation.
- R2-backed mobile catalogue API, revision-aware Room snapshot cache, search, genre/year/status filters, sorting by API order/rating/year/title, offline fallback and retry UI.
- Home, catalogue, anime detail and player screens. Home includes continue-watching cards with stored episode/time progress, a real-data high-rating collection (only titles with a supplied rating), and the original catalogue picks. News search is implemented in source across title, summary, linked title and author; CI verification pending. The Kodik player uses the existing YUBERA server route; provider credentials remain server-side and WebView is confined to the player.
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
- **Android CI #50 passed** on `21341f4240b4601417a1f8bb567347ea59fe9499`: debug resources, debug APK assembly, unit tests and artifact upload all succeeded. Remote progress-deletion tombstones now remove the matching account-scoped Room progress row.\n- **Android CI #51 passed** on `9183f9aa74c34ad71f64334a2aa2e6c61b0a6504`: APK build and unit tests succeeded.\n- **Android CI #52 passed** on `ebe3e260e568ffd3ecd6b5a714af2df56c90de1c`: continue-watching cards display poster, season/episode, stored playback progress and a progress bar; debug APK build, unit tests and artifact upload succeeded.\n- Android CI #53 is running for `df2ead6cdcafda351308387af5665bcb7cab8dac`, covering catalogue sorting, year/status filters, rating-based home collection and policy tests. CI #54 will cover local news search added after #53 started.
- Website CI #2568, #2569, #2570 and #2572 passed. Website CI #2572 validates the Android episode-progress projection into viewing history, deletion behavior, Worker build and the clean migration/test pipeline.
- CI confirms source/build/test validation only. It does not confirm Cloudflare production deployment, live-account API behavior or real Android runtime behavior.

## Known integration gaps and constraints

1. Android `progress` records are now projected by the website sync handler into `yume_view_history`, including season, episode, playback position, duration and progress timestamp. Website history DELETE writes a tombstone; Android applies remote progress tombstones to its account-scoped Room row. Website CI #2572 and Android CI #50 passed. The website migration `migrations-clean/0003_view_history_episode_progress.sql` adds the required history columns. Production migration/deployment and real-account cross-device behavior remain unverified.
2. The existing Kodik WebView does not expose a verified playback-position callback. The app can sync the last opened episode, but does not yet capture a reliable resume position or watch percentage.
3. Production rollout is not independently verified. The user reports that migrations 0001 and 0002 have already been applied; this has not been independently confirmed against the production D1 database. The new website migration `0003_view_history_episode_progress.sql` is pending. No production deployment or migration was initiated by this work.
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
| 5. Catalogue | Search, genre/year/status filters and sorting implemented in source; CI #53 running; production/runtime verification pending |
| 6. Home | Continue-watching progress card and rating-ranked real-data collection implemented in source; CI #52 passed, CI #53 covers current changes; runtime verification pending |
| 7. Anime detail and player | Implemented; player/device QA pending |
| 8. Personal library and sync | Account-scoped storage and sync implemented; episode progress projects to website history; production migration 0003, live conflict/retry and cross-device checks pending |
| 9. News | Cached feed, category filters and source attribution implemented; local search added in source, CI #54 pending; article detail/bookmarks/sharing still pending |
| 10. Profile | Implemented; runtime verification pending |
| 11. Notifications | Implemented; account-scoped worker and delivery guards pass CI; live Shikimori and device-delivery verification pending |
| 12. QA and release | Pending |

## Change log

- 2026-11-03: Android CI #37 and #43 passed; notification worker checks system/channel/subscription state and rechecks global settings immediately before delivery.
- 2026-11-03: Website personal-library sync endpoint, version/tombstone D1 state, legacy write-through and origin/timestamp validation passed website CI #2568-#2570.
- 2026-11-03: Android local-first sync implemented for favorites, watch-later, subscriptions and last-opened episode progress. Room v5 migration, per-item conflict ordering, subscription tombstones, WorkManager scheduling and auth-triggered sync passed Android CI #41 and #43.
- 2026-11-04: Account partitioning added to Room schema v6, repository flows, library sync and episode-notification workers. Existing rows are preserved in `legacy-unassigned` until an existing session is verified.
- 2026-11-04: Native session restoration hardened against a missing token, a stale response after logout/account switch and mismatch between the persisted account ID and backend `user.id`. Account isolation and authentication edge-case unit tests passed Android CI #48.
- 2026-11-04: Website episode-progress projection and deletion tombstones implemented; website CI #2572 and Android CI #50 passed. Added clean D1 migration 0003; production migration/deployment remains pending. User reports website migrations 0001 and 0002 are already applied.
\n\n- 2026-11-04: Android CI #51 passed. Home continue-watching cards now show poster, episode, playback position/duration and stored progress bar; Android CI #52 passed. Added catalogue sorting (catalog order, rating, year, title), combined year/status filters, result count and high-rating home collection based only on catalog ratings; Android CI #53 running. Added local news search across title, summary, linked title and author with unit tests; CI #54 pending.\n