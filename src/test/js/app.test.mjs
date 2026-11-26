import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { runInNewContext } from "node:vm";
import test from "node:test";

const script = await readFile(new URL("../../main/resources/static/app.js", import.meta.url), "utf8");
const page = await readFile(new URL("../../main/resources/static/index.html", import.meta.url), "utf8");
const styles = await readFile(new URL("../../main/resources/static/styles.css", import.meta.url), "utf8");

class FakeElement {
  constructor() {
    this.children = [];
    this.attributes = new Map();
    this.listeners = new Map();
    this.classes = new Set();
    this.classList = {
      add: (name) => this.classes.add(name),
      toggle: (name, enabled) => enabled ? this.classes.add(name) : this.classes.delete(name),
      remove: (name) => this.classes.delete(name),
      contains: (name) => this.classes.has(name),
    };
    this.text = "";
    this.html = "";
    this.hidden = false;
    this.dataset = {};
  }

  set textContent(value) {
    this.children = [];
    this.text = String(value);
  }

  get textContent() {
    return this.text + this.children.map((child) => child.textContent).join("");
  }

  set innerHTML(value) {
    this.children = [];
    this.text = "";
    this.html = String(value);
  }

  get innerHTML() {
    return this.html;
  }

  set className(value) {
    this.classes = new Set(value.split(/\s+/).filter(Boolean));
  }

  replaceChildren(...children) {
    this.text = "";
    this.children = [...children];
  }

  append(...children) {
    this.text = "";
    this.children.push(...children);
  }

  setAttribute(name, value) {
    this.attributes.set(name, String(value));
  }

  removeAttribute(name) {
    this.attributes.delete(name);
  }

  addEventListener(name, callback) {
    this.listeners.set(name, callback);
  }

  querySelectorAll(selector) {
    const className = selector.slice(1);
    const matches = this.children.filter((child) => child.classes.has(className));
    const nodeList = { length: matches.length };
    nodeList.forEach = (callback) => matches.forEach(callback);
    matches.forEach((match, index) => { nodeList[index] = match; });
    return nodeList;
  }

  click() {
    return this.listeners.get("click")?.();
  }

  submit() {
    return this.listeners.get("submit")?.({ preventDefault() {} });
  }
}

function runApp(fetch, markdownGlobals = {}) {
  const elements = new Map([
    ["#position-list", new FakeElement()],
    ["#position-details", new FakeElement()],
    ["#new-job-offer", new FakeElement()],
    ["#edit-job-offer", new FakeElement()],
    ["#delete-job-offer", new FakeElement()],
    ["#job-offer-delete-error", new FakeElement()],
    ["#job-offer-modal", new FakeElement()],
    ["#job-offer-modal-title", new FakeElement()],
    ["#job-offer-save-error", new FakeElement()],
    ["#discard-confirmation", new FakeElement()],
    ["#job-offer-form", new FakeElement()],
    ["#job-company", new FakeElement()],
    ["#job-name", new FakeElement()],
    ["#job-description", new FakeElement()],
    ["#required-qualifications", new FakeElement()],
    ["#job-location", new FakeElement()],
    ["#job-allocation", new FakeElement()],
    ["#job-company-error", new FakeElement()],
    ["#job-name-error", new FakeElement()],
    ["#job-description-error", new FakeElement()],
    ["#required-qualifications-error", new FakeElement()],
    ["#job-location-error", new FakeElement()],
    ["#job-allocation-error", new FakeElement()],
    ["#cancel-job-offer", new FakeElement()],
    ["#close-job-offer-modal", new FakeElement()],
    ["#continue-editing", new FakeElement()],
    ["#discard-changes", new FakeElement()],
    ["#save-job-offer", new FakeElement()],
  ]);
  [
    ["#job-company", "company"],
    ["#job-name", "jobName"],
    ["#job-description", "jobDescription"],
    ["#required-qualifications", "requiredQualifications"],
    ["#job-location", "location"],
    ["#job-allocation", "allocation"],
  ].forEach(([selector, name]) => { elements.get(selector).name = name; });
  const document = {
    querySelector: (selector) => elements.get(selector),
    createElement: () => new FakeElement(),
  };
  const bootstrap = {
    Modal: {
      getOrCreateInstance(element) {
        return {
          show: () => { element.hidden = false; element.classList.add("show"); },
          hide: () => { element.hidden = true; element.classList.toggle("show", false); },
        };
      },
    },
  };
  runInNewContext(script, { document, fetch, bootstrap, ...markdownGlobals });
  return elements;
}

