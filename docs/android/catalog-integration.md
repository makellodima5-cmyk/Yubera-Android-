# Android catalog integration

## Contract

Android reads the public, read-only endpoint `GET /api/mobile/catalog` on the existing YUME website origin. The response uses schema version 1 and contains a catalogue revision, update timestamp, total count, page count and anime items. The server caps page size at 48.

The Android client does not access D1, the private catalogue Reader Worker, or its token. It reads only the published R2-backed snapshot exposed by the website route.

## Local-first behavior

- The app downloads all pages from one catalogue revision.
- If the revision changes while pages are being downloaded, the client retries once.
- The complete snapshot and revision metadata are committed to Room in one transaction.
- A failed request leaves the previous local snapshot intact.
- The catalogue remains observable from Room while offline.

## Deployment note

The website repository CI validates the route and Worker build, but GitHub Actions is not the production deployment mechanism. The public route becomes available only after the website's configured Cloudflare deployment pipeline publishes the commit. Until then, Android handles API errors and continues showing a cached snapshot if one exists.

## Current limitations

- Search and genre filtering currently operate against the locally cached catalogue.
- Detail-page navigation and local favorite/watch-later/subscription actions are implemented. Remote account synchronization still depends on native authentication.
- No user session or private account data is required to read the catalogue.
