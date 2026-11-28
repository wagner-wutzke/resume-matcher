const positionList = document.querySelector("#position-list");
const positionDetails = document.querySelector("#position-details");
const candidateMatches = document.querySelector("#candidate-matches");
const findMatchesButton = document.querySelector("#find-matches");
const matchStatus = document.querySelector("#match-status");
const matchEmpty = document.querySelector("#match-empty");
const matchError = document.querySelector("#match-error");
const retryMatchesButton = document.querySelector("#retry-matches");
const matchResultsList = document.querySelector("#match-results");
const matchDetailsPanel = document.querySelector("#match-details");
const backToMatchResultsButton = document.querySelector("#back-to-match-results");
const matchDetailsContent = document.querySelector("#match-details-content");
const matchDetailsStatus = document.querySelector("#match-details-status");
const matchDetailsError = document.querySelector("#match-details-error");
const retryMatchDetailsButton = document.querySelector("#retry-match-details");
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
const candidateList = document.querySelector("#candidate-list");
const candidateListStatus = document.querySelector("#candidate-list-status");
const candidateListError = document.querySelector("#candidate-list-error");
const retryCandidateList = document.querySelector("#retry-candidate-list");
const candidatePagination = document.querySelector("#candidate-pagination");
const candidatePreviousPage = document.querySelector("#candidate-previous-page");
const candidateNextPage = document.querySelector("#candidate-next-page");
const candidatePageIndicator = document.querySelector("#candidate-page-indicator");
const candidateDetailsContent = document.querySelector("#candidate-details-content");
const candidateDetailsPanel = document.querySelector("#candidate-details");
const candidateDetailsStatus = document.querySelector("#candidate-details-status");
const candidateDetailsError = document.querySelector("#candidate-details-error");
const retryCandidateDetails = document.querySelector("#retry-candidate-details");
const candidatesTab = document.querySelector("#candidates-tab");
let selectedPosition = null;
let positions = [];
let formMode = "create";
let originalFormValues = [];
let candidatePage = 0;
let candidateTotalPages = 0;
let selectedCandidateId = null;
let matchRequestGeneration = 0;
let matchRequestInFlight = false;
let currentMatchResults = [];
let matchPositionId = null;
let matchDetailsRequestGeneration = 0;
let selectedMatch = null;
const CANDIDATE_PAGE_SIZE = 15;
const EMPTY_VALUE = "—";

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

function appendFormattedInline(container, text) {
  const markerPattern = /(\*\*[^*\n]+\*\*|\*[^*\n]+\*)/g;
  let start = 0;
  for (const match of text.matchAll(markerPattern)) {
    const offset = match.index;
    if (offset > start) {
      const plainText = document.createElement("span");
      setText(plainText, text.slice(start, offset));
      container.append(plainText);
    }
    const isBold = match[0].startsWith("**");
    const formattedText = document.createElement(isBold ? "strong" : "em");
    setText(formattedText, match[0].slice(isBold ? 2 : 1, isBold ? -2 : -1));
    container.append(formattedText);
    start = offset + match[0].length;
  }
  if (start < text.length) {
    const plainText = document.createElement("span");
    setText(plainText, text.slice(start));
    container.append(plainText);
  }
}

function renderFormattedPositionText(element, value) {
  const lines = value == null ? [] : String(value).split(/\r?\n/);
  let index = 0;
  while (index < lines.length) {
    if (/^\s*-\s+/.test(lines[index])) {
      const list = document.createElement("ul");
      while (index < lines.length && /^\s*-\s+/.test(lines[index])) {
        const item = document.createElement("li");
        appendFormattedInline(item, lines[index].replace(/^\s*-\s+/, ""));
        list.append(item);
        index += 1;
      }
      element.append(list);
      continue;
    }
    if (lines[index] === "") {
      element.append(document.createElement("br"));
      index += 1;
      continue;
    }
    const paragraph = document.createElement("p");
    appendFormattedInline(paragraph, lines[index]);
    element.append(paragraph);
    index += 1;
  }
}

