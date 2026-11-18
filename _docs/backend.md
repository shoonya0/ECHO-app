# ECHO Android: the backend

The server is a separate repo. This page says where it is, which parts the app uses,
and where the server's own docs answer contract questions. **The server's docs are the
source of truth for the API**; don't copy them here, link them.

| | |
|---|---|
| Repo | [`shoonya0/ECHO`](https://github.com/shoonya0/ECHO) (Go, Gin, MongoDB, Redis), active development |
| Local clone | `D:\projects\ECHO` (has its own CLAUDE.md, `_docs/` and AI harness, playbook `2026-10-14`) |
| Production host | `https://echo-server.onrender.com` (Render), set in `app/build.gradle.kts` `defaultConfig` |
| Debug host | `http://10.0.2.2:8080` = the dev machine's `localhost:8080`, seen from the Android emulator |
| API prefix | `/echo/v1/` (REST, `NetworkModule.kt`) · `/echo/v1/ws/chat` (WebSocket, `EchoWebSocketClient.kt`) |

## Where to look in the backend repo

| Question | Backend doc |
|---|---|
| Docs index | [`_docs/README.md`](https://github.com/shoonya0/ECHO/blob/main/_docs/README.md) |
| REST: base path, auth header, response envelope, error and token codes | [`_docs/rest-api/basics.md`](https://github.com/shoonya0/ECHO/blob/main/_docs/rest-api/basics.md) |
| REST: users, contacts / chats, messages / groups | [`_docs/rest-api/`](https://github.com/shoonya0/ECHO/blob/main/_docs/rest-api/README.md) |
| WebSocket protocol and close codes | [`_docs/WEBSOCKET_PROTOCOL.md`](https://github.com/shoonya0/ECHO/blob/main/_docs/WEBSOCKET_PROTOCOL.md) |
| Every request / every event | [`websocket-requests.md`](https://github.com/shoonya0/ECHO/blob/main/_docs/websocket-requests.md) · [`websocket-events.md`](https://github.com/shoonya0/ECHO/blob/main/_docs/websocket-events.md) |
| **Server changes the app must handle** (read before touching networking or login) | [`Android/_docs/08_SERVER_CHANGES/`](https://github.com/shoonya0/ECHO/blob/main/Android/_docs/08_SERVER_CHANGES/README.md) |
| The app's original plans (phases 0–8, screens, models, testing strategy) | [`Android/_docs/`](https://github.com/shoonya0/ECHO/blob/main/Android/_docs/README.md) |
| The app's original coding rules and skills | `Android/rules and skills/` in the backend repo |
| Run the server locally | [`_docs/getting-started.md`](https://github.com/shoonya0/ECHO/blob/main/_docs/getting-started.md) (`docker compose -f docker/docker-compose.yml up -d`, then `go run ./cmd/server`, health at `localhost:8080/health`) |

With the local clone present, read those files from `D:\projects\ECHO` directly; they may
be ahead of `main` on the branch being worked on.

## What the app uses today

Verified from the `*ApiService.kt` files and `WebSocketCommand.kt` / `ServerFrame.kt` on 2026-11-18.

| Area | Calls |
|---|---|
| Auth | `POST login` · `POST signup` · `POST auth/logout` |
| Chats | `POST chats/direct/{userId}` · `GET chats/hub/stats` · `GET messages/{chatId}/messages` |
| Contacts | `GET users/contacts/`, `…/requests`, `…/sent-requests`, `…/blocked`, `…/favorites` · `POST/PUT/DELETE users/contacts/{id}` · `POST users/contacts/blockUnblock/{userId}` · `POST/DELETE users/contacts/favorite/{userId}` |
| Users | `GET users/{id}` · `GET users/suggestions` |
| Profile | `GET profile/` · `PUT profile/` · `DELETE profile/delete` |
| WebSocket out | `send_message` · `join_chat` · `leave_chat` · `set_typing` · `mark_read` · `add_reaction` · `remove_reaction` |
| WebSocket in | `message` · `typing` · `presence` · `reaction` · `delivery` · `join` · `leave` · `response` · `error` |

Not used yet: everything under groups and invite codes.

## Working across both repos

- A change that needs a server change: make the server change in `D:\projects\ECHO` on its own
  branch and PR first (it has its own verify and docs), then the app change here. Say in both
  PRs which one depends on the other.
- When the server docs list a new change in `Android/_docs/08_SERVER_CHANGES/`, add the app's
  gap to [current-state.md](./current-state.md#known-issues-and-risks) until it's handled.
