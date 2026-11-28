package net.wowdev.ai.resume.matcher.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import net.wowdev.ai.resume.matcher.dto.CandidateMatchItemResponse;
import net.wowdev.ai.resume.matcher.dto.CandidateMatchResponse;
import net.wowdev.ai.resume.matcher.dto.CandidateSummaryResponse;
import net.wowdev.ai.resume.matcher.matching.CandidateMatchService;
import net.wowdev.ai.resume.matcher.matching.CandidateMatchingUnavailableException;
import net.wowdev.ai.resume.matcher.service.JobOfferNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CandidateMatchControllerTest {

  private final CandidateMatchService service = mock(CandidateMatchService.class);
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new CandidateMatchController(service))
            .setControllerAdvice(new CandidateMatchExceptionHandler())
            .build();
  }

  @Test
  void returnsRankedMatchesForPosition() throws Exception {
    when(service.findMatches("position-1"))
        .thenReturn(
            new CandidateMatchResponse(
                "position-1",
                List.of(
                    new CandidateMatchItemResponse(
                        1,
                        85,
                        new CandidateSummaryResponse(
                            "candidate-1",
                            "Jane Doe",
                            "Engineer",
                            "Brazil",
                            "São Paulo",
                            "5 years"),
                        "Evidence: Java experience. Gaps: none established."))));

    mockMvc
        .perform(post("/api/job-offers/position-1/matches"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.positionId").value("position-1"))
        .andExpect(jsonPath("$.items[0].rank").value(1))
        .andExpect(jsonPath("$.items[0].matchPercentage").value(85))
        .andExpect(jsonPath("$.items[0].candidate.id").value("candidate-1"))
        .andExpect(jsonPath("$.items[0].candidate.fullname").value("Jane Doe"))
        .andExpect(
            jsonPath("$.items[0].explanation")
                .value("Evidence: Java experience. Gaps: none established."));

    verify(service).findMatches("position-1");
  }

  @Test
  void returnsEmptySuccessfulResult() throws Exception {
    when(service.findMatches("position-1"))
        .thenReturn(new CandidateMatchResponse("position-1", List.of()));

    mockMvc
        .perform(post("/api/job-offers/position-1/matches"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items").isEmpty());
  }

  @Test
  void returnsGenericNotFoundForMissingPosition() throws Exception {
    when(service.findMatches("missing")).thenThrow(new JobOfferNotFoundException("sensitive-id"));

    mockMvc
        .perform(post("/api/job-offers/missing/matches"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("JOB_OFFER_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("The job offer was not found."));
  }

  @Test
  void returnsGenericUnavailableResponseWithoutDependencyDetails() throws Exception {
    when(service.findMatches("position-1"))
        .thenThrow(new CandidateMatchingUnavailableException(new IllegalStateException("secret")));

    mockMvc
        .perform(post("/api/job-offers/position-1/matches"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("CANDIDATE_MATCHING_UNAVAILABLE"))
        .andExpect(jsonPath("$.message").value("Candidate matching is temporarily unavailable."));
  }
}
