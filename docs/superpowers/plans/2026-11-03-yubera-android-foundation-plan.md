# YUBERA Android Foundation and App Shell Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a buildable native Android project with the agreed Kotlin/Compose architecture, original visual resources, real-launch startup flow, and five-destination navigation shell.

**Architecture:** Use a single Android application module with feature-oriented Compose packages and MVVM-ready boundaries. The startup Activity hosts a branded in-app startup surface only during genuine process initialization; the app shell owns the five top-level navigation destinations. Authentication integration is represented by a narrow session-state interface until the separate authentication plan is approved and backend compatibility is verified.

**Tech Stack:** Kotlin 2.2.10 (AGP built-in Kotlin), Jetpack Compose (Compose compiler plugin 2.2.10), Material 3, Navigation Compose, Hilt, Coroutines/Flow, DataStore, Room, Retrofit/OkHttp, Kotlin Serialization, Coil, WorkManager, AndroidX SplashScreen. Android Gradle Plugin 9.1.1, Gradle 9.3.1, JDK 17; toolchain compatibility rechecked against official Android docs on 2026-11-03.

**Spec:** `docs/superpowers/specs/2026-11-03-yubera-android-design.md`

## Global Constraints

- Minimum SDK: 26 (Android 8.0).
- Target SDK: latest stable SDK available and compatible at project creation.
- Initial UI language: Russian.
- Main branch: `main`.
- Use original supplied PNG assets without destructive modification.
- Implement screens with native Compose; do not use full-screen screenshots as UI.
- Branded startup screen appears only on genuine app process startup while actual initialization is running.
- Do not add artificial startup delays or fake percentage progress.
- Do not show the branded startup screen for navigation, ordinary return from background while process remains alive, or Activity recreation.
- Five top-level destinations: Главная, Каталог, Моё, Новости, Профиль.
- No guest mode; session routing must be abstracted pending the separate authentication implementation.
- Never place backend secrets in the APK; Android must not access D1 directly.

## Review Focus

- Cold process start versus warm foreground return: test that only cold starts show the branded startup surface.
- Activity recreation/configuration change: test that it does not restart the branded startup flow.
- Slow initialization: test that startup remains visible until actual initialization completes, without a timer.
- Missing or malformed optional artwork: test safe resource fallback and no crash.
- Navigation state restoration: test selecting each of the five destinations and restoring the selected destination after Activity recreation.

---

