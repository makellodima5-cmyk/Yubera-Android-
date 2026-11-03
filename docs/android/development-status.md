# YUBERA Android development status

**Last updated:** 2026-11-03  
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
- Client-side episode notifications: WorkManager with a six-hour target, local active-subscription filtering, Shikimori GraphQL polling, episode watermark and deduplication, retryable delivery, Android notification channel/permission checks. Favorites and watch-later are not polled.
- Conflict-aware personal-library sync for favorites, watch-later, subscriptions and last-opened episode progress/history. Android stores changes in Room first, uses per-item timestamps and deterministic source-ID tie-breaking, retains subscription deletion tombstones, retries failed requests and schedules immediate sync after local changes/login plus a six-hour periodic fallback.
- Website backend endpoint `/api/telegram/library/sync` with bearer/cookie authentication, per-item versions, tombstones, bounded request validation, idempotent conflict resolution, legacy website write-through and a D1 migration. Existing website library and subscription mutations update the same canonical sync state.
- Room schema v5 contains independent favorite/watch-later versions and subscription tombstone state. Sync scheduling starts after successful native login.

## Verified CI

- Android CI #33 passed: debug APK build, unit tests and artifact upload.
- Android CI #36 passed on `6fe52b8f5c5780f8f96a1e9cbed2f3f641be2d2f`.
- Android CI #37 passed on `c5e33deb45af53c5078e561c5dbd53e023e2aeae`; notification system/channel/subscription guards compiled and unit tests passed.
- Android CI #41 passed on `ddc98ad83e564158c307d2a84d56ee301f2d7ecc`: SDK verification, debug APK build, unit tests and artifact upload all succeeded. Artifact `yubera-debug-apk`, 27,807,106 bytes, SHA-256 `2371e170c92a0d43d2645a098eeaa6021f1f6296a6a15b0e5d836114ca7ca2af`, expires 2026-11-17.
- Website CI #2568, #2569 and #2570 passed. The latest run (#2570, commit `1eafc14f504fc48898eca207f885b305c3ba8ccb`) passed automated tests, dependency audit, TypeScript lint, Worker build, D1 clean-migration validation and deploy-artifact checks.
- CI confirms code compilation and automated tests only. It does not confirm Cloudflare production deployment or real Android runtime behavior.

## Remaining before first hands-on Android QA

1. **Production backend rollout:** verify the website Worker build includes the sync endpoint and that the new D1 migration has been applied. No production deployment/migration has been initiated.
2. **Runtime API verification:** authenticate with a real account and exercise GET/POST sync, favorite/watch-later add and removal, subscription add/removal, conflict resolution, expired session and retry behavior against the deployed Worker.
3. **Device/emulator QA:** verify Telegram deep-link login, cold start, navigation, catalogue, anime detail, player, theme, Room migration and notifications on Android.
4. **Player progress limitation:** the current Kodik WebView does not expose a verified playback-position callback. The app can sync the last opened episode, but does not yet capture a reliable resume position or watch percentage.
5. **Release readiness:** debug APK exists; release signing, final QA and distribution are not configured.

## Milestones

| Milestone | Status |
|---|---|
| 0. Foundation and CI | Implemented; Android CI #41 green |
| 1. Assets | Implemented |
| 2. Architecture | Implemented; local-first data and sync path present |
| 3. Startup and navigation | Implemented; runtime verification pending |
| 4. Native authentication | Implemented; production/runtime verification pending |
| 5. Catalogue | Implemented; production/runtime verification pending |
| 6. Home | Implemented; runtime verification pending |
| 7. Anime detail and player | Implemented; player/device QA pending |
| 8. Personal library and sync | Client and backend code implemented; production migration and live conflict/retry verification pending |
| 9. News | Implemented; live API verification pending |
| 10. Profile | Implemented; runtime verification pending |
| 11. Notifications | Implemented; live Shikimori and device delivery verification pending |
| 12. QA and release | Pending |

## Important constraints

- Do not claim production API availability until the Worker deployment and D1 migration are verified.
- Do not claim Telegram login, playback, notifications or sync are runtime-verified until tested against the live backend/on Android.
- The user has an iPhone. Testing the Android APK requires an Android device or a cloud emulator.
- No production deployment, production D1 migration or release signing has been performed.

## Change log

- 2026-11-03: Android CI #36 and #37 passed; notification worker hardened against disabled app/channel notifications and subscription changes during polling.
- 2026-11-03: Website personal-library sync endpoint, version/tombstone D1 state, legacy write-through and origin/timestamp validation passed website CI #2568-#2570.
- 2026-11-03: Android local-first sync implemented for favorites, watch-later, subscriptions and last-opened episode progress/history. Room v5 migration, per-item conflict ordering, subscription tombstones, WorkManager scheduling and auth-triggered sync passed Android CI #41.