async function flushApp() {
  await new Promise((resolve) => setImmediate(resolve));
}

test("renders offers and selected details as text", async () => {
  const markupLikeText = "<img src=x onerror=alert(1)>";
  const elements = runApp(async () => ({
    ok: true,
    json: async () => [{
      id: "offer-1",
      jobId: "BE-00100010",
      company: "Example Company",
      jobName: "Backend Engineer",
      jobDescription: markupLikeText,
      requiredQualifications: "Java and Spring",
      location: "Dublin",
      allocation: "Remote",
    }],
  }));
  await flushApp();

  const list = elements.get("#position-list");
  assert.equal(list.children.length, 1);
  assert.equal(list.children[0].textContent, "Backend Engineer");
  assert.equal(elements.get("#position-details").textContent, "");

  list.children[0].click();
  assert.equal(list.children[0].attributes.get("aria-pressed"), "true");
  const detailText = elements.get("#position-details").textContent;
  assert.match(detailText, /Backend Engineer/);
  assert.match(detailText, /Example Company/);
  assert.ok(detailText.indexOf("Example Company") < detailText.indexOf(markupLikeText));
  assert.match(detailText, /<img src=x onerror=alert\(1\)>/);
  assert.match(detailText, /Java and Spring/);
  assert.match(detailText, /Dublin/);
  assert.match(detailText, /Remote/);
});

test("renders Markdown after disabling raw HTML and sanitizing generated markup", async () => {
  const parseOptions = [];
  const sanitizedInputs = [];
  const unsafeMarkup = "<p><strong>Safe Markdown</strong><img src=x onerror=alert(1)></p>";
  const elements = runApp(async () => ({
    ok: true,
    json: async () => [{
      id: "offer-markdown",
      company: "<b>Company</b>",
      jobName: "Engineer",
      jobDescription: "**Safe Markdown**",
      requiredQualifications: "- Java",
      location: "Dublin",
      allocation: "Remote",
    }],
  }), {
    marked: { parse: (_source, options) => { parseOptions.push(options); return unsafeMarkup; } },
    DOMPurify: { sanitize: (html) => { sanitizedInputs.push(html); return "<p><strong>Safe Markdown</strong></p>"; } },
  });
  await flushApp();
  elements.get("#position-list").children[0].click();

  const detailFields = elements.get("#position-details").children[1].children;
  const company = detailFields[0].children[1];
  const description = detailFields[1].children[1];
  assert.equal(company.textContent, "<b>Company</b>");
  assert.equal(company.html, "");
  assert.equal(parseOptions.length, 2);
  assert.ok(parseOptions.every((options) => options.html === false));
  assert.deepEqual(sanitizedInputs, [unsafeMarkup, unsafeMarkup]);
  assert.equal(description.innerHTML, "<p><strong>Safe Markdown</strong></p>");
});

test("leaves the list and details empty for an empty response", async () => {
  const elements = runApp(async () => ({ ok: true, json: async () => [] }));
  await flushApp();

  assert.equal(elements.get("#position-list").children.length, 0);
  assert.equal(elements.get("#position-details").textContent, "");
});

test("loads the catalog once and labels offers with their job titles", async () => {
  const requests = [];
  const elements = runApp(async (url) => {
    requests.push(url);
    return {
      ok: true,
      json: async () => [{ id: "offer-1", jobName: "Backend Engineer" }],
    };
  });
  await flushApp();

  assert.deepEqual(requests, ["/api/job-offers"]);
  assert.equal(elements.get("#position-list").children[0].textContent, "Backend Engineer");
});

