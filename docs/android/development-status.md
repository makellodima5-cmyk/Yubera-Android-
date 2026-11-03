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
- Website backend now contains native Telegram OIDC start/callback, one-time PKCE exchange, revocable bearer sessions and bearer support for personal account endpoints. Website CI passed on the native-auth change.
- Android native auth uses Android Keystore AES-GCM storage, PKCE state/verifier, a fixed deep-link callback, bearer session validation and logout. Registration remains disabled until final legal documents are approved and published.

## Validation history

- Android CI #27 passed for the catalogue implementation.
- Android CI #28 and #29 exposed SavedStateHandle type-inference errors; those call sites were corrected.
- Android CI #30 exposed a BuildConfig assumption; the profile version label now reads package metadata.
- Android CI #31 exposed the missing `PersonalLibraryRepository` Hilt binding; `LibraryBindings` is now present.
- Android CI #32 compiled the application and packaged the debug APK successfully, but one news-category unit test failed because Russian word inflections were not covered by the keyword matcher. The matcher is corrected in the next change. The build is not considered green until the next full CI run passes.
- Website CI for native Telegram authentication passed automated tests, TypeScript lint, Worker build and deployment-artifact validation. GitHub CI is validation-only; Cloudflare Workers Builds owns production deployment.

## Still incomplete or awaiting external verification

- Android remote account library/history/subscription synchronization is not yet implemented. Backend bearer support exists, but no conflict-safe Android sync workflow has been validated.
- Cloudflare production deployment of the mobile catalogue and native-auth endpoints is not confirmed by GitHub CI alone.
- Notifications remain preference-only. WorkManager polling, deduplication, channels and Android runtime permission are not active.
- Shikimori news API response shape and category coverage require runtime verification against the live service.
- Real-device QA is pending for navigation, WebView player behavior, deep-link handoff, theme rendering and Room migrations.
- Release signing and distribution are not configured. The debug APK is not a release package.
- The user has an iPhone; installing/testing the Android APK requires an Android device or cloud emulator.

## Milestones

| Milestone | Status |
|---|---|
| 0. Foundation and CI | Core foundation exists; latest run has one unit-test failure |
| 1. Assets | Mapped and committed |
| 2. Architecture | Core layers exist; sync remains |
| 3. Startup and navigation | Implemented; session-aware startup uses bearer validation |
| 4. Native authentication | Backend and Android code implemented; final CI/deployment/device verification pending |
| 5. Catalogue | Implemented; Cloudflare deployment/device verification pending |
| 6. Home | Implemented; Hilt binding included, full CI pending |
| 7. Anime detail and player | Implemented; device/provider QA pending |
| 8. Personal library and sync | Local actions implemented; remote sync pending |
| 9. News | Implemented against Shikimori source; live API verification pending |
| 10. Profile | Theme/preferences/logout implemented |
| 11. Notifications | Preferences only; delivery pending |
| 12. QA and release | Pending |

## Verification rules

- Never claim CI success without checking the exact workflow conclusion.
- Never claim production API availability without checking Cloudflare deployment.
- Never claim playback or Telegram login is verified until tested on Android.
- Never claim remote account sync is complete until conflict and retry behavior are tested.
