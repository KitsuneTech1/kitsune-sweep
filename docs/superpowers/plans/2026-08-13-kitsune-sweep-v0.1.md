# Kitsune Sweep v0.1 implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a sideloadable Android APK that finds large and duplicate shared files, ranks installed apps by storage and last use, and opens honest Android or Samsung cleanup controls.

**Architecture:** A single Kotlin Android module uses Jetpack Compose for four destinations and manual dependency injection. Pure domain policies remain JVM-testable. Android repositories isolate storage, package, usage, and settings APIs, while one activity owns permission and deletion result contracts.

**Tech Stack:** Kotlin with AGP 9.0.1 built-in Kotlin, Gradle 9.1.0, compile and target SDK 36, Jetpack Compose BOM 2026.05.01, Activity Compose 1.12.4, Lifecycle 2.10.0, Kotlin coroutines 1.10.2, JUnit 4, and AndroidX test.

## Global constraints

- Package name is `com.kitsunetech.sweep`.
- Version is `0.1.0`, version code 1.
- Minimum Android version is API 28. Compile and target SDK are API 36.
- The app is a personal sideload build for a Samsung Galaxy S25 Ultra.
- The manifest must not declare Internet, contacts, location, microphone, camera, notifications, or accessibility permissions.
- All Files Access and Usage Access stay optional. Denial must leave read-only fallback functions usable.
- No automatic deletion. Nothing is selected by default. Every deletion shows file count and total bytes first.
- Never follow symbolic links or delete directories, protected roots, private app paths, or the app's own files.
- System apps are never uninstall suggestions.
- Cache copy must describe ordinary cache accurately and must not promise a speed increase.
- UI touch targets are at least 48 dp and information never depends on color alone.
- No em dash, en dash, smart quote, corporate filler, or fake cleaner language in visible copy.

---

### Task 1: Build foundation and byte formatting

**Files:**

- Create: `.gitignore`
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Copy from the verified local Android project: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- Create: `app/src/test/java/com/kitsunetech/sweep/domain/ByteSizeFormatterTest.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/domain/ByteSizeFormatter.kt`

**Interfaces:**

- Produces: `fun Long.toReadableBytes(): String`
- Produces: an Android module that compiles Kotlin and Compose against API 36.

- [ ] **Step 1: Add the Gradle project and manifest**

Use AGP 9.0.1 and Gradle 9.1.0. Apply `org.jetbrains.kotlin.plugin.compose` version 2.2.10. Enable Compose and BuildConfig. Declare only `MANAGE_EXTERNAL_STORAGE`, `PACKAGE_USAGE_STATS`, and `QUERY_ALL_PACKAGES`. Do not declare `INTERNET`.

The app dependencies are:

```kotlin
val composeBom = platform("androidx.compose:compose-bom:2026.05.01")
implementation(composeBom)
androidTestImplementation(composeBom)
implementation("androidx.activity:activity-compose:1.12.4")
implementation("androidx.compose.foundation:foundation")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
debugImplementation("androidx.compose.ui:ui-tooling")
debugImplementation("androidx.compose.ui:ui-test-manifest")
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
androidTestImplementation("androidx.test.ext:junit:1.3.0")
androidTestImplementation("androidx.test:runner:1.7.0")
```

- [ ] **Step 2: Write the failing byte-format tests**

```kotlin
class ByteSizeFormatterTest {
    @Test fun formatsZero() = assertEquals("0 B", 0L.toReadableBytes())
    @Test fun formatsBinaryMegabytes() = assertEquals("1.5 MB", 1_572_864L.toReadableBytes())
    @Test fun clampsNegativeValues() = assertEquals("0 B", (-20L).toReadableBytes())
}
```

- [ ] **Step 3: Run the focused test and verify RED**

Run: `./gradlew testDebugUnitTest --tests "*ByteSizeFormatterTest"`

Expected: compilation fails because `toReadableBytes` does not exist.

- [ ] **Step 4: Implement the formatter**

Implement binary units B, KB, MB, GB, and TB. Show no decimal below 1 KB, one decimal when the result is below 10, and no decimal for larger values. Use `Locale.US` so tests and UI stay deterministic.

