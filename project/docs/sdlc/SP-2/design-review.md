# Design review: SP-2

Reviewed: architecture.md (revision 2)
Round: 2
Open high-severity findings: 0

Round 2 re-reviews architecture rev 2 against requirements rev 2 (approved at CP1). Round 1 findings DR-001 to DR-016 are tracked in the Previous findings table. New findings start at DR-017. Round 1 content is kept below as the record.

## Round 2

### Checks applied
- Coverage: All 32 REQ IDs (REQ-001 to REQ-032) and all 19 NFR IDs (NFR-001 to NFR-019) appear in the requirement-to-component map, and each component row agrees with the map in both directions. One gap: REQ-012 has no REST API layer (DR-017).
- Responsibility: No duplicate ownership found. The REQ-002 (Item query service) and REQ-003 (Item service) split is clear. The Recipe suggestion service responsibility does not cover the expiry status its ranking needs (DR-019).
- Failure modes: H2 startup, lock, corruption and request-time 503 are defined; recipe file startup failure is defined. The API client states no request timeout (DR-024).
- Empty and missing data: Null-expiry rule defined (DR-005); empty search returns full inventory; empty recipe match returns an empty list. GET of a missing item is not stated (DR-024). Request binding failures are not routed (DR-023).
- Security: Localhost bind added (DR-013). Secret handling is not designed (DR-020). The H2 console default profile is not named (DR-021). No-login exposure is a named, accepted risk (Q-017).
- Data integrity: Single writer (Item service) stated; last-write-wins accepted and recorded (DR-012).
- Testability: Performance, no-network and null-expiry tests are assigned to Test suites. No test is assigned for the H2 console rule (DR-021).
- Dependencies and technology risk: File-mode H2 risks named (DR-016, resolved). JaCoCo open for the human (DR-015).
- Scale and performance: 1,000-item performance test assigned (DR-010); per-request classification named as a risk. No new limit problem.
- Operability: Correlation ID and stdout logging defined (DR-011, resolved); startup failures logged. No new gap beyond DR-023.

### Round 2 findings (new)
| ID | Severity | Concerns | Description | Proposed fix | Disposition |
|---|---|---|---|---|---|
| DR-017 | low | REQ-012; REST API layer | The REQ-012 map row lists Browser screens and Expiry classifier only. The REST API layer is absent, although the DR-002 disposition extends it to REQ-007 to REQ-027 and the status reaches the screen over HTTP. The map and the stated disposition disagree. | Add REST API layer to the REQ-012 map row, or state that the indicator is derived in the browser from the status field of the REST response. | |
| DR-018 | low | DR-013; Application shell; NFR-015 | The DR-013 disposition says the localhost bind and H2 console rule are stated in the Application shell. The Application shell row names neither. They appear only in the Runtime exposure row under Technology choices. | Add the default bind address and the console rule to the Application shell row. | |
| DR-019 | medium | REQ-023; REQ-020; A-007; Recipe suggestion service | The first ranking key ("uses at least one active Expiring soon item") needs each pantry item's expiry status. The Recipe suggestion service responsibility says it reads "pantry item names", and data flow step 9 does not say where status comes from. Duplicate names are allowed (A-007), so a recipe whose ingredient name matches one Expiring soon item and one Valid item is ambiguous. REQ-023 cannot be tested deterministically without a rule. | State that the service reads name and expiry status per item (Item repository and Expiry classifier). Define that a recipe uses an Expiring soon item when any active item with a matching name is Expiring soon, and record that rule as an assumption for human confirmation. | |
| DR-020 | medium | NFR-014; Security; Application shell | Checklist item 5 (secret handling) is not designed. The design names no source for configuration secrets (for example an H2 user or password, if one is set) and no rule that keeps them out of API responses, logs and the Vite frontend bundle. NFR-014 is mapped only to the API error handler and API client, and neither configures secrets. | State where any credential is configured (environment or local configuration outside the repository), that no endpoint returns configuration values, and that the frontend bundle contains no configuration secret. Map NFR-014 to Application shell. | |
| DR-021 | medium | Runtime exposure; H2 web console; NFR-018; NFR-015 | The H2 console is disabled "outside the development profile", but the design does not name the profile used by the documented build and run command (NFR-018) or the default profile. If the default is development, the console runs on a default start with no credentials, and the localhost bind is the only limit. | Name the default profile. Make the console off by default and enabled only by an explicit development setting. Add a test that the console is not served in the default profile. | |
| DR-022 | low | Item validator; REQ-001; REQ-030; A-005; DR-006 | The Item validator applies "purchase date not later than today" (REQ-001, REQ-030). The Date and time row and the Item validator row do not say that the validator uses the same injected Clock as the Expiry classifier. Near midnight the two can disagree on "today". | State that the Item validator takes the same injected Clock. This is part of the DR-006 decision. | |
| DR-023 | low | REQ-028 to REQ-031; REST API layer; API error handler | Request binding failures (for example a non-numeric quantity or an unparseable date) happen in the REST API layer before the Item validator runs. The design does not say they return the same 400 response with field messages as validator failures, so REQ-029 and REQ-030 test cases could receive different response shapes. | State that binding failures are converted by the API error handler into the same 400 response shape, with a field message and the correlation ID. | |
| DR-024 | low | API client; REQ-003; REQ-032; NFR-007; DR-012 | Checklist items 3 and 4 are partly unaddressed. The API client states no request timeout and no slow-backend behaviour. The read of one item (REQ-003) has no not-found behaviour, although DR-012 notes that another tab can delete the item in the meantime. | State a client request timeout that shows the readable message with a correlation ID. State that GET of a missing item returns 404 with a readable message. | |

