# YUBERA Android development status

**Last updated:** 2026-11-04  
**Repository:** `makellodima5-cmyk/Yubera-Android-`  
**Branch:** `main`

## Implemented

- Native Kotlin + Jetpack Compose foundation, MVVM/repository layers, Hilt, Room, Retrofit, DataStore and Navigation Compose.
- Original YUBERA assets, launcher resources, startup and bottom navigation.
- R2-backed mobile catalogue API, revision-aware Room snapshot cache, search, genre filters, offline fallback and retry UI.
- Home, catalogue, anime detail and player screens. The Kodik player uses the existing YUBERA server route; provider credentials remain server-side and WebView is confined to the player.
- Local-first favorites, watch-later, last-opened episode history and personal subscriptions.
- Shikimori news feed with Room cache, attribution and category filters.
- Profile theme settings, notification preferences, Android notification permission request and server-session logout.
- Telegram native OIDC with PKCE, one-time code exchange, revocable bearer sessions and Android Keystore-backed token storage. Registration remains disabled until legal documents are approved and published.
- Client-side episode notifications: WorkManager with a six-hour target, local active-subscription filtering, Shikimori GraphQL polling, episode watermark and deduplication, retryable delivery, Android notification channel/permission checks. App-level and per-subscription settings are checked again immediately before each notification; favorites and watch-later are not polled.
- Conflict-aware personal-library sync records for favorites, watch-later, subscriptions and last-opened episode progress. Android stores changes in Room first, uses per-item timestamps and deterministic source-ID tie-breaking, retains subscription deletion tombstones, retries failed requests and schedules immediate sync after local changes/login plus a six-hour periodic fallback.
- Website backend endpoint `/api/telegram/library/sync` with bearer/cookie authentication, per-item versions, tombstones, bounded request validation and idempotent conflict resolution. Existing website library and subscription mutations write to the canonical sync state.

## Verified CI

- Android CI #37 passed on `c5e33deb45af53c5078e561c5dbd53e023e2aeae`; notification system/channel/subscription guards compiled and unit tests passed.
- Android CI #41 passed on `ddc98ad83e564158c307d2a84d56ee301f2d7ecc`: SDK verification, debug APK build, unit tests and artifact upload all succeeded.
- Android CI #43 passed on `d9bc83737c6eea4071e5897c404ceec5e495a34d`; the Room v5 migration fix and the full debug build/unit-test workflow passed. Artifact `yubera-debug-apk`, 27,807,233 bytes, expires 2026-11-17.
- Website CI #2568, #2569 and #2570 passed. Run #2570 on `1eafc14f504fc48898eca207f885b305c3ba8ccb` validated automated tests, dependency audit, TypeScript lint, Worker build, clean D1 migrations against local SQLite and dry-run deploy artifacts.
- CI confirms source/build/test validation only. It does not confirm Cloudflare production deployment or real Android runtime behavior.

## Known integration gaps

1. The Android sync worker stores last-opened episode data as `progress` records in `yume_library_sync_items`. The current website sync handler writes favorites, watch-later and subscriptions through to the legacy website tables, but does not project `progress` into `yume_view_history`. Website history parity for Android playback progress is therefore not implemented/verified. A backend change and an agreed mapping are needed; none is made in this Android repository.
2. The existing Kodik WebView does not expose a verified playback-position callback. The app can sync the last opened episode, but does not yet capture a reliable resume position or watch percentage.
3. Production rollout is unverified. The current website Worker config points at the clean `yumedb` database and `migrations-clean`; CI validates this schema locally, but no available evidence confirms the production Worker deployment or applied production D1 migration.
4. Real-account API checks are still required for GET/POST sync, favorite/watch-later add and removal, subscription add/removal, conflict resolution, expired sessions and retry behavior.
5. Device/emulator QA is still required for Telegram deep-link login, cold start, navigation, catalogue, anime detail, player, theme, Room migration and notification delivery.
6. Release signing, final QA and distribution are not configured.

## Milestones

| Milestone | Status |
|---|---|
| 0. Foundation and CI | Implemented; Android CI #43 green |
| 1. Assets | Implemented |
| 2. Architecture | Implemented; local-first data and sync path present |
| 3. Startup and navigation | Implemented; runtime verification pending |
| 4. Native authentication | Implemented; production/runtime verification pending |
| 5. Catalogue | Implemented; production/runtime verification pending |
| 6. Home | Implemented; runtime verification pending |
| 7. Anime detail and player | Implemented; player/device QA pending |
| 8. Personal library and sync | Android client and backend sync records implemented; progress-to-website-history projection, production migration and live conflict/retry verification pending |
| 9. News | Implemented; live API verification pending |
| 10. Profile | Implemented; runtime verification pending |
| 11. Notifications | Implemented; live Shikimori and device delivery verification pending; delivery gates are rechecked during polling |
| 12. QA and release | Pending |

## Important constraints

- Do not claim production API availability until the Worker deployment and D1 migration are verified.
- Do not claim Telegram login, playback, notifications or sync are runtime-verified until tested against the live backend/on Android.
- The user has an iPhone. Testing the Android APK requires an Android device or a cloud emulator.
- No production deployment, production D1 migration or release signing has been performed.

## Change log

- 2026-11-03: Android CI #37 and #43 passed; notification worker checks system/channel/subscription state and now rechecks global settings immediately before delivery.
- 2026-11-03: Website personal-library sync endpoint, version/tombstone D1 state, legacy write-through and origin/timestamp validation passed website CI #2568-#2570.
- 2026-11-03: Android local-first sync implemented for favorites, watch-later, subscriptions and last-opened episode progress. Room v5 migration, per-item conflict ordering, subscription tombstones, WorkManager scheduling and auth-triggered sync passed Android CI #41 and #43.
- 2026-11-04: Audited the current website sync handler and clean D1 schema. Recorded the missing progress-to-legacy-history projection; no website code, production deployment or production D1 data was changed.
