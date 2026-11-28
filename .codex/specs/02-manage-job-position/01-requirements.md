# Feature Spec: 02-manage-job-position

> **Current scope:** This is the canonical specification for job-offer management. The original
> read-only baseline in `.codex/specs/01-recruiter-application/` is historical and superseded here.

- **Status:** [ ] Draft | [ ] Approved for Design | [ ] In Implementation | [ ] Completed
- **Author/Agent:** @architect
- **Date:** 2026-11-26

## 1. Overview

Allow the recruiter to create, view, edit, and delete a job position. Persist the position in the
system database. The initial version supports one selected job position at a time in the Details
panel.

## 2. Feature Scope

### In Scope

#### Job offer list and details

- [ ] Item 1: On the Open Positions tab, selecting a job offer in the list displays the position
  details in the **Details panel** (right-side panel).
- [ ] Item 2: When a job offer is selected, display an **Edit** button above the Details panel. When
  no job offer is selected, hide the **Edit** button.
- [ ] Item 3: Display a **New Job Offer** button above the Details panel at all times.
- [ ] Item 4: Job offers are not to be displayed as buttons in the list. They are to shown in the
  format: [jobTitle] - [company] - [jobId]
- [ ] Item 18: Assign every newly created job offer a unique sequential Job ID in the format
  **JD-########**, starting at **JD-00000001** when no generated IDs exist.

#### Create and edit form

- [ ] Item 19: Clicking **New Job Offer** opens a modal containing an empty form for a new job offer.
- [ ] Item 5: The form provides Cancel and Save buttons. Clicking Cancel closes the modal without
  saving. A custom confirmation prompt is to be displayed, before discarding unsaved changes.
- [ ] Item 6: All form fields are required. If the recruiter clicks **Save** while any required
  field is empty, do not save the job offer. Highlight each empty field with a red border and show
  an accessible indication that the field is required. Validation errors appear only after clicking
  **Save**.
- [ ] Item 7: Clicking **Edit** opens the same modal with the selected job offer’s existing values
  loaded into the form. The recruiter can change those values and save the changes.
- [ ] Item 8: After a successful save, close the modal.
- [ ] Item 9: After a successful save, update the job offer list to reflect the saved job offer. If
  the saved offer is selected, update the Details panel as well.
- [ ] Item 10: After a successful save, the job offer must be selected in the list.
- [ ] Item 11: A failed save keeps the modal open and shows an error message.
- [ ] Item 12: The Allocation field is required and offers these values: **Remote**, **Hybrid**, and
  **On-site**.
- [ ] Item 13: Provide a required **Company** field for the hiring company's name. In the Details
  panel, display Company above the Job Description section.
- [ ] Item 14: Render plain-text formatting markers in Job Description and Required
  Qualifications in the Details panel: `**text**` for bold, `*text*` for italic, and lines
  beginning `- ` for bullet points. Treat other content, including HTML-looking text, as literal
  text.
- [ ] Item 15: Use the same visual style as **New Job Offer** for all buttons in the job-offer
  feature, including action, form, and discard-confirmation buttons. Set them to a font size of
  `0.75rem` and a height of `35px`. Offer list entries are not buttons and are excluded from this
  styling requirement.

#### Delete

- [ ] Item 16: Display a **Delete** button above the Details panel when a job offer is selected;
  hide it when no job offer is selected.
- [ ] Item 17: The recruiter can delete a saved job offer. After a successful deletion, remove it
  from persisted data and the job-offer list. If the deleted offer was selected, clear the Details
  panel.
- [ ] Item 20: Provide a required numeric **Years of Experience** field in the create/edit form and
  persist its value with the job offer.
- [ ] Item 21: Display `.detail-field > dt` labels at a font size of `14px`.

### Out of Scope

- Nothing to describe now.

## 3. Business Rules

- **BR01 — Single selection:** At most one job offer is selected and displayed in the Details panel
  at a time.
- **BR02 — Required values:** Company, Job Name, Job Description, Required Qualifications, Years of
  Experience, Location, and Allocation are required for a job offer.
- **BR03 — Allocation values:** Allocation must be one of **Remote**, **Hybrid**, or **On-site**.
- **BR04 — Persistence outcomes:** A create, edit, or delete operation changes persisted data and
  the displayed state only when it succeeds. On failure, keep the existing data and display state.
- **BR05 — Discarding edits:** If a form has unsaved changes, the recruiter must confirm before
  discarding them. An unchanged form can be canceled without a discard prompt.
- **BR06 — Button styling:** All buttons in the job-offer feature use the same visual style as
  **New Job Offer**, with a font size of `0.75rem` and a height of `35px`.
- **BR07 — Generated Job ID:** Generate a unique sequential Job ID for each new offer using the
  format `JD-########`. Start at `JD-00000001` if no generated IDs exist and increment the numeric
  suffix for each new offer. The recruiter does not enter or edit this value.
- **BR08 — Years of Experience:** A numeric Years of Experience value is required and is persisted
  with each job offer.
- **BR09 — Position detail formatting:** Job Description and Required Qualifications support only
  the specified bold, italic, and bullet markers. Render generated elements safely without
  interpreting arbitrary HTML or other Markdown syntax.
- **BR10 — Detail label typography:** `.detail-field > dt` labels use a `14px` font size.

## 4. Acceptance Criteria

These scenarios must be validated and verified by the `@reviewer`.

### AC01 — Selecting a job offer

- **Given** the recruiter is viewing the job offer list
- **When** they select a job offer
- **Then** that offer is the only selected offer and its details appear in the right-side **Details
  **
  panel.
- **And** any previously selected offer is no longer selected.

### AC02 — Edit button visibility

