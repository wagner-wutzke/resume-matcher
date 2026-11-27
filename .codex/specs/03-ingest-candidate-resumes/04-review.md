# Review Report: Candidate Resume Ingestion

- **Reviewer:** @reviewer
- **Date:** 2026-11-27
- **Outcome:** CHANGES REQUIRED — architect document updates pending
- **References:** `01-requirements.md`, `02-design.md`, `03-tasks.md`

## Spec compliance

The implementation covers the main ingestion flow: input scanning and watching, PDF extraction,
cleanup, LLM response validation, MongoDB persistence, Qdrant embedding replacement, file movement,
and compensating rollback. The implementation remains within the feature scope. I found the gaps
below, so the feature is not approved yet.

## Acceptance criteria coverage

The full Maven verification completed successfully: **134 tests, 0 failures, 0 errors, 0 skipped**.
Focused evidence is in the ingestion test classes listed in the design verification mapping:

- **AC01–AC03, AC18:** `ResumeDirectoryWatcherTest`, `ResumeIngestionRunnerTest`, and queue coverage
  exercise startup/live discovery, direct-child PDF filtering, duplicate suppression, and no error
  folder retry.
- **AC04–AC07, AC15, AC23:** `ResumePdfTextExtractorTest`, `ResumeTextCleanerTest`,
  `CandidateDataExtractorTest`, `CandidateDataValidatorTest`, and `CandidateResumePipelineTest` cover
  extraction, cleanup, invalid input rejection, and failure before persistence.
- **AC06:** `ExperienceDurationCalculatorTest` covers year-only ranges with dash, `to`, and
  `until` separators, ongoing dates, month precision, overlap, and unusable periods. The focused
  calculator, stage-logger, and runner tests pass after the parser change.
- **AC08–AC11, AC16–AC17, AC24:** candidate repository/model, Qdrant repository, document builder,
  and processor tests cover structured persistence, replacement, metadata, snapshots, and rollback.
- **AC12–AC14, AC22:** `ResumeFileMoverTest` and processor tests cover result movement, overwrite,
  movement failure, and error routing.
- **AC19–AC21:** `ResumeFileReadinessGateTest` and `ResumeIngestionPropertiesTest` cover readiness,
  timeout, and required configuration.
- **AC25:** `ResumeIngestionStageLoggerTest`, `ResumeIngestionQueueTest`,
  `CandidateResumePipelineTest`, and `CandidateResumeProcessorTest` cover stage lifecycle,
  discovery/queue events, skipped and failed outcomes, filename/stage fields, elapsed time, fixed
  sanitized causes, severity, and absence of candidate data in captured messages.

JaCoCo line coverage for the added ingestion classes and candidate model/repository classes is at
least 90% in the generated report. The focused Spotless check passes for ingestion sources and
tests. The repository-wide `mvn spotless:check` still reports formatting violations in 20
job-offer files outside this feature; this remains non-blocking per the user's decision.

## Identified gaps

1. **Structured output design decision — design §4 / Task 2.3. RESOLVED BY PRODUCT DECISION.**
   Keep raw JSON plus strict validation. The design document should be updated by the architect to
   approve this approach; no structured-output converter is required.

2. **Conflicting experience example — FR05 / BR05 / AC06.**
   FR05 says `2016–Present` totals 12 years when processed in 2026, while BR05 and AC06 specify
   end year minus start year, which gives 10 years. The implementation and its test follow the
   explicit BR05/AC06 calculation. Decision: BR05/AC06 are authoritative, so the example must be
   corrected to 10 years. The architect should update FR05 to match.

3. **Formatter gate — Task 6.1. NON-BLOCKING BY USER DECISION.**
   Repository-wide Spotless violations are in 20 job-offer files outside this feature. The focused
   ingestion source/test check passes. Track the unrelated violations in their owning scope.

## Readability and complexity

The ingestion responsibilities are split across focused components and the tests use injectable
boundaries for most external operations. The processor's compensation and stage orchestration are
the most complex areas, but the flow is traceable. No out-of-scope UI, API, OCR, retry, or matching
behavior was found.

## Improvement suggestions

- The year-only separator change is implemented and verified; no further parser change is needed.
- Update FR05's example to match the authoritative 10-year BR05/AC06 calculation.
- Task 5.4 is implemented and verified; no additional ingestion logging change is required.
- Update the design to approve raw JSON plus strict validation.
- Track the unrelated job-offer formatter violations in their owning scope; they do not block this
  feature.

## Review outcome

**CHANGES REQUIRED.** Tasks 2.5 and 5.4 are verified complete. The architect must update FR05's
example and the design to reflect the accepted decisions before the feature can receive final
approval. The unrelated job-offer formatter failures are not blocking.