- [ ] **Step 5: Run the test and verify GREEN**

Run: `./gradlew testDebugUnitTest --tests "*ByteSizeFormatterTest"`

Expected: 3 tests pass.

- [ ] **Step 6: Commit the foundation**

```text
git add .
git commit -m "build: start Kitsune Sweep Android app"
```

---

### Task 2: Safe large-file scanning

**Files:**

- Create: `app/src/main/java/com/kitsunetech/sweep/domain/StorageFile.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/domain/FileSafetyPolicy.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/data/storage/StorageScanner.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/data/storage/DirectStorageScanner.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/data/storage/MediaStoreScanner.kt`
- Create: `app/src/test/java/com/kitsunetech/sweep/domain/FileSafetyPolicyTest.kt`
- Create: `app/src/test/java/com/kitsunetech/sweep/data/storage/DirectStorageScannerTest.kt`

**Interfaces:**

- Produces: `data class StorageFile(val id: String, val displayName: String, val path: String?, val contentUri: String?, val sizeBytes: Long, val modifiedAtMillis: Long, val mimeType: String?, val source: FileSource)`
- Produces: `enum class FileSource { DIRECT, MEDIA_STORE }`
- Produces: `interface StorageScanner { suspend fun scanLargeFiles(minBytes: Long, onProgress: (ScanProgress) -> Unit): ScanResult }`
- Produces: `data class ScanProgress(val visited: Long, val matched: Int)`
- Produces: `data class ScanResult(val files: List<StorageFile>, val skipped: Int, val errors: List<String>)`
- Produces: `FileSafetyPolicy.canDelete(path: Path, roots: Set<Path>): Boolean`

- [ ] **Step 1: Write failing safety-policy tests**

Cover a normal shared file, a directory, a symbolic link, the storage root, `Android`, `Android/data`, and a path outside the allowed shared root. The allowed file must return true. Every other case must return false.

- [ ] **Step 2: Run safety tests and verify RED**

Run: `./gradlew testDebugUnitTest --tests "*FileSafetyPolicyTest"`

Expected: compilation fails because `FileSafetyPolicy` does not exist.

- [ ] **Step 3: Implement fail-closed path checks**

Normalize absolute paths without following links. Confirm the candidate starts with exactly one allowed root. Reject the root itself, directories, symbolic links, any first relative segment equal to `Android`, and any path whose metadata cannot be read.

- [ ] **Step 4: Run safety tests and verify GREEN**

Run: `./gradlew testDebugUnitTest --tests "*FileSafetyPolicyTest"`

Expected: all path-policy tests pass.

- [ ] **Step 5: Write failing direct-scanner tests**

Create a temporary directory with 20 MB, 100 MB, and 150 MB sparse files plus a nested directory. Scan at 100 MB. Assert that the result contains the 150 MB file and the 100 MB file, excludes the 20 MB file, sorts descending, and reports progress.

- [ ] **Step 6: Run scanner tests and verify RED**

Run: `./gradlew testDebugUnitTest --tests "*DirectStorageScannerTest"`

Expected: compilation fails because `DirectStorageScanner` does not exist.

- [ ] **Step 7: Implement both scanners**

`DirectStorageScanner` uses `Files.walkFileTree` without `FOLLOW_LINKS`, checks cancellation once per entry, emits progress every 128 visited entries, and retains only regular files meeting the threshold. `MediaStoreScanner` queries `MediaStore.Files` for size, display name, date modified, MIME type, and content URI, then applies the same threshold and descending sort.

- [ ] **Step 8: Run scanner tests and verify GREEN**

Run: `./gradlew testDebugUnitTest --tests "*DirectStorageScannerTest" --tests "*FileSafetyPolicyTest"`

Expected: all scanner and safety tests pass.

- [ ] **Step 9: Commit storage scanning**

```text
git add app/src/main app/src/test
git commit -m "feat: scan shared storage for large files"
```

---

### Task 3: Exact duplicates and deletion planning

**Files:**

- Create: `app/src/main/java/com/kitsunetech/sweep/domain/DuplicateDetector.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/domain/DeletePlan.kt`
- Create: `app/src/test/java/com/kitsunetech/sweep/domain/DuplicateDetectorTest.kt`
- Create: `app/src/test/java/com/kitsunetech/sweep/domain/DeletePlanTest.kt`

