---
name: design-review-checklist
description: Checklist, severity scale, finding format and template for design reviews of architecture.md. Load for Phase 3 work.
---

# Design review checklist

## Checks (apply to every component and every requirement)
1. **Coverage.** Every requirement ID maps to at least one component, and the mapping is correct.
2. **Responsibility.** Each component has one clear responsibility, and no two components duplicate it.
3. **Failure modes.** For each external call or dependency, the design states what happens when it fails, times out, or returns no data.
4. **Empty and missing data.** The design states what happens for empty input, missing fields, and not-found results.
5. **Security.** Input validation at each boundary, secret handling, authorization where applicable, and no secrets in logs or outputs.
6. **Data integrity.** Who owns each piece of data, how it is changed, and how concurrent changes are handled.
7. **Testability.** Each component can be tested in isolation, and each requirement's acceptance criteria can be tested against it.
8. **Dependencies and technology risk.** Choices are justified. Known risks of chosen dependencies are named.
9. **Scale and performance.** Only where a non-functional requirement sets a limit. Check the design can meet it.
10. **Operability.** Errors are observable, and the design says how a failure is diagnosed.

## Severity scale
- **high:** a requirement cannot be met, data can be lost or exposed, or a security boundary is missing. Blocks progress until addressed.
- **medium:** a real gap that can be fixed in planning or implementation. Non-blocking, but must be recorded.
- **low:** a clarity or consistency issue. Non-blocking.

## Finding format
Each finding must include every field:
- ID (`DR-NNN`)
- Severity
- Requirement or component concerned
- Description (what is wrong, and why it matters)
- Proposed fix
- Disposition (left blank for the architect and human to complete)

## Template

```
# Design review: <JIRA-KEY>

Reviewed: architecture.md (revision <N>)
Round: <n>
Open high-severity findings: <count>

## Checks applied
- Coverage: <result>
- <other checks>: <result>

## Findings
| ID | Severity | Concerns | Description | Proposed fix | Disposition |
|---|---|---|---|---|---|
| DR-001 | high | REQ-004 | <description> | <fix> | |

## Previous findings (re-review only)
| ID | Status (resolved, partly resolved, open) | Note |
|---|---|---|

## Revision log
| Rev | Date (UTC) | Trigger | Agent | Summary |
|---|---|---|---|---|
```
