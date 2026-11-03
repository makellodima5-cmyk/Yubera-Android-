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


## Follow-up verification: dependency injection and startup session boundary

**Verified date:** 2026-11-03  
**Verified commit:** `544aacabd4b3268a656d0213fa412b1547085d93`

GitHub Actions run #10: [Android CI](https://github.com/makellodima5-cmyk/Yubera-Android-/actions/runs/41640840948)

The same clean verification command completed with `BUILD SUCCESSFUL in 4m 8s`:

```bash
./gradlew --no-daemon clean :app:processDebugResources :app:assembleDebug :app:testDebugUnitTest
```

Observed:
- `:app:processDebugResources` completed.
- `:app:assembleDebug` completed.
- `:app:compileDebugUnitTestKotlin` completed.
- `:app:testDebugUnitTest` completed.
- Hilt KSP and aggregation tasks completed.
- The CI workflow uploaded a new `yubera-debug-apk` artifact.

The TDD RED check was run in CI run #9 before implementation. It failed on the expected missing startup/session types after dependency setup and main-source compilation succeeded. Run #10 is the GREEN check after those types and the Hilt application binding were added. The build output does not provide a reliable test-count summary, so no test count is claimed.

New artifact:
- Artifact ID: `13232577386`.
- Name: `yubera-debug-apk`.
- Size: `18,512,589` bytes.
- SHA-256 (artifact ZIP): `10413d8cbc0679714b3bce620832ea37e3519e93a6f0a786719468a86c16921d`.
- Expires: 2026-11-17 13:07:17 UTC.

This increment adds the `SessionStateProvider` boundary and `StartupCoordinator`, with an explicitly temporary `UnconfiguredSessionStateProvider` that always reports no restorable session. It is not native authentication. The coordinator is not yet connected to Activity startup, and the branded startup UI is not yet implemented.

The earlier limitations still apply: no physical-device or emulator runtime verification, no real authentication/backend/catalog/player integration, and no release readiness.
