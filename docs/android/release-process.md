# YUBERA Android release process
- `versionCode` must increase monotonically; `versionName` is semantic versioning.
- The signed release workflow is manual and runs only from `main`.
- Required Actions secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`. The signing key must remain stable across releases.
- Run **Prepare signed YUBERA Android release** with version name/code, minimum supported code and notes. It builds a signed APK, computes SHA-256 and size, writes `yubera-update.json`, and creates a **draft** GitHub release. It is not published automatically.
- The manifest contains schemaVersion, versionCode, versionName, minimumSupportedVersionCode, releaseNotes, apkUrl, apkSha256, fileSizeBytes, publishedAt. The Android client checks GitHub Releases, validates the manifest, downloads from allowlisted HTTPS hosts, checks APK size/hash/package/version/signing certificate, then opens Android's installer for user confirmation.
- Without signing secrets, the CI release APK is unsigned and cannot be prepared as a distributable update.
