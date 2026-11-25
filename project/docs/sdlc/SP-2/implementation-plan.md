# Implementation plan: SP-2

Architecture source: architecture.md (revision 2)
Requirements source: requirements.md (revision 2, approved at CP1)
Design review source: design-review.md (round 2). Open findings DR-017 to DR-024 are carried as acceptance notes on the tasks listed in the notes column of the "Finding carry-over" table below. Pending items are marked "provisional, pending human decision".
Status: draft (revision 1)

## Planning rules applied
- Stack: Java 21, Spring Boot, Spring Data JPA, H2 (file mode), Maven for the backend; React with Vite for the frontend; JUnit, Mockito, REST Assured and Playwright for tests.
- cp4_granularity is `task`: each task is one CP4 approval unit. The phase groups below are for readability only and are not approval units.
- Settled decisions applied: DR-019 (recipe logic reads each pantry item's expiry status from the inventory service; the recipe service has no expiry logic of its own); DR-020 (secrets come from environment variables; a dedicated task, T15, covers this).
- Working defaults applied to pending items: REQ-023 tie-break = recipe name ascending, case-insensitive (Locale.ROOT); A-005 "today" = server clock through an injected Clock; A-003 name matching = case-insensitive exact match, no trimming; DR-015 JaCoCo kept for NFR-019.
- Paths are limited to `project/backend/` and `project/frontend/`. No task lists a path under `project/docs/`, `project/.claude/`, `project/sdlc.config.json`, `framework/`, `reference/`, or `.githooks/`.

## Verification prerequisite (human action before Phase 7)
`project/sdlc.config.json` has an empty `verification` list, and `lint_command` and `dependency_audit_command` are null. The build and test commands for the verification gate are not known to the planner and must be supplied by the human in `project/sdlc.config.json` before Phase 7 (verification) can run. Until then, Phase 7 is blocked. The documented commands in T28 are proposals, not the configured gate.

## Phases (readability only; not CP4 approval units)
| Phase | Tasks | Purpose |
|---|---|---|
| P1 Foundation and domain | T1 to T9 | Scaffold, persistence, expiry rules, validation, item services, dashboard, recipe catalogue |
| P2 Recipes and HTTP API | T10 to T15 | Recipe suggestions, error handling, REST endpoints, application shell, secrets |
| P3 Frontend | T16 to T24 | API client, shared components, screens, responsive checks |
| P4 Quality gates | T25 to T28 | Performance, no-AI-call test, coverage gate, build documentation and smoke test |

## Requirement coverage
| Requirement | Tasks |
|---|---|
| REQ-001 | T4, T5, T12, T20 |
| REQ-002 | T7, T12, T19 |
| REQ-003 | T5, T7, T12, T21 |
| REQ-004 | T4, T5, T12, T20, T21 |
| REQ-005 | T5, T12, T21 |
| REQ-006 | T2 |
| REQ-007 | T2, T4, T20 |
| REQ-008 | T6, T7, T12, T19 |
| REQ-009 | T3, T4, T20 |
| REQ-010 | T3, T7, T8, T19 |
| REQ-011 | T3, T6, T7, T8 |
| REQ-012 | T3, T7, T18, T19, T21 |
| REQ-013 | T7, T19, T21 |
| REQ-014 | T8, T13, T22 |
| REQ-015 | T8, T13, T22 |
| REQ-016 | T8, T13, T22 |
| REQ-017 | T8, T13, T22 |
| REQ-018 | T8, T13, T22 |
| REQ-019 | T8, T22 |
| REQ-020 | T10, T13, T23 |
| REQ-021 | T9, T10, T13, T23 |
| REQ-022 | T10, T13, T23 |
| REQ-023 | T10, T13, T23 |
| REQ-024 | T9, T10, T13, T23, T26 |
| REQ-025 | T6, T7, T12, T19 |
| REQ-026 | T3, T6, T7, T12, T19 |
| REQ-027 | T6, T7, T12, T19 |
| REQ-028 | T4, T11, T12, T20 |
| REQ-029 | T4, T11, T12, T20 |
| REQ-030 | T4, T11, T12, T20 |
| REQ-031 | T4, T11, T12, T20 |
| REQ-032 | T11, T14, T16, T18, T21, T22, T23 |
| NFR-001 | T19, T24 |
| NFR-002 | T24 (click-count check); screens in T19, T20, T21 |
| NFR-003 | T6, T7, T25 |
| NFR-004 | T6, T25 |
| NFR-005 | T5, T7, T8, T25 |
| NFR-006 | T2 |
| NFR-007 | T11, T16 |
| NFR-008 | T4, T5 |
| NFR-009 | Code review gate applied to every task (T1 to T28) |
| NFR-010 | T1 (folder separation), T2 (persistence module), T5 (service layer), T16 (frontend API layer) |
| NFR-011 | T3 (shared classifier), T17 (shared API modules), T18 (shared components) |
| NFR-012 | T4, T12 |
| NFR-013 | T11, T15 |
| NFR-014 | T11, T15, T16, T28 |
| NFR-015 | T12, T14 |
| NFR-016 | T18, T19, T24 |
| NFR-017 | T24 |
| NFR-018 | T1, T14, T28 |
| NFR-019 | T1 (JaCoCo measurement set-up, provisional), T27 (coverage gate, provisional) |

## Finding carry-over (design review round 2)
| Finding | Severity (as in design-review.md) | Carried into | Acceptance note |
|---|---|---|---|
| DR-017 | low | T7, T18 | Indicator is rendered from the expiryStatus field of the REST response; the browser does not classify dates. |
| DR-018 | low | T14 | Application shell states the default bind address (localhost) and the H2 console rule. |
| DR-019 | medium | T7, T10 | Recipe logic uses the inventory service for name and status. Recipe uses an Expiring soon item when any active item with a matching name is Expiring soon (assumption for human confirmation). |
| DR-020 | medium | T1, T11, T15, T28 | Secrets from environment variables only; no endpoint returns configuration values; frontend bundle scanned. |
| DR-021 | medium in design-review.md (listed among the low findings in the brief) | T14 | Default profile has the H2 console off; enabled only by explicit dev profile; test that the console is not served in the default profile. |
| DR-022 | low | T3, T4 | Item validator uses the same injected Clock bean as the expiry classifier. |
| DR-023 | low | T11 | Request binding failures return the same 400 shape as validator failures, with field message and correlation ID. |
| DR-024 | low | T5, T12, T16, T20, T21 | Client request timeout with readable message and correlation ID; GET of a missing item returns 404 with readable message. |

## Tasks

### T1: Project scaffold (Maven backend and Vite frontend)
- Requirements: NFR-010, NFR-018, NFR-019
- Depends on: none
- Status: ready
- Batch: P1 Foundation
- Provisional: JaCoCo Maven plugin for NFR-019 coverage measurement, provisional, pending human decision (DR-015). Working default kept.
- Files:
  - project/backend/pom.xml
  - project/backend/src/main/java/com/smartpantry/SmartPantryApplication.java
  - project/backend/src/main/resources/application.properties
  - project/backend/src/test/java/com/smartpantry/SmartPantryApplicationTests.java
  - project/frontend/package.json
  - project/frontend/vite.config.js
  - project/frontend/index.html
  - project/frontend/src/main.jsx
- Scaffold note: 8 files, above the five-file guideline. All are generated skeleton files for the two required stacks; the task is not split because the brief requires Task 1 to cover both.
- Acceptance notes: pom pins the H2 version (DR-016); spring-boot-starter-web, spring-boot-starter-data-jpa, H2 (file mode), spring-boot-starter-test (JUnit 5, Mockito), REST Assured and JaCoCo are declared; application.properties sets port 8080 and an H2 file URL under `./data/` and contains no credential values (DR-020). vite.config.js sets build.outDir to `../backend/src/main/resources/static` with emptyOutDir true (architecture: Vite output served by Spring Boot). Frontend devDependency: @playwright/test. No router, UI library or Vitest is added (outside the Section 5 preferences).
- Test expectations:
  - Happy path: `mvn -B verify` in project/backend builds, runs SmartPantryApplicationTests (context loads against an H2 file in a temporary directory), and writes a JaCoCo report. `npm install` and `npm run build` in project/frontend succeed and write static files to the backend static folder.
  - Not found / missing field: not applicable to a scaffold. A missing dependency or missing main class must make the build fail, which the build run itself verifies.

### T2: Category and grocery item persistence
- Requirements: REQ-006, REQ-007, NFR-006, NFR-010
- Depends on: T1
- Status: blocked by T1
- Batch: P1 Foundation
- Files:
  - project/backend/src/main/java/com/smartpantry/item/Category.java
  - project/backend/src/main/java/com/smartpantry/item/GroceryItem.java
  - project/backend/src/main/java/com/smartpantry/item/ItemRepository.java
  - project/backend/src/test/java/com/smartpantry/item/ItemPersistenceTest.java
- Acceptance notes: Category enum holds exactly Vegetables, Fruits, Dairy, Grains, Snacks, Others (REQ-007), stored as a string. GroceryItem has no active flag: stored items are the active inventory because delete is permanent (DR-008, REQ-005). Quantity is an int. Name, quantity and category are not-null columns; purchase and expiry dates are nullable.
- Test expectations:
  - Happy path: an item with all fields saved to an H2 file in a temporary directory; the context is closed and reopened on the same file; every field value is identical (REQ-006). All six category names persist and round-trip.
  - Not found / missing field: findById for an unknown id returns an empty Optional. Saving an item with a null name, null quantity or null category fails with a data integrity violation. Null purchase and expiry dates persist as null.

### T3: Clock and expiry classifier
- Requirements: REQ-010, REQ-011, REQ-012, REQ-013, REQ-026, NFR-011
- Depends on: T1
- Status: blocked by T1
- Batch: P1 Foundation
- Provisional: "today" = server clock through the injected Clock (A-005), provisional, pending human decision (DR-006). Working default kept; no time zone chosen.
- Files:
  - project/backend/src/main/java/com/smartpantry/config/ClockConfig.java
  - project/backend/src/main/java/com/smartpantry/expiry/ExpiryStatus.java
  - project/backend/src/main/java/com/smartpantry/expiry/ExpiryClassifier.java
  - project/backend/src/test/java/com/smartpantry/expiry/ExpiryClassifierTest.java
- Acceptance notes: ClockConfig exposes one Clock bean (Clock.systemDefaultZone()); the classifier and the item validator (T4) both use this bean (DR-022). ExpiryStatus has EXPIRED, EXPIRING_SOON and VALID; the REST layer sends these names and the frontend supplies labels "Expired", "Expiring soon" and "Valid". Date bounds: EXPIRED is expiry before today; EXPIRING_SOON is today through today plus 3 days inclusive; VALID is expiry on or after today plus 4 days and requires a non-null expiry (DR-005). The window is fixed at 3 days (Q-002).
- Test expectations (fixed Clock in every test):
  - Happy path: expiry yesterday gives EXPIRED; expiry today gives EXPIRING_SOON; today plus 3 gives EXPIRING_SOON; today plus 4 gives VALID. Each status returns the bounds stated above.
  - Not found / missing field: a null expiry date gives VALID, is never EXPIRED, and has no date bound, so it is excluded from every expiry filter.

### T4: Item request and validator
- Requirements: REQ-001, REQ-004, REQ-007, REQ-009, REQ-028, REQ-029, REQ-030, REQ-031, NFR-008, NFR-012
- Depends on: T2, T3
- Status: blocked by T2, T3
- Batch: P1 Foundation
- Provisional: "today" through the injected Clock (A-005), provisional, pending human decision (DR-006). Name whitespace rule: a name made only of whitespace is rejected, but the stored value is not trimmed (A-003 working default). The whitespace-only rule is an assumption pending DR-007 and is listed for human confirmation.
- Files:
  - project/backend/src/main/java/com/smartpantry/item/ItemRequest.java
  - project/backend/src/main/java/com/smartpantry/item/ItemValidator.java
  - project/backend/src/main/java/com/smartpantry/item/ItemValidationException.java
  - project/backend/src/test/java/com/smartpantry/item/ItemValidatorTest.java
- Acceptance notes: ItemRequest is a record with name (String), quantity (Integer), category (Category), purchaseDate and expiryDate (LocalDate, nullable). The validator returns one message per invalid field. Quantity must be a positive whole number (Q-004). Purchase date must not be later than today (uses the Clock bean, DR-022) and must be on or before the expiry date when both are given (Q-005). Duplicate names are not rejected (A-007). Decimal and non-numeric quantities and unparseable dates are rejected at binding in T11 (DR-023), not here.
- Test expectations:
  - Happy path: a fully populated item passes; an item with only name, quantity and category passes; purchase date equal to today passes; purchase date equal to expiry date passes; a duplicate name passes.
  - Not found / missing field: null, empty and whitespace-only name each give a message on "name"; quantity null, 0 and -1 each give a message on "quantity"; null category gives a message on "category"; purchase date tomorrow gives a message on "purchaseDate" (Clock fixed to 2026-11-25); purchase date after expiry date gives a message on "purchaseDate"; several invalid fields give several messages.

### T5: Item service
- Requirements: REQ-001, REQ-003, REQ-004, REQ-005, NFR-005, NFR-008
- Depends on: T4
- Status: blocked by T4
- Batch: P1 Foundation
- Files:
  - project/backend/src/main/java/com/smartpantry/item/ItemNotFoundException.java
  - project/backend/src/main/java/com/smartpantry/item/ItemService.java
  - project/backend/src/test/java/com/smartpantry/item/ItemServiceTest.java
- Acceptance notes: the Item service is the single writer (architecture). Last-write-wins for concurrent edits, no version field (DR-012). A request for a deleted item returns not-found. GET of one item returns the stored entity; the view with status is added in T7. Missing-item GET is a not-found case (DR-024).
- Test expectations (Mockito for the repository and validator):
  - Happy path: create with a valid request calls save once and returns the saved item with its id; update with a valid request replaces the stored fields; delete of an existing item calls delete; get of an existing item returns it; a second update overwrites the first (last-write-wins).
  - Not found / missing field: get, update and delete of an unknown id throw ItemNotFoundException and call neither save nor delete; get after delete throws ItemNotFoundException (DR-012, DR-024); create or update with an invalid request throws ItemValidationException and save is never invoked.

### T6: Repository search query
- Requirements: REQ-008, REQ-011, REQ-025, REQ-026, REQ-027, NFR-004
- Depends on: T2, T3
- Status: blocked by T2, T3
- Batch: P1 Foundation
- Files:
  - project/backend/src/main/java/com/smartpantry/item/ItemRepository.java (modify: add a combined search query)
  - project/backend/src/test/java/com/smartpantry/item/ItemSearchRepositoryTest.java
- Acceptance notes: the query takes nullable parameters: a name term, a category, and expiry date bounds with a flag that says whether an expiry filter applies. Name match is a case-insensitive substring match (Q-007); LIKE wildcards in the user term (% and _) are escaped. When an expiry filter applies, items with a null expiry date are excluded by the query. The repository does not compute status; the bounds come from the expiry classifier through T7. Conditions combine with AND (REQ-027).
- Test expectations (@DataJpaTest on H2):
  - Happy path: name "ap" returns Apple and Grapes; name "MILK" returns Oat milk; name, category and expiry bounds together return only items that meet all three; Expiring soon bounds include today and today plus 3 and exclude today plus 4; no name and no category returns the full inventory.
  - Not found / missing field: a search with no match returns an empty list, not an error; an expiry filter excludes items with a null expiry date (including VALID); with no expiry filter, null-expiry items are included; a name term of "%" or "_" matches only items containing that literal character.

### T7: Item query service and item view
- Requirements: REQ-002, REQ-003, REQ-008, REQ-010, REQ-012, REQ-013, REQ-025, REQ-026, REQ-027, NFR-003, NFR-005
- Depends on: T5, T6
- Status: blocked by T5, T6
- Batch: P1 Foundation
- Provisional: status depends on the server-clock "today" (A-005), provisional, pending human decision (DR-006). Inherited from T3; no separate decision.
- Files:
  - project/backend/src/main/java/com/smartpantry/item/ItemFilter.java
  - project/backend/src/main/java/com/smartpantry/item/ItemView.java
  - project/backend/src/main/java/com/smartpantry/item/ItemQueryService.java
  - project/backend/src/test/java/com/smartpantry/item/ItemQueryServiceTest.java
- Acceptance notes: ItemFilter is a record (name, category, expiryStatus; all nullable). ItemView is a record (id, name, quantity, category, purchaseDate, expiryDate, expiryStatus) so the browser can derive the indicator from the status field (DR-017). The service exposes list(filter), getView(id) and findAllWithStatus(); findAllWithStatus() is the inventory-service method the recipe service uses for name and status (DR-019). The service converts the expiry status filter into bounds through the classifier and does not classify in the repository.
- Test expectations (Mockito for the repository, real classifier with fixed Clock):
  - Happy path: empty filter returns all items with status; category only returns items of that category; combined filters apply AND; getView of an existing item returns the view with status; findAllWithStatus returns every item with its status; an item with null expiry is returned with expiryStatus VALID and null expiryDate.
  - Not found / missing field: getView of an unknown id throws ItemNotFoundException (REQ-003); a filter with no matches returns an empty list; a null filter field is treated as "no condition".

### T8: Dashboard service
- Requirements: REQ-014, REQ-015, REQ-016, REQ-017, REQ-018, REQ-019, NFR-005
- Depends on: T2, T3
- Status: blocked by T2, T3
- Batch: P1 Foundation
- Provisional: status depends on the server-clock "today" (A-005), provisional, pending human decision (DR-006). Inherited from T3.
- Files:
  - project/backend/src/main/java/com/smartpantry/dashboard/DashboardSummary.java (record, with nested CategoryTotal record: category, itemCount, quantityTotal)
  - project/backend/src/main/java/com/smartpantry/dashboard/DashboardService.java
  - project/backend/src/test/java/com/smartpantry/dashboard/DashboardServiceTest.java
- Acceptance notes: the service reads the repository and classifies through the expiry classifier on every call; no caching (REQ-019). Category totals list all six categories in REQ-007 order, including those with zero items. Items with no expiry date count in totals and category totals and are excluded from expiring and expired lists (Q-006).
- Test expectations:
  - Happy path: total count and total quantity equal the seeded items; expiringSoon contains exactly the EXPIRING_SOON items; expired contains exactly the EXPIRED items; category item counts and quantity totals sum to the overall totals; a second call after a repository change returns the updated values.
  - Not found / missing field: an empty inventory returns zero totals and six categories with zero counts (not an error); an item with null expiry counts in totals but is not in the expiring or expired lists.

### T9: Recipe catalogue
- Requirements: REQ-021, REQ-024
- Depends on: T1
- Status: blocked by T1
- Batch: P1 Foundation
- Files:
  - project/backend/src/main/java/com/smartpantry/recipe/RecipeDefinition.java (record: name, List of ingredient names)
  - project/backend/src/main/java/com/smartpantry/recipe/RecipeCatalogue.java
  - project/backend/src/main/resources/recipes.json (about 10 recipes; content is a delivery item authored by the Developer)
  - project/backend/src/test/java/com/smartpantry/recipe/RecipeCatalogueTest.java
- Acceptance notes: the catalogue loads recipes.json with Jackson at startup, validates it, and fails startup with a logged reason on a missing, malformed or empty file (DR-009). It is read-only and exposes no add or edit method (Q-011). Ingredients are plain names with no quantities (A-003).
- Test expectations:
  - Happy path: recipes.json loads with 10 recipes, each with a name and at least one ingredient; the returned list cannot be modified.
  - Not found / missing field: a missing file stops startup with a logged reason; malformed JSON stops startup; an empty array stops startup; a recipe with no name or no ingredients stops startup and the log names the recipe.

### T10: Recipe suggestion service
- Requirements: REQ-020, REQ-021, REQ-022, REQ-023, REQ-024
- Depends on: T7, T9
- Status: blocked by T7, T9
- Batch: P2 Recipes and HTTP API
- Provisional (all three, pending human decision): (1) REQ-023 final tie-break = recipe name ascending, case-insensitive (Locale.ROOT), DR-001. (2) A-003 name matching = case-insensitive exact match, no trimming, DR-007. (3) DR-019 rule, as an assumption for human confirmation: a recipe uses an Expiring soon item when any active item with a matching name is EXPIRING_SOON.
- Files:
  - project/backend/src/main/java/com/smartpantry/recipe/RecipeSuggestion.java (record: name, ingredients, makeable, missingIngredients, usesExpiringSoon)
  - project/backend/src/main/java/com/smartpantry/recipe/RecipeSuggestionService.java
  - project/backend/src/test/java/com/smartpantry/recipe/RecipeSuggestionServiceTest.java
- Acceptance notes: DR-019 is settled: the service takes name and expiry status from ItemQueryService.findAllWithStatus() (the inventory service). It has no dependency on the expiry classifier and no expiry logic of its own. It has no HTTP client dependency (REQ-024). Ranking: first recipes with usesExpiringSoon, then fewest missing ingredients, then recipe name ascending case-insensitive. All matching recipes are returned with no cap (A-012). Note: architecture rev 2 data flow step 9 names the Item repository and Expiry classifier for this service; this plan follows DR-019 instead, and architecture rev 3 should record it.
- Test expectations (Mockito for the inventory service):
  - Happy path: a recipe whose ingredients all match (case-insensitive) is makeable with an empty missing list; a partial match is not makeable and its missing list equals required minus pantry; a recipe using an EXPIRING_SOON item is listed before one that does not, even with more missing ingredients; with equal use, fewer missing ingredients come first; with equal values on both keys, names sort ascending case-insensitive; a name matching both an Expiring soon item and a Valid item counts as an Expiring soon use.
  - Not found / missing field: an empty pantry gives no makeable recipe and each missing list equals the full ingredient list; no recipe matches gives an empty list, not an error; an ingredient with leading or trailing spaces does not match (no trimming).

### T11: API error handling and request binding
- Requirements: REQ-028, REQ-029, REQ-030, REQ-031, REQ-032, NFR-007, NFR-013, NFR-014
- Depends on: T1, T4, T5
- Status: blocked by T1, T4, T5
- Batch: P2 Recipes and HTTP API
- Files:
  - project/backend/src/main/java/com/smartpantry/api/ApiErrorResponse.java (record: correlationId, message, fieldErrors map)
  - project/backend/src/main/java/com/smartpantry/api/ApiErrorHandler.java
  - project/backend/src/main/java/com/smartpantry/api/CorrelationIdFilter.java
  - project/backend/src/main/java/com/smartpantry/api/JacksonConfig.java
  - project/backend/src/test/java/com/smartpantry/api/ApiErrorHandlerTest.java
- Acceptance notes: every handled failure is logged to standard output with its correlation ID, and the same ID is returned to the client; logs and responses carry no secrets (DR-011). Request binding failures (HttpMessageNotReadableException and type mismatch) return the same 400 shape with the field name, a message and the correlation ID (DR-023). JacksonConfig disables float-to-int coercion so quantity 1.5 is rejected (REQ-029). DataAccessException returns a sanitized 503 (DR-009). Unexpected exceptions return a sanitized 500. No stack trace, SQL, class name or configuration value reaches the client (REQ-032, NFR-013).
- Test expectations (MockMvc with a test-only controller inside the test class, because the item endpoints come in T12):
  - Happy path: ItemValidationException gives 400 with fieldErrors and a correlationId that also appears in the log line; ItemNotFoundException gives 404 with a readable message.
  - Not found / missing field: quantity 1.5 gives 400 with a message on "quantity"; quantity "abc" gives 400; purchaseDate "2026-02-30" gives 400 on "purchaseDate"; category "Meat" gives 400 on "category"; an empty request body gives 400 with a readable message; an induced DataAccessException gives 503 with no SQL, class name or stack trace; an induced RuntimeException gives 500 with no stack trace or class name.

### T12: Item REST endpoints
- Requirements: REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-008, REQ-025, REQ-026, REQ-027, REQ-028, REQ-029, REQ-030, REQ-031, NFR-012, NFR-015
- Depends on: T5, T7, T11
- Status: blocked by T5, T7, T11
- Batch: P2 Recipes and HTTP API
- Files:
  - project/backend/src/main/java/com/smartpantry/item/ItemController.java
  - project/backend/src/test/java/com/smartpantry/item/ItemControllerTest.java
- Acceptance notes: endpoints are POST /api/items (201), GET /api/items with query parameters name, category and expiryStatus (all optional), GET /api/items/{id}, PUT /api/items/{id} and DELETE /api/items/{id} (204). An unknown expiryStatus value returns 400. No authentication is required (NFR-015). DR-024: GET of a missing item returns 404 with a readable message.
- Test expectations (REST Assured, random port, H2 temporary file):
  - Happy path: create returns 201 and the item appears in GET /api/items; GET by id returns 200; PUT returns the new values; DELETE returns 204 and the item is gone from the list; combined name, category and expiryStatus filters return only matching items.
  - Not found / missing field: GET, PUT and DELETE of an unknown id return 404 with a readable message; GET after DELETE returns 404 (DR-012); POST with a missing name, quantity 0, quantity 1.5, an invalid category, or an unknown expiryStatus returns 400 with field messages and the item count is unchanged; requests succeed with no credentials.

### T13: Dashboard and recipe endpoints
- Requirements: REQ-014, REQ-015, REQ-016, REQ-017, REQ-018, REQ-019, REQ-020, REQ-021, REQ-022, REQ-023, REQ-024, NFR-005
- Depends on: T8, T10, T11
- Status: blocked by T8, T10, T11
- Batch: P2 Recipes and HTTP API
- Provisional: the ranking assertions depend on the REQ-023 tie-break and the A-003 matching rule (T10), provisional, pending human decision (DR-001, DR-007).
- Files:
  - project/backend/src/main/java/com/smartpantry/dashboard/DashboardController.java
  - project/backend/src/main/java/com/smartpantry/recipe/RecipeController.java
  - project/backend/src/test/java/com/smartpantry/api/DashboardRecipeApiTest.java
- Acceptance notes: endpoints are GET /api/dashboard and GET /api/recipes only. No create, update or delete endpoint exists for recipes (REQ-024).
- Test expectations (REST Assured):
  - Happy path: dashboard totals match the seeded inventory; recipes response includes each recipe's full ingredient list, makeable flag, missing list and ordering per T10.
  - Not found / missing field: an empty inventory returns zero totals and six categories with zero counts; a pantry matching no recipe returns an empty list; POST /api/recipes, PUT /api/recipes/{id} and DELETE /api/recipes/{id} return 404 or 405; no response contains internal details.

### T14: Application shell, profiles and frontend serving
- Requirements: NFR-015, NFR-018, REQ-032
- Depends on: T1, T11
- Status: blocked by T1, T11
- Batch: P2 Recipes and HTTP API
- Files:
  - project/backend/src/main/resources/application.properties (modify: bind address, console off by default)
  - project/backend/src/main/resources/application-dev.properties
  - project/backend/src/main/java/com/smartpantry/config/WebConfig.java
  - project/backend/src/test/java/com/smartpantry/config/ApplicationShellTest.java
- Acceptance notes (DR-013, DR-018, DR-021): the default bind address is localhost, set as server.address=${SMARTPANTRY_BIND_ADDRESS:localhost}. The default profile is the one used when no profile is set, including by the documented run command in T28. The H2 console is off in the default profile and is enabled only by the dev profile (SPRING_PROFILES_ACTIVE=dev), which sets spring.h2.console.enabled=true. WebConfig forwards non-API paths to index.html. Unknown /api paths return JSON 404, not HTML.
- Test expectations:
  - Happy path: the default start serves the built main screen at "/"; the dev profile serves the H2 console path; with no bind variable set the server binds to localhost.
  - Not found / missing field: /h2-console returns 404 in the default profile (DR-021); /inventory returns index.html (deep link); /api/unknown returns JSON 404 with no stack trace; startup with SMARTPANTRY_BIND_ADDRESS unset uses the localhost default.

### T15: Secret handling from environment variables
- Requirements: NFR-014, NFR-013
- Depends on: T11, T14
- Status: blocked by T11, T14
- Batch: P2 Recipes and HTTP API
- Files:
  - project/backend/src/main/resources/application.properties (modify: datasource credentials from environment)
  - project/backend/.gitignore
  - project/backend/src/test/java/com/smartpantry/config/SecretHandlingTest.java
- Acceptance notes (DR-020, settled): spring.datasource.username=${SMARTPANTRY_DB_USER:sa} and spring.datasource.password=${SMARTPANTRY_DB_PASSWORD:} (an empty default is not a credential). No credential literal is committed anywhere under project/. .gitignore excludes application-local.properties, .env files, the data/ directory and the H2 database files (*.mv.db, *.trace.db). No endpoint returns configuration values, and logs exclude secrets (DR-011).
- Test expectations:
  - Happy path: a value supplied through SMARTPANTRY_DB_PASSWORD is applied to the datasource (asserted through the Spring Environment), and the application starts.
  - Not found / missing field: with SMARTPANTRY_DB_PASSWORD unset, the application starts with the empty default; an induced error response body contains neither the password value nor the JDBC URL; a file-content check confirms .gitignore lists the patterns above; a scan of src/main/resources finds no literal password.

### T16: Frontend HTTP client
- Requirements: REQ-032, NFR-007, NFR-014
- Depends on: T1
- Status: blocked by T1
- Batch: P3 Frontend
- Acceptance notes: the client calls relative /api paths (same origin, no CORS). Request timeout is 10 seconds; this value is a planner default, not a requirement, and is listed in assumptions (DR-024). Every failure maps to a readable message. The correlation ID from the response is shown when present (DR-011). Raw bodies, stack traces, URLs and configuration values are never displayed.
- Files:
  - project/frontend/src/api/httpClient.js
  - project/frontend/playwright.config.js
  - project/frontend/tests/api-client.spec.js
- Test expectations (Playwright with page.route interception):
  - Happy path: a 2xx JSON response is returned parsed; a 400 response with fieldErrors is returned as a structured error.
  - Not found / missing field: a 404 shows a readable message; an unreachable backend (aborted route) shows "Cannot reach the server. Try again."; a stalled request beyond 10 seconds shows a readable timeout message with the correlation ID when one is present; a 500 whose body contains a stack trace shows only the readable message and correlation ID.

### T17: Frontend API modules
- Requirements: REQ-002, REQ-003, REQ-004, REQ-005, REQ-014, REQ-015, REQ-016, REQ-017, REQ-018, REQ-019, REQ-020, REQ-022, REQ-023, REQ-025, REQ-026, REQ-027, NFR-011
- Depends on: T13, T16
- Status: blocked by T13, T16
- Batch: P3 Frontend
- Files:
  - project/frontend/src/api/itemsApi.js
  - project/frontend/src/api/dashboardApi.js
  - project/frontend/src/api/recipesApi.js
  - project/frontend/tests/api-modules.spec.js
- Test expectations (Playwright with page.route):
  - Happy path: listItems sends name, category and expiryStatus query parameters only when set; createItem and updateItem send JSON with the right method; deleteItem sends DELETE; getDashboard and getRecipes call their endpoints.
  - Not found / missing field: getItem for an unknown id rejects with a readable not-found error (DR-024); listItems with an empty filter sends no query parameters, so the full inventory is requested.

### T18: Shared UI components and responsive base styles
- Requirements: REQ-012, REQ-032, NFR-009, NFR-011, NFR-016
- Depends on: T16
- Status: blocked by T16
- Batch: P3 Frontend
- Acceptance notes: ExpiryBadge renders from the expiryStatus field returned by the REST response; it does no date arithmetic (DR-017). Each status has a distinct text label and colour: "Expired", "Expiring soon", "Valid". ErrorMessage shows the readable message and the correlation ID only. Base CSS includes breakpoints for 1280px and 375px.
- Files:
  - project/frontend/src/components/ExpiryBadge.jsx
  - project/frontend/src/components/ErrorMessage.jsx
  - project/frontend/src/components/ConfirmDialog.jsx
  - project/frontend/src/styles/app.css
  - project/frontend/tests/components.spec.js
- Test expectations (Playwright, driving the running app with mocked API responses):
  - Happy path: each of the three statuses renders distinct text and class; ErrorMessage shows the readable text and correlation ID; ConfirmDialog calls onConfirm on confirm and onCancel on cancel.
  - Not found / missing field: a missing status renders no indicator and does not crash the page; ErrorMessage with no correlation ID shows the message only.

### T19: Inventory list and search and filter screen
- Requirements: REQ-002, REQ-008, REQ-012, REQ-013, REQ-025, REQ-026, REQ-027, NFR-001, NFR-002, NFR-016
- Depends on: T17, T18
- Status: blocked by T17, T18
- Batch: P3 Frontend
- Acceptance notes: App.jsx switches screens through state, with no router, to avoid a dependency outside the preferences. Search, category and expiry controls are on the inventory screen and apply to the same list (REQ-027). Rows show name, quantity, category, expiry date and status badge, or "No expiry date" with the Valid badge.
- Files:
  - project/frontend/src/App.jsx
  - project/frontend/src/pages/InventoryPage.jsx
  - project/frontend/tests/inventory.spec.js
- Test expectations (Playwright):
  - Happy path: rows show all listed fields; search "ap" shows Apple and Grapes; search "MILK" shows Oat milk; search, category and expiry filters together show only matching items; an empty search shows the full list.
  - Not found / missing field: no match shows "No items match your search"; an unreachable backend shows a readable message; an item with no expiry date shows "No expiry date" and the Valid badge.

### T20: Add and edit item form
- Requirements: REQ-001, REQ-004, REQ-007, REQ-009, REQ-028, REQ-029, REQ-030, REQ-031, NFR-002
- Depends on: T17, T18, T19
- Status: blocked by T17, T18, T19
- Batch: P3 Frontend
- Acceptance notes: frontend validation mirrors the backend rules, but the backend is authoritative (REQ-028 to REQ-031). Server-side fieldErrors are shown next to each field. Date inputs use the browser date control.
- Files:
  - project/frontend/src/components/ItemForm.jsx (shared by add and edit)
  - project/frontend/src/pages/ItemFormPage.jsx
  - project/frontend/src/validation/itemValidation.js
  - project/frontend/src/App.jsx (modify: add and edit routes)
  - project/frontend/tests/item-form.spec.js
- Test expectations (Playwright, request counters on page.route):
  - Happy path: a valid add is saved and appears in the inventory list; a valid edit shows the new values; a duplicate name is accepted.
  - Not found / missing field: a missing name, quantity 0, quantity 1.5, missing category, purchase date after today, purchase date after expiry, and an invalid date each show a field message and send no request; a 400 response with fieldErrors is shown per field; editing an item deleted elsewhere shows a readable not-found message (DR-024).

### T21: Item detail and delete
- Requirements: REQ-003, REQ-004, REQ-005, REQ-012, REQ-013, REQ-032
- Depends on: T12, T18, T20
- Status: blocked by T12, T18, T20
- Batch: P3 Frontend
- Files:
  - project/frontend/src/pages/ItemDetailPage.jsx
  - project/frontend/src/App.jsx (modify: detail route)
  - project/frontend/tests/item-detail.spec.js
- Test expectations (Playwright):
  - Happy path: the detail view shows name, quantity, category, purchase date, expiry date and status badge; an item with no expiry date shows "No expiry date" and the Valid badge; the edited values show after save; Confirm on the delete prompt removes the item from the list; Cancel leaves the item unchanged.
  - Not found / missing field: opening the detail of an item deleted in another tab shows a readable "This item no longer exists" message with no stack trace (DR-024); a failed delete shows a readable message and keeps the item.

### T22: Dashboard screen
- Requirements: REQ-014, REQ-015, REQ-016, REQ-017, REQ-018, REQ-019, REQ-032
- Depends on: T17, T18, T19
- Status: blocked by T17, T18, T19
- Batch: P3 Frontend
- Acceptance notes: the dashboard fetches on every view and has no cache or refresh control (REQ-019).
- Files:
  - project/frontend/src/pages/DashboardPage.jsx
  - project/frontend/src/App.jsx (modify: dashboard route)
  - project/frontend/tests/dashboard.spec.js
- Test expectations (Playwright):
  - Happy path: the count equals the inventory list count; total quantity matches; expiring and expired lists match the badges in the inventory; the category table has six rows whose counts and quantities sum to the totals; after an add, edit or delete, the next dashboard view shows updated values with no refresh control.
  - Not found / missing field: an empty inventory shows zero totals and six category rows with zero; a failed dashboard request shows a readable message.

### T23: Recipe suggestions screen
- Requirements: REQ-020, REQ-021, REQ-022, REQ-023, REQ-024, REQ-032
- Depends on: T17, T18, T19
- Status: blocked by T17, T18, T19
- Batch: P3 Frontend
- Provisional: the display order follows the backend ranking in T10, which uses the REQ-023 tie-break and the A-003 rule, provisional, pending human decision (DR-001, DR-007).
- Files:
  - project/frontend/src/pages/RecipesPage.jsx
  - project/frontend/src/App.jsx (modify: recipes route)
  - project/frontend/tests/recipes.spec.js
- Test expectations (Playwright):
  - Happy path: makeable recipes carry a "Makeable" label; each recipe shows its full ingredient list; not-makeable recipes list their missing ingredients; the order matches the backend order (provisional).
  - Not found / missing field: no add, edit or delete recipe control exists on the page (REQ-024); no match shows "No recipes match your pantry right now"; a backend error shows a readable message.

### T24: Responsive and interaction checks
- Requirements: NFR-001, NFR-002, NFR-016, NFR-017
- Depends on: T19, T20, T21, T22, T23
- Status: blocked by T19, T20, T21, T22, T23
- Batch: P3 Frontend
- Files:
  - project/frontend/playwright.config.js (modify: browser projects)
  - project/frontend/src/styles/app.css (modify: breakpoints as needed)
  - project/frontend/tests/responsive.spec.js
- Test expectations (Playwright):
  - Happy path: at 1280px and 375px width, each core screen (inventory, detail, add and edit, dashboard, recipes) shows all controls, with no overlap, no clipped control and no horizontal scroll; projects run on Chromium, Firefox and Edge (latest); add, edit, delete, search and filter each complete in 3 clicks or fewer from the inventory screen (NFR-002).
  - Not found / missing field: an empty inventory still shows every control at both widths; a clipped control at 375px fails the spec.

### T25: Performance test at 1,000 items
- Requirements: NFR-003, NFR-004, NFR-005
- Depends on: T12
- Status: blocked by T12
- Batch: P4 Quality gates
- Files:
  - project/backend/src/test/java/com/smartpantry/performance/InventoryPerformanceTest.java
- Test expectations (REST Assured, timed after one warm-up call):
  - Happy path: with 1,000 seeded active items (mixed categories, statuses and some null expiry dates), the inventory list, each search and each filter response arrives within 1 second.
  - Not found / missing field: a search with no matches over 1,000 items returns an empty list within 1 second.

### T26: No external AI call test
- Requirements: REQ-024
- Depends on: T13
- Status: blocked by T13
- Batch: P4 Quality gates
- Files:
  - project/backend/src/test/java/com/smartpantry/recipe/RecipeNoExternalCallTest.java
- Test expectations:
  - Happy path: GET /api/recipes returns 200 with the expected results while outbound HTTP is pointed at an unreachable proxy address (the application makes no outbound call); a source scan of the recipe package finds no import of java.net.http, RestTemplate, WebClient or an AI SDK.
  - Not found / missing field: a scan of application*.properties finds no AI endpoint or AI API key property; suggestions are unchanged when no AI service is available.

### T27: Coverage gate (JaCoCo)
- Requirements: NFR-019, NFR-018
- Depends on: T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T25, T26
- Status: blocked by T2 to T15, T25, T26
- Batch: P4 Quality gates
- Provisional: JaCoCo is not in the Section 5 preferences and is pending human decision (DR-015). If the human rejects it, NFR-019's 80% criterion cannot be measured with the current stack; a replacement tool needs a new task.
- Files:
  - project/backend/pom.xml (modify: jacoco:check rule at 0.80 line coverage for the backend bundle)
- Test expectations:
  - Happy path: `mvn -B verify` runs all backend unit and integration tests with zero failures, and jacoco:check passes at 80% line coverage; the report is written under target/site/jacoco.
  - Not found / missing field: when the threshold is raised above actual coverage, the build fails (verified once locally; the change is not committed); frontend Playwright tests are not part of the backend coverage figure.

### T28: Build documentation and end-to-end smoke test
- Requirements: NFR-018, NFR-014, NFR-015
- Depends on: T15, T24, T27
- Status: blocked by T15, T24, T27
- Batch: P4 Quality gates
- Proposed build commands (not the configured gate; see verification prerequisite): `npm ci` and `npm run build` in project/frontend, then `mvn -B verify` in project/backend, then `java -jar` for the packaged jar.
- Files:
  - project/backend/README.md
  - project/frontend/tests/smoke.spec.js
- Test expectations:
  - Happy path: the documented build completes; the default-profile application starts and serves the inventory screen at http://localhost:8080; Playwright smoke test loads the main screen and adds one item.
  - Not found / missing field: a scan of the built static bundle finds no value of SMARTPANTRY_DB_PASSWORD (DR-020, NFR-014); GET /api/unknown returns JSON 404 (T14); the H2 console path returns 404 in the default profile (DR-021).

## Provisional items and open decisions
| Item | Working default in use | Tasks | Status |
|---|---|---|---|
| REQ-023 tie-break (DR-001) | Recipe name ascending, case-insensitive (Locale.ROOT) | T10, T13, T23 | provisional, pending human decision |
| A-005 "today" (DR-006) | Server clock through injected Clock; no named time zone | T3, T4, T7, T8 | provisional, pending human decision |
| A-003 name matching (DR-007) | Case-insensitive exact match, no trimming; whitespace-only names rejected on create and update | T4, T10 | provisional, pending human decision |
| JaCoCo for NFR-019 (DR-015) | JaCoCo Maven plugin | T1, T27 | provisional, pending human decision |
| DR-019 recipe expiry rule | Recipe uses an Expiring soon item when any active item with a matching name is Expiring soon | T7, T10 | assumption for human confirmation |

No task is blocked by a human decision. Provisional tasks proceed on the working defaults above.

## Batches (readability only)
cp4_granularity is `task`, so these phases are not approval units. Each phase is a grouping of tasks; each task is approved on its own at CP4.

| Batch | Tasks | Independently testable because |
|---|---|---|
| P1 Foundation and domain | T1 to T9 | Backend domain, persistence and services are testable with unit and data-JPA tests without the HTTP layer |
| P2 Recipes and HTTP API | T10 to T15 | Each endpoint is testable with REST Assured or MockMvc against the P1 services |
| P3 Frontend | T16 to T24 | Playwright tests use mocked routes for screens, and the full app for the responsive checks |
| P4 Quality gates | T25 to T28 | Performance, no-AI-call, coverage and smoke checks run against the completed application |

## Revision log
| Rev | Date (UTC) | Trigger | Agent | Summary |
|---|---|---|---|---|
| 1 | 2026-11-25T00:00Z | initial | implementation-planner | Initial plan from architecture rev 2 and design review round 2: 28 tasks in 4 phases (T1 scaffold, T28 smoke and build docs). All 32 REQ and 19 NFR IDs covered. DR-019 and DR-020 applied as settled; DR-017, DR-018, DR-021 to DR-024 carried as acceptance notes. Provisional items: REQ-023 tie-break, A-005, A-003, JaCoCo. Verification commands are missing from sdlc.config.json and must be supplied before Phase 7. |
