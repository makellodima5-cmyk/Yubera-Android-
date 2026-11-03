# YUBERA Android — Foundation Verification

**Verification date:** 2026-11-03  
**Repository:** `makellodima5-cmyk/Yubera-Android-`  
**Branch:** `main`  
**Verified commit:** `6ce56e28e20eae84ee088cd668918689d99c4214`

## Automated build

<badge>SUCCESS</badge>

GitHub Actions run #5: [Android CI](https://github.com/makellodima5-cmyk/Yubera-Android-/actions/runs/41629457593)

The job completed with conclusion `success`. It executed this command:

```bash
./gradlew --no-daemon clean :app:processDebugResources :app:assembleDebug :app:testDebugUnitTest
```

Observed output:
- `:app:processDebugResources` completed.
- `:app:assembleDebug` completed.
- `:app:testDebugUnitTest` completed.
- Gradle reported `BUILD SUCCESSFUL in 3m 20s`.
- GitHub Actions uploaded the `yubera-debug-apk` artifact.

The job output does not include a test-count summary, so this record confirms the unit-test task completed successfully but does not claim a specific test count.

## Toolchain used by the successful run

- Android Gradle Plugin: 9.1.1.
- Gradle Wrapper: 9.3.1.
- Java: 17.
- Kotlin: AGP built-in Kotlin 2.2.10.
- Compose BOM: 2026.09.00.
- `compileSdk`: 37.
- `targetSdk`: 37.
- `minSdk`: 26.

The stable Android platform package used in CI is `platforms;android-37.0`, installed under `$ANDROID_HOME/platforms/android-37.0`.

## Debug APK artifact

- Artifact name: `yubera-debug-apk`.
- Artifact ID: `13227433085`.
- Size: 18,183,590 bytes.
- SHA-256 (artifact ZIP): `45ce8ce7f166039334a918468223bbcc892e24d5a90692d58a9084cc50c95107`.
- Expires: 2026-11-17 11:44:20 UTC.
- Download through the Actions run above.

The artifact ZIP contains `app-debug.apk` (18,726,860 bytes). The extracted APK was checked as an Android package and its ZIP structure passed integrity validation.

## What this proves, and what it does not

Proven:
- The project configures under the pinned JDK/Gradle/AGP toolchain.
- Android resources compile.
- The debug APK assembles.
- The existing unit-test task exits successfully.
- The APK artifact is uploaded and downloadable.

Not yet proven:
- Installation or runtime behavior on a physical Android device.
- Android 8.0/API 26 compatibility in an emulator.
- Compose navigation instrumentation tests.
- Startup process-death, warm-return or Activity-recreation behavior.
- Any real authentication, backend, catalog, player or personal-data feature.
- Release signing or release APK readiness.

Current screens remain placeholders. This milestone is the buildable project foundation, not a user-ready app.
