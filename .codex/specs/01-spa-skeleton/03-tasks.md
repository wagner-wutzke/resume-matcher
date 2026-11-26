# Task List: Resume Matcher SPA Skeleton

- **Status:** [ ] Ready | [x] Active | [ ] Paused | [ ] Completed
- **Target Agent:** @engineer
- **Validation Agent:** @reviewer
- **References:** `01-requirements.md`, `02-design.md`

> Complete one task at a time. Each task includes focused tests or verification. Do not implement
> behavior outside the referenced requirements and design.

## Phase 1: Read-Only API and Data Contract

- [x] **Task 1.1 — Job-offer data mapping:** Map the existing `job_offers` record fields required
  by the design: `company`, `jobId`, `jobTitle`, `jobDescription`, `requiredQualifications`,
  numeric `yearsOfExperience`, `location`, and `allocation`. Map MongoDB `_id` to API `id`. Add
  mapping and serialization tests (**AC04, AC07**).
- [x] **Task 1.2 — List endpoint contract:** Implement or update `GET /api/job-offers` to return
  `id`, `jobId`, `company`, `jobTitle`, `jobDescription`, `requiredQualifications`,
  `yearsOfExperience`, `location`, and `allocation`. Return `200 OK` with `[]` when the collection
  is empty. Add controller tests for populated and empty results (**AC04–AC05**).
- [x] **Task 1.3 — Retrieval failure response:** Map MongoDB/API retrieval failures to
  `503 Service Unavailable` with the documented stable error body. Verify failures are not returned
  as an empty success (**AC09**).
- [x] **Task 1.4 — Individual MongoDB properties:** Configure MongoDB with separate host, port,
  database, and optional credential properties. Do not declare or use `spring.mongodb.uri`. Add
  configuration tests covering property resolution and confirming the URI property is absent
  (**AC12**).

## Phase 2: SPA Shell

- [x] **Task 2.1 — SPA availability and identity:** Serve the SPA at `/` on port 8080 and display
  **Resume Matcher**, **Candidates Matching made easy**, and the **Candidates** and **Open
  Positions** tabs (**AC01–AC02**).
- [x] **Task 2.2 — Candidate placeholder and split panels:** Provide left list and right details
  areas in both tabs. Keep candidate areas empty (**AC03**).
- [x] **Task 2.3 — Visual presentation:** Apply the specified dark gray and royal blue palette and
  use the available page space (**NFR01**).

## Phase 3: Open Positions List and Details

- [x] **Task 3.1 — Load and list offers:** Fetch `GET /api/job-offers` once on application load
  and display each offer's `jobTitle` in the Open Positions list (**AC04**).
- [x] **Task 3.2 — Empty and initial selection states:** For a successful empty response, keep the
  list and Details panel empty. For populated results, leave all offers unselected and the Details
  panel empty on initial load (**AC05–AC06**).
- [x] **Task 3.3 — Offer selection and details:** Selecting an offer displays Company, Job Title,
  Job Description, Required Qualifications, Years of Experience, Location, and Allocation.
  Selecting another offer replaces the selection and displayed details (**AC07–AC08**).
- [x] **Task 3.4 — Retrieval failure UI:** Display an error state distinct from the successful
  empty state when the API request fails (**AC09**).
- [x] **Task 3.5 — Safe text rendering:** Render returned fields as text, never interpreting them
  as HTML or script. Add coverage with markup-like field values (**AC10**).

## Phase 4: Scope Review and Acceptance Verification

- [x] **Task 4.1 — Read-only boundary clarification:** AC11 applies to the behavior introduced by
  this SPA Skeleton feature. The later `02-manage-job-position` feature owns job-offer create,
  update, and delete behavior; those operations remain outside this feature's contribution.
- [x] **Task 4.2 — Acceptance coverage and verification:** Confirm every AC01–AC12 has corresponding
  automated or browser-level verification. Run the relevant suites and address failures before
  review.
- [x] **Task 4.3 — Reviewer report:** Review the implementation against the requirements and design;
  record compliance, acceptance coverage, gaps, and improvement suggestions in `04-review.md`.
