# Task List: Job-Offer Management

- **Status:** [ ] Ready | [ ] Active | [ ] Paused | [x] Completed
- **Target Agent:** @engineer
- **Validation Agent:** @reviewer
- **References:** `01-requirements.md`, `02-design.md`

> Complete one task at a time. Keep each change within its listed scope and add focused tests for
> the behavior introduced by that task.

## Phase 1: Data Access and Business Logic

- [x] **Task 1.1 — Write-capable repository methods:** Extend the job-offer repository with the
  minimal persistence operations needed for create, lookup, update, and delete. Add focused tests
  confirming the repository contract supports the required operations.
- [x] **Task 1.2 — Request and error DTOs:** Define request and response types for create/update
  and stable API error responses, preserving the documented JSON field names. Add serialization
  tests for these types.
- [x] **Task 1.3 — Required-field validation:** Implement server-side validation for all five
  editable fields, including whitespace-only values and the allowed Allocation values. Add tests
  for valid input, each missing/blank field, and unsupported Allocation values.
- [x] **Task 1.4 — Create and update service behavior:** Implement validated create and update
  behavior; update must find an existing offer and must not upsert. Add tests for successful create,
  successful update, and missing identifiers.
- [x] **Task 1.5 — Delete service behavior:** Implement deletion of an existing offer and
  not-found behavior for unknown identifiers. Add focused success and not-found tests.
- [x] **Task 1.6 — Persistence failure behavior:** Ensure service operations propagate or translate
  persistence failures without representing them as successful writes. Add tests for create,
  update, and delete failure paths.

## Phase 2: REST API

- [x] **Task 2.1 — Create endpoint:** Implement `POST /api/job-offers`, returning `201 Created`,
  the saved offer, and a `Location` header. Add controller tests for success and invalid input.
- [x] **Task 2.2 — Update endpoint:** Implement `PUT /api/job-offers/{id}`, returning the saved
  offer, `400 Bad Request` for invalid values, and `404 Not Found` for unknown identifiers. Add
  controller tests for each response.
- [x] **Task 2.3 — Delete endpoint:** Implement `DELETE /api/job-offers/{id}`, returning
  `204 No Content` for success and `404 Not Found` for unknown identifiers. Add controller tests.
- [x] **Task 2.4 — Consistent API errors:** Map validation, not-found, and MongoDB failures to the
  documented stable error codes and response statuses; exclude internal exception details. Add
  controller tests for `400`, `404`, and `503` responses.

## Phase 3: Job-Offer Selection and Actions

- [x] **Task 3.1 — Selection and action visibility:** Preserve single selection in the Open
  Positions list; show **New Job Offer** at all times and show **Edit** and **Delete** only when an
  offer is selected. Add frontend tests for selection and visibility (**AC01–AC03, AC12**).
- [x] **Task 3.2 — New-offer modal:** Add the shared modal and empty form with the five required
  fields and **Cancel**/**Save** actions. Add frontend tests for opening the form and its initial
  values (**AC04**).
- [x] **Task 3.3 — Edit form initialization:** Open the same modal in edit mode with the selected
  offer’s current values. Add frontend tests for every initialized field (**AC07**).
- [x] **Task 3.4 — Allocation selector:** Provide exactly **Remote**, **Hybrid**, and **On-site**
  as Allocation choices. Add a frontend test for the available options (**AC06**).
- [x] **Task 3.5 — Deferred accessible validation:** On Save submission, identify empty required
  fields, mark only those fields invalid with red borders and accessible error associations, and
  prevent the request. Do not show validation before the first Save attempt. Add frontend tests
  covering empty and populated fields (**AC05**).

## Phase 4: Save, Cancel, and Delete Interactions

- [x] **Task 4.1 — Create success flow:** Submit valid new values; on success close the modal,
  add the returned offer to the list, select it, and show its details. Add frontend tests (**AC08**).
- [x] **Task 4.2 — Edit success flow:** Submit valid edited values; on success close the modal,
  update the list, keep the offer selected, and show returned details. Add frontend tests (**AC09**).
- [x] **Task 4.3 — Failed save flow:** On create or update failure, keep the modal and entered
  values, show an error, and leave list/details unchanged. Add frontend tests (**AC11**).
- [x] **Task 4.4 — Cancel and discard flow:** Close unchanged forms immediately; for changed
  forms, show the custom discard prompt and preserve values when the recruiter chooses to continue
  editing. Add frontend tests for discard and continue (**AC10**).
- [x] **Task 4.5 — Delete success flow:** Delete the selected offer; on success remove it from the
  list, clear details, and hide **Edit**/**Delete**. Add frontend tests (**AC13**).
- [x] **Task 4.6 — Failed deletion flow:** On delete failure, keep the selected offer and details
  visible and show an error. Add frontend tests (**AC14**).

## Phase 5: Acceptance Verification and Review

- [x] **Task 5.1 — Acceptance coverage and test run:** Verify each AC01–AC14 has automated test
  coverage and run the backend and frontend test suites. Fix failures before review.
- [x] **Task 5.2 — Scope review:** Confirm there are no candidate data operations, all job-offer
  changes follow the specified CRUD behaviors, and no unrequested fields or workflows were added.
- [x] **Task 5.3 — Reviewer report:** Review implementation against the requirements and design;
  record spec compliance, acceptance coverage, gaps, and improvement suggestions.
