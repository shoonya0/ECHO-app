# ECHO Android: how we work with the AI agent

The parts of the setup playbook this project uses, adapted to it. The playbook itself
(`D:\projects\app-setup`, outside this repo) also covers other stacks,
new-project setup and background reading that this app doesn't need; you don't need it to
work here. Playbook version this was set up with: `2026-10-14`
([setup log](../ai-harness-setup.md)).

| Need | Read |
|---|---|
| The loop for every feature, bug or refactor, with prompts to copy | [feature-loop.md](./feature-loop.md) |
| Short checklists: session start, before "done", before a PR, weekly, release | [checklists.md](./checklists.md) |
| What the harness is (hooks, skills, scripts, CI, graph), setting up a new clone, upgrading the playbook | [harness.md](./harness.md) |

The method in one line: **you** set the goal and judge the evidence; the agent writes the
code inside guides (CLAUDE.md, `_docs/`, skills) and sensors (verify, hooks, CI, a reviewer in
a fresh context). When the agent gets something wrong twice, fix the environment (a doc
line, a test, a hook, a lint rule), not just the output.
