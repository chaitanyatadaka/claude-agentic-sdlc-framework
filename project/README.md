# Project

This folder will hold the application that the framework develops. It is not created yet.

## Intended contents

- Application source and tests, in the layout chosen for the application's technology stack
- `sdlc.config.yml`: application-specific framework configuration, such as the Jira project key, test commands, and branch prefix
- `CLAUDE.md`: instructions for Claude Code when working on this application
- `CHANGELOG.md`: the application's changelog, updated once per story at checkpoint 6
- `docs/sdlc/<JIRA-KEY>/`: the record of each story's run, including requirements, architecture, approvals, and evidence

## Rules

- Only the framework's story runs and people working on the application change this folder.
- The framework never edits files in `framework/` or `reference/` from inside this folder.
- Application technology choices are made per application, not in the framework.
