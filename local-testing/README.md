# Local testing: two users on two emulators

ECHO is a real-time chat app: chat, presence, contacts and unread counts only show
themselves when **two users are online at once**. This folder runs two (or more) emulator
instances side by side against your local Go backend, so you can be user A and user B at
the same time.

- Package `com.Shoonya.Echo`, launcher activity `com.shoonya.echo.MainActivity`
- Debug build calls `http://10.0.2.2:8080` and `ws://10.0.2.2:8080`
  (`10.0.2.2` is your host's `localhost` as seen from an emulator)
- API contract: [`_docs/backend.md`](../_docs/backend.md)

## What you need

| Requirement | Notes |
|---|---|
| Windows 10/11 with PowerShell 5.1 | The launcher is `emulators.ps1`. macOS/Linux: [manual commands](#macos--linux). |
| Android SDK and one working AVD | Android Studio → Device Manager. Opening this project once writes `local.properties` with `sdk.dir`; the script reads it. |
| Hardware acceleration | `<sdk>\emulator\emulator.exe -accel-check` must say "is installed and usable" (WHPX on Windows). |
| The ECHO Go backend on port 8080 | Separate repo `shoonya0/ECHO`. Start it before logging in; without it the login screen fails. |
| Free RAM | ~2 GB per emulator on top of Gradle and the IDE. Two instances on a 16 GB machine is fine. |
| Free disk | A cloned AVD costs a few KB at creation and grows as it is used (a few GB over time). |

## First run

```powershell
# 1. Start the ECHO backend in its own terminal (see that repo's README);
#    it must listen on :8080.

# 2. Build the debug APK (the first build downloads dependencies).
.\gradlew.bat :app:assembleDebug

# 3. Start both users, install the APK, launch the app.
.\local-testing\emulators.ps1 -Install -Launch
```

The script prints a table like this — these serials are all you need from here on:

```
Role  AvdName        Serial         AdbPort State   Pid
userA Pixel_5-userA  emulator-5554  5555    started 12345
userB Pixel_5-userB  emulator-5556  5557    started 23456
```

Then: create account **A** on `emulator-5554`, account **B** on `emulator-5556`, add each
other as contacts, open the chat and send messages both ways.

If your only AVD is not called `Pixel_5`, pass `-TemplateAvd "<name>"` — or keep exactly
one AVD in Device Manager and the script picks it automatically. The **first** start of a
cloned AVD is a cold boot (~60–90 s); later starts reuse the quickboot snapshot and take
seconds.

## Everyday commands

| Goal | Command |
|---|---|
| Start / re-attach, install, launch | `.\local-testing\emulators.ps1 -Install -Launch` |
| One AVD twice with `-read-only` (no extra disk) | `.\local-testing\emulators.ps1 -Mode ReadOnly -Install` |
| Cheapest two devices (720×1560, no windows) | `.\local-testing\emulators.ps1 -Lite -Headless` |
| Look at a headless instance | `scrcpy --serial emulator-5556` |
| Type into one device | `adb -s emulator-5556 shell input text "hello"` |
| Screenshot (binary-safe) | `cmd /c "adb -s emulator-5556 exec-out screencap -p > _docs\evidence\userB.png"` |
| App logs for one device | `adb -s emulator-5554 logcat -s WS ChatRepo AuthInterceptor SessionManager AndroidRuntime:E` |
| UI tree / tap | `adb -s emulator-5554 shell uiautomator dump /sdcard/ui.xml`, then `adb -s emulator-5554 exec-out cat /sdcard/ui.xml` |
| Device tests against ONE user | `$env:ANDROID_SERIAL='emulator-5556'; .\gradlew.bat --stop; .\gradlew.bat :app:connectedDebugAndroidTest` |
| Stop everything | `.\local-testing\emulators.ps1 -Stop` |

The script never edits the app or its Gradle configuration; it starts emulators and
optionally installs the existing debug APK with `adb -s <serial> install -r -t`.

`-Mode ReadOnly` starts the instances **one at a time**: two `-read-only` instances of the
same AVD started together race on its shared files and one can hang before it opens its
adb port. `Clone` mode uses one AVD per user, so it starts them in parallel.

If an instance hangs that way anyway, `-Stop` still gets it: besides `adb emu kill` it
scans the emulator processes and force-kills any of this script's AVDs that never reached
adb.

## Rules that bite when two devices are attached

- **`adb` picks the first device.** `adb shell …` with two devices attached targets
  `emulator-5554` and silently does something you did not intend. Always pass
  `-s <serial>`; the script prints the exact commands for each user.
- **`connectedDebugAndroidTest` runs on every attached device.** Pin one:
  `$env:ANDROID_SERIAL = 'emulator-5556'`, then `.\gradlew.bat --stop` (the daemon caches
  the environment it was started with), then run the task. `installDebug` without
  `ANDROID_SERIAL` intentionally installs on both.
- **`adb shell input text` cannot type spaces** — use `%s`:
  `adb -s emulator-5556 shell input text "hi%sfrom%sB"`.
- **PowerShell 5.1 `>` corrupts binary output** (it writes UTF-16 with a BOM). For
  screenshots and any `exec-out` binary use `cmd /c "adb … > file.png"`, or run the
  command in PowerShell 7 (`pwsh`).
- **In Git Bash, `export MSYS_NO_PATHCONV=1` first**, or device paths such as
  `/sdcard/ui.xml` are rewritten to `C:/Program Files/Git/...` and the dump goes nowhere.

## Choosing a lighter device

Two instances cost roughly twice one instance, so make each one cheap. The launcher already
disables the boot animation and audio, and turns device animations off after boot.

| Want | Flag / action | Effect |
|---|---|---|
| Use the real GPU instead of software rendering | `-Gpu angle_indirect` (default) or `-Gpu host` | Biggest CPU win on Windows; a discrete GPU handles both instances |
| Fewer pixels | `-Lite` (new clones only) | 720×1560 @293 dpi: same dp size as Pixel 5, 2.25× fewer pixels |
| Less RAM per instance | `-MemoryMb 1536` | More headroom for Gradle and the IDE |
| Fewer emulated cores | `-Cores 2` (default) | Leaves host cores for the build |
| No windows at all | `-Headless`, then `scrcpy` on demand | Removes host UI compositing |
| A lighter system image | SDK Manager → Android 14 (API 34) `google_apis` x86_64 → new AVD, then `-TemplateAvd "<name>"` | Faster boot, lighter than the API 37.1 16 KB-page image; `minSdk` is 34, so the app runs |
| The smallest CI-style image | `sdkmanager "system-images;android-34;aosp_atd;x86_64"` (needs Android SDK Command-line Tools) | Google's test-optimised image; no Play services, which ECHO does not use |

Notes:

- A **Play Store** image is unnecessary: no dependency here uses Google Play services.
- The current `Pixel_5` AVD uses a **16 KB page-size** image (`google_apis_ps16k`). That only
  matters if native libraries are added later; it is not needed for UI/chat testing.
- `-Lite` writes settings into the AVD when it is created. To re-create a clone with
  different geometry, remove the AVD in Device Manager and run the script again.
- Do not go below API 34: `minSdk` in `app/build.gradle.kts` is 34.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| Only one emulator window appears | The other instance was started with `-no-window` | Drop `-Headless`, or use `scrcpy --serial emulator-5556`. Both are still in `adb devices`. |
| `emulator exited with code 1` | GPU/driver issue or port clash | Read `%TEMP%\echo-emulators\emulator-<port>.log`; retry with `-Gpu swiftshader_indirect` |
| `AVD is already running` | The same AVD was started twice without `-read-only` | Use the default Clone mode (one AVD per user) |
| `error: more than one device/emulator` | Missing `-s` | `adb -s emulator-5556 …` |
| Login fails, messages never arrive | Backend not running, or the wrong host | The debug build needs `localhost:8080` on the host: `Test-NetConnection 127.0.0.1 -Port 8080` |
| `ANDROID_SERIAL` seems ignored | The Gradle daemon kept the old environment | `.\gradlew.bat --stop`, then rerun the task |
| Host fans loud, everything janky | Software rendering and/or too many pixels | `-Gpu angle_indirect`, `-Lite`, close other emulators |
| Boot never completes | Stuck snapshot or too little RAM | `-ColdBoot`, then `-MemoryMb 2048`; check the log |
| An instance never shows up in `adb devices` | It hung before opening its console port (two simultaneous `-read-only` boots of the same AVD) | `-Mode ReadOnly` now starts them sequentially; rerun `-Stop`, which also force-kills the hung process |
| Script reports a missing template AVD | Several AVDs exist and none is named `Pixel_5` | `.\local-testing\emulators.ps1 -TemplateAvd "<your avd>"` |
| `Running scripts is disabled on this system` | Execution policy | Run once: `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` |

## Clean up

```powershell
.\local-testing\emulators.ps1 -Stop     # kills the instances by AVD name
adb devices                            # should be empty afterwards
```

Cloned AVDs live in `%USERPROFILE%\.android\avd\<template>-userA.avd` (and `-userB`, …).
Remove them in Device Manager, or delete those folders while the emulators are stopped.
`-Mode ReadOnly` leaves nothing behind at all.

## macOS / Linux

`emulators.ps1` is Windows-only. The equivalent by hand (create the second AVD once in
Device Manager, then):

```bash
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
"$SDK/emulator/emulator" -avd Pixel_5   -port 5554 -gpu host -no-boot-anim -no-audio &
"$SDK/emulator/emulator" -avd Pixel_5-B -port 5556 -gpu host -no-boot-anim -no-audio &
"$SDK/platform-tools/adb" -s emulator-5554 wait-for-device
"$SDK/platform-tools/adb" -s emulator-5556 wait-for-device
"$SDK/platform-tools/adb" -s emulator-5554 install -r -t app/build/outputs/apk/debug/app-debug.apk
"$SDK/platform-tools/adb" -s emulator-5556 install -r -t app/build/outputs/apk/debug/app-debug.apk
"$SDK/platform-tools/adb" -s emulator-5554 shell am start -n com.Shoonya.Echo/com.shoonya.echo.MainActivity
"$SDK/platform-tools/adb" -s emulator-5556 shell am start -n com.Shoonya.Echo/com.shoonya.echo.MainActivity
```

On Linux, `-gpu host` needs a working GL driver; if the emulator exits immediately, use
`-gpu swiftshader_indirect`.

