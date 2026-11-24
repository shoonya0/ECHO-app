# Increment log

Newest first. Started 2026-11-21: increments closed before that date are recorded only in
`_docs/current-state.md` and in the PR list.

## 2026-11-24 - one ContactsViewModel for Home, profile and requests (#44)

- **Problem:** opening a user profile or the contact-requests screen created a second
  `ContactsViewModel`, whose `init` reloaded all five contact lists; returning Home then
  reloaded all five again through its resume refresh. One visit cost up to 10 extra
  requests, and because those screens kept their own state, Home only saw an accepted
  request through that refresh.
- **Change:** `MainNavigation` now hands Home's `ContactsViewModel` (scoped to the `home`
  back stack entry) to `UserProfileScreen` and `ContactRequestsScreen`, so opening them
  loads only what they show (the profile: one `GET users/{id}`) and their actions update
  Home directly. `ContactsViewModel.ScreenResumed` refreshes only the lists that can change
  server-side: contacts (a peer accepts/removes/blocks; presence moves) and the two request
  lists; favorites and blocked users are not re-fetched and are kept from this ViewModel's
  own actions. Because the profile
  request now outlives the screen, `loadUserProfile` cancels the previous request and
  ignores a cancelled reply, so a late answer cannot replace the profile opened next; it
  also clears the previous user first, so a failed load cannot leave someone else's profile
  on screen.
- **Tests:** `ContactsViewModelTest` pins the new resume behaviour (renamed from "reloads
  contacts once"), plus a favorite-flag-kept-on-resume test, a failed-refresh test, a
  profile-load-does-not-reload-lists test, a stale-profile-cleared test and a late-reply
  race test (verified to fail with the cancellation guard removed);
  `FakeContactsRepository` now counts profile calls and can hold a profile reply open.
  `node scripts/verify.mjs`: 261 unit tests, 0 failures (was 256).
- **Evidence:** `node scripts/verify.mjs --full` passed (8 steps: docs-check, architecture,
  ktlint on changed files, compile + unit tests, Android lint, UI-test compile, debug APK;
  device UI tests skipped — no emulator attached). Not run on the emulator: no device and no
  local backend were up this session, so the request counts are proven by unit tests and
  code, not by a server log.
- **Commit:** `<pending>`

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
