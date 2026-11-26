# Stage 1 — Android Technical Foundation

Scope: Yubera Android only. UI styling, navigation structure, player implementation/testing, notifications, and APK updater are out of scope for this stage.

## Existing foundation verified

- Kotlin + Jetpack Compose; application ID `com.yubera.app`; minimum API 26.
- Hilt, Retrofit + OkHttp + Kotlin Serialization.
- Room, DataStore, and WorkManager dependencies are present.
- Native Telegram OIDC callback uses `yubera://auth/callback`.
- Native session exchange: `POST api/telegram/mobile-auth`.
- Session restore: `GET api/telegram/init-data`.
- Catalog API: `GET api/mobile/catalog` with pagination.
- Catalog repository exposes observe, observe-item, and refresh.
- Startup routes based on a session-state provider.
- Application startup schedules library synchronization and episode-notification checks.

## Stage 1 goals

1. Review and harden authentication/session restoration and logout/account-switch behavior.
2. Review API/network configuration and error handling without changing Website.
3. Review catalog cache-first startup, pagination, revision handling, and offline behavior.
4. Review local account isolation and library synchronization.
5. Keep screens as technical placeholders; do not design navigation or appearance.
6. Add/improve tests for confirmed issues where the current setup supports it.

## Audit findings so far

- Auth repository already uses state and a PKCE-style verifier/challenge.
- Callback code/state validation is performed before exchanging the code.
- Session restoration checks token consistency after network calls to avoid stale responses.
- Stored account identity is compared with server identity; mismatches fail closed.
- Existing unit tests cover authorization URL/challenge, registration consent, callback state, successful exchange, legacy-account claiming, identity mismatch, missing identity, missing token, and profile identity checks.
- Startup routing has tests for signed-in and signed-out states.
- API base URL and Retrofit construction are centralized and require HTTPS.
- Current API base URL is configured as a Cloudflare Worker URL. Website changes are not planned for this stage.

## Work status

- [x] Stage 1 branch created: `stage1-technical-foundation`.
- [x] Existing application stack and key API/auth/catalog entry points inspected.
- [x] Existing auth/startup test coverage inspected.
- [ ] Complete source-tree inventory and review all implementations/DAOs/DI bindings.
- [ ] Audit expired/invalid token, offline, logout, and account-switch behavior.
- [ ] Audit catalog refresh/cache behavior and offline-first startup.
- [ ] Audit sync account isolation and race conditions.
- [ ] Add tests for confirmed issues and run build/test validation.
- [ ] Report Website API changes separately; do not modify Website.

## Constraints

- Work only in the Android repository.
- Do not implement screen design or navigation-bar choices.
- Do not implement or test the player in Stage 1.
- Do not implement notifications or APK updating in Stage 1.
- Do not change Website/backend repositories.
- User-facing strings must not expose internal infrastructure terminology.
