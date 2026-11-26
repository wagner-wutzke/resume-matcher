package net.wowdev.ai.resume.matcher.joboffer;

import net.wowdev.ai.resume.matcher.controller.JobOfferController;
import net.wowdev.ai.resume.matcher.controller.JobOfferExceptionHandler;
import net.wowdev.ai.resume.matcher.dto.JobOfferRequest;
import net.wowdev.ai.resume.matcher.model.JobOffer;
import net.wowdev.ai.resume.matcher.service.JobOfferNotFoundException;
import net.wowdev.ai.resume.matcher.service.JobOfferPersistenceException;
import net.wowdev.ai.resume.matcher.service.JobOfferService;
import net.wowdev.ai.resume.matcher.service.JobOfferValidationException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class JobOfferControllerTest {

  private final JobOfferService service = mock(JobOfferService.class);
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new JobOfferController(service))
        .setControllerAdvice(new JobOfferExceptionHandler())
        .build();
  }

  @Test
  void returnsAllOfferFields() throws Exception {
    when(service.findAll()).thenReturn(List.of(new JobOffer(
        "offer-1", "Backend Engineer", "Build services", "Java and Spring", "Dublin", "Remote")));

    mockMvc.perform(get("/api/job-offers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value("offer-1"))
        .andExpect(jsonPath("$[0].jobName").value("Backend Engineer"))
        .andExpect(jsonPath("$[0].jobDescription").value("Build services"))
        .andExpect(jsonPath("$[0].requiredQualifications").value("Java and Spring"))
        .andExpect(jsonPath("$[0].location").value("Dublin"))
        .andExpect(jsonPath("$[0].allocation").value("Remote"));
  }

  @Test
  void returnsEmptyArrayWhenThereAreNoOffers() throws Exception {
    when(service.findAll()).thenReturn(List.of());

    mockMvc.perform(get("/api/job-offers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void reportsRepositoryFailureAsServiceUnavailable() throws Exception {
    when(service.findAll()).thenThrow(new DataAccessResourceFailureException("MongoDB unavailable"));

    mockMvc.perform(get("/api/job-offers"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("JOB_OFFERS_UNAVAILABLE"))
        .andExpect(jsonPath("$.message").value("Job offers could not be loaded."));
  }

  @Test
  void createsJobOfferAndReturnsLocationHeader() throws Exception {
    var saved = new JobOffer("offer-1", "Engineer", "Build services", "Java", "Dublin", "Remote");
    when(service.create(new JobOfferRequest(
        "Engineer", "Build services", "Java", "Dublin", "Remote"))).thenReturn(saved);

    mockMvc.perform(post("/api/job-offers")
            .contentType("application/json")
            .content("""
                {"jobName":"Engineer","jobDescription":"Build services",
                 "requiredQualifications":"Java","location":"Dublin","allocation":"Remote"}
                """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/job-offers/offer-1"))
        .andExpect(jsonPath("$.id").value("offer-1"))
        .andExpect(jsonPath("$.jobName").value("Engineer"))
        .andExpect(jsonPath("$.allocation").value("Remote"));

    verify(service).create(new JobOfferRequest(
        "Engineer", "Build services", "Java", "Dublin", "Remote"));
  }

  @Test
  void returnsFieldErrorsForInvalidCreateRequest() throws Exception {
    var request = new JobOfferRequest(" ", "Build services", "Java", "Dublin", "Remote");
    when(service.create(request)).thenThrow(new JobOfferValidationException(
        java.util.Map.of("jobName", "required")));

    mockMvc.perform(post("/api/job-offers")
            .contentType("application/json")
            .content("""
                {"jobName":" ","jobDescription":"Build services",
                 "requiredQualifications":"Java","location":"Dublin","allocation":"Remote"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("JOB_OFFER_VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors.jobName").value("required"));
  }

  @Test
  void updatesJobOfferAndReturnsUpdatedFields() throws Exception {
    var updated = new JobOffer("offer-1", "Platform Engineer", "Build systems", "Java", "Dublin", "Hybrid");
    when(service.update("offer-1", new JobOfferRequest(
        "Platform Engineer", "Build systems", "Java", "Dublin", "Hybrid"))).thenReturn(updated);

    mockMvc.perform(put("/api/job-offers/offer-1")
            .contentType("application/json")
            .content("""
                {"jobName":"Platform Engineer","jobDescription":"Build systems",
                 "requiredQualifications":"Java","location":"Dublin","allocation":"Hybrid"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("offer-1"))
        .andExpect(jsonPath("$.jobName").value("Platform Engineer"))
        .andExpect(jsonPath("$.allocation").value("Hybrid"));
  }

  @Test
  void returnsFieldErrorsForInvalidUpdateRequest() throws Exception {
    var request = new JobOfferRequest(" ", "Build systems", "Java", "Dublin", "Hybrid");
    when(service.update("offer-1", request)).thenThrow(new JobOfferValidationException(
        java.util.Map.of("jobName", "required")));

    mockMvc.perform(put("/api/job-offers/offer-1")
            .contentType("application/json")
            .content("""
                {"jobName":" ","jobDescription":"Build systems",
                 "requiredQualifications":"Java","location":"Dublin","allocation":"Hybrid"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors.jobName").value("required"));
  }

  @Test
  void returnsNotFoundForUnknownIdentifier() throws Exception {
    when(service.update(org.mockito.ArgumentMatchers.eq("missing"),
        org.mockito.ArgumentMatchers.any(JobOfferRequest.class)))
        .thenThrow(new JobOfferNotFoundException("missing"));

    mockMvc.perform(put("/api/job-offers/missing")
            .contentType("application/json")
            .content("""
                {"jobName":"Engineer","jobDescription":"Build services",
                 "requiredQualifications":"Java","location":"Dublin","allocation":"Remote"}
                """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("JOB_OFFER_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("The job offer was not found."));
  }

  @Test
  void deletesOfferAndReturnsNoContent() throws Exception {
    mockMvc.perform(delete("/api/job-offers/offer-1"))
        .andExpect(status().isNoContent());

    verify(service).delete("offer-1");
  }

  @Test
  void returnsNotFoundWhenDeletingUnknownOffer() throws Exception {
    org.mockito.Mockito.doThrow(new JobOfferNotFoundException("missing"))
        .when(service).delete("missing");

    mockMvc.perform(delete("/api/job-offers/missing"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("JOB_OFFER_NOT_FOUND"));
  }

  @Test
  void hidesPersistenceDetailsFromFailedCreateResponse() throws Exception {
    when(service.create(org.mockito.ArgumentMatchers.any(JobOfferRequest.class)))
        .thenThrow(new JobOfferPersistenceException(
            new IllegalStateException("mongodb://user:password@internal-host")));

    mockMvc.perform(post("/api/job-offers")
            .contentType("application/json")
            .content("""
                {"jobName":"Engineer","jobDescription":"Build services",
                 "requiredQualifications":"Java","location":"Dublin","allocation":"Remote"}
                """))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("JOB_OFFER_PERSISTENCE_UNAVAILABLE"))
        .andExpect(content().string(org.hamcrest.Matchers.not(
            org.hamcrest.Matchers.containsString("internal-host"))))
        .andExpect(content().string(org.hamcrest.Matchers.not(
            org.hamcrest.Matchers.containsString("password"))));
  }

}
