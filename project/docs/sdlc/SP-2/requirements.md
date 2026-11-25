# Requirements: SP-2

Source: input/story.md (snapshot hash recorded by the orchestrator)
Status: draft (revision 2)

## Summary
Smart Pantry Organizer is a web application for an individual household to manage its pantry inventory. It covers grocery item CRUD, category organisation, expiry tracking, a dashboard summary, rule-based recipe suggestions from predefined recipe data, and search and filtering. Revision 2 applies the human decisions on Q-001 to Q-022 (accepted defaults). Each decision is recorded as "Decided" against the affected requirement or assumption, and no open questions remain.

## Functional requirements
| ID | Requirement | Acceptance criteria |
|---|---|---|
| REQ-001 | The application lets a user add a new grocery item with item name, quantity, category, purchase date and expiry date. (FR-01, AC-01) | Mandatory fields are item name, quantity and category; purchase date and expiry date are optional (Decided Q-001). A valid item submitted through the add form is saved and appears in the inventory list. An item missing a mandatory field, or with an invalid value, is not saved and the user sees a validation message for each invalid field. Duplicate item names are accepted (Decided Q-020, A-007). Purchase date cannot be later than today, and must be on or before the expiry date when both are given (Decided Q-005). |
| REQ-002 | The application lets a user view all saved grocery items. (FR-01, AC-02) | The inventory list shows every saved active item, and each row shows the item's name, quantity, category and expiry information (expiry date and status, or "No expiry date" where none was given). |
| REQ-003 | The application lets a user view the details of an individual grocery item. (FR-01) | Selecting an item opens a detail view showing all stored fields of that item (name, quantity, category, purchase date, expiry date, expiry status). |
| REQ-004 | The application lets a user edit the details of an existing grocery item. (FR-01, AC-03) | After editing and saving a valid change, the new values are shown in the item detail view and the inventory list. An invalid edit is rejected with the same mandatory-field and validation rules as REQ-001, including the purchase-date rules (Decided Q-001, Q-005). |
| REQ-005 | The application lets a user delete an existing grocery item. (FR-01, AC-04) | Selecting delete shows a confirmation prompt; cancelling leaves the item unchanged. Confirming deletes the item permanently, with no recovery (Decided Q-012). After deletion the item no longer appears in the inventory list, search results, filter results or dashboard counts. |
| REQ-006 | Grocery items are persisted in a database. (NFR-03, FR-01) | An item added or edited is still present with identical field values after the application is restarted. |
| REQ-007 | Every grocery item belongs to exactly one category from the set Vegetables, Fruits, Dairy, Grains, Snacks, Others. (FR-02, AC-05, BR-5) | Saving an item with no category, or with a category outside the set, is rejected with a validation message; each item's category is displayed in the list and detail view. Category is mandatory (Decided Q-001). |
| REQ-008 | The application lets a user filter pantry items by category. (FR-02, AC-05) | Selecting a category shows only active items whose category equals the selected category; selecting no category shows all items. The category filter combines with search and expiry filters using AND (Decided Q-008, see REQ-027). |
| REQ-009 | The application accepts an expiry date entered by the user when adding or editing an item. (FR-03, BR-3) | Expiry date is optional (Decided Q-001). A valid calendar date is accepted and displayed in the item's expiry information; a value that is not a valid date is rejected with a validation message. When both are given, expiry date must be on or after purchase date (Decided Q-005). |
| REQ-010 | The application identifies expired items. (FR-03, AC-06, BR-4) | An active item whose expiry date is earlier than today is classified as Expired. An expiry date equal to today is Expiring soon, not Expired (Decided Q-003). An item with no expiry date is never Expired (Decided Q-006). |
| REQ-011 | The application identifies items approaching their expiry date. (FR-03, AC-06) | An active item whose expiry date is today or within 3 days after today is classified as Expiring soon. The window is fixed at 3 days and is not user-configurable (Decided Q-002). Example: expiry date today + 3 days is Expiring soon; today + 4 days is Valid. |
| REQ-012 | The application shows a visual indicator that distinguishes Expired, Expiring soon and Valid items. (FR-03, BR-4) | Each of the three statuses renders a different indicator (for example a different colour or label) in the inventory list and detail view, and an item's status can be read from the indicator without opening it. An item with no expiry date shows the Valid indicator (Decided Q-006). |
| REQ-013 | The application displays expiry information clearly on item views. (FR-03, AC-06) | The inventory list and detail view each show the expiry date and the expiry status for every item that has an expiry date. An item with no expiry date shows "No expiry date" and status Valid (Decided Q-006). |
| REQ-014 | The dashboard shows the total number of active grocery items. (FR-04, AC-07) | The dashboard count equals the number of active items in the inventory list. |
| REQ-015 | The dashboard shows total inventory quantity. (FR-04) | The dashboard total equals the sum of quantities of all active items (Decided Q-004). Quantity is a unitless positive whole number, so no unit conversion is applied and no items are excluded. |
| REQ-016 | The dashboard shows the items approaching expiry. (FR-04, AC-07) | The dashboard lists or counts exactly the active items classified as Expiring soon under REQ-011. Items with no expiry date are excluded (Decided Q-006). |
| REQ-017 | The dashboard shows the expired items. (FR-04, AC-07) | The dashboard lists or counts exactly the active items classified as Expired under REQ-010. |
| REQ-018 | The dashboard shows a category-wise inventory summary. (FR-04, AC-07) | For each category in REQ-007, the summary shows the item count and quantity total, and the category totals sum to the overall totals in REQ-014 and REQ-015. Items with no expiry date are included in category totals. |
| REQ-019 | Changes to inventory are reflected in the dashboard. (BR-7, AC-07) | The dashboard reads current inventory data every time it is viewed (Decided Q-018). After an item is added, edited or deleted, the dashboard values shown on the next view match the updated inventory, with no refresh action needed. |
| REQ-020 | The application suggests recipes that can be prepared from the ingredients currently in the pantry. (FR-05, AC-08) | A recipe is flagged as makeable only when every ingredient in its definition matches an active pantry item (Decided Q-009: all ingredients are required). Ingredient names match case-insensitively on the exact name (Decided Q-009; see A-003). A recipe with at least one unmatched ingredient is not flagged as makeable. |
| REQ-021 | For each suggested recipe, the application displays the full list of ingredients required. (FR-05) | Each suggested recipe view shows every ingredient in that recipe's predefined definition. |
| REQ-022 | For each recipe that is not fully makeable, the application identifies the missing ingredients. (FR-05, AC-08) | The missing list for a recipe equals its required ingredients minus the active pantry items (matched as in REQ-020); a fully makeable recipe reports no missing ingredients. |
| REQ-023 | Recipe suggestions give priority to recipes that use pantry items approaching expiry. (FR-05) | Ranking (Decided Q-010): first, recipes that use at least one active Expiring soon item; then fewest missing ingredients. Test: of two recipes differing only in Expiring soon use, the one using such an item is listed first; with equal use, the recipe with fewer missing ingredients is listed first. All matching recipes are shown with no cap on the count (see A-012). |
| REQ-024 | Recipe suggestions use rule-based matching against predefined recipe data, with no call to an external AI API. (FR-05, BR-6, BR-8, Section 9) | Recipes come from a predefined data file shipped with the application, containing about 10 recipes (Decided Q-011). The application provides no screen or endpoint to add or edit recipes (Decided Q-011). A test confirms that suggestion generation makes no external AI API call and works with no external AI service available. |
| REQ-025 | The application lets a user search grocery items by name. (FR-06, AC-09) | Name search is case-insensitive and matches substrings (Decided Q-007): searching "ap" returns "Apple" and "Grapes", and searching "MILK" returns "Oat milk". Only active items are returned. An empty search returns the full inventory. |
| REQ-026 | The application lets a user filter items by expiry status. (FR-06, AC-09) | Selecting Expired, Expiring soon or Valid shows only active items with that status, as defined by REQ-010 and REQ-011. Items with no expiry date are excluded from the expiry filters, including Valid (Decided Q-006). |
| REQ-027 | Search and filter results are shown in the same view as the inventory, without requiring the user to page through the full inventory. (FR-06) | After one search or filter action, the matching items are displayed on the inventory screen with no separate navigation step. Search, category and expiry filters combine with AND (Decided Q-008): for example, "milk" with Dairy and Expiring soon shows only active items that meet all three conditions. |
| REQ-028 | The backend rejects an item whose name is missing or empty. (BR-1, NFR-05, AC-10) | A create or update request with an empty or missing item name is rejected by the backend with an error response and the item is not stored, even when the frontend validation is bypassed. |
| REQ-029 | The backend rejects an item whose quantity is not a positive whole number. (BR-2, NFR-05, AC-10) | A create or update request with a missing, zero, negative, decimal (for example 1.5) or non-numeric quantity is rejected by the backend with an error response and the item is not stored. Quantity is a positive whole number, unitless (Decided Q-004). |
| REQ-030 | The backend rejects an item with an invalid date. (BR-3, NFR-05, AC-10) | A create or update request is rejected by the backend with an error response, and the item is not stored, when: a date is not a valid calendar date; the purchase date is later than today; or the purchase date is later than the expiry date (Decided Q-005). |
| REQ-031 | The backend rejects an item whose category is not in the REQ-007 set. (BR-5, NFR-05, AC-10) | A create or update request with a missing category or a category outside the allowed set is rejected by the backend with an error response and the item is not stored. |
| REQ-032 | API errors are handled gracefully and shown to the user as readable messages. (AC-10, NFR-03, NFR-05) | When an API call fails (for example, the backend returns an error or is unreachable), the user sees a readable message, and no stack trace, internal class name or configuration value is displayed. |

