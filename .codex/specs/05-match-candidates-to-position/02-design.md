# Technical Design: Match Candidates to an Open Position

- **Status:** [ ] Draft | [x] Approved for Implementation
- **Author/Agent:** @architect
- **Reference Spec:** `01-requirements.md`

## 1. Architecture

Extend the existing Spring MVC application and static SPA. A match request loads the selected
persisted job offer, searches candidate profile and experience embeddings through the configured
Qdrant-backed Spring AI `VectorStore`, resolves retrieved candidate emails to MongoDB records, and
uses the configured `ChatModel` to rank bounded candidate chunks in parallel. Matching is
request-scoped; it does not persist results or modify candidates, job offers, or embeddings.

Qdrant is used as a semantic retriever only. Do not request or consume similarity scores, and do not
use the returned hit order to determine the final candidate ranking. The LLM's within-chunk ordinal
rankings determine matching relevance; the deterministic cross-chunk rule is defined in Section 5.

Keep new types under `net.wowdev.ai.resume.matcher` and follow the current controller, service,
repository, DTO, and scoped exception-handler structure.

| Component                          | Responsibility                                                                                                                                                                                                      |
|------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `CandidateMatchController`         | Accept a request for matches for one persisted job offer.                                                                                                                                                           |
| `CandidateMatchService`            | Load the offer, coordinate retrieval, candidate resolution, chunk ranking, aggregation, and response creation.                                                                                                      |
| `CandidateSemanticRetriever`       | Abstract semantic candidate-vector retrieval from the Qdrant adapter.                                                                                                                                               |
| `QdrantCandidateSemanticRetriever` | Query the configured Qdrant `VectorStore` for candidate profile and experience documents. Read only the normalized email metadata needed for candidate resolution; do not use similarity scores or result ordering. |
| `CandidateMatchRanker`             | Rank one chunk with the configured `ChatModel` and validate the structured response.                                                                                                                                |
| `CandidateRepository`              | Resolve retrieved candidate emails and load candidate records by ID for details.                                                                                                                                    |
| `JobOfferRepository`               | Load the current selected position by its persisted ID.                                                                                                                                                             |
| Match DTOs                         | Return position ID, ordered candidate summaries, ordinal rank, and explanation without vector or prompt data.                                                                                                       |
| Match UI state                     | Show action, loading/error/empty/result states, and a dedicated candidate detail view while preserving the current result list.                                                                                     |

Keep the matching retriever separate from ingestion's `CandidateVectorRepository`: the latter
manages vector replacement and rollback by email and must not be expanded into a matching or
ranking service.

## 2. Configuration

Add these settings to `application.properties`:

| Property                         | Default | Meaning                                                |
|----------------------------------|--------:|--------------------------------------------------------|
| `candidate.matching.max-results` |    `10` | Maximum ranked candidates returned and shown.          |
| `candidate.matching.pool-cap`    |    `20` | Maximum unique MongoDB candidates supplied to ranking. |
| `candidate.matching.chunk-size`  |    `10` | Maximum candidates sent to one LLM ranking call.       |

Bind these values to one matching configuration object and validate all as positive integers during
application startup. A missing property uses the stated default. Invalid values prevent startup and
identify the invalid setting without exposing credentials or candidate data. Do not require
`pool-cap` to be greater than or equal to `max-results`; if it is lower, the service can return at
most the number of candidates in the pool.

Use the existing `spring.ai.openai.chat.model` and `spring.ai.vectorstore.qdrant.*` configuration.
Do not introduce a provider-specific model setting, credentials, or a second vector store.

## 3. API Contract

### `POST /api/job-offers/{positionId}/matches`

The request body is empty. `positionId` is the MongoDB ID of the selected job offer. Matching is a
computed, non-persistent operation, so the UI starts it with an explicit `POST` request.

On success, return `200 OK` with the position ID and ranked match items:

```json
{
  "positionId": "position-id",
  "items": [
    {
      "rank": 1,
      "matchPercentage": 85,
      "candidate": {
        "id": "candidate-id",
        "fullname": "Jane Doe",
        "position": "Software Engineer",
        "country": "Brazil",
        "city": "São Paulo",
        "yearsOfExperience": "5 years and 0 months"
      },
      "explanation": "Evidence: ... Gaps: ..."
    }
  ]
}
```

`candidate` uses the existing `CandidateSummaryResponse` fields from Feature 04. Each
`matchPercentage` is an integer from 0 through 100 estimated by the LLM from the supplied structured
position and candidate evidence. It is uncalibrated, is not a probability, and is display-only. The
`items` array is already sorted by final ordinal rank, which is computed without using the
percentage. An empty successful search returns `200 OK` with `items: []`. The response excludes
email, resume text, vector data, Qdrant scores, LLM prompts, and raw model output.

