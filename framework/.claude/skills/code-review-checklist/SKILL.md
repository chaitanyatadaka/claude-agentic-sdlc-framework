---
name: code-review-checklist
description: The seven-area code review checklist, severity scale, finding format and template for code-review.md. Non-blocking nits are recorded without stopping progress. Load for Phase 6 work.
---

# Code review checklist

## The seven areas
Rate each area as `pass`, `fail`, or `not applicable`, and give evidence (file and line) for every rating.

1. **Correctness.** Does each changed component behave as specified in `requirements.md`?
2. **Security.** Are secrets kept out of output and logs? Is user input validated at each boundary?
3. **Error handling.** Are API failures, missing files, and empty results handled without crashing or silently succeeding?
4. **Test coverage.** Do the tests cover the happy path and the not-found and missing-field cases?
5. **Code clarity.** Are names self-explanatory? Can the logic be followed without comments?
6. **DRY.** Is there duplicated logic that should be one shared function?
7. **Dependency safety.** Does any new or existing dependency have a known vulnerability at its pinned version? Use the configured audit command when one exists.

## Severity scale
- **blocker:** incorrect behavior against a requirement, a security exposure, or missing error handling for a required case. Triggers the fix loop.
- **high:** a likely defect or a clear failure of a checklist area. Triggers the fix loop.
- **medium:** a real weakness that does not break requirements. Recorded, non-blocking.
- **nit:** style or minor clarity. Recorded, non-blocking. Never causes a loop (decision D9).

## Finding format
Each finding has: ID (`CR-NNN`), area, severity, file, line, description, and a suggested fix.

## Rules
- Cite a file and line for every finding. A finding without a location is not a finding.
- Do not report a secret value. Report the file and line only.
- Do not repeat a finding that a previous round resolved. Mark it resolved instead.

## Template

```
# Code review: <JIRA-KEY>

Reviewed: commits <list>
Round: <n>
Blocker and high findings: <count>

## Area ratings
| Area | Rating | Evidence |
|---|---|---|
| Correctness | pass | <file:line or note> |
| Security | fail | <file:line> |

## Findings
| ID | Area | Severity | Location | Description | Suggested fix |
|---|---|---|---|---|---|
| CR-001 | Security | high | src/x.ext:42 | <description> | <fix> |

## Non-blocking notes (medium and nit)
- CR-010 (nit): <note>

## Revision log
| Rev | Date (UTC) | Trigger | Agent | Summary |
|---|---|---|---|---|
```
