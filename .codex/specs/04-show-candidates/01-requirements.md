# Feature Specification: 04 — Show Candidates

- **Status:** [x] Draft | [ ] Approved for Design | [ ] In Implementation | [ ] Completed
- **Author/Agent:** @architect
- **Date:** 2026-11-27

## 1. Objective

Show ingested candidates in the Candidates tab, allow users to page through the candidate list,
and display the details of a selected candidate.

## 2. Context

Feature 03 ingests candidate resumes into the MongoDB `candidates` collection. The application
already has a Candidates tab with a list panel on the left and a details panel on the right, but
neither panel currently displays candidate data. This feature connects those panels to persisted
candidate records.

Candidate records contain personally identifiable information. The UI must show only the fields
defined by this feature and must not expose resume source text, LLM prompts, or embedding payloads.

## 3. Feature Scope

### In Scope

- Retrieve candidate records from the application persistence layer.
- Display candidate summaries in the Candidates tab.
- Paginate candidate summaries, with no more than 15 candidates per page.
- Display the selected candidate's structured details and work experiences.
- Represent loading, empty, and retrieval-failure states in the Candidates tab.

### Out of Scope

- Candidate creation, editing, deletion, resume upload, or ingestion controls.
- Candidate search, filtering, sorting controls, matching, ranking, or recommendations.
- Displaying original resume files, extracted resume text, LLM prompts/responses, or embeddings.
- Authentication or authorization changes.

## 4. Business Rules

- **BR01 — Data source:** Display candidates persisted in MongoDB by Feature 03. Do not retrieve
  candidate profile data from Qdrant.
- **BR02 — Page size:** A page contains at most 15 candidates. The first page is shown initially.
  Page navigation cannot move before the first page or past the last available page.
- **BR03 — Candidate identity:** Identify and select a candidate by its persisted candidate ID;
  email is not used as a UI key.
- **BR04 — Candidate summary:** Each list entry displays the candidate's full name, position,
  location, and derived years of experience. Missing values display as an em dash (`—`).
- **BR05 — Candidate details:** The detail panel displays email, full name, country, city,
  position, skills, allocation, derived years of experience, and all reported work experiences.
  Each experience displays title, company, period, responsibilities, and achievements. Missing
  scalar values display as an em dash; an empty experience list is identified as having no reported
  experience.
- **BR06 — Read-only display:** Candidate data in this feature is read-only. Selecting, paging, or
  viewing details does not modify candidate records.
- **BR07 — Ordering:** Candidates are displayed by full name in case-insensitive ascending order;
  candidate ID provides a deterministic tie-breaker. This keeps page membership stable for an
  unchanged candidate set.

## 5. Functional Requirements

### FR01 — Load the candidate list

When the Candidates tab is opened, load the first page of candidates from the application
candidate-list service. The service reads persisted candidate records and returns at most 15
records for the requested page, together with the current page number and total number of pages.
The page uses the ordering defined by BR07.

### FR02 — Display candidate summaries

Display each candidate returned for the current page as a selectable list entry. Show the values
defined by BR04. Selecting an entry marks it as selected and requests the candidate details by its
persisted ID.

### FR03 — Navigate candidate pages

Provide previous-page and next-page controls and show the current page and total page count. Disable
previous-page on the first page and next-page on the last page. Changing pages replaces the visible
list with that page's candidates and clears the selection and detail panel if the selected candidate
is no longer on the displayed page.

### FR04 — Display selected candidate details

Show the selected candidate's details in the right-hand details panel according to BR05. Details
must correspond to the selected candidate ID. Selecting another entry replaces the details with the
new selection.

### FR05 — Empty candidate list

If the candidate service returns no records, display a clear empty state in the list panel, show no
pagination controls, and show no candidate details.

### FR06 — Loading and retrieval errors

Show a loading state while requesting a candidate page or candidate details. If a request fails,
show a non-sensitive error message in the affected panel and provide a way to retry that request.
A failure to load candidates must not disable the Open Positions tab.

### FR07 — Invalid or missing selected candidate

If the selected candidate is no longer available when its details are requested, clear the
selection and details, then refresh the current candidate page. Do not display stale details for a
different candidate.

## 6. Non-Functional Requirements

- **NFR01 — Privacy:** Do not log candidate names, email addresses, skills, experience details, or
  other candidate values. Do not return resume text, LLM content, or Qdrant vectors in list or detail
  responses.