**Interfaces:**

- Produces: `interface ContentHasher { suspend fun sha256(file: StorageFile): String }`
- Produces: `class DuplicateDetector(private val hasher: ContentHasher) { suspend fun findExact(files: List<StorageFile>, onProgress: (HashProgress) -> Unit): List<DuplicateGroup> }`
- Produces: `data class DuplicateGroup(val sha256: String, val files: List<StorageFile>, val reclaimableBytes: Long)`
- Produces: `data class DeletePlan(val files: List<StorageFile>, val totalBytes: Long)`
- Produces: `fun buildDeletePlan(selected: Collection<StorageFile>): DeletePlan`

- [ ] **Step 1: Write failing duplicate tests**

Use a fake `ContentHasher` that records calls. Assert that unique-size files are never hashed, same-size files are hashed, only equal hashes form a group, each group contains at least two files, and reclaimable bytes equal the sum minus one retained copy.

- [ ] **Step 2: Run duplicate tests and verify RED**

Run: `./gradlew testDebugUnitTest --tests "*DuplicateDetectorTest"`

Expected: compilation fails because `DuplicateDetector` does not exist.

- [ ] **Step 3: Implement size-first exact duplicate grouping**

Group positive-size files by `sizeBytes`, discard singleton groups, hash candidates, group by SHA-256, discard hash singletons, sort files within each group by path or display name, and sort groups by reclaimable bytes descending.

- [ ] **Step 4: Run duplicate tests and verify GREEN**

Run: `./gradlew testDebugUnitTest --tests "*DuplicateDetectorTest"`

Expected: all duplicate tests pass.

- [ ] **Step 5: Write failing delete-plan tests**

Assert empty selection produces zero files and bytes. Assert duplicate IDs are counted once. Assert negative sizes contribute zero bytes. Assert the output is sorted by size descending.

- [ ] **Step 6: Run delete-plan tests and verify RED**

Run: `./gradlew testDebugUnitTest --tests "*DeletePlanTest"`

Expected: compilation fails because `buildDeletePlan` does not exist.

- [ ] **Step 7: Implement deletion planning and verify GREEN**

Run: `./gradlew testDebugUnitTest --tests "*DeletePlanTest" --tests "*DuplicateDetectorTest"`

Expected: all deletion and duplicate tests pass.

- [ ] **Step 8: Commit duplicate analysis**

```text
git add app/src/main app/src/test
git commit -m "feat: find exact duplicate files"
```

---

### Task 4: Installed-app, cache, and last-use inventory

**Files:**

- Create: `app/src/main/java/com/kitsunetech/sweep/domain/AppRecord.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/domain/ColdAppPolicy.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/data/apps/AppInventoryRepository.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/data/apps/AndroidAppInventoryRepository.kt`
- Create: `app/src/test/java/com/kitsunetech/sweep/domain/ColdAppPolicyTest.kt`
- Create: `app/src/test/java/com/kitsunetech/sweep/data/apps/AppInventoryRepositoryTest.kt`

**Interfaces:**

- Produces: `data class AppRecord(val packageName: String, val label: String, val appBytes: Long?, val dataBytes: Long?, val cacheBytes: Long?, val lastUsedAtMillis: Long?, val firstInstalledAtMillis: Long, val isSystem: Boolean, val isCold: Boolean)`
- Produces: `fun classifyColdApp(isSystem: Boolean, lastUsedAtMillis: Long?, nowMillis: Long, thresholdDays: Long = 90): Boolean`
- Produces: `interface AppInventoryRepository { suspend fun loadApps(onProgress: (AppProgress) -> Unit): List<AppRecord> }`

- [ ] **Step 1: Write failing cold-app tests**

Assert a user app last used 91 days ago is cold. Assert 89 days is not cold. Assert system apps are never cold. Assert an unknown last-use timestamp is not cold.

- [ ] **Step 2: Run policy tests and verify RED**

Run: `./gradlew testDebugUnitTest --tests "*ColdAppPolicyTest"`

Expected: compilation fails because `classifyColdApp` does not exist.

