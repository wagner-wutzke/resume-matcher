# Task List: Show Candidates

- **Status:** [ ] Draft | [ ] Ready | [ ] Active | [ ] Paused | [x] Completed
- **Target Agent:** @engineer
- **Validation Agent:** @reviewer
- **References:** `01-requirements.md`, `02-design.md`

> This is a planned breakdown only. Requirements and design are still Draft; implementation must
> wait until they are approved. Implement one unchecked task at a time, with focused automated
> verification for its acceptance criteria. The reviewer owns task-status changes after review.

## Phase 1: Candidate Read API and MongoDB Queries

- [x] **Task 1.1 — Add candidate summary, page, and detail response models:** Define response DTOs
  matching the shapes in `02-design.md`. Map only the permitted summary fields and structured
  detail fields; do not expose resume content, vector data, or persistence internals. Add focused
  mapping tests (**BR04–BR05, NFR01, AC01, AC03, AC06, AC10**).
- [x] **Task 1.2 — Add bounded candidate reads to the repository:** Extend the candidate repository
  abstraction and Mongo adapter with an ID lookup, a projected page query capped at 15, a total
  count, and case-insensitive fullname ordering with candidate ID tie-breaking. Add repository
  tests for projection, sort, page boundaries, count, and ID lookup (**BR01–BR03, BR07, FR01–FR02,
  NFR02, AC01–AC03, AC09–AC10**).
- [x] **Task 1.3 — Implement candidate query service and page validation:** Coordinate page and
  details reads, default to page zero, enforce fixed page size 15, calculate page metadata, accept
  page zero for an empty collection, and reject negative, malformed, or out-of-range pages as
  designed. Add unit tests for empty, first, intermediate, last, invalid, and stale page cases
  (**BR02, FR01, FR03, FR07, AC01–AC02, AC05, AC09**).
- [x] **Task 1.4 — Expose read-only candidate endpoints and safe errors:** Add
  `GET /api/candidates?page=0` and `GET /api/candidates/{id}` with the designed status codes and
  error response codes. Keep exception handling scoped to candidate routes; return no database
  details and do not log candidate values. Add MockMvc tests for response shapes, invalid pages,
  not-found IDs, storage failures, and absence of write operations (**BR01–BR03, BR06, FR01–FR04,
  NFR01–NFR02, AC01–AC03, AC07, AC09–AC10**).

## Phase 2: Candidates Tab UI

- [x] **Task 2.1 — Add candidate pagination and accessible status markup:** Add previous/next
  controls, a page indicator, retry/status regions, and semantic keyboard-operable candidate
  entries to the existing Candidates tab. Preserve the current left-list/right-details layout and
  Open Positions tab. Add focused markup/accessibility verification (**FR03, FR05–FR06, NFR03–NFR04,
  AC02, AC05, AC07, AC11**).
- [x] **Task 2.2 — Load and render candidate summaries and pages:** Fetch the first page when the
  Candidates tab opens, render no more than 15 summaries using text-safe DOM APIs, show only the
  BR04 name/position label and missing-value placeholders, separate entries with a fine gray line,
  handle previous/next boundaries, refresh on tab re-entry, and display loading, empty, and
  retryable list-error states. Add focused UI behavior tests
  (**BR02, BR04, BR07, FR01–FR03, FR05–FR06, NFR01–NFR03, AC01–AC02, AC05, AC07, AC11**).
- [x] **Task 2.3 — Load and render selected candidate details:** Request details by candidate ID;
  render the BR05 fields and experiences with missing-value and empty-experience states; clear
  prior details on selection/page changes; ignore stale asynchronous responses; and handle retry,
  retrieval failure, and not-found refresh behavior. Add focused UI behavior tests (**BR03, BR05–BR06,
  FR02–FR04, FR06–FR07, NFR01, NFR03, AC03–AC04, AC06, AC08–AC09, AC11**).

## Phase 3: Acceptance Verification and Review

- [x] **Task 3.1 — Verify candidate display acceptance coverage:** Map AC01–AC11 to automated API,
  repository, and UI tests; run the relevant backend and UI suites; resolve failures; and verify
  bounded responses, keyboard behavior, and read-only access (**all ACs, NFR02–NFR05**).
- [x] **Task 3.2 — Review privacy, failure isolation, and feature boundaries:** Confirm responses
  contain only specified summary/detail fields; logs contain no candidate values; errors are
  sanitized; the Open Positions tab remains usable; and no candidate writes, ingestion controls,
  search, or Qdrant reads were added (**BR01, BR03, BR06, NFR01, NFR04, AC07, AC10**).
- [x] **Task 3.3 — Reviewer report and status:** Record spec compliance, acceptance-criteria
  evidence, gaps, and improvement suggestions in `04-review.md`. Update task checkboxes only after
  reviewer verification; approve completion only when no unresolved gaps remain (**all ACs**).
