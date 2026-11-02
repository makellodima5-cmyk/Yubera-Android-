# YUBERA Android — Design Specification

**Status:** Approved by user on 2026-11-03  
**Product:** YUBERA Android  
**Repository:** `makellodima5-cmyk/Yubera-Android-`  
**Default branch:** `main`

## 1. Intent

Build a native Android application for YUBERA that follows the supplied original screen designs and uses the existing YUBERA website/backend as its service platform. The Android app and website remain separate clients of shared backend services. The app must not be a WebView copy of the website; native UI is implemented with Kotlin and Jetpack Compose.

## 2. Goals

- Reproduce the supplied YUBERA visual direction and screen layouts using original assets.
- Provide welcome, sign-in and registration flows, then a five-item bottom navigation: Home, Catalog, My, News, Profile.
- Make the core screens functional rather than static mockups.
- Support Russian in the initial release.
- Support Android 8.0 (API 26) and newer.
- Keep personal data usable offline after a successful sign-in, with local persistence and later synchronization.
- Reuse the existing backend where possible and avoid breaking the production website.

## 3. Non-goals for the initial release

- Google Play publication; initial distribution is by APK.
- Guest access.
- Offline video downloads.
- External service connections in the profile.
- Video quality, autoplay, next-episode autoplay, or startup-behavior preference controls.
- A public social network or user-to-user messaging system.

## 4. Product structure

### Startup and authentication
- On each genuine app process launch, show the branded startup screen while real startup work is in progress.
- Do not add an artificial minimum display duration or fake percentage progress.
- Do not show the branded startup screen for navigation between sections, returning from another app while the process remains alive, or Activity recreation such as configuration changes.
- Use the supplied portrait background artwork; render the logo and progress indication as app UI elements if they are not already part of the source asset.
- Startup work includes app initialization, reading persisted authentication state, opening local storage, and preparing the first destination.
- After startup, route an authenticated user to Home and an unauthenticated user to sign-in.
- Welcome, sign-in and registration screens follow the supplied references.
- Existing website authentication currently uses Telegram OIDC and a protected session cookie. The native flow should use Authorization Code + PKCE if compatible with the current identity provider and backend. This compatibility must be verified before implementation; do not embed server secrets in the APK. Store native credentials securely using Android Keystore-backed storage.

### Main navigation
Five bottom-navigation destinations:
1. Home
2. Catalog
3. My
4. News
5. Profile

### Home
- Continue watching.
- Popular and recommended anime.
- New and seasonal releases.
- Curated collections.
- Data must eventually come from the YUBERA catalog/API, not hardcoded demo content.

### Catalog
- Full anime catalog.
- Search, genre/year/status filters and sorting.
- Catalog source is the existing R2-first publication flow with Workers KV caching. Android must not query D1 directly.
- The app stores a catalog snapshot locally in Room and checks a revision/manifest endpoint before downloading a replacement snapshot. Reuse an existing public route if one exists; add or adapt an endpoint only after checking current routes.

### My
- Favorites.
- Want-to-watch list.
- Viewing history and episode progress.
- Personal subscriptions to anime updates.
- Favorites and notification subscriptions are separate concepts.
- Personal data is stored locally first in Room and synchronized with the account through the YUBERA backend. Changes made on the website and app should converge through the shared account data model.

### News
- Anime-industry news feed with categories: All, Announcements, New Seasons, Industry, Events, Interviews.
- Featured story, chronological feed, search, article detail, related anime, source/original links where available, bookmarks, and sharing.
- News are presented under YUBERA branding. Required attribution and original-source links must not be removed where legally or contractually required.
- Shikimori is not named in user-facing UI. Whether Shikimori's API provides a sufficiently broad and suitable news feed must be validated; if it does not, use additional permitted sources rather than representing it as comprehensive.

### Profile and settings
- User avatar chosen from a curated set of YUBERA-provided avatars; no arbitrary image upload in the initial release.
- Editable username and profile description; show registration date.
- App appearance: light, dark, and system theme.
- Language: Russian in the initial release; architecture should not prevent adding English later.
- Compact notification settings: master enable/disable, new-episode notifications, and tracked-anime information updates, subject to Android notification permission.
- Account sign-out.
- App version, user agreement, and privacy policy links.
- Do not duplicate catalog, personal-list, or news functions in Profile.