test("does not preselect a returned offer", async () => {
  const elements = runApp(async () => ({
    ok: true,
    json: async () => [{ id: "offer-1", jobName: "Backend Engineer" }],
  }));
  await flushApp();

  assert.equal(elements.get("#position-list").children[0].attributes.get("aria-pressed"), "false");
  assert.equal(elements.get("#position-details").textContent, "");
});

test("shows a distinct error when the request fails", async () => {
  const elements = runApp(async () => { throw new Error("unavailable"); });
  await flushApp();

  assert.equal(elements.get("#position-list").textContent, "Unable to load open positions");
  assert.equal(elements.get("#position-list").classList.contains("load-error"), true);
  assert.equal(elements.get("#position-details").textContent, "");
});

test("shows a distinct error for an unsuccessful API response", async () => {
  const elements = runApp(async () => ({ ok: false, status: 503 }));
  await flushApp();

  assert.equal(elements.get("#position-list").textContent, "Unable to load open positions");
  assert.equal(elements.get("#position-list").classList.contains("load-error"), true);
  assert.equal(elements.get("#position-details").textContent, "");
});

test("shows edit and delete actions only when an offer is selected", async () => {
  const elements = runApp(async () => ({
    ok: true,
    json: async () => [
      { jobName: "Engineer" },
      { jobName: "Designer" },
    ],
  }));
  await flushApp();

  const edit = elements.get("#edit-job-offer");
  const remove = elements.get("#delete-job-offer");
  assert.equal(elements.get("#new-job-offer").hidden, false);
  assert.equal(edit.hidden, true);
  assert.equal(remove.hidden, true);

  const buttons = elements.get("#position-list").children;
  buttons[0].click();
  assert.equal(edit.hidden, false);
  assert.equal(remove.hidden, false);
  assert.equal(buttons[0].attributes.get("aria-pressed"), "true");

  buttons[1].click();
  assert.equal(buttons[0].attributes.get("aria-pressed"), "false");
  assert.equal(buttons[1].attributes.get("aria-pressed"), "true");
  assert.equal(buttons[1].classList.contains("job-offer-button"), true);
  assert.match(elements.get("#position-details").textContent, /Designer/);
  assert.doesNotMatch(elements.get("#position-details").textContent, /Engineer/);
});

test("uses the shared style and dimensions for job-offer buttons", () => {
  const actionIds = ["new-job-offer", "edit-job-offer", "delete-job-offer", "continue-editing",
    "discard-changes", "close-job-offer-modal", "cancel-job-offer", "save-job-offer"];
  actionIds.forEach((id) => {
    const button = page.match(new RegExp(`<button[^>]*id="${id}"[^>]*>`));
    assert.ok(button, `missing button ${id}`);
    assert.match(button[0], /class="[^"]*job-offer-button/);
  });
  assert.match(styles, /\.job-offer-button\s*\{[^}]*height:\s*35px;[^}]*font-size:\s*0\.75rem;/s);
  assert.match(styles, /\.position-button\.job-offer-button\s*\{/);
  assert.match(styles, /\.position-button\.job-offer-button\.selected\s*\{/);
});

test("opens an empty job-offer form from New Job Offer", async () => {
  const elements = runApp(async () => ({ ok: true, json: async () => [] }));
  const fieldSelectors = [
    "#job-company",
    "#job-name",
    "#job-description",
    "#required-qualifications",
    "#job-location",
    "#job-allocation",
  ];
  fieldSelectors.forEach((selector) => { elements.get(selector).value = "old value"; });

  elements.get("#new-job-offer").click();

  assert.equal(elements.get("#job-offer-modal").classList.contains("show"), true);
  assert.equal(elements.get("#job-offer-modal-title").textContent, "New Job Offer");
  assert.ok(elements.get("#cancel-job-offer"));
  assert.ok(elements.get("#save-job-offer"));
  fieldSelectors.forEach((selector) => assert.equal(elements.get(selector).value, ""));
});