function candidateValue(value) {
  const text = value == null ? "" : String(value).trim();
  return text || EMPTY_VALUE;
}

function clearCandidateDetails() {
  selectedCandidateId = null;
  candidateDetailsPanel.setAttribute("aria-busy", "false");
  candidateDetailsContent.replaceChildren();
  candidateDetailsStatus.textContent = "";
  candidateDetailsError.textContent = "";
  candidateDetailsError.hidden = true;
  retryCandidateDetails.hidden = true;
}

function renderCandidateSummaries(items) {
  candidateList.replaceChildren();
  items.slice(0, CANDIDATE_PAGE_SIZE).forEach((candidate) => {
    const item = document.createElement("div");
    item.setAttribute("role", "listitem");
    const entry = document.createElement("button");
    entry.type = "button";
    entry.className = "position-entry candidate-entry";
    entry.setAttribute("aria-pressed", "false");
    entry.dataset.candidateId = String(candidate.id ?? "");
    setText(
      entry,
      `${candidateValue(candidate.fullname)} - ${candidateValue(candidate.position)}`,
    );
    entry.addEventListener("click", () => selectCandidate(candidate, entry));
    item.append(entry);
    candidateList.append(item);
  });
}

function renderCandidatePagination() {
  const hasPages = candidateTotalPages > 0;
  candidatePagination.hidden = !hasPages;
  candidatePageIndicator.textContent = hasPages
    ? `Page ${candidatePage + 1} of ${candidateTotalPages}`
    : "";
  candidatePreviousPage.disabled = candidatePage <= 0;
  candidateNextPage.disabled = candidatePage >= candidateTotalPages - 1;
}

async function loadCandidatePage(page = candidatePage) {
  candidateList.setAttribute("aria-busy", "true");
  candidateListStatus.textContent = "Loading candidates…";
  candidateListError.hidden = true;
  retryCandidateList.hidden = true;
  candidateList.replaceChildren();
  clearCandidateDetails();

  try {
    const response = await fetch(`/api/candidates?page=${page}`);
    let result = null;
    try {
      result = await response.json();
    } catch {
      result = null;
    }
    if (!response.ok) {
      if (result?.code === "CANDIDATE_PAGE_OUT_OF_RANGE" && page !== 0) {
        candidatePage = 0;
        return await loadCandidatePage(0);
      }
      throw new Error("Candidate list unavailable");
    }
    if (!result || !Array.isArray(result.items)) throw new Error("Candidate list unavailable");

    candidatePage = Number.isInteger(result.page) ? result.page : page;
    candidateTotalPages = Number.isInteger(result.totalPages) ? result.totalPages : 0;
    renderCandidateSummaries(result.items);
    renderCandidatePagination();
    candidateListStatus.textContent = result.items.length === 0 ? "No candidates available." : "";
  } catch {
    candidateList.replaceChildren();
    candidatePagination.hidden = true;
    candidatePageIndicator.textContent = "";
    candidateListStatus.textContent = "";
    candidateListError.textContent = "Unable to load candidates.";
    candidateListError.hidden = false;
    retryCandidateList.hidden = false;
  } finally {
    candidateList.setAttribute("aria-busy", "false");
  }
}

function selectCandidate(candidate, selectedEntry) {
  selectedCandidateId = String(candidate.id ?? "");
  candidateList.querySelectorAll(".candidate-entry").forEach((entry) => {
    const selected = entry === selectedEntry;
    entry.classList.toggle("selected", selected);
    entry.setAttribute("aria-pressed", String(selected));
  });
  candidateDetailsContent.replaceChildren();
  return loadCandidateDetails(selectedCandidateId);
}

function appendCandidateField(list, label, value) {
  const field = document.createElement("div");
  field.className = "detail-field";
  const term = document.createElement("dt");
  setText(term, label);
  const description = document.createElement("dd");
  setText(description, candidateValue(value));
  field.append(term, description);
  list.append(field);
}

