# Feature Specification: 03 — Ingest Candidate Resumes

- **Status:** [ ] Draft | [x] Approved for Design | [ ] In Implementation | [ ] Completed
- **Author/Agent:** @architect
- **Date:** 2026-11-27

## 1. Objective

Automatically process candidate resume PDF files from a configured local folder, extract and
validate structured candidate data, persist it in MongoDB, and save searchable embeddings in
Qdrant for future RAG use.

## 2. Context

The application currently has no candidate ingestion workflow. This feature establishes the data
and ingestion foundation for later candidate search and matching. Candidate data is personally
identifiable; resume text and extracted values must be handled as sensitive information.

The configured folder is the ingestion boundary. Successfully handled PDFs are moved into its
`handled` subfolder, and PDFs that fail processing are moved into its `error` subfolder. Those
subfolders are output locations and are not themselves watched as input folders.

## 3. Feature Scope

### In Scope

- Configure a local resume input folder in `application.properties` and inject its value with
  Spring `@Value`.
- Ingest existing eligible PDFs when the application starts and automatically ingest new PDFs
  added to the configured folder while the application is running.
- Extract PDF text with Spring AI's `PagePdfDocumentReader`, clean the extracted text, and use the
  configured LLM to produce candidate data in the specified JSON shape.
- Validate and persist candidate data in MongoDB, replacing the existing candidate for a matching
  email address.
- Create embeddings from the structured candidate data and store them in Qdrant with the specified
  metadata.
- Move each processed PDF to the appropriate result subfolder.

### Out of Scope

- Candidate search, matching, ranking, recommendations, or a candidate management UI/API.
- OCR or other image-based extraction for scanned PDFs that contain no extractable text.
- Resume editing, manual review/approval, and automated retries of files already moved to `error`.
- Defining the future RAG query flow or the embedding model/provider.

## 4. Business Rules

- **BR01 — Eligible files:** Only regular files with a `.pdf` extension, case-insensitive, directly
  inside the configured input folder are ingestion candidates. Nested folders and other files are
  ignored.
- **BR02 — Single processing:** A PDF path must not be processed concurrently more than once by
  this application instance, including when the filesystem reports duplicate events.
- **BR03 — Required identity:** A candidate must have a syntactically valid email address to be
  persisted. Missing optional scalar fields are stored as empty strings, and missing experiences
  are stored as an empty list.
- **BR04 — Email uniqueness and replacement:** Normalize email by trimming surrounding whitespace
  and converting it to lowercase before comparison or storage.
  At most one candidate record and one current set of candidate embeddings may exist for a
  normalized email. Re-ingesting a resume for an existing email replaces its candidate data and
  embeddings.
- **BR05 — Experience calculation:** Calculate `yearsOfExperience` from the sum of the durations
  of the reported experience periods. Treat `Present`, `Today`, or an equivalent ongoing marker as
  the date when that resume is processed. When a period contains only years, calculate end year
  minus start year and report zero months; when start and end months are available, calculate
  elapsed months (end month minus start month, combined with the year difference). Include the full
  duration of every reported experience, including time overlapping with another experience. If no
  usable period is available, store `"0 years and 0 months"`.
- **BR06 — Successful handling:** A PDF is successful only when extraction, structured-data
  validation, MongoDB persistence, Qdrant embedding storage, and the move to `handled` complete.
- **BR07 — Failed handling:** A failure at any processing stage prevents the file from being
  considered successful and routes it to `error` when the move is possible. Reprocessing the same
  email must replace, not duplicate, its MongoDB record and Qdrant embeddings.
- **BR08 — Destination collision:** If a file with the same filename already exists in the
  destination result folder, replace it with the processed PDF.
- **BR09 — Cross-store rollback:** If ingestion fails after changing MongoDB or Qdrant, restore
  both stores to their pre-ingestion state. Remove newly created candidate data and embeddings, or
  restore the previous candidate data and embeddings when replacing an existing candidate. The
  file is not successful and is routed to `error` when possible.
- **BR10 — Incomplete rollback:** If rollback cannot restore both stores, stop automatic rollback
  attempts, record that manual recovery is required, log the source filename and sanitized failure
  cause, and route the PDF to `error` when possible. Do not report the ingestion as successful.
- **BR11 — No automatic retry:** A PDF moved to `error` is not automatically retried. A later
  ingestion requires the file to be presented again in the configured input folder.
