# Feature 05 Review — Match Candidates to an Open Position

- **Reviewer:** @reviewer
- **Date:** 2026-11-27
- **Result:** Approved.

## Spec compliance

The implementation follows the main feature flow: it loads the persisted position, performs semantic
retrieval, resolves and deduplicates candidates from MongoDB, ranks chunks in Virtual Threads,
aggregates ordinal ranks, limits the response, and renders matching and detail states in the UI.
Matching is read-only. The match controller advice is scoped to `CandidateMatchController`.

The Qdrant adapter reads email metadata only. It does not read document text or scores, and the
resolver sorts normalized email keys before candidate resolution, so hit order does not determine
the final ranking. A null Qdrant response is treated as a matching failure rather than empty
success.

No resume source text, prompts, model responses, or vector values are returned or logged. Matching
logs contain fixed stage names, outcomes, and sanitized categories only. A worker failure causes the
ranker to cancel outstanding futures and fail the whole request without returning partial rankings.

### Resolution of previous finding

The architect aligned BR06, FR01, the approved design, and Task 1.1 on a pool-cap default of **20**.
`CandidateMatchingProperties`, `application.properties`, and
`CandidateMatchingPropertiesTest.shouldUseDefaultValuesWhenPropertiesAreNotSet` also use/verify 20.
The earlier discrepancy is resolved.

## Acceptance-criteria coverage

| AC | Evidence reviewed |
|---|---|
| AC01 | `CandidateMatchServiceTest`, `CandidateMatchControllerTest`; UI request test verifies the selected position path. |
| AC02 | `CandidateMatchServiceTest`, `CandidateMatchControllerTest`; UI test confirms no request without a selected position. |
| AC03 | `QdrantCandidateSemanticRetrieverTest`, `CandidatePoolResolverTest`; adapter test reverses hit order and verifies no score/text access. |
| AC04 | `CandidateMatchPromptBuilderTest` and `CandidateRankingResponseValidatorTest` cover evidence instructions and percentage bounds; DTO/controller tests cover API propagation; UI tests verify clickable `Candidate Name - Position - P% match` headings, safe text rendering, candidate-details navigation, and ordinal result order. |
| AC05 | `CandidateMatchingPropertiesTest`, `CandidateMatchRankerTest`, and `CandidateMatchAggregatorTest` cover chunking, Virtual Threads, joining, normalized ordering, candidate-ID ties, percentage independence, and truncation. |
| AC06 | `CandidateMatchingPropertiesTest` covers defaults, overrides, and invalid values. |
| AC07 | `CandidateMatchServiceTest` and UI empty-result test. |
| AC08 | UI tests cover loading, clearing results on position changes/new requests, and stale match response rejection. |
| AC09 | UI tests cover detail fetch/render, explanation and position association, detail failure/retry, stale detail rejection, and return without rerunning. |
| AC10 | Qdrant, resolver, validator, ranker, service, controller, and UI tests cover unavailable/invalid responses, worker failure, retry, and no partial results. |
| AC11 | DTO/controller tests, service read-only assertions, and `CandidateMatchLoggingTest` cover output and logging boundaries. |

Evidence limitation: explanation and percentage grounding are instructed in the prompt, while the
validator checks schema and numeric bounds rather than semantic factual support. This matches the
specified LLM-estimated approach; no additional grounding rule is required by the approved design.

## Validation

- `mvn -q verify`: 207 Java tests passed; no failures or errors.
- `node --test src/test/js/app.test.mjs`: 46 UI tests passed.
- `mvn -q spotless:check`: passed.
- JaCoCo line coverage for Feature 05 matching classes and matching controller/advice is at least
  90%; the lowest is `CandidateRankingResponseValidator` at 90.2% (`CandidateMatchRanker`: 91.5%).

## Match-percentage re-review (Task 6.5)

- The prompt requests an evidence-grounded, uncalibrated integer estimate from 0 through 100, says
  it is not a probability, and explicitly says it must not affect rank.
- The validator requires the percentage field, an integral JSON number convertible to `int`, and
  inclusive 0–100 bounds. Missing, fractional, negative, and above-100 values fail validation and
  are surfaced through the existing generic matching-unavailable path.
- The percentage is propagated through chunk rankings, global results, API DTO serialization, and
  controller response tests. It is not used in the aggregator comparator or limit selection.
- Aggregator tests use differing percentages to verify normalized-rank ordering, ID tie-breaking,
  and truncation remain unchanged. UI tests verify the exact label and preserve ordinal order even
  when the later-ranked candidate has a higher percentage.
- The UI renders the label through `textContent`; the API and matching logs do not expose raw LLM
  output or candidate/position values.
- Focused percentage tests, the 46-test UI suite, and formatting passed during review. Full-suite
  verification evidence is recorded above.

## Gaps and improvement suggestions

1. Confirm whether prompt-level evidence instructions are sufficient for AC04, or whether an
   additional validation requirement is intended. Do not add a validation rule without an
   architect-approved specification change.
2. The Qdrant query caps raw vector hits at `pool-cap`; multiple profile/experience hits for one
   candidate can therefore produce fewer unique candidates than that cap. This is consistent with
   the current bounded retrieval design, but the architect may clarify whether underfilling is an
   accepted retrieval tradeoff.

## Approval

The pool-cap default is aligned across the specification, design, task list, configuration, and
default-value test. The match-percentage revision satisfies its prompt, validation, API, display,
ordering-independence, and privacy requirements. The two improvement notes above are non-blocking
clarifications and do not require changes to the current implementation. Feature 05 is approved.
