# Technical Design: Job-Offer Management

- **Status:** [x] Draft | [ ] Approved for Implementation
- **Author/Agent:** @architect
- **Reference Spec:** `01-requirements.md`

## 1. Architecture

Extend the existing Spring Boot SPA with job-offer create, read, update, and delete behavior.
Spring MVC serves the static files from `src/main/resources/static` and handles the `/api` routes.
Spring Data MongoDB reads and writes the existing `job_offers` collection.

Keep the feature in the `net.wowdev.ai.resume.matcher` package. Use the existing MongoDB document,
repository, service, request/response DTO, controller, and exception-handler patterns. The service
validates and persists create/update/delete operations; the controller translates service results
to HTTP responses. Keep the selected offer in browser state, with no more than one selected offer.

Use the individual MongoDB connection properties. Do not configure or use `spring.mongodb.uri`.
Keep credentials out of source-controlled configuration; resolve optional username and password
from environment-backed properties.

## 2. Data Model and Field Mapping

Continue using `job_offers`. Map MongoDB `_id` to API `id`. Preserve the existing stored `jobTitle`
field and expose it to the management API as `jobName` so the form, create/update request, and
Details view use the requirement's Job Name terminology.

The six editable fields are:

| API field | Meaning | Validation |
|---|---|---|
| `company` | Hiring company name | Required |
| `jobName` | Job Name; stored in MongoDB `jobTitle` | Required |
| `jobDescription` | Job description stored as Markdown source | Required |
| `requiredQualifications` | Qualifications stored as Markdown source | Required |
| `location` | Job location | Required |
| `allocation` | Work allocation | Required; `Remote`, `Hybrid`, or `On-site` |

The existing collection also has a `jobId` value used by the Open Positions list. It is not an
editable form field in the current requirements. The requirements do not specify how a new offer
gets a `jobId`; this blocks finalizing the list behavior after create until the rule is clarified.
Do not add Years of Experience to the job-offer form or Details panel.

Treat null, empty, and whitespace-only required strings as invalid. Do not seed records or add
fields beyond the six editable values, the MongoDB identifier, and the existing catalog `jobId`.

## 3. REST API

All routes use `/api/job-offers`.

### `GET /api/job-offers`

Return the catalog with `200 OK`; an empty collection returns `[]`. Return each offer's `id`,
`jobId`, `jobName`, `company`, `jobDescription`, `requiredQualifications`, `location`, and
`allocation`. The frontend uses `jobTitle`, `company`, and `jobId` to form the required label:
`[jobTitle] - [company] - [jobId]`.

`jobName` is mapped from stored MongoDB `jobTitle`. Whether `jobId` can be absent for newly created
offers remains unresolved as described in Section 9.

### `POST /api/job-offers`

Accept the six editable fields. The server generates MongoDB `id`, validates the request, persists
the offer, and returns `201 Created`, the saved response, and a `Location` header for
`/api/job-offers/{id}`.

### `PUT /api/job-offers/{id}`

Accept the six editable fields and replace the values on the identified offer. Return `200 OK` and
the saved response. Resolve the identifier before saving; do not upsert. Return `404 Not Found` when
the identifier does not exist.

### `DELETE /api/job-offers/{id}`

Delete the identified offer and return `204 No Content`. Return `404 Not Found` when the
identifier does not exist.

### Errors

- Invalid create/update data returns `400 Bad Request` with stable code
  `JOB_OFFER_VALIDATION_FAILED`, a general message, and field-addressable errors.
- Unknown identifiers return `404 Not Found` with stable code `JOB_OFFER_NOT_FOUND`.
- MongoDB read failures return `503 Service Unavailable` with code `JOB_OFFERS_UNAVAILABLE`.
- MongoDB write failures return `503 Service Unavailable` with a stable job-offer persistence
  error code.
- Error responses must not include credentials, stack traces, or internal exception details.

## 4. SPA List, Selection, and Details

