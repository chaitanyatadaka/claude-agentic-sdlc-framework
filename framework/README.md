# Agentic SDLC Framework

A reusable framework that drives a software change through eight SDLC phases with six human approval checkpoints, using Claude Code.

**Version:** 0.1.0 (skeleton). See `VERSION` and `CHANGELOG.md`.

## What the framework does (when complete)

It takes a Jira issue key and, through eight specialized agents coordinated by one orchestrator, produces requirements, architecture, design review, an implementation plan, code changes with tests, a code review, verification results, and a pull request description. A human approves each of six checkpoints. The framework never merges a pull request.

## What it is not

- It contains no application code. Application code lives in `project/`.
- It contains no project-specific values such as a Jira project key, test command, or language. Those belong in each project's `sdlc.config.yml`.
- It does not write to its own folder during a run.

## Folder map

| Path | Purpose | Status |
|---|---|---|
| `.claude/agents/` | Eight specialized subagent definitions | Defined; not yet active (delivery pending). See `docs/agents.md` |
| `.claude/skills/` | Nine shared skills, plus the `sdlc` orchestrator skill (`/sdlc`) | Defined; see `docs/orchestrator.md` |
| `.claude/commands/` | Not used. Custom commands are merged into skills in current Claude Code | Placeholder only |
| `.claude/hooks/` | Hook definitions that enforce approval and path rules | Empty; defined in a later milestone |
| `scripts/` | Deterministic run, gate, and approval scripts (PowerShell 5.1) | Implemented: `sdlc-lib.ps1`, `sdlc-run.ps1`, `sdlc-approve.ps1` |
| `templates/` | Dependency graph, policy defaults, example project configuration | Implemented; artifact templates live inside the skills |
| `docs/` | Framework documentation: agents, orchestrator | `agents.md`, `orchestrator.md` |
| `tests/` | Tests for the scripts | `test-sdlc-run.ps1`, `test-git-gates.ps1` |
| `VERSION` | Framework version (semantic versioning) | `0.1.0` |
| `CHANGELOG.md` | Framework release history | Initial entry |

## Principles

1. **Application-independent.** Nothing here depends on a particular project, Jira project, or technology stack.
2. **Configuration lives with the project.** Each project supplies its own configuration file.
3. **Enforcement over instruction.** Rules that matter are enforced by permission settings, hooks, and git hooks, not only by prompts.
4. **Human approval is required.** Each checkpoint records an approval tied to the exact content approved.

## Delivery to a project

How the framework is loaded into a project is not decided yet. The options are a local Claude Code plugin, or a generated copy under the project's `.claude/` folder. This depends on a live test planned for the next milestone.

## Maintainers

Changes to this folder are made on `framework/*` branches and merged through pull requests, one per milestone.
