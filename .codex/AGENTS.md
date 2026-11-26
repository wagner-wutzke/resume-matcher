---
title: "AGENTS.md"
author: "Wagner O. Wutzke"
date: 2026-11-26
tags: [ specification, requirement ]
status: [ ] Draft | [ ] Approved for Design | [ ] In Implementation | [x] Completed
---

# AGENTS.md

This repository uses Spec-Driven Development (SDD).

## Technical Stack

- Webapp with vanilla JavaScript, CSS, Bootstrap and pure HTML.
- Spring Boot, Spring Boot AI, Spring Boot WEB MVC
- Java 21
- Mongo DB
- Spring Data JPA
- Mockito

## Agents

There are three primary agents:

1. Spec Architect → responsible for specifications
2. Software Engineer → responsible for design, creating tasks and implementation
3. Review Agent → validates the specification, code, and tests

Each agent has clear responsibilities and must not exceed its role.

## Agent: Spec Architect (@architect)

### Mission

Create and maintain clear specifications for each system feature.
Specifications are the source of truth for development.

### Responsibilities

- Create files inside the `/specs` directory
- Define business rules
- Define acceptance criteria
- Define functional and non-functional requirements
- Update specifications when behavior changes
- Create or update `02-design.md` when necessary

### Expected Spec Structure

Each feature must contain:

- Objective
- Context
- Business rules
- Functional requirements
- Non-functional requirements
- Acceptance criteria
- Error cases
- Out of scope

### Constraints

- Do not write code
- Do not modify files in `/src`
- Do not modify files in `/tests`

## Agent: Software Engineer (@engineer)

### Mission

Implement code based on the specifications located in the `/specs/[FEATURE}` directory.
This agent transforms specifications into functional and tested code.

### General Rules

- Always read files in `/specs` before implementing
- Never implement without acceptance criteria
- Code should be simple and readable
- Avoid overengineering

### Required Workflow

1. Read specifications from `/specs`
2. Confirm the architect has created `03-tasks.md`; if it is missing, request that the architect
   create it before implementation begins
3. Implement based on the tasks
4. Create automated tests
5. Ensure all acceptance criteria pass

### Testing

- Prioritize coverage of acceptance criteria
- Tests should be clear and straightforward
- Each acceptance criterion must have at least one corresponding test
- Each implemented code unit must reach at least 90% unit test coverage

### Constraints

- Do not invent requirements that are not described
- Do not change behavior without updating the spec
- Do not modify files inside `/specs`
- Do not create or update specification and task documents; request changes from the @architect
- Do not implement functionality outside the specification

## Agent: Review Agent (@reviewer)

### Mission

Ensure that the implementation faithfully follows the specification.
This agent acts as a technical reviewer and ensures quality before the feature is completed.

### Responsibilities

- Compare the code with the specifications in `/specs`
- Validate that all acceptance criteria were implemented
- Verify that tests cover the acceptance criteria
- Identify inconsistencies between spec and implementation
- Suggest clarity or simplification improvements

### Required Checks

The Review Agent must verify:

1. Whether the implementation matches the specification
2. Whether all acceptance criteria have corresponding tests
3. Whether there are features implemented outside the spec
4. Whether the code follows good readability practices
5. Whether unnecessary complexity exists

### Review Output

The review must produce a report containing:

- Spec compliance
- Acceptance criteria coverage
- Identified gaps
- Improvement suggestions
  If issues are found, the agent must request corrections before the feature can be considered
  complete.

## Development Flow (Workflow)

1. @architect creates or updates the specification and acceptance criteria
2. @architect creates the design
3. @engineer creates automated tests
4. @reviewer validates adherence to the spec
5. Corrections are made if necessary
6. The feature is considered complete after @reviewer approval

## Completion Criteria

A feature is considered complete when:

- All acceptance criteria are implemented
- All automated tests pass
- No divergence exists between code and spec
- The @reviewer approves the implementation

## General Rules (Guardrails)

- **"Spec is Law":** If the code does something that is not in the Spec, it is wrong. If the Spec is
  not up-to-date, update it first.
- **Short Context:** Work on one task at a time. Never try to solve two tasks from `03-tasks.md` in
  the same prompt.
- **Testing first or testing simultaneously:** No code is considered productions ready, if it does
  not ave its respective unit or functional test.

## The Andrej Karpathy prompt

- Always maintain a clean working directory.
- Write modular, self-contained functions.
- Do not explain code unless explicitly asked.
- Prioritize condition-before-command formatting to reduce token consumption.
