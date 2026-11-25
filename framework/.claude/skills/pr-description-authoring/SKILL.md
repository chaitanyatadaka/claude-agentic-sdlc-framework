---
name: pr-description-authoring
description: Rules, template and changelog format for pr-description.md and changelog-entry.md. Every test claim must link to verified evidence. Reviewer checklist items are never pre-ticked. Load for Phase 8 work.
---

# PR description authoring

## Required sections
Every `pr-description.md` has these five sections, in this order:

1. **Summary.** Two to three sentences: what was built and why, using the Jira key.
2. **Changes Made.** A bulleted list of every changed file, with the reason for each. Derive the list from the commit diff, not from memory.
3. **Test Evidence.** Each test claim links to `verification.md` or an evidence summary. Include the overall result.
4. **Known Limitations.** Every untested requirement, every "Not Found" item, and every out-of-scope item.
5. **Reviewer Checklist.** Unchecked boxes only.

## Rules
- Do not claim a test passed unless `verification.md` records it.
- Do not use the words "approved," "merged," or "ready to merge" as statements of status.
- Do not write that the PR has been created. Creating the PR happens after CP6 and is recorded by the orchestrator.
- Do not pre-tick the Reviewer Checklist. Do not remove checklist items.
- Changes Made must cover every file in the run's diff. Check the list against `git diff --stat` before finishing.

## Changelog entry
- One entry per run in `changelog-entry.md`.
- Use the Keep a Changelog style: a heading for the change type (`Added`, `Changed`, `Fixed`, or `Removed`), then one bullet per user-visible change, ending with the Jira key.
- Do not write internal implementation detail that users cannot see.

## Template: pr-description.md

```
# <JIRA-KEY>: <short title>

## Summary
<two to three sentences>

## Changes Made
- `<path>`: <reason>

## Test Evidence
- Overall result: <pass or fail> (see verification.md)
- <command>: exit <code>, <counts> (evidence/summary-<name>.md)

## Known Limitations
- <untested requirement, Not Found item, or out-of-scope item>

## Reviewer Checklist
- [ ] Requirements reviewed (requirements.md)
- [ ] Architecture and design review reviewed
- [ ] Implementation plan reviewed
- [ ] Implementation log and commits reviewed
- [ ] Code review findings reviewed
- [ ] Verification results reviewed
- [ ] Changelog entry reviewed

## Revision log
| Rev | Date (UTC) | Trigger | Agent | Summary |
|---|---|---|---|---|
```

## Template: changelog-entry.md

```
## [Unreleased]

### Added
- <user-visible change> (<JIRA-KEY>)
```
