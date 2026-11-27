import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { runInNewContext } from "node:vm";
import test from "node:test";

const script = await readFile(new URL("../../main/resources/static/app.js", import.meta.url), "utf8");
const page = await readFile(new URL("../../main/resources/static/index.html", import.meta.url), "utf8");
const styles = await readFile(new URL("../../main/resources/static/styles.css", import.meta.url), "utf8");

class FakeElement {
  constructor() {
    this.tagName = "DIV";
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
    return this.children.flatMap((child) => [
      ...(child.classes.has(className) ? [child] : []),
      ...child.querySelectorAll(selector),
    ]);
  }

  click() {
    if (this.disabled || this.hidden) return undefined;
    return this.listeners.get("click")?.();
  }

  fire(name, event = {}) {
    return this.listeners.get(name)?.(event);
  }

  focus() {
    this.focused = true;
  }

  submit() {
    return this.listeners.get("submit")?.({ preventDefault() {} });
  }
}

function runApp(fetch, markdownGlobals = {}, candidateMode = false) {
  const appFetch = (url, options) => {
    if (!candidateMode && String(url).startsWith("/api/candidates")) {
      return Promise.resolve({ ok: true, json: async () => ({ items: [], page: 0, totalPages: 0 }) });
    }
    return fetch(url, options);
  };
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
    ["#years-of-experience", new FakeElement()],
    ["#job-location", new FakeElement()],
    ["#job-allocation", new FakeElement()],
    ["#job-company-error", new FakeElement()],
    ["#job-name-error", new FakeElement()],
    ["#job-description-error", new FakeElement()],
    ["#required-qualifications-error", new FakeElement()],
    ["#years-of-experience-error", new FakeElement()],
    ["#job-location-error", new FakeElement()],
    ["#job-allocation-error", new FakeElement()],
    ["#cancel-job-offer", new FakeElement()],
    ["#close-job-offer-modal", new FakeElement()],
    ["#continue-editing", new FakeElement()],
    ["#discard-changes", new FakeElement()],
    ["#save-job-offer", new FakeElement()],
    ["#candidate-list", new FakeElement()],
    ["#candidate-list-status", new FakeElement()],
    ["#candidate-list-error", new FakeElement()],
    ["#retry-candidate-list", new FakeElement()],
    ["#candidate-pagination", new FakeElement()],
    ["#candidate-previous-page", new FakeElement()],
    ["#candidate-next-page", new FakeElement()],
    ["#candidate-page-indicator", new FakeElement()],
    ["#candidate-details-content", new FakeElement()],
    ["#candidate-details", new FakeElement()],
    ["#candidate-details-status", new FakeElement()],
    ["#candidate-details-error", new FakeElement()],
    ["#retry-candidate-details", new FakeElement()],
    ["#candidates-tab", new FakeElement()],
  ]);
  [
    ["#job-company", "company"],
    ["#job-name", "jobName"],
    ["#job-description", "jobDescription"],
    ["#required-qualifications", "requiredQualifications"],
    ["#years-of-experience", "yearsOfExperience"],
    ["#job-location", "location"],
    ["#job-allocation", "allocation"],
  ].forEach(([selector, name]) => { elements.get(selector).name = name; });
  const document = {
    querySelector: (selector) => elements.get(selector),
    createElement: (tagName) => {
      const element = new FakeElement();
      element.tagName = String(tagName).toUpperCase();
      return element;
    },
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
  runInNewContext(script, { document, fetch: appFetch, bootstrap, ...markdownGlobals });
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
      jobTitle: "Backend Engineer",
      jobDescription: markupLikeText,
      requiredQualifications: "Java and Spring",
      yearsOfExperience: 7,
      location: "Dublin",
      allocation: "Remote",
    }],
  }));
  await flushApp();

  const list = elements.get("#position-list");
  assert.equal(list.children.length, 1);
  assert.equal(list.children[0].textContent, "Backend Engineer - Example Company - BE-00100010");
  assert.equal(list.children[0].tagName, "DIV");
  assert.equal(list.children[0].attributes.get("role"), "option");
  assert.equal(elements.get("#position-details").textContent, "");

  list.children[0].click();
  assert.equal(list.children[0].attributes.get("aria-selected"), "true");
  const detailText = elements.get("#position-details").textContent;
  assert.match(detailText, /Backend Engineer/);
  assert.match(detailText, /Example Company/);
  assert.ok(detailText.indexOf("Example Company") < detailText.indexOf(markupLikeText));
  assert.match(detailText, /<img src=x onerror=alert\(1\)>/);
  assert.match(detailText, /Java and Spring/);
  assert.match(detailText, /7/);
  assert.match(detailText, /Years of experience/);
  assert.match(detailText, /Dublin/);
  assert.match(detailText, /Remote/);
});

