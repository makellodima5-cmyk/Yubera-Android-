# YUBERA Android development status

**Last updated:** 2026-11-03  
**Repository:** `makellodima5-cmyk/Yubera-Android-`  
**Branch:** `main`

## Implemented in the repository

- Native Kotlin + Jetpack Compose app scaffold, MVVM/repository layers, Hilt, Room, Retrofit, DataStore, Navigation Compose and WorkManager dependency foundation.
- Original YUBERA drawable resources are committed under `app/src/main/res/drawable-nodpi`; app icon and adaptive launcher resources are present.
- Revisioned R2-backed mobile catalogue API exists in the website repository; Android fetches pages and commits complete snapshots atomically to Room.
- Catalog screen includes search, genre filters, poster cards, offline snapshot display and retry states.
- Anime detail screen uses the original portrait poster ratio, shows catalogue metadata and supports local favorites, watch-later and personal title subscriptions.
- Dedicated Kodik player screen uses the existing YUBERA server route. The Kodik API token remains server-side; Android validates HTTPS links against the same trusted host allowlist and confines WebView to the player.
- Home screen reads catalogue items and local viewing progress; My screen reads favorites, watch-later, history and personal subscriptions from Room.
- News feed reads Shikimori's public news forum, stores a local Room cache, attributes each item and uses heuristic category filters. It is explicitly not represented as complete industry coverage.
- Profile screen stores theme and notification preferences locally; theme palette switches between system/light/dark.
- Android CI run [#27](https://github.com/makellodima5-cmyk/Yubera-Android-/actions/runs/41668049373) passed for the catalogue commit.
- Android CI run [#28](https://github.com/makellodima5-cmyk/Yubera-Android-/actions/runs/41669796846) failed during Kotlin compilation because Navigation's SavedStateHandle argument type was not explicit.
- Android CI run [#29](https://github.com/makellodima5-cmyk/Yubera-Android-/actions/runs/41671118175) failed on the same SavedStateHandle inference issue in the detail and player ViewModels. The code is corrected in the next commit; its CI result must be checked before calling this milestone verified.

## Still incomplete or awaiting external verification

- Native Telegram OIDC handoff and bearer app sessions are not implemented yet. The website currently has a browser-cookie OIDC flow; Android must not copy browser cookies or embed server secrets.
- Account synchronization is not connected. Local library data is preserved, but there is no verified conflict-safe upload/download path for Android sessions yet.
- Cloudflare production deployment of `GET /api/mobile/catalog` is not confirmed by GitHub CI alone.
- Notifications are preference-only at this point. WorkManager polling, notification channels, deduplication and Android permission flow are not yet active.
- Shikimori news categories are keyword heuristics, not editorial metadata. The API contract and response shape still require runtime verification against the live service.
- The player, navigation, themes and layout need real-device testing. WebView provider behavior cannot be validated by a JVM unit test.
- The debug APK is not a release package. Release signing and distribution require a secure signing-key handoff.
- The user has an iPhone; Android APK installation/testing requires an Android device or cloud emulator.

## Milestone overview

| Milestone | Status |
|---|---|
| 0. Foundation and CI | Core foundation exists; latest validation pending |
| 1. Asset inventory | Original assets mapped and drawable resources committed |
| 2. Architecture | Core layers exist; sync/auth integration remains |
| 3. Startup and navigation | Native navigation exists; session-aware startup still uses the unconfigured auth provider |
| 4. Native authentication | Pending secure native OIDC exchange |
| 5. Catalogue | Implemented; live Cloudflare deployment/device verification pending |
| 6. Home | Implemented against local catalogue/progress flows; device QA pending |
| 7. Anime detail and player | Implemented; device/provider QA pending |
| 8. Personal library and sync | Local Room lists/actions implemented; remote sync pending |
| 9. News | Shikimori source/cache/filter implemented; live API verification pending |
| 10. Profile | Local theme and notification preferences implemented; account UI pending |
| 11. Notifications | Preferences only; worker and notification delivery pending |
| 12. QA and release | Pending |

## Verification rules

- Do not claim CI success without checking the exact workflow conclusion.
- Do not claim production API availability without checking Cloudflare deployment.
- Do not claim playback is verified until tested on Android.
- Do not mark native login or account sync complete until the real PKCE exchange and token lifecycle are tested.
