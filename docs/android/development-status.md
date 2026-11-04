# YUBERA Android development status

**Last updated:** 2026-11-04  
**Repository:** `makellodima5-cmyk/Yubera-Android-`  
**Branch:** `main`  
**Current source commit:** `7f7beffd54f72be6a102d676a7ff0f54c086a786`  
**Latest verified CI:** Android CI #59, passed on `7f7beffd54f72be6a102d676a7ff0f54c086a786`.  
**Debug APK artifact:** `yubera-debug-apk`, ID `13330829565`, 27,881,768 bytes; expires 2026-11-18. SHA-256 of the uploaded artifact ZIP: `f345fde440558431862dff5a0da2d77aa2568d3f7907b4bf6f7baa6577d400bc`.

## Implemented

- Native Kotlin + Jetpack Compose / Material 3 foundation, MVVM/repository layers, Hilt, Room, Retrofit, DataStore, Navigation Compose, Coil and WorkManager.
- Original YUBERA logo, splash artwork, banner, authentication background and launcher icon resources are integrated. Full-screen reference screenshots are not used as UI.
- Real process-start splash flow and five top-level destinations: Home, Catalog, My, News and Profile. Runtime cold/warm/recreation behavior still needs physical-device verification.
- R2-first catalog integration with revision-aware Room snapshot cache, search, genre/year/status filters, sorting by API order/rating/year/title, offline fallback and retry UI.
- Home with catalogue picks, continue-watching cards (stored episode and available progress information) and a high-rating collection using only supplied ratings.
- Anime detail page using vertical poster proportions, metadata, description and personal-library actions. Kodik playback is isolated in a dedicated WebView route with trusted HTTPS host checks. The player still lacks a verified playback-position callback, so reliable resume percentage/position is not captured.
- Local-first favorites, watch-later, last-opened episode history and subscriptions. Personal data is partitioned by account ID. Sync uses per-item versions/timestamps, tombstones, deterministic conflict ordering, retry and WorkManager scheduling.
- Android notifications check only personal subscriptions, use a six-hour target (best-effort, as Android controls scheduling), deduplicate episodes and respect application, subscription and system permission settings.
- Telegram native OIDC Authorization Code + PKCE, one-time code exchange, revocable bearer sessions and Android Keystore-backed token storage. No browser cookie or backend secret is placed in the APK.
- Profile displays the authenticated Telegram first name, last name and username from the session API after checking that the returned account ID and bearer token still match the active account. It also has persisted system/light/dark theme, notification preferences, Android 13+ notification permission request, logout and app version display.
- News integration with Shikimori cache, category classification, local search (title, preview, linked anime and author), detail screen, external source link, related-anime label, bookmark action and Android sharing. Bookmarks are local-only and survive feed refreshes; a dedicated bookmarks filter is available.

## Verified CI

- Android CI #48 passed on `76397dd9ddfbcc24448da6e951906790e779b17c`; debug resources, APK build, unit tests and artifact upload succeeded.
- Android CI #50 passed on `21341f4240b4601417a1f8bb567347ea59fe9499`; account-scoped remote progress deletion/tombstones passed.
- Android CI #51 passed on `9183f9aa74c34ad71f64334a2aa2e6c61b0b6504`.
- Android CI #52 passed on `ebe3e260e568ffd3ecd6b5a714af2df56c90de1c`; continue-watching progress card compiled and tests passed.
- Android CI #53 passed on `df2ead6cdcafda351308387af5665bcb7cab8dac`; catalog sorting, filters and home rating collection compiled and tests passed.
- Android CI #54 passed on `e29a6113c9b545cbe73fca7602650ebd57f8445c`; news search compiled and tests passed.
- Android CI #56 failed at Kotlin compilation because `NewsViewModel` tried to combine six flows using a typed `combine` overload that does not support six typed parameters. This was corrected by composing two supported combines.
- **Android CI #57 passed** on `442b358c69fe26b651e7ea29535cb848d42b89a2`. Debug resource processing, debug APK assembly, unit tests and artifact upload all succeeded. The same commit adds the bookmarks filter and preserves bookmarked article records outside the current feed page.
- **Android CI #58 passed** on `458b60aebaeb92d8d612454679a8f8b7649017f6` (documentation refresh).
- **Android CI #59 passed** on `7f7beffd54f72be6a102d676a7ff0f54c086a786`. Profile now loads and displays Telegram first/last name and username only when the authenticated session response matches the currently stored account. New unit tests cover the matching and mismatching account cases. Debug resource processing, APK assembly, unit tests and artifact upload all succeeded; artifact ID `13330829565`.
- Website CI #2568, #2569, #2570 and #2572 passed for the earlier library sync and episode-progress projection changes.

CI confirms compilation, unit tests and artifact packaging. It does not confirm production deployment, live backend compatibility or app runtime behavior on a real device.

## Known integration gaps and constraints

