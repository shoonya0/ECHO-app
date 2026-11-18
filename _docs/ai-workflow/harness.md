# The AI harness: what it is, new clones, upgrades

Everything here is in the repo; the setup record and evidence are in
[ai-harness-setup.md](../ai-harness-setup.md).

## Guides (read before the agent acts)

| Piece | Where | What it does |
|---|---|---|
| Project map | `CLAUDE.md` | Commands, workflow, git rules, project rules (≤ 150 lines) |
| Docs | `_docs/` ([index](../README.md)) | Current state, architecture, backend contract, feature docs |
| Skills | `.claude/skills/` | `/verify-change` (verification ladder), `/close-increment` (record + commit), `/feature-docs` (per-feature docs: start, resume, update, split, check) |
| Reviewer | `.claude/agents/code-reviewer.md` | Reviews a diff in a fresh context against the spec |
| Code graph | `.mcp.json` → `code-review-graph serve` | Callers, tests, impact radius, flows as MCP tools |
| Library docs | plugin `context7` (`.claude/settings.json` `enabledPlugins`) | Current docs for Compose, Hilt, OkHttp, … |

## Sensors (check after the agent acts)

| Piece | Where | Runs when |
|---|---|---|
| Fast verify | `node scripts/verify.mjs`: docs-check, `check-architecture`, `spotlessCheck` (ktlint, files changed vs `origin/main`), compile + unit tests | Stop hook after each turn that changed files; before "done" |
| Full verify | `--full` adds Android lint, UI-test compile, debug APK, device tests if an emulator is attached | Before a PR; in CI |
| Docs check | `scripts/docs-check.mjs`: links, registry, budgets (index 80, state 120, leaf 200), stale feature state. Skips `_docs/plans/` (`SKIP_PATHS`) | verify, pre-commit, CI (`--strict --base` on PRs) |
| Layer check | `scripts/check-architecture.mjs` (exceptions listed in the script) | verify, pre-commit |
| Formatting | Spotless + ktlint 1.8.0, style in `.editorconfig` (`android_studio`, Composable naming) | verify; fix with `./gradlew spotlessApply` |
| Pre-commit | `.githooks/pre-commit` → docs-check + layer check (< 1 s) | every commit, once `core.hooksPath` is set |
| CI | `.github/workflows/verify.yml`: Ubuntu, Temurin 21, `verify --full` | push to `main`, every PR |

## Hooks (`.claude/settings.json`, scripts in `.claude/hooks/`)

| Hook | Does |
|---|---|
| `session-context.mjs` (SessionStart) | Prints Active work and the active feature's "Now" |
| `protect-files.mjs` (before Edit/Write) | Blocks `.env*`, keystores, `local.properties`, `google-services.json`, `gradle/wrapper/`, build output, `.idea/`, `_docs/plans/` |
| `guard-shell.mjs` (before shell) | Blocks `rm -rf` of roots, `git reset --hard`, `git clean -f`, force-push to main, `--no-verify` |
| `git-guard.mjs` (before shell) | No commits on `main`; author must be the GitHub login + noreply email; no AI watermark |
| `stop-verify.mjs` (Stop) | Runs `CLAUDE_VERIFY_CMD`; blocks "done" up to 3 times while it fails |
| `stop-state.mjs` (Stop) | Asks for `/feature-docs update` when feature code changed but its state didn't |

Deliberately left out: `format-on-edit` (a Gradle call per edit is too slow; ktlint runs in
verify instead) and `graph-update` (the user-level settings already update the graph after edits).

## Setting up a new clone (or a new machine)

1. Android Studio (its JBR is the JDK: set `JAVA_HOME` to `<Android Studio>/jbr`), Android SDK
   with an emulator image, Node 20+, git, `gh` (`gh auth login`).
2. Open the project once in Android Studio (writes `local.properties` with `sdk.dir`).
3. `git config core.hooksPath .githooks` (per clone; turns on the pre-commit hook).
4. Optional but recommended: `code-review-graph` (`uv tool install code-review-graph` or
   `pipx install code-review-graph`), then `code-review-graph build`. Start Claude Code in the
   repo and approve the `code-review-graph` MCP server when asked.
5. Check: `node scripts/verify.mjs` → "verify passed". The first run downloads Gradle
   dependencies and takes a few minutes.
6. Backend for debug builds: clone `shoonya0/ECHO` and run it on port 8080 ([backend.md](../backend.md)).

## Upgrading the playbook

The playbook lives outside the repo (`D:\projects\app-setup`; this repo's `app-setup/` is a
git-ignored copy). When it gets a newer version (its `CHANGELOG.md` has a heading newer than
`2026-10-14`), start Claude Code in this repo with:

```text
Read D:\projects\app-setup\agent\00-START.md and follow it exactly to set up the AI development
harness for this project. The playbook root is D:\projects\app-setup. Ask me before installing,
authenticating, overwriting, committing or pushing anything.
```

It detects the older version and runs UPGRADE mode: only the CHANGELOG steps since
`2026-10-14`, each shown as a diff, keeping this project's adaptations. Those adaptations, so an
upgrade doesn't undo them: `SKIP_PATHS` in `docs-check.mjs`; Android paths in
`protect-files.mjs`; Gradle/adb rungs in `verify-change`; `_docs/features/<feature>/` (never
co-located) in `feature-docs`; `_docs/status/increments.md` as the status log; the user merges PRs.
