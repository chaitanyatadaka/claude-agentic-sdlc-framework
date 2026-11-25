# Changelog

All notable changes to the framework are recorded here. Versions follow semantic versioning.

## [Unreleased]

### Added

- Eight subagent definitions (`.claude/agents/`) and nine shared skills (`.claude/skills/`).
- `/sdlc` orchestrator skill (`.claude/skills/sdlc/SKILL.md`).
- `sdlc-lib.ps1`, `sdlc-run.ps1`, `sdlc-approve.ps1`: hash-bound artifact records, a hash-chained history, gate checks, and a human-only approval recorder.
- Templates: dependency graph, policy defaults, example project configuration.
- Tests: `test-sdlc-run.ps1` (37 checks) and `test-git-gates.ps1` (10 checks).
- Documentation: `docs/agents.md`, `docs/orchestrator.md`.

### Changed

- Moved `.claude/agents/README.md` to `docs/agents.md`.

### Known limits

- Approval is tamper-evident, not tamper-proof, until OS-level isolation exists.
- Agent write-path restrictions and hook enforcement are not yet implemented.
- Commits require an agent Git identity, which is not yet configured.
- Batch-level CP4 is not implemented.

## [0.1.0] - 2026-11-25

### Added

- Framework folder skeleton: `.claude/` (agents, skills, commands, hooks), `scripts/`, `templates/`, `docs/`, `tests/`.
- Framework `README.md`, `VERSION`, and this changelog.
- Workspace-level `README.md` describing the `reference/`, `framework/`, and `project/` layout.

### Not yet included

- Agent, skill, command, and hook definitions.
- Orchestration, approval recording, status engine, and git hooks.
