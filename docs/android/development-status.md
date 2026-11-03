# YUBERA Android development status

**Last updated:** 2026-11-03  
**Repository:** `makellodima5-cmyk/Yubera-Android-`  
**Branch:** `main`

## Verified

- Kotlin + Jetpack Compose Android application skeleton, navigation scaffold and Hilt/Room/Retrofit/DataStore foundations exist.
- Room stores the local personal-library entities and a revisioned catalogue snapshot.
- Website integration audit is documented in `site-integration-audit.md`.
- Website added the read-only R2-backed `GET /api/mobile/catalog` contract; production deployment is not assumed.
- Catalog screen and repository support local snapshot observation, pagination, revision checks and atomic snapshot replacement.
- Commit `e05ead13047f6e5dd09d90d54b9b13ee7b6a03a9` passed Android CI run [#27](https://github.com/makellodima5-cmyk/Yubera-Android-/actions/runs/41668049373). The debug APK artifact was uploaded by that run.
- Anime detail navigation, portrait-poster detail layout and local favorite/watch-later/subscription actions were added in commit `01e0b965e8a99cc0d0c712cb8a7bcb5ef039eae0`. CI run [#28](https://github.com/makellodima5-cmyk/Yubera-Android-/actions/runs/41669796846) is the validation for that commit; do not mark it green until its conclusion is confirmed.

## In progress / not yet verified

- Home and My screens are being connected to the real local catalogue and Room personal-library flows.
- Dedicated Kodik player route is being integrated using the existing website API route.
- Original image resources have been inventoried and mapped, but the actual original files are not yet confirmed as committed Android resources.
- The first APK is a debug artifact, not a release build and not a signed distribution package.

## Blocked by external requirements

- Native Telegram login is not available yet. Existing website OIDC callback is browser-cookie based and has no native one-time app-session exchange. Android must not copy browser cookies or contain server secrets.
- Account synchronization cannot be enabled until a secure native app-session contract exists on the website backend.
- The catalogue endpoint must be deployed by the website's Cloudflare deployment pipeline before the app can retrieve it from the public production origin. GitHub CI alone does not deploy it.
- News feed source coverage and attribution need a defined contract; a partial Shikimori feed must not be presented as complete industry coverage.
- Release signing and installation/update verification require a secure signing-key handoff and an Android device or cloud emulator. APK cannot be installed on iPhone.

## Milestone overview

| Milestone | Status |
|---|---|
| 0. Foundation and CI | Partially complete; CI green for the catalogue commit |
| 1. Asset inventory | Inventory and mapping complete; resource integration pending |
| 2. Architecture | Core layers exist; remaining feature repositories/tests in progress |
| 3. Startup and navigation | Basic navigation exists; auth-aware startup still pending |
| 4. Native authentication | Blocked on backend native-session exchange |
| 5. Catalogue | Implemented; public deployment and device validation pending |
| 6. Home | In progress |
| 7. Anime detail and player | Detail added; player integration in progress |
| 8. Personal library and sync | Local entities/actions exist; complete UI and remote sync pending |
| 9. News | Pending source/attribution contract |
| 10. Profile | Pending |
| 11. Notifications | Pending |
| 12. QA and release | Pending |

## Verification rules

- Do not claim CI success without checking the exact run conclusion.
- Do not claim production API availability without checking Cloudflare deployment.
- Do not claim player behavior is verified until tested on Android.
- Do not mark native login or account sync complete until a real session exchange and token lifecycle are tested.
