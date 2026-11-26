# Stage 1 — Android Technical Foundation

## Scope

This stage covers technical foundation only for the Yubera Android APK. It intentionally does not redesign screens or navigation, and does not include the video player, player testing, push-notification delivery, background episode detection, or APK updater. Those remain in later stages as agreed.

Repository changes are isolated to the `stage1-technical-foundation` branch. The Website/backend repository was not modified.

## Existing foundation retained

- Kotlin + Jetpack Compose, application ID `com.yubera.app`, minimum Android API 26.
- Hilt, Retrofit, OkHttp, Kotlin Serialization.
- Room and DataStore for local persistence.
- WorkManager for existing scheduled tasks.
- Native Telegram OIDC callback: `yubera://auth/callback`.
- Native code exchange: `POST api/telegram/mobile-auth`.
- Session inspection: `GET api/telegram/init-data`.
- Catalog API: `GET api/mobile/catalog`, with paging and revision metadata.
- Local catalog stored in Room, exposed through repository flows.
- Existing account-scoped library records and server synchronization.
- Startup routing based on session availability.

## Stage 1 changes

### Authentication and account safety

- Session restoration now verifies that token, stored account ID, and active account remain unchanged while the server request is in flight.
- Invalid, expired, or identity-mismatched sessions fail closed.
- Offline restoration is allowed only for a previously account-bound session; unassigned legacy account data is not exposed as an offline-authenticated session.
- Profile fetch checks both stored and active account identity after the network request, preventing a delayed response from another account from being displayed.
- Regression tests cover login URL/PKCE, registration consent, callback state, session restore, legacy account claiming, account mismatch, missing identity/token, offline restoration, and an in-flight account switch.

### API/network validation

- Retrofit base URL must be a parseable HTTPS URL.
- URL must have a trailing slash and must not contain credentials, query parameters, or a fragment.
- Tests cover HTTPS, HTTP rejection, path slash, malformed URL, credentials, query, and fragment.

### Catalog and local cache

- The client validates catalog schema, revision, page number, page size, total pages, expected page lengths, non-empty IDs, and duplicate IDs.
- A catalog snapshot is replaced only after every page has been fetched and the page revision remains consistent.
- Revision changes trigger a bounded retry. Invalid or incomplete responses fail before replacing the last good Room snapshot.
- Tests cover successful multi-page load, revision changes, cache preservation on failures, malformed page metadata, duplicates, and empty catalog.

### Personal library and cross-device sync

- Local mutations for favorites, watch-later, progress, and subscriptions are serialized to avoid read-modify-write races and timestamp collisions.
- Account-aware library flows wait for outstanding local mutation work before switching the observed account.
- Library sync reads a consistent local snapshot, sends changes in bounded batches, preserves acknowledgements from all batches, and compares server values with the latest account-scoped local versions before applying them.
- Sync requests verify the currently active token/account before and after network calls.
- A stable per-install sync source identifier is generated and hashed; raw hardware identifiers are not used.

### Application label

- Android launcher label is set to `Yubera`.

## Validation status

- [x] Separate working branch created.
- [x] Android source audited for auth, startup, catalog, local Room storage, account scoping, and sync paths.
- [x] Regression tests added for confirmed cases.
- [x] Android label updated.
- [x] Draft pull request opened for CI validation.
- [ ] GitHub Actions build/lint/unit-test run result must be confirmed before calling this stage complete.

## Explicit exclusions

- No screen visual design or navigation-bar decisions.
- No player implementation or playback testing.
- No Push/Firebase integration.
- No change to server-side notifications or episode detection.
- No APK version updater.
- No Website/backend code or migration changes.
