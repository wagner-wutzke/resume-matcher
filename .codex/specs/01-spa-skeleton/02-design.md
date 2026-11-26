# Technical Design: Resume Matcher SPA Skeleton

- **Status:** [ ] Draft | [x] Approved for Implementation
- **Author/Agent:** @architect
- **Reference Spec:** `01-requirements.md`

## 1. Architecture

Use a Spring Boot web application to serve a static single-page frontend and a small read-only API.
The frontend uses HTML, CSS, Bootstrap, and vanilla JavaScript. Spring MVC serves the static files
from `src/main/resources/static` and handles the `/api` routes. Spring Data MongoDB reads existing
job-offer records from MongoDB.

The existing application base package is `net.wowdev.ai.resume.matcher`. Keep API code within this
package, organized into `joboffer` model/repository/controller types or equivalent small cohesive
components. No separate service layer is required unless it provides needed behavior beyond the
repository call.

The SPA entry point is `src/main/resources/static/index.html`, with its CSS and JavaScript in
adjacent static assets. The server serves the application at `http://localhost:8080`.

## 2. MongoDB Document

Read records from the existing `job_offers` collection. Map the stored MongoDB `_id` to the
application's `id` property. The fields used by this feature are:

```json
{
  "_id": "MongoDB ObjectId",
  "jobName": "Backend Engineer",
  "jobDescription": "Job description text",
  "requiredQualifications": "Required experience and skills",
  "location": "Dublin",
  "allocation": "Remote"
}
```

The API exposes the identifier as `id` and returns the five display fields: `jobName`,
`jobDescription`, `requiredQualifications`, `location`, and `allocation`. Keep these values as text.
Do not add writes, seed data, schema migrations, or collection lifecycle behavior in this feature.

Configure the MongoDB connection through `MONGODB_URI`; use
`mongodb://localhost:27017/resume_matcher` as the local development default. Do not store database
credentials in source-controlled configuration.

## 3. API Contract

### `GET /api/job-offers`

Returns all records in the `job_offers` collection. The request has no body or query parameters.

**Success — `200 OK`**

```json
[
  {
    "id": "6650c4c93f1b8f0a6f8a1234",
    "jobName": "Backend Engineer",
    "jobDescription": "Job description text",
    "requiredQualifications": "Required experience and skills",
    "location": "Dublin",
    "allocation": "Remote"
  }
]
```

When the collection is empty, return `200 OK` with `[]`.

**Retrieval failure — `503 Service Unavailable`**

```json
{
  "code": "JOB_OFFERS_UNAVAILABLE",
  "message": "Job offers could not be loaded."
}
```

Do not translate a database or retrieval failure into a successful empty result. This allows the
SPA to distinguish an empty collection from a failed request.

## 4. Request and Rendering Flow

1. The browser loads the SPA and its static assets from the Spring Boot application.
2. The frontend sends one `GET /api/job-offers` request on application load.
3. The API reads the collection and serializes the result using the contract above.
4. On success, the frontend adds each job name to the Open Positions list.
5. The details panel begins empty. Selecting a list entry renders the selected offer's job name,
   job description, required qualifications, location, and allocation.
6. The frontend inserts all values as text nodes or text properties. It does not treat returned data
   as HTML.
7. An empty successful response leaves the list and details panel empty. A failed request displays
   a distinct retrieval failure state and leaves the details panel without a selected offer.

The Candidates tab is a placeholder in this feature. It has left and right content regions but
does not load or display candidate data.

## 5. SPA Layout and Visual Direction

- Show **Resume Matcher** as the application name and **Candidates Matching made easy** as the
  visible title.
- Provide **Candidates** and **Open Positions** tabs.
- Use a two-panel layout in each tab: a list area on the left and a details area on the right.
- Keep candidate list and details content empty in this phase.
- Use a simple dark gray and royal blue palette and let the application occupy the available page
  space.
- Keep the layout and behavior clear at the viewport sizes supported by the application. Exact
  colors, typography, responsive breakpoints, and empty/failure copy remain presentation choices.

## 6. Error Handling

- An empty `job_offers` collection is a successful response and produces an empty list.
- A MongoDB or API retrieval failure is reported as an unsuccessful request and shown as a distinct
  failure state in Open Positions.
- A failure must not clear the distinction between an unavailable result and a known empty result.
- Job-offer text is rendered without HTML interpretation.

## 7. Verification Mapping

| Requirement area                       | Design verification                                                                               |
|----------------------------------------|---------------------------------------------------------------------------------------------------|
| AC01–AC02: application and identity    | Start the Spring Boot application and request `/`; inspect the rendered page title and tabs.      |
| AC03: candidate placeholder            | Inspect the Candidates panel and confirm it contains no candidate records or details.             |
| AC04–AC05: empty and populated offers  | Exercise `GET /api/job-offers` with an empty and populated collection and inspect the list state. |
| AC06–AC07: selection and initial state | Load with offers and confirm details start empty, then select a row and inspect all five fields.  |
| AC08: retrieval failure                | Simulate an unavailable API/database and confirm the frontend shows a distinct failure state.     |
| AC09: safe text rendering              | Return markup-like field content and confirm it appears as literal text.                          |
| AC10: read-only scope                  | Inspect API routes and UI controls; only offer retrieval is exposed by this feature.              |

## 8. Design Boundaries

- No candidate API or candidate persistence behavior is introduced.
- No create, update, or delete operation is added for candidates or job offers.
- No search, matching, ranking, recommendation, authentication, or authorization behavior is
  introduced.
- The implementation must remain consistent with `01-requirements.md`; any behavior change first
  requires updating that specification.
