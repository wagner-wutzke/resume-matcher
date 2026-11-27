# Review Report: Show Candidates

- **Reviewed:** 2026-11-27
- **References:** `01-requirements.md`, `02-design.md`, `03-tasks.md`
- **Review scope:** Candidate read API, MongoDB queries, Candidates tab, privacy, error handling,
  and accessibility

## Spec Compliance

The implementation provides read-only list and detail endpoints, bounded MongoDB summary reads,
ID-based candidate lookup, safe response DTOs, and the specified candidate-list behavior. Candidate
values are rendered as text. Retrieval errors use generic response messages and fixed log
categories. The code inspection found no candidate mutation, Qdrant read, or change to Open
Positions behavior.

## Acceptance-Criteria Coverage

| Criterion | Evidence                                                                                                                                                                                                |
|-----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| AC01      | Repository test verifies the summary projection, fullname/ID sort keys, page limit, and collation presence. Controller/UI tests cover first-page response and summary rendering.                        |
| AC02      | Repository and service tests cover page size, offset, page count, and range validation. UI tests cover navigation and disabled boundaries.                                                              |
| AC03–AC04 | UI tests cover ID-based detail loading, selection state, replacement selection, and stale response suppression.                                                                                         |
| AC05      | Service/UI tests cover empty results, zero pages, hidden pagination, and no candidate details.                                                                                                          |
| AC06      | UI tests cover missing-value placeholders and the empty-experience message.                                                                                                                             |
| AC07      | UI test forces candidate-list failure, verifies the safe retry state, then selects and displays an Open Position successfully.                                                                              |
| AC08      | Controller/UI tests cover sanitized detail failures, retry, and stale-response suppression.                                                                                                             |
| AC09      | Controller/UI tests cover `404`, selection clearing, and current-page refresh.                                                                                                                          |
| AC10      | DTO/controller tests cover response privacy; a log-capture test exercises list and detail storage failures and asserts canary name, email, and skills values are absent from captured logs.                |
| AC11      | UI/markup tests cover native buttons, list semantics, selected state, live status regions, and pagination controls.                                                                                     |

## Test Runs

- `mvn -q test` — 160 backend tests passed, with no failures or errors.
- `node --test src/test/js/app.test.mjs` — 37 UI tests passed.
- `mvn -q spotless:check` — passed.

## Identified Gaps

No unresolved gaps found against AC01–AC11 or the feature's privacy and scope boundaries.

## Improvement Suggestions

No required changes. The candidate read endpoints remain read-only, and the UI failure states remain
isolated from Open Positions.

## Review Outcome

**APPROVED**
