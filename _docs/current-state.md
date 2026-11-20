# ECHO Android: current state (snapshot 2026-11-18)

Snapshot taken at `201396e` ("app v1 - final") on `chore/ai-harness`. The working tree had
uncommitted edits from the user (release base URL → `echo-server.onrender.com` in
`app/build.gradle.kts` and `network_security_config.xml`, `.idea/*`); they don't change
the debug build the checks use. One page. Claims are **VERIFIED** (run or read in the
code) or **ASSUMED**.

## Active work

<!-- Read by the SessionStart hook (session-context.mjs): keep this section first, short,
and with a link to exactly one feature state.md. Updated by /close-increment and
/feature-docs update. -->

- **Feature:** fix [#10](https://github.com/shoonya0/ECHO-app/issues/10) on `fix/10-mark-read` (`cb4a77d` format, `4794e10` fix, `a6d7bb0` review fixes: one `mark_read` in flight until this user's `delivery` confirms it, retry on `RATE_LIMITED`/drop/timeout, throttle); verify passed, 143/143 unit tests; not pushed, no device check yet
- **Next step:** user reviews and merges [PR #14](https://github.com/shoonya0/ECHO-app/pull/14); then [#11](https://github.com/shoonya0/ECHO-app/issues/11), [#13](https://github.com/shoonya0/ECHO-app/issues/13), telling the backend it can stop writing `readBy` (#42), or the group invite screen (backend #5 step 2)
- **Updated:** 2026-11-20 at `a6d7bb0`

## What a user can do today

VERIFIED by reading the screens, ViewModels and API services. On the emulator (no backend
running) only the login screen and the switch to the signup form were checked
(`_docs/evidence/setup-smoke-login.png`, `setup-smoke-signup.png`).

- Sign up, log in, log out; the session ends (back to Login) on any `401` with a token.
- Contacts: list, incoming and sent requests (accept, decline), favourites, block and
  unblock, remove, suggestions, view a user's profile.
- Chats: a list of **direct** chats with each contact's last message; open a chat, load
  history (paged), send messages, typing indicator, read receipts, reactions, presence,
  all live over the WebSocket.
- Settings: view and edit profile, delete account, dark/light theme.
- **Not built:** groups and invite codes (plan phase 6), push notifications, offline
  storage (nothing is persisted except the token and the theme).

## How to build, run, test

- Needs: Android Studio's JBR as `JAVA_HOME` (Gradle picks its JDK 21 daemon toolchain),
  Android SDK (`local.properties` → `sdk.dir`), Node 20+ for the scripts.
- **Fast check, run before saying done: `node scripts/verify.mjs`** · full: `node scripts/verify.mjs --full`
- Single test class: `./gradlew :app:testDebugUnitTest --tests "com.shoonya.echo.chat.ChatDetailViewModelTest"`
- Build and install the debug app: `./gradlew :app:installDebug` (emulator `Pixel_5`).
  Debug builds call `http://10.0.2.2:8080`, so run the backend locally first
  ([backend.md](./backend.md)). Release builds call `https://echo-server.onrender.com`.
- Instrumented UI tests: `./gradlew :app:connectedDebugAndroidTest` (needs a running emulator).

## Test baseline (at `201396e`)

Run 2026-11-18 on Windows 11, JBR 25 + Gradle daemon JDK 21, Gradle 9.5.0, AGP 9.3.1.

| Command | Duration | Result | Known failures (by name) |
|---|---|---|---|
| `./gradlew :app:testDebugUnitTest` | 254 s (cold daemon, first compile) | ✅ 113/113, 0 skipped, 28 classes | none |
| `./gradlew :app:lintDebug` | 91 s | ✅ 0 errors, 55 warnings (25 `UnusedResources`, 10 `NewerVersionAvailable`, 5 `GradleDependency`, 5 `UseKtx`, 3 `ModifierParameter`, 7 others) | none |
| `./gradlew :app:assembleDebug` | 83 s | ✅ `app-debug.apk` built | none |
| `./gradlew :app:connectedDebugAndroidTest` | 146 s | ✅ 10/10 (4 classes) on the Pixel_5 AVD, Android 17 / API 37 | none |

Compare new runs against this list **by test name**.

## Open decisions

| # | Decision needed | Options | Owner | Blocking? |
|---|---|---|---|---|
| 1 | How the chat list finds chats | keep "create direct chat per contact" · ask the server for a "my chats" endpoint | user + backend | No |
| 2 | Groups (plan phase 6) | build next · later | user | No |

## Known issues and risks

VERIFIED = read in the code on 2026-11-18. None is reproduced on a device yet. Each one is a
candidate GitHub issue; filed ones link to it.

1. **Two WebSocket connections at once** ([#1](https://github.com/shoonya0/ECHO-app/issues/1); VERIFIED in code, effect ASSUMED):
   `EchoWebSocketClient.connect()` (`features/chat/data/remote/EchoWebSocketClient.kt:38`)
   opens a new socket without closing an open one, and both `ChatListViewModel` and
   `ChatDetailViewModel` call it. Likely result: duplicate incoming events and a leaked socket.
2. **Chat header without a name when opened from the list** ([#2](https://github.com/shoonya0/ECHO-app/issues/2); VERIFIED in code):
   `getChatList` stores only the id (`ChatRepositoryImpl.kt:37`, `addChat`), so
   `getChat()` fails for those chats and `loadChatMetadata` only logs the error.
3. **Chat list is N+1 and writes** (VERIFIED): for each contact it calls
   `POST chats/direct/{id}` (creates the chat if missing) and one message fetch
   (`ChatRepositoryImpl.kt:32-34`). Slow with many contacts; group chats never appear.
4. **Server changes not handled yet** (close codes: [#1](https://github.com/shoonya0/ECHO-app/issues/1); the rest: [#3](https://github.com/shoonya0/ECHO-app/issues/3); VERIFIED by grep; list in the backend's
   [`08_SERVER_CHANGES`](https://github.com/shoonya0/ECHO/blob/main/Android/_docs/08_SERVER_CHANGES/README.md)):
   - WebSocket close codes `4401`/`4403`: the app reconnects anyway
     (`WebSocketRepositoryImpl.kt:56,61`) instead of logging out.
   - `403 ACCOUNT_BANNED` / `ACCOUNT_INACTIVE`: not handled; only `401` ends the session
     (`AuthInterceptor.kt:27`). (`503 AUTH_UNAVAILABLE` correctly keeps the session.)
   - `RATE_LIMITED` errors: no retry or backoff; `set_typing` isn't debounced (ASSUMED).
   - Contact lists come in pages: the app reads only the first page (`limit = 50`,
     `ContactsApiService.kt:15-32`).
   - Message history: the server rejects `offset` with `400`, so history never loaded
     ([#7](https://github.com/shoonya0/ECHO-app/issues/7), fixed in PR #8).
   - Read state (backend #42): handled in PR #9 ("read by" from `readSeqs` and
     `delivery.readUpToSeq`); the backend can stop writing `readBy`.
   - The app never sent `mark_read` ([#10](https://github.com/shoonya0/ECHO-app/issues/10)):
     fixed on `fix/10-mark-read` (not merged yet).
   - Messages that arrive while history loads disappear
     ([#11](https://github.com/shoonya0/ECHO-app/issues/11)).
   - Missed messages (backend #43): messages keep their `seq` (used for read state), but the
     app never catches up with `afterSeq` after a reconnect or a skipped number.
   - Live `message` events carry `data.sender` (backend #50); the app still uses only the
     frame's `username`.
   - Group invites (backend #5): the server will soon refuse history and send to pending
     members. The app has no groups yet, so it needs an invite screen
     (`GET groups/pending`, accept/decline) before the server switches that on.
5. **Auto-backup of the encrypted prefs** ([#4](https://github.com/shoonya0/ECHO-app/issues/4); ASSUMED, known Android behaviour):
   `android:allowBackup="true"` with the default `backup_rules.xml` backs up
   `echo_secure_prefs` but not its Keystore key; after a restore to a new device,
   opening the prefs can throw at start-up.
6. **JWT in debug logs** ([#4](https://github.com/shoonya0/ECHO-app/issues/4); VERIFIED): `HttpLoggingInterceptor.Level.BODY`
   (`NetworkModule.kt:47`) logs the `Authorization` header in debug builds.
7. Minor, seen on the emulator (VERIFIED by screenshot, cause ASSUMED): in the dark theme the
   status-bar icons are dark on the dark background (`setup-smoke-login.png`); the app's
   theme and the edge-to-edge system-bar style probably disagree (`MainActivity.kt`).
8. **Chat screen's connection status never updates** ([#13](https://github.com/shoonya0/ECHO-app/issues/13); VERIFIED in code):
   `ChatDetailViewModel.connectAndJoin()` calls `collectWsEvents()`, which never returns, before
   `collectConnectionStatus()`, so the state stays `CONNECTING` and the banner likely never clears.
9. Minor: `ChatCache` uses unsynchronised collections from IO coroutines
   (`ChatCache.kt:9-10`); `ChatRepositoryImpl.sendMessage` is dead code with a stale
   "Phase 5" message (`:97`); `HomePlaceholder.kt` is unused; the WebSocket and data layers
   have no unit tests.

## Next task

1. **Safety net for the WebSocket layer** (chosen at setup instead of characterization
   tests now): give `EchoWebSocketClient` an interface (or use OkHttp `MockWebServer`),
   then pin today's behaviour of `WebSocketRepositoryImpl` and `ReconnectManager` with
   tests, including issue 1. Then fix issues 1, 2 and 4 one branch each.
