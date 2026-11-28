# Feature Specification: 05 — Match Candidates to an Open Position

- **Status:** [ ] Draft | [ ] Approved for Design | [x] In Implementation | [ ] Completed
- **Author/Agent:** @architect
- **Date:** 2026-11-27

## 1. Objective

Allow a recruiter to request and review the best matching candidates for the selected open
position. Retrieve a relevant candidate pool through semantic retrieval, rank its members for the
position, and show the ranked results.

## 2. Context

Feature 02 manages open positions and allows one position to be selected at a time. Feature 03
stores structured candidate profiles in MongoDB and profile/experience embeddings in Qdrant.
Feature 04 displays candidate data but does not provide search or ranking. This feature uses the
selected position's job description, required qualifications, years of experience, location, and
allocation as matching context. Semantic retrieval from Qdrant assembles the candidate pool; the
configured LLM evaluates and ranks those candidates against the position. Candidate details shown
in results come from MongoDB.

Candidate records and match explanations are sensitive. Do not expose resume text, prompts, model
responses, or embedding vectors to the UI or application logs.

## 3. Feature Scope

### In Scope

- Request matches for the currently selected open position.
- Search candidate profile and experience embeddings in Qdrant using the selected position.
- Collapse multiple retrieved embeddings belonging to one candidate into one candidate result.
- Load structured candidate data from MongoDB and ask the configured LLM to rank the retrieved
  candidate pool for the position.
- Display up to the configured maximum number of ranked candidates, including a rank, an
  LLM-estimated match percentage, and a concise position-specific explanation.
- Open a dedicated match-details view for a ranked candidate, showing the candidate's structured
  details and position-specific explanation.
- Represent loading, empty-result, and matching-failure states in the application UI.

### Out of Scope

- Creating or editing positions or candidate records, and resume ingestion.
- Structured-field retrieval as a candidate-pool path, or using retrieval order as the final
  ranking.
- Automatic or background matching when a position or candidate changes.
- Candidate availability, compensation, work authorization, demographic, or other criteria not
  present in the position or candidate data.
- Saving, exporting, or sharing match results; feedback or human-edited rankings.
- Changing authentication or authorization behavior.

## 4. Business Rules

- **BR01 — Position input:** Match against the selected persisted position. Use its job name,
  company, job description, required qualifications, years of experience, location, and allocation.
  A request for a missing or deleted position fails as not found.
- **BR02 — Semantic candidate retrieval:** Use Qdrant only to retrieve candidates from semantic
  searches over candidate profile and experience embeddings. Qdrant must not calculate, return, or
  determine candidate rankings or similarity scores. Candidate identity and displayed candidate
  fields are loaded from MongoDB; vector payloads are not displayed as candidate records.
- **BR03 — Candidate deduplication:** A candidate may have profile and multiple experience vectors.
  Treat all hits with the same normalized email as one candidate and provide the candidate's
  structured profile and experiences to ranking at most once.
- **BR04 — Ranking:** Rank retrieved candidates from best fit to least fit using only the selected
  position requirements and structured candidate data. Prioritize evidence for required
  qualifications, followed by relevant experience; use the remaining position fields as additional
  fit context. Divide the candidate pool into chunks and rank every candidate in each chunk using a
  configurable chunk size, defaulting to 10 candidates per chunk, and use a separate Java Virtual
  Thread for each chunk. Join all worker results before combining the chunk rankings into one global
  ranking. Do not use Qdrant similarity scores or retrieval order to rank candidates. Normalize each
  worker's within-chunk ranks linearly by relative position, with first place equal to 1 and last
  place equal to 0; a one-candidate chunk receives 1. Sort all candidates by descending normalized
  within-chunk rank, using candidate ID as the deterministic tie-breaker. Do not make another LLM
  call to combine chunks. Assign a unique, contiguous, one-based ordinal rank to the resulting list.
  For every candidate, the LLM must also return an integer `matchPercentage` from 0 through 100,
  estimated from the supplied structured candidate and position evidence. This estimate is
  uncalibrated and is not a probability. It is display-only and must not affect within-chunk ranks,
  normalized cross-chunk ordering, candidate-ID tie-breaking, final ordinal ranks, or result limits.
