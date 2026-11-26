# Technical Design: Resume Matcher SPA Skeleton

- **Status:** [ ] Draft | [x] Approved for Implementation
- **Author/Agent:** @architect
- **Reference Spec:** `01-requirements.md`

## 1. Architecture

Use a Spring Boot web application to serve a static single-page frontend and a read-only API. The
frontend uses HTML, CSS, Bootstrap, and vanilla JavaScript. Spring MVC serves files from
`src/main/resources/static` and handles `/api` routes. Spring Data MongoDB reads existing job-offer
records from MongoDB.

Keep API code within the existing `net.wowdev.ai.resume.matcher` package, organized into cohesive
job-offer model, repository, and controller components. A separate service layer is optional and
should be added only if it provides behavior beyond the repository call.

The SPA entry point is `src/main/resources/static/index.html`, with CSS and JavaScript in adjacent
static assets. Serve the application at `http://localhost:8080`.

Configure MongoDB using individual properties, never `spring.mongodb.uri`:

```properties
spring.mongodb.host=${MONGODB_HOST:localhost}
spring.mongodb.port=${MONGODB_PORT:27017}
spring.mongodb.database=${MONGODB_DATABASE:resume_matcher}
spring.mongodb.username=${MONGODB_USERNAME:}
spring.mongodb.password=${MONGODB_PASSWORD:}
spring.mongodb.authentication-database=${MONGODB_AUTHENTICATION_DATABASE:admin}
```

Use the local host, port, database, and authentication-database defaults shown above. Supply
credentials through environment variables when authentication is configured; do not store
credentials in source-controlled configuration. MongoDB configuration tests verify the individual
properties and confirm `spring.mongodb.uri` is absent.

## 2. MongoDB Document and API Representation

Read existing records from the `job_offers` collection. Map MongoDB `_id` to the API's `id`. The
records used by this feature contain:

```json
{
  "_id": "MongoDB ObjectId",
  "jobId": "BE-00100010",
  "company": "Example Company",
  "jobTitle": "Backend Engineer",
  "jobDescription": "Job description text",
  "requiredQualifications": "Required experience and skills",
  "yearsOfExperience": 7,
  "location": "Dublin",
  "allocation": "Remote"
}
```

The read API exposes `id`, `jobId`, `company`, `jobTitle`, `jobDescription`,
`requiredQualifications`, `yearsOfExperience`, `location`, and `allocation`. `yearsOfExperience`
is numeric in the API response; the other listed values are strings. The UI displays the fields
required by the requirements: Company, Job Title, Job Description, Required Qualifications, Years
of Experience, Location, and Allocation. `jobId` is retained in the API representation but is not
displayed by this feature.

Do not add writes, seed data, schema migrations, or collection lifecycle behavior.

## 3. API Contract

### `GET /api/job-offers`

Return all records in the `job_offers` collection. The request has no body or query parameters.

**Success — `200 OK`**

```json
[
  {
    "id": "6650c4c93f1b8f0a6f8a1234",
    "jobId": "BE-00100010",
    "company": "Example Company",
    "jobTitle": "Backend Engineer",
    "jobDescription": "Job description text",
    "requiredQualifications": "Required experience and skills",
    "yearsOfExperience": 7,
    "location": "Dublin",
    "allocation": "Remote"
  }
]
```

An empty collection returns `200 OK` with `[]`.

**Retrieval failure — `503 Service Unavailable`**

```json
{
  "code": "JOB_OFFERS_UNAVAILABLE",
  "message": "Job offers could not be loaded."
}
```

Do not translate a database or retrieval failure into a successful empty result.

## 4. Request and Rendering Flow

1. The browser loads the SPA and its static assets from the Spring Boot application.
2. On application load, the frontend sends one `GET /api/job-offers` request.
3. The API reads the collection and returns the records using the contract above.
4. On success, the frontend displays each offer's `jobTitle` in the Open Positions list.
5. The Details panel starts empty. Selecting an offer displays Company, Job Title, Job Description,
   Required Qualifications, Years of Experience, Location, and Allocation.
6. The frontend inserts returned values as text nodes or text properties; it does not interpret
   field values as HTML.
7. A successful empty response leaves the list and Details panel empty. A failed request displays
   a distinct retrieval failure state and does not imply that the collection is empty.

The Candidates tab is a placeholder. It provides left and right content regions and does not load
or display candidate data.

## 5. SPA Layout and Visual Direction

- Display **Resume Matcher** as the application name and **Candidates Matching made easy** as the
  visible title.
- Provide **Candidates** and **Open Positions** tabs.
- Use a two-panel layout in each tab, with a list area on the left and a details area on the right.
- Keep candidate list and details content empty in this phase.
- Use a simple dark gray and royal blue palette and let the application occupy the available page
  space.
- Keep the layout clear at supported viewport sizes. Exact colors, typography, responsive
  breakpoints, and empty/failure copy remain presentation choices.

## 6. Error Handling and Data Safety

- An empty collection is a successful response and produces an empty Open Positions list.
- A MongoDB or API retrieval failure is reported as an unsuccessful request and shown as a distinct
  failure state.
- A failure must not be represented as an empty result.
- Render all returned job-offer values as text without HTML interpretation.
- This feature performs no writes to the `job_offers` collection.

## 7. Verification Mapping

| Requirement                                      | Design verification                                                                                                                                 |
|--------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| AC01–AC02: application availability and identity | Start the Spring Boot application, request `/`, and inspect the title and tabs.                                                                     |
| AC03: candidate placeholder                      | Inspect the Candidates tab and confirm both content areas contain no candidate data.                                                                |
| AC04: retrieval and list                         | Return populated records from `GET /api/job-offers` and confirm each `jobTitle` appears in the list.                                                |
| AC05: empty result                               | Return `200 OK` with `[]` and confirm the list and Details panel are empty.                                                                         |
| AC06–AC08: selection and details                 | Load populated records, confirm no initial selection, then select offers and verify all seven required detail fields and single-selection behavior. |
| AC09: retrieval failure                          | Simulate an unavailable API/database and confirm a distinct failure state.                                                                          |
| AC10: safe text rendering                        | Return markup-like field content and confirm it appears literally without HTML interpretation.                                                      |
| AC11: read-only scope                            | Inspect API routes and UI controls; confirm this feature exposes only offer retrieval and no write operations.                                      |
| AC12: individual MongoDB properties              | Verify separate host, port, database, and optional credential properties are used and `spring.mongodb.uri` is absent.                              |

## 8. Design Boundaries

- Do not add candidate API or candidate persistence behavior.
- Do not add create, update, or delete operations for candidates or job offers.
- Do not add search, matching, ranking, recommendation, authentication, or authorization behavior.
- Keep implementation consistent with `01-requirements.md`; update that specification before
  changing behavior.
