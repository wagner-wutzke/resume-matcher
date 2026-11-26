---
title: "prompts.md"
author: "Wagner O. Wutzke"
date: 2026-11-26
tags: [ specification, requirement ]
status: [ ] Draft | [ ] Approved for Design | [ ] In Implementation | [x] Completed
---
# Prompt Playbook: [Short Feature Name]

Copy-pasteable prompts for the repository's SDD phases. Read `.codex/AGENTS.md` before using a
prompt. Replace `[FEATURE_NAME]` with the feature directory name (for example,
`01-spa-skeleton`). Feature documents live under `.codex/specs/[FEATURE_NAME]/`.

---

## Phase 1: Requirements Discovery (@architect)

Use this prompt when starting a feature from an initial idea.

```text
Act as @architect defined in `.codex/AGENTS.md`.

We are starting a feature: [INSERT BRIEF FEATURE IDEA]. Do not generate project code.
Review the repository context and create `.codex/specs/[FEATURE_NAME]/01-requirements.md` using
the expected structure in `.codex/AGENTS.md`.

Ask concise clarifying questions one at a time when important requirements cannot be inferred.
Capture the objective, context, scope, business rules, functional and non-functional requirements,
acceptance criteria, error cases, and out-of-scope items. Do not add behavior that has not been
requested. Summarize any assumptions that remain.
```

---

## Phase 2: Technical Design (@architect)

Use this prompt after the requirements are approved.

```text
Act as @architect defined in `.codex/AGENTS.md`.

Read `.codex/specs/[FEATURE_NAME]/01-requirements.md` and inspect the existing codebase patterns.
Create or update `.codex/specs/[FEATURE_NAME]/02-design.md` so each design decision traces to an
approved requirement.

Describe the architecture, data model and persistence changes if required, API contracts if
required, UI and logic flow, relevant error handling, security considerations required by the
specification, and verification mapping to every acceptance criterion. Do not add generic features
such as hashing, rate limiting, or schema migrations unless the requirements call for them. If a
needed behavior is unspecified, ask the user instead of inventing it.

Write the design file, then summarize the main decisions and any unresolved requirement gaps.
```

---

## Phase 3: Task Breakdown (@architect)

Prepare the task file before implementation. The engineer workflow can then follow its required
task file without modifying other specification documents.

```text
Act as @architect defined in `.codex/AGENTS.md`.

Read `.codex/specs/[FEATURE_NAME]/01-requirements.md` and
`.codex/specs/[FEATURE_NAME]/02-design.md`. Create or update
`.codex/specs/[FEATURE_NAME]/03-tasks.md` with small, sequential implementation tasks.

Derive phases from the approved design; do not force database, service, API, or UI phases when they
do not apply. Keep each task limited to one coherent change and include the relevant acceptance
criteria plus its focused test or verification. Include integration and review work where needed.
Do not write project code. Summarize the task order and dependencies after saving the file.
```

---

## Phase 4: Atomic Implementation (@engineer)

Use this prompt once per task. Do not combine separate tasks in one execution.

```text
Act as @engineer defined in `.codex/AGENTS.md`.

Work only on Task [X.X] — "[INSERT TASK NAME]" in
`.codex/specs/[FEATURE_NAME]/03-tasks.md`. First read that task and the related requirements and
design in `.codex/specs/[FEATURE_NAME]/01-requirements.md` and
`.codex/specs/[FEATURE_NAME]/02-design.md`.

Implement only this task. Add or update focused automated tests for its acceptance criteria. Do not
implement later tasks, invent requirements, or modify specification files. Run the relevant tests
and report changed files, test results, and any blockers. The task status is updated by the reviewer
after verification/review.
```

---

## Phase 5: Code Review (@reviewer)

Use this prompt after implementation and test results are available.

```text
Act as @reviewer defined in `.codex/AGENTS.md`.

Review the implementation for [FEATURE_NAME] against
`.codex/specs/[FEATURE_NAME]/01-requirements.md`,
`.codex/specs/[FEATURE_NAME]/02-design.md`, and
`.codex/specs/[FEATURE_NAME]/03-tasks.md`. Inspect the relevant code and tests, and verify reported
test results where possible.

Return a report with these sections:
- Spec compliance
- Acceptance criteria coverage, naming evidence/tests for each criterion
- Identified gaps or out-of-scope behavior
- Readability and complexity observations
- Improvement suggestions
- Review outcome: APPROVED only if all criteria are met and no unresolved gaps remain; otherwise
  list the required corrections.

Do not modify implementation files, requirements, or design files during review. After verification,
update only the task-status checkboxes in `.codex/specs/[FEATURE_NAME]/03-tasks.md`.
```