- **BR12 — File readiness:** Do not begin reading a newly added PDF while it is still being
  written. Wait until its size has remained unchanged for the stability interval configured in
  `application.properties`, defaulting to 2 seconds.
- **BR13 — Readiness timeout:** If a new PDF does not stabilize before the timeout configured in
  `application.properties`, treat it as a failed file and route it to `error` when possible. The
  timeout is configurable and must be explicitly set; it has no default.

## 5. Functional Requirements

### FR01 — Configure and load resume files

Read the input folder from an `application.properties` setting and inject it with Spring `@Value`.
On application startup, inspect its immediate contents and queue eligible PDFs that are not in the
`handled` or `error` folders. Do not scan nested folders.

### FR02 — Automatically ingest new PDFs

Monitor the configured input folder while the application is running. Ingest each newly added
eligible PDF without requiring a manual API call. Ignore non-PDF files and files inside the
`handled` and `error` folders, and do not watch nested input folders. Do not process the same path
concurrently more than once or begin reading a new PDF until its size has remained unchanged for
the interval configured by
`resume.ingestion.stability-interval` in `application.properties`; the default is 2 seconds. If
the file does not stabilize before `resume.ingestion.stability-timeout`, route it to `error` when
possible. This property is required and has no default; application startup fails with a
configuration error if it is missing or invalid.

### FR03 — Extract PDF text

Use a `PagePdfDocumentReader` instance to extract text from each eligible PDF. A PDF that cannot
be read or yields no usable text is a processing failure.

### FR04 — Clean extracted text

Trim leading and trailing whitespace, collapse consecutive horizontal whitespace within a line to
a single space, and reduce three or more consecutive line breaks to two. Preserve single line
breaks and paragraph separation.

### FR05 — Extract structured candidate data

Send the cleaned text to the configured LLM and request one JSON object with this shape:

```json
{
  "email": "john.doe@gmail.com",
  "fullname": "John Doe Gallagher",
  "country": "Mexico",
  "city": "Monterey",
  "position": "AWS Cloud Developer",
  "skills": "AWS CDK, Terraform, CloudFormation, Lambda, ECS Fargate, EKS",
  "allocation": "Remote",
  "yearsOfExperience": "12 years and 0 months",
  "experiences": [
    {
      "title": "AWS Cloud Developer",
      "company": "Vale Byte - digital banking",
      "period": "2016 – Present",
      "responsibilities": "Designed secure document exchange for digital banking...",
      "achievements": "Delivered secure document exchange capability for digital banking..."
    }
  ]
}
```

The top-level fields are `email`, `fullname`, `country`, `city`, `position`, `skills`,
`allocation`, `yearsOfExperience`, and `experiences`. Each experience has `title`, `company`,
`period`, `responsibilities`, and `achievements`. Use an empty string for a missing optional scalar
field and an empty array when no experiences are found. Do not invent candidate facts. Derive
`yearsOfExperience` from the reported experience periods according to BR05. For the example's
periods, using 2026 as the processing year, the derived total is 12 years and 0 months.

### FR06 — Validate structured data

Accept the LLM result only if it is parseable as one JSON object matching the specified field
shape, every scalar field is a string, `experiences` is an array of objects with the specified
string fields, and `email` is a valid non-empty email after trimming. Missing optional scalar
values must be normalized to empty strings and missing experiences to an empty array before
validation. Reject malformed JSON, wrong field types, invalid email, or an invalid experience
entry. Do not persist or create embeddings from a rejected response.

### FR07 — Persist candidate data in MongoDB

Persist all structured fields, including the nested experience records. Before persistence, find a
candidate using the normalized email defined by BR04. Replace the existing record for that email
when present; otherwise create a record. Enforce email uniqueness in storage so concurrent
ingestion cannot create duplicate candidates.

### FR08 — Create and store candidate embeddings

Create one profile embedding from `email`, `fullname`, `country`, `city`, `position`, `skills`,
`allocation`, and `yearsOfExperience`. Create one separate embedding for each work experience from
`title`, `company`, `period`, `responsibilities`, and `achievements`. Store the embeddings in
Qdrant. If there are no experiences, store only the profile embedding. Include these metadata
values on every embedding document:

- `location`: city and country joined with `, `, omitting either part if it is unavailable. Use an
  empty string if both are unavailable.
- `email`: the normalized email.
- `fileName`: the source PDF's filename.
- `position`: the extracted position, or an empty string if unavailable.
- `yearsOfExperience`: the derived value, or `"0 years and 0 months"` if unavailable.