- **NFR02 — Bounded responses:** Candidate-list responses contain no more than 15 candidate
  summaries, regardless of the requested page size.
- **NFR03 — Accessibility:** Candidate entries and pagination controls are keyboard-operable. The
  selected entry and loading, empty, and error states are exposed to assistive technology.
- **NFR04 — Failure isolation:** Candidate retrieval failures do not prevent use of the Open
  Positions tab or unrelated application functionality.
- **NFR05 — Testability:** Candidate page retrieval, detail retrieval, pagination boundaries,
  selection behavior, and UI states are independently testable.

## 7. Acceptance Criteria

### AC01 — Initial candidate page

- **Given** persisted candidates exist
- **When** the Candidates tab is opened
- **Then** the first page is displayed with no more than 15 candidates.
- **And** summaries contain the fields defined by BR04.
- **And** candidates are ordered according to BR07.

### AC02 — Pagination

- **Given** more than 15 persisted candidates exist
- **When** the Candidates tab is opened
- **Then** the first page displays 15 candidates and indicates that additional pages are available.
- **When** the user moves to another page
- **Then** that page's candidates are displayed and the page indicator is updated.
- **And** navigation cannot move before the first page or past the last page.
- **And** each page contains no more than 15 candidates.

### AC03 — Candidate selection and details

- **Given** a candidate is visible in the current page
- **When** the user selects that candidate
- **Then** the selected entry is indicated and that candidate's details appear in the right-hand
  panel.
- **And** the details contain the fields and experiences defined by BR05.
- **And** the details are loaded for the selected candidate ID.

### AC04 — Change selection

- **Given** one candidate's details are displayed
- **When** the user selects another candidate
- **Then** the previous selection is cleared and the second candidate's details are displayed.

### AC05 — Empty list

- **Given** there are no persisted candidates
- **When** the Candidates tab is opened
- **Then** a clear empty state is displayed with no page navigation or candidate details.

### AC06 — Missing values and experiences

- **Given** a candidate has missing optional values or no reported experiences
- **When** the candidate details are displayed
- **Then** missing scalar values use the BR05 placeholder and the empty experience state is shown.

### AC07 — List retrieval failure

- **Given** the candidate-list service is unavailable
- **When** the Candidates tab requests candidates
- **Then** a non-sensitive error state and retry action are displayed.
- **And** the Open Positions tab remains usable.

### AC08 — Detail retrieval failure

- **Given** a candidate is selected
- **When** its details cannot be retrieved
- **Then** a non-sensitive detail error state and retry action are displayed.
- **And** details from a previously selected candidate are not shown as the selected candidate's
  details.

### AC09 — Candidate no longer exists

- **Given** a candidate is listed but is no longer available when selected
- **When** the detail request returns that the candidate is missing
- **Then** the selection and details are cleared and the current page is refreshed.

### AC10 — Read-only behavior and response privacy

- **Given** candidate list and detail requests are made
- **Then** no candidate data is modified.
- **And** the responses contain no resume text, LLM prompt/response, or embedding vector data.
- **And** application logs contain no candidate values.

### AC11 — Keyboard operation

- **Given** the Candidates tab has focus
- **When** the user navigates candidate entries and page controls using the keyboard
- **Then** candidates can be selected and pages changed without using a pointer.
- **And** the selected entry and loading, empty, and error states are available to assistive
  technology.

## 8. Error Cases

- Candidate storage is unavailable: return a controlled list or detail failure and show the
  appropriate retryable UI error. Do not expose database details or candidate values.
- The requested page is outside the available range: return a controlled response; the UI must not
  display an invalid page or stale candidate selection.
- A candidate ID is malformed or no longer exists: return a not-found response and clear the
  selected details as specified by FR07.
- A candidate detail request fails after another candidate was selected: do not associate the prior
  candidate's details with the new selection.
- A candidate contains missing optional data or an empty experience list: display the placeholders
  specified by BR05 without treating the record as a retrieval failure.

## 9. Assumptions for Design

- The existing Candidates tab split layout is authoritative: the list remains on the left and
  selected details appear on the right.
- The candidate-list service applies pagination before returning results. Its transport route and
  exact response DTOs will be defined in `02-design.md`.
- Candidate list ordering is case-insensitive full-name order with ID as a stable tie-breaker.