- **BR05 — Evidence and missing data:** Do not infer or invent candidate qualifications. Explanations
  must concisely identify relevant supporting evidence and gaps against position requirements, and
  be supported by available candidate and position data. Identify a gap only when available data
  shows a requirement is unmet; missing information is unknown, not evidence that a candidate meets
  or fails a requirement. Estimate `matchPercentage` only from supplied evidence; do not treat
  missing information alone as evidence that a requirement is unmet.
- **BR06 — Result limits:** Return no more than the configured maximum number of displayed matches.
  The setting defaults to 10 and can be overridden in `application.properties`; it must be a
  positive integer. The candidate pool sent for ranking includes unique semantic retrieval hits up
  to a separately configured positive integer pool cap, which defaults to 20. Invalid configuration
  prevents application startup with a clear configuration error. If fewer candidates are retrieved,
  return only those available.
- **BR07 — Empty results:** A successful search with no eligible candidates returns an empty result
  list and is distinct from a matching failure.
- **BR08 — Fresh request:** Each explicit match request uses the currently selected position and
  performs retrieval and ranking; prior results must not be presented as results for a different
  position.
- **BR09 — Match details:** Selecting a ranked candidate opens a dedicated match-details view for
  that candidate and the position used for matching. Display the candidate's structured details and
  experiences defined by Feature 04, together with the match explanation, supporting evidence, and
  gaps.
- **BR10 — Privacy:** Do not log candidate values, position text, prompts, model responses, or
  vector data. Do not return resume source text, prompt/model content, or embedding vectors.

## 5. Functional Requirements

### FR01 — Configure result limit

Read the maximum number of displayed matches, maximum unique candidate pool size, and candidate
chunk size from `application.properties` settings. Their defaults are 10 displayed matches, 20
candidates in the ranking pool, and 10 candidates per chunk. All settings must be positive integers
and are validated at application startup. The exact property keys are defined in `02-design.md`.

### FR02 — Request position matches

When the recruiter requests matches for a selected position, submit that persisted position's ID to
the matching service. The service must load the current position data and reject a missing position
with a controlled not-found outcome. The UI must not issue a match request when no position is
selected.

### FR03 — Retrieve a semantic candidate pool

Build a semantic query from the position fields listed in BR01 and use Qdrant to retrieve candidate
profile and experience embedding hits. Do not request, consume, or use Qdrant similarity scores or
candidate ordering. Resolve hits to persisted MongoDB candidates and deduplicate by normalized email.
Send no more than the configured pool cap of unique retrieved candidates to ranking. Ignore vector
hits that cannot be resolved to a persisted MongoDB candidate; they must not be fabricated into
results.

### FR04 — Rank the candidate pool

Divide the retrieved candidate pool, up to the pool cap, into chunks no larger than the configured
chunk size and send each chunk with the selected position requirements and candidate structured
profile and experience data to the configured LLM. Process each chunk in its own Java Virtual
Thread. Request that each worker rank every candidate in its chunk from best to least fit,
prioritizing evidence for required qualifications followed by relevant experience, with other
position fields as additional context. For each candidate, request an integer `matchPercentage`
from 0 through 100 as an uncalibrated fit estimate based only on the supplied structured candidate
and position evidence. It is not a probability and is for display only; it must not affect rankings
or ordering. Require each worker's concise explanation to identify relevant supporting evidence and
gaps against the position requirements, without treating missing candidate information as evidence
of a gap. Join all workers before producing a final ranking and applying the display limit. Require
each response to identify candidates only by the stable candidate ID supplied in that chunk.
Validate that every response is parseable, includes every candidate in its chunk exactly once,
contains no out-of-chunk IDs or duplicate ranks, uses contiguous ranks starting at one within the
chunk, includes a `matchPercentage` integer in the inclusive range 0–100, and includes a non-empty
explanation for every candidate. Reject missing, malformed, non-integer, or out-of-range
percentages, other malformed or incomplete output, or any worker failure as a matching failure; do
not expose raw responses. Combine chunks using the BR04 normalized within-chunk-rank and
candidate-ID tie-break rules. Do not use `matchPercentage`, Qdrant scores, or retrieval ordering to
rank candidates, and do not make another LLM call to merge the chunk rankings. Assign contiguous
one-based final ranks before applying the display limit.

### FR05 — Bound and return results

Return at most the configured maximum number of ranked candidates. Each result includes candidate
ID, ordinal rank, the LLM's integer `matchPercentage` from 0 through 100, concise match explanation,
and the structured summary fields supported by the Candidate Summary defined in Feature 04. The
percentage is an uncalibrated evidence-based fit estimate, not a probability, and does not determine
result order. Return no resume text, prompts, raw model output, or vectors. The service must not
modify candidate or position records.

