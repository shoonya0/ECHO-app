# AI harness setup log

This is the setup agent's record and resume point. If the setup is interrupted, start
the agent again with the same prompt and it continues from the first `TODO` row. Every
other status is final.

| Field | Value |
|---|---|
| Started | 2026-11-18 |
| Target | `D:\projects\ECHO-app` |
| Playbook | `D:\projects\ECHO-app\app-setup` (git-ignored copy inside the target) |
| Playbook version | `2026-10-14` |
| OS / shell | `win32` / Bash (Git Bash) + PowerShell |
| Mode | EXISTING |
| Stack | Kotlin, Jetpack Compose, Android (Gradle Kotlin DSL), Hilt |
| Platforms (CI matrix) | Android (mobile) |
| Depth | Full |
| Methodology plugin | none |
| Hosting | GitHub (`shoonya0/ECHO-app`) |
| Setup branch | `chore/ai-harness` (from `origin/main` @ `201396e`) |
| Verify command | `node scripts/verify.mjs` (full: `node scripts/verify.mjs --full`) |

Status values: `TODO` · `DONE` · `SKIPPED` (with consequence) · `DEFERRED` (with
re-enable condition) · `BLOCKED` (with reason)

Related repo: the backend is [`shoonya0/ECHO`](https://github.com/shoonya0/ECHO) (Go),
cloned at `D:\projects\ECHO`, set up with the same playbook (`2026-10-14`).

## Tools

| Tool | Needed? | Status | Version / evidence | Who installed |
|---|---|---|---|---|
| git | yes | DONE | git 2.55.0.windows.3 | pre-installed |
| node ≥ 20 | yes | DONE | v24.19.0 (npm 11.17.0) | pre-installed |
| gh (+ auth) | yes (GitHub) | DONE | gh 2.102.0, logged in as `shoonya0` | pre-installed |
| uv / pipx / pip | no | SKIPPED | uv/pipx missing; not needed, code-review-graph already installed | — |
| code-review-graph | yes | DONE | on PATH (`check-tools.mjs` → ok) | pre-installed |
| agent-browser (+ Chrome) | no | SKIPPED | 0.38.1 present, but this is a native Android app (no web UI); UI access goes through adb/emulator (H14) | pre-installed |
| stack toolchain | yes | DONE | JDK: `JAVA_HOME` = Android Studio JBR (OpenJDK 25.0.2); `java` not on PATH (Gradle uses JAVA_HOME). Android SDK `D:\Applications\AndroidSdk`, adb present, AVD `Pixel_5` | pre-installed |

## Steps

| ID | Step | Status | Evidence (command → result) / decision |
|---|---|---|---|
| P1 | Target confirmed | DONE | `D:\projects\ECHO-app` (session working directory) |
| P2 | Platform + Node | DONE | `node -p process.platform…` → `win32 x64 node v24.19.0` |
| P3 | Project inspected | DONE | `inspect-project.mjs` → JVM (Gradle), Kotlin 164 files, 29 tests, no AI artifacts, no CI, no pre-commit, suggested EXISTING |
| P4 | Mode chosen | DONE | User: EXISTING |
| P5 | Scope answers | DONE | Platforms: Android (detected). Depth: Full. Method: none. Hosting: GitHub (detected from `origin`) |
| P6 | Setup log created | DONE | this file |
| T1–T8 | Tools (see table) | DONE | `check-tools.mjs` → missing required: none; missing recommended: uv (not needed) |
| N1 / E1 | Git + safety (branch) | DONE | Dirty tree (user's API-URL edits, `.gitignore`, `.idea/*`) left as is by user choice, never staged. `git switch -c chore/ai-harness origin/main` |
| N2 / E2 | Vision interview / secrets check | DONE | No tracked `.env`/keystore/`google-services.json`/keys; no history hits; one pattern match was a prefs key name (`SecureTokenStore.kt:43`), not a secret. No signing config in `app/build.gradle.kts` |
| N3 / E3 | Stack ADR / graph build | DONE | `code-review-graph build` → 165 files, 981 nodes, 8136 edges (4 s); cache ignores itself (`.code-review-graph/.gitignore`) |
| N4 / E4 | Architecture doc + review / architecture tour | DONE | graph architecture/communities/flows/large-functions + entry points read; layering clean; 10 riskiest areas in `architecture-overview.md` |
| N5 / E5 | Roadmap + test plans / architecture-overview + current-state | DONE | `_docs/README.md`, `current-state.md`, `architecture-overview.md`, `backend.md`. User: `appInfo/` → `_docs/plans/` with `git mv` (28 renames) + index. One root `_docs/` (single module) |
| N6 / E6 | Scaffold + deps / checks found | DONE | checks: `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `connectedDebugAndroidTest`. User approved the first three for the baseline |
| — / E7 | Baseline at `<sha>` | DONE | see Test baseline below |
| — / E8 | Verify command | DONE | `scripts/verify.mjs`: fast = docs-check, architecture, spotlessCheck, unit tests; `--full` adds lint, UI-test compile, assembleDebug, device tests. Lint moved to `--full` (warm with a change: unit 94 s + lint 44 s). Fast → passed (15 s warm); `--full` → passed (device tests SKIPPED, no emulator). Gradle called by absolute path: `NoDefaultCurrentDirectoryInExePath=1` |
| — / E9 | Characterization tests | SKIPPED | user: make the WebSocket safety net task 1 (needs a seam first; current-state "Next task") |
| H1 | .gitignore + .claude tracking | DONE | harness block appended to `.gitignore`; user: `/app-setup` stays ignored and is committed with it. `git check-ignore`: local files ignored; `.claude/settings.json`, hooks, skills, agents, `.mcp.json`, `.githooks` not ignored |
| H2 | Verify command | DONE | see E8 |
| H3 | Hook scripts copied, adapted, stdin-tested | DONE | copied protect-files (+ Android paths, `_docs/plans/`), guard-shell, git-guard, session-context, stop-state, stop-verify. Node stdin harness (JSON.stringify, Windows paths): 23/23 expected exit codes, incl. commit on `main` blocked for the branch reason. stop-verify → 0; stop-state → 0. Not copied: format-on-edit (Gradle per edit too slow; ktlint in verify), graph-update (user-level hook already runs `code-review-graph update`) |
| H4 | .claude/settings.json | DONE | `.claude/settings.json` parses; attribution off; `CLAUDE_VERIFY_CMD=node scripts/verify.mjs`; Stop hook registered (baseline green). Hooks were live in this session: V3 probes 1–2 blocked |
| H5 | Skills + reviewer agent | DONE | `verify-change` (Gradle + adb rungs), `close-increment` (`_docs/status/increments.md`), `feature-docs` + templates (docs in `_docs/features/<f>/`, pointer via `.claude/rules/features/`), `code-reviewer` agent (+ Android and backend-contract checks) |
| H6 | CLAUDE.md | DONE | `CLAUDE.md` 101 lines with the save-state rule; every command in it was run. Merge rule taken from the user's global CLAUDE.md (not asked again): push + PR, then stop; the user merges (`--rebase`, never `--squash`); merging deploys nothing (no CD) |
| H7 | Docs skeleton | DONE | `scripts/docs-check.mjs` (+ `SKIP_PATHS` `_docs/plans`; probe: a broken link into `plans/` is still an ERROR) → 10 files, 0 errors, 0 warnings, also `--strict` |
| H8 | Formatter + linter | DONE | user: Spotless 8.11.0 + ktlint 1.8.0, `ratchetFrom("origin/main")`, `app/src/**/*.kt` only (root `build.gradle.kts`, `libs.versions.toml`), `.editorconfig` (android_studio, Composable naming). Probe file → spotlessCheck failed with diff → spotlessApply fixed it → deleted. Android lint: 0 errors, 55 warnings |
| H9 | Architecture rules | DONE | user: `scripts/check-architecture.mjs` (errors, not warnings: 0 violations at baseline; 1 listed exception). Scratch file with 3 bad imports → 3 errors with fix messages → deleted |
| H10 | Pre-commit | DONE | `.githooks/pre-commit(.mjs)`: docs-check + architecture. User approved `git config core.hooksPath .githooks`; `git hook run pre-commit` → exit 0. The shim is staged `--chmod=+x` at commit (V7) |
| H11 | CI | DONE | `.github/workflows/verify.yml`: push to `main` + PRs, ubuntu-latest, Temurin 21, setup-gradle, `verify --full`, docs-check PR step, `fetch-depth: 0`. Top-level keys checked (no YAML parser available). Not run on GitHub yet |
| H12 | Plugins | DONE | user chose context7: `claude plugin install context7@claude-plugins-official --scope project` → enabled (`enabledPlugins`). Its MCP says "Needs authentication" (user: `/mcp`). Declined: security-guidance, kotlin-lsp |
| H13 | code-review-graph MCP | DONE | `.mcp.json` written by hand, identical to the backend's (the installer would also append to CLAUDE.md and .gitignore). `claude mcp list` → `code-review-graph` "Pending approval" (approve after restart) |
| H14 | Agent UI access | DONE | Pixel_5 AVD (Android 17, API 37) booted in 20 s; installDebug 39 s; cold launch 5.3 s, no FATAL in logcat; uiautomator dump of the login screen (package com.Shoonya.Echo), tap on "Sign Up" bounds → signup form (Username field); screenshots in _docs/evidence/setup-smoke-{login,signup}.png. connectedDebugAndroidTest → 10/10 (146 s). Trap found: Git Bash rewrites /sdcard paths (MSYS_NO_PATHCONV=1), now in verify-change |
| H15 | Optional extras | DONE | user request: the playbook parts this project needs are now in `_docs/ai-workflow/` (feature loop, checklists, harness + new clone + upgrade path); `app-setup/` stays git-ignored |
| V1 | Restart | DONE | user restarted Claude Code in the repo (2026-11-18) and approved the `code-review-graph` MCP server |
| V2 | Registration checks | DONE | `claude mcp list` → `code-review-graph` ✔ Connected; SessionStart hooks ran on resume (session-context printed Active work). context7 still "Needs authentication" (optional, the user's call). `/context` token counts not captured |
| V3 | Live probes | DONE | 1 protect: Write `.env.harness-probe` → blocked, no file. 2 guard: `git reset --hard --dry-run-probe` → blocked. 3 format hook: not installed. 4 graph MCP: `list_graph_stats_tool` → 1031 nodes, 176 files; `semantic_search_nodes_tool("EchoWebSocketClient connect")` → `EchoWebSocketClient.connect`. 5 verify: passed (15 s). 6 session-context: printed on resume. 7 docs-check `--strict` → 0/0. 8 Stop hook: no pending failure. 9 UI: H14 |
| V4 | Context budget | DONE | CLAUDE.md 101 lines (≈1.5k tokens, under the 3k guide). 3 project skills; `close-increment` is `disable-model-invocation`. MCP: user-level `postman` is unused by this project, so disable it here via `/mcp` if `/context` shows its cost. Exact token counts need `/context` (V2) |
| V5 | Log finalized | DONE | this log: every step has a final status except V7 |
| V6 | Completion report | DONE | completion report below and in chat |
| V7 | Commit / first task | DONE | user: commit, review, push + PR. Commits a319001 (build: Spotless), 9d81161 (chore: harness + docs), ac84e86 (docs: layer table fix from review), then a docs commit with this row. code-reviewer agent (fresh context): no blocking findings; 1 low (layer table vs check) fixed in ac84e86. Issues #1–#4 filed in shoonya0/ECHO-app (user-approved). PR opened from chore/ai-harness; the user merges. First task: WebSocket safety net, then #1 |

## Test baseline (EXISTING)

At `201396e` (plus the user's uncommitted release-URL edits, which don't change the debug build):

| Command | Duration | Result | Failing tests (by name) |
|---|---|---|---|
| `./gradlew :app:testDebugUnitTest` | 254 s (cold daemon) | ✅ 113/113, 0 skipped | none |
| `./gradlew :app:lintDebug` | 91 s | ✅ 0 errors, 55 warnings | none |
| `./gradlew :app:assembleDebug` | 83 s | ✅ `app-debug.apk` | none |
| `./gradlew :app:connectedDebugAndroidTest` | 146 s | ✅ 10/10 (4 classes) on Pixel_5 AVD, API 37 | none |

## Decisions made by the user

- 2026-11-18: Mode EXISTING, Full harness, no methodology plugin.
- 2026-11-18: Work on `chore/ai-harness`; leave the uncommitted API-URL / `.gitignore` /
  `.idea` changes untouched in the working tree (the user commits them).
- 2026-11-18: File known issues as GitHub issues #1–#4 (WebSocket, chat header, server changes, backup + debug logs).
- 2026-11-18: All project docs live in `_docs/` (user request: current state of the app
  plus other project docs).

## Deferred items (and what re-enables them)

- …

## Completion report

### Change summary
- Added: `CLAUDE.md`; `_docs/` (index, current-state, architecture-overview, backend, ai-workflow/, setup log, evidence/); `.claude/` (settings, 6 hooks, 3 skills + feature templates, code-reviewer agent); `scripts/` (verify, docs-check, check-architecture); `.githooks/`; `.github/workflows/verify.yml`; `.mcp.json`; `.editorconfig`.
- Changed: `.gitignore` (harness block, plus the user's `/app-setup` line); root `build.gradle.kts` and `gradle/libs.versions.toml` (Spotless 8.11.0 + ktlint 1.8.0).
- Moved: `appInfo/` → `_docs/plans/` (`git mv`, 28 files, unchanged), with a new index.
- No app code changed.

### Validation
- Covered by automated checks: hook stdin tests 23/23 (H3); `node scripts/verify.mjs` passed (fast) and `--full` passed (H2/E8); settings JSON parsed (H4); workflow shape checked (H11); docs-check `--strict` 0/0; architecture check with a deliberate violation (H9); ktlint probe (H8); `git hook run pre-commit` (H10).
- Verified live in Claude Code on Windows 11: protect and guard hooks blocked their probes; session-context output; stop-verify exit 0.
- Verified on the emulator (Pixel_5 AVD, API 37): install, cold launch, UI dump, tap → signup form, screenshots; instrumented tests 10/10.
- Not verified: macOS/Linux runs of the hooks and scripts (written cross-platform); CI on GitHub (not pushed yet); the graph MCP tools (pending approval); context7 (needs auth); any flow past login (no backend running).

### Commands executed
- `./gradlew :app:testDebugUnitTest` → 113/113 · `:app:lintDebug` → 0 errors, 55 warnings · `:app:assembleDebug` → ok · `:app:connectedDebugAndroidTest` → 10/10
- `node scripts/verify.mjs` → passed (15 s warm) · `node scripts/verify.mjs --full` → passed (device step skipped without an emulator)
- `node scripts/docs-check.mjs --strict` → 0 errors, 0 warnings · `node scripts/check-architecture.mjs` → 0 violations

### Remaining risks / next steps
- User: restart Claude Code, approve `code-review-graph` (`/mcp`), authenticate context7 if wanted.
- After the first push: `gh run list --workflow verify.yml` must show jobs > 0. The Android SDK platform 37.1 may need downloading on the runner (not tested).
- ktlint ratchet: the first edit of an old Kotlin file may reformat it all (CLAUDE.md says how to commit that).
- First task: the WebSocket safety net (current-state "Next task"), then the known issues one branch each.

