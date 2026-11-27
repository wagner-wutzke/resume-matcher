# Technical Design: Candidate Resume Ingestion

- **Status:** [ ] Draft | [x] Approved for Implementation
- **Author/Agent:** @architect
- **Reference Spec:** `01-requirements.md`

## 1. Architecture

Implement ingestion as a Spring Boot background service. It exposes no HTTP endpoint and does not
change the existing SPA. A startup runner validates configuration, creates the `handled` and
`error` directories when absent, scans direct child files, and starts a `WatchService` on the
configured input directory. Startup scan and filesystem events feed the same single-instance
processing queue so a create event cannot race a startup scan for the same path.

Use one worker per application instance. Keep an in-flight set keyed by normalized absolute path
to suppress duplicate events while a file is queued or processing. On watcher overflow, rescan the
input directory. The queue does not watch nested folders or either result directory. A file moved
to `error` is never picked up by startup scan or watcher; a later manual re-presentation in the
input directory is a new ingestion attempt.

The processing pipeline is:

1. Wait for file readiness using the configured stability interval and timeout.
2. Extract PDF pages, join their text, and normalize whitespace.
3. Ask the configured Spring AI chat model for the structured candidate result.
4. Normalize and validate the result, including the required email and derived experience total.
5. Capture the current MongoDB candidate and Qdrant points for compensation.
6. Replace candidate data in MongoDB and profile/experience documents in Qdrant.
7. Move the PDF to `handled`.
8. If a post-write stage fails, compensate both stores, then move the PDF to `error`.

Suggested components, following the existing `net.wowdev.ai.resume.matcher` package structure:

| Component | Responsibility |
|---|---|
| `ResumeIngestionProperties` | Read settings using Spring `@Value` and validate the input path, stability interval, timeout, and Qdrant settings. |
| `ResumeIngestionRunner` | Validate directories, perform startup scan, and start/close the watcher and worker. |
| `ResumeDirectoryWatcher` | Observe direct child create/modify events and request rescans on overflow. |
| `ResumeFileReadinessGate` | Wait for unchanged file size and enforce the configured timeout. |
| `ResumePdfTextExtractor` | Use `PagePdfDocumentReader` and return extracted page text. |
| `ResumeTextCleaner` | Apply FR04 whitespace normalization. |
| `CandidateDataExtractor` | Invoke the configured Spring AI chat model and parse the structured output. |
| `CandidateDataValidator` | Normalize optional values/email, validate the response, and derive experience total. |
| `CandidateResumeProcessor` | Coordinate persistence, vector replacement, compensation, file movement, and logging. |
| `CandidateRepository` | Read and replace candidate records by normalized email. |
| `CandidateVectorStore` | Create, replace, snapshot, and restore profile/experience embeddings in Qdrant. |
| `ResumeFileMover` | Create result directories and move/overwrite files according to FR09. |

Keep the processor's dependencies behind interfaces so PDF, LLM, MongoDB, Qdrant, and filesystem
failures can be tested independently with JUnit 5 and Mockito.

## 2. Configuration and Startup

Add ingestion settings in `application.properties` and inject the input folder with `@Value` as
required by FR01:

| Setting | Default / validation |
|---|---|
| `resume.ingestion.input-directory` | Required path. It must resolve to an existing readable directory. |
| `resume.ingestion.stability-interval` | ISO-8601 duration; defaults to `PT2S`; must be positive. |
| `resume.ingestion.stability-timeout` | Required ISO-8601 duration; no default; must be positive and greater than the stability interval. |
| `spring.ai.vectorstore.type` | Set to `qdrant` for this feature; remove the Pinecone starter from Maven. |
| `spring.ai.vectorstore.qdrant.host` / `port` | Resolve from deployment configuration; do not hard-code service credentials. |
| `spring.ai.vectorstore.qdrant.collection-name` | Use a dedicated candidate-resume collection; keep the name configurable. |

If the input folder, required timeout, or vector store settings are missing or invalid, fail
startup with a clear configuration error. Do not start a watcher that cannot meet the specified
behavior. Create `handled` and `error` as siblings of the input folder; failure to create either
folder also prevents ingestion from starting.

Use the Qdrant Spring AI vector-store integration compatible with the project's Spring AI BOM,
replacing the current Pinecone integration. Keep `ChatModel` and `EmbeddingModel` provider/model
selection outside this feature as required by the scope. The deployment must supply working model
beans; the current `none` model settings are suitable only for contexts that do not run ingestion.
Provision the Qdrant collection for the configured embedding dimension as a deployment
prerequisite; the model/provider choice is outside this feature. Provision an `email` payload index
for candidate point filtering. Qdrant unavailability during a file's processing is handled as a
per-file failure and routed to `error` when possible.

## 3. Data Model and Persistence

Add a MongoDB `Candidate` document in a `candidates` collection:

| Field | Type | Rule |
|---|---|---|
| Mongo `_id` | Mongo identifier | Generated for new candidates; retained when replacing the same email. |
| `email` | String | Trimmed and lowercased before persistence; unique index. |
| `fullname`, `country`, `city`, `position`, `skills`, `allocation` | String | Store empty strings when missing. |
| `yearsOfExperience` | String | Derived and formatted as `N year(s) and M month(s)`. |
| `experiences` | List of embedded experience values | Empty list when none; each value has `title`, `company`, `period`, `responsibilities`, and `achievements` strings. |

Create a MongoDB unique index on the normalized `email`. Ensure index creation happens before
ingestion starts; duplicate existing values or index-creation errors are startup failures and must
not be silently ignored. Store the source filename in Qdrant metadata as specified; do not add it
to the candidate document because FR07 does not require that persisted field.

For a new email, insert a candidate. For an existing normalized email, replace its structured
fields while retaining its MongoDB identifier. Handle a concurrent unique-index violation as a
failed ingestion and do not create duplicate records.

## 4. PDF and LLM Processing

Create one `PagePdfDocumentReader` per file using a filesystem resource. Join page content in page
order with line breaks, then apply FR04: trim the whole text, collapse consecutive spaces/tabs
within a line to one space, reduce runs of at least three line breaks to two, and preserve single
line breaks. Treat unreadable PDFs and empty cleaned content as processing failures. Do not invoke
OCR.

Pass the cleaned text to the configured Spring AI chat model with a structured-output converter
for the FR05 object. Require a single parseable JSON object; reject invalid JSON, invalid email,
incorrect scalar/list/object types, or malformed experience entries. Normalize email before using
it. Normalize absent optional scalar fields (including absent fields within an experience) to
empty strings and absent experiences to an empty list. Do not accept an LLM-supplied total as
authoritative; calculate it from the parsed `period` values.

Parse calendar-year ranges and month/year ranges when recognizable. Treat `Present`, `Today`, and
equivalent end markers as the processing date. For year-only ranges, use end year minus start year
and zero months. For month-level ranges, calculate elapsed months. Sum each experience duration
independently, including overlaps. Ignore periods that cannot be parsed for the total while
retaining their extracted text; if none are usable, set `yearsOfExperience` to
`0 years and 0 months`. Format the result with singular/plural unit labels.

## 5. Qdrant Documents and Metadata

Create a profile document with these text fields in a stable labeled format: `email`, `fullname`,
`country`, `city`, `position`, `skills`, `allocation`, and `yearsOfExperience`. Create one
additional document per experience, containing `title`, `company`, `period`, `responsibilities`,
and `achievements`. With no experiences, create only the profile document.

Attach these metadata values to every document:

| Metadata key | Value |
|---|---|
| `location` | City and country joined with `, `; omit a missing component; empty string if both are missing. |
| `email` | Normalized lowercase email. |
| `fileName` | Source PDF basename. |
| `position` | Extracted position, or empty string. |
| `yearsOfExperience` | Derived formatted total. |

Use a dedicated Qdrant collection for these documents. Filter replacement and rollback by normalized
email. Qdrant point IDs are deterministic UUIDs derived from the candidate MongoDB identifier and
document role/index, so they do not contain email. This gives the profile and each experience a
stable, distinct point ID across replacements.

## 6. Replacement and Cross-Store Compensation

MongoDB and Qdrant do not share a transaction. Implement a compensating workflow to satisfy BR09:

1. Before changing either store, load the prior candidate (if any) and snapshot all Qdrant points
   matching the normalized email, including point IDs, vector values, text, and payload.
2. If either snapshot cannot be read, make no changes and fail the file.
3. Replace the MongoDB candidate, then replace the Qdrant points for the email with the new profile
   and experience documents.
4. Move the PDF to `handled` only after both writes succeed. If moving to `handled` fails, treat it
   as a processing failure and compensate both stores as well.
5. On a failure after mutation, remove newly written Qdrant points, restore the Qdrant snapshot,
   and restore the prior MongoDB document (or remove the new document when there was no prior
   candidate). Attempt each independent compensation once even if another compensation fails; do
   not retry a failed compensation. Verify restoration before treating rollback as complete.
6. If compensation fails, stop automatic rollback attempts, leave the file unhandled, move it to
   `error` when possible, and mark manual recovery required. Log filename, stage, and a sanitized
   cause. Do not log the email, resume text, extracted fields, prompts, or credentials.

Use Qdrant operations that can list all points matching the email filter and restore their
original IDs, vectors, text, and metadata. If Spring AI's `VectorStore` abstraction cannot expose
the vector snapshot, use the Qdrant client for snapshot/restore while using the Spring AI vector
store for normal document embedding and writes. Do not begin a replacement unless the previous
state can be snapshotted.

This is compensating rollback, not a distributed transaction. The implementation stops after a
failed compensation and leaves manual recovery to an operator; it does not start a retry loop.