function renderCandidateDetails(candidate) {
  candidateDetailsContent.replaceChildren();
  const title = document.createElement("h2");
  title.className = "detail-title";
  setText(title, candidateValue(candidate.fullname));
  candidateDetailsContent.append(title);

  const fields = document.createElement("dl");
  [
    ["Email", candidate.email],
    ["Full name", candidate.fullname],
    ["Country", candidate.country],
    ["City", candidate.city],
    ["Position", candidate.position],
    ["Skills", candidate.skills],
    ["Allocation", candidate.allocation],
    ["Years of experience", candidate.yearsOfExperience],
  ].forEach(([label, value]) => appendCandidateField(fields, label, value));
  candidateDetailsContent.append(fields);

  const experienceHeading = document.createElement("h3");
  experienceHeading.className = "h5 mt-4";
  setText(experienceHeading, "Work experience");
  candidateDetailsContent.append(experienceHeading);
  const experiences = Array.isArray(candidate.experiences) ? candidate.experiences : [];
  if (experiences.length === 0) {
    const emptyMessage = document.createElement("p");
    setText(emptyMessage, "No reported experience.");
    candidateDetailsContent.append(emptyMessage);
    return;
  }

  const experienceList = document.createElement("ul");
  experienceList.className = "candidate-experiences";
  experiences.forEach((experience) => {
    const item = document.createElement("li");
    item.className = "candidate-experience";
    const experienceTitle = document.createElement("h4");
    experienceTitle.className = "h6";
    setText(experienceTitle, candidateValue(experience.title));
    const experienceFields = document.createElement("dl");
    [
      ["Company", experience.company],
      ["Period", experience.period],
      ["Responsibilities", experience.responsibilities],
      ["Achievements", experience.achievements],
    ].forEach(([label, value]) => appendCandidateField(experienceFields, label, value));
    item.append(experienceTitle, experienceFields);
    experienceList.append(item);
  });
  candidateDetailsContent.append(experienceList);
}

function clearCandidateSelection() {
  selectedCandidateId = null;
  candidateList.querySelectorAll(".candidate-entry").forEach((entry) => {
    entry.classList.toggle("selected", false);
    entry.setAttribute("aria-pressed", "false");
  });
  candidateDetailsContent.replaceChildren();
  candidateDetailsStatus.textContent = "";
  candidateDetailsError.textContent = "";
  candidateDetailsError.hidden = true;
  retryCandidateDetails.hidden = true;
}

async function loadCandidateDetails(candidateId) {
  candidateDetailsPanel.setAttribute("aria-busy", "true");
  candidateDetailsStatus.textContent = "Loading candidate details…";
  candidateDetailsError.hidden = true;
  retryCandidateDetails.hidden = true;
  candidateDetailsContent.replaceChildren();

  try {
    const response = await fetch(`/api/candidates/${encodeURIComponent(candidateId)}`);
    let candidate = null;
    try {
      candidate = await response.json();
    } catch {
      candidate = null;
    }
    if (response.status === 404 || candidate?.code === "CANDIDATE_NOT_FOUND") {
      if (selectedCandidateId === candidateId) {
        clearCandidateSelection();
        await loadCandidatePage(candidatePage);
      }
      return;
    }
    if (!response.ok || !candidate) throw new Error("Candidate details unavailable");
    if (selectedCandidateId !== candidateId) return;

    renderCandidateDetails(candidate);
    candidateDetailsStatus.textContent = "";
  } catch {
    if (selectedCandidateId !== candidateId) return;
    candidateDetailsContent.replaceChildren();
    candidateDetailsStatus.textContent = "";
    candidateDetailsError.textContent = "Unable to load candidate details.";
    candidateDetailsError.hidden = false;
    retryCandidateDetails.hidden = false;
  } finally {
    if (selectedCandidateId === candidateId || selectedCandidateId === null) {
      candidateDetailsPanel.setAttribute("aria-busy", "false");
    }
  }
}

