# Technical Design: Show Candidates

- **Status:** [ ] Draft | [x] Approved for Implementation
- **Author/Agent:** @architect
- **Reference Spec:** `01-requirements.md`

## 1. Architecture

Extend the existing read-only SPA and Spring MVC backend. The Candidates tab loads paged candidate
summaries from MongoDB and loads a selected candidate's details by persisted ID. Qdrant is not used
for candidate display.

| Component                             | Responsibility                                                                                       |
|---------------------------------------|------------------------------------------------------------------------------------------------------|
| `CandidateController`                 | Expose read-only list and detail endpoints under `/api/candidates`.                                  |
| `CandidateQueryService`               | Validate page requests and coordinate summary/detail reads.                                          |
| `CandidateRepository`                 | Define the candidate page and ID lookup operations used by display.                                  |
| `MongoCandidateRepository`            | Execute a bounded summary query, count candidates, and load a candidate by ID using `MongoTemplate`. |
| `CandidateSummaryResponse`            | Return only ID and the fields shown in list entries.                                                 |
| `CandidatePageResponse`               | Return the current zero-based page, fixed page size, total elements/pages, and summaries.            |
| `CandidateDetailsResponse`            | Return only the structured candidate fields shown in the details panel.                              |
| `CandidateControllerExceptionHandler` | Map invalid page, missing candidate, and storage errors to controlled responses.                     |
| Candidates-tab UI functions           | Fetch pages and details, render loading/empty/error states, and manage selection and pagination.     |

Keep read operations separate from ingestion's email-based replacement and vector persistence. Do
not add write operations to the candidate display endpoints.

## 2. API Contract

### 2.1 List candidate summaries

`GET /api/candidates?page=0`

- `page` is a zero-based integer and defaults to `0`.
- The server always uses a page size of `15`; clients cannot increase it.
- The result is sorted by `fullname` using case-insensitive collation, then by candidate ID in
  ascending order.
- Empty storage returns HTTP `200` with an empty `items` array, `totalElements: 0`, and
  `totalPages: 0`.
- A negative or non-integer page returns HTTP `400` with error code
  `CANDIDATE_PAGE_INVALID`.
- A page outside the currently available range returns HTTP `400` with error code
  `CANDIDATE_PAGE_OUT_OF_RANGE`. For an empty collection, page `0` remains valid.
- MongoDB read failure returns HTTP `503` with error code `CANDIDATES_UNAVAILABLE`.

Successful response shape:

```json
{
  "items": [
    {
      "id": "candidate-id",
      "fullname": "Jane Doe",
      "position": "Software Engineer",
      "country": "Brazil",
      "city": "São Paulo",
      "yearsOfExperience": "5 years and 0 months"
    }
  ],
  "page": 0,
  "pageSize": 15,
  "totalElements": 1,
  "totalPages": 1
}
```

The summary response must not include email, skills, experience records, resume text, or vector
payload.

### 2.2 Load candidate details

`GET /api/candidates/{id}`

- `id` is the persisted MongoDB candidate ID.
- A missing candidate returns HTTP `404` with error code `CANDIDATE_NOT_FOUND`.
- MongoDB read failure returns HTTP `503` with error code `CANDIDATE_DETAILS_UNAVAILABLE`.

Successful response shape:

```json
{
  "id": "candidate-id",
  "email": "jane@example.com",
  "fullname": "Jane Doe",
  "country": "Brazil",
  "city": "São Paulo",
  "position": "Software Engineer",
  "skills": "Java, Spring",
  "allocation": "Remote",
  "yearsOfExperience": "5 years and 0 months",
  "experiences": [
    {
      "title": "Software Engineer",
      "company": "Example Co.",
      "period": "2021 – Present",
      "responsibilities": "Built backend services.",
      "achievements": "Improved service reliability."
    }
  ]
}
```

An absent candidate ID resolves as not found. The detail response excludes original resume content,
LLM prompts/responses, MongoDB internal metadata, and Qdrant embeddings.

## 3. Persistence and Query Design

Use the existing `Candidate` MongoDB document and `candidates` collection. Add read methods to the
candidate repository abstraction without changing its ingestion methods:

- Read a `CandidatePageResponse` projection for a zero-based page and fixed size of 15.
- Count candidates for page metadata.
- Find a candidate by persisted ID for details.

