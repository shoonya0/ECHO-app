# ECHO Android: docs index

Linked from CLAUDE.md. This page is a table of contents: one line per doc or feature, no
detail. Read top-down and stop as soon as you have what the task needs.

<!-- Budget: 80 lines. When the registry outgrows it, group features into areas
(kind: area) and give each area its own index. docs-check reports every feature index
that isn't reachable from this page. -->

## Start here

| Need | Read |
|---|---|
| Where the app stands, active work, test baseline, known issues | [current-state.md](./current-state.md) |
| Modules, layers, allowed imports, main flows, riskiest areas | [architecture-overview.md](./architecture-overview.md) |
| The backend: repo, hosts, API contract, which endpoints the app uses | [backend.md](./backend.md) |
| How we work with the agent: daily loop, checklists, harness, new clone, playbook upgrades | [ai-workflow/](./ai-workflow/README.md) |
| The original plans, rules and skills the app was built from (Aug 2026 snapshot) | [plans/](./plans/README.md) |
| Increment log, written by `/close-increment` (turn into a link once it exists) | `status/increments.md` |
| Why a cross-cutting decision was made (create with the first ADR) | `adr/` |
| How the AI harness was set up, what's deferred, playbook version | [ai-harness-setup.md](./ai-harness-setup.md) |

## Feature registry

One row per feature that has its own docs folder. Status: ⏳ planned · 🟡 in progress ·
✅ done · ⚠️ blocked · 💤 dormant.

| Feature | Docs index | Code | Status | One line |
|---|---|---|---|---|
| *(none yet: `/feature-docs start <feature> <folder>` adds a row)* | | | | |

<!-- Example row:
| chat | [index](./features/chat/README.md) | `app/src/main/java/com/shoonya/echo/features/chat/` | 🟡 | Chat list, chat detail, WebSocket |
-->

## Rules for these docs

- One fact, one home. Other docs link to it instead of repeating it. The API contract's
  home is the backend repo ([backend.md](./backend.md)).
- Feature docs live in `_docs/features/<feature>/` (kept out of the Kotlin source sets), with
  `scope:` naming the feature's code folder, e.g. `app/src/main/java/com/shoonya/echo/features/chat`.
- Over budget (index 80, state 120, other docs 200 lines)? Split it into a folder with
  its own `README.md` index. Run `node scripts/docs-check.mjs` after any docs change.
