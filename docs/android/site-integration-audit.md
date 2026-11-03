# YUBERA Android — read-only site integration audit

**Audit date:** 2026-11-03  
**Website repository:** `makellodima5-cmyk/-YUME-Website`  
**Website main commit inspected:** `f9c3522fd673af98e729b62c97d0ea6f7cd136c1`  
**Scope:** read-only inspection of the current website main tree and route/data implementations. No website files, Workers, D1 data, Cloudflare settings, or production configuration were changed.

## Findings

### 1. Telegram OIDC / native session

Existing routes:

- `POST /api/telegram/oidc` starts the Telegram OIDC authorization-code flow.
- `GET /api/telegram/oidc/callback` handles the browser callback.

The implementation already uses PKCE S256, nonce validation, signed Telegram ID-token verification, short-lived server-side state, and the D1-backed session table. However, the current flow is browser-specific:

- The callback URI is fixed to the website origin plus `/api/telegram/oidc/callback`.
- OIDC state is bound to the HttpOnly `__Host-yume-oidc-state` cookie.
- Success creates the existing `__Host-yume-session` HttpOnly/Secure/SameSite=Lax cookie and redirects to the website profile.
- No native-app callback/deep-link route or one-time app-session exchange endpoint was found in the inspected API route tree.

**Consequence:** Android must not try to copy or read the browser cookie. A native flow needs an explicitly designed app callback and secure one-time code/session exchange, while keeping Telegram client secrets on the server. Telegram OIDC redirect-URI registration may also need an approved native callback URI.

### 2. Existing account-owned data routes

- `/api/telegram/library`: GET and POST; cookie-session authenticated. POST supports individual favorite/watchlist changes and a merge operation.
- `/api/telegram/history`: GET, POST and DELETE; cookie-session authenticated.
- `/api/anime-subscriptions`: GET, POST and DELETE; cookie-session authenticated.
- The write routes currently use same-origin protections intended for browser requests.

These contracts are useful and should be reused where possible, but Android needs a supported native authentication mechanism before it can call them safely. Do not invent a bearer-token contract or bypass the same-origin protections in the Android client.

### 3. Public catalogue

The current site uses the R2-first catalogue architecture:

- The website server calls `getShikimoriCatalog` through a TanStack Start server function.
- `src/data/catalog-r2.server.ts` reads the published snapshot through the internal `YUME_CATALOG_READER` service binding.
- The reader Worker exposes `/v1/state` only behind `X-Yume-Reader-Token` and is bound as an internal Cloudflare service.
- The inspected public API route tree has no stable public REST endpoint for Android to fetch a published catalogue manifest/snapshot.

**Consequence:** Android must not call the private reader Worker, expose its token, access D1, or use a website server-function implementation detail as a permanent mobile API. A small public read-only API contract is needed on the website Worker, returning the published revision metadata and snapshot (or an agreed paged equivalent) from the existing R2/KV flow.

### 4. Existing Kodik route

- `GET /api/kodik?animeId=...` exists.
- It validates the Shikimori anime ID, applies rate limiting, calls Kodik server-side, and filters returned player links to trusted HTTPS Kodik hosts.

This route can be reused by the Android player screen after native app auth/catalog integration and provider terms are checked. Kodik credentials must remain server-side.

### 5. News

No dedicated public news-feed REST route was found in the inspected route tree. The current website primarily renders its feed through its application/server data layer. Before Android news integration, the allowed source coverage, attribution requirements, and a stable feed contract must be agreed and verified; do not represent a partial source as complete industry coverage.

## User approval checkpoint before site changes

No website/backend changes have been made. Before implementing the integration, obtain the user's explicit approval for the smallest necessary website changes:

1. Add a native Telegram OIDC callback and secure one-time app-session exchange. Keep all client secrets on the server; store only revocable/expiring app credentials using Android Keystore-backed storage.
2. Add a public read-only catalogue endpoint backed by the existing R2/KV published snapshot. Do not expose the internal reader token or D1.
3. Adapt or extend account-owned library/history/subscription routes to accept the new native app session without weakening browser CSRF protections.
4. Agree the news feed source/attribution contract before building the native news client.

Keep the website and Android as separate clients of the same backend. Do not modify production data or perform migrations until a specific implementation plan and rollback strategy are reviewed.