test("opens the shared form with selected offer values for editing", async () => {
  const offer = {
    id: "offer-1",
    company: "Example Company",
    jobName: "Engineer",
    jobDescription: "Build services",
    requiredQualifications: "Java",
    location: "Dublin",
    allocation: "Remote",
  };
  const elements = runApp(async () => ({ ok: true, json: async () => [offer] }));
  await flushApp();

  elements.get("#position-list").children[0].click();
  elements.get("#edit-job-offer").click();

  assert.equal(elements.get("#job-offer-modal").classList.contains("show"), true);
  assert.equal(elements.get("#job-offer-modal-title").textContent, "Edit Job Offer");
  assert.equal(elements.get("#job-company").value, offer.company);
  assert.equal(elements.get("#job-name").value, offer.jobName);
  assert.equal(elements.get("#job-description").value, offer.jobDescription);
  assert.equal(elements.get("#required-qualifications").value, offer.requiredQualifications);
  assert.equal(elements.get("#job-location").value, offer.location);
  assert.equal(elements.get("#job-allocation").value, offer.allocation);
});

test("offers only the specified Allocation choices", () => {
  const options = [...page.matchAll(/<option value="([^"]*)"/g)].map((match) => match[1]);

  assert.deepEqual(options.slice(1), ["Remote", "Hybrid", "On-site"]);
});

test("shows accessible required errors only after Save is submitted", async () => {
  let fetchCount = 0;
  const elements = runApp(async () => {
    fetchCount += 1;
    return { ok: true, json: async () => [] };
  });
  await flushApp();
  elements.get("#new-job-offer").click();

  const fieldSelectors = [
    "#job-company",
    "#job-name",
    "#job-description",
    "#required-qualifications",
    "#job-location",
    "#job-allocation",
  ];
  const errorSelectors = [
    "#job-company-error",
    "#job-name-error",
    "#job-description-error",
    "#required-qualifications-error",
    "#job-location-error",
    "#job-allocation-error",
  ];
  fieldSelectors.forEach((selector) => {
    assert.equal(elements.get(selector).attributes.has("aria-invalid"), false);
  });
  errorSelectors.forEach((selector) => assert.equal(elements.get(selector).hidden, true));

  elements.get("#job-description").value = "Build services";
  elements.get("#job-company").value = "Example Company";
  elements.get("#required-qualifications").value = "Java";
  elements.get("#job-location").value = "Dublin";
  elements.get("#job-allocation").value = "Remote";
  elements.get("#job-offer-form").submit();

  assert.equal(elements.get("#job-name").classList.contains("is-invalid"), true);
  assert.equal(elements.get("#job-name").attributes.get("aria-invalid"), "true");
  assert.equal(elements.get("#job-name-error").hidden, false);
  assert.equal(elements.get("#job-description").classList.contains("is-invalid"), false);
  assert.equal(elements.get("#job-description-error").hidden, true);
  assert.equal(fetchCount, 1);
});

test("creates, selects, and displays the returned offer after a successful save", async () => {
  const savedOffer = {
    id: "offer-2",
    company: "Example Company",
    jobName: "Platform Engineer",
    jobDescription: "Build systems",
    requiredQualifications: "Java",
    location: "Dublin",
    allocation: "Hybrid",
  };
  const requests = [];
  const elements = runApp(async (url, options = {}) => {
    requests.push({ url, options });
    if (options.method === "POST") return { ok: true, json: async () => savedOffer };
    return { ok: true, json: async () => [] };
  });
  await flushApp();
  elements.get("#new-job-offer").click();
  elements.get("#job-company").value = savedOffer.company;
  elements.get("#job-name").value = savedOffer.jobName;
  elements.get("#job-description").value = savedOffer.jobDescription;
  elements.get("#required-qualifications").value = savedOffer.requiredQualifications;
  elements.get("#job-location").value = savedOffer.location;
  elements.get("#job-allocation").value = savedOffer.allocation;

  await elements.get("#job-offer-form").submit();

  assert.equal(requests.length, 2);
  assert.equal(requests[1].url, "/api/job-offers");
  assert.equal(requests[1].options.method, "POST");
  assert.deepEqual(JSON.parse(requests[1].options.body), {
    company: savedOffer.company,
    jobName: savedOffer.jobName,
    jobDescription: savedOffer.jobDescription,
    requiredQualifications: savedOffer.requiredQualifications,
    location: savedOffer.location,
    allocation: savedOffer.allocation,
  });
  const list = elements.get("#position-list");
  assert.equal(list.children.length, 1);
  assert.equal(list.children[0].textContent, savedOffer.jobName);
  assert.equal(list.children[0].attributes.get("aria-pressed"), "true");
  assert.match(elements.get("#position-details").textContent, /Platform Engineer/);
  assert.equal(elements.get("#job-offer-modal").hidden, true);
});

