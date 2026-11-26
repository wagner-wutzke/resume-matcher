# Review Report: Job-Offer Management

- **Reviewed:** 2026-11-26
- **References:** `01-requirements.md`, `02-design.md`, `03-tasks.md`
- **Review scope:** Requirements added after the initial job-offer management review

## Spec Compliance

The implementation now carries Company through MongoDB, create/update requests and responses,
validation, form create/edit flows, and the Details panel. The existing MongoDB `jobTitle` field
maps to the specified API/form `jobName`, preserving existing records. Job Description and
Required Qualifications render as Markdown with raw HTML disabled and sanitized output. Job-offer
buttons share one style with the specified font size and height.

## Acceptance-Criteria Coverage

| Criteria | Verification |
|----------|--------------|
| AC01–AC14 | Existing controller, service, application, and frontend tests cover selection, CRUD flows, validation, failure handling, and discard behavior. |
| AC15 | Mapping, DTO serialization, validator, controller, and frontend tests cover Company persistence, required validation, form values, and display order. |
| AC16 | Frontend test verifies both Markdown fields pass through the renderer with raw HTML disabled and sanitized markup inserted; Company remains text. |
| AC17 | Frontend style test verifies shared button classes, `0.75rem` font size, `35px` height, and selected-list styling. |

## Test Runs

- `mvn test -q` — passed.
- `node src/test/js/app.test.mjs` — 22 tests passed.
- `git diff --check` — passed.

## Identified Gaps

No gaps found against AC01–AC17.

## Improvement Suggestion

The Markdown renderer and sanitizer are pinned CDN assets. Consider serving them locally if the
deployment environment cannot rely on CDN access; the current fallback safely displays Markdown
source as text if either library fails to load.

## Review Outcome

**APPROVED**