### FR06 — Show and refresh matches

Provide an explicit **Find Matches** action only when a position is selected. Place it in the
existing top job-offer action row immediately to the right of **Delete**, and style it with the exact
color `#198754`. Show a loading state while the request is running. On success, display results in
ascending rank order. Each result has a clickable heading in the form
`Candidate Name - Position - 85% match`; activating the heading opens that candidate's current
match-details view. Do not display list-number markers or ordinal ranks in the result heading. Show
the concise explanation below the heading. The percentage is display-only and does not alter result
order. A new request for another selected position replaces prior results and clears stale
selection/details. If there are no matches, show the empty state and no stale results.

### FR07 — Show match details

Selecting a ranked candidate opens a dedicated match-details view that shows the candidate's
structured details and work experiences defined by Feature 04, together with the match explanation,
supporting evidence, and gaps. The view must correspond to the selected candidate ID and the position
used for the current match results. Provide a way to return to the ranked results without rerunning
the match request.

### FR08 — Handle failures and retry

If position loading, Qdrant retrieval, MongoDB candidate resolution, or LLM ranking fails, show a
non-sensitive error state and a way to retry. Do not show partial or stale results as a successful
ranking. Matching failure must not prevent use of position management or the Candidates tab.

## 6. Non-Functional Requirements

- **NFR01 — Privacy:** Do not log candidate values, position contents, emails, resume text, prompts,
  model responses, or vectors. Return only the fields defined in FR05 and the concise explanation.
- **NFR02 — Bounded output:** Every successful response contains no more results than the configured
  maximum, regardless of the number of vector hits or LLM-returned entries. Each returned
  `matchPercentage` is an integer in the inclusive range 0–100.
- **NFR03 — Failure isolation:** A match failure does not disable position management, candidate
  viewing, or other application features.
- **NFR04 — Testability:** Configuration validation, retrieval/deduplication, candidate resolution,
  LLM response validation, match-percentage validation, virtual-thread chunk processing,
  cross-chunk ordering, result limiting, display-only percentage behavior, and UI states must be
  independently testable.
- **NFR05 — Traceable safe errors:** Matching failures may be logged with the stage and sanitized
  cause, but logs must not include candidate or position content or model input/output.

## 7. Acceptance Criteria

### AC01 — Request matches for selected position

- **Given** a persisted position is selected
- **When** the recruiter requests matches
- **Then** the system retrieves and ranks candidates using that position's current requirements.
- **And** the selected position's requirements, rather than another position's, are used.

### AC02 — No position selected or position missing

- **Given** no position is selected
- **When** the recruiter views the matching control
- **Then** the control is unavailable and no request is sent.
- **Given** a position is selected
- **When** the recruiter views the job-offer actions
- **Then** **Find Matches** is shown in the existing top action row immediately to the right of
  **Delete**.
- **And** the **Find Matches** action uses the exact color `#198754`.
- **Given** a requested position was deleted or cannot be found
- **When** a match request is processed
- **Then** a controlled not-found error is returned and no results are shown.

### AC03 — Semantic retrieval and deduplication

- **Given** Qdrant returns semantic hits for candidate profile or experience embeddings
- **When** the matching pool is prepared
- **Then** candidates are selected only from the semantic retrieval results.
- **And** multiple profile or experience hits for one candidate yield at most one candidate in the
  ranking pool.
- **And** candidates used for ranking are resolved from MongoDB.
- **And** Qdrant similarity scores and result ordering are not used to rank candidates.

### AC04 — Ranked matches

- **Given** eligible persisted candidates are retrieved and the ranking response is valid
- **When** the match request completes
- **Then** each result has a unique contiguous one-based rank, candidate summary, and concise
  evidence-based explanation.
- **And** results are displayed from rank 1 upward.
- **And** every result heading displays the candidate name, candidate position, and integer match
  percentage in the form `Candidate Name - Position - 85% match`, without an ordinal rank or list
  number marker.
- **And** activating the heading opens the existing match-details view for that candidate.
- **And** the percentage is grounded in the supplied structured candidate and position evidence,
  uncalibrated, and not a probability.