1. **News coverage and full text:** the current source is one Shikimori news endpoint, one page of up to 30 topics. The API data currently consumed by the app is a preview, not a guaranteed complete article body. The detail screen makes this limitation explicit and links to the original publication. The feed must not be presented as exhaustive industry coverage. Additional sources require a separate coverage, licensing and API review.
2. **News bookmarks:** saved locally in Room on this Android installation. They are not synchronized with the website or another device.
3. **Kodik progress:** no verified playback callback is available in the current integration. The app can store the last opened episode and any existing progress fields but cannot claim reliable in-player resume tracking yet.
4. **Profile completeness:** theme, notification settings, logout and version are implemented. The current app displays Telegram first name, last name and username. The website database has a `created_at` field, but the session endpoint does not return it. There is no profile description field in the current account schema. The uploaded asset archive does not contain a distinct curated avatar set, only a composite profile-screen reference. Do not invent registration dates, profile biographies or avatar choices.
5. **Legal links and registration:** the website user-agreement file is explicitly a draft with unresolved owner/contact/domain placeholders, and the privacy-policy publication URL is not yet confirmed. Registration must remain disabled until legal documents are approved and published. Do not add fake or draft legal URLs to the app.
6. **Room migration:** Android database schema is currently version 7; local Room migration 6→7 adds news bookmarks. This is separate from Cloudflare D1 migrations.
7. **Production rollout:** the user reports website migrations 0001 and 0002 already applied, but this has not been independently checked against production. The active website config points to `migrations-clean`, which contains two different `0002_*.sql` files, so migration history must be checked before issuing any commands. Migration `0003_view_history_episode_progress.sql` is still pending according to project notes. No production migration or deployment was performed.
8. **Account-scoped local data:** old rows preserved in the `legacy-unassigned` partition are not automatically assigned to a new account. They can be claimed only after validating the corresponding existing session. Offline startup after migration leaves them local until session verification succeeds.
9. **Real account QA:** live checks remain required for Telegram deep-link login, expired sessions, account switching, sync add/remove/conflicts/retry, offline behavior and cross-device consistency.
10. **Device QA:** an Android device is available for testing. Runtime checks are still outstanding for API 26/current Android, cold and warm startup, Activity recreation, navigation restoration, catalog, anime details, Kodik, theme, Room migrations, login, and notification delivery.
11. **Release:** release signing, final accessibility/visual review, acceptance testing, update path and release APK are not configured.

## Milestones

| Milestone | Status |
|---|---|
| 0. Foundation and CI | Implemented; repeated CI green |
| 1. Assets | Core production assets integrated; launcher and device appearance still need visual validation |
| 2. Architecture | Implemented; account-scoped local-first storage and sync path present |
| 3. Startup and navigation | Implemented in source; device/lifecycle verification pending |
| 4. Native authentication | Implemented in source; real Telegram login/session runtime QA pending |
| 5. Catalogue | Search, filters, sorting, cache and offline fallback implemented; live/device QA pending |
| 6. Home | Continue-watching and rating-based collection implemented; visual/device QA pending |
| 7. Anime detail and player | Implemented; Kodik/device QA and reliable playback progress pending |
| 8. My library and sync | Account-scoped storage/sync implemented; production migration, real-account conflict/retry and cross-device checks pending |
| 9. News | Feed, categories, search, detail, source link, related anime, bookmarks and sharing implemented; single-source/preview limitation remains, additional coverage review pending |
| 10. Profile | Theme, notification preferences, logout and version implemented; user profile fields/avatar/legal links remain incomplete |
| 11. Notifications | Worker, subscription filtering, dedupe and permission guards implemented; real-device/live API QA pending |
| 12. QA and release | Pending |

## News source coverage review

A separate review is recorded in `docs/android/news-sources-review.md`. It identifies Anime News Network as the first source worth approaching for permission to display headlines in a native Android app. ANN documents RSS/Atom feeds and a website newsfeed program, but its published instructions do not clearly grant reuse inside an Android APK. Crunchyroll also exposes RSS, but its terms restrict substantial-content copying/integration without explicit written permission. No new publisher feed has been integrated; Shikimori remains the only active source until rights and the technical contract are confirmed.

## Change log

- 2026-11-04: Android CI #52 and #53 passed for continue-watching, catalog sorting/filters and rating-ranked home collection.
- 2026-11-04: Android CI #54 passed for news search.
- 2026-11-04: Android CI #56 exposed a six-flow Kotlin `combine` compile error in the news ViewModel.
- 2026-11-04: Fixed the news flow composition; added a bookmarks-only filter, combined search/category behavior in bookmarks, and transactional feed updates that retain bookmarked article rows outside the latest feed. Android CI #57 passed and uploaded `yubera-debug-apk`.
- 2026-11-04: Added live Telegram first/last name and username to the Android profile with active-session/account matching, retry UI and unit tests; Android CI #59 passed. Audited profile requirements: created_at exists in D1 but is not returned by the session API, no biography column exists, no separate curated-avatar set was supplied, and legal documents remain drafts.
- 2026-11-04: Reviewed additional anime-news sources and reuse restrictions. ANN is the first candidate to approach for native-app permission; Crunchyroll is not integrated without explicit written approval. See `docs/android/news-sources-review.md`.