test("updates a selected offer and keeps it selected after a successful save", async () => {
  const originalOffer = {
    id: "offer-1",
    company: "Example Company",
    jobName: "Engineer",
    jobDescription: "Build services",
    requiredQualifications: "Java",
    location: "Dublin",
    allocation: "Remote",
  };
  const updatedOffer = { ...originalOffer, jobName: "Platform Engineer", allocation: "Hybrid" };
  const requests = [];
  const elements = runApp(async (url, options = {}) => {
    requests.push({ url, options });
    if (options.method === "PUT") return { ok: true, json: async () => updatedOffer };
    return { ok: true, json: async () => [originalOffer] };
  });
  await flushApp();
  elements.get("#position-list").children[0].click();
  elements.get("#edit-job-offer").click();
  elements.get("#job-name").value = updatedOffer.jobName;
  elements.get("#job-allocation").value = updatedOffer.allocation;

  await elements.get("#job-offer-form").submit();

  assert.equal(requests[1].url, "/api/job-offers/offer-1");
  assert.equal(requests[1].options.method, "PUT");
  assert.equal(elements.get("#position-list").children.length, 1);
  assert.equal(elements.get("#position-list").children[0].textContent, updatedOffer.jobName);
  assert.equal(elements.get("#position-list").children[0].attributes.get("aria-pressed"), "true");
  assert.match(elements.get("#position-details").textContent, /Platform Engineer/);
  assert.match(elements.get("#position-details").textContent, /Hybrid/);
  assert.equal(elements.get("#job-offer-modal").hidden, true);
});

test("keeps a failed create form open with values and an error message", async () => {
  const elements = runApp(async (url, options = {}) => {
    if (options.method === "POST") {
      return { ok: false, json: async () => ({ message: "Create failed" }) };
    }
    return { ok: true, json: async () => [] };
  });
  await flushApp();
  elements.get("#new-job-offer").click();
  elements.get("#job-company").value = "Example Company";
  elements.get("#job-name").value = "Engineer";
  elements.get("#job-description").value = "Build services";
  elements.get("#required-qualifications").value = "Java";
  elements.get("#job-location").value = "Dublin";
  elements.get("#job-allocation").value = "Remote";

  await elements.get("#job-offer-form").submit();

  assert.equal(elements.get("#job-offer-modal").hidden, false);
  assert.equal(elements.get("#job-name").value, "Engineer");
  assert.equal(elements.get("#job-offer-save-error").textContent, "Create failed");
  assert.equal(elements.get("#job-offer-save-error").hidden, false);
  assert.equal(elements.get("#position-list").children.length, 0);
});

test("keeps the selected details and edit values after a failed update", async () => {
  const originalOffer = {
    id: "offer-1",
    company: "Example Company",
    jobName: "Engineer",
    jobDescription: "Build services",
    requiredQualifications: "Java",
    location: "Dublin",
    allocation: "Remote",
  };
  const elements = runApp(async (url, options = {}) => {
    if (options.method === "PUT") {
      return { ok: false, json: async () => ({ message: "Update failed" }) };
    }
    return { ok: true, json: async () => [originalOffer] };
  });
  await flushApp();
  elements.get("#position-list").children[0].click();
  elements.get("#edit-job-offer").click();
  elements.get("#job-name").value = "Changed Engineer";

  await elements.get("#job-offer-form").submit();

  assert.equal(elements.get("#job-offer-modal").hidden, false);
  assert.equal(elements.get("#job-name").value, "Changed Engineer");
  assert.equal(elements.get("#job-offer-save-error").textContent, "Update failed");
  assert.match(elements.get("#position-details").textContent, /Engineer/);
  assert.doesNotMatch(elements.get("#position-details").textContent, /Changed Engineer/);
});

