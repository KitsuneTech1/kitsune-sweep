# Kitsune Sweep

Kitsune Sweep is a local Android storage review tool built for a Samsung Galaxy S25 Ultra. It finds large shared files, checks exact duplicates, ranks installed apps by storage and last use, and opens Android's own storage and cache controls.

It does not claim that ordinary cache is junk. It does not kill apps, boost RAM, clean private app data, or delete anything automatically.

## What version 0.1 does

- Scans shared storage at 50 MB, 100 MB, 250 MB, 500 MB, or 1 GB thresholds.
- Requires All Files Access for complete Files and Duplicates scans on Android 11 and newer. The app does not present Android's partial no-access view as a complete scan.
- Finds exact duplicates with size grouping followed by streaming SHA-256 hashes.
- Shows app code, data, cache, and last-use facts when Usage Access is available.
- Marks non-system apps cold only after at least 90 days without recorded use.
- Opens Samsung or Android storage tools, Android's cache-clearing confirmation, app details, and uninstall screens.
- Requires an exact count and byte total before any file deletion request.

## Privacy and permissions

The app has no Internet permission, ads, account, analytics, or telemetry.

It declares three permissions:

- `MANAGE_EXTERNAL_STORAGE` for complete shared-storage scans and direct deletion on Android 11 and newer.
- `PACKAGE_USAGE_STATS` for optional app storage and last-use facts.
- `QUERY_ALL_PACKAGES` because installed-app review is a core feature of this personal sideload build.

Permission denial leaves Home and Apps usable. Files and Duplicates explain that shared-file review is unavailable until access is granted. Android 10 and older can use the dashboard and app review, but shared-file cleaning requires Android 11 or newer. The app never requests contacts, location, camera, microphone, notifications, or accessibility access.

## Deletion safety

Nothing is selected by default. Direct files are checked against the allowed shared-storage root again immediately before deletion. The checked file identity, byte size, and modified time must still match what was reviewed. Directories, symbolic links, the storage root, the top-level `Android` tree, replaced files, and paths outside the allowed root are refused. MediaStore files use Android's own confirmation dialog. After deletion, Kitsune Sweep verifies each requested item and lists every path or URI that remains.

## Build and verification

The project uses Gradle 9.1.0, Android Gradle Plugin 9.0.1, Kotlin 2.2.10, compile SDK 36, and Jetpack Compose. The build is capped at one worker and a 1.5 GB JVM heap to avoid hammering the host machine.

Run `scripts/verify.ps1` from PowerShell for unit tests, lint, connected emulator tests when exactly one emulator is present, and debug APK assembly. The script refuses connected tests if any physical Android device is attached. The verified artifact is produced at `app/build/outputs/apk/debug/app-debug.apk`.

The 0.1.0 debug APK was installed on Moo's S25 Ultra after USB debugging was explicitly connected. Physical acceptance covered permission refresh, shared-file scanning, exact duplicate hashing, app inventory, and the anchored-deletion capability check. No deletion or uninstall action was confirmed.

## 0.1.0 debug release evidence

The headless glass API 34 verification pass completed on 2026-08-14: 44 JVM tests, 14 instrumentation tests, and Android lint all passed. The first clean corrected build produced a 29,624,587-byte APK with SHA-256 `6068DD06E86A10BDBFC45BD231D0FD7BF42458EEEAE5006F7B74D419EA1C9B98`.

Manifest inspection found exactly the three documented special-access permissions. The emulator acceptance record is in `docs/testing/0.1.0-emulator-acceptance.md`; it records the remaining gap: live MediaStore approval and cancellation were unavailable from the headless provider, while the Compose confirmation and remaining-path result flows passed.

The physical S25 pass checked 16,418 shared files, found 68 files above the 100 MB threshold, hashed 8 duplicate candidates, and reviewed 778 installed apps without an error. The S25 shared-storage provider passed the required `SecureDirectoryStream` assertion. Live deletion approval and cancellation remain intentionally untested; nothing was selected or deleted.