When replacing a candidate by email, remove the previous profile and experience embeddings for that
normalized email so only the current resume's profile and experience embeddings remain.

### FR09 — Move the processed PDF

After all processing stages succeed, move the PDF to the configured folder's `handled` subfolder.
If any processing stage fails, move it to the `error` subfolder. Create either subfolder when it
does not exist. If a file with the same name already exists in the destination, overwrite it with
the processed PDF. If the move itself fails, leave the source file in place and log the failure.

### FR10 — Report processing failures

For a processing or rollback failure, log the source filename, the failing processing stage, and a
sanitized cause that identifies the error without logging resume text, extracted candidate values,
credentials, or other resume content. If rollback is incomplete, make that status clear in the log
and indicate that manual recovery is required.

## 6. Non-Functional Requirements

- **NFR01 — Privacy:** Do not log resume text, extracted candidate fields, email addresses, LLM
  prompts/responses, credentials, or other resume content. Logs must include the source filename
  and sanitized failure cause when processing or rollback fails, as required by FR10.
- **NFR02 — Reliability:** Duplicate filesystem notifications and repeated processing of the same
  email must not create duplicate candidate records or duplicate current embeddings.
- **NFR03 — Failure isolation:** Failure to process one PDF must not stop ingestion of other PDFs
  in the input folder.
- **NFR04 — Configuration:** The input folder and file stability interval must be configurable
  without changing Java code. The stability timeout must also be configurable and explicitly set.
- **NFR05 — Testability:** PDF reading, LLM extraction, MongoDB persistence, Qdrant writes, and file
  movement must be independently testable through focused automated tests.
- **NFR06 — Pipeline traceability:** Emit a log event when each eligible PDF is discovered and
  queued, and start and terminal events for each processing stage. A terminal event must identify
  whether the stage succeeded, failed, or was skipped; include elapsed time when it ran. Log
  downstream stages as skipped when an earlier failure prevents them from running. Cover at least
  readiness checking, PDF extraction, text cleanup, LLM extraction, structured-data validation,
  experience calculation, MongoDB snapshot/write, Qdrant snapshot/replacement, compensation, file
  movement, and the overall file outcome. Every event must identify the source filename and stage.
  Use parameterized SLF4J messages. Log successful lifecycle events at `INFO`, recoverable
  per-file failures at `WARN`, and incomplete rollback at `ERROR` with a sanitized exception.
  Never log resume content, extracted candidate fields, email values, credentials, prompts, or
  model responses; log only a sanitized failure cause.

## 7. Acceptance Criteria

### AC01 — Startup ingestion

- **Given** the configured input folder contains eligible PDF files directly inside it when the
  application starts
- **Then** each eligible PDF is queued for processing.
- **And** PDFs already under `handled` or `error` are not re-queued.

### AC02 — New-file ingestion

- **Given** the application is running and monitoring the configured folder
- **When** a new `.pdf` or `.PDF` file is added
- **Then** it is ingested without a manual request.
- **And** a non-PDF file is ignored.

### AC03 — No concurrent duplicate processing

- **Given** the filesystem reports the same PDF more than once while it is being processed
- **Then** the application processes that path no more than once concurrently.

### AC04 — PDF extraction and text cleanup

- **Given** a readable PDF with extractable text
- **When** it is processed
- **Then** text is extracted with `PagePdfDocumentReader`, horizontal whitespace is collapsed,
  three-or-more consecutive line breaks are reduced to two, and single line breaks are preserved.

### AC05 — Structured LLM response

- **Given** cleaned resume text
- **When** the configured LLM extracts candidate data
- **Then** the result contains the specified top-level fields and experience shape as JSON.
- **And** unavailable optional scalar values are represented as empty strings, and unavailable
  experiences as an empty array, rather than fabricated facts.

### AC06 — Experience total

- **Given** valid experience periods are present
- **When** `yearsOfExperience` is calculated
- **Then** durations are summed, `Present`, `Today`, or an equivalent ongoing marker uses the
  processing date, year-only periods contribute end year minus start year with zero months,
  month-level dates use elapsed duration (January 2020 to January 2021 is 12 months), and
  overlapping periods are included individually.
- **And** if no usable period is available, the result is `"0 years and 0 months"`.

### AC07 — Invalid or unreadable input handling

- **Given** a PDF is unreadable, contains no usable text, or the LLM result is malformed or fails
  FR06 validation
