# Android Kodik player integration

## Data flow

- The Android app calls the existing YUBERA route `GET /api/kodik?animeId=...`.
- The YUBERA Worker keeps the Kodik API token server-side.
- The route returns source/translation groups with numbered episodes and HTTPS player links.
- Android rejects malformed anime IDs and independently validates each returned link against the same trusted Kodik host allowlist before opening it.

## WebView boundary

WebView is used only inside the dedicated native player destination. The rest of the app is native Compose.

- JavaScript and DOM storage are enabled because the embedded player requires them.
- File/content access and mixed content are disabled.
- Multiple windows and JavaScript-opened windows are disabled.
- Top-level navigation is allowed only to HTTPS URLs on the trusted Kodik host allowlist.
- External top-level links are blocked; no external intent is launched automatically.
- Android does not inject scripts, extract media streams, bypass DRM, or download video.

## Limitations

- The app relies on the existing server route and Kodik availability/configuration.
- WebView playback behavior, ads, cookies and device compatibility still require real Android-device testing.
- Opening a trusted episode records the last-opened episode in local history and syncs that record across devices. The record starts at position 0 because selecting/opening an episode is not proof that it was watched.
- Actual playback position and duration are not captured: the embedded third-party player does not currently expose a verified progress callback to the native app.
- No offline video is supported.