### Previous findings (round 2 status)
Status is based on the architect's dispositions in architecture.md rev 2. Findings marked "open" for a pending human decision are not closed by this review.

| ID | Status (resolved, partly resolved, open) | Note |
|---|---|---|
| DR-001 | open (pending human decision) | Working default (name ascending) is in data flow step 9 and Risks. The REQ-023 tie-break stays with the human. |
| DR-002 | resolved | REST API layer added to REQ-001 to REQ-005 and REQ-007 to REQ-031. REQ-012 gap raised as DR-017. |
| DR-003 | resolved | Browser screens responsibility names dashboard, recipe and search/filter screens; map rows updated. |
| DR-004 | resolved | REQ-002 on Item query service (empty filter); Item service reads one item; Test suites on REQ-024; API error handler on REQ-028 to REQ-031. |
| DR-005 | resolved | Null expiry gives status Valid with no date bound; Valid filter requires a non-null date; tests assigned to Test suites. |
| DR-006 | open (pending human decision) | Server clock kept; no named time zone yet. Injected Clock for the validator raised as DR-022. |
| DR-007 | open (pending human decision) | Working default kept (case-insensitive, no trimming). Proposed trim and whitespace rule not adopted and not rejected; it stays with the human. |
| DR-008 | resolved | Q-012 removed; stored items are the active inventory (REQ-005). Consistent with requirements rev 2, which has no open questions. |
| DR-009 | resolved | Startup validation and fail-fast for recipe file and H2 lock or corruption; sanitized 503 for request-time DB failure. |
| DR-010 | resolved | Performance test (1,000 items) and REQ-024 no-network test assigned to Test suites. |
| DR-011 | resolved | Correlation ID in logs and sanitized response; stdout destination named. |
| DR-012 | resolved | Last-write-wins recorded as accepted behaviour; 404 for a deleted target. Missing-item GET raised as DR-024. |
| DR-013 | partly resolved | Bind address and console rule are in Runtime exposure and Risks, but not in the Application shell row (DR-018). Default profile and secret handling raised as DR-021 and DR-020. |
| DR-014 | resolved | Step 3 now matches the diagram: Item service calls Item validator. |
| DR-015 | open (pending human decision) | JaCoCo kept as working default; no fallback named. NFR-019 coverage depends on the human decision. |
| DR-016 | resolved | Single-process locking, backup by copy while stopped, and pinned H2 version named as risks. |

### Round 2 summary
- Open high-severity findings: 0.
- New findings: 8 (0 high, 3 medium: DR-019, DR-020, DR-021; 5 low: DR-017, DR-018, DR-022, DR-023, DR-024).
- Previous findings: 11 resolved, 1 partly resolved (DR-013), 4 open pending human decision (DR-001, DR-006, DR-007, DR-015).

### Items awaiting the human (not closed)
- DR-001: REQ-023 final tie-break (working default: recipe name ascending).
- DR-006: time zone for "today" (A-005); server clock versus browser date.
- DR-007: name normalisation beyond case-insensitive comparison (A-003).
- DR-015: JaCoCo for NFR-019 coverage measurement (CP2).

## Round 1 record

### Round 1 checks applied
- Coverage: All 32 REQ IDs and all 19 NFR IDs appear in the requirement-to-component map. Mapping errors are recorded in DR-002, DR-003 and DR-004.
- Responsibility: 15 components reviewed. Overlap between Item service and Item query service for the inventory list is recorded in DR-004.
- Failure modes: External calls are limited to H2 and the recipe data file. H2 and recipe-file failures are not designed (DR-009).
- Empty and missing data: The no-expiry case is not designed in the classifier (DR-005).
- Security: H2 console and bind address are not stated (DR-013). Auth is an accepted risk (Q-017).
- Data integrity: Concurrent edit behaviour is not stated (DR-012).
- Testability: Performance tests for NFR-003 to NFR-005 are not assigned to any suite (DR-010).
- Dependencies and technology risk: H2 file-mode risks are not named (DR-016). JaCoCo flagged for CP2 (DR-015).
- Scale and performance: No caching; per-request classification at 1,000 items is a named risk (test in DR-010).
- Operability: No server-side logging or diagnosis path (DR-011).

