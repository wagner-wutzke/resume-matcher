# Task List: Match Candidates to an Open Position

- **Status:** [ ] Draft | [ ] Ready | [x] Active | [ ] Paused | [ ] Completed
- **Target Agent:** @engineer
- **Validation Agent:** @reviewer
- **References:** `01-requirements.md`, `02-design.md`

## Phase 1: Matching Configuration and Semantic Retrieval

- [x] **Task 1.1 — Add matching configuration and startup validation:** Bind
  `candidate.matching.max-results` (default `10`), `candidate.matching.pool-cap` (default `20`), and
  `candidate.matching.chunk-size` (default `10`). Reject non-integer, zero, and negative values at
  startup. Add focused tests for defaults, overrides, and invalid configuration (**FR01, AC06**).
- [x] **Task 1.2 — Define match response and error DTOs:** Add response models for the position ID and
  ordered match items, combining final rank, `CandidateSummaryResponse`, and explanation. Add stable,
  generic match error response models without candidate, position, prompt, vector, or provider details.
  Add mapping/serialization tests (**FR05, AC04, AC11**).
- [x] **Task 1.3 — Add the semantic retrieval abstraction and Qdrant adapter:** Query candidate profile
  and experience documents from the configured Spring AI `VectorStore`; use only email metadata for
  candidate resolution and do not consume hit ordering or similarity scores. Add focused adapter
  tests for the query, document metadata handling, and absence of score/order use (**BR02, FR03,
  AC03, AC10–AC11**).
- [x] **Task 1.4 — Resolve and deduplicate retrieved candidates:** Normalize retrieved email metadata,
  resolve persisted candidates through `CandidateRepository`, deduplicate profile/experience hits by
  normalized email, skip stale or unresolvable hits, and enforce the configured unique-candidate
  pool cap. Add tests for duplicate hits, missing metadata, stale hits, MongoDB failures, and pool
  boundaries (**BR02–BR03, FR03, AC03, AC05, AC07, AC10**).

## Phase 2: LLM Ranking and Cross-Chunk Ordering

- [x] **Task 2.1 — Build the per-chunk ranking prompt and response validator:** Provide the position
  requirements and structured candidate fields for one chunk; request complete, evidence-based
  ordinal rankings and explanations in the specified JSON shape. Validate candidate IDs, complete
  coverage, uniqueness, contiguous one-based ranks, and non-empty explanations. Add tests for valid,
  malformed, incomplete, duplicate, and out-of-chunk responses (**BR04–BR05, FR04, AC04, AC10**).
- [x] **Task 2.2 — Rank candidate chunks on Java Virtual Threads:** Partition candidates by the
  configured chunk size, execute one LLM ranking call per chunk on a separate Java Virtual Thread,
  join every worker, and fail the entire request on any worker failure without returning partial
  results. Add focused concurrency, completion, cancellation/failure, and no-partial-result tests
  (**BR04, FR04, NFR04, AC05, AC10**).
- [x] **Task 2.3 — Aggregate chunk ranks and apply the result limit:** Normalize each chunk's ordinal
  ranks linearly by relative position (first `1`, last `0`, one-candidate chunk `1`), sort by
  descending normalized rank and candidate ID ascending for ties, assign contiguous final ranks, and
  return no more than `max-results`. Add tests for normalization, varying chunk sizes, ties,
  deterministic ordering, and truncation (**BR04, BR06, FR04–FR05, AC04–AC05**).

## Phase 3: Match API and Failure Handling

- [x] **Task 3.1 — Implement the match orchestration service:** Load the current persisted position;
  build the semantic query from all BR01 fields; coordinate retrieval, MongoDB resolution, chunk
  ranking, global ordering, and result creation; return an empty result without invoking the LLM
  when the resolved pool is empty. Add service tests for each stage and read-only behavior (**BR01–
  BR08, FR02–FR05, AC01–AC08, AC11**).
- [x] **Task 3.2 — Expose the position-match endpoint:** Add
  `POST /api/job-offers/{positionId}/matches` with the response shape and status codes in
  `02-design.md`. Add a match-scoped exception handler for missing positions and safe matching
  failures, leaving existing job-offer and candidate routes unchanged. Add MockMvc tests for success,
  empty results, not-found, and unavailable dependencies (**FR02, FR05, FR08, AC01–AC02, AC07,
  AC10–AC11**).
- [x] **Task 3.3 — Add safe matching-stage logs:** Log stage, outcome, and sanitized failure category
  only. Exclude candidate/position identifiers and values, emails, prompts, model responses, Qdrant
  documents, and vector data. Add log-capture tests for success and failure paths (**BR10, NFR01,
  NFR05, AC10–AC11**).

## Phase 4: Open Positions Matching UI

- [x] **Task 4.1 — Add accessible matching controls and result states:** Extend the Open Positions
  panel with a selected-position-only **Find Matches** action, loading/status/error/empty regions,
  retry action, result list, and match-details view/back controls. Preserve current position and
  Candidates tab behavior. Add focused markup/accessibility verification (**FR06–FR08, NFR03,
  AC02, AC07–AC10**).
