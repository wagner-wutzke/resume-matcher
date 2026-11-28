# Task List: Job-Offer Management

- **Status:** [x] Ready | [ ] Active | [ ] Paused | [ ] Completed
- **Target Agent:** @engineer
- **Validation Agent:** @reviewer
- **References:** `01-requirements.md`, `02-design.md`

> Execute one task at a time. The design is still Draft; complete the architect clarification and
> approval gates before implementation tasks begin. The reviewer updates task checkboxes after
> verifying implementation and focused tests.

> **Task breakdown approval:** Approved by the user. The Ready status remains until Phase 0 is
> resolved and the design is approved for implementation.

## Phase 1: Persistence Model and Validation

- [ ] **Task 1.1 — Align job-offer persistence mapping:** Implement the approved field mapping for
  MongoDB `_id`, `jobId`, `jobTitle`, Company, and all other approved persisted fields. Add mapping
  tests for field names and round-trip values (**BR02–BR04, approved model/API criteria**).
- [ ] **Task 1.2 — Define create/update DTOs:** Add request and response contracts matching the
  approved field names, including the resolved Job Name/`jobTitle` mapping. Add JSON serialization
  tests (**AC04, AC07–AC09, AC15**).
- [ ] **Task 1.3 — Validate required values:** Enforce required-field and allocation rules on the
  server, including whitespace-only values. Add tests for each missing field and unsupported
  allocation (**BR02–BR03, AC05–AC06, AC15**).
- [ ] **Task 1.4 — Implement create and update service behavior:** Persist valid creates and updates;
  resolve identifiers before updates and do not upsert. Add success, validation, and not-found tests
  (**BR04, AC08–AC09, AC11**).
- [ ] **Task 1.5 — Implement delete service behavior:** Delete existing offers and report unknown
  identifiers. Add success and not-found tests (**AC13–AC14**).
- [ ] **Task 1.6 — Handle persistence failures:** Translate create, update, and delete persistence
  failures without exposing internals or changing successful-state assumptions. Add failure-path
  tests (**BR04, AC11, AC14**).

## Phase 2: REST API

- [ ] **Task 2.1 — Implement catalog and create endpoints:** Return the approved catalog fields and
  implement create with the documented status, response, and Location header. Add populated/empty
  catalog and create tests (**AC04–AC05, AC08, approved list criterion**).
- [ ] **Task 2.2 — Implement update endpoint:** Implement update success, validation, and not-found
  responses. Add controller tests for each outcome (**AC09, AC11**).
- [ ] **Task 2.3 — Implement delete endpoint:** Implement successful deletion and not-found
  responses. Add controller tests (**AC13–AC14**).
- [ ] **Task 2.4 — Standardize API errors:** Return stable validation, not-found, retrieval, and
  persistence error bodies without stack traces or internal details. Add response tests (**AC11,
  AC14 and approved error criteria**).

## Phase 3: Open Positions List and Details

- [ ] **Task 3.1 — Render the required offer label:** Display each offer as
  `[jobTitle] - [company] - [jobId]` using the approved list-entry semantics. Render values as text.
  Add tests for label content, semantics, and markup-like values (**list-format criterion, NFR-safe
  rendering**).
- [ ] **Task 3.2 — Implement selection and Details display:** Support one selected offer at a time;
  display Company above Job Description and all other approved details. Add selection replacement
  tests (**AC01, AC15**).
- [ ] **Task 3.3 — Show New, Edit, and Delete actions appropriately:** Keep New Job Offer visible and
  show Edit/Delete only when an offer is selected. Apply the approved element semantics and button
  styling (**AC02–AC03, AC12, AC17**).
- [ ] **Task 3.4 — Handle list retrieval states:** Load the catalog on application start, keep
  successful empty results distinct from failures, and leave the Details panel empty until
  selection (**AC04–AC06, approved empty/error criteria**).

## Phase 4: Create and Edit Form

- [ ] **Task 4.1 — Build the new-offer form:** Open the modal with the approved required fields and
  Cancel/Save actions; verify it starts empty (**AC04, AC15**).
- [ ] **Task 4.2 — Initialize edit form:** Reuse the modal and populate every editable field from
  the selected offer (**AC07**).
- [ ] **Task 4.3 — Add allocation choices and required validation:** Offer exactly Remote, Hybrid,
  and On-site; show accessible validation only after Save and mark only invalid fields (**AC05–AC06**).
- [ ] **Task 4.4 — Implement create success and failure UI:** On success close the modal, refresh the
  list, select the new offer, and display its details. On failure preserve entered values and show
  an error (**AC08, AC11**).
- [ ] **Task 4.5 — Implement update success and failure UI:** On success close the modal, refresh
  the selected entry/details, and retain selection. On failure preserve form and displayed state
  and show an error (**AC09, AC11**).
- [ ] **Task 4.6 — Confirm discarding edits:** Close unchanged forms directly; ask for confirmation
  when changed; preserve values when continuing to edit and discard only on confirmation (**AC10**).

## Phase 5: Delete UI and Position Detail Formatting

- [ ] **Task 5.1 — Implement delete success and failure UI:** On success remove the selected offer
  and clear Details; on failure retain the offer/details and show an error (**AC12–AC14**).
- [ ] **Task 5.2 — Render plain-text formatting markers safely:** Render `**bold**`, `*italic*`,
  and `- ` bullet markers using DOM elements; keep HTML-looking text literal (**AC16**).
- [ ] **Task 5.3 — Apply consistent button sizing:** Apply the approved New Job Offer style to all
  job-offer buttons with a font size of `0.75rem` and height of `35px` (**AC17**).
- [ ] **Task 5.4 — Set detail label typography:** Set `.detail-field > dt` to `14px` and verify the
  rule with a focused style test (**AC21**).

## Phase 6: Acceptance Verification and Review

- [ ] **Task 6.1 — Verify acceptance coverage:** Map every approved acceptance criterion to
  automated or documented browser-level verification, run relevant backend/frontend suites, and
  resolve failures (**all ACs**).
- [ ] **Task 6.2 — Review scope and implementation:** Confirm CRUD behavior, fields, list semantics,
  errors, and UI match the approved requirements/design; identify out-of-scope behavior (**all ACs**).
- [ ] **Task 6.3 — Reviewer report and status:** Record compliance, acceptance coverage, gaps, and
  suggestions in `04-review.md`; update task status only after review. Approve the feature only when
  no unresolved gaps remain.