The Mongo adapter should query only fields needed for the summary projection and apply server-side
pagination and the ordering from BR07. Apply a case-insensitive collation to the `fullname` sort
and `_id` as the tie-breaker. Details are loaded separately after selection, so list responses do
not transfer experience arrays or email addresses.

The candidate count and page query are separate MongoDB reads. If the collection changes between
them, the service may return a page that is no longer available. The UI handles an out-of-range
response by clearing selection/details and reloading page `0`. No snapshot consistency guarantee
is required.

## 4. UI Flow

1. On initial application load, fetch candidate page `0` for the active Candidates tab. Fetch a
   fresh page when the user opens the tab again.
2. While the page is loading, show an accessible loading state. On success, render at most 15
   candidate entries and the page controls from the response metadata.
3. Render all candidate-provided values with DOM text nodes / `textContent`; do not inject candidate
   values as HTML.
4. Selecting an entry marks it selected, clears the previous detail content, displays a detail
   loading state, and requests `/api/candidates/{id}`.
5. Render the response in the existing right-hand details panel. Show scalar values as an em dash
   when empty and show a clear empty-experiences message when `experiences` is empty.
6. Use previous/next controls to fetch the adjacent page. Disable previous on page `0` and next on
   the last page. Clear the current selection and details when changing pages.
7. Use a selected-ID guard when resolving asynchronous detail requests. If the user changes
   selection before an earlier request completes, discard the earlier response instead of
   rendering stale details.
8. On list or detail failure, show a non-sensitive message and a retry action in the affected
   panel. Keep the Open Positions tab available.
9. On detail `404`, clear selection/details and reload the current page. On page-out-of-range,
   clear selection/details and reload page `0`.
10. With no candidates, render an empty-list message, hide page controls, and leave the details
    panel without candidate data.

The current markup already has a left candidate list and right details panel. Add semantic list and
pagination controls with keyboard operation, selected-state attributes, and live status regions for
loading, empty, and error messages. Candidate entries show only `[name] - [position]` and each entry
has a fine gray horizontal separator below it.

## 5. Error Handling and Privacy

- Scope the controller advice to `CandidateController` so job-offer error behavior is unaffected.
- Return stable error codes and generic messages; do not expose exception messages or database
  details in API responses.
- Log retrieval failures with a static stage/category and sanitized cause. Do not log candidate
  ID, email, names, skills, experience data, response bodies, or query contents.
- Do not log successful candidate reads or selected candidate values.
- Do not query or return Qdrant data from this feature.

## 6. Verification Mapping

| Acceptance criterion | Design verification                                                                                            |
|----------------------|----------------------------------------------------------------------------------------------------------------|
| AC01                 | Controller/service/repository tests verify first-page summary fields, ordering, and a bounded result; UI tests verify the `[name] - [position]` label and separator. |
| AC02                 | Repository and controller tests verify 15-record pages, page metadata, boundaries, and bounded responses.      |
| AC03                 | UI test verifies selection, ID-based details loading, and fields/experience rendering.                         |
| AC04                 | UI test selects two entries and verifies the second selection replaces the first details.                      |
| AC05                 | Empty Mongo result and UI test verify empty state, zero pages, hidden controls, and no details.                |
| AC06                 | UI tests cover missing scalar placeholders and an empty experience list.                                       |
| AC07                 | Controller/UI tests verify `503`, safe message, retry action, and continued Open Positions use.                |
| AC08                 | UI test verifies detail failure state and ignores stale details after selection changes.                       |
| AC09                 | Controller and UI tests verify `404`, cleared selection, and current-page refresh.                             |
| AC10                 | API tests verify read-only endpoints and response fields; log-capture test checks candidate values are absent. |
| AC11                 | UI tests verify keyboard selection, pagination, and accessible selected/loading/empty/error states.            |

## 7. Design Boundaries

- Do not modify ingestion behavior, candidate records, or Qdrant embeddings.
- Do not add candidate create, edit, delete, upload, search, filter, matching, or ranking behavior.
- Do not expose additional candidate fields beyond the summary and detail DTOs defined above.
- Do not add authentication or authorization changes; access control remains outside this feature.
- Keep routes read-only and leave the existing Open Positions functionality unchanged.
