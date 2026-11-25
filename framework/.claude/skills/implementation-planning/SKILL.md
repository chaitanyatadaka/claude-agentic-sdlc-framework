---
name: implementation-planning
description: "Rules and template for implementation-plan.md: dependency-ordered tasks, exact file lists, test expectations including not-found and missing-field cases, blocked-task marking, and optional CP4 batches. Load for Phase 4 work."
---

# Implementation planning

## Rules
- Every requirement ID is covered by at least one task. Record the coverage in the table.
- Tasks are numbered `T1`, `T2`, and so on, in dependency order. Every dependency points to an earlier task. Reject cycles.
- Each task lists:
  - the exact file paths it may create or change (no wildcards, no directories);
  - its test expectations;
  - its status: `ready`, or `blocked by Tn`.
- Test expectations include the happy path. Also include the not-found and missing-field cases wherever the requirement has them.
- No task may list a path under `framework/`, `reference/`, `.githooks/`, `.claude/`, or `project/docs/sdlc/`.
- Keep tasks small enough that one task is one reviewable change. A task that touches more than about five files should be split.
- When the configuration sets `cp4_granularity: batch`, group tasks into named batches. Each batch must be independently testable, and every task belongs to exactly one batch.

## Template

```
# Implementation plan: <JIRA-KEY>

Architecture source: architecture.md (revision <N>)
Status: draft (revision <N>)

## Requirement coverage
| Requirement | Tasks |
|---|---|
| REQ-001 | T1, T2 |

## Tasks
### T1: <summary>
- Requirements: REQ-001
- Depends on: none
- Status: ready
- Batch: <name, if batching>
- Files:
  - <exact/path/file.ext>
- Test expectations:
  - Happy path: <description>
  - Not found / missing field: <description>

### T2: <summary>
- Requirements: REQ-001
- Depends on: T1
- Status: blocked by T1
- Batch: <name, if batching>
- Files:
  - <exact/path/file.ext>
- Test expectations:
  - Happy path: <description>

## Batches (only when cp4_granularity is batch)
| Batch | Tasks | Independently testable because |
|---|---|---|
| <name> | T1, T2 | <reason> |

## Revision log
| Rev | Date (UTC) | Trigger | Agent | Summary |
|---|---|---|---|---|
```
