# Orchestrator

The `/sdlc` skill (`framework/.claude/skills/sdlc/SKILL.md`) runs in the main Claude Code session. It coordinates the eight agents and calls the framework scripts for every state change. It holds no approval authority.

## Why a skill, not a command file
Current Claude Code documentation merges custom commands into skills. A skill named `sdlc` in `.claude/skills/sdlc/SKILL.md` is invoked as `/sdlc`. The orchestrator must run in the main session, not in a forked context, because subagents cannot launch subagents.

## Commands

`/sdlc <JIRA-KEY> [start|status|resume|abort]` in the workspace. The key may belong to any project that has a `project/sdlc.config.json`.

The scripts are called as:

```
powershell -NoProfile -ExecutionPolicy Bypass -File framework\scripts\sdlc-run.ps1 -Command <command> -Key <KEY> [options]
```

| Command | Effect | Exit codes |
|---|---|---|
| `init` | Creates the run folder, state, manifest, and history. One active run per project. | 0 ok, 1 refused, 2 usage |
| `status` | Reports phase, awaiting checkpoint, node revisions, gate states, and history integrity. | 0, 1 if history broken |
| `revise` | Preserves the current file under `revisions/` and returns the next revision. | 0, 1 |
| `complete` | Checks the artifact (revision row, no unfilled placeholders), then records its hash. | 0, 1 with problems |
| `gate` | Reports VALID, INVALID, or MISSING for a checkpoint (and unit, for CP4). | 0 VALID, 1 otherwise |
| `event` | Appends an allowed event type to the hash-chained history. | 0, 1, 2 |
| `checkpoint-commit` | Commits run documents for an approved checkpoint, or one implementation unit's files. | 0, 1 refused |
| `push` | Pushes the branch. Requires CP5 VALID and a configured remote. | 0, 1 refused |
| `pr-create` | Opens the PR. Requires CP6 VALID, the branch on the remote, and gh authentication. Never merges. | 0, 1 refused |
| `abort` | Marks the run aborted. Keeps the branch and files. | 0 |

Approval is a separate script, run by a person:

```
powershell -NoProfile -ExecutionPolicy Bypass -File framework\scripts\sdlc-approve.ps1 -Key <KEY> -Checkpoint CPn [-Unit <id>]
```

It refuses when input or output is redirected, shows the artifacts and hashes it will bind, and requires the approver's name and the exact typed phrase `APPROVE CPn`.

## Checkpoints

| Checkpoint | Covers | Binds to | Authorizes |
|---|---|---|---|
| CP1 | requirements | requirements hash; story snapshot hash | Architecture work |
| CP2 | architecture, design-review | their hashes; requirements hash | Planning |
| CP3 | implementation-plan | its hash; requirements, architecture, design-review hashes | Implementation |
| CP4 (per unit) | implementation-log | its hash; plan and requirements hashes; the unit's commit | The next unit |
| CP5 | code-review, verification | their hashes; plan, log, requirements hashes; branch HEAD | Push |
| CP6 | pr-description, changelog-entry, CHANGELOG.md | their hashes; review, verification, log, requirements hashes | PR creation |

**Invalidation.** A gate is VALID only while every covered and input hash still matches the approved one. Any upstream change therefore invalidates the downstream approvals automatically, and the orchestrator resumes at the earliest affected checkpoint. For CP5, commits after approval are allowed only if they touch the run's documents or `project/CHANGELOG.md`. Any other change invalidates the approval.

## Policy bounds

Framework defaults are in `framework/templates/policy.defaults.json`. A project may override keys under `policy` in `project/sdlc.config.json`. Unknown keys are refused.

| Key | Default | Allowed range | Meaning |
|---|---|---|---|
| `agent_retry_max` | 1 | 0–2 | Retries after an agent execution failure |
| `malformed_output_retry_max` | 2 | 0–3 | Retries after output fails `complete` |
| `test_fix_attempts_per_task` | 3 | 1–5 | Developer fix attempts per unit |
| `review_fix_rounds` | 2 | 1–3 | Code review fix rounds |
| `verification_fix_rounds` | 2 | 1–3 | Verification fix rounds |
| `design_review_rounds` | 2 | 1–3 | Architect and reviewer rounds |
| `clarification_rounds` | 3 | 1–5 | Requirement question rounds with the human |
| `human_revision_soft_warning` | 3 | no hard cap | Warn after this many revisions at one checkpoint |
| `automated_attempt_budget` | 25 | 5–100 | Total automated attempts per run |
| `identical_failure_stop` | 2 | fixed | Stop after this many identical failure signatures |

Note: this version enforces the history and gates. The retry counters and the attempt budget are applied by the orchestrator's instructions and are not yet recorded or enforced by a script.

## Known limits
- Approval is tamper-evident (hash-bound records and a chained history), not tamper-proof, until OS-level isolation exists.
- The Claude-side permission rules are partly in place (`.claude/settings.json`). Their pattern syntax is unverified, and the approval script is protected only by a rule on its name.
- Agent Git identity is not yet configured, so commits are refused.
- Batch-level CP4 is not implemented. Use `cp4_granularity: task`.
