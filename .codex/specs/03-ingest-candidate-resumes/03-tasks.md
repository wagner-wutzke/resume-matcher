# Task List: Candidate Resume Ingestion

- **Status:** [x] Ready | [ ] Active | [ ] Paused | [ ] Completed
- **Target Agent:** @engineer
- **Validation Agent:** @reviewer
- **References:** `01-requirements.md`, `02-design.md`

> The design is approved for implementation. Implement one task at a time. Each implementation task
> includes focused automated verification; each implemented code unit must meet the repository's
> 90% unit-coverage rule. The reviewer owns task-status changes after verification.

## Phase 1: Dependencies, Configuration, and Candidate Persistence

- [x] **Task 1.1 — Replace the Pinecone integration with Qdrant:** Update Maven dependencies and
  Spring AI vector-store configuration to use Qdrant, retaining provider-neutral `ChatModel` and
  `EmbeddingModel` interfaces. Verify dependency resolution and application-context configuration
  without requiring a live Qdrant service (**FR08, NFR04**).
- [x] **Task 1.2 — Add ingestion configuration and startup validation:** Inject the input directory
  using `@Value`; configure the two-second stability interval and required timeout; validate paths,
  duration values, and result-directory creation. Add focused property/startup tests (**FR01–FR02,
  AC21, NFR04**).
- [x] **Task 1.3 — Add candidate and experience persistence models:** Map the candidate document and
  embedded experience fields to MongoDB, normalize email to lowercase/trimmed form, and enforce a
  unique email index before ingestion starts. Add mapping, normalization, and uniqueness tests
  (**BR03–BR04, FR07, AC08–AC09**).
- [x] **Task 1.4 — Implement candidate replacement persistence:** Insert new candidates and replace
  existing candidates by normalized email while retaining the MongoDB identifier. Cover successful
  insert/replacement and unique-index race failures (**BR04, FR07, AC08–AC09**).

## Phase 2: PDF Extraction and Candidate Data Processing

- [x] **Task 2.1 — Implement PDF text extraction:** Use `PagePdfDocumentReader` for each file,
  preserve page order, and treat unreadable PDFs or empty extracted text as failures. Verify that
  image-only PDFs do not invoke OCR (**FR03, AC04, AC07, AC23**).
- [x] **Task 2.2 — Implement extracted-text cleanup:** Trim outer whitespace, collapse consecutive
  horizontal whitespace, reduce three-or-more line breaks to two, and preserve single line breaks.
  Add focused cleaner tests (**FR04, AC04**).
- [x] **Task 2.3 — Implement structured LLM extraction:** Request the FR05 JSON shape from the
  configured Spring AI chat model and parse one structured result without selecting a model
  provider. Add adapter tests with mocked model responses (**FR05, AC05**).
- [x] **Task 2.4 — Validate and normalize candidate data:** Normalize optional scalar fields and
  nested experience fields to empty strings, missing experiences to an empty list, and email to
  trimmed lowercase. Reject malformed JSON, invalid email, wrong types, or invalid experience
  entries. Add valid/invalid schema tests (**FR06, BR03–BR04, AC05, AC07, AC15**).
- [x] **Task 2.5 — Calculate total experience:** Parse supported year-only, month/year, and ongoing
  periods; calculate elapsed durations, sum overlapping periods independently, and format zero
  when no usable periods exist. Add tests for processing date, month precision, overlaps, and no
  periods (**BR05, AC06**).

## Phase 3: Qdrant Document Construction and Persistence

- [x] **Task 3.1 — Build profile and experience embedding documents:** Format profile text with
  email and the approved profile fields; format a separate document for each experience. Attach
  the exact FR08 metadata, including combined location and empty-string fallbacks. Add document
  builder tests (**FR08, AC10, AC24**).
