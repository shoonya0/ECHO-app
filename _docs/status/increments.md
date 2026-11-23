# Increment log

Newest first. Started 2026-11-21: increments closed before that date are recorded only in
`_docs/current-state.md` and in the PR list.

## 2026-11-21 - local two-user emulator kit

- **Problem:** chat, presence and contacts can only be verified with two accounts online,
  and driving a second emulator by hand kept failing: the second instance is invisible when
  started with `-no-window`, a bare `adb` command silently targets the first device, and two
  `-read-only` boots of the same AVD started together hang one of them before it opens its
  adb port.
- **Change:** `local-testing/emulators.ps1` starts 2-4 instances on fixed ports
  (`emulator-5554`, `emulator-5556`, ...), clones one AVD per user by default, reuses
  instances that are already online, waits for `sys.boot_completed` with a timeout, and
  installs and launches the debug APK on each. `local-testing/README.md` is the fresh-user
  guide: prerequisites, first run, the two-device traps, lighter-image tuning and a
  troubleshooting table. One row added to `_docs/ai-workflow/harness.md`. No app, Gradle or
  CI changes.
- **Tests:** none added (no app code touched); the script was exercised as recorded below.
- **Evidence:** cold start with `-Install -Launch` (userA `emulator-5554`, userB
  `emulator-5556`, both `topResumedActivity=com.Shoonya.Echo/...MainActivity`; both instances
  signed in against the local backend showed the same contacts
  (`_docs/evidence/multi-emu-5554-contacts.png`, `multi-emu-5556-contacts.png`)); a rerun
  reuses both instances instead of duplicating them; `-Mode ReadOnly` brings both online
  with `-read-only`; `-Stop` empties `adb devices` and force-kills a hung instance; an
  unknown `-TemplateAvd` exits 1 listing the available AVDs; a `cmd /c` screenshot is a
  valid PNG; `node scripts/verify.mjs` passed (4 steps).
- **Commit:** `368c6d7` (PR #52, merged in `f125211`)
