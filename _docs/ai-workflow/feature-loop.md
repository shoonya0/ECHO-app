# The daily loop (feature, bug, refactor)

Every change goes through the same loop. Adapted from playbook chapter 06.

```text
 ┌─► 1 Pick ONE task ─► 2 Fresh context ─► 3 Orient (graph + docs, read-only)
 │   4 Specify (spec / failing repro test) ─► 5 Plan (+ second opinion)
 │   6 Implement in small steps, tests in the same session
 │   7 Verify (ladder) ── fail ──► fix ──┐
 │   8 Review in a fresh context ── gaps ┘
 │   9 Record (/close-increment) ─► 10 Commit · PR · stop (you merge)
 └──────────────────────────── 11 /clear ◄─┘
```

## The steps

1. **Pick one task** from [current-state.md](../current-state.md) (Next task, Known issues) or a
   GitHub issue. Size it so the diff can be reviewed in about 15 minutes. A feature that will take
   more than one session gets docs first: `/feature-docs start <feature>` (folder
   `_docs/features/<feature>/`) and a branch `feature/<feature>`.
2. **Fresh context:** `/clear` or a new session. The SessionStart hook prints "Active work" and
   the feature's "Now". `/feature-docs resume <feature>` switches feature.
3. **Orient (read-only):**
   ```text
   Task: <one line>. Don't edit anything yet.
   1. Use code-review-graph (semantic_search_nodes, query_graph callers_of/tests_for,
      get_impact_radius) to find the code involved.
   2. Read the feature's state.md, then only the docs its README.md lists for this task.
      Networking/login/WebSocket task: also the backend's Android/_docs/08_SERVER_CHANGES/
      and the matching rest-api/ or WEBSOCKET_PROTOCOL.md page (see _docs/backend.md).
   3. Summarize: current behaviour, files (file:line), callers, existing tests, and anything
      in the docs that contradicts the code.
   ```
4. **Specify.** Feature: "Interview me with AskUserQuestion about <feature> (behaviour, edge
   cases, error and loading states, offline/reconnect, what's out of scope), then write
   `_docs/features/<feature>/spec.md` with checkable acceptance criteria, a Review focus list,
   and an end-to-end check on the emulator." Bug: "Reproduce it with a FAILING unit test first
   (fakes in `app/src/test/.../fakes/`) and show the failure; then the root cause with file:line."
5. **Plan** (plan mode, `Shift+Tab`) when several files change or the approach is unclear: steps
   in order, files per step, the test each step adds, the verify command, what must NOT change,
   risks; every step leaves the build green. Approved steps go into the feature's `tasks.md`.
   Second opinion: paste the plan into a new session — "Review this plan against
   `_docs/architecture-overview.md` and the spec. Flag wrong layer, missing edge cases,
   untestable steps, scope creep, simpler alternatives."
6. **Implement** one step at a time: "Implement step 1 only, with its tests, run
   `node scripts/verify.mjs`, show the output, stop." No new dependency without a reason (and
   the version catalog); never edit a test just to make it pass; anything outside the plan goes
   to a follow-ups list.
7. **Verify:** `/verify-change`. Rungs: fast verify always; `--full` for Gradle, manifest,
   resource or dependency changes; `connectedDebugAndroidTest` when a screen changed; the
   emulator + adb flow when a user flow changed (screenshot into `_docs/evidence/`).
8. **Review in a fresh context:** the `code-reviewer` agent ("review `git diff` against
   `_docs/features/<feature>/spec.md`") or `/code-review`; `/security-review` for auth, tokens,
   storage or network changes. Fix real gaps; skip style nitpicks.
9. **Record:** `/close-increment` updates `_docs/status/increments.md`, the feature's
   `tasks.md`/`state.md`, Active work, runs docs-check, adds one CLAUDE.md lesson only for a
   mistake that would repeat, and writes the completion report.
10. **Commit, PR, stop.** One commit per increment, staged by path. When the feature's last
    increment is in: push, open a PR with the completion report, report the URL and stop.
    **You** review and merge (`gh pr merge --rebase --delete-branch` only when you ask).
11. **Reset:** `/clear`. Stopping mid-increment? `/feature-docs update` first.

## Variants

| Variant | Differences |
|---|---|
| Tiny change (one-sentence diff) | Skip 4 and 5: "do X, run verify, show output". Still one atomic commit. |
| Bug fix | Failing repro test first; the fix turns it green without touching unrelated tests. |
| Refactor | Characterization tests first, no behaviour change; check `get_impact_radius`. |
| Needs a server change | Server part first in `D:\projects\ECHO` (its own branch, verify and PR); the app PR says it depends on it. |
| UI change | Screenshot or design in the prompt; check light and dark theme, small and large font scale, the `*Content` preview. |
| Dependency upgrade | One per increment; read its changelog (context7 or the web); `verify --full` plus the release build. |
| Big unknown bug | Write findings, root causes and a plan into `_docs/<topic>-handoff/00-START-HERE.md` before fixing. |

## When things go wrong

| Symptom | Fix |
|---|---|
| You corrected the same thing twice | `/clear` and a better first prompt with what you learned. |
| "Done" but it fails | The Stop hook runs verify; ask for evidence (commands + output). |
| Invented API or option | context7 for current docs; the compile step in verify catches it. |
| Drifts from the plan or edits unrelated files | Back to plan mode, smaller steps, name the files; the protect-files hook guards sensitive paths. |
| Keeps breaking a CLAUDE.md rule | Shorten CLAUDE.md, mark the line IMPORTANT, or make it a hook or a check. |
| Context filling up | `/compact Focus on <task>`; send exploration to a subagent. |
| Undo | `Esc Esc` / `/rewind`, or `git restore` / `git revert`. |

## Prompts to copy

- **Evidence:** "Don't tell me it works. Show the commands you ran, their output (tail), a
  screenshot for UI, and list anything you did NOT verify."
- **Scope guard:** "Only change files under `<path>`. If something outside it must change, stop and ask."
- **Lesson:** "That mistake will happen again. Add ONE line to CLAUDE.md, or propose a hook."
- **Hard problem:** "Take your time. Read all related code first. Propose 2 approaches with
  tradeoffs before implementing."
- **Self-review:** "Before you say done, re-read the spec and your diff. List each acceptance
  criterion with PASS/FAIL and its evidence."
