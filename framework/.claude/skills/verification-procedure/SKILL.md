---
name: verification-procedure
description: Procedure for running the configured verification commands, writing evidence summaries, building the requirement trace matrix, checking document quality, and writing verification.md without fixing failures. Load for Phase 7 work.
---

# Verification procedure

## Steps
1. Read the ordered `verification` list in `project/sdlc.config.yml`. If it is missing or empty, report `blocked`. Do not invent commands.
2. Run each command from the project root, in order. Record the start time, the command, and the exit code.
3. Stop the sequence at the first command that fails only if the configuration says `stop_on_failure: true`. Otherwise continue and record every result.
4. Write an evidence summary for each command (format in `implementation-discipline`). Put raw output in `evidence/raw/`.
5. Build the trace matrix from `requirements.md`, `implementation-plan.md`, `implementation-log.md`, and the results.
6. Run the document quality check, then write `verification.md`.

## Trace matrix rules
- Every requirement ID appears once.
- A requirement is `pass` only when a test that covers its acceptance criteria passed. Otherwise it is `fail` (a test failed) or `untested` (no test covers it).
- A requirement with no test is never `pass`.

## Document quality check
- Requirement IDs in the trace matrix match `requirements.md` exactly.
- No placeholder text (for example `<...>` or `TODO`) remains in any run document.
- Every run document has a revision log.
- Every commit cited exists in the log.

## Rules
- Report failures. Do not fix them. The Developer fixes them through the orchestrator.
- Do not rerun a command to make a failure disappear. If a command is flaky, record each run.
- Do not install dependencies or change configuration to get a command to pass.
- Overall result is `pass` only when every configured command exits 0 and every requirement is `pass`. Otherwise it is `fail`.

## Template

```
# Verification: <JIRA-KEY>

Round: <n>
Overall result: pass | fail

## Commands
| Order | Name | Command | Exit code | Counts | Summary |
|---|---|---|---|---|---|
| 1 | unit | <command> | 0 | 42 passed, 0 failed | evidence/summary-unit.md |

## Trace matrix
| Requirement | Tasks | Commits | Tests | Result |
|---|---|---|---|---|
| REQ-001 | T1 | <sha> | <test names> | pass |

## Document quality check
- IDs consistent: yes | no
- Placeholder text removed: yes | no
- Revision logs present: yes | no

## Failures (if any)
- <command or requirement>: <first relevant error>

## Revision log
| Rev | Date (UTC) | Trigger | Agent | Summary |
|---|---|---|---|---|
```