- **And** the percentage does not affect the ordinal rank or result order.
- **And** each explanation identifies relevant supporting evidence and gaps against the position
  requirements, and does not claim unsupported qualifications or treat unknown information as a gap.

### AC05 — Configured pool and display limits

- **Given** the display limit is `N` and the ranking pool cap is `P`
- **When** semantic retrieval produces more than `P` unique candidates
- **Then** no more than `P` candidates are sent for ranking.
- **And** the entire supplied pool is divided into chunks and each chunk is ranked in parallel by a
  separate Java Virtual Thread.
- **And** each chunk contains no more than the configured chunk size, which defaults to 10.
- **And** each worker ranks every candidate in its chunk and all worker results are joined before the
  display limit is applied.
- **And** joined chunk rankings are combined into one global order using normalized within-chunk
  ranks and candidate ID as a tie-breaker, without Qdrant scores or another LLM call.
- **And** match percentages do not affect within-chunk ranks, global ordering, final ordinal ranks,
  or which results pass the display limit.
- **And** no more than `N` ranked results are returned and displayed.
- **And** if fewer than `P` candidates are retrieved, all retrieved candidates are ranked, with at
  most `N` displayed.

### AC06 — Invalid limit configuration

- **Given** any result-limit, candidate-pool-cap, or chunk-size setting is zero, negative, or not an
  integer
- **When** the application starts
- **Then** startup fails with a clear configuration error.

### AC07 — No matches

- **Given** semantic retrieval resolves no eligible candidates
- **When** a match request completes successfully
- **Then** an empty state is displayed without an error or stale results.

### AC08 — Loading and changing positions

- **Given** a match request is running
- **Then** a loading state is displayed.
- **When** the selected position changes and a new request is made
- **Then** prior results and candidate details are cleared or replaced, and only results for the
  current position are displayed.

### AC09 — Match details

- **Given** ranked candidates are displayed
- **When** the recruiter selects a candidate
- **Then** a dedicated match-details view opens for that candidate and the position used for
  matching.
- **And** the view displays the candidate's Feature 04 structured details and experiences, plus the
  match explanation, supporting evidence, and gaps.
- **And** returning to results preserves the ranking without rerunning the match request.

### AC10 — Retrieval or ranking failure

- **Given** Qdrant retrieval, MongoDB candidate resolution, or LLM ranking fails, or the ranking
  response is invalid, including a missing, non-integer, or out-of-range `matchPercentage`
- **When** the match request completes
- **Then** a non-sensitive error and retry action are displayed.
- **And** partial or previous-position results are not presented as current successful results.
- **And** position management and candidate viewing remain usable.
- **And** if any chunk worker fails, no partial ranking is displayed.

### AC11 — Data and privacy boundaries

- **Given** a matching request succeeds or fails
- **Then** candidate and position records are unchanged.
- **And** results and logs contain no resume source text, prompts, raw model response, or vector
  values.
- **And** result explanations and summaries contain only the fields permitted by FR05.

## 8. Error Cases

- The selected position does not exist: return a controlled not-found error and clear match results.
- Qdrant semantic retrieval is unavailable or returns an unusable response: report a retryable
  matching failure; do not show an empty-success state.
- A Qdrant hit has no matching MongoDB candidate: skip that hit; if no candidate remains, return an
  empty-success result.
- MongoDB is unavailable while resolving candidates: report a retryable matching failure.
- The LLM is unavailable, returns malformed output, invents an ID, duplicates a candidate/rank, or
  supplies invalid ranks or a missing, non-integer, or out-of-range `matchPercentage`: report a
  retryable matching failure without exposing the response.
- Any Virtual Thread fails or returns an incomplete ranking: treat the whole matching request as
  failed; do not display results from only the successful chunks.
- No candidate vectors exist or no candidates are semantically retrieved: return an empty-success
  result.
- The display limit, candidate pool cap, or chunk size is invalid: fail application startup with a
  configuration error.

## 9. Assumptions for Design

- The existing Open Positions selection and Candidate Summary presentation in Features 02 and 04
  are authoritative. `02-design.md` defines the matching action placement, service/API route,
  response DTO, property key, semantic search strategy, and LLM integration details.
- “Rank” is the ordinal ordering. `matchPercentage` is an uncalibrated LLM fit estimate for display
  only, not a probability, and does not affect ranking or ordering.
- The display limit applies after the LLM ranks the semantic candidate pool. The separate pool cap
  bounds the amount of candidate data sent to the LLM.
