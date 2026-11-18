---
name: verify-change
description: Run the project's verification ladder (typecheck, lint, unit, and when relevant integration, e2e, agent UI check, build) and report evidence in a fixed format. Use before claiming any change is done, after implementing a plan step, or when the user asks "does it work?".
---

# Verify change

Prove the current change works. Report **evidence, not claims**.

## 1. Scope

- `git status` and `git diff --stat`: what changed?
- Choose the rungs needed. Rungs 1 to 3 always run. Add the others when the change
  touches them:

| Rung | Command | Run when |
|---|---|---|
| 1-3 Fast | `node scripts/verify.mjs` (docs-check, architecture, ktlint on changed files, compile + unit tests) | Always |
| 4 Lint + build | `node scripts/verify.mjs --full` (adds Android lint, UI-test compile, debug APK, device tests if an emulator is up) | Gradle/manifest/resource changes, new dependencies, before a PR |
| 5 UI tests | `./gradlew :app:connectedDebugAndroidTest` (emulator running) | A screen or `*Content` composable changed |
| 6 Agent UI | emulator + adb (below) | A user flow changed |
| 7 Release build | `./gradlew :app:assembleRelease` (R8 minify) | ProGuard/serialization/reflection changes |

Single unit test class: `./gradlew :app:testDebugUnitTest --tests "com.shoonya.echo.chat.ChatDetailViewModelTest"`.
`spotlessCheck` failed? Run `./gradlew spotlessApply` and re-run.

## 2. Run

- Run each chosen rung. On failure, find the **root cause** and fix it, then rerun
  from rung 1.
- Never delete, skip or weaken a test to get green. If a test is wrong, stop and
  explain why.
- Compare failures against the **baseline** in `_docs/current-state.md` by test name.
  A pre-existing failure is reported as such, with evidence. It is never silently
  ignored.

## 3. Agent UI check (rung 6)

Native Android: no browser automation. Drive the debug build on the `Pixel_5` emulator
with adb (`<sdk>` = `sdk.dir` in `local.properties`). Debug builds call the backend at
`http://10.0.2.2:8080`; anything past login needs it running (`_docs/backend.md`).
Start the emulator only with the user's OK. Tested at setup (2026-11-18, Pixel_5, API 37):
login screen dumped, "Sign Up" tapped, signup form confirmed (`_docs/evidence/setup-smoke-*.png`).

Traps: in Git Bash, `export MSYS_NO_PATHCONV=1` first, or device paths such as `/sdcard/ui.xml`
are rewritten to `C:/Program Files/Git/...` and the dump silently goes nowhere.
`connectedDebugAndroidTest` uninstalls the app afterwards: run `installDebug` again before
`am start`. Check the dump shows `package="com.Shoonya.Echo"`, not the launcher.

```text
<sdk>/emulator/emulator -avd Pixel_5 -no-snapshot-save     # background; or reuse a running one
<sdk>/platform-tools/adb wait-for-device
./gradlew :app:installDebug
adb shell am start -n com.Shoonya.Echo/com.shoonya.echo.MainActivity
adb shell uiautomator dump /sdcard/ui.xml && adb exec-out cat /sdcard/ui.xml   # UI tree: text, bounds
adb shell input tap <x> <y>  |  adb shell input text "hello"   # centre of a node's bounds
adb exec-out screencap -p > _docs/evidence/<feature>-<step>.png
adb logcat -d -s WS ChatRepo AuthInterceptor SessionManager AndroidRuntime:E
```

Compare against the spec or design and list the differences. Real-device behaviour
(keyboard, notifications, rotation, a physical network drop) needs a human check: list it
under "Requires physical verification".

## 4. Report (exact format)

```markdown
### Verification
| Rung | Command | Result | Notes |
|---|---|---|---|
| Fast | `node scripts/verify.mjs` | ✅ pass (95 s) | 113/113 unit |
| UI tests | `./gradlew :app:connectedDebugAndroidTest` | ✅ pass | 10/10 |
| UI | emulator + adb | ✅ | flow X ok; logcat clean; _docs/evidence/x-1.png |

- New failures vs baseline @<sha>: none
- Not verified: <list, or "nothing">
- Requires physical verification on <OS/device>: <list, or "nothing">
```

If any rung fails and can't be fixed in scope, stop and report it. Don't claim done.