### Video playback
- Kodik is the first video provider.
- Existing YUBERA `/api.kodik?animeId=...` route returns sources, voiceovers and episodes and validates domains.
- If the provider's player permits it, embed the Kodik player in a WebView limited to the native playback screen. Verify provider behavior and terms before implementation.
- No offline video downloads.

### Notifications
- Track only the user's explicit personal anime subscriptions, not the entire catalog and not all favorites.
- Query Shikimori directly from Android where API access permits; do not proxy these requests through YUBERA D1/Workers unless a later technical/legal finding requires it.
- Use WorkManager with a target cadence around six hours; Android battery management means exact timing is not guaranteed.
- Deduplicate notifications and stop tracking removed subscriptions.
- Persist subscriptions locally and synchronize them with the user's YUBERA account so they can be restored after reinstall/sign-in.

## 5. Technical architecture

- Language/UI: Kotlin + Jetpack Compose.
- Architecture: MVVM, repository pattern, Coroutines and Flow.
- Dependency injection: Hilt.
- Networking: Retrofit + OkHttp + Kotlin Serialization.
- Local database: Room.
- Preferences: DataStore.
- Navigation: Navigation Compose.
- Image loading: Coil.
- Background work: WorkManager.
- Secure credential storage: Android Keystore-backed solution.
- Minimum SDK: 26.
- Target SDK: latest stable SDK selected at project creation and compatible with current Android tooling.
- Initial app language: Russian.
- All backend communication goes through documented YUBERA API routes; Android never connects directly to D1.
- Keep existing production identifiers and website internals unchanged unless a concrete compatibility requirement demands a coordinated migration.

## 6. Asset handling

The supplied archive contains 14 PNG files, including one byte-identical duplicate (13 unique images). It includes portrait screen/reference artwork, a square app icon, wide banner artwork, a logo asset and portrait background artwork for startup/authentication. Preserve originals without destructive editing. During asset integration, assign descriptive filenames, place resources in Android-supported resource directories, and verify transparency, aspect ratio and density behavior. Do not use complete screen screenshots as production UI backgrounds; use the original component artwork and implement text, controls and layouts natively.

## 7. Approaches considered

- **A. Build all screens as a visual shell first, then connect behavior.** Faster initial visual coverage, but risks reworking screens when real data, loading and authentication states are introduced.
- **B. Build in functional vertical slices.** Establish the project foundation, then implement authentication and navigation, then each destination with its real data contracts, followed by playback, synchronization and notifications. This is the selected approach because each stage leaves a testable working increment and reduces late integration risk.

## 8. Delivery sequence

1. Android project foundation, theme, resources, startup flow and navigation shell.
2. Authentication and account session handling.
3. Home and catalog, including catalog snapshot contract.
4. My, local persistence and user-data synchronization.
5. News feed and article details.
6. Profile and settings.
7. Kodik playback integration.
8. Personal anime subscriptions and notifications.
9. End-to-end verification, APK build and installation testing on a physical Android device or emulator.

Each subsystem receives its own focused implementation plan after this design is approved. The repository is currently empty apart from README.md, so the Android project will be scaffolded from scratch.

## 9. Open technical validations

- Verify native Telegram OIDC / PKCE compatibility with the current YUBERA authentication backend.
- Inspect existing public catalog routes before designing a new manifest/snapshot endpoint.
- Verify Kodik embedding constraints and permitted WebView integration.
- Verify the available Shikimori news/topic endpoints and coverage before promising a complete industry news feed.
- Confirm legal/attribution requirements for every external news source and image.

## 10. Acceptance criteria

- App installs and launches on Android 8.0 (API 26) or newer.
- A genuine process launch shows the branded startup screen only while real initialization is running; normal navigation and returning to a still-running app do not show it.
- Authenticated and unauthenticated startup routing is correct.
- Five bottom-navigation destinations are present and independently navigable.
- UI uses native Compose components and supplied assets rather than screenshot-based fake screens.
- Local user data remains available offline after successful sign-in.
- Android does not access D1 directly and does not contain backend secrets.
- Core screens and integrations are verified with automated tests where practical and manual device/emulator checks before claiming release readiness.