- [ ] **Step 3: Implement the cold-app policy and verify GREEN**

Use integer millisecond duration with a 90-day default. Treat future timestamps and non-positive timestamps as unknown.

Run: `./gradlew testDebugUnitTest --tests "*ColdAppPolicyTest"`

Expected: all cold-app tests pass.

- [ ] **Step 4: Write failing inventory merge tests**

Test the pure merge function with installed-package facts, usage facts, and optional storage facts. Assert missing storage becomes null, missing usage becomes null, labels sort case-insensitively, and system status survives the merge.

- [ ] **Step 5: Run inventory tests and verify RED**

Run: `./gradlew testDebugUnitTest --tests "*AppInventoryRepositoryTest"`

Expected: compilation fails because the merge function does not exist.

- [ ] **Step 6: Implement the Android repository**

Load launchable and installed packages through `PackageManager`. Query aggregate usage for the last 365 days. Query `StorageStatsManager` per package only when Usage Access is granted. Catch `NameNotFoundException`, `SecurityException`, and `IOException` per package, returning null sizes rather than failing the whole scan. Emit progress after each package.

- [ ] **Step 7: Run inventory tests and verify GREEN**

Run: `./gradlew testDebugUnitTest --tests "*AppInventoryRepositoryTest" --tests "*ColdAppPolicyTest"`

Expected: all app-policy and merge tests pass.

- [ ] **Step 8: Commit app analysis**

```text
git add app/src/main app/src/test
git commit -m "feat: rank apps by storage and last use"
```

---

### Task 5: Permission state, Samsung links, and cache actions

**Files:**

- Create: `app/src/main/java/com/kitsunetech/sweep/data/system/PermissionStateReader.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/data/system/SystemActions.kt`
- Create: `app/src/test/java/com/kitsunetech/sweep/data/system/SystemActionSpecTest.kt`
- Create: `app/src/androidTest/java/com/kitsunetech/sweep/SystemActionsInstrumentedTest.kt`

**Interfaces:**

- Produces: `data class PermissionState(val allFilesAccess: Boolean, val usageAccess: Boolean)`
- Produces: `sealed interface SystemActionSpec { data object RequestAllFiles; data object RequestUsage; data object ManageStorage; data object ClearExternalCaches; data class AppDetails(val packageName: String); data class Uninstall(val packageName: String) }`
- Produces: `fun SystemActionSpec.toIntent(packageName: String): Intent`

- [ ] **Step 1: Write failing intent-spec tests**

Assert each action maps to the documented Android action and expected `package:` URI. Assert app package names reject whitespace, slashes, and empty values before an intent is created.

- [ ] **Step 2: Run intent tests and verify RED**

Run: `./gradlew testDebugUnitTest --tests "*SystemActionSpecTest"`

Expected: compilation fails because `SystemActionSpec` does not exist.

- [ ] **Step 3: Implement permission and intent adapters**

Usage Access is true only when `AppOpsManager` returns `MODE_ALLOWED`. All Files Access uses `Environment.isExternalStorageManager()` on API 30 and higher. Manage Storage uses `Settings.ACTION_INTERNAL_STORAGE_SETTINGS`, falling back to `StorageManager.ACTION_MANAGE_STORAGE`, then `Settings.ACTION_SETTINGS` if no activity resolves. External cache clearing uses `StorageManager.ACTION_CLEAR_APP_CACHE` and remains user-confirmed by Android.

- [ ] **Step 4: Run unit tests and verify GREEN**

Run: `./gradlew testDebugUnitTest --tests "*SystemActionSpecTest"`

Expected: all intent-spec tests pass.

- [ ] **Step 5: Add emulator intent-resolution tests**

Assert request-all-files, request-usage, manage-storage, and app-details intents resolve on API 34. Do not launch deletion or uninstall during the test.

- [ ] **Step 6: Commit system actions**

```text
git add app/src/main app/src/test app/src/androidTest
git commit -m "feat: link Android storage and cache controls"
```

---

### Task 6: Screen state and view models

**Files:**

- Create: `app/src/main/java/com/kitsunetech/sweep/ui/SweepDestination.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/ui/SweepUiState.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/ui/SweepViewModel.kt`
- Create: `app/src/test/java/com/kitsunetech/sweep/ui/SweepViewModelTest.kt`