### Task 1: Create the Android project skeleton

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle/libs.versions.toml`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/yubera/app/YuberaApplication.kt`
- Create: `app/src/main/java/com/yubera/app/MainActivity.kt`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`

**Interfaces:**
- Produces: Android application ID `com.yubera.app`; `YuberaApplication`; `MainActivity`; Gradle tasks `:app:assembleDebug` and `:app:testDebugUnitTest`.

- [x] **Step 1: Add Gradle wrapper and project configuration** with pinned compatible stable plugin/library versions and Java 17 toolchain. AGP 9.1.1 requires Gradle 9.3.1 and JDK 17; AGP supplies built-in Kotlin 2.2.10, so do not also apply `org.jetbrains.kotlin.android`. Apply `org.jetbrains.kotlin.plugin.compose` 2.2.10 where Compose is enabled.
- [x] **Step 2: Configure app module** with namespace/applicationId `com.yubera.app`, minSdk 26, selected stable target/compile SDK, Compose enabled, and release minification disabled until release hardening.
- [x] **Step 3: Add the Application and Activity entry points** and register them in the manifest.
- [x] **Step 4: Run the project configuration/build check** using `./gradlew :app:assembleDebug`; expected result: a successful debug APK build.
- [x] **Step 5: Commit**. The scaffold, CI workflow and wrapper were added in separate verified commits.

### Task 2: Establish app architecture and dependency injection

**Files:**
- Create: `app/src/main/java/com/yubera/app/di/AppModule.kt`
- Create: `app/src/main/java/com/yubera/app/core/model/StartupState.kt`
- Create: `app/src/main/java/com/yubera/app/core/session/SessionStateProvider.kt`
- Create: `app/src/main/java/com/yubera/app/core/startup/StartupCoordinator.kt`
- Create: `app/src/test/java/com/yubera/app/core/startup/StartupCoordinatorTest.kt`

**Interfaces:**
- `SessionStateProvider`: `suspend fun hasRestorableSession(): Boolean`.
- `StartupCoordinator`: `suspend fun initialize(): StartupDestination`.
- `StartupDestination`: sealed destinations `SignIn` and `Home`.

- [x] **Step 1: Write unit tests** for initialization completion and destination selection using a fake `SessionStateProvider`.
- [x] **Step 2: Run tests** with `./gradlew :app:testDebugUnitTest`; the expected RED result was observed in CI run [#9](https://github.com/makellodima5-cmyk/Yubera-Android-/actions/runs/41631028143): dependencies and main-source compilation succeeded, and test compilation failed only because the three planned production types did not yet exist.
- [x] **Step 3: Implement the session abstraction and coordinator** without adding provider-specific authentication behavior.
- [x] **Step 4: Run unit tests**; the expected GREEN result was observed in CI run [#10](https://github.com/makellodima5-cmyk/Yubera-Android-/actions/runs/41640840948).
- [x] **Step 5: Commit** as `feat: add startup coordination and session boundary` (`544aacabd4b3268a656d0213fa412b1547085d93`).

### Task 3: Integrate original visual assets and design tokens

**Files:**
- Create: `app/src/main/res/drawable-nodpi/yubera_logo.png`
- Create: `app/src/main/res/drawable-nodpi/yubera_splash_background.png`
- Create: `app/src/main/res/drawable-nodpi/yubera_app_icon_source.png`
- Create: `app/src/main/res/drawable-nodpi/yubera_auth_background.png`
- Create: `app/src/main/res/drawable-nodpi/yubera_banner.png`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create: `app/src/main/res/drawable/ic_launcher_foreground.xml` (or a bitmap/vector foreground derived from the supplied icon)
- Create: `app/src/main/res/values/ic_launcher_background.xml`
- Create: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/java/com/yubera/app/core/ui/theme/Color.kt`
- Create: `app/src/main/java/com/yubera/app/core/ui/theme/Theme.kt`

**Interfaces:**
- Produces: central YUBERA color/theme definitions and named drawable resources.
- Asset source: the latest user-provided archive; map files by visual inspection and dimensions before assigning resource names.

- [x] **Step 1: Inventory every PNG** by previewing it, recording dimensions, transparency and duplicate hashes; identify logo, app icon, startup art, banner and screen-specific artwork.
- [ ] **Step 2: Copy originals unchanged** into Android-supported resource directories; avoid unsupported nested directories under `res/drawable`. Build a valid adaptive launcher icon with both foreground and background resources referenced by `mipmap-anydpi-v26/ic_launcher.xml`; a lone XML file is not a complete adaptive icon.
- [ ] **Step 3: Define theme tokens** from the approved screen references; do not invent a new visual direction.
- [ ] **Step 4: Verify resource compilation** with `./gradlew :app:processDebugResources`; expected result: all resources compile and resolve.
- [ ] **Step 5: Commit** as `feat: add YUBERA visual resources and theme`.

### Task 4: Implement genuine-launch startup experience

**Files:**
- Create: `app/src/main/java/com/yubera/app/core/startup/StartupViewModel.kt`
- Create: `app/src/main/java/com/yubera/app/core/startup/StartupScreen.kt`
- Create: `app/src/main/java/com/yubera/app/core/startup/StartupUiState.kt`
- Modify: `app/src/main/java/com/yubera/app/MainActivity.kt`
- Test: `app/src/test/java/com/yubera/app/core/startup/StartupViewModelTest.kt`

**Interfaces:**
- `StartupUiState`: `Initializing`, `Ready(destination: StartupDestination)`, `Failed(retryable: Boolean)`.
- `StartupViewModel`: exposes `StateFlow<StartupUiState>` and starts one initialization operation per process launch.

