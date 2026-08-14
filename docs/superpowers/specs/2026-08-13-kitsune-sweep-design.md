# Kitsune Sweep design

## Decision

Build a focused personal Android cleaner named **Kitsune Sweep** for Moo's Samsung Galaxy S25 Ultra. The first release is a sideloaded APK. It stays local, contains no ads or telemetry, and never pretends Android grants it powers it does not have.

Package name: `com.kitsunetech.sweep`

Version: `0.1.0`

Minimum Android version: Android 9, API 28

Compile and target SDK: API 36

Primary test target: Pixel 6 API 34 emulator

Hardware acceptance target: Samsung Galaxy S25 Ultra

## Adopt, fork, or build

### Adopt SD Maid SE

SD Maid SE already has storage analysis, duplicate detection, app control, remnant cleanup, and optional cache automation. It is the fastest way to get a general-purpose cleaner today. It does not provide the focused Kitsune interface, honest cache explanation, or unused-app review Moo asked for.

### Fork SD Maid SE

This would inherit years of Android edge-case handling. It would also inherit a large GPL-3.0 codebase with billing, root, Shizuku, accessibility automation, translations, and upstream maintenance. Any distributed fork would need to follow GPL source-distribution requirements. That is too much surface area for this personal utility.

### Build Kitsune Sweep

This is the selected approach. It produces a smaller app with no network permission, no account, no billing, and no unrelated cleaner features. Version 0.1 will use ordinary Android APIs and user-confirmed system screens. It will not include root, Shizuku, or accessibility automation.

## Product boundary

Kitsune Sweep helps Moo understand storage, find large or repeated files, identify large apps he no longer uses, and reach Samsung or Android cleanup controls quickly.

The app does not promise performance gains from clearing cache. It does not call normal app cache dangerous. It does not kill background apps, boost RAM, cool the CPU, optimize the battery, or show invented junk totals.

## Version 0.1 features

### Storage dashboard

- Show total, used, and free internal storage.
- Display a storage-strata graphic whose sections reflect files, app storage, cache, and free space when those values are available.
- Show when All Files Access or Usage Access is missing.
- Keep useful read-only functions available when either permission is denied.
- Provide one direct action for Android or Samsung storage settings.

### Large-file review

- Scan shared internal storage without following symbolic links.
- Use MediaStore when All Files Access is unavailable.
- Use direct shared-storage traversal after Moo grants All Files Access.
- Default to files at least 100 MB and allow 50 MB, 100 MB, 250 MB, 500 MB, and 1 GB filters.
- Show name, path, type, size, and modified date.
- Sort by size descending by default.
- Never scan private app data or claim to see `Android/data` when Android blocks it.
- Exclude the app's own files and known protected system paths from deletion.

### Exact duplicate detection

- Group files by byte size first.
- Hash only size groups with more than one candidate.
- Use SHA-256 for the final exact-match decision.
- Never call similar photos duplicates in version 0.1.
- Do not select a copy for deletion automatically.

### App and cache review

- Read the installed app list after package visibility is granted by the manifest.
- Read total app size and cache size through `StorageStatsManager` after Moo grants Usage Access.
- Read last-used timestamps through `UsageStatsManager`.
- Rank by total size, cache size, or oldest use.
- Mark a user-installed app as cold only when its last recorded use is at least 90 days old.
- Show `Usage unknown` when Android provides no trustworthy timestamp.
- Never mark system apps as uninstall suggestions.
- Open the selected app's Android settings page for cache or data controls.
- Open the user-confirmed Android external-cache clearing dialog when supported.
- Open Android's storage-management screen, which routes into Samsung storage tools on the S25 Ultra.

### File deletion safety

- No automatic deletion.
- No item is selected by default.
- Show the exact file count and total bytes before deletion.
- Require a final confirmation in the app or Android's MediaStore confirmation dialog.
- Refuse to delete directories, symbolic links, app-private paths, or protected roots.
- Refresh the scan after Android confirms or completes deletion.
- Report partial failure with the number deleted and the paths that remain.