- [x] **Task 3.2 — Implement Qdrant write and replacement operations:** Store profile/experience
  documents, filter by normalized email, remove a candidate's prior documents, and ensure point IDs
  are stable and do not contain email. Provision the Qdrant email payload index. Test with a mocked
  Qdrant adapter (**BR04, FR08, AC10–AC11**).
- [x] **Task 3.3 — Implement Qdrant snapshot and restore operations:** Read all matching points,
  including IDs, vectors, text, and payload, and restore an exact snapshot or remove newly created
  points. Add success and failure-path tests (**BR09–BR10, AC16–AC17**).

## Phase 4: Input Folder Lifecycle

- [x] **Task 4.1 — Implement the file-readiness gate:** Wait for unchanged file size for the
  configured interval, apply the required timeout, and route timed-out files without reading them.
  Add deterministic clock/filesystem tests (**BR12–BR13, AC19–AC21**).
- [x] **Task 4.2 — Implement result-folder movement and failure logging:** Create `handled` and
  `error` folders, move files to the correct destination, overwrite same-name destinations, retain
  the source on move failure, and log filename/stage/sanitized cause without candidate data. Add
  filesystem and logger tests (**BR06–BR08, FR09–FR10, AC12–AC14, AC22**).
- [x] **Task 4.3 — Implement startup scanning and directory watching:** Scan only direct input
  children, watch create/modify events, handle overflow by rescanning, deduplicate in-flight paths,
  ignore non-PDF files/result folders, and never retry files from `error`. Add watcher/queue tests
  (**BR01–BR02, BR11, FR01–FR02, AC01–AC03, AC18**).

## Phase 5: Ingestion Orchestration and Compensation

- [x] **Task 5.1 — Implement the candidate processing pipeline:** Coordinate readiness, PDF
  extraction, cleanup, LLM extraction, validation, and experience calculation. Verify failures stop
  before any store writes and do not stop later queued files (**FR02–FR06, NFR03, AC04–AC07,
  AC15, AC23**).
- [x] **Task 5.2 — Implement compensating rollback across MongoDB and Qdrant:** Snapshot prior
  candidate/vector state before mutation; on any later write or `handled` move failure, restore
  both stores. If compensation fails, stop automatic attempts, flag manual recovery, log the
  filename and cause, and route the PDF to `error` when possible. Add tests for new candidates,
  replacements, and incomplete rollback (**BR06–BR10, AC13–AC17**).
- [x] **Task 5.3 — Wire the startup runner and process lifecycle:** Connect configuration,
  directories, watcher, queue, processor, and shutdown cleanup. Verify startup scan plus live
  events use one queue and one file failure does not stop later files (**FR01–FR02, AC01–AC03,
  AC12–AC13, AC18–AC21**).
- [x] **Task 5.4 — Add stage-level ingestion logging:** Log discovery/queueing and stage start/end
  events with source filename, stage, outcome, and elapsed time; log downstream stages as skipped
  after a prior failure. Cover the required pipeline and compensation stages with approved severity
  levels; prevent candidate data, email, prompts, model responses, and credentials from entering
  logs. Add log-capture tests (**FR10, NFR01, NFR06, AC14, AC17, AC25**).

## Phase 6: Acceptance Verification and Review

- [x] **Task 6.1 — Verify acceptance coverage:** Map AC01–AC25 to automated tests, run the relevant
  backend suite, and resolve failures. Verify coverage requirements and `spotless:check` (**all
  ACs, NFR05–NFR06**).
- [x] **Task 6.2 — Review scope and data safety:** Confirm no UI/API, OCR, automatic retry, model
  provider selection, or candidate matching was added; inspect email normalization, stage logs,
  overwrite behavior, and rollback boundaries (**BR01–BR13, NFR01–NFR04, NFR06**).
- [x] **Task 6.3 — Reviewer report and status:** Record spec compliance, AC evidence, gaps, and
  improvement suggestions in `04-review.md`. Update task checkboxes only after reviewer
  verification; approve completion only when no unresolved gaps remain (**all ACs**).
