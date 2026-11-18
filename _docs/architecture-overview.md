# ECHO Android: architecture overview

The map the agent follows when changing code. Written 2026-11-18 at `201396e` from the
code graph (`code-review-graph`, 165 files, 981 nodes) and the entry points. Marked
**target** where the code doesn't enforce a rule yet. The server side lives in another
repo: see [backend.md](./backend.md).

## What the app is

ECHO is a real-time chat app (one Gradle module, `:app`, package `com.shoonya.echo`,
applicationId `com.Shoonya.Echo`, minSdk 34, targetSdk 36, compileSdk 37). Users sign up
or log in, manage contacts (requests, favourites, blocking, suggestions), open direct
chats and message in real time over a WebSocket, and edit their profile and theme.

## Overview diagram

```text
 MainActivity ─► EchoNavHost (MainNavigation.kt) ─► Screen (Compose)
                                                      │ collects StateFlow
                                                      ▼
                                                  ViewModel (@HiltViewModel)
                                                      │ calls
                                                      ▼
                                                  UseCase (domain/usecase)
                                                      │ calls interface
                                                      ▼
                         domain/repository ◄─implements─ data/repository/*Impl
                                                      │
                     ┌────────────── REST ────────────┴──── WebSocket ─────────┐
                     ▼                                                         ▼
       Retrofit *ApiService ─► OkHttp @RestClient           EchoWebSocketClient ─► OkHttp @WsClient
       (+ AuthInterceptor)        │                         (+ ReconnectManager)        │
                                  └──────► ECHO server /echo/v1/  ◄─────────────────────┘
```

## Modules

All under `app/src/main/java/com/shoonya/echo/`.

| Module | Path | Responsibility | Owns state? |
|---|---|---|---|
| App shell | `EchoApplication.kt`, `MainActivity.kt`, `MainNavigation.kt` | Hilt app, theme, nav graph (`Routes`), session-expired → Login | — |
| core/data | `core/data/{local,remote,model}` | `SecureTokenStore` (JWT + user id), `NetworkModule` (Retrofit/OkHttp/Json), `AuthInterceptor`, `SessionManager`, `ApiResponse` envelope | JWT in EncryptedSharedPreferences |
| core/presentation, theme, util | `core/presentation`, `core/theme`, `core/util` | `SafeScreen`, avatars, `EchoTheme`, `ThemeStore` (dark-mode pref), `ErrorMapper`, `DateFormatter` | theme pref in SharedPreferences |
| auth | `features/auth` | Splash (token check), login + signup (one screen), logout | — |
| chat | `features/chat` | Chat list, chat detail, REST history, WebSocket client, commands and events | `ChatCache` (in memory) |
| contacts | `features/contacts` | Contacts, requests, favourites, blocking, suggestions, user profile | — |
| settings | `features/settings` | Profile view/edit, delete account, theme, logout | — |
| home | `features/home` | Bottom-nav host: Chats · Contacts · Settings; forwards cross-feature events | — |

Each feature uses `data/{di,model,remote,repository}` → `domain/{model,repository,usecase}`
→ `presentation/`. Hilt modules live in `data/di`.

## Layers and allowed imports

| From \ To | presentation | domain | data | core |
|---|---|---|---|---|
| **presentation** | ✅ | ✅ | ❌ | ✅ (not `core/data`) |
| **domain** | ❌ | ✅ | ❌ | ✅ `core/domain` only |
| **data** | ❌ | ✅ | ✅ | ✅ (not `core/presentation`) |

Also: `domain/` stays plain Kotlin (no `android`, `androidx`, `retrofit2`, `okhttp3`
imports); `core/` never imports `features/`; only `features/home` may import another
feature's screens. Verified 2026-11-18: zero violations. **Enforced** by
`node scripts/check-architecture.mjs` (part of verify and pre-commit); its messages say how
to fix each violation.

Cross-feature imports (all others stay inside their feature):

- `auth/data/repository/AuthRepositoryImpl.kt:6` → `chat.data.local.ChatCache` (clears it on logout)
- `chat/presentation/chatlist/ChatListScreen.kt:59` → `contacts.domain.model.Contact`
- `settings/presentation/SettingsViewModel.kt:6` → `auth.domain.usecase.LogoutUseCase`
- `home/` imports chat, contacts and settings screens (it is the host)

Rule: a feature may import another feature's `domain/` only, never its `data/`. The
`AuthRepositoryImpl → ChatCache` link is the one listed exception (`EXCEPTIONS` in the script).

## Main flows

1. **Start-up and session**: `SplashScreen`/`SplashViewModel` checks the stored token →
   Login or Home. `AuthInterceptor` adds `Bearer <jwt>`; a `401` on a request that carried a
   token calls `SessionManager.expire()` → `EchoNavHost` navigates to Login
   (`MainNavigation.kt:38`).
2. **Login / signup**: `LoginScreen` → `LoginViewModel` → `Login/SignupUseCase` →
   `AuthRepositoryImpl` → `POST login` / `POST signup` → token + user id saved in
   `SecureTokenStore`.
