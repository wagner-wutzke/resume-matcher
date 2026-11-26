# Technical Design: Job-Offer Management

- **Status:** [ ] Draft | [x] Approved for Implementation
- **Author/Agent:** @architect
- **Reference Spec:** `01-requirements.md`

## 1. Architecture

Extend the existing Spring Boot SPA with job-offer create, update, and delete API operations. Keep
the single-page frontend in `src/main/resources/static` using HTML, CSS, Bootstrap, and vanilla
JavaScript. Spring MVC serves the page and `/api` endpoints. Spring Data MongoDB persists offers
in the existing `job_offers` collection.

Use the existing base package `net.wowdev.ai.resume.matcher`. Keep the feature in a small cohesive
`joboffer` package containing the MongoDB document, repository, service, request/response DTOs, and
controller. The service owns validation and create/update/delete behavior so controller endpoints
remain thin. Keep UI selection state in the browser; there is no separate active-position field in
MongoDB. At most one offer is selected in the Details panel at a time.

Read the MongoDB URI from `MONGODB_URI`, with
`mongodb://localhost:27017/resume_matcher` as the local default. Keep credentials out of
source-controlled configuration.

## 2. Job-Offer Data

Continue using the existing `job_offers` collection and `JobOffer` document. MongoDB `_id` maps to
the API's `id`. The editable fields are:

| API field | Meaning | Validation |
| --- | --- | --- |
| `jobName` | Job name | Required |
| `jobDescription` | Job description | Required |
| `requiredQualifications` | Required qualifications | Required |
| `location` | Job location | Required |
| `allocation` | Work allocation | Required; one of `Remote`, `Hybrid`, `On-site` |

The server validates every field for create and update. Treat a blank or whitespace-only value as
empty. Do not create seed records or introduce other persistence changes for this feature.

## 3. REST API

All job-offer endpoints use `/api/job-offers`. Successful response objects use `id`, `jobName`,
`jobDescription`, `requiredQualifications`, `location`, and `allocation`.

### `GET /api/job-offers`

Return all offers with `200 OK`. An empty collection returns `[]`.

### `POST /api/job-offers`

Create an offer from the five editable fields. The server generates the MongoDB identifier and
returns the saved offer with `201 Created` and a `Location` header pointing to
`/api/job-offers/{id}`.

### `PUT /api/job-offers/{id}`

Replace the editable values on an existing offer and return the saved offer with `200 OK`. Resolve
the identifier first; do not upsert. If the identifier is unknown, return `404 Not Found`.

### `DELETE /api/job-offers/{id}`

Delete the identified offer and return `204 No Content`. If the identifier is unknown, return
`404 Not Found`.

### Validation and persistence errors

For invalid create/update values, return `400 Bad Request` with a field-addressable error body:

```json
{
  "code": "JOB_OFFER_VALIDATION_FAILED",
  "message": "The job offer is invalid.",
  "fieldErrors": {
    "jobName": "required",
    "allocation": "unsupported value"
  }
}
```

For MongoDB failures, return `503 Service Unavailable` with a stable error code and message. Do not
report a failed operation as a successful write or empty result. For a missing identifier, return
`404 Not Found` with a stable not-found code. Error bodies must not include database credentials
or internal exception details.

## 4. SPA State and Interaction

### List and details

- Load offers from `GET /api/job-offers` on page load. Keep the read-only baseline's empty and
  retrieval-error states.
- Selecting an offer makes it the only selected list item and displays its details in the right
  panel.
- Show **New Job Offer** above the Details panel regardless of selection.
- Show **Edit** and **Delete** above the Details panel only while an offer is selected.

### Create and edit modal

Use one modal and one form for both modes. New mode starts with empty values. Edit mode starts with
the selected offer's values. The form contains labeled controls for Job Name, Job Description,
Required Qualifications, Location, and Allocation, plus **Cancel** and **Save** buttons. Allocation
is a select control containing exactly `Remote`, `Hybrid`, and `On-site`.

Validate on Save submission. Do not show required-field validation before the first submit attempt.
On an invalid attempt, keep the modal open, mark each empty field with the specified red border,
and expose the error through an accessible relationship to its field (for example, `aria-invalid`
and an associated error description). Do not mark valid fields as empty.

Track the original form values on open. Cancel closes an unchanged form immediately. If values have
changed, show an in-app confirmation prompt with actions to discard or continue editing. Discard
closes the modal without sending a write request; continue editing closes the prompt and preserves
the current form values.

### Successful writes

- After a successful create, close the modal, add the returned offer to the list, select it, and
  show its details.
- After a successful update, close the modal, update the list entry, keep that offer selected, and
  show the returned values in the Details panel.
- After a successful delete, remove the offer from the list and clear the Details panel. With no
  selected offer, hide **Edit** and **Delete**; **New Job Offer** remains visible.

### Failed writes

- If create or update fails, keep the modal and values open and display the server error. Do not
  update the list or Details panel as if the write had succeeded.
- If delete fails, keep the offer selected in the list and Details panel and display the server
  error.
- Render all job-offer values and error text as text, not HTML.

## 5. Error Handling

- Distinguish validation errors (`400`) from missing offers (`404`) and unavailable persistence
  (`503`).
- Client-side validation provides immediate visible and accessible field feedback only after Save
  is attempted; server-side validation remains authoritative.
- Preserve form values after failed saves so the recruiter can correct or retry.
- Do not mutate browser state until the corresponding write request succeeds.
- Do not expose stack traces, database details, or credentials in API responses.

## 6. Verification Mapping

| Requirement | Design verification |
| --- | --- |
| AC01–AC03, AC12: selection and action visibility | UI tests check single selection and the visibility rules for Edit, New Job Offer, and Delete. |
| AC04: new form | UI test opens New Job Offer and checks blank fields and form actions. |
| AC05–AC06: validation and allocation | Form tests check deferred validation, accessible errors, required fields, and the exact allocation options. |
| AC07: edit form | UI test opens Edit and checks every field is initialized from the selected offer. |
| AC08–AC09: successful create/update | API and UI tests verify persisted response values, modal close, list update, selection, and details update. |
| AC10: cancel/discard | UI tests cover unchanged cancel, discard, and continue editing with values preserved. |
| AC11: failed save | API failure test plus UI test verify modal and values remain and saved state does not change. |
| AC13–AC14: delete success/failure | API and UI tests verify persistence outcomes, list/details state, and error handling. |

## 7. Boundaries

- Do not add candidate persistence, candidate ingestion, matching, or ranking behavior.
- Do not add job-offer fields beyond the five specified editable fields and MongoDB `id`.
- Do not add authentication or authorization behavior in this feature.
- Keep all implementation behavior traceable to `01-requirements.md`.