**Interfaces:**

- Produces: `enum class SweepDestination { HOME, FILES, DUPLICATES, APPS }`
- Produces: immutable `HomeState`, `FilesState`, `DuplicatesState`, and `AppsState` inside `SweepUiState`.
- Produces: `SweepViewModel.refreshPermissions()`, `scanLargeFiles(minBytes)`, `scanDuplicates()`, `loadApps()`, `toggleFile(id)`, `toggleDuplicateFile(id)`, `clearSelection()`, and `onDeleteResult(result)`.

- [ ] **Step 1: Write failing view-model tests**

Use fake repositories and a test dispatcher. Cover initial permission state, a successful large-file scan, cancellation when the threshold changes, partial scan errors, empty results, selection totals, duplicate scanning from the latest file result, and app sorting by total size, cache, and oldest use.

- [ ] **Step 2: Run view-model tests and verify RED**

Run: `./gradlew testDebugUnitTest --tests "*SweepViewModelTest"`

Expected: compilation fails because `SweepViewModel` does not exist.

- [ ] **Step 3: Implement immutable state and cancellable jobs**

Keep one job each for file, duplicate, and app scans. Starting the same scan type cancels the previous job. Run repository work on the injected IO dispatcher. Expose state through `StateFlow<SweepUiState>`. Preserve the previous successful list while a refresh runs and show progress separately.

- [ ] **Step 4: Run view-model tests and verify GREEN**

Run: `./gradlew testDebugUnitTest --tests "*SweepViewModelTest"`

Expected: all view-model tests pass with no unfinished coroutines.

- [ ] **Step 5: Commit state management**

```text
git add app/src/main app/src/test
git commit -m "feat: manage cancellable cleaner scans"
```

---

### Task 7: Compose interface and safe deletion flow

**Files:**

- Create: `app/src/main/java/com/kitsunetech/sweep/MainActivity.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/SweepDependencies.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/ui/SweepApp.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/ui/theme/Color.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/ui/theme/Type.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/ui/components/StorageStrata.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/ui/components/PermissionCard.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/ui/components/ScanStatus.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/ui/screens/HomeScreen.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/ui/screens/FilesScreen.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/ui/screens/DuplicatesScreen.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/ui/screens/AppsScreen.kt`
- Create: `app/src/main/java/com/kitsunetech/sweep/data/storage/FileDeletionCoordinator.kt`
- Create: `app/src/androidTest/java/com/kitsunetech/sweep/ui/SweepAppTest.kt`

**Interfaces:**

- Produces: `@Composable fun SweepApp(state: SweepUiState, actions: SweepActions)`
- Produces: `data class SweepActions` with callbacks for navigation, permissions, scans, selection, deletion review, app details, uninstall, storage settings, and cache clearing.
- Produces: `FileDeletionCoordinator.createRequest(plan: DeletePlan): DeletionRequest`, where MediaStore files use `MediaStore.createDeleteRequest` and direct files require the in-app confirmation before `deleteConfirmedDirectFiles` is called.

- [ ] **Step 1: Write failing Compose tests**

Cover these semantics and exact visible strings:

```text
Kitsune Sweep
Storage, without the scare tactics.
Scan large files
Find exact duplicates
Review apps
Samsung storage tools
All Files Access
Usage Access
Nothing over 100 MB
No exact duplicates found
Usage unknown
Clear cache in Android
```

Assert the delete action stays disabled with no selection. Assert selecting two files shows their count and combined size. Assert the confirmation sheet says exactly what will be deleted. Assert a 200 percent font scale keeps bottom navigation labels and the primary scan action visible.

- [ ] **Step 2: Run Compose tests and verify RED**

Run: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.kitsunetech.sweep.ui.SweepAppTest`

Expected: test compilation fails because `SweepApp` does not exist.

- [ ] **Step 3: Implement the theme and app shell**

Use Deep Current `#13252E`, Slate Bin `#20343E`, Cold Mint `#77D6B4`, Warning Clay `#F1A66A`, Mist `#B9C9CD`, and Paper `#ECF4F4`. Use condensed sans for large byte values, system sans for body copy, and monospace for paths. Use `NavigationBar` with Home, Files, Duplicates, and Apps. Every icon receives a text label and content description.