3. **Chat list**: there is no "list my chats" REST endpoint, so `ChatRepositoryImpl.getChatList`
   calls `POST chats/direct/{contactId}` **for every contact**, then `GET messages/{id}/messages?limit=1`
   for each preview (N+1 calls). `ChatListViewModel` also opens the WebSocket.
4. **Chat detail (real time)**: route `chat/{chatId}` (or `chat/new:{userId}` → create the
   direct chat first). `ChatDetailViewModel` loads history over REST, then `connect()` →
   `join_chat`; sends `send_message`, `set_typing`, `mark_read`, reactions as
   `WebSocketCommand`s with a `requestId`; `WebSocketRepositoryImpl` matches
   `response`/`error` frames to the pending request (10 s timeout) and turns broadcasts
   (`message`, `typing`, `presence`, `reaction`, `delivery`, `join`, `leave`) into `DomainEvent`s.
5. **Reconnect**: on close or failure, `ReconnectManager` retries 5 times with exponential
   backoff + jitter (1 s, 2 s, 4 s, …), then reports `FAILED`.
6. **Contacts and profile**: plain REST through `ContactsRepositoryImpl`, `SettingsRepositoryImpl`.

## Data and state

- No database. Everything comes from the server on each launch.
- `SecureTokenStore`: JWT and current user id in `EncryptedSharedPreferences`
  (`echo_secure_prefs`, AES-256, Android Keystore master key).
- `ThemeStore`: dark-mode preference in plain SharedPreferences.
- `ChatCache`: in-memory map of chats created or discovered this session; cleared on logout.

## Secrets and security

- No API keys in the app. The only secret is the user's JWT (see above).
- `network_security_config.xml`: HTTPS only for the production host; cleartext allowed only for
  `10.0.2.2` and `localhost` (debug builds talk to a local server at `http://10.0.2.2:8080`).
- Base URLs come from `BuildConfig` (`app/build.gradle.kts`): release = production host,
  debug = emulator loopback.
- Debug builds log full HTTP bodies and headers (`HttpLoggingInterceptor.Level.BODY`, no
  `redactHeader("Authorization")`), so the JWT appears in debug logcat.

## Errors, logging, observability

- Repositories return `Result<T>`. `ApiResponse<T>.toResult()` maps `success=false` to
  `ApiException`. `ErrorMapper` turns HTTP codes and exceptions into user-facing text.
- Logging: Timber, `DebugTree` planted only in debug (`EchoApplication.kt`). Tags: `WS`,
  `ChatRepo`, `AuthInterceptor`, `SessionManager`, `<Screen>VM`.
- Agent access to logs: `adb logcat -s WS ChatRepo AuthInterceptor` on a debug build.

## Testing seams

- Repositories are interfaces. Fakes live in `app/src/test/java/com/shoonya/echo/fakes/`
  (`FakeAuthRepository`, `FakeChatRepository`, `FakeWebSocketRepository`, …) with `TestData.kt`.
- `UserIdProvider` (`core/domain`) hides `SecureTokenStore` from ViewModels.
- ViewModel tests use `kotlinx-coroutines-test` (`StandardTestDispatcher`).
- UI tests (`app/src/androidTest/.../ui/`) render the stateless `*Content` composables
  (`LoginContent`, `ChatListContent`, `ChatDetailContent`), so they need no backend or Hilt.

## Riskiest areas

| # | Where | Why |
|---|---|---|
| 1 | `features/chat/data/remote/EchoWebSocketClient.kt` `connect()` | Opens a new socket without closing an open one; called from both chat ViewModels |
| 2 | `features/chat/data/repository/WebSocketRepositoryImpl.kt` | Request/response matching, reconnect trigger on every close (incl. `4401`/`4403`) |
| 3 | `features/chat/data/repository/ChatRepositoryImpl.kt` `getChatList` | N+1 calls; calls *create* direct chat for each contact; only direct chats shown |
| 4 | `features/chat/presentation/chatdetail/ChatDetailViewModel.kt` (485 lines) | Largest ViewModel: history, live events, typing, reactions, reconnect state |
| 5 | `features/chat/presentation/chatdetail/ChatDetailScreen.kt` (629 lines) | Largest file |
| 6 | `core/data/remote/AuthInterceptor.kt` | Only `401` ends the session; new server codes (`403` banned/inactive) not handled |
| 7 | `core/data/local/SecureTokenStore.kt` + `res/xml/backup_rules.xml` | Encrypted prefs + default auto-backup rules |
| 8 | `features/chat/data/local/ChatCache.kt` | Unsynchronised mutable maps used from IO coroutines |
| 9 | `features/contacts/data/remote/ContactsApiService.kt` | Only the first page (`limit=50`) is ever loaded |
| 10 | `MainNavigation.kt` | All routes; `chat/new:{userId}` convention |

Details and issue drafts: [current-state.md](./current-state.md#known-issues-and-risks).