### Error responses

Use a match-specific controller advice so existing job-offer and candidate error contracts remain
unchanged. Error responses use the established `{ "code": "...", "message": "..." }` shape.

| Condition                                                            |  HTTP | Stable code                      |
|----------------------------------------------------------------------|------:|----------------------------------|
| Requested position does not exist                                    | `404` | `JOB_OFFER_NOT_FOUND`            |
| Qdrant, MongoDB resolution, or LLM ranking is unavailable or invalid | `503` | `CANDIDATE_MATCHING_UNAVAILABLE` |

Messages are generic and contain no position text, candidate values, provider response, or internal
exception details. A missing position is not reported as a matching-service outage.

### Candidate details

Use the existing `GET /api/candidates/{id}` endpoint when a recruiter opens a match detail. It
returns `CandidateDetailsResponse` as specified in Feature 04. The frontend keeps the selected
match's explanation alongside the selected candidate ID and displays both together. Do not add a
second candidate-detail endpoint or copy candidate details into the match result response.

## 4. Retrieval and Candidate Resolution

1. Load the current `JobOffer` from `JobOfferRepository` using the requested position ID. If absent,
   fail before contacting Qdrant or the LLM.
2. Build one semantic query from job title, company, job description, required qualifications,
   years of experience, location, and allocation, as listed in BR01.
3. Query the configured `VectorStore` for candidate profile and experience documents, with retrieval
   bounded by `pool-cap`. The Qdrant adapter may use the vector store's semantic-search primitive,
   but the application must not read similarity-score metadata or depend on result ordering for
   matching rank.
4. Read the normalized `email` payload from each returned document. Deduplicate vector hits by
   normalized email, so a profile and several experience hits resolve to one candidate. Resolve each
   distinct email through `CandidateRepository.findByEmail` and use the persisted candidate record
   as the ranking input.
5. Ignore hits with missing/unusable email metadata and hits with no MongoDB candidate. Do not
   fabricate a candidate from Qdrant document text or metadata. Pass no more than `pool-cap` unique
   resolved candidates to ranking. If no candidate remains, return the successful empty response.

Do not add structured-field retrieval, hard eligibility filters, similarity thresholds, or a second
ranking stage in the retriever. Candidate resolution failures from MongoDB fail the request; an
individual missing candidate for a stale Qdrant hit is skipped.

## 5. LLM Chunk Ranking and Aggregation

### 5.1 Input preparation

Split resolved candidates into consecutive chunks no larger than `chunk-size`. For each chunk,
provide the selected position's required qualifications and other position fields plus the
candidates' structured profile and experience fields. Include the persisted candidate ID as the
response key. Do not include email, Qdrant payloads, resume source documents, or vector values in
the
prompt because they are not needed to rank position fit.

Use the application's configured `ChatModel`. Request one JSON object per chunk with this logical
shape:

```json
{
  "items": [
    {
      "candidateId": "candidate-id",
      "rank": 1,
      "matchPercentage": 85,
      "explanation": "Evidence: ... Gaps: ..."
    }
  ]
}
```

Ask the model to rank every candidate in its chunk, prioritizing required qualifications, then
relevant experience, with the other position fields as additional context. For each candidate,
request an integer `matchPercentage` from 0 through 100 estimated from the supplied structured
candidate and position evidence. The estimate is uncalibrated and is not a probability. It is for
display only and must not affect the candidate's ordinal rank or any cross-chunk ordering rule.
Explanations must cite supporting evidence and gaps grounded in the supplied structured data.
Missing candidate data is unknown, not proof of a gap.

### 5.2 Parallel execution and validation

Create one Java Virtual Thread per chunk for the duration of a match request. Submit all chunk calls
and wait for all of them before producing a response. Use Java 21 virtual-thread executor APIs; do
not create a permanent thread per candidate or run one LLM request per candidate.

For each chunk response, validate that it is parseable and that:

- Every candidate ID from the chunk appears exactly once.
- No ID outside that chunk appears.
- Ranks are unique, contiguous, and one-based within that chunk.
- Every item contains an integer `matchPercentage` in the inclusive range 0–100. Reject missing,
  non-integer, or out-of-range values.
- Every candidate has a non-empty explanation.
- The response contains no extra candidate entries.

Any invalid response or worker failure fails the entire match request. Cancel outstanding work where
possible, discard all partial chunk results, and return the controlled matching error. Never expose
the raw model response.

### 5.3 Final global order

Within each completed chunk of size `n`, convert the model's ordinal rank `r` into a normalized
within-chunk value:

```text
n == 1: value = 1
n > 1:  value = (n - r) / (n - 1)
```

Sort all candidates from all chunks by this value in descending order. If values tie, sort by
persisted candidate ID in ascending lexical order. Assign contiguous one-based final ranks after
sorting, then apply `max-results`.