- [ ] **Step 4: Implement all four screens**

Home shows the storage-strata meter, permission cards, and system links. Files shows threshold chips, progress, sorted rows, selection, and deletion review. Duplicates shows reclaimable totals and groups without preselection. Apps shows sort chips, cache size, total size, last-used text, a cold marker only for qualifying user apps, and explicit Android settings actions.

- [ ] **Step 5: Implement deletion coordination**

MediaStore items create a system `PendingIntent` deletion request. Direct paths are revalidated by `FileSafetyPolicy` immediately before deletion to prevent time-of-check to time-of-use mistakes. Delete regular files one at a time, collect failures, and refresh after completion. Never recurse into a directory.

- [ ] **Step 6: Run Compose tests and verify GREEN**

Run: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.kitsunetech.sweep.ui.SweepAppTest`

Expected: all UI tests pass.

- [ ] **Step 7: Inspect rendered screens**

Capture Home, Files with results, Duplicates, Apps, and the delete-confirmation sheet from the emulator. Check clipping, contrast, selection clarity, bottom inset handling, 200 percent text, and landscape. Fix visible problems and rerun the UI tests.

- [ ] **Step 8: Commit the interface**

```text
git add app/src/main app/src/androidTest
git commit -m "feat: add Kitsune Sweep cleaner interface"
```

---

### Task 8: Full verification and APK packaging

**Files:**

- Create: `README.md`
- Create: `docs/testing/0.1.0-emulator-acceptance.md`
- Create: `scripts/verify.ps1`
- Modify only if verification finds a defect: files created in Tasks 1 through 7.

**Interfaces:**

- Produces: `scripts/verify.ps1` that runs unit tests, lint, instrumentation tests when an emulator is connected, and debug APK assembly with explicit exit checks.
- Produces: `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 1: Write the verification script**

The script sets `JAVA_HOME` to Android Studio's JBR when available, verifies API 36 exists, runs `testDebugUnitTest`, `lintDebug`, `connectedDebugAndroidTest` when `adb devices` reports an emulator, and runs `assembleDebug`. It exits nonzero on the first failed command and prints the final APK path only after every required command succeeds.

- [ ] **Step 2: Run all JVM tests**

Run: `./gradlew testDebugUnitTest`

Expected: zero failed tests.

- [ ] **Step 3: Run Android lint**

Run: `./gradlew lintDebug`

Expected: zero fatal or error findings.

- [ ] **Step 4: Run emulator tests**

Start the existing `glass` API 34 emulator in the background if no emulator is already running. Wait for `sys.boot_completed=1`. Run: `./gradlew connectedDebugAndroidTest`.

Expected: zero failed instrumentation tests.

- [ ] **Step 5: Perform emulator acceptance**

Verify denied permissions, granted Usage Access, permission return refresh, large-file thresholds, exact duplicate grouping, delete cancellation, app sort modes, app details link, storage settings link, process recreation, portrait, landscape, and 200 percent font scale. Record exact results in `docs/testing/0.1.0-emulator-acceptance.md`.

- [ ] **Step 6: Assemble and inspect the APK**

Run: `./gradlew clean assembleDebug`.

Use `apkanalyzer manifest permissions` or `aapt dump permissions` to confirm the APK does not contain Internet, contacts, location, microphone, camera, notification, or accessibility permissions. Record SHA-256 and file size.

- [ ] **Step 7: Run a focused security review**

Check path normalization, symbolic-link refusal, time-of-check to time-of-use revalidation, package-name validation, exception handling, permission denial, manifest permissions, external intent resolution, and absence of secrets or network code. Fix every critical or important issue and rerun Steps 2 through 6.

- [ ] **Step 8: Commit the release candidate**

```text
git add README.md docs/testing scripts app
git commit -m "test: verify Kitsune Sweep 0.1.0 APK"
```

- [ ] **Step 9: Prepare the handoff**

Copy the verified APK to a user-selected output folder without opening Explorer or stealing focus. Do not install it on a physical phone until the device owner explicitly asks for installation.