- [ ] **Step 1: Write tests** proving the startup state remains initializing until coordinator completion and emits the correct destination afterward.
- [ ] **Step 2: Run tests**; expected result: fail before implementation.
- [ ] **Step 3: Implement StartupScreen** using the supplied startup artwork, logo and an indeterminate progress indicator; do not show a fake numeric percentage.
- [ ] **Step 4: Connect Activity lifecycle correctly** so configuration changes do not restart startup, and returning to an existing process does not re-show the branded screen.
- [ ] **Step 5: Run unit tests and build**; expected result: startup tests pass and debug APK builds.
- [ ] **Step 6: Commit** as `feat: add process-start branded loading screen`.

### Task 5: Implement the five-destination navigation shell

**Files:**
- Create: `app/src/main/java/com/yubera/app/navigation/AppDestination.kt`
- Create: `app/src/main/java/com/yubera/app/navigation/AppNavHost.kt`
- Create: `app/src/main/java/com/yubera/app/navigation/BottomNavigationBar.kt`
- Create: `app/src/main/java/com/yubera/app/features/home/HomeScreen.kt`
- Create: `app/src/main/java/com/yubera/app/features/catalog/CatalogScreen.kt`
- Create: `app/src/main/java/com/yubera/app/features/my/MyScreen.kt`
- Create: `app/src/main/java/com/yubera/app/features/news/NewsScreen.kt`
- Create: `app/src/main/java/com/yubera/app/features/profile/ProfileScreen.kt`
- Test: `app/src/androidTest/java/com/yubera/app/navigation/BottomNavigationTest.kt`

**Interfaces:**
- `AppDestination`: stable routes `home`, `catalog`, `my`, `news`, `profile`.
- `AppNavHost`: accepts the initial destination and owns top-level navigation.
- Feature screens are independent composables; real data and detailed UI are delivered by their own feature plans.

- [ ] **Step 1: Write Compose navigation tests** that select each destination and verify its screen content is displayed.
- [ ] **Step 2: Implement destination definitions and Navigation Compose host** with state restoration.
- [ ] **Step 3: Implement the bottom bar** matching the supplied visual references and Russian labels.
- [ ] **Step 4: Add feature screen placeholders** with correct screen ownership and no duplicated features; placeholders must be clearly temporary and must not masquerade as completed functionality.
- [ ] **Step 5: Run** `./gradlew :app:connectedDebugAndroidTest` where an emulator is available; otherwise run unit/build checks and record that device verification remains outstanding.
- [ ] **Step 6: Commit** as `feat: add five-section navigation shell`.

### Task 6: Verify and document the foundation milestone

**Files:**
- Create: `docs/android/asset-map.md`
- Create: `docs/android/foundation-verification.md`

- [x] **Step 1: Document the exact source-to-resource mapping** for all supplied images, including unused references and duplicate file.
- [x] **Step 2: Run** `./gradlew clean :app:assembleDebug :app:testDebugUnitTest`; CI run [#10](https://github.com/makellodima5-cmyk/Yubera-Android-/actions/runs/41640840948) completed the clean resource, APK and unit-test tasks successfully.
- [ ] **Step 3: Run instrumentation tests** on an Android 8.0/API 26 emulator and a current Android emulator when available; record any unavailable environment.
- [ ] **Step 4: Verify startup behavior** for cold launch, warm foreground return, Activity recreation and process recreation.
- [ ] **Step 5: Commit** as `test: verify Android foundation milestone`.

## Follow-on plans (separate documents)

These are intentionally separate because they are independently testable subsystems:
1. Authentication and secure account session integration, after confirming backend PKCE compatibility.
2. Home and catalog data integration, including the existing public catalog API contract.
3. Personal library, local-first persistence and cross-client synchronization.
4. News feed, article details, attribution and source coverage.
5. Profile, curated avatars, theme/language preferences and notification settings.
6. Kodik player integration and WebView constraints.
7. Personal anime subscriptions and scheduled notifications.
8. Release signing, APK packaging and device acceptance testing.
