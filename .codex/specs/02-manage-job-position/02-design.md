# Technical Design: Job-Offer Management

- **Status:** [ ] Draft | [x] Approved for Implementation
- **Author/Agent:** @architect
- **Reference Spec:** `01-requirements.md`

## 1. Architecture

Extend the existing Spring Boot single-page application with job-offer list, create, update, and
delete behavior. Spring MVC serves the page and `/api` endpoints. The static frontend remains in
`src/main/resources/static` and uses HTML, CSS, Bootstrap, and vanilla JavaScript. Spring Data
MongoDB persists offers in the existing `job_offers` collection.

Keep the feature in the existing `net.wowdev.ai.resume.matcher` package structure with a MongoDB
document, repository, service, request/response DTOs, and controller. The service owns validation
and persistence behavior; controller endpoints remain thin. Keep selection state in the browser,
with at most one offer selected in the Details panel.

Read the MongoDB URI from `application.properties`, with

- `spring.mongodb.host=localhost`
- `spring.mongodb.port=27017`
- `spring.mongodb.username=admin`
- `spring.mongodb.password=secretpassword`
- `spring.mongodb.database=resume_matcher`
- `spring.mongodb.authentication-database=admin`
  as the local default. Keep credentials out of source-controlled configuration.

## 2. Job-Offer Data

Continue using the `job_offers` collection. MongoDB `_id` maps to the API's `id`. The editable
fields are:

| API field                | Meaning                                          | Validation                                     |
|--------------------------|--------------------------------------------------|------------------------------------------------|
| `company`                | Hiring company name                              | Required                                       |
| `jobTitle`               | Job Title                                        | Required                                       |
| `jobDescription`         | Job description, stored as Markdown text         | Required                                       |
| `requiredQualifications` | Required qualifications, stored as Markdown text | Required                                       |
| `yearsOfExperience`      | Required years of experience                     | Required                                       |
| `location`               | Job location                                     | Required                                       |
| `allocation`             | Work allocation                                  | Required; one of `Remote`, `Hybrid`, `On-site` |


The server validates all six fields for create and update. Treat blank or whitespace-only values
as empty. Do not seed records or make persistence changes beyond these fields and the identifier.

## 3. REST API

All job-offer endpoints use `/api/job-offers`. Successful response objects use `id`, `company`,
`jobTitle`, `jobDescription`, `requiredQualifications`, `yearsOfExperience`, `location`, and 
`allocation`.

### `GET /api/job-offers`

Return all offers with `200 OK`. An empty collection returns `[]`.

### `POST /api/job-offers`

Create an offer from the six editable fields. The server generates the MongoDB identifier and
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
    "company": "required",
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

- Load offers from `GET /api/job-offers` on page load. Preserve the existing empty and retrieval
  error states.
- Selecting an offer makes it the only selected list item and displays its details in the right
  panel.
- Display Company above the Job Description section in the Details panel.
- Keep **New Job Offer** visible above the Details panel at all times. Show **Edit** and **Delete**
  there only while an offer is selected.
- Apply the same visual style as **New Job Offer** to every button in the job-offer feature,
  including list selection, action, form, and discard-confirmation buttons. Set each to `0.75rem`
  font size and `35px` height.

### Create and edit modal

Use one modal and form for both modes. New mode starts with empty values. Edit mode loads the
selected offer's Company, Job Name, Job Description, Required Qualifications, Location, and
Allocation values. Provide labeled controls for all six fields, plus **Cancel** and **Save**.
Allocation is a select containing exactly `Remote`, `Hybrid`, and `On-site`.

Validate on Save submission. Do not display required-field errors before the first Save attempt.
On an invalid attempt, keep the modal open, mark each empty field with a red border, and expose an
accessible required indication associated with that field. Do not mark valid fields as empty.

Track the original form values on open. Cancel closes an unchanged form immediately. If values have
changed, show an in-app confirmation prompt with actions to discard or continue editing. Discard
closes the modal without sending a write request. Continue editing closes the prompt and preserves
the current form values.

### Markdown display

Store Job Description and Required Qualifications as Markdown source text. In the Details panel,
render their Markdown formatting with a client-side Markdown renderer. Disable raw HTML in the
source and sanitize generated markup before inserting it into the document. Render Company, Job
Name, Location, Allocation, list labels, and error messages as text.

### Successful writes

- After successful creation, close the modal, add the returned offer to the list, select it, and
  display its details.
- After successful update, close the modal, update the list entry, keep that offer selected, and
  display the returned values.
- After successful deletion, remove the offer from the list and clear the Details panel. Hide
  **Edit** and **Delete** when no offer is selected; keep **New Job Offer** visible.

### Failed writes

- If create or update fails, keep the modal and entered values open and display the error. Do not
  update the list or Details panel as though the write succeeded.
- If delete fails, keep the offer selected in the list and Details panel and display the error.
- Do not mutate browser state until the corresponding write request succeeds.

## 5. Error Handling and Security

- Distinguish validation errors (`400`), missing offers (`404`), and unavailable persistence
  (`503`).
- Client-side required validation provides visible and accessible field feedback only after Save is
  attempted; server-side validation remains authoritative.
- Preserve form values after failed saves so the recruiter can correct or retry.
- Do not expose stack traces, database details, or credentials in API responses.
- Render Markdown with raw HTML disabled and sanitize generated markup. Treat all non-Markdown
  values and error text as text.

## 6. Verification Mapping

| Requirement                                      | Design verification                                                                                      |
|--------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| AC01–AC03, AC12: selection and action visibility | UI tests check single selection and New Job Offer, Edit, and Delete visibility.                          |
| AC04, AC07: create/edit forms                    | UI tests check all six fields, empty new values, and values loaded for editing.                          |
| AC05–AC06: validation and allocation             | Tests check deferred required-field validation, accessible errors, and exact allocation choices.         |
| AC08–AC09: successful create/update              | API and UI tests check stored/returned values, modal close, list update, selection, and details.         |
| AC10: cancel/discard                             | UI tests cover unchanged cancel, discard, and continue editing with values preserved.                    |
| AC11: failed save                                | API and UI tests check the open modal, preserved values, error, and unchanged saved state.               |
| AC13–AC14: delete success/failure                | API and UI tests check persistence outcomes, list/details state, and errors.                             |
| AC15: Company field and display order            | UI and API tests check Company is required, saved, loaded, and displayed above Job Description.          |
| AC16: Markdown details                           | UI tests check Markdown formatting in Job Description and Required Qualifications.                       |
| AC17: button styling                             | UI/style tests check all job-offer buttons match New Job Offer, use `0.75rem` text, and are `35px` high. |

## 7. Boundaries

- Do not add candidate persistence, candidate ingestion, matching, or ranking behavior.
- Do not add job-offer fields beyond the six specified editable fields and MongoDB `id`.
- Do not add authentication or authorization behavior in this feature.
- Keep implementation behavior traceable to `01-requirements.md`.