### Round 1 findings
| ID | Severity | Concerns | Description | Proposed fix | Disposition |
|---|---|---|---|---|---|
| DR-001 | medium | REQ-023; Recipe suggestion service | REQ-023 defines two ranking keys. Recipes equal on both keys have no defined order, so output order is non-deterministic. | Add a deterministic final key (for example recipe name ascending, case-insensitive) and record it as an assumption for human confirmation at CP2. | |
| DR-002 | medium | REST API layer; REQ-001 to REQ-027 | The REST API layer is listed only for REQ-028 to REQ-031 and some NFRs. Endpoint ownership for most requirements is untraced. | Add REST API layer to the map rows for REQ-001 to REQ-027 served over HTTP; list the recipe endpoint explicitly. | |
| DR-003 | medium | Browser screens; REQ-015 to REQ-018, REQ-020, REQ-022 to REQ-026 | No component owns the UI for dashboard, recipe and search/expiry-filter requirements. | Add those screens to the Browser screens responsibility and map rows. | |
| DR-004 | low | Item service; Item query service; REQ-002, REQ-024, REQ-028 to REQ-031 | (a) Inventory read duplicated across two services. (b) REQ-024 omits Test suites. (c) REQ-028 to REQ-031 omit API error handler. | (a) Inventory list on Item query service with empty filter; Item service reads one item. (b) Add Test suites to REQ-024. (c) Add API error handler to REQ-028 to REQ-031. | |
| DR-005 | medium | Expiry classifier; REQ-010, REQ-012, REQ-013, REQ-026 | No rule for items with no expiry date; Valid filter could wrongly include them. | State the no-expiry rule and explicit null exclusion from date bounds; add a test case for each. | |
| DR-006 | medium | Expiry classifier; A-005; REQ-010, REQ-011, REQ-001, REQ-030 | Server clock chosen; time zone not stated. | Inject a Clock bound to an explicit configured zone; record A-005 as a decision for the human at CP2. | |
| DR-007 | medium | Item validator; Recipe suggestion service; A-003; REQ-020, REQ-022, REQ-028 | Name normalisation (trim, internal spaces, Unicode case folding) undefined; whitespace-only names pass the empty check. | Define one normalisation rule; reject whitespace-only names under REQ-028; record for human confirmation with A-003. | |
| DR-008 | low | Item repository; Item query service; Dashboard service; REQ-005, REQ-014, REQ-025 | Q-012 open question already decided by permanent delete; "active" wording redundant. | Remove the open question; state that stored items are the active inventory. | |
| DR-009 | medium | Recipe catalogue; H2 database; NFR-003, NFR-006, REQ-024, REQ-006 | Failure modes for recipe file and H2 lock or corruption not designed. | Fail fast at startup with a logged reason; define empty-catalogue response; sanitized 503 for request-time DB failure. | |
| DR-010 | medium | Test suites; NFR-003, NFR-004, NFR-005, REQ-024 | No performance test at 1,000 items; REQ-024 no-network test not assigned. | Add performance test to Test suites; assign the no-network test to Test suites. | |
| DR-011 | medium | API error handler; REST API layer; NFR-007, NFR-013; Operability | No server-side logging, correlation ID or diagnosis path. | Log every handled failure with a correlation ID also returned to the client; log no secrets, SQL or stack traces to the client; state the destination. | |
| DR-012 | low | Item service; REQ-004, REQ-005; Data integrity | Concurrent edit and delete not addressed. | Add a version field with 409, or record last-write-wins as accepted behaviour. | |
| DR-013 | low | REST API layer; Application shell; NFR-015, NFR-014; Security | Bind address and H2 console not stated. | Default to localhost unless configured; disable H2 console outside a development profile; state both in the Application shell. | |
| DR-014 | low | Diagram; Data flow step 3; Item validator | Step 3 and diagram disagree on who calls the validator. | Align diagram with step 3, or correct step 3. | |
| DR-015 | low | Test suites; JaCoCo; NFR-019; Technology choices | No fallback coverage tool if CP2 rejects JaCoCo. | Name a fallback for CP2, or state NFR-019 cannot be verified without a measurement tool. | |
| DR-016 | low | H2 database; Risks; Technology choices | File-mode H2 risks not named. | Name single-process locking, backup and recovery, and version pinning as risks with mitigations. | |

## Revision log
| Rev | Date (UTC) | Trigger | Agent | Summary |
|---|---|---|---|---|
| 1 | 2026-11-25T00:00Z | initial review | design-reviewer | Round 1 review of architecture rev 1 against requirements rev 2: 16 findings (0 high, 7 medium, 9 low). Coverage check: all 32 REQ and 19 NFR IDs are in the map; mapping gaps in DR-002 to DR-004. Architect's points addressed: DR-001 (REQ-023 tie-break), DR-006 (A-005), DR-007 (A-003), DR-008 (Q-012), DR-015 (JaCoCo). |
| 2 | 2026-11-25T00:00Z | design re-review round 2 | design-reviewer | Re-review of architecture rev 2 against requirements rev 2. Previous findings: 11 resolved, 1 partly resolved (DR-013), 4 open pending human decision (DR-001, DR-006, DR-007, DR-015). New findings DR-017 to DR-024: 0 high, 3 medium, 5 low. Open high-severity count: 0. Coverage re-check: all 32 REQ and 19 NFR IDs map to at least one component; one row gap (REQ-012, DR-017). No approvals; architecture.md and requirements.md not edited. |
