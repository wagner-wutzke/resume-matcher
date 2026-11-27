const positionList = document.querySelector("#position-list");
const positionDetails = document.querySelector("#position-details");
const editButton = document.querySelector("#edit-job-offer");
const deleteButton = document.querySelector("#delete-job-offer");
const deleteError = document.querySelector("#job-offer-delete-error");
const newButton = document.querySelector("#new-job-offer");
const modalElement = document.querySelector("#job-offer-modal");
const offerForm = document.querySelector("#job-offer-form");
const modalTitle = document.querySelector("#job-offer-modal-title");
const formError = document.querySelector("#job-offer-save-error");
const discardPrompt = document.querySelector("#discard-confirmation");
const offerFields = [
  document.querySelector("#job-company"),
  document.querySelector("#job-name"),
  document.querySelector("#job-description"),
  document.querySelector("#required-qualifications"),
  document.querySelector("#years-of-experience"),
  document.querySelector("#job-location"),
  document.querySelector("#job-allocation"),
];
const fieldErrors = [
  document.querySelector("#job-company-error"),
  document.querySelector("#job-name-error"),
  document.querySelector("#job-description-error"),
  document.querySelector("#required-qualifications-error"),
  document.querySelector("#years-of-experience-error"),
  document.querySelector("#job-location-error"),
  document.querySelector("#job-allocation-error"),
];
const cancelButton = document.querySelector("#cancel-job-offer");
const closeButton = document.querySelector("#close-job-offer-modal");
const continueEditingButton = document.querySelector("#continue-editing");
const discardChangesButton = document.querySelector("#discard-changes");
let selectedPosition = null;
let positions = [];
let formMode = "create";
let originalFormValues = [];

const detailFields = [
  ["Company", "company"],
  ["Job description", "jobDescription", true],
  ["Required qualifications", "requiredQualifications", true],
  ["Years of experience", "yearsOfExperience"],
  ["Location", "location"],
  ["Allocation", "allocation"],
];

function setText(element, value) {
  element.textContent = value == null ? "" : String(value);
}

function positionLabel(position) {
  return position.jobTitle ?? position.jobName ?? "";
}

function positionListLabel(position) {
  return `${positionLabel(position)} - ${position.company ?? ""} - ${position.jobId ?? ""}`;
}

function renderMarkdown(element, value) {
  const markdown = value == null ? "" : String(value);
  if (typeof marked === "undefined" || typeof DOMPurify === "undefined") {
    setText(element, markdown);
    return;
  }
  const generatedMarkup = marked.parse(markdown, { html: false });
  element.innerHTML = DOMPurify.sanitize(generatedMarkup);
}

function findPositionEntry(positionId) {
  const entries = positionList.querySelectorAll(".position-entry");
  for (let index = 0; index < entries.length; index += 1) {
    if (entries[index].dataset.jobOfferId === String(positionId)) return entries[index];
  }
  return null;
}

function setSelectionActions(hasSelection) {
  editButton.hidden = !hasSelection;
  deleteButton.hidden = !hasSelection;
}

function showPosition(position, selectedEntry) {
  selectedPosition = position;
  setSelectionActions(true);
  positionList.querySelectorAll(".position-entry").forEach((entry) => {
    const selected = entry === selectedEntry;
    entry.classList.toggle("selected", selected);
    entry.setAttribute("aria-selected", String(selected));
  });

  positionDetails.replaceChildren();

  const title = document.createElement("h2");
  title.className = "detail-title";
  setText(title, positionLabel(position));
  positionDetails.append(title);

  const details = document.createElement("dl");
  detailFields.forEach(([label, property, isMarkdown]) => {
    const field = document.createElement("div");
    field.className = "detail-field";

    const term = document.createElement("dt");
    term.textContent = label;
    const description = document.createElement("dd");
    if (isMarkdown) renderMarkdown(description, position[property]);
    else setText(description, position[property]);

    field.append(term, description);
    details.append(field);
  });
  positionDetails.append(details);
}

function renderPositions(nextPositions) {
  setSelectionActions(false);
  positions = nextPositions;
  positionList.replaceChildren();
  positions.forEach((position) => {
    const entry = document.createElement("div");
    entry.className = "position-entry";
    entry.setAttribute("role", "option");
    entry.setAttribute("aria-selected", "false");
    entry.tabIndex = 0;
    entry.dataset.jobOfferId = String(position.id ?? "");
    setText(entry, positionListLabel(position));
    entry.addEventListener("click", () => showPosition(position, entry));
    entry.addEventListener("keydown", (event) => {
      if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        showPosition(position, entry);
      }
      if (event.key === "ArrowDown" || event.key === "ArrowUp") {
        event.preventDefault();
        const entries = [...positionList.querySelectorAll(".position-entry")];
        const offset = event.key === "ArrowDown" ? 1 : -1;
        entries[(entries.indexOf(entry) + offset + entries.length) % entries.length]?.focus();
      }
    });
    positionList.append(entry);
  });
}

function openNewOfferForm() {
  formMode = "create";
  offerFields.forEach((field) => { field.value = ""; });
  clearValidation();
  clearFormError();
  clearDiscardPrompt();
  originalFormValues = getFormValues();
  modalTitle.textContent = "New Job Offer";
  bootstrap.Modal.getOrCreateInstance(modalElement).show();
}

function openEditOfferForm() {
  if (!selectedPosition) return;
  formMode = "edit";
  modalTitle.textContent = "Edit Job Offer";
  offerFields.forEach((field) => {
    const value = field.name === "jobName"
      ? positionLabel(selectedPosition)
      : selectedPosition[field.name];
    field.value = value == null ? "" : String(value);
  });
  clearValidation();
  clearFormError();
  clearDiscardPrompt();
  originalFormValues = getFormValues();
  bootstrap.Modal.getOrCreateInstance(modalElement).show();
}

