# Claude-Agentic-SDLC

A workspace for building a reusable Agentic SDLC framework on Claude Code, and the applications it develops.

## Layout

| Folder | Purpose | Who changes it |
|---|---|---|
| `reference/` | Source requirements and reference material (read-only for tooling) | Humans only, on `reference/*` branches |
| `framework/` | The reusable SDLC framework: agents, skills, commands, hooks, scripts, templates | Humans, during framework development |
| `project/` | The application being built by the framework, plus per-story run records | The framework during story runs; humans otherwise |

## Rules

- **One Git repository**, rooted at this folder. Nested repositories are not allowed.
- `framework/` and `project/` are separated by path and by branch, not by repository.
- Story work happens on `sdlc/<JIRA-KEY>` branches cut from `main`, and touches only `project/`.
- Framework work happens on `framework/<topic>` branches and touches only `framework/`.
- Nothing merges automatically. Every pull request is reviewed and merged by a person.

## Status

Workspace skeleton only. Git is not initialized yet, no remote is configured, and no application code exists yet.

See `framework/README.md` for the framework itself and `project/README.md` for the application area.