## 7. File Readiness, Watching, and Movement

At startup, list direct regular files with case-insensitive `.pdf` extensions. For each candidate,
sample its size until it is unchanged for `stability-interval`; poll at an implementation-defined
sub-interval that allows the configured window to be observed. If it does not stabilize before
`stability-timeout`, move it to `error` without parsing it.

Register the input directory for create and modify events. Route eligible events through the same
readiness gate and queue as startup files. On `OVERFLOW`, rescan direct children. Ignore result
subfolders and non-PDF files. Keep one active queue entry per normalized absolute path. Processing
is sequential in this design; one failure is caught and logged so later queued files still run.

For successful processing, use `Files.move` to place the source under `handled`; for a failed
processing attempt, use `error`. Create result folders at startup. Use replacement semantics for a
same-name destination per BR08. If moving to `handled` fails, compensate stores and then attempt
the `error` move. If an error move also fails, keep the source file in place and log the move
failure. Never scan the `error` folder for retries.

## 8. Logging and Error Handling

For each eligible PDF, log discovery/queueing and start/end events for every executed processing
stage. Log downstream stages as skipped when an earlier failure prevents them from running. Include
the source filename, stage, outcome, and elapsed time on completed stages. Cover
readiness, PDF extraction, cleanup, LLM extraction, validation, experience calculation, MongoDB
and Qdrant snapshots/writes, compensation, file movement, and the overall outcome. Use
parameterized SLF4J messages; successful lifecycle events use `INFO`, recoverable per-file failures
use `WARN`, and incomplete rollback uses `ERROR` with a sanitized exception that does not retain a
sensitive original exception message or cause chain. The filename is an explicit logging exception
to the general PII restriction; never log email, resume text, extracted fields, LLM prompt/output,
or credentials. Log an explicit manual-recovery-required outcome when compensation fails.

Failures before mutation do not need compensation. Failures after the MongoDB or Qdrant write, or
after a failed `handled` move, invoke compensation. A failed single file does not stop the worker.
Do not expose an API error contract because this feature has no public ingestion API.

## 9. Verification Mapping

| Acceptance criterion | Design verification |
|---|---|
| AC01 | Startup runner test with direct PDFs and pre-existing `handled`/`error` contents. |
| AC02 | Watcher test for lowercase/uppercase PDF create events and non-PDF ignore. |
| AC03 | Queue/in-flight test for duplicate events on one normalized path. |
| AC04 | PDF extractor and cleaner tests for page order and whitespace rules. |
| AC05 | LLM adapter/validator tests for JSON shape, missing optional values, and no invented defaults. |
| AC06 | Experience calculator tests for year-only, month-level, ongoing, overlapping, and no-period cases. |
| AC07 | Pipeline tests for unreadable/empty PDFs and malformed LLM output; assert no store writes. |
| AC08 | Mongo repository/service test for all structured fields and nested experiences. |
| AC09 | Mongo tests for normalized email replacement, retained identifier, and unique-index violation. |
| AC10 | Vector-store test for profile/experience count, metadata values, and empty experiences. |
| AC11 | Qdrant integration-adapter test that old email-filtered points are replaced. |
| AC12 | File mover test verifies source is under `handled` after all writes succeed. |
| AC13 | Failure-path tests verify error movement, overwrite behavior, and queue continuation. |
| AC14 | File mover failure test verifies source retention and sanitized filename/cause logging. |
| AC15 | Validator/pipeline test verifies invalid or absent email causes no writes and routes to error. |
| AC16 | Compensation tests cover new-candidate removal and previous Mongo/Qdrant state restoration. |
| AC17 | Compensation-failure test verifies stop, error move, manual recovery log, and no PII fields. |
| AC18 | Startup/watcher tests verify the `error` folder is never scanned for automatic retries. |
| AC19 | Readiness-gate test proves extraction waits for the configured stable-size window. |
| AC20 | Readiness-gate test verifies timeout routing without extraction. |
| AC21 | Configuration tests verify missing, invalid, and valid timeout settings. |
| AC22 | File mover test verifies replacement of same-name `handled` and `error` files. |
| AC23 | PDF pipeline test verifies no OCR call and error routing for image-only/no-text PDFs. |
| AC24 | Document builder tests verify exact profile and experience text fields, including profile email. |
| AC25 | Log-capture tests verify stage start/end and skipped events, filename, outcome, elapsed time, required stages, severity, and absence of candidate data. |

## 10. Design Boundaries and Notes

- Do not add candidate search, matching, ranking, recommendations, UI, or an ingestion API.
- Do not select a specific LLM or embedding model/provider; use configured Spring AI model
  interfaces.
- Keep the configured `PagePdfDocumentReader` as the only PDF text extraction mechanism; no OCR.
- Qdrant replaces the current Pinecone dependency for this feature, consistent with the approved
  requirement and `TECH-SPECS.md`.
