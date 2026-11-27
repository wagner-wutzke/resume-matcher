package net.wowdev.ai.resume.matcher.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import net.wowdev.ai.resume.matcher.dto.CandidateDetailsResponse;
import net.wowdev.ai.resume.matcher.dto.CandidateExperienceResponse;
import net.wowdev.ai.resume.matcher.dto.CandidatePageResponse;
import net.wowdev.ai.resume.matcher.dto.CandidateSummaryResponse;
import net.wowdev.ai.resume.matcher.service.CandidateDetailsReadException;
import net.wowdev.ai.resume.matcher.service.CandidateListReadException;
import net.wowdev.ai.resume.matcher.service.CandidateNotFoundException;
import net.wowdev.ai.resume.matcher.service.CandidatePageInvalidException;
import net.wowdev.ai.resume.matcher.service.CandidatePageOutOfRangeException;
import net.wowdev.ai.resume.matcher.service.CandidateQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(OutputCaptureExtension.class)
class CandidateControllerTest {

  private final CandidateQueryService service = mock(CandidateQueryService.class);
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new CandidateController(service))
            .setControllerAdvice(new CandidateControllerExceptionHandler())
            .build();
  }

  @Test
  void returnsPagedSummaryWithoutDetailFields() throws Exception {
    when(service.findPage(0))
        .thenReturn(
            new CandidatePageResponse(
                List.of(
                    new CandidateSummaryResponse(
                        "id-1", "Jane Doe", "Engineer", "Brazil", "São Paulo", "5 years")),
                0,
                15,
                1,
                1));

    mockMvc
        .perform(get("/api/candidates"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.pageSize").value(15))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.items[0].id").value("id-1"))
        .andExpect(jsonPath("$.items[0].fullname").value("Jane Doe"))
        .andExpect(jsonPath("$.items[0].email").doesNotExist())
        .andExpect(jsonPath("$.items[0].experiences").doesNotExist());
    verify(service).findPage(0);
    verifyNoMoreInteractions(service);
  }

  @Test
  void returnsStructuredDetailsById() throws Exception {
    when(service.findDetails("id-1"))
        .thenReturn(
            new CandidateDetailsResponse(
                "id-1",
                "jane@example.com",
                "Jane Doe",
                "Brazil",
                "São Paulo",
                "Engineer",
                "Java",
                "Remote",
                "5 years",
                List.of(
                    new CandidateExperienceResponse(
                        "Engineer", "Example Co.", "2021-present", "Build", "Improved"))));

    mockMvc
        .perform(get("/api/candidates/id-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("jane@example.com"))
        .andExpect(jsonPath("$.skills").value("Java"))
        .andExpect(jsonPath("$.experiences[0].company").value("Example Co."))
        .andExpect(jsonPath("$.resumeText").doesNotExist())
        .andExpect(jsonPath("$.vector").doesNotExist());
    verify(service).findDetails("id-1");
    verifyNoMoreInteractions(service);
  }

  @Test
  void mapsInvalidAndOutOfRangePagesToBadRequest() throws Exception {
    when(service.findPage(-1)).thenThrow(new CandidatePageInvalidException());
    mockMvc
        .perform(get("/api/candidates?page=-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CANDIDATE_PAGE_INVALID"));
    mockMvc
        .perform(get("/api/candidates?page=not-a-number"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CANDIDATE_PAGE_INVALID"));

    when(service.findPage(9)).thenThrow(new CandidatePageOutOfRangeException());
    mockMvc
        .perform(get("/api/candidates?page=9"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CANDIDATE_PAGE_OUT_OF_RANGE"));
  }

  @Test
  void mapsMissingCandidateToNotFound() throws Exception {
    when(service.findDetails("missing")).thenThrow(new CandidateNotFoundException());

    mockMvc
        .perform(get("/api/candidates/missing"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("CANDIDATE_NOT_FOUND"));
  }

  @Test
  void mapsStorageFailuresToSafeServiceUnavailableResponses() throws Exception {
    when(service.findPage(0))
        .thenThrow(new CandidateListReadException(new RuntimeException("secret")));
    mockMvc
        .perform(get("/api/candidates"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("CANDIDATES_UNAVAILABLE"))
        .andExpect(jsonPath("$.message").value("Candidate data could not be loaded."))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))));

    when(service.findDetails("id-1"))
        .thenThrow(new CandidateDetailsReadException(new RuntimeException("secret")));
    mockMvc
        .perform(get("/api/candidates/id-1"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("CANDIDATE_DETAILS_UNAVAILABLE"));
  }

  @Test
  void doesNotLogCandidateValuesWhenStorageReadsFail(CapturedOutput output) throws Exception {
    String candidateValues = "CANARY-NAME|canary@example.invalid|CANARY-SKILLS";
    when(service.findPage(0))
        .thenThrow(new CandidateListReadException(new RuntimeException(candidateValues)));
    when(service.findDetails("canary-id"))
        .thenThrow(new CandidateDetailsReadException(new RuntimeException(candidateValues)));

    mockMvc.perform(get("/api/candidates")).andExpect(status().isServiceUnavailable());
    mockMvc.perform(get("/api/candidates/canary-id")).andExpect(status().isServiceUnavailable());

    assertTrue(output.getAll().contains("list-storage-unavailable"));
    assertTrue(output.getAll().contains("details-storage-unavailable"));
    assertFalse(output.getAll().contains(candidateValues));
    assertFalse(output.getAll().contains("CANARY-NAME"));
    assertFalse(output.getAll().contains("canary@example.invalid"));
    assertFalse(output.getAll().contains("CANARY-SKILLS"));
  }
}
