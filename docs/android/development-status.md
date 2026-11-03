# YUBERA Android development status

**Last updated:** 2026-11-03  
**Repository:** `makellodima5-cmyk/Yubera-Android-`  
**Branch:** `main`

## Implemented

- Native Kotlin + Jetpack Compose foundation, MVVM/repository layers, Hilt, Room, Retrofit, DataStore and Navigation Compose.
- Original YUBERA drawable assets and launcher resources.
- R2-backed mobile catalogue API and Android revision-aware atomic Room snapshot cache.
- Catalogue search, genre filters, poster cards, offline fallback and retry UI.
- Anime detail screen with portrait poster, metadata and local favorites/watch-later/subscription actions.
- Kodik player screen using the existing YUBERA server route. Provider credentials remain server-side; Android validates trusted HTTPS hosts and confines WebView to the player.
- Home and My screens backed by local catalogue, favorites, watch-later, progress/history and personal subscriptions.
- Shikimori news forum feed with Room cache, source attribution and keyword-based category filters.
- Profile theme settings (system/light/dark), local notification preferences and server-session logout.
- Website backend contains native Telegram OIDC start/callback, one-time PKCE exchange, revocable bearer sessions and bearer support for personal account endpoints. Website CI passed on the native-auth change.
- Android native auth uses Android Keystore AES-GCM storage, PKCE state/verifier, a fixed deep-link callback, bearer session validation and logout. Registration remains disabled until final legal documents are approved and published.
- Personal episode notification implementation: WorkManager periodic task with a six-hour target, connected-network constraint, local Room subscription filtering, direct Shikimori GraphQL polling, per-anime episode watermark, per-episode deduplication/outbox marker, Android notification channel and Android 13+ runtime permission request from Profile.
- Notification polling is client-side only. No new YUBERA Worker/D1 endpoint was added. The worker only sends IDs from local active notification subscriptions; favorites and watch-later are not queried.
- First successful check establishes a baseline without sending old episodes. Later episode-count increases create notifications for newly released episode numbers. A stale lower API count does not move the watermark backwards. A pending notification can be retried if the process stops before delivery is marked.
- Room schema upgraded from v3 to v4 with a non-destructive migration for subscription watermarks and episode notification deduplication records. Re-saving a subscription preserves its existing watermark.
- Notification delivery now checks both app-wide notification availability and the Android O+ channel importance before polling; it re-checks subscription state and notification availability immediately before delivery so a disabled channel does not silently consume the episode watermark.

## Validation history

- Android CI #27 passed for the catalogue implementation.
- Android CI #28 and #29 exposed SavedStateHandle type-inference errors; those call sites were corrected.
- Android CI #30 exposed a BuildConfig assumption; the profile version label now reads package metadata.
- Android CI #31 exposed the missing `PersonalLibraryRepository` Hilt binding; `LibraryBindings` is now present.
- Android CI #32 compiled the application and packaged the debug APK successfully, but one news-category unit test failed because Russian word inflections were not covered by the keyword matcher.
- Android CI #33 passed in full on commit `21d09b67a12383e9e885355391b84e4abccfb02b`: Android SDK verification, debug APK build, unit tests and artifact upload all succeeded.
- CI #33 uploaded the `yubera-debug-apk` artifact (27,737,672 bytes; SHA-256 `2ca260939b24079d52cf7eb49c6160a0ed0db15fea3233977f101da1934ad848`).
- Android CI #36 passed in full on commit `6fe52b8f5c5780f8f96a1e9cbed2f3f641be2d2f`: SDK verification, debug APK build, unit tests and artifact upload all succeeded. Artifact `yubera-debug-apk`, 27,769,348 bytes, SHA-256 `7daf26dcbde0a7f912e011815d4344fb0af1f8812ad68e99a95ad76e98365767`, expires 2026-11-17.
- Website CI for native Telegram authentication passed automated tests, TypeScript lint, Worker build and deployment-artifact validation. GitHub CI is validation-only; Cloudflare Workers Builds owns production deployment.
- Android CI #37 passed in full on commit `c5e33deb45af53c5078e561c5dbd53e023e2aeae`: SDK verification, debug APK build, unit tests and artifact upload all succeeded. Artifact `yubera-debug-apk`, 27,770,377 bytes, SHA-256 `583aaac75d241c1a467abedd0bb73512e2cc1b7f37923acaea3218136ee7f3d3`, expires 2026-11-17.
- Android notification hardening (app-wide notifications enabled, channel importance, and active-subscription recheck) passed CI #37.
- Website repository now contains a versioned, per-item timestamp/tombstone sync endpoint for favorites, watchlist, subscriptions and last-opened episode records, plus legacy-site write-through and D1 migration. Website CI #2568, #2569 and #2570 passed tests, lint, Worker build and migration validation. Production migration/deployment is not performed.
- Android sync client now performs local-first Room merges, deterministic timestamp/source-ID conflict ordering, subscription tombstones, independent favorite/watch-later timestamps, retryable WorkManager sync, immediate scheduling after local changes and a six-hour periodic fallback. Room schema v5 adds the per-list timestamps and subscription tombstone flag. CI #41 is pending.