test("uses the specified palette, page space, and responsive split layout", () => {
  assert.match(page, /bootstrap@5\.3\.3\/dist\/css\/bootstrap\.min\.css/);
  assert.match(styles, /--ink:\s*#343a40;/);
  assert.match(styles, /--royal-blue:\s*#4169e1;/);
  assert.match(styles, /\.app-shell\s*\{[^}]*min-height:\s*100vh;/s);
  assert.match(styles, /@media\s*\(max-width:\s*700px\)[\s\S]*?grid-template-columns:\s*1fr;/);
});

test("provides accessible candidate pagination and retry regions without changing tabs", () => {
  assert.match(page, /id="candidate-list"[^>]*role="list"/);
  assert.match(page, /id="candidate-list-status"[^>]*role="status"[^>]*aria-live="polite"/);
  assert.match(page, /id="candidate-pagination"[^>]*aria-label="Candidate pages"[^>]*hidden/);
  assert.match(page, /id="candidate-previous-page" type="button"/);
  assert.match(page, /id="candidate-next-page" type="button"/);
  assert.match(page, /id="retry-candidate-list" type="button" hidden/);
  assert.match(page, /id="retry-candidate-details" type="button" hidden/);
  assert.match(page, /id="positions-tab"/);
  assert.match(page, /id="position-list"/);
});

test("loads and safely renders candidate summaries with placeholders and pagination", async () => {
  const unsafeName = "<img src=x onerror=alert(1)>";
  const requests = [];
  const elements = runApp(async (url) => {
    if (url.startsWith("/api/candidates")) requests.push(url);
    return {
      ok: true,
      json: async () => ({
        items: [{
          id: "candidate-1",
          fullname: unsafeName,
          position: "Engineer",
          country: "Brazil",
          city: "São Paulo",
          yearsOfExperience: "5 years",
        }],
        page: 0,
        pageSize: 15,
        totalElements: 16,
        totalPages: 2,
      }),
    };
  }, {}, true);
  await flushApp();

  const list = elements.get("#candidate-list");
  assert.deepEqual(requests, ["/api/candidates?page=0"]);
  assert.equal(list.children.length, 1);
  const entries = list.querySelectorAll(".candidate-entry");
  assert.equal(entries[0].tagName, "BUTTON");
  assert.equal(list.children[0].attributes.get("role"), "listitem");
  assert.match(entries[0].textContent, /<img src=x onerror=alert\(1\)>/);
  assert.match(entries[0].textContent, /São Paulo, Brazil/);
  assert.equal(elements.get("#candidate-pagination").hidden, false);
  assert.equal(elements.get("#candidate-page-indicator").textContent, "Page 1 of 2");
  assert.equal(elements.get("#candidate-previous-page").disabled, true);
  assert.equal(elements.get("#candidate-next-page").disabled, false);
});

test("caps candidate summaries at 15 and displays placeholders for missing summary values", async () => {
  const items = Array.from({ length: 16 }, (_, index) => ({ id: `candidate-${index}` }));
  const elements = runApp(async (url) => {
    if (url.startsWith("/api/candidates")) {
      return { ok: true, json: async () => ({ items, page: 0, totalPages: 2 }) };
    }
    return { ok: true, json: async () => [] };
  }, {}, true);
  await flushApp();

  const entries = elements.get("#candidate-list").querySelectorAll(".candidate-entry");
  assert.equal(entries.length, 15);
  assert.equal(entries[0].textContent, "— · — · — · —");
});

test("changes candidate pages within boundaries and refreshes when the tab is reopened", async () => {
  const requests = [];
  const elements = runApp(async (url) => {
    if (url.startsWith("/api/candidates")) requests.push(url);
    const page = Number(new URL(url, "http://local").searchParams.get("page"));
    return {
      ok: true,
      json: async () => ({
        items: [{ id: `candidate-${page}`, fullname: `Candidate ${page}` }],
        page,
        totalPages: 2,
      }),
    };
  }, {}, true);
  await flushApp();

  await elements.get("#candidate-next-page").click();
  await flushApp();
  assert.equal(elements.get("#candidate-page-indicator").textContent, "Page 2 of 2");
  assert.equal(elements.get("#candidate-next-page").disabled, true);
  await elements.get("#candidate-previous-page").click();
  await flushApp();
  assert.equal(elements.get("#candidate-page-indicator").textContent, "Page 1 of 2");

  await elements.get("#candidates-tab").fire("shown.bs.tab");
  await flushApp();
  assert.deepEqual(requests, [
    "/api/candidates?page=0",
    "/api/candidates?page=1",
    "/api/candidates?page=0",
    "/api/candidates?page=0",
  ]);
});

test("shows an empty candidate state without pagination", async () => {
  const elements = runApp(async () => ({
    ok: true,
    json: async () => ({ items: [], page: 0, totalElements: 0, totalPages: 0 }),
  }), {}, true);
  await flushApp();

  assert.equal(elements.get("#candidate-list").children.length, 0);
  assert.equal(elements.get("#candidate-list-status").textContent, "No candidates available.");
  assert.equal(elements.get("#candidate-pagination").hidden, true);
  assert.equal(elements.get("#candidate-details-content").children.length, 0);
});

test("shows a retryable safe error and reloads a failed candidate page", async () => {
  let attempts = 0;
  const elements = runApp(async (url) => {
    if (!url.startsWith("/api/candidates")) return { ok: true, json: async () => [] };
    attempts += 1;
    if (attempts === 1) throw new Error("private database details");
    return { ok: true, json: async () => ({ items: [], page: 0, totalPages: 0 }) };
  }, {}, true);
  await flushApp();

  assert.equal(elements.get("#candidate-list-error").textContent, "Unable to load candidates.");
  assert.equal(elements.get("#candidate-list-error").hidden, false);
  assert.equal(elements.get("#retry-candidate-list").hidden, false);
  assert.equal(elements.get("#candidate-list-error").textContent.includes("private"), false);
  await elements.get("#retry-candidate-list").click();
  await flushApp();
  assert.equal(attempts, 2);
  assert.equal(elements.get("#candidate-list-error").hidden, true);
});

test("keeps Open Positions usable when candidate list loading fails", async () => {
  const elements = runApp(async (url) => {
    if (url.startsWith("/api/candidates")) {
      return {
        ok: false,
        status: 503,
        json: async () => ({ code: "CANDIDATES_UNAVAILABLE", message: "Unavailable" }),
      };
    }
    return {
      ok: true,
      json: async () => [{
        id: "position-1",
        jobTitle: "Backend Engineer",
        company: "Example Co.",
        jobId: "JOB-01",
      }],
    };
  }, {}, true);
  await flushApp();

  assert.equal(elements.get("#candidate-list-error").hidden, false);
  const positionList = elements.get("#position-list");
  assert.equal(positionList.children.length, 1);
  positionList.children[0].click();
  assert.equal(elements.get("#position-details").textContent.includes("Backend Engineer"), true);
  assert.equal(elements.get("#position-details").textContent.includes("Example Co."), true);
});

test("loads and renders selected candidate details and structured experiences as text", async () => {
  const unsafeText = "<script>candidate data</script>";
  const candidate = {
    id: "candidate-1",
    email: "jane@example.com",
    fullname: unsafeText,
    country: "Brazil",
    city: "São Paulo",
    position: "Engineer",
    skills: "Java",
    allocation: "Remote",
    yearsOfExperience: "5 years",
    experiences: [{
      title: "Developer",
      company: "Example Co.",
      period: "2021 – Present",
      responsibilities: "Built services",
      achievements: "Improved reliability",
    }],
  };
  const requests = [];
  const elements = runApp(async (url) => {
    if (url.startsWith("/api/candidates")) requests.push(url);
    if (url.startsWith("/api/candidates/")) return { ok: true, json: async () => candidate };
    return {
      ok: true,
      json: async () => ({ items: [{ id: candidate.id, fullname: candidate.fullname }], page: 0, totalPages: 1 }),
    };
  }, {}, true);
  await flushApp();

  await elements.get("#candidate-list").querySelectorAll(".candidate-entry")[0].click();

  assert.equal(requests[1], "/api/candidates/candidate-1");
  assert.equal(elements.get("#candidate-pagination").hidden, false);
  assert.equal(elements.get("#candidate-previous-page").disabled, true);
  assert.equal(elements.get("#candidate-next-page").disabled, true);
  assert.equal(
    elements.get("#candidate-list").querySelectorAll(".candidate-entry")[0].attributes.get("aria-pressed"),
    "true",
  );
  const details = elements.get("#candidate-details-content");
  assert.match(details.textContent, /jane@example.com/);
  assert.match(details.textContent, /Brazil/);
  assert.match(details.textContent, /Java/);
  assert.match(details.textContent, /Example Co\./);
  assert.match(details.textContent, /Improved reliability/);
  assert.match(details.textContent, /<script>candidate data<\/script>/);
  assert.equal(details.children.some((child) => child.html !== ""), false);
  assert.equal(elements.get("#candidate-details").attributes.get("aria-busy"), "false");
});

test("replaces details on selection change and discards stale responses", async () => {
  const pending = new Map();
  const elements = runApp((url) => {
    if (url.startsWith("/api/candidates/")) {
      return new Promise((resolve) => pending.set(url, resolve));
    }
    return Promise.resolve({
      ok: true,
      json: async () => ({
        items: [{ id: "one", fullname: "First" }, { id: "two", fullname: "Second" }],
        page: 0,
        totalPages: 1,
      }),
    });
  }, {}, true);
  await flushApp();
  const entries = elements.get("#candidate-list").querySelectorAll(".candidate-entry");
  const staleRequest = entries[0].click();
  const currentRequest = entries[1].click();
  pending.get("/api/candidates/two")({
    ok: true,
    json: async () => ({ id: "two", fullname: "Second candidate", experiences: [] }),
  });
  await currentRequest;
  pending.get("/api/candidates/one")({
    ok: true,
    json: async () => ({ id: "one", fullname: "First candidate", experiences: [] }),
  });
  await staleRequest;

  assert.match(elements.get("#candidate-details-content").textContent, /Second candidate/);
  assert.doesNotMatch(elements.get("#candidate-details-content").textContent, /First candidate/);
  assert.equal(entries[0].attributes.get("aria-pressed"), "false");
  assert.equal(entries[1].attributes.get("aria-pressed"), "true");
});

test("shows empty experience state and retries failed candidate details", async () => {
  let detailAttempts = 0;
  const elements = runApp(async (url) => {
    if (url.startsWith("/api/candidates/")) {
      detailAttempts += 1;
      if (detailAttempts === 1) throw new Error("private storage details");
      return { ok: true, json: async () => ({ id: "candidate-1", fullname: "Jane", experiences: [] }) };
    }
    return {
      ok: true,
      json: async () => ({ items: [{ id: "candidate-1", fullname: "Jane" }], page: 0, totalPages: 1 }),
    };
  }, {}, true);
  await flushApp();
  await elements.get("#candidate-list").querySelectorAll(".candidate-entry")[0].click();

  assert.equal(elements.get("#candidate-details-error").textContent, "Unable to load candidate details.");
  assert.equal(elements.get("#retry-candidate-details").hidden, false);
  await elements.get("#retry-candidate-details").click();

  assert.equal(detailAttempts, 2);
  assert.match(elements.get("#candidate-details-content").textContent, /No reported experience\./);
  assert.equal(elements.get("#candidate-details-error").hidden, true);
});

test("shows placeholders for missing detail fields and empty experience values", async () => {
  const elements = runApp(async (url) => {
    if (url.startsWith("/api/candidates/")) {
      return {
        ok: true,
        json: async () => ({ id: "candidate-1", experiences: [{ title: "", company: "" }] }),
      };
    }
    return {
      ok: true,
      json: async () => ({ items: [{ id: "candidate-1" }], page: 0, totalPages: 1 }),
    };
  }, {}, true);
  await flushApp();
  await elements.get("#candidate-list").querySelectorAll(".candidate-entry")[0].click();

  const detailText = elements.get("#candidate-details-content").textContent;
  assert.match(detailText, /Email—/);
  assert.match(detailText, /Full name—/);
  assert.match(detailText, /Company—/);
  assert.match(detailText, /Period—/);
});

test("clears a missing candidate and refreshes the current list page", async () => {
  const pageRequests = [];
  const elements = runApp(async (url) => {
    if (url.startsWith("/api/candidates/")) return { ok: false, status: 404, json: async () => ({}) };
    if (url.startsWith("/api/candidates")) pageRequests.push(url);
    return {
      ok: true,
      json: async () => ({ items: [{ id: "candidate-1", fullname: "Jane" }], page: 0, totalPages: 1 }),
    };
  }, {}, true);
  await flushApp();
  await elements.get("#candidate-list").querySelectorAll(".candidate-entry")[0].click();
  await flushApp();

  assert.deepEqual(pageRequests, ["/api/candidates?page=0", "/api/candidates?page=0"]);
  assert.equal(
    elements.get("#candidate-list").querySelectorAll(".candidate-entry")[0].attributes.get("aria-pressed"),
    "false",
  );
  assert.equal(elements.get("#candidate-details-content").children.length, 0);
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

test("loads the catalog once and labels offers with title, company, and generated job ID", async () => {
  const requests = [];
  const elements = runApp(async (url) => {
    requests.push(url);
    return {
      ok: true,
      json: async () => [{ id: "offer-1", jobTitle: "Backend Engineer", company: "Example Company", jobId: "JD-00000001" }],
    };
  });
  await flushApp();

  assert.deepEqual(requests, ["/api/job-offers"]);
  assert.equal(elements.get("#position-list").children[0].textContent,
    "Backend Engineer - Example Company - JD-00000001");
});

test("does not preselect a returned offer", async () => {
  const elements = runApp(async () => ({
    ok: true,
    json: async () => [{ id: "offer-1", jobTitle: "Backend Engineer", company: "Example Company", jobId: "JD-00000001" }],
  }));
  await flushApp();

  assert.equal(elements.get("#position-list").children[0].attributes.get("aria-selected"), "false");
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
      { id: "offer-1", jobId: "JD-00000001", company: "Example Company", jobTitle: "Engineer" },
      { id: "offer-2", jobId: "JD-00000002", company: "Example Inc", jobTitle: "Designer" },
    ],
  }));
  await flushApp();

  const edit = elements.get("#edit-job-offer");
  const remove = elements.get("#delete-job-offer");
  assert.equal(elements.get("#new-job-offer").hidden, false);
  assert.equal(edit.hidden, true);
  assert.equal(remove.hidden, true);

  const entries = elements.get("#position-list").children;
  entries[0].click();
  assert.equal(edit.hidden, false);
  assert.equal(remove.hidden, false);
  assert.equal(entries[0].attributes.get("aria-selected"), "true");

  entries[1].click();
  assert.equal(entries[0].attributes.get("aria-selected"), "false");
  assert.equal(entries[1].attributes.get("aria-selected"), "true");
  assert.equal(entries[1].classList.contains("job-offer-button"), false);
  assert.match(elements.get("#position-details").textContent, /Designer/);
  assert.doesNotMatch(elements.get("#position-details").textContent, /Engineer/);
});

