---
name: requirements-authoring
description: Rules and template for writing requirements.md from a Jira story snapshot, with stable requirement IDs, acceptance criteria, assumptions and open questions. Load for Phase 1 work.
---

# Requirements authoring

## Rules
- One requirement per row. Each requirement is a single verifiable statement.
- IDs are `REQ-NNN`, assigned in order, and never reused. Removed requirements stay in the document, marked `Removed`.
- Every requirement has at least one acceptance criterion that a test could check. Phrase criteria as observable results.
- Use `Functional` or `Non-functional` as the category. For non-functional requirements, the criterion must be measurable (for example, a number, a limit, or a checkable condition).
- Put every unstated assumption under Assumptions. Put every question you cannot answer under Open questions.
- Do not add requirements the story does not support. Record a suggested addition as an open question instead.
- Use the story's own terms. Do not rename concepts.

## Template

```
# Requirements: <JIRA-KEY>

Source: input/story.md (snapshot hash recorded by the orchestrator)
Status: draft (revision <N>)

## Summary
<two to three sentences>

## Functional requirements
| ID | Requirement | Acceptance criteria |
|---|---|---|
| REQ-001 | <statement> | <criterion 1>; <criterion 2> |

## Non-functional requirements
| ID | Category | Requirement | Measurable criterion |
|---|---|---|---|
| NFR-001 | <category> | <statement> | <measure> |

## Out of scope
- <item>

## Assumptions
- A-001: <assumption and reason>

## Open questions
- Q-001: <question>

## Revision log
| Rev | Date (UTC) | Trigger | Agent | Summary |
|---|---|---|---|---|
```