function positionLabel(position) {
  return position.jobTitle ?? position.jobName ?? "";
}

function positionListLabel(position) {
  return `${positionLabel(position)} - ${position.company ?? ""} - ${position.jobId ?? ""}`;
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
  findMatchesButton.hidden = !hasSelection;
}

function clearMatchResults() {
  matchRequestGeneration += 1;
  matchDetailsRequestGeneration += 1;
  matchRequestInFlight = false;
  currentMatchResults = [];
  matchPositionId = null;
  selectedMatch = null;
  findMatchesButton.disabled = false;
  matchResultsList.setAttribute("aria-busy", "false");
  matchResultsList.hidden = false;
  matchResultsList.replaceChildren();
  matchStatus.textContent = "";
  matchEmpty.hidden = true;
  matchError.textContent = "";
  matchError.hidden = true;
  retryMatchesButton.hidden = true;
  matchDetailsPanel.hidden = true;
  matchDetailsPanel.setAttribute("aria-busy", "false");
  matchDetailsContent.replaceChildren();
  matchDetailsStatus.textContent = "";
  matchDetailsError.textContent = "";
  matchDetailsError.hidden = true;
  retryMatchDetailsButton.hidden = true;
}

function renderMatchResults(items) {
  matchResultsList.replaceChildren();
  items.forEach((match) => {
    const item = document.createElement("li");
    item.className = "match-result";
    const summary = match.candidate ?? {};
    const heading = document.createElement("h3");
    heading.className = "h5 match-result-heading";
    const detailsButton = document.createElement("button");
    detailsButton.type = "button";
    detailsButton.className = "match-result-link";
    detailsButton.setAttribute(
      "aria-label",
      `View match details for ${candidateValue(summary.fullname)}`,
    );
    setText(
      detailsButton,
      `${candidateValue(summary.fullname)} - ${candidateValue(summary.position)} - `
        + `${match.matchPercentage}% match`,
    );
    detailsButton.addEventListener("click", () => loadMatchCandidateDetails(match));
    heading.append(detailsButton);
    const explanation = document.createElement("p");
    explanation.className = "match-explanation";
    setText(explanation, match.explanation);
    item.append(heading, explanation);
    matchResultsList.append(item);
  });
}

function renderMatchCandidateDetails(candidate, match, position) {
  matchDetailsContent.replaceChildren();
  const title = document.createElement("h2");
  title.className = "detail-title";
  setText(title, candidateValue(candidate.fullname));
  matchDetailsContent.append(title);

  const positionContext = document.createElement("p");
  setText(positionContext, `Match for ${positionLabel(position)} · ${candidateValue(position.company)}`);
  matchDetailsContent.append(positionContext);

  const explanationHeading = document.createElement("h3");
  explanationHeading.className = "h5";
  setText(explanationHeading, "Match explanation");
  const explanation = document.createElement("p");
  setText(explanation, match.explanation);
  matchDetailsContent.append(explanationHeading, explanation);

  const fields = document.createElement("dl");
  [
    ["Email", candidate.email],
    ["Full name", candidate.fullname],
    ["Country", candidate.country],
    ["City", candidate.city],
    ["Position", candidate.position],
    ["Skills", candidate.skills],
    ["Allocation", candidate.allocation],
    ["Years of experience", candidate.yearsOfExperience],
  ].forEach(([label, value]) => appendCandidateField(fields, label, value));
  matchDetailsContent.append(fields);

  const experienceHeading = document.createElement("h3");
  experienceHeading.className = "h5 mt-4";
  setText(experienceHeading, "Work experience");
  matchDetailsContent.append(experienceHeading);
  const experiences = Array.isArray(candidate.experiences) ? candidate.experiences : [];
  if (experiences.length === 0) {
    const emptyMessage = document.createElement("p");
    setText(emptyMessage, "No reported experience.");
    matchDetailsContent.append(emptyMessage);
    return;
  }

  const experienceList = document.createElement("ul");
  experienceList.className = "candidate-experiences";
  experiences.forEach((experience) => {
    const item = document.createElement("li");
    item.className = "candidate-experience";
    const experienceTitle = document.createElement("h4");
    experienceTitle.className = "h6";
    setText(experienceTitle, candidateValue(experience.title));
    const experienceFields = document.createElement("dl");
    [
      ["Company", experience.company],
      ["Period", experience.period],
      ["Responsibilities", experience.responsibilities],
      ["Achievements", experience.achievements],
    ].forEach(([label, value]) => appendCandidateField(experienceFields, label, value));
    item.append(experienceTitle, experienceFields);
    experienceList.append(item);
  });
  matchDetailsContent.append(experienceList);
}