## Non-functional requirements
| ID | Category | Requirement | Measurable criterion |
|---|---|---|---|
| NFR-001 | Usability | The interface is simple and intuitive, and navigation is easy to understand. (NFR-01) | Checkable through NFR-002 (step limit) and NFR-016/NFR-017 (layout checks). No separate usability test protocol was specified. |
| NFR-002 | Usability | Common operations (add, edit, delete, search, filter) require minimal steps. (NFR-01) | Each of add, edit, delete, search and filter can be completed in at most 3 taps or clicks, counted from the main inventory screen (Decided Q-014). |
| NFR-003 | Performance | Inventory data loads within a reasonable response time. (NFR-02) | With 1,000 active items stored, the inventory list finishes loading within 1 second, measured in a performance test (Decided Q-013). |
| NFR-004 | Performance | Search and filtering perform efficiently. (NFR-02) | With 1,000 active items stored, each search and each filter action shows its results within 1 second, measured in a performance test (Decided Q-013). |
| NFR-005 | Performance | The application handles normal household inventory volumes without noticeable degradation. (NFR-02) | Normal household volume is 1,000 items (Decided Q-013). NFR-003 and NFR-004 limits are met at that volume. |
| NFR-006 | Reliability | Grocery information is persisted in the database. (NFR-03) | Verified by REQ-006: data is unchanged after an application restart. |
| NFR-007 | Reliability | Application errors are handled gracefully. (NFR-03, AC-10) | Verified by REQ-032: no unhandled error page or stack trace is displayed to the user in a test of induced failures. |
| NFR-008 | Reliability | Invalid data is not accepted. (NFR-03) | Verified by REQ-028 to REQ-031: each invalid-input test case results in rejection and no stored record. |
| NFR-009 | Maintainability | Code follows clean code principles and consistent naming and coding standards. (NFR-04) | Checked in team code review against the team's review standard (Decided Q-016). The change is not merged while the review has unresolved clean-code or naming findings. |
| NFR-010 | Maintainability | Frontend, backend and database layers are separated. (NFR-04) | Checkable condition: frontend, backend and persistence code are held in separately identifiable modules, confirmed in code review. |
| NFR-011 | Maintainability | Reusable components and services are used. (NFR-04) | Checked in team code review (Decided Q-016): shared UI and service logic is not duplicated across screens or modules. |
| NFR-012 | Security | Inputs are validated on the backend. (NFR-05) | Verified by REQ-028 to REQ-031: backend tests cover each validation rule and pass. |
| NFR-013 | Security | API errors are handled safely. (NFR-05) | Verified by REQ-032: error responses to the client contain no stack trace, SQL or internal detail (test-checkable). |
| NFR-014 | Security | Sensitive configuration or credentials are not exposed. (NFR-05) | Checkable condition: no configuration secret or credential appears in any API response or frontend bundle, confirmed by a test or scan. |
| NFR-015 | Security | Appropriate data access boundaries are used. (NFR-05) | No login or access control is provided (Decided Q-017, single household). Checkable: no login screen exists and every API endpoint and screen is reachable without credentials. |
| NFR-016 | Responsiveness | The frontend works on desktop and mobile screen sizes. (NFR-06) | Core screens (inventory list, item detail, add/edit, dashboard, recipes) are usable at a desktop width of 1280px and a mobile width of 375px, in the latest Chrome, Edge and Firefox (Decided Q-015). |
| NFR-017 | Responsiveness | Layouts and controls remain usable across supported screen sizes. (NFR-06) | At 1280px and 375px in the latest Chrome, Edge and Firefox, no control is cut off or overlapped, and all controls named in NFR-002 can be reached (Decided Q-015). |
| NFR-018 | Quality | The application can be built and executed successfully. (AC-10) | Checkable condition: the documented build command completes without error and the application starts and serves its main screen. Build instructions are a delivery item, not a requirement (Decided Q-019). |
| NFR-019 | Quality | Relevant unit and integration tests pass. (AC-10) | Checkable condition: the full unit and integration test suite passes with zero failing tests, and backend line coverage is at least 80% in the build coverage report (Decided Q-016). |