- Load the catalog once from `GET /api/job-offers` on page load.
- Show each offer using the exact text format `[jobTitle] - [company] - [jobId]`.
- Represent each offer as a selectable list entry, not a button, as required by Item 4.
- Selecting one entry clears the prior selection and displays that offer's details.
- Show Company above the Job Description section. Show Job Name, Job Description, Required
  Qualifications, Location, and Allocation in the Details panel.
- Keep **New Job Offer** visible at all times. Show **Edit** and **Delete** only while an offer is
  selected.
- A successful create adds and selects the returned offer. A successful edit updates and retains
  the selected offer. A successful delete removes the selected entry and clears the Details panel.
- A failed write leaves the existing list and Details state unchanged and displays an error.

## 5. Create and Edit Form

Use one modal and form for both modes. New mode starts with empty Company, Job Name, Job
Description, Required Qualifications, Location, and Allocation fields. Edit mode loads the selected
offer's values into those same fields.

Allocation is a select with exactly `Remote`, `Hybrid`, and `On-site`. Validate required fields
when Save is submitted, not before. If fields are missing, keep the modal open, mark only empty
fields with a red border, and associate an accessible required message with each invalid field.

Store the form's opening values to detect changes. Cancel closes an unchanged form immediately. If
the form changed, show an in-app discard prompt. Discard closes without a write; continue editing
closes the prompt and preserves entered values.

## 6. Markdown and Safe Rendering

Store Job Description and Required Qualifications as Markdown source. Render them in the Details
panel with a client-side Markdown renderer configured to disable raw HTML. Sanitize generated
markup before insertion into the document. Render Company, Job Name, Location, Allocation, list
labels, and error text as text.

## 7. Button Styling

Every actual button in the job-offer feature uses the same visual style as **New Job Offer**, with
font size `0.75rem` and height `35px`. This includes action buttons, form buttons, and discard
prompt buttons. The requirements also mention list-selection buttons, which conflicts with Item 4's
instruction that list entries are not buttons; resolve this conflict before approval.

## 8. Verification Mapping

| Requirement | Design verification |
|---|---|
| Items 1–3; AC01–AC03 | UI tests cover selection, Details display, and New/Edit visibility. |
| Item 4 | Verify exact `[jobTitle] - [company] - [jobId]` labels and confirm list entries are not buttons. |
| Items 4–7; AC04–AC07 | Frontend tests cover modal fields, initial values, edit initialization, allocation options, and deferred accessible validation. |
| Items 8–11; AC08–AC11 | API/UI tests cover successful and failed create/update and resulting modal/list/details state. |
| Items 12–13; AC12–AC14 | API/UI tests cover delete visibility, successful removal, failed deletion, and Details state. |
| Items 13 and 15; AC15 | Tests verify Company is required, saved, loaded, and shown above Job Description. |
| Item 14; AC16 | UI tests verify Markdown formatting and safe rendering. |
| Item 15; AC17 | Style tests verify shared button appearance, `0.75rem` font size, and `35px` height. |

## 9. Decisions Needed Before Approval

1. **Job ID for newly created offers:** The list format requires `jobId`, but the form has no Job ID
   field and no generation rule is specified. Decide whether the ID is generated, entered, or may
   be absent.
2. **List entry semantics and button styling:** Item 4 prohibits button list entries, while Item
   15 includes list selection among the buttons that must share styling. Decide which requirement
   takes precedence or revise them to be consistent.
3. **Acceptance coverage for Item 4:** Item 4 has no corresponding acceptance criterion. Add one
   before implementation and reviewer approval.

## 10. Boundaries

- Do not add candidate persistence, ingestion, matching, ranking, or recommendation behavior.
- Do not add job-offer fields beyond the six editable fields and the specified catalog `jobId`.
- Do not add authentication or authorization behavior.
- Do not configure `spring.mongodb.uri`.
- Keep implementation behavior traceable to `01-requirements.md`.
