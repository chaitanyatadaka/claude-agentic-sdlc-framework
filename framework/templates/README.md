# Templates

This folder will hold the framework's reusable templates: the artifact templates (requirements, architecture, design review, implementation plan, implementation log, code review, verification, PR description, changelog entry), the default policy values, the dependency graph between artifacts, and the example project configuration file.

Implemented:

- `dependencies.json`: the framework-owned artifact graph and checkpoint definitions. Projects cannot change it.
- `policy.defaults.json`: default retry and loop limits. Projects may override them within bounds.
- `sdlc.config.example.json`: the template for a project's `project/sdlc.config.json`. Copy it and replace every REPLACE value.

Artifact templates (requirements, architecture, and so on) live inside the skill that owns each one, under `.claude/skills/`.
