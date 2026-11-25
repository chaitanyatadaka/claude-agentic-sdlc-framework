# Agents

The framework has eight specialized subagents. Each is a Claude Code subagent definition in `framework/.claude/agents/`. The `/sdlc` orchestrator (`framework/.claude/skills/sdlc/SKILL.md`) is the only thing that invokes them. No agent invokes another agent.

## Activation status
These definitions are source files. Claude Code loads agents from a project's `.claude/agents/` folder or from an enabled plugin, not from this folder directly. Activation depends on the delivery method (decision D2), which is not yet tested. Until then, the agents are documented but not active.

## Roster

| Agent | Phase | Tools | Writes (only) | Never |
|---|---|---|---|---|
| `requirements-analyst` | 1 | Read, Grep, Glob, Write | `requirements.md` | Contacts Jira, designs architecture, approves |
| `architect` | 2 (and revisions) | Read, Grep, Glob, Write, Edit | `architecture.md`; dispositions in `design-review.md` | Writes code, changes requirements, approves |
| `design-reviewer` | 3 | Read, Grep, Glob, Write | `design-review.md` (findings) | Edits architecture, records accept or reject decisions |
| `implementation-planner` | 4 | Read, Grep, Glob, Write | `implementation-plan.md` | Writes code, lists framework or run-document paths as task files |
| `developer` | 5 | Read, Grep, Glob, Edit, Write, Bash | Listed task files; `implementation-log.md`; `evidence/` | Commits under the orchestrator, pushes, opens PRs, weakens tests |
| `code-reviewer` | 6 | Read, Grep, Glob, Write, Bash (read-only git) | `code-review.md` | Edits source, runs state-changing commands |
| `verifier` | 7 | Read, Grep, Glob, Write, Bash (configured commands) | `verification.md`; `evidence/` | Edits source or tests, fixes failures |
| `pr-author` | 8 | Read, Grep, Glob, Write, Bash (read-only git) | `pr-description.md`; `changelog-entry.md` | Runs `gh`, pushes, commits, creates or merges PRs |

Every agent denies `Agent` (no subagent spawning), `AskUserQuestion` (questions go to the orchestrator), `WebFetch`, and `WebSearch`. No agent receives any Atlassian or GitHub MCP tool.

## Shared skills
Each agent loads the skills named in its definition:

| Skill | Used by | Purpose |
|---|---|---|
| `sdlc-run-context` | all | Jira key rule, run layout, configuration keys, revision logs, handoff block, boundaries |
| `requirements-authoring` | requirements-analyst | Template and rules for `requirements.md` |
| `architecture-authoring` | architect | Template and rules for `architecture.md` |
| `design-review-checklist` | design-reviewer | Checks, severity scale, finding format |
| `implementation-planning` | implementation-planner | Task rules, file lists, test expectations |
| `implementation-discipline` | developer | Test-first work, bounded fix loop, log and evidence formats |
| `code-review-checklist` | code-reviewer | Seven areas, severity scale, finding format |
| `verification-procedure` | verifier | Running commands, trace matrix, document checks |
| `pr-description-authoring` | pr-author | Five PR sections, changelog format |

## Limits that are enforced by scripts, not by these definitions
Agent instructions are advisory. The following limits are enforced by `framework/scripts/`, and only the orchestrator can change run state:

- Approvals are written only by `sdlc-approve.ps1`, run by a person in a console window.
- Artifacts are completed only through `sdlc-run.ps1 complete`, which checks the revision row and placeholders.
- Push and PR creation are gated by `sdlc-run.ps1 push` and `pr-create`, which check approvals and scope.

Write restrictions per agent (for example, the Developer writing only its task's files) are **not yet enforced** by hooks. That is planned for the hooks milestone.

## Changing an agent
Edit the definition file, then run the framework tests. Keep the agent's tool list minimal. Do not add `Bash` to an agent that does not need it, and never add Atlassian write tools or `gh`.
