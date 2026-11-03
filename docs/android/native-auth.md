# Native Android authentication

## Android flow

- The app generates an independent PKCE verifier and app state for every Telegram sign-in attempt.
- The verifier and state are encrypted with AES-GCM using a non-exportable Android Keystore key. SharedPreferences stores ciphertext only.
- The app opens the website's native OIDC start URL in the system browser. Telegram redirects to the existing website callback; browser cookies are never copied into the app.
- The callback deep link is fixed to `yubera://auth/callback`. The app validates its stored state before exchanging the one-time code.
- The backend verifies the S256 challenge and returns a revocable 30-day bearer session. The token is encrypted by the same Keystore-backed store.
- Subsequent session checks and account API calls use `Authorization: Bearer`. Logout revokes the server session and clears local encrypted values.

## Security boundaries

- Telegram client secret and YUBERA session HMAC secret remain server-side.
- The deep-link code is short-lived and single-use. PKCE prevents another app from exchanging an intercepted code without the verifier.
- The Android app does not read, copy, or depend on browser cookies.
- Startup keeps the user in the app with cached/offline data if session validation cannot reach the server. An explicit unauthenticated response or HTTP 401 clears the stored token.
- The first implementation has no refresh token; after 30 days the user signs in again.

## Current limitation

Registration remains intentionally unavailable in Android until the final user agreement and privacy policy are published and approved. The backend native flow supports a consent-gated registration request, but the app does not show a fake or unreviewed legal document.
