# Git hooks

These hooks are the Git-level enforcement layer for the workspace repository. The repository's `core.hooksPath` is set to `.githooks`, so Git runs these files instead of its default samples.

**Current status (M0): placeholders only.** Each hook prints a notice and exits successfully. They perform no checks yet. Do not rely on them for any control until the milestone that implements them is complete.

## Planned hooks (implemented in M5)

| Hook | Planned checks |
|---|---|
| `pre-commit` | Staged paths must match the branch's allowed namespace; no changes under `reference/` except on `reference/*` branches; no nested `.git`; secret scan; agent Git identity on `sdlc/*` branches |
| `pre-push` | Branch name matches an allowed namespace; push permitted only with a valid CP5 approval record (or CP6 for PR creation); no force push; no path-scope violation in the pushed range |

## Setup notes

- Hook scripts must use LF line endings and a `#!/bin/sh` shebang, because Git for Windows runs them through its bundled `sh`.
- The executable bit cannot be set until the files are staged. Whether Git for Windows requires it is to be verified in M5 (see verification item V10).
- Changing `core.hooksPath` is denied to agents, so these hooks cannot be disabled by a run.
