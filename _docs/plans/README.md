# ECHO Android: original plans (snapshot, August 2026)

These are the plans the app was built from, moved here from `appInfo/` on 2026-11-18
(`git log --follow` shows their history). **They are a frozen snapshot**, kept as written:
they describe what was planned, not what the code does today.

- What the code does today: [architecture-overview.md](../architecture-overview.md) and
  [current-state.md](../current-state.md).
- **Newer copy:** the backend repo keeps these plans up to date in
  [`Android/_docs/`](https://github.com/shoonya0/ECHO/blob/main/Android/_docs/README.md), plus
  [`08_SERVER_CHANGES/`](https://github.com/shoonya0/ECHO/blob/main/Android/_docs/08_SERVER_CHANGES/README.md),
  the server changes the app must handle. Read that copy, not this one, before planning new work.
- `scripts/docs-check.mjs` doesn't scan this folder (`SKIP_PATHS`), so its long files don't
  count against the docs budgets.

| Doc | Topic |
|---|---|
| [00_PROJECT_OVERVIEW.md](./00_PROJECT_OVERVIEW.md) | Identity, stack, architecture summary |
| [01_PHASE_PLAN.md](./01_PHASE_PLAN.md) | Phases 0–8 with acceptance criteria |
| [02_UI_SCREENS.md](./02_UI_SCREENS.md) | Screens and navigation |
| [03_DATA_MODELS.md](./03_DATA_MODELS.md) | DTOs and domain models |
| [04_API_INTEGRATION.md](./04_API_INTEGRATION.md) | REST integration |
| [05_WEBSOCKET_CLIENT.md](./05_WEBSOCKET_CLIENT.md) | WebSocket client design |
| [06_DEPENDENCIES.md](./06_DEPENDENCIES.md) | Dependencies and build configuration |
| [07_TESTING_STRATEGY.md](./07_TESTING_STRATEGY.md) | Testing strategy |
| [rules-and-skills/rules/](./rules-and-skills/rules/README.md) | 12 Android coding rules (architecture, Compose, coroutines, DI, errors, Gradle, style, modularity, networking, security, testing, WebSocket) |
| [rules-and-skills/skills/](./rules-and-skills/skills/README.md) | 6 skills (debugging, build-verify loop, Compose, error feedback, modular development, WebSocket integration) |

The rules and skills are **not loaded by Claude Code** from here (only `.claude/rules/` and
`.claude/skills/` are). Promoting one means checking it against the code first (some describe
things the app doesn't use, such as Room or DataStore), then copying it into `.claude/`.

## Plan vs. code (2026-11-18)

| Phase (plan) | In the code? |
|---|---|
| 0 Scaffolding · 1 Core infrastructure · 2 Auth · 3 Contacts | Yes |
| 4 Chat REST · 5 Chat WebSocket | Yes, direct chats only (see current-state known issues) |
| 6 Group management | **No**: no groups or invite-code screens or API calls |
| 7 Profile and settings · 8 Polish and tests | Yes (113 unit tests, 10 Compose UI tests) |
