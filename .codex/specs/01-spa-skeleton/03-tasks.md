# Task List: Resume Matcher SPA Skeleton

- **Status:** [ ] Ready | [x] Active | [ ] Paused | [ ] Completed
- **Target Agent:** @engineer
- **Validation Agent:** @reviewer
- **References:** `01-requirements.md`, `02-design.md`

> Complete one task at a time. Each task includes focused tests or verification. Do not implement
> behavior outside the referenced requirements and design.

> The engineer implements and reports task results. The reviewer updates task checkboxes only after
> verifying the implementation and its focused tests.

## Phase 1: MongoDB Read Path and API

- [x] **Task 1.1 — Serve the SPA from Spring MVC:** Ensure Web MVC serves the static application
  entry point at `/` on port 8080. Add or update a focused availability test (**FR01, AC01**).
- [x] **Task 1.2 — Configure MongoDB with individual properties:** Configure host, port, database,
  and optional credentials from separate properties. Confirm `spring.mongodb.uri` is neither
  declared nor used. Add configuration tests for defaults and environment overrides (**BR08,
  FR12, AC12**).
- [x] **Task 1.3 — Map the job-offer catalog:** Map MongoDB `_id` to `id` and the documented
  `job_offers` fields, preserving numeric `yearsOfExperience`. Add focused mapping and JSON
  serialization tests (**FR05–FR06, FR08, AC04, AC07, AC10**).
- [x] **Task 1.4 — Add read-only catalog repository:** Provide only the read operation needed to
  retrieve existing offers. Verify no write operation is introduced by this feature (**BR01,
  BR07, FR11, NFR04, AC11**).
- [x] **Task 1.5 — Implement the catalog endpoint:** Implement `GET /api/job-offers` with the
  documented response fields and `200 OK`, including `[]` for an empty collection. Add controller
  tests for populated and empty results (**FR05–FR06, FR09, AC04–AC05**).
- [x] **Task 1.6 — Handle catalog retrieval failures:** Return the documented `503 Service
  Unavailable` error response when retrieval fails; never return a successful empty result for a
  failure. Add controller tests (**BR06, FR10, NFR05, AC09**).

## Phase 2: SPA Shell and Layout

- [x] **Task 2.1 — Add application identity and tabs:** Display **Resume Matcher**, **Candidates
  Matching made easy**, and the **Candidates** and **Open Positions** tabs. Add page-level
  verification (**FR02–FR03, AC02**).
- [x] **Task 2.2 — Add split panels and candidate placeholder:** Provide left list and right details
  areas in both tabs, and keep both Candidates areas empty. Add UI verification (**FR04, BR03,
  AC03**).
- [x] **Task 2.3 — Apply the specified visual design:** Use the dark gray and royal blue palette,
  available page space, and specified frontend stack. Verify layout usability at supported
  viewport sizes (**NFR01–NFR02**).

## Phase 3: Open Positions Retrieval and Interaction

- [x] **Task 3.1 — Load and list job offers:** Request `GET /api/job-offers` once on application
  load and display each returned job title. Render the title as text (**FR05–FR06, AC04, AC10**).
- [x] **Task 3.2 — Preserve empty and initial states:** For a successful empty response, keep the
  Open Positions list and Details panel empty. For populated results, leave all offers unselected
  and the Details panel empty on initial load (**BR02, BR04, FR07, FR09, AC05–AC06**).
- [x] **Task 3.3 — Select an offer and show its details:** Selecting an offer displays Company,
  Job Title, Job Description, Required Qualifications, Years of Experience, Location, and
  Allocation. Selecting another offer replaces the details and selection (**BR05, FR08, AC07–AC08**).
- [x] **Task 3.4 — Show retrieval failure distinctly:** Display an unsuccessful retrieval state
  distinct from the successful empty state. Keep the Details panel without a selected offer
  (**BR06, FR10, NFR05, AC09**).
- [x] **Task 3.5 — Render returned values safely:** Ensure every job-offer value is treated as text
  and markup-like content is not interpreted as HTML or script. Add a focused frontend test
  (**NFR03, AC10**).

## Phase 4: Acceptance Verification and Review

- [x] **Task 4.1 — Verify acceptance coverage:** Map AC01–AC12 to automated tests or documented
  browser-level verification. Run the backend and frontend suites and address failures.
- [x] **Task 4.2 — Review feature scope:** Confirm SPA Skeleton changes add no job-offer or
  candidate create/update/delete controls or API operations. Treat CRUD owned by a separate
  feature as outside this scope (**BR01, FR11, AC11**).
- [x] **Task 4.3 — Reviewer report:** Compare implementation and tests with the requirements and
  design. Record compliance, acceptance coverage, gaps, and improvement suggestions; update task
  checkboxes after review.
