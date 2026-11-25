# Scripts

This folder will hold the framework's deterministic scripts, written for Windows PowerShell 5.1. They will cover artifact hashing, the status engine that determines which approvals are valid, artifact validators, and the human-run approval recorder.

Implemented:

- `sdlc-lib.ps1`: shared helpers. Key validation, normalized hashing, the hash-chained history, locking, approval records, gate checks, read-only Git checks. It contains no project values.
- `sdlc-run.ps1`: the command dispatcher the orchestrator calls (`init`, `status`, `revise`, `complete`, `gate`, `event`, `checkpoint-commit`, `push`, `pr-create`, `abort`).
- `sdlc-approve.ps1`: the human-only approval recorder. It refuses redirected input and requires a typed confirmation.

Run them with Windows PowerShell 5.1 from the workspace root. The tests are in `framework/tests/`.