Do not use Qdrant result order or scores in this sort and do not make a final LLM call to compare
chunks. This is the specified deterministic cross-chunk merge; candidates with equal normalized
within-chunk values are ordered by ID. Do not use `matchPercentage` in the sort, tie-break, final
rank assignment, or result-limit selection.

## 6. SPA Flow

Extend the Open Positions experience in `static/app.js` and `static/index.html`:

1. When no position is selected, hide the **Find Matches** action and show no prior match results.
2. When a position is selected, show **Find Matches** in the existing top job-offer action row,
   immediately to the right of **Delete**. Style it with the exact color `#198754`. Keep existing
   position selection, edit, delete, and details behavior intact.
3. On click, clear any results belonging to another position, show an accessible loading status, and
   send `POST /api/job-offers/{selectedPosition.id}/matches`. Prevent duplicate submissions while
   that request is pending.
4. On success, render results in their existing ordinal rank order without showing list-number
   markers. Each entry heading is a keyboard-accessible link-style button labeled
   `Candidate Name - Position - 85% match`; it opens that candidate's current match-details view.
   Render the explanation below the heading. Render all values as text, not HTML. The percentage is
   display-only and does not alter order. For an empty `items` array, show an empty-result message
   without an error.
5. Selecting a result opens a dedicated match-details view. Fetch
   `GET /api/candidates/{candidate.id}` and render the Feature 04 details and experiences together
   with that result's explanation. Keep the result array in frontend state. A **Back to results**
   action returns to the existing ranking without sending another request.
6. If selection or position changes while a request is in flight, use a request-generation/selected
   position guard and ignore stale responses. Selecting a different candidate while details load
   similarly discards the old candidate response.
7. A new explicit match request replaces the prior result set. On failure, clear any result set for
   the current attempted request, show a generic retryable error, and do not present partial or
   stale
   results as current.

Use semantic buttons/list markup and live regions for loading, empty, and error states. Preserve
keyboard operation. Existing candidate details rendering conventions (text nodes, empty-value
placeholder, and empty-experience message) apply to the match-details view.

## 7. Logging, Error Handling, and Privacy

- Scope match exception handling to `CandidateMatchController`.
- Return stable error codes and generic messages. Do not return stack traces, exception messages,
  provider responses, prompts, or candidate/position contents.
- Log only the matching stage, outcome, and sanitized failure category. Do not log candidate IDs,
  emails, names, position IDs or text, candidate fields, Qdrant documents, prompts, LLM responses,
  or vector data.
- Successful matching and candidate details reads do not log candidate values.
- A failure in one worker makes the whole request fail; no partial match result is logged or
  returned.
- Matching errors must not affect the existing job-offer endpoints or Candidates tab.

## 8. Verification Mapping

| Acceptance criterion | Design verification                                                                                                                                                                     |
|----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| AC01–AC02            | Controller/service tests verify selected position ID, missing-position response, and no retrieval for a missing position; UI tests verify selected-only action placement after Delete and exact `#198754` color. |
| AC03                 | Retriever and service tests verify semantic-only retrieval, duplicate profile/experience hit deduplication, MongoDB resolution, and ignored Qdrant score/order data.                    |
| AC04                 | Prompt/validator tests verify evidence instructions and valid percentage bounds; API and UI tests verify the score field, `Candidate Name - Position - P% match` clickable heading, and result order. |
| AC05                 | Configuration/service/ranker/aggregator tests verify limits, chunking, Virtual Threads, joins, ordering independent of percentages, and truncation.                                        |
| AC06                 | Configuration tests verify zero, negative, non-integer, and valid values/defaults.                                                                                                      |
| AC07                 | Service/UI tests verify empty successful results and no LLM call for an empty pool.                                                                                                     |
| AC08                 | UI tests verify loading and clearing stale results on position change.                                                                                                                  |
| AC09                 | UI tests verify candidate detail loading, explanation association, back navigation, and preserved results.                                                                              |
| AC10                 | Controller/service/UI tests verify Qdrant, MongoDB, LLM, malformed-output, and worker failures return no partial/stale results with a retry action.                                     |
| AC11                 | API and log-capture tests verify read-only data flow and absence of candidate, position, prompt, response, and vector content.                                                          |

## 9. Design Boundaries

- Do not implement or specify structured-field candidate retrieval, Qdrant score-based ranking,
  persisted match history, or background matching.
- Do not modify candidate ingestion, candidate records, job-offer persistence, or Qdrant embeddings.
- Do not add candidate management endpoints or change the existing candidate summary/detail
  contracts.
- Do not add authentication or authorization behavior.
- Keep all behavior traceable to `01-requirements.md`; the ranking prompt and output validator must
  not invent additional eligibility rules.