- **When** processing ends
- **Then** the candidate is not persisted and no new embeddings are created for that result.
- **And** the source PDF is routed to `error` when possible.

### AC08 — Candidate persistence

- **Given** a valid structured result with a valid email
- **When** MongoDB persistence succeeds
- **Then** the candidate's structured fields and nested experiences are stored.

### AC09 — Replacement and email uniqueness

- **Given** MongoDB already contains a candidate with the same normalized email
- **When** another resume for that email is successfully ingested
- **Then** the existing candidate is replaced by the new structured data.
- **And** only one candidate record exists for that normalized email.

### AC10 — Embedding metadata

- **Given** a valid candidate result is stored
- **When** embeddings are created in Qdrant
- **Then** one profile embedding and one embedding per reported experience are stored.
- **And** each embedding has `location`, normalized `email`, source `fileName`, `position`, and
  `yearsOfExperience` metadata with the values defined in FR08.
- **And** when there are no experiences, only the profile embedding is stored.

### AC11 — Embedding replacement

- **Given** a candidate already has embeddings for a normalized email
- **When** a new resume for that email is ingested
- **Then** the old embeddings are removed and only embeddings for the current resume remain.

### AC12 — Successful file movement

- **Given** extraction, validation, MongoDB persistence, and Qdrant storage all succeed
- **When** file handling completes
- **Then** the PDF is moved to `handled` and is no longer in the input folder.

### AC13 — Processing failure movement

- **Given** any processing stage fails
- **When** file handling completes
- **Then** the source PDF is moved to `error` when possible, replacing any existing file with the
  same name in that destination.
- **And** the failure does not stop processing other PDFs.

### AC14 — Move failure

- **Given** the application cannot move a processed PDF
- **Then** it leaves the source file in place and records a sanitized failure log with the source
  filename and cause, without resume text, extracted fields, email addresses, credentials, or
  prompts/responses.

### AC15 — Missing required email

- **Given** the LLM cannot find or returns an invalid email
- **When** the response is validated
- **Then** the response is rejected, no candidate is persisted, no embeddings are created, and the
  PDF is routed to `error` when possible.

### AC16 — Cross-store rollback

- **Given** ingestion changes MongoDB or Qdrant and a later persistence stage fails
- **When** rollback completes
- **Then** both stores contain their pre-ingestion state for that candidate email.
- **And** a newly created candidate and any embeddings created for it are removed.
- **And** for a replacement, the prior candidate data and embeddings are restored.
- **And** the source PDF is not moved to `handled`.

### AC17 — Incomplete rollback

- **Given** a processing failure occurs and the compensating rollback also fails
- **When** automatic rollback attempts stop
- **Then** the processing result is marked as requiring manual recovery and the PDF is moved to
  `error` when possible.
- **And** the error log identifies the source filename, failing stage, and sanitized cause.
- **And** the log contains no resume text, extracted candidate values, credentials, or resume
  content.

### AC18 — No automatic retry after error

- **Given** a PDF has been moved to the `error` subfolder
- **When** the application starts or continues monitoring the input folder
- **Then** it does not automatically retry that PDF from the `error` subfolder.

### AC19 — Wait for file readiness

- **Given** a PDF is added while its size is changing
- **When** the watcher observes the file
- **Then** it does not begin PDF extraction until the file has remained unchanged for the
  configured stability interval.

### AC20 — Readiness timeout

- **Given** a PDF continues changing and does not stabilize before the configured timeout
- **When** the timeout is reached
- **Then** it is not read or ingested and is moved to `error` when possible.

### AC21 — Required readiness-timeout configuration

- **Given** `resume.ingestion.stability-timeout` is missing or invalid
- **When** the application starts
- **Then** startup fails with a configuration error instead of starting ingestion without a
  readiness timeout.

### AC22 — Destination filename collision

- **Given** a file with the same name already exists in the selected `handled` or `error` folder
- **When** the processed PDF is moved there
- **Then** the existing destination file is overwritten by the processed PDF.

### AC23 — No OCR for image-only PDFs

- **Given** a PDF contains no text extractable by `PagePdfDocumentReader`
- **When** it is processed
- **Then** ingestion fails without OCR and the PDF is moved to `error` when possible.

### AC24 — Embedding text content

- **Given** a valid structured candidate with one or more experiences
- **When** embedding documents are created
- **Then** the profile text contains email, fullname, country, city, position, skills, allocation,
  and years of experience.
