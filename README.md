# Kitsune Sweep

Kitsune Sweep is a local Android storage review tool built for a Samsung Galaxy S25 Ultra. It finds large shared files, checks exact duplicates, ranks installed apps by storage and last use, and opens Android's own storage and cache controls.

It does not claim that ordinary cache is junk. It does not kill apps, boost RAM, clean private app data, or delete anything automatically.

## What version 0.1 does

- Scans shared storage at 50 MB, 100 MB, 250 MB, 500 MB, or 1 GB thresholds.
- Uses MediaStore without All Files Access and direct shared-storage traversal when that access is granted.
- Finds exact duplicates with size grouping followed by streaming SHA-256 hashes.
- Shows app code, data, cache, and last-use facts when Usage Access is available.
- Marks non-system apps cold only after at least 90 days without recorded use.
- Opens Samsung or Android storage tools, Android's cache-clearing confirmation, app details, and uninstall screens.
- Requires an exact count and byte total before any file deletion request.

## Privacy and permissions

The app has no Internet permission, ads, account, analytics, or telemetry.

It declares three permissions:

- `MANAGE_EXTERNAL_STORAGE` for optional direct shared-storage scans.
- `PACKAGE_USAGE_STATS` for optional app storage and last-use facts.
- `QUERY_ALL_PACKAGES` because installed-app review is a core feature of this personal sideload build.

Permission denial leaves the other screens usable. The app never requests contacts, location, camera, microphone, notifications, or accessibility access.

## Deletion safety

Nothing is selected by default. Direct files are checked against the allowed shared-storage root again immediately before deletion. Directories, symbolic links, the storage root, the top-level `Android` tree, and paths outside the allowed root are refused. MediaStore files use Android's own confirmation dialog.

## Build and verification

The project uses Gradle 9.1.0, Android Gradle Plugin 9.0.1, Kotlin 2.2.10, compile SDK 36, and Jetpack Compose. The build is capped at one worker and a 1.5 GB JVM heap to avoid hammering the host machine.

Run `scripts/verify.ps1` from PowerShell for unit tests, lint, connected emulator tests when an emulator is present, and debug APK assembly. The verified artifact is produced at `app/build/outputs/apk/debug/app-debug.apk`.

The hardware acceptance pass on the S25 Ultra is separate. The app should not be installed on the phone until USB debugging is connected and the owner explicitly requests installation.

## 0.1.0 debug release evidence

The headless glass API 34 verification pass completed on 2026-08-14: 36 JVM tests, 12 instrumentation tests, and Android lint all passed. `clean assembleDebug` produced a 29,591,819-byte APK with SHA-256 `30E6997252C9590D0B785519A84BA7ACAE58B37A193274FF3137BEA6C55A20B5`.

Manifest inspection found exactly the three documented special-access permissions. The emulator acceptance record is in `docs/testing/0.1.0-emulator-acceptance.md`; it records the one gap: live MediaStore cancellation was unavailable from the headless provider, while the Compose confirmation flow passed.
