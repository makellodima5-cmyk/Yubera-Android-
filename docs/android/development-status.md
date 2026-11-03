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
- Notification hardening after CI #36 is committed separately and awaits its own CI run.

## Still incomplete or awaiting external verification

- Android remote account library/history/subscription synchronization is not implemented. Backend bearer support exists, but no conflict-safe Android sync workflow has been validated.
- The current saved-list backend supports merge-only additions for initial migration plus individual add/remove operations. It has no per-item revision/tombstone contract, so Android must not implement naive full-list replacement or union-only sync; removals across devices need a versioned conflict-safe contract.
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
| 0. Foundation and CI | Core foundation exists; CI #36 green and debug APK artifact uploaded |
| 1. Assets | Mapped and committed |
| 2. Architecture | Core layers exist; remote sync remains |
| 3. Startup and navigation | Implemented; session-aware startup uses bearer validation |
| 4. Native authentication | Android/backend code implemented; production deployment and device verification pending |
| 5. Catalogue | Implemented; Cloudflare deployment/device verification pending |
| 6. Home | Implemented; full CI #33 passed, subsequent CI #36 passed |
| 7. Anime detail and player | Implemented; device/provider QA pending |
| 8. Personal library and sync | Local actions implemented; remote sync pending |
| 9. News | Implemented against Shikimori source; live API verification pending |
| 10. Profile | Theme/preferences/logout implemented; notification permission request wired |
| 11. Notifications | Client-side WorkManager polling, dedupe and system/channel availability guards implemented; CI #36 passed; live API/device verification pending |
| 12. QA and release | Pending |

## Verification rules

- Never claim CI success without checking the exact workflow conclusion.
- Never claim production API availability without checking Cloudflare deployment.
- Never claim playback or Telegram login is verified until tested on Android.
- Never claim remote account sync is complete until conflict and retry behavior are tested.
- Never claim notification delivery is verified until the GraphQL query, Room migration, WorkManager execution and Android notification permission are tested on a device.

## Change log

- 2026-11-03: Recorded verified Android CI #36 success, debug APK artifact metadata and current outstanding deployment/device/sync/notification work.
- 2026-11-03: Added local subscription-only episode polling with WorkManager, Shikimori GraphQL batches, per-episode deduplication, watermark protection, Android notification permission and Room v4 migration. Awaiting runtime verification.
- 2026-11-03: Hardened notification delivery against app-wide/channel notification disablement and subscription removal during a network poll; preserves retry semantics when system notification availability changes mid-run.