test("closes an unchanged form without showing the discard prompt", async () => {
  const elements = runApp(async () => ({ ok: true, json: async () => [] }));
  await flushApp();
  elements.get("#new-job-offer").click();

  elements.get("#cancel-job-offer").click();

  assert.equal(elements.get("#discard-confirmation").hidden, true);
  assert.equal(elements.get("#job-offer-modal").hidden, true);
});

test("keeps changed form values when the recruiter continues editing", async () => {
  const elements = runApp(async () => ({ ok: true, json: async () => [] }));
  await flushApp();
  elements.get("#new-job-offer").click();
  elements.get("#job-name").value = "Engineer";
  elements.get("#cancel-job-offer").click();

  assert.equal(elements.get("#discard-confirmation").hidden, false);
  elements.get("#continue-editing").click();

  assert.equal(elements.get("#discard-confirmation").hidden, true);
  assert.equal(elements.get("#job-offer-modal").hidden, false);
  assert.equal(elements.get("#job-name").value, "Engineer");
});

test("discards changed form values only after confirmation", async () => {
  const elements = runApp(async () => ({ ok: true, json: async () => [] }));
  await flushApp();
  elements.get("#new-job-offer").click();
  elements.get("#job-name").value = "Engineer";
  elements.get("#cancel-job-offer").click();
  elements.get("#discard-changes").click();

  assert.equal(elements.get("#discard-confirmation").hidden, true);
  assert.equal(elements.get("#job-offer-modal").hidden, true);
});

test("deletes the selected offer and clears its details after success", async () => {
  const offer = {
    id: "offer-1",
    company: "Example Company",
    jobName: "Engineer",
    jobDescription: "Build services",
    requiredQualifications: "Java",
    location: "Dublin",
    allocation: "Remote",
  };
  const requests = [];
  const elements = runApp(async (url, options = {}) => {
    requests.push({ url, options });
    if (options.method === "DELETE") return { ok: true };
    return { ok: true, json: async () => [offer] };
  });
  await flushApp();
  elements.get("#position-list").children[0].click();

  await elements.get("#delete-job-offer").click();

  assert.equal(requests[1].url, "/api/job-offers/offer-1");
  assert.equal(requests[1].options.method, "DELETE");
  assert.equal(elements.get("#position-list").children.length, 0);
  assert.equal(elements.get("#position-details").textContent, "");
  assert.equal(elements.get("#edit-job-offer").hidden, true);
  assert.equal(elements.get("#delete-job-offer").hidden, true);
  assert.equal(elements.get("#new-job-offer").hidden, false);
});

test("keeps the selected offer and details after a failed delete", async () => {
  const offer = {
    id: "offer-1",
    company: "Example Company",
    jobName: "Engineer",
    jobDescription: "Build services",
    requiredQualifications: "Java",
    location: "Dublin",
    allocation: "Remote",
  };
  const elements = runApp(async (url, options = {}) => {
    if (options.method === "DELETE") {
      return { ok: false, json: async () => ({ message: "Delete failed" }) };
    }
    return { ok: true, json: async () => [offer] };
  });
  await flushApp();
  elements.get("#position-list").children[0].click();

  await elements.get("#delete-job-offer").click();

  assert.equal(elements.get("#position-list").children.length, 1);
  assert.equal(elements.get("#position-list").children[0].attributes.get("aria-pressed"), "true");
  assert.match(elements.get("#position-details").textContent, /Engineer/);
  assert.equal(elements.get("#edit-job-offer").hidden, false);
  assert.equal(elements.get("#delete-job-offer").hidden, false);
  assert.equal(elements.get("#job-offer-delete-error").textContent, "Delete failed");
  assert.equal(elements.get("#job-offer-delete-error").hidden, false);
});
