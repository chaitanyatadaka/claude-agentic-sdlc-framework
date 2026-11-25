---
name: implementation-discipline
description: "Working rules for the Developer agent: scope checks, test-first work, the bounded fix loop, escalation rules, commit format, the implementation log entry, and evidence summaries. Load for Phase 5 work."
---

# Implementation discipline

## Before writing code
1. Read your task in `implementation-plan.md`. Confirm the task is `ready`, and that every task it depends on is complete.
2. Confirm every file you plan to change is on the task's file list. If not, stop and report `needs-human-input`.
3. Read the requirements the task covers and the test expectations.

## Writing and testing
1. Write or update the tests first, from the expectations. Include the happy path, and the not-found and missing-field cases.
2. Run the tests. They should fail for the right reason before you implement.
3. Implement the minimum that makes the tests pass, within the file list.
4. Run the configured test command, then the lint command.
5. Confirm with `git status` that only listed files changed.

## Fix loop
- After a test failure, make one fix attempt, then rerun the tests.
- Record a failure signature for each attempt: the failing test name plus the first error line.
- If the signature is the same as the previous attempt, stop. Do not repeat the same fix.
- Stop after the attempt limit in `policy.test_fix_attempts_per_task` (default 3), and report `needs-human-input` with the signatures.
- A fix that needs a file outside the list, a changed requirement, or a changed design is not a fix. Stop and report it.

## Never
- Weaken, delete, skip, or mark as pending any test to make it pass.
- Change assertions to match wrong output. If a test is wrong, report it as a finding.
- Hard-code expected values only to satisfy a test.
- Disable linters or type checks, or add suppression comments without a reason in the implementation log.

## Commits
- Under the `/sdlc` orchestrator you do not commit. The orchestrator commits the files your task lists, after your tests pass. Report the files you changed so it can do that.
- If a brief ever asks you to commit directly, do it only when the agent Git identity is configured.
- Stage only the task's files, by name. Never `git add -A` or `git add .`.
- Message format: `<JIRA-KEY> T<n>: <short summary>`.
- Never amend, rebase, or rewrite an existing commit.
- Never push.

## Implementation log entry
Append one entry per attempt to `implementation-log.md`. The file is a revisioned artifact: it must end with a `## Revision log` table, and each unit you write adds one row for the revision number in your brief. Without that row the orchestrator cannot record the file.

```
### T<n> attempt <k>: <summary>
- Status: complete | blocked | needs-human-input
- Files changed: <list>
- Tests run: <command> -> <exit code>, <pass> passed, <fail> failed
- Failure signature(s): <list, or none>
- Deviations from plan: <list, or none>
- Lint suppressions (with reason): <list, or none>
- Commit: <sha, or not committed>
```

## Evidence summary
For each test or lint run, write `evidence/summary-T<n>-<k>.md`:

```
Command: <command>
Exit code: <code>
Counts: <pass> passed, <fail> failed, <skipped> skipped
First failure excerpt (if any): <up to ten lines>
Raw log: evidence/raw/T<n>-<k>.log (not committed)
```

Write the raw log to `evidence/raw/`. That folder is git-ignored.