function isCurrentMatchDetailsRequest(requestGeneration, positionId, match) {
  return requestGeneration === matchDetailsRequestGeneration
    && selectedMatch === match
    && selectedPosition
    && String(selectedPosition.id ?? "") === positionId
    && matchPositionId === positionId;
}

async function loadMatchCandidateDetails(match) {
  if (!selectedPosition || matchPositionId !== String(selectedPosition.id ?? "")) return;
  const candidateId = String(match.candidate?.id ?? "");
  if (!candidateId) return;

  selectedMatch = match;
  const position = selectedPosition;
  const positionId = String(position.id ?? "");
  const requestGeneration = ++matchDetailsRequestGeneration;
  matchResultsList.hidden = true;
  matchDetailsPanel.hidden = false;
  matchDetailsPanel.setAttribute("aria-busy", "true");
  matchDetailsStatus.textContent = "Loading candidate details…";
  matchDetailsError.textContent = "";
  matchDetailsError.hidden = true;
  retryMatchDetailsButton.hidden = true;
  matchDetailsContent.replaceChildren();

  try {
    const response = await fetch(`/api/candidates/${encodeURIComponent(candidateId)}`);
    let candidate = null;
    try {
      candidate = await response.json();
    } catch {
      candidate = null;
    }
    if (!response.ok
        || !candidate
        || candidate.code === "CANDIDATE_NOT_FOUND"
        || String(candidate.id ?? "") !== candidateId) {
      throw new Error("Candidate details unavailable");
    }
    if (!isCurrentMatchDetailsRequest(requestGeneration, positionId, match)) return;
    renderMatchCandidateDetails(candidate, match, position);
    matchDetailsStatus.textContent = "";
  } catch {
    if (!isCurrentMatchDetailsRequest(requestGeneration, positionId, match)) return;
    matchDetailsContent.replaceChildren();
    matchDetailsStatus.textContent = "";
    matchDetailsError.textContent = "Unable to load candidate details. Please retry.";
    matchDetailsError.hidden = false;
    retryMatchDetailsButton.hidden = false;
  } finally {
    if (isCurrentMatchDetailsRequest(requestGeneration, positionId, match)) {
      matchDetailsPanel.setAttribute("aria-busy", "false");
    }
  }
}

function returnToMatchResults() {
  matchDetailsRequestGeneration += 1;
  selectedMatch = null;
  matchDetailsPanel.hidden = true;
  matchDetailsPanel.setAttribute("aria-busy", "false");
  matchDetailsContent.replaceChildren();
  matchDetailsStatus.textContent = "";
  matchDetailsError.textContent = "";
  matchDetailsError.hidden = true;
  retryMatchDetailsButton.hidden = true;
  matchResultsList.hidden = false;
}

