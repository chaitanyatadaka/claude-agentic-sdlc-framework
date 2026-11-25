# SP-2: Smart Pantry Organizer

## Summary
Smart Pantry Organizer is a web application for one household. It tracks grocery items with categories, quantities and expiry dates. It shows a dashboard with expiry counts and category totals, and suggests recipes from the items in the pantry. The backend is Spring Boot with an H2 file database. The frontend is React with Vite. Development followed the Agentic SDLC framework, with human approval at each checkpoint.

## Changes Made
- Backend (`project/backend`): item create, read, update and delete (REST at `/api/items`); validation with field-level errors; expiry classification (valid, expiring within 3 days, expired, none); dashboard summary (`/api/dashboard`); recipe catalogue of 10 predefined recipes and rule-based suggestions (`/api/recipes`); consistent error responses with a correlation ID and no stack traces; the server binds to 127.0.0.1.
- Frontend (`project/frontend`): dashboard, pantry inventory with search, category and expiry filters, add, edit (pre-filled), delete with confirmation, item detail view, recipes page, responsive layout, error and loading states.
- Project files: `project/sdlc.config.json` (framework configuration), `project/README.md`, `project/CHANGELOG.md`.
- Framework and run records (`framework/`, `project/docs/sdlc/SP-2/`) are in this branch history as the delivery record.

## Test Evidence
- Backend: configured command `mvnw verify` passed with 119 tests, 0 failures (verification record, `project/docs/sdlc/SP-2/verification.md`, round 1; the test count rose to 119 after the code-review fixes).
- Frontend: production build passes.
- Browser end-to-end (Playwright, Chrome, run serially): an earlier run passed 3 of 3 (add, edit and delete, recipes). After the code-review fixes, the suite grew to 11 tests. The last run, against a backend that had not yet been restarted, passed 10 of 11. The one failure was a purchase-date case that needs the restarted backend. The owner declined further automated testing and verified the application manually.
- No code-review round 2 and no fresh verification run after the fix round (declined by the owner).

## Known Limitations
- Recipe photographs are placeholders. Licence-checked photos could not be downloaded, so no attribution file exists.
- Search and filtering run in the browser over the loaded list. The backend search query (plan task T6) was not built.
- No unit of measure: quantity is unitless, per the approved requirements.
- Performance test at 1,000 items, the no-external-AI test, the JaCoCo coverage gate and the build documentation (plan tasks T25 to T28) were not built.
- Responsive layout is checked only at one breakpoint (800 px) by design, not at 375 px and 1280 px.
- Approvals are tamper-evident, not tamper-proof. Browser tests must run serially because the specs share one database.
- Local H2 database files (`project/backend/data/`) are not git-ignored on `main` and are excluded from this branch.

## Reviewer Checklist
- [ ] Requirements reviewed (`project/docs/sdlc/SP-2/requirements.md`, revision 2)
- [ ] Architecture and design review reviewed (revision 2)
- [ ] Implementation plan reviewed (revision 1)
- [ ] Commits reviewed: one unit per commit, with its CP4 approval
- [ ] Code review round 1 and the fix round reviewed (`code-review.md`)
- [ ] Verification record reviewed (`verification.md`)
- [ ] Known limitations accepted
- [ ] `project/backend/data/` added to `.gitignore` on `main`

## Revision log
| Rev | Date (UTC) | Trigger | Agent | Summary |
|---|---|---|---|---|
| 1 | 2026-11-25T12:00Z | CP6 preparation | orchestrator | PR description for SP-2, with limitations and the declined testing recorded. |