- [x] **Task 4.2 — Request and render ranked matches:** Call the matching endpoint for the selected
  position, prevent duplicate in-flight submissions, render results in ranking order with a
  candidate-name/position/percentage heading that opens candidate details and an explanation below,
  keep the titled result list hidden until the request completes successfully, use the Job
  Description details typography, title the list **Matching Candidates**, collapse Job Offer
  Details after successful results load with an accessible expand/collapse control, clear
  stale results on position changes or new requests, and handle empty and retryable error states.
  Guard against stale asynchronous responses. Add UI behavior tests (**BR08, FR06, FR08, AC01–AC02,
  AC04, AC07–AC08, AC10–AC12**).
- [x] **Task 4.3 — Show candidate match details and return to results:** Fetch details from the
  existing `/api/candidates/{id}` endpoint, associate the returned details with the selected match
  explanation and position, render structured fields and experiences, and return to the preserved
  result list without rerunning the match request. Add tests for successful detail rendering, missing
  candidate/detail failure, stale response protection, and back navigation (**BR09, FR07, AC09,
  AC11**).

## Phase 5: Acceptance Verification and Review

- [x] **Task 5.1 — Verify acceptance-criteria coverage:** Map AC01–AC11 to backend and UI tests, run
  the relevant suites, resolve failures, and verify the repository coverage and formatting rules
  (**all ACs, NFR02–NFR04**).
- [x] **Task 5.2 — Review privacy, failure isolation, and feature boundaries:** Confirm the match
  response contains only the specified candidate summary, explanation, and display-only percentage;
  no candidate or position values reach logs; no Qdrant scores/order affect ranking; failed workers
  return no partial results;
  and no persistent matching, structured retrieval, or unrelated candidate/job-offer behavior was
  added (**BR01–BR10, NFR01–NFR05, AC03, AC05, AC10–AC11**).
- [x] **Task 5.3 — Reviewer report and status:** Record spec compliance, acceptance-criteria
  evidence, gaps, and improvement suggestions in `04-review.md`. Update task checkboxes only after
  reviewer verification; approve completion only when no unresolved gaps remain (**all ACs**).

## Phase 6: Display-Only Match Percentage

- [x] **Task 6.1 — Request and validate LLM match percentages:** Extend the per-candidate ranking
  response schema and prompt with `matchPercentage`, an evidence-grounded, uncalibrated integer
  estimate from 0 through 100. Validate presence, integer type, and inclusive bounds; reject invalid
  values as matching failures. Add prompt and validator tests for valid, missing, non-integer,
  negative, and above-100 values (**BR04–BR05, FR04, AC04, AC10**).
- [x] **Task 6.2 — Return match percentages in the API:** Add `matchPercentage` to each API match item
  and update DTO mapping/serialization and controller tests. Keep the existing candidate summary and
  explanation fields and do not expose raw model output (**FR05, AC04, AC11**).
- [x] **Task 6.3 — Display the percentage in the result heading:** Render each clickable result
  heading as `Candidate Name - Position - P% match`, without a visible ordinal rank or list-number
  marker. Preserve result order regardless of percentage values, and open the current candidate
  details view from the heading. Add UI tests for the exact heading, safe text rendering, navigation,
  and unchanged order (**FR06, AC04–AC05, AC11**).
- [x] **Task 6.4 — Re-verify percentage and ranking boundaries:** Test that match percentages do not
  affect within-chunk ranking, normalized cross-chunk ordering, candidate-ID ties, final ranks, or
  display-limit selection. Re-run relevant backend/UI suites, coverage checks, and formatting; update
  the AC coverage evidence (**AC04–AC05, AC10–AC11, NFR02, NFR04**).
- [x] **Task 6.5 — Reviewer re-approval:** Review the percentage prompt, validator, API/UI contract,
  ordering independence, privacy boundaries, and updated tests. Update `04-review.md` and task status
  only after independent verification, and approve only if no unresolved gaps remain (**all ACs**).

## Phase 7: Find Matches Action Placement and Styling

- [ ] **Task 7.1 — Place and style the selected-position matching action:** Move **Find Matches** to
  the existing top job-offer action row immediately to the right of **Delete**. Show it only while a
  position is selected and style it with the exact color `#198754`. Add focused markup and UI tests
  for action order, selected/no-selection visibility, exact color, and preservation of the existing
  job-offer actions (**FR06, AC02**).
- [ ] **Task 7.2 — Present matching results with details collapse:** Add the **Matching Candidates**
  title and result list hidden before a request and while it is pending; reveal them only after a
  successful response. Match the Job Description details typography and provide an accessible
  control that collapses Job Offer Details after successful matching and expands it on request. Keep
  the result region hidden after failures and keep the details panel expanded during loading and
  failures. Add focused UI tests (**FR06, AC12**).