async function loadMatches() {
  if (!selectedPosition || matchRequestInFlight) return;

  const positionId = String(selectedPosition.id ?? "");
  const requestGeneration = ++matchRequestGeneration;
  matchRequestInFlight = true;
  currentMatchResults = [];
  matchPositionId = positionId;
  matchResultsList.replaceChildren();
  matchResultsList.setAttribute("aria-busy", "true");
  matchStatus.textContent = "Finding matching candidates…";
  matchEmpty.hidden = true;
  matchError.textContent = "";
  matchError.hidden = true;
  retryMatchesButton.hidden = true;
  matchDetailsPanel.hidden = true;
  matchDetailsRequestGeneration += 1;
  selectedMatch = null;
  matchResultsList.hidden = false;
  matchDetailsContent.replaceChildren();
  matchDetailsStatus.textContent = "";
  matchDetailsError.textContent = "";
  matchDetailsError.hidden = true;
  retryMatchDetailsButton.hidden = true;
  findMatchesButton.disabled = true;

  try {
    const response = await fetch(
      `/api/job-offers/${encodeURIComponent(positionId)}/matches`,
      { method: "POST" },
    );
    let result = null;
    try {
      result = await response.json();
    } catch {
      result = null;
    }
    if (!response.ok || !result || !Array.isArray(result.items)) {
      throw new Error("Candidate matching unavailable");
    }
    if (requestGeneration !== matchRequestGeneration
        || !selectedPosition
        || String(selectedPosition.id ?? "") !== positionId) return;

    currentMatchResults = result.items;
    matchPositionId = positionId;
    matchStatus.textContent = "";
    if (currentMatchResults.length === 0) {
      matchEmpty.hidden = false;
    } else {
      renderMatchResults(currentMatchResults);
    }
  } catch {
    if (requestGeneration !== matchRequestGeneration
        || !selectedPosition
        || String(selectedPosition.id ?? "") !== positionId) return;
    currentMatchResults = [];
    matchPositionId = null;
    matchResultsList.replaceChildren();
    matchStatus.textContent = "";
    matchError.textContent = "Unable to find matches. Please retry.";
    matchError.hidden = false;
    retryMatchesButton.hidden = false;
  } finally {
    if (requestGeneration === matchRequestGeneration) {
      matchRequestInFlight = false;
      findMatchesButton.disabled = false;
      matchResultsList.setAttribute("aria-busy", "false");
    }
  }
}

function showPosition(position, selectedEntry) {
  if (selectedPosition?.id !== position.id) clearMatchResults();
  selectedPosition = position;
  candidateMatches.hidden = false;
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
  detailFields.forEach(([label, property, supportsFormatting]) => {
    const field = document.createElement("div");
    field.className = "detail-field";

    const term = document.createElement("dt");
    term.textContent = label;
    const description = document.createElement("dd");
    if (supportsFormatting) renderFormattedPositionText(description, position[property]);
    else setText(description, position[property]);

    field.append(term, description);
    details.append(field);
  });
  positionDetails.append(details);
}

function renderPositions(nextPositions) {
  clearMatchResults();
  setSelectionActions(false);
  candidateMatches.hidden = true;
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
findMatchesButton.addEventListener("click", loadMatches);
retryMatchesButton.addEventListener("click", loadMatches);
backToMatchResultsButton.addEventListener("click", returnToMatchResults);
retryMatchDetailsButton.addEventListener("click", () =>
  selectedMatch ? loadMatchCandidateDetails(selectedMatch) : undefined);
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
candidatePreviousPage.addEventListener("click", () => {
  if (candidatePage > 0) loadCandidatePage(candidatePage - 1);
});
candidateNextPage.addEventListener("click", () => {
  if (candidatePage < candidateTotalPages - 1) loadCandidatePage(candidatePage + 1);
});
retryCandidateList.addEventListener("click", () => loadCandidatePage(candidatePage));
retryCandidateDetails.addEventListener("click", () =>
  selectedCandidateId ? loadCandidateDetails(selectedCandidateId) : undefined);
candidatesTab.addEventListener("shown.bs.tab", () => loadCandidatePage(0));

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
loadCandidatePage(0);