## Permissions

`MANAGE_EXTERNAL_STORAGE` is optional and requested only from the large-file screen. It enables direct scanning of shared storage but still does not grant private app-data access.

`PACKAGE_USAGE_STATS` is optional and granted from Android Settings. It enables app sizes, cache sizes, and last-used dates.

`QUERY_ALL_PACKAGES` is declared because installed-app analysis is a core function of the personal sideload build.

The app must not declare `INTERNET`, contacts, location, microphone, camera, notification, or accessibility permissions.

## Architecture

The app is a single Android application module written in Kotlin. Jetpack Compose renders the interface. A single activity owns navigation and Android activity-result contracts.

Pure domain functions handle byte formatting, cold-app classification, file safety checks, and duplicate grouping. Android repositories wrap `StorageManager`, `MediaStore`, `StorageStatsManager`, `UsageStatsManager`, and `PackageManager`. View models run scans on `Dispatchers.IO`, expose immutable screen state, and cancel obsolete scans when the user changes filters or leaves a screen.

The first release uses four destinations:

- Home: storage summary, permission status, and system cleanup links.
- Files: large-file scan, filters, selection, and deletion review.
- Duplicates: exact duplicate groups and manual selection.
- Apps: size, cache, last-used data, and system action links.

## Data flow

1. The activity reads permission state without prompting.
2. A view model starts the requested scan only after a user action.
3. The repository emits progress and typed results from a background dispatcher.
4. The view model merges results into immutable UI state.
5. Compose renders loading, data, empty, permission, or partial-error states.
6. Destructive actions return to an explicit review sheet before any system request or direct deletion.
7. A completed action triggers a fresh scan instead of guessing how much space changed.

## Error handling

- Permission denial leaves the rest of the app usable and explains which result is unavailable.
- A file disappearing during a scan is skipped and counted, not treated as a fatal scan failure.
- A package disappearing during app analysis is skipped.
- Storage-stat failures display `Size unavailable` for that app.
- Cancellation stops traversal and hashing promptly.
- Out-of-memory risk is limited by streaming directory entries and file hashes instead of loading file contents into memory.
- UI errors state what failed and give one useful next action.

## Visual direction

Kitsune Sweep looks like a storage instrument, not an antivirus ad. The palette uses Deep Current `#13252E`, Slate Bin `#20343E`, Cold Mint `#77D6B4`, Warning Clay `#F1A66A`, Mist `#B9C9CD`, and Paper `#ECF4F4`.

Large storage numbers use Android's condensed sans typeface. Body copy uses the system sans typeface. File sizes and paths use the system monospace face.

The signature element is a storage-strata meter. Its stacked bands correspond to real categories and preserve labels at large font sizes. Screens use quiet surfaces, strong spacing, and one clear primary action. Nothing pulses, spins indefinitely, or flashes a fake warning.

All controls have at least a 48 dp touch target. Content supports screen readers, large text, keyboard navigation, and reduced motion. Information never depends on color alone.

## Testing

- Pure JVM tests cover formatting, cold-app classification, duplicate grouping, protected-path rules, and delete-plan totals.
- Repository tests use fakes around Android data sources.
- Compose tests cover permission cards, scan states, filters, selection totals, confirmation copy, and large-font layout.
- Lint, unit tests, instrumentation tests, and debug APK assembly run before an APK is handed off.
- Emulator acceptance covers denied permissions, granted Usage Access, empty results, sample large files, exact duplicates, deletion cancellation, and process recreation.
- S25 Ultra acceptance remains separate because no phone is currently connected. Installation requires Moo's explicit request once USB debugging is available.

## Deferred work

- Similar-photo detection
- User-defined protected folders
- Storage-history trends
- Scheduled scans
- Shizuku or root support
- Accessibility-based cache automation
- Play Store flavor
- Release signing and public distribution

These features are outside version 0.1 so the first APK remains small, understandable, and testable.