## Out of scope
- Paid external AI integrations.
- Online grocery ordering.
- Payment processing.
- Multiple household member accounts.
- Login, user authentication and access control (Decided Q-017).
- Advanced machine-learning-based recipe recommendations.
- Third-party grocery delivery integrations.
- Technology stack choices (Section 5 of the story). These are preferences recorded as context and are not requirements; architecture is for the architecture agent and human approval.
- Section 8 deliverables (REST API documentation, setup instructions, technical documentation, GitHub repository, and the other Section 8 items) are delivery items, not requirements (Decided Q-019). Only the behaviour they describe is covered by REQ and NFR entries.
- Item edit history or change log (Decided Q-022).
- Recipe creation or editing through the user interface (Decided Q-011).
- Languages other than English; UI text and error messages are English only (Decided Q-021).

## Assumptions
- A-001: The application serves a single individual household; there is one user context and no multi-user accounts, since multiple household accounts are out of scope (Section 9). **Confirmed** by Q-017 (single household).
- A-002: No login or user authentication is required in the initial version. **Confirmed** by Q-017 (no login or access control).
- A-003: Recipe matching is by ingredient name: every ingredient in a recipe must match an active pantry item, with names compared case-insensitively on the exact name (Decided Q-009). **Partly confirmed:** the all-ingredients rule and name matching are confirmed. Recipe ingredient quantities are not compared against pantry quantities; this remains an assumption because the story does not define it.
- A-004: The recipe catalogue is predefined, about 10 recipes, shipped in a data file with the application; users do not create or edit recipes (Decided Q-011). **Confirmed.**
- A-005: "Today" for expiry classification is the current system date at the time the view is rendered. Not decided by Q-001 to Q-022; remains an assumption.
- A-006: Quantity is a positive whole number with no unit of measure (Decided Q-004). **Confirmed.** This replaces the rev 1 assumption that quantity was a numeric count with an unstated unit.
- A-007: Duplicate item names are permitted. **Confirmed** by Q-020.
- A-008: The category set in REQ-007 is fixed; users cannot create custom categories. Not decided by Q-001 to Q-022; remains an assumption.
- A-009: The product is a browser-based web application; no native mobile app is in scope, as the story says "Web Application". Browser and screen-size support is decided by Q-015 (latest Chrome, Edge, Firefox; 1280px and 375px). The web-only scope itself remains an assumption.
- A-010: The database technology named in Section 5 (H2) and the other stack preferences are context only and do not constitute requirements; they are not reflected as REQ entries. Not decided by Q-001 to Q-022; remains an assumption.
- A-011: Expiry status (Expired, Expiring soon, Valid) is derived from the expiry date and the current date, and is not entered manually by the user. The status rules are set by Q-002, Q-003 and Q-006; the derivation itself remains an assumption.
- A-012: "Show all matches" (Decided Q-010) is read as no cap on the number of recipes shown. Because the ranking orders recipes by fewest missing ingredients, recipes that are not fully makeable are included after the fully makeable ones, so that missing ingredients can be shown (FR-05, AC-08). New assumption; not confirmed by the human.