function clearValidation() {
  offerFields.forEach((field, index) => {
    field.classList.remove("is-invalid");
    field.removeAttribute("aria-invalid");
    fieldErrors[index].hidden = true;
    fieldErrors[index].textContent = "Required.";
  });
}

function validateOfferForm() {
  let valid = true;
  offerFields.forEach((field, index) => {
    const value = field.value == null ? "" : String(field.value);
    const blank = value.trim() === "";
    const invalidNumber = field.name === "yearsOfExperience" && value !== ""
      && !Number.isInteger(Number(value));
    const unsupportedAllocation = field.name === "allocation"
      && !blank
      && !["Remote", "Hybrid", "On-site"].includes(value);
    const errorMessage = blank ? "Required." : invalidNumber ? "Enter a whole number."
      : unsupportedAllocation ? "Choose a supported allocation." : "";
    field.classList.toggle("is-invalid", Boolean(errorMessage));
    if (errorMessage) {
      valid = false;
      field.setAttribute("aria-invalid", "true");
      fieldErrors[index].textContent = errorMessage;
      fieldErrors[index].hidden = false;
    } else {
      field.removeAttribute("aria-invalid");
      fieldErrors[index].hidden = true;
    }
  });
  return valid;
}

function offerFormValues() {
  return Object.fromEntries(offerFields.map((field) => [
    field.name,
    field.name === "yearsOfExperience" && field.value !== ""
      ? Number(field.value)
      : field.value,
  ]));
}

async function saveNewOffer(event) {
  event.preventDefault();
  if (!validateOfferForm()) return;

  const response = await fetch("/api/job-offers", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(offerFormValues()),
  });
  if (!response.ok) throw new Error(await responseErrorMessage(response));

  const savedPosition = await response.json();
  positions = [...positions, savedPosition];
  renderPositions(positions);
  const entries = positionList.querySelectorAll(".position-entry");
  const createdEntry = entries[entries.length - 1];
  showPosition(savedPosition, createdEntry);
  bootstrap.Modal.getOrCreateInstance(modalElement).hide();
}

async function saveEditedOffer(event) {
  event.preventDefault();
  if (!validateOfferForm() || !selectedPosition) return;

  const response = await fetch(`/api/job-offers/${encodeURIComponent(selectedPosition.id)}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(offerFormValues()),
  });
  if (!response.ok) throw new Error(await responseErrorMessage(response));

  const updatedPosition = await response.json();
  positions = positions.map((position) => position.id === updatedPosition.id ? updatedPosition : position);
  renderPositions(positions);
  const updatedEntry = findPositionEntry(updatedPosition.id);
  showPosition(updatedPosition, updatedEntry);
  bootstrap.Modal.getOrCreateInstance(modalElement).hide();
}

async function deleteSelectedOffer() {
  if (!selectedPosition) return;
  const response = await fetch(`/api/job-offers/${encodeURIComponent(selectedPosition.id)}`, {
    method: "DELETE",
  });
  if (!response.ok) throw new Error(await responseErrorMessage(response));

  positions = positions.filter((position) => position.id !== selectedPosition.id);
  selectedPosition = null;
  renderPositions(positions);
  positionDetails.replaceChildren();
  deleteError.hidden = true;
  deleteError.textContent = "";
}

function saveOffer(event) {
  const save = formMode === "edit" ? saveEditedOffer : saveNewOffer;
  return save(event).catch((error) => showFormError(error.message));
}

async function responseErrorMessage(response) {
  try {
    const error = await response.json();
    if (error && typeof error.message === "string") return error.message;
  } catch {
    return "The job offer could not be saved.";
  }
  return "The job offer could not be saved.";
}

function clearFormError() {
  formError.textContent = "";
  formError.hidden = true;
}

function getFormValues() {
  return offerFields.map((field) => field.value == null ? "" : String(field.value));
}

function requestCloseForm() {
  const changed = getFormValues().some((value, index) => value !== originalFormValues[index]);
  if (changed) {
    discardPrompt.hidden = false;
    return;
  }
  bootstrap.Modal.getOrCreateInstance(modalElement).hide();
}

function clearDiscardPrompt() {
  discardPrompt.hidden = true;
}

function showFormError(message) {
  formError.textContent = message || "The job offer could not be saved.";
  formError.hidden = false;
}

newButton.addEventListener("click", openNewOfferForm);
editButton.addEventListener("click", openEditOfferForm);
deleteButton.addEventListener("click", () => {
  return deleteSelectedOffer().catch((error) => {
    deleteError.textContent = error.message || "The job offer could not be deleted.";
    deleteError.hidden = false;
  });
});
cancelButton.addEventListener("click", requestCloseForm);
closeButton.addEventListener("click", requestCloseForm);
continueEditingButton.addEventListener("click", clearDiscardPrompt);
discardChangesButton.addEventListener("click", () => {
  clearDiscardPrompt();
  bootstrap.Modal.getOrCreateInstance(modalElement).hide();
});
offerForm.addEventListener("submit", saveOffer);

async function loadPositions() {
  try {
    const response = await fetch("/api/job-offers");
    if (!response.ok) throw new Error("Job-offer request failed");
    const positions = await response.json();
    renderPositions(Array.isArray(positions) ? positions : []);
  } catch {
    positionList.textContent = "Unable to load open positions";
    positionList.classList.add("load-error");
  }
}

loadPositions();
