# Checklists

Short lists to run through. Details: [feature-loop.md](./feature-loop.md) and
[harness.md](./harness.md). Adapted from playbook chapter 10.

## Start of every session

- [ ] One task, sized for a review of about 15 minutes
- [ ] `/clear` or a new session; the hook printed Active work (or `/feature-docs resume <feature>`)
- [ ] A feature spanning sessions has `_docs/features/<feature>/` and a `feature/<feature>` branch
- [ ] Graph lookup of the code involved; only the docs the task needs; contradictions noted
- [ ] Networking/login/WebSocket work: the backend's `08_SERVER_CHANGES` read

## Before saying "done"

- [ ] Each acceptance criterion marked PASS/FAIL **with evidence**
- [ ] `node scripts/verify.mjs` green; no new failures vs the baseline in current-state (by test name)
- [ ] UI change: `connectedDebugAndroidTest` run; emulator flow + screenshot in `_docs/evidence/`; light and dark
- [ ] `code-reviewer` agent or `/code-review` run; real gaps fixed (`/security-review` for auth, tokens, storage, network)
- [ ] No deleted, skipped or weakened tests; no `@Suppress` or lint baseline added without approval
- [ ] New dependency: needed, maintained, in `gradle/libs.versions.toml`
- [ ] State saved: `tasks.md` ticked, `state.md` "Now" overwritten, Active work updated
- [ ] Completion report with the exact validation categories (below)

## Before the PR

- [ ] Commits contain only this work's files (never the `.idea/` or local URL edits); messages say what and why
- [ ] `node scripts/verify.mjs --full` green; device tests run or listed as not verified
- [ ] Status log, feature `state.md` and current-state updated in the same PR; docs-check clean
- [ ] PR body = completion report; then stop — the user reviews and merges
- [ ] After the first push of a new workflow: `gh run list --workflow verify.yml` shows jobs > 0

## Completion report (exact format)

```markdown
### Change summary
- Files changed / behaviour changed / modules affected

### Validation
- Covered by automated tests: <suites + counts, e.g. "unit 113/113, UI 10/10">
- Verified by agent UI check (emulator Pixel_5, API <n>): <flows + screenshot paths>
- Tested physically on <device>: <what>
- Build validated: <debug / release APK>
- Reviewed but not executed: <what>
- Requires physical verification on <device>: <what>

### Commands executed
- `node scripts/verify.mjs` → pass (exit 0, 95 s)

### Baseline comparison
- New failures vs baseline @<sha>: none / <list>

### Remaining risks
- …
```

Never write "verified" for something that wasn't run.

## Weekly upkeep

- [ ] `code-review-graph dead-code` and lint's `UnusedResources` warnings: remove what's really unused
- [ ] `node scripts/docs-check.mjs --strict`: split big docs, update stale state
- [ ] Flaky tests fixed or quarantined with an issue
- [ ] One dependency upgrade (lint's `NewerVersionAvailable` / `GradleDependency` list them)
- [ ] Known issues in current-state still true? Close or file them

## Monthly upkeep

- [ ] CLAUDE.md pruned (under 150 lines); procedures moved into skills
- [ ] Unused plugins and MCP servers removed (`/context` shows their cost)
- [ ] Graph hubs and large files (`code-review-graph large-functions`) reviewed for refactoring
- [ ] Hooks, skills and the reviewer agent still match how we work

## Release

- [ ] `verify --full` green in CI and locally with the emulator
- [ ] `./gradlew :app:assembleRelease` (R8 minify on) installed and clicked through: login, contacts, chat, settings
- [ ] Signing keystore kept outside the repo (hook-protected `*.jks`); `versionCode` / `versionName` bumped
- [ ] Release talks to `https://echo-server.onrender.com` and the backend there has the changes this release needs
- [ ] `/security-review`; current-state known issues 5 (backup) and 6 (debug logging) decided
- [ ] Physical checks listed per device; anything unverified under "Remaining risks"