## Open questions
None. All questions from rev 1 (Q-001 to Q-022) were decided by the human and applied above (see "Decided" notes against REQ-001, REQ-004 to REQ-005, REQ-007 to REQ-011, REQ-013 to REQ-016, REQ-019, REQ-020, REQ-023 to REQ-027, REQ-029, REQ-030, NFR-002 to NFR-005, NFR-009, NFR-011, NFR-015 to NFR-019, the Out of scope list, and A-001 to A-007).

## Revision log
| Rev | Date (UTC) | Trigger | Agent | Summary |
|---|---|---|---|---|
| 1 | 2026-11-25T00:00Z | initial | requirements-analyst | Initial draft from SP-2 snapshot: 32 functional and 19 non-functional requirements, 11 assumptions, 22 open questions, out-of-scope list from Section 9. |
| 2 | 2026-11-25T00:00Z | human decisions on Q-001 to Q-022 (accept defaults) | requirements-analyst | Applied all 22 decisions. Updated REQ-001, 004, 005, 008 to 011, 013, 015, 016, 019, 020, 023 to 027, 029, 030; NFR-002 to 005, 009, 011, 015 to 019 now measurable. Added Decided notes and out-of-scope items (Q-011, Q-017, Q-019, Q-021, Q-022). Confirmed A-001, A-002, A-004, A-006, A-007; reworded A-003, A-009, A-011; added A-012. No requirement IDs changed; none removed. Open questions: none. |
