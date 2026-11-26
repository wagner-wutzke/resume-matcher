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
  document.querySelector("#job-location"),
  document.querySelector("#job-allocation"),
];
const fieldErrors = [
  document.querySelector("#job-company-error"),
  document.querySelector("#job-name-error"),
  document.querySelector("#job-description-error"),
  document.querySelector("#required-qualifications-error"),
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
  ["Location", "location"],
  ["Allocation", "allocation"],
];

function setText(element, value) {
  element.textContent = value == null ? "" : String(value);
}

function positionLabel(position) {
  return position.jobName ?? "";
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

function findPositionButton(positionId) {
  const buttons = positionList.querySelectorAll(".position-button");
  for (let index = 0; index < buttons.length; index += 1) {
    if (buttons[index].dataset.jobOfferId === String(positionId)) return buttons[index];
  }
  return null;
}

function setSelectionActions(hasSelection) {
  editButton.hidden = !hasSelection;
  deleteButton.hidden = !hasSelection;
}

function showPosition(position, selectedButton) {
  selectedPosition = position;
  setSelectionActions(true);
  positionList.querySelectorAll(".position-button").forEach((button) => {
    const selected = button === selectedButton;
    button.classList.toggle("selected", selected);
    button.setAttribute("aria-pressed", String(selected));
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
    const button = document.createElement("button");
    button.className = "position-button job-offer-button";
    button.type = "button";
    button.dataset.jobOfferId = String(position.id ?? "");
    button.textContent = String(positionLabel(position));
    button.setAttribute("aria-pressed", "false");
    button.addEventListener("click", () => showPosition(position, button));
    positionList.append(button);
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
    field.value = selectedPosition[field.name] == null ? "" : String(selectedPosition[field.name]);
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
    const unsupportedAllocation = field.name === "allocation"
      && !blank
      && !["Remote", "Hybrid", "On-site"].includes(value);
    const errorMessage = blank ? "Required." : unsupportedAllocation ? "Choose a supported allocation." : "";
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
  return Object.fromEntries(offerFields.map((field) => [field.name, field.value]));
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
  const buttons = positionList.querySelectorAll(".position-button");
  const createdButton = buttons[buttons.length - 1];
  showPosition(savedPosition, createdButton);
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
  const updatedButton = findPositionButton(updatedPosition.id);
  showPosition(updatedPosition, updatedButton);
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