test("supports keyboard focus and selection for non-button offer entries", async () => {
  const elements = runApp(async () => ({
    ok: true,
    json: async () => [
      { id: "offer-1", jobId: "JD-00000001", company: "Example", jobTitle: "Engineer" },
      { id: "offer-2", jobId: "JD-00000002", company: "Example", jobTitle: "Designer" },
    ],
  }));
  await flushApp();
  const entries = elements.get("#position-list").children;
  let prevented = false;
  entries[0].listeners.get("keydown")({ key: "ArrowDown", preventDefault: () => { prevented = true; } });
  assert.equal(prevented, true);
  assert.equal(entries[1].focused, true);

  entries[1].listeners.get("keydown")({ key: "Enter", preventDefault() {} });
  assert.equal(entries[1].attributes.get("aria-selected"), "true");
  assert.match(elements.get("#position-details").textContent, /Designer/);
  entries[0].listeners.get("keydown")({ key: " ", preventDefault() {} });
  assert.equal(entries[0].attributes.get("aria-selected"), "true");
  assert.equal(entries[1].attributes.get("aria-selected"), "false");
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
  assert.match(styles, /\.position-entry\.selected\s*\{/);
  assert.doesNotMatch(page, /<button[^>]*class="[^"]*position/);
});

test("opens an empty job-offer form from New Job Offer", async () => {
  const elements = runApp(async () => ({ ok: true, json: async () => [] }));
  const fieldSelectors = [
    "#job-company",
    "#job-name",
    "#job-description",
    "#required-qualifications",
    "#years-of-experience",
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
    yearsOfExperience: 5,
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
  assert.equal(elements.get("#years-of-experience").value, String(offer.yearsOfExperience));
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
    "#years-of-experience",
    "#job-location",
    "#job-allocation",
  ];
  const errorSelectors = [
    "#job-company-error",
    "#job-name-error",
    "#job-description-error",
    "#required-qualifications-error",
    "#years-of-experience-error",
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
  elements.get("#years-of-experience").value = "5";
  elements.get("#job-location").value = "Dublin";
  elements.get("#job-allocation").value = "Remote";
  elements.get("#job-offer-form").submit();

  assert.equal(elements.get("#job-name").classList.contains("is-invalid"), true);
  assert.equal(elements.get("#job-name").attributes.get("aria-invalid"), "true");
  assert.equal(elements.get("#job-name-error").hidden, false);
  assert.equal(elements.get("#job-description").classList.contains("is-invalid"), false);
  assert.equal(elements.get("#job-description-error").hidden, true);
  assert.equal(elements.get("#years-of-experience").classList.contains("is-invalid"), false);
  assert.equal(elements.get("#years-of-experience-error").hidden, true);
  assert.equal(fetchCount, 1);
});

test("requires a whole numeric Years of Experience value", async () => {
  const elements = runApp(async () => ({ ok: true, json: async () => [] }));
  await flushApp();
  elements.get("#new-job-offer").click();
  elements.get("#job-company").value = "Example Company";
  elements.get("#job-name").value = "Engineer";
  elements.get("#job-description").value = "Build services";
  elements.get("#required-qualifications").value = "Java";
  elements.get("#job-location").value = "Dublin";
  elements.get("#job-allocation").value = "Remote";

  await elements.get("#job-offer-form").submit();

  assert.equal(elements.get("#years-of-experience").classList.contains("is-invalid"), true);
  assert.equal(elements.get("#years-of-experience").attributes.get("aria-invalid"), "true");
  assert.equal(elements.get("#years-of-experience-error").hidden, false);
});

test("creates, selects, and displays the returned offer after a successful save", async () => {
  const savedOffer = {
    id: "offer-2",
    jobId: "JD-00000001",
    company: "Example Company",
    jobName: "Platform Engineer",
    jobDescription: "Build systems",
    requiredQualifications: "Java",
    yearsOfExperience: 5,
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
  elements.get("#years-of-experience").value = String(savedOffer.yearsOfExperience);
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
    yearsOfExperience: savedOffer.yearsOfExperience,
    location: savedOffer.location,
    allocation: savedOffer.allocation,
  });
  const list = elements.get("#position-list");
  assert.equal(list.children.length, 1);
  assert.equal(list.children[0].textContent, `${savedOffer.jobName} - ${savedOffer.company} - ${savedOffer.jobId}`);
  assert.equal(list.children[0].attributes.get("aria-selected"), "true");
  assert.match(elements.get("#position-details").textContent, /Platform Engineer/);
  assert.equal(elements.get("#job-offer-modal").hidden, true);
});

test("updates a selected offer and keeps it selected after a successful save", async () => {
  const originalOffer = {
    id: "offer-1",
    jobId: "JD-00000001",
    company: "Example Company",
    jobName: "Engineer",
    jobDescription: "Build services",
    requiredQualifications: "Java",
    yearsOfExperience: 5,
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
  assert.equal(elements.get("#position-list").children[0].textContent,
    `${updatedOffer.jobName} - ${updatedOffer.company} - ${updatedOffer.jobId}`);
  assert.equal(elements.get("#position-list").children[0].attributes.get("aria-selected"), "true");
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
  elements.get("#years-of-experience").value = "5";
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
    yearsOfExperience: 5,
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
    yearsOfExperience: 5,
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
    yearsOfExperience: 5,
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
  assert.equal(elements.get("#position-list").children[0].attributes.get("aria-selected"), "true");
  assert.match(elements.get("#position-details").textContent, /Engineer/);
  assert.equal(elements.get("#edit-job-offer").hidden, false);
  assert.equal(elements.get("#delete-job-offer").hidden, false);
  assert.equal(elements.get("#job-offer-delete-error").textContent, "Delete failed");
  assert.equal(elements.get("#job-offer-delete-error").hidden, false);
});