## Still incomplete or awaiting external verification

- Android remote account synchronization is implemented in source for favorites, watch-later, subscriptions and last-opened episode history. The sync worker uses the versioned backend endpoint, Room-first writes, per-item timestamps, source-ID tie-breaking, deletion tombstones, retry and WorkManager scheduling. Android CI #41 and runtime API verification are pending.
- The Kodik WebView does not expose a verified playback-position callback. The app records the last opened trusted episode as history with position/duration zero; actual watch percentage and resume position are not captured.
- Android has not yet integrated the new per-item sync endpoint. Client-side outbox, conflict handling, retry behavior and Room migration remain to be implemented and tested.
- Cloudflare production deployment of the mobile catalogue and native-auth endpoints is not confirmed by GitHub CI alone.
- Notification worker, Room v3-to-v4 migration and live Shikimori GraphQL response shape passed Android compilation/unit tests in CI #36, but still require live API/runtime/device verification. WorkManager's six-hour interval is best-effort and can be delayed by Android battery and scheduling policies.
- Notification delivery currently covers new episode counts only. The separate Profile preference for tracked-title information changes is not yet implemented.
- Shikimori news API response shape and category coverage require runtime verification against the live service.
- Real-device QA is pending for navigation, WebView player behavior, deep-link handoff, notification permission/delivery, theme rendering and Room migrations.
- Release signing and distribution are not configured. The debug APK is not a release package.
- The user has an iPhone; installing/testing the Android APK requires an Android device or cloud emulator.

## Milestones

| Milestone | Status |
|---|---|
| 0. Foundation and CI | Core foundation exists; CI #37 green and debug APK artifact uploaded |
| 1. Assets | Mapped and committed |
| 2. Architecture | Core layers exist; remote sync remains |
| 3. Startup and navigation | Implemented; session-aware startup uses bearer validation |
| 4. Native authentication | Android/backend code implemented; production deployment and device verification pending |
| 5. Catalogue | Implemented; Cloudflare deployment/device verification pending |
| 6. Home | Implemented; CI #33, #36 and #37 passed |
| 7. Anime detail and player | Implemented; device/provider QA pending |
| 8. Personal library and sync | Local-first versioned sync implemented for saved lists, subscriptions and last-opened episode history; CI #41 and runtime verification pending |
| 9. News | Implemented against Shikimori source; live API verification pending |
| 10. Profile | Theme/preferences/logout implemented; notification permission request wired |
| 11. Notifications | Client-side WorkManager polling, dedupe and system/channel availability guards implemented; CI #37 passed; live API/device verification pending |
| 12. QA and release | Pending; production D1 migration/deployment and Android device/emulator verification required |

## Verification rules

- Never claim CI success without checking the exact workflow conclusion.
- Never claim production API availability without checking Cloudflare deployment.
- Never claim playback or Telegram login is verified until tested on Android.
- Never claim remote account sync is complete until conflict and retry behavior are tested.
- Never claim notification delivery is verified until the GraphQL query, Room migration, WorkManager execution and Android notification permission are tested on a device.

## Change log

- 2026-11-03: Recorded verified Android CI #36 success, debug APK artifact metadata and current outstanding deployment/device/sync/notification work.
- 2026-11-03: Added local subscription-only episode polling with WorkManager, Shikimori GraphQL batches, per-episode deduplication, watermark protection, Android notification permission and Room v4 migration. Awaiting runtime verification.
- 2026-11-03: Hardened notification delivery against app-wide/channel notification disablement and subscription removal during a network poll; CI #37 passed.
- 2026-11-03: Website personal-library sync endpoint, migration, legacy write-through, origin checks, timestamp bounds and idempotent legacy mirroring passed website CI #2568-#2570.
- 2026-11-03: Added Android local-first library sync for favorites, watch-later, subscriptions and last-opened episode history; Room v5 migration, per-item timestamp conflict ordering, subscription tombstones, WorkManager scheduling and sync tests. CI #39 exposed a test assertion issue; CI #40 exposed an incorrect Hilt qualifier import. Both fixes are in the next commit; CI #41 pending.
