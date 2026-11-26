# Stage 1 — Android Technical Foundation

Scope: Yubera Android only. This stage prepares the app's existing technical foundation. It does not redesign screens or navigation and does not implement/test the player, push notifications, or APK updater.

## Foundation retained and reviewed

- Kotlin + Jetpack Compose; application ID `com.yubera.app`; minimum API 26.
- Hilt dependency injection; Retrofit/OkHttp and Kotlin Serialization.
- Room local database, DataStore preferences, and WorkManager.
- Native Telegram sign-in and registration callback through `yubera://auth/callback`.
- Native session exchange at `POST api/telegram/mobile-auth`; session inspection at `GET api/telegram/init-data`.
- Paginated catalog API at `GET api/mobile/catalog`; catalogue snapshot and revision are stored locally.
- Account-bound local library data and synchronization using existing server endpoints.

## Stage 1 improvements in this branch

- Native session restore now verifies the saved token, saved account identity, and active account as one consistent snapshot. Stale responses after logout/account switching cannot restore an old account.
- Offline startup is allowed only for a session already bound to an account. A legacy unbound session must be confirmed by the server before local account rows are claimed.
- Profile responses are discarded when the saved or active account changes while a request is running.
- Local logout cleanup runs even when the remote logout request fails.
- A pending-OAuth cleanup failure after a successful session exchange does not falsely report the completed login as failed.
- Token and account-id writes/clears use checked synchronous preference commits to avoid publishing a partially persisted session.
- API base URL validation requires a valid HTTPS URL with a slash-terminated path and rejects embedded credentials, query parameters, and fragments.
- Catalog refresh is serialized in-process. Concurrent refresh attempts cannot replace the Room snapshot simultaneously. The first page supplies the revision; when the revision is unchanged, the second refresh stops before requesting the remaining pages.
- Catalog responses are validated for supported schema, page numbering/count, page size, revision, required IDs, duplicate IDs, and page completeness before a snapshot replaces the cache.
- Personal-library writes and sync acknowledgements are account-scoped and serialized/merged against current data to avoid racing local changes with sync results.
- Android launcher label and associated existing branding resource references are set to Yubera; no screen design or navigation layout was introduced.

## Verification

- [x] GitHub Actions Android CI passed for this branch head: debug APK build, release APK build, lint, and unit tests.
- [x] Unit tests passed: 74 tests, 0 failures.
- [x] CI uploaded the debug APK artifact.
- [x] No Website/backend repository was modified.

## Deliberately deferred

- Visual design, screen layouts, app icon design refinements, and navigation structure.
- Player/WebView/Kodik implementation and device testing.
- Push notifications/FCM and server delivery integration.
- APK updater and release distribution automation.
- Physical-device runtime verification. CI validates compilation and automated tests, not behavior on the user's handset.
