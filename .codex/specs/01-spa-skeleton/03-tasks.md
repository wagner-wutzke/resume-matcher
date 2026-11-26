# Task List: Resume Matcher SPA Skeleton

- **Status:** [ ] Ready | [ ] Active | [ ] Paused | [x] Completed
- **Target Agent:** @engineer
- **Validation Agent:** @reviewer
- **References:** `01-requirements.md`, `02-design.md`

> Complete one task at a time. Each task includes its own focused tests or verification where
> applicable. Do not implement behavior outside the referenced requirements and design.

## Phase 1: Backend and Data Access

- [x] **Task 1.1 — Web MVC dependency:** Add Spring Boot Web MVC support required to serve the SPA
  and expose the API. Verify the application compiles with the existing Java and Spring Boot
  configuration.
- [x] **Task 1.2 — MongoDB configuration:** Configure the MongoDB URI from `MONGODB_URI` with the
  documented local default, without committing credentials. Verify configuration binding.
- [x] **Task 1.3 — Job-offer document:** Map the `job_offers` collection and its `_id`, `jobName`,
  `jobDescription`, `requiredQualifications`, `location`, and `allocation` fields. Add a focused
  mapping/serialization test.
- [x] **Task 1.4 — Read-only repository:** Add MongoDB repository access for listing job offers.
  Verify the repository supports the required read operation and introduces no write workflow.
- [x] **Task 1.5 — Job-offer API:** Implement `GET /api/job-offers` with the documented response
  fields. Return `200 OK` with `[]` for an empty collection. Add controller tests for populated and
  empty results and the JSON field names (**AC04–AC05**).
- [x] **Task 1.6 — Retrieval failure response:** Map retrieval failures to the documented
  `503 Service Unavailable` response and ensure they are not returned as empty success. Add
  controller tests (**AC08**).

## Phase 2: SPA Structure and Styling

- [x] **Task 2.1 — SPA entry point:** Add the static application page and confirm Spring Boot serves
  it at `/` on port 8080 (**AC01**).
- [x] **Task 2.2 — Application identity and navigation:** Display **Resume Matcher**, the title
  **Candidates Matching made easy**, and the **Candidates** and **Open Positions** tabs
  (**AC02**).
- [x] **Task 2.3 — Split-panel views:** Add a left list area and right details area to each tab.
  Keep the candidate areas empty and keep Open Positions details empty before selection
  (**AC03, AC07**).
- [x] **Task 2.4 — Visual presentation:** Apply the specified dark gray and royal blue palette and
  make the application use the available page space. Verify the palette is applied consistently to
  the page background, text, navigation, and selected or hovered list items. Check that the
  two-panel layout remains usable at supported viewport sizes (**NFR01**).

## Phase 3: Open Positions Interaction

- [x] **Task 3.1 — Load job offers:** Fetch `GET /api/job-offers` once when the SPA loads and
  populate the Open Positions list from the returned job names (**AC05**).
- [x] **Task 3.2 — Empty and failed states:** Keep list and details empty for a successful empty
  response; show a distinct retrieval failure state when the request fails. Add frontend tests or
  browser-level verification for both states (**AC04, AC08**).
- [x] **Task 3.3 — Selected job details:** On selection, display the job name, job description,
  required qualifications, location, and allocation. Verify the initial details state is empty and
  selection displays all fields (**AC06–AC07**).
- [x] **Task 3.4 — Safe text rendering:** Render all returned field values as text, not HTML. Add a
  test using markup-like content and verify it remains literal text (**AC09**).

## Phase 4: Feature Verification and Review

- [x] **Task 4.1 — Acceptance coverage:** Run the automated tests and verify each acceptance
  criterion AC01–AC10 has corresponding automated or browser-level coverage. Address failures
  before review.
- [x] **Task 4.2 — Scope and code review:** Confirm there are no candidate data operations or
  create/update/delete operations for candidates or job offers, and produce the review report
  required by the repository workflow (**AC10**).
