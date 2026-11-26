# Stage 1 — Android Technical Foundation

Scope: Yubera Android only. This document tracks Stage 1 technical work; UI styling, navigation structure, player implementation/testing, notifications, and APK updater are out of scope for this stage.

## Existing foundation verified

- Kotlin + Jetpack Compose project.
- Android application ID: `com.yubera.app`.
- Minimum Android API: 26.
- Hilt dependency injection.
- Retrofit + OkHttp + Kotlin Serialization.
- Room and DataStore dependencies are present.
- WorkManager dependency is present.
- Native Telegram OIDC callback is handled through the `yubera://auth/callback` deep link.
- Native session exchange uses `POST api/telegram/mobile-auth`.
- Session restore uses `GET api/telegram/init-data`.
- Catalog API uses `GET api/mobile/catalog` with pagination.
- Catalog domain repository exposes observe, observe-item, and refresh operations.
- Startup resolves sign-in vs home through a startup coordinator and session state provider.
- The app schedules library synchronization and episode notification checks on application startup.

## Stage 1 goals

1. Review and harden authentication/session restoration and logout/account-switch behavior.
2. Review API/network configuration and error handling without changing the Website.
3. Review catalog cache-first startup, pagination, revision handling, and offline behavior.
4. Review local account isolation and library synchronization.
5. Keep UI screens as technical placeholders; do not design navigation or screen appearance.
6. Add or improve tests for the above where the current repository/test setup supports it.

## Work status

- [x] Stage 1 branch created: `stage1-technical-foundation`.
- [x] Existing application stack and key API/auth/catalog entry points inspected.
- [ ] Complete source-tree inventory and review all implementations/DAOs/DI bindings.
- [ ] Audit auth flows, including expired/invalid token, network outage, callback state mismatch, logout, and account switch.
- [ ] Audit catalog refresh/cache behavior and offline-first startup.
- [ ] Audit sync account isolation and race conditions.
- [ ] Add tests for confirmed issues and run build/test validation.
- [ ] Report any Website API changes needed separately; do not modify Website in this project.

## Important constraints

- Work only in the Android repository.
- Do not implement screen design or navigation-bar choices.
- Do not implement or test the player in Stage 1.
- Do not implement notifications or APK updating in Stage 1.
- Do not change Website or backend repositories.
- User-facing strings must not expose internal infrastructure terminology.
