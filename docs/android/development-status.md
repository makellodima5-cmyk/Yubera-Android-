# YUBERA Android development status

**Last updated:** 2026-11-03  
**Repository:** `makellodima5-cmyk/Yubera-Android-`  
**Branch:** `main`

## Implemented

- Native Kotlin + Jetpack Compose foundation, MVVM/repository layers, Hilt, Room, Retrofit, DataStore and Navigation Compose.
- Original YUBERA drawable assets and launcher resources are committed.
- R2-backed mobile catalogue API and Android revision-aware atomic Room snapshot cache.
- Catalogue search, genre filters, poster cards, offline fallback and retry UI.
- Anime detail screen with portrait poster, metadata and local favorites/watch-later/subscription actions.
- Kodik player screen using the existing YUBERA server route. Provider credentials remain server-side; Android validates trusted HTTPS hosts and confines WebView to the player.
- Home and My screens backed by local catalogue, favorites, watch-later, progress/history and personal subscriptions.
- Shikimori news forum feed with Room cache, source attribution and keyword-based category filters. It is explicitly a single-source feed, not complete industry coverage.
- Profile theme settings (system/light/dark) and local notification preferences.
- Native Telegram OIDC backend handoff has been added to the website repository: browser-based Telegram callback, one-time app exchange code, PKCE verification, revocable bearer session and bearer support on account endpoints. Website CI still needs to pass before treating this integration as validated.
- Android native auth code is prepared: Android Keystore AES-GCM token storage, PKCE state/verifier, fixed deep link callback, bearer session validation and logout. Android CI must pass before considering it build-verified.
- Registration is intentionally disabled in the Android UI until final legal documents are approved and published.

## Validation history

- Android CI #27 passed for the catalogue implementation.
- Android CI #28 and #29 failed on missing explicit SavedStateHandle argument types; those call sites were corrected.
- Android CI #30 failed because the profile screen referenced BuildConfig while BuildConfig generation was disabled; the version label now reads package metadata.
- Android CI #31 failed at Hilt compilation because the `PersonalLibraryRepository` binding was missing. A `LibraryBindings` module is included in the next change. Do not mark the Home/My/detail milestone validated until the next CI run passes.
- Website native-auth CI currently requires a passing run for the PKCE base64url exchange fix.

## Still incomplete or awaiting external verification

- Remote account library/history/subscription synchronization is not yet implemented on Android. Backend bearer support exists, but no conflict-safe Android sync workflow has been validated.
- Cloudflare production deployment of `GET /api/mobile/catalog` and the new native-auth endpoints is not confirmed by GitHub CI alone.
- Notifications remain preference-only. WorkManager polling, deduplication, channels and Android runtime permission are not active.
- Shikimori news API response shape and category coverage require runtime verification against the live service.
- Real-device QA is pending for navigation, WebView player behavior, deep-link handoff, theme rendering and Room migrations.
- Release signing and distribution are not configured. The debug APK is not a release package.
- The user has an iPhone; installing/testing the Android APK requires an Android device or cloud emulator.

## Milestones

| Milestone | Status |
|---|---|
| 0. Foundation and CI | Core foundation exists; latest CI failure is being corrected |
| 1. Assets | Mapped and committed |
| 2. Architecture | Core layers exist; sync remains |
| 3. Startup and navigation | Implemented; session-aware startup now uses native bearer validation in pending CI |
| 4. Native authentication | Backend and Android code prepared; CI/deployment/device verification pending |
| 5. Catalogue | Implemented; Cloudflare deployment/device verification pending |
| 6. Home | Implemented; Hilt binding fix awaiting CI |
| 7. Anime detail and player | Implemented; build/device/provider QA pending |
| 8. Personal library and sync | Local actions implemented; remote sync pending |
| 9. News | Implemented against Shikimori source; live API verification pending |
| 10. Profile | Theme/preferences/logout implemented in pending change |
| 11. Notifications | Preferences only; delivery pending |
| 12. QA and release | Pending |

## Verification rules

- Never claim CI success without checking the exact workflow conclusion.
- Never claim production API availability without checking Cloudflare deployment.
- Never claim playback or Telegram login is verified until tested on Android.
- Never claim remote account sync is complete until conflict and retry behavior are tested.
