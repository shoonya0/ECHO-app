# ECHO Android

The Android client of ECHO, a real-time chat app: accounts, contacts, direct chats over a
WebSocket, profile and settings. Stack: Kotlin 2.2, Jetpack Compose (Material 3), Hilt,
Retrofit + OkHttp, kotlinx.serialization, coroutines/Flow; Gradle 9.5 (Kotlin DSL), AGP 9.3,
one module `:app`. Platform: Android (minSdk 34, targetSdk 36). The server is a separate
repo: [`shoonya0/ECHO`](https://github.com/shoonya0/ECHO) (Go), local clone `D:\projects\ECHO`.

## Read first (map, not manual)

| Need | Read |
|---|---|
| Where the app stands, active work, test baseline, known issues | `_docs/current-state.md` |
| Which features have docs, and every other doc | [`_docs/README.md`](_docs/README.md) → Feature registry |
| Modules, layers, allowed imports, main flows, riskiest areas | `_docs/architecture-overview.md` |
| The API contract and server changes the app must handle | `_docs/backend.md` (links into the backend repo) |
| Daily loop, checklists, the harness, new-clone setup, playbook upgrades | `_docs/ai-workflow/README.md` |
| The original plans, rules and skills (frozen, Aug 2026) | `_docs/plans/README.md` |

## Commands

Bash uses `./gradlew`; PowerShell/cmd use `.\gradlew.bat` (always with the path: a bare
`gradlew.bat` isn't found from Claude Code).

- **Fast check, run before saying done: `node scripts/verify.mjs`** (docs-check,
  architecture, ktlint on changed files, compile + unit tests; ~2 min warm)
- Full check: `node scripts/verify.mjs --full` (adds Android lint, UI-test compile, debug APK,
  and device UI tests when an emulator is attached)
- Single test class: `./gradlew :app:testDebugUnitTest --tests "com.shoonya.echo.chat.ChatDetailViewModelTest"`
- Format: `./gradlew spotlessApply` (fixes what `spotlessCheck` reports)
- Docs: `node scripts/docs-check.mjs` · layers: `node scripts/check-architecture.mjs`
- UI tests on the emulator: `./gradlew :app:connectedDebugAndroidTest`
- Agent UI check (emulator + adb): see `/verify-change` rung 6.

## Workflow

- Find code with the code graph (code-review-graph MCP: `semantic_search_nodes`,
  `query_graph` callers_of/tests_for, `get_impact_radius`) before Grep or reading files.
- Non-trivial change: spec → plan → implement in small steps → `/verify-change` →
  `code-reviewer` agent or `/code-review` → `/close-increment`.
- Feature docs live in `_docs/features/<feature>/`. Read its `state.md` and `tasks.md` first,
  then only what its `README.md` lists for the task. Link facts, don't copy them. Document
  only what the code can't tell (intent, reasons, progress, traps). Docs changed:
  `node scripts/docs-check.mjs`. Branch per feature: `feature/<feature>`.
- Bugs: write a failing test that reproduces the bug first.
- Write tests in the same session as the feature. Never delete, skip or weaken a test
  without explaining why and getting approval.
- Commit only the files you changed for this increment.
- Report evidence (commands + output, screenshots), not claims. Use the validation
  categories in `/close-increment`.

## Git

- Never commit on `main`. Branch from an up-to-date base: `git fetch` then
  `git switch -c <type>/<name> origin/main` with `feature/<name>`, `fix/<issue#>-<name>`,
  `refactor/`, `docs/`, `chore/`. One branch, one change.
- Author = the signed-in GitHub user (`gh api user`): name = login, email =
  `<id>+<login>@users.noreply.github.com`. No AI watermark (Co-Authored-By, "Generated with
  Claude Code", 🤖) in commits, PRs or issues. `.claude/hooks/git-guard.mjs` enforces all three.
- Conventional Commits (`type(scope): summary`, body says why, `Fixes #N`). Stage by path;
  review `git diff --staged`.
- After each commit, verify: `git log -1 --format='%an <%ae>%n%n%B'`, `git show --stat HEAD`, `git status --short`.
- Increments are committed locally. Feature done and verified → `/code-review` if non-trivial →
  push → PR (completion report in the body) → **stop: the user reviews and merges.** Never run
  `gh pr merge` unless the user asks for that PR; then `gh pr merge --rebase --delete-branch`,
  never `--squash`. Don't wait for or watch CI.
- A change that needs the server: do the server part in `D:\projects\ECHO` on its own branch
  and PR, and say in both PRs which depends on which.
- Bug outside the current task → propose a GitHub issue (draft title, repro, expected/actual,
  `file:line`, severity); don't fix it here. Create it with `gh issue create` only after the user agrees.

## Rules (each one exists because something went wrong)

- IMPORTANT: Always save the current state of the app after every piece of work: run
  `/feature-docs update` (tick `tasks.md`, overwrite "Now" in the feature's `state.md`,
  update "Active work" in `_docs/current-state.md`). A Stop hook checks this.
- IMPORTANT: Before changing networking, login or WebSocket code, read the backend's
  `Android/_docs/08_SERVER_CHANGES/` (via `_docs/backend.md`). The server changed after the
  app was built, and the app doesn't handle all of it yet (current-state, known issue 4).
- IMPORTANT: Secrets only in `EncryptedSharedPreferences` (`SecureTokenStore`). Never in code,
  logs, fixtures or prompts. Log with Timber and a tag (no `android.util.Log`), never a token.
- New dependency: say why, add it to `gradle/libs.versions.toml`, and check that it exists and
  is maintained before adding it.
- Respect the layers in `_docs/architecture-overview.md#layers-and-allowed-imports`:
  presentation → domain ← data; `domain/` is plain Kotlin. `scripts/check-architecture.mjs`
  enforces them, and its message says how to fix a violation.
- New screen: keep a stateless `*Content` composable next to the stateful screen, so
  Compose UI tests (`app/src/androidTest/.../ui/`) can render it without Hilt or a server.
  Unit tests use the fakes in `app/src/test/java/com/shoonya/echo/fakes/`.
- Debug builds call `http://10.0.2.2:8080` (the backend on this machine); release builds call
  `https://echo-server.onrender.com`. Both come from `BuildConfig` (`app/build.gradle.kts`).
- ktlint runs only on files changed since `origin/main`, so the first edit of an old file can
  reformat all of it. If that diff is large, commit `./gradlew spotlessApply` on its own as
  `style(<scope>): format <file>` before the real change.
- Hook-protected, don't edit: `_docs/plans/` (frozen snapshot), `.idea/`, `local.properties`,
  `gradle/wrapper/`, build output.

## When compacting

Keep: the list of modified files, the current task and its spec path, and the test
commands with their latest results.
