---
name: architecture-authoring
description: "Rules and template for architecture.md: components with single responsibilities, a requirement-to-component map, technology choices with rationale, data flow, a Mermaid diagram, and recorded review dispositions. Load for Phase 2 and revision work."
---

# Architecture authoring

## Rules
- Every requirement ID appears in the map with at least one component. A missing ID is a defect.
- Each component has one responsibility, stated in one sentence. If a component needs "and" to describe itself, split it.
- Choose technologies only where the project configuration or the requirements constrain them. For every choice, record why, and the alternatives rejected.
- Describe data flow as numbered steps from the entry point to storage or output.
- Include one Mermaid diagram of the components and their connections.
- Record each review finding you respond to under Dispositions, with accepted, rejected, or deferred, and the reason. Never leave a finding without a disposition when you were asked to address it.
- Keep the design as simple as the requirements allow. Do not add components for requirements that do not exist.

## Template

```
# Architecture: <JIRA-KEY>

Requirements source: requirements.md (revision <N>)
Status: draft (revision <N>)

## Overview
<two to four sentences>

## Components
| Component | Responsibility | Requirements |
|---|---|---|
| <name> | <one responsibility> | REQ-001, REQ-002 |

## Requirement-to-component map
| Requirement | Components |
|---|---|
| REQ-001 | <name> |

## Technology choices
| Area | Choice | Rationale | Alternatives rejected |
|---|---|---|---|
| <area> | <choice> | <reason> | <alternatives and why> |

## Data flow
1. <step>
2. <step>

## Diagram
```mermaid
flowchart LR
  A[<component>] --> B[<component>]
```

## Dispositions (review findings)
| Finding | Disposition | Change made or reason |
|---|---|---|
| DR-001 | accepted | <change> |

## Risks and open questions
- <risk or question>

## Revision log
| Rev | Date (UTC) | Trigger | Agent | Summary |
|---|---|---|---|---|
```
