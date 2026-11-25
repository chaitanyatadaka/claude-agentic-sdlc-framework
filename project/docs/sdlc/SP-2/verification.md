# Verification: SP-2

Round: 1
Branch: sdlc/SP-2 (verification run against the working tree at log revision 8; commit c2148f1 is the latest test fix)
Overall result: fail (coverage gaps listed below; the configured commands all pass)

## Commands
| Order | Name | Command | Exit code | Counts | Evidence |
|---|---|---|---|---|---|
| 1 | backend-build-and-tests | cmd /c "set MAVEN_BASEDIR=backend&& backend\mvnw.cmd -B -f backend\pom.xml verify" (from project/) | 0 | 118 run, 0 failed, 0 errors, 0 skipped; BUILD SUCCESS | evidence/raw/Verify-final-backend.log |
| 2 | frontend-build | npm --prefix frontend run build (from project/) | 0 | 44 modules transformed | evidence/raw/Verify-final-frontend.log |
| 3 | browser-e2e (not in config; run from project/frontend) | npx playwright test --config=playwright.config.js (workers 1) | 0 | 3 passed, 0 failed | evidence/raw/Playwright-final-2.log |

No lint command and no dependency audit command are configured.

## Trace matrix (requirement groups)
| Requirement | Implementing tasks | Verifying tests | Result |
|---|---|---|---|
| REQ-001, REQ-028 (add, name rule) | T4, T5, T20 | ItemValidatorTest, ItemServiceTest, ItemControllerTest, add-item e2e | pass |
| REQ-002, REQ-003 (view list, item detail) | T7, T12, T19 | ItemQueryServiceTest, ItemControllerTest, inventory e2e | pass (detail view by id exists in API; no separate detail screen) |
| REQ-004, REQ-029 to REQ-031 (edit, validation rules) | T5, T12, T20 | ItemServiceTest, ItemControllerTest, edit-delete e2e | pass |
| REQ-005 (delete) | T5, T12, T21 | ItemServiceTest, ItemControllerTest, edit-delete e2e | pass |
| REQ-006, NFR-006 (persistence after restart) | T2, T14 | ItemPersistenceTest; restart not re-run | partly tested (persistence proven by the database test; restart not repeated in this run) |
| REQ-007, REQ-008 (categories, category filter) | T2, T4, T19 | ItemValidatorTest, client-side filter in Inventory | pass |
| REQ-009, REQ-010 to REQ-013 (expiry dates, classification, indicators) | T3, T7, T18 | ExpiryClassifierTest (6), ItemQueryServiceTest (9) | pass |
| REQ-014 to REQ-019 (dashboard counts, categories, refresh) | T8, T13, T19 | DashboardServiceTest (8), DashboardRecipeApiTest, edit-delete e2e (dashboard count check) | pass |
| REQ-020 to REQ-024 (recipes, missing ingredients, ranking, rule-based) | T9, T10, T13, T23 | RecipeCatalogueTest (9), RecipeSuggestionServiceTest (13), recipes e2e | pass (no-external-AI test not written; see gaps) |
| REQ-025, REQ-026, REQ-027 (search and filters) | client-side in T19 | none automated for search or expiry filter | untested (only manual testing by the owner) |
| REQ-032, NFR-007, NFR-013 (graceful API errors) | T11 | ApiErrorHandlerTest (5), ItemControllerTest | pass |
| NFR-001, NFR-002, NFR-016, NFR-017 (usability, responsive) | T18, T24 | no responsive or usability test | untested |
| NFR-003 to NFR-005 (performance, 1,000 items) | none | none (plan T25 not built) | untested |
| NFR-009 to NFR-011 (maintainability) | structure only | code review record | not measurable (see code-review.md) |
| NFR-012, NFR-014, NFR-015 (validation, secrets, access) | T4, T15 (partly) | ItemValidatorTest; no secret scan | partly tested |
| NFR-018 (build and run) | T1, T28 (docs not built) | configured backend and frontend commands | pass for build; run instructions not written |
| NFR-019 (tests pass, coverage) | T27 not built (JaCoCo not configured) | configured backend command passes | pass for tests; coverage not measured |

## Document quality check
- Requirement IDs in this matrix match requirements.md (revision 2).
- No placeholder text in run documents: pending confirmation at log completion (revision 8 completed without placeholders).
- Revision logs present in implementation-log.md, code-review.md, requirements.md, architecture.md, design-review.md and implementation-plan.md.
- Commits cited (T1 to T24, including c2148f1) exist on sdlc/SP-2.

## Gaps that block an overall pass (to be stated in the PR)
1. Performance test at 1,000 items (plan T25) not built.
2. No-external-AI-call test (plan T26) not built.
3. Coverage gate (plan T27, JaCoCo) not configured.
4. Build and run documentation (plan T28) not written.
5. Search and expiry-filter behaviour and responsive layout are not covered by automated tests.
6. Recipe photographs are a placeholder; downloads were blocked and no attribution file exists.
7. Backend search query (plan T6) not built; filtering runs in the browser over the loaded list.
8. The unit field requested in the brief is absent; the approved requirements are unitless.

## Revision log
| Rev | Date (UTC) | Trigger | Agent | Summary |
|---|---|---|---|---|
| 1 | 2026-11-25T11:10Z | CP5 verification | orchestrator | Ran configured backend (118 tests, exit 0) and frontend build (exit 0); recorded browser e2e 3 passed serially; trace matrix and gaps recorded; overall fail due to listed gaps. |