- **Given** no job offer is selected
- **Then** the **Edit** button is hidden.
- **When** the recruiter selects a job offer
- **Then** the **Edit** button is displayed above the Details panel.

### AC03 — New Job Offer button availability

- **Given** no job offer is selected or a job offer is selected
- **Then** the **New Job Offer** button is displayed above the Details panel.

### AC04 — Opening a new job-offer form

- **Given** the recruiter is viewing the Open Positions tab
- **When** they click **New Job Offer**
- **Then** a modal opens with an empty form containing Company, Job Name, Job Description,
  Required Qualifications, Years of Experience, Location, and Allocation fields, plus **Cancel**
  and **Save** buttons.

### AC05 — Required-field validation

- **Given** the new or edit form has one or more required fields empty
- **When** the recruiter clicks **Save**
- **Then** no save is performed, the modal remains open, each empty field has a red border and an
  accessible indication that it is required, and fields with values are not marked as empty.
- **And** required-field validation errors are not displayed before the recruiter attempts to save.
- **And** Company, Job Name, Job Description, Required Qualifications, Years of Experience,
  Location, and Allocation are all required.

### AC06 — Allocation choices

- **Given** the recruiter is creating or editing a job offer
- **When** they open the Allocation field
- **Then** the available values are **Remote**, **Hybrid**, and **On-site**.
- **And** the recruiter must choose one value before saving.

### AC07 — Editing an existing job offer

- **Given** a job offer is selected
- **When** the recruiter clicks **Edit**
- **Then** the same form modal opens with the selected job offer’s current Company, Job Name, Job
  Description, Required Qualifications, Years of Experience, Location, and Allocation values
  loaded.

### AC08 — Successfully creating a job offer

- **Given** the recruiter has entered valid values for every required field in a new job-offer form
- **When** they click **Save** and persistence succeeds
- **Then** the new job offer is stored and the modal closes.
- **And** the job-offer list reflects the new job offer and selects it.
- **And** the Details panel displays the newly created job offer.

### AC09 — Successfully editing a job offer

- **Given** the recruiter has changed one or more values in the selected job offer’s edit form and
  every required field is valid
- **When** they click **Save** and persistence succeeds
- **Then** the existing job offer is updated in storage and the modal closes.
- **And** the job-offer list reflects the updated values and keeps that offer selected.
- **And** the Details panel displays the updated values.

### AC10 — Canceling a form

- **Given** the form has no unsaved changes
- **When** the recruiter clicks **Cancel**
- **Then** the modal closes without saving.
- **Given** the form has unsaved changes
- **When** the recruiter clicks **Cancel**
- **Then** a custom confirmation prompt asks whether to discard the changes.
- **When** the recruiter chooses to discard
- **Then** the modal closes and no changes are saved.
- **When** the recruiter chooses to keep editing
- **Then** the prompt closes, the modal remains open, and the entered values remain available.

### AC11 — Failed save

- **Given** the recruiter submits a valid new or edited job offer
- **When** persistence fails
- **Then** the modal remains open, the entered values remain available, and an error message is
  shown.
- **And** the job-offer list and Details panel do not show the failed changes as saved.

### AC12 — Delete button visibility

- **Given** no job offer is selected
- **Then** the **Delete** button is hidden.
- **When** the recruiter selects a job offer
- **Then** the **Delete** button is displayed above the Details panel.

### AC13 — Deleting a job offer

- **Given** a saved job offer is selected
- **When** the recruiter deletes that job offer and persistence succeeds
- **Then** it is removed from persisted data and from the job-offer list.
- **And** the Details panel is cleared.

### AC14 — Failed deletion

- **Given** a saved job offer is selected
- **When** the recruiter attempts to delete it and persistence fails
- **Then** the job offer remains in the list and in the Details panel, and an error message is
  shown.

### AC15 — Company field and details display

- **Given** the recruiter is creating or editing a job offer
- **Then** the form includes a required Company field for the hiring company's name.
- **When** a job offer is displayed in the Details panel
- **Then** its Company value appears above the Job Description section.

### AC16 — Plain-text formatting markers in job details

- **Given** a job offer has `**bold**`, `*italic*`, or `- ` bullet markers in its Job Description
  or Required Qualifications
- **When** the offer is displayed in the Details panel
- **Then** those fields display the requested bold, italic, or bullet formatting.
- **And** HTML-looking content and unsupported formatting syntax display as literal text.

### AC17 — Job-offer button sizing

- **Given** a job-offer management button is displayed
- **Then** its visual style matches **New Job Offer**, its font size is `0.75rem`, and its height
  is `35px`.

### AC18 — Sequential Job ID generation

- **Given** the recruiter submits a valid new job-offer form
- **When** the offer is successfully created
- **Then** the system assigns it a unique Job ID in the format `JD-########`, beginning with
  `JD-00000001` when no generated IDs exist and incrementing the numeric suffix for each new offer.
- **And** the Job ID is displayed in that offer's list entry and is not an editable form field.

### AC19 — Offer list entry format and semantics

- **Given** one or more job offers are available
- **When** the Open Positions list is displayed
- **Then** each offer appears as a selectable non-button list entry with the text
  `[jobTitle] - [company] - [jobId]`.

### AC20 — Required Years of Experience field

- **Given** the recruiter creates or edits a job offer
- **Then** the form provides a required numeric Years of Experience field.
- **When** a valid offer is saved
- **Then** the entered Years of Experience value is persisted with the offer and returned when the
  offer is loaded for editing.
- **And** the Years of Experience value is displayed in the offer's Details panel, preserving the
  SPA Skeleton's existing detail-view requirement.

### AC21 — Detail label font size

- **Given** job-position or candidate detail fields are displayed
- **When** a `.detail-field` label is rendered as a direct `dt` child
- **Then** the label uses a font size of `14px`.