- **And** each experience text contains that experience's title, company, period,
  responsibilities, and achievements.

### AC25 — Pipeline stage logging

- **Given** an eligible PDF is discovered
- **When** it proceeds through the ingestion pipeline, including any failure and compensation
- **Then** each executed stage has a start event and an end event identifying the source filename,
  stage, and outcome.
- **And** completed stages include elapsed time.
- **And** logs cover the readiness check, extraction, cleanup, LLM extraction, validation,
  experience calculation, MongoDB and Qdrant operations, compensation, file movement, and overall
  outcome as applicable.
- **And** downstream stages that do not run after a failure have a terminal `skipped` event.
- **And** logs contain no resume content, candidate fields, email values, credentials, prompts, or
  model responses.
- **And** incomplete rollback is logged at `ERROR` with a sanitized exception and indicates that
  manual recovery is required.

## 8. Error Cases

- The configured input folder is missing, is not a directory, or cannot be read: report a
  sanitized configuration/startup error; do not silently claim ingestion is active.
- A file is unreadable, is not a valid PDF despite its extension, or has no extractable text:
  route it to `error` when possible and continue with other files.
- The LLM is unavailable, times out, returns malformed JSON, or returns invalid candidate data:
  do not accept that result; route the PDF to `error` when possible.
- MongoDB or Qdrant is unavailable: do not move the file to `handled`; route it to `error` when
  possible. Reprocessing the file must remain idempotent by normalized email.
- A destination filename already exists: replace it with the processed PDF.
- Moving the file fails: leave it at its source and log a sanitized failure category.
- Compensating rollback fails: stop automatic attempts, log the filename and sanitized cause, mark
  manual recovery as required, and route the PDF to `error` when possible.

## 9. Confirmed Decisions and Assumptions for Design

- **Confirmed:** A valid email is required. If it is missing or invalid, reject the result, persist
  no candidate or embeddings, and route the PDF to `error` when possible. Missing optional resume
  facts do not reject ingestion.
- **Confirmed:** Normalize email by trimming surrounding whitespace and converting it to lowercase
  before comparison or storage.
- **Confirmed:** Sum the duration of every reported experience separately, including overlapping
  periods.
- **Confirmed:** For month-level dates, use elapsed duration; January 2020 to January 2021 counts
  as 12 months.
- **Confirmed:** Build Qdrant `location` metadata from city and country joined with `, `, omitting
  either component when it is missing.
- **Confirmed:** If both city and country are missing, store an empty string as Qdrant `location`
  metadata.
- **Confirmed:** Store missing optional scalar fields as empty strings and missing experiences as
  an empty array.
- **Confirmed:** Overwrite any existing destination file with the same filename when moving the
  processed PDF to `handled` or `error`.
- **Confirmed:** Do not use OCR. A PDF with no text extractable by `PagePdfDocumentReader` is a
  processing failure.
- **Confirmed:** Create one profile embedding from the agreed profile fields, including email, and
  one separate embedding for each work experience from its agreed fields. Every embedding carries
  the same candidate metadata.
- **Confirmed:** Do not automatically retry PDFs after they have been moved to `error`.
- **Confirmed:** Scan and watch only files directly inside the configured input folder; do not
  recurse into nested folders.
- **Confirmed:** Wait for a new PDF's size to stabilize before beginning processing. Configure the
  interval with `resume.ingestion.stability-interval`, defaulting to 2 seconds.
- **Confirmed:** Configure a readiness timeout with `resume.ingestion.stability-timeout`. Its
  value is required; it has no default.
- The `handled` and `error` folders are sibling output folders and are excluded from input scans.
- Candidate persistence and Qdrant storage are separate systems without a cross-store transaction.
  A file is marked handled only after both succeed. On a persistence failure, both stores must be
  restored to their pre-ingestion state as required by BR09.
- The example's experience total uses processing year 2026. At other processing dates, calculate
  the total from the rule in BR05.
- The repository currently declares a Pinecone Spring AI dependency. The selected vector store for
  this feature is Qdrant; the design must account for replacing or removing the Pinecone
  integration.

## 10. Out of Scope

- OCR, resume scoring, search, candidate matching, ranking, recommendations, candidate-facing
  workflows, or manual review.
- Automatic retries of files after they have been moved to `error`.
- Support for resume formats other than PDF.
- Cross-system distributed transactions between MongoDB and Qdrant.
