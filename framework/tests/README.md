# Framework tests

This folder will hold tests for the framework's own scripts and hooks: hashing, status and staleness logic, approval invalidation, validators, and path checks.

Current tests (run with Windows PowerShell 5.1 from the workspace root):

- `test-sdlc-run.ps1`: 37 checks on key validation, run initialization, completion checks, approvals, invalidation, tamper detection, gate refusals, and the console guard. Runs in a temporary workspace.
- `test-git-gates.ps1`: 10 checks on the CP5 branch-head rule, docs commits, task commits, and path refusals. Runs in a temporary Git repository.

Both create their fixtures under `$env:TEMP` and remove them afterwards.
