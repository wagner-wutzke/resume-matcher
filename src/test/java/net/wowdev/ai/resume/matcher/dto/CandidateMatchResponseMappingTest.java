package net.wowdev.ai.resume.matcher.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import net.wowdev.ai.resume.matcher.model.Candidate;
import net.wowdev.ai.resume.matcher.model.CandidateExperience;
import org.junit.jupiter.api.Test;

class CandidateMatchResponseMappingTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldMapCandidateToMatchSummaryWithoutSensitiveFields() throws Exception {
    Candidate candidate = candidate();
    CandidateMatchResponse response =
        new CandidateMatchResponse(
            "position-1",
            List.of(
                CandidateMatchItemResponse.from(1, 85, candidate, "Evidence: Java experience.")));

    JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));
    JsonNode item = json.path("items").get(0);

    assertThat(json.path("positionId").asText()).isEqualTo("position-1");
    assertThat(item.path("rank").asInt()).isEqualTo(1);
    assertThat(item.path("matchPercentage").asInt()).isEqualTo(85);
    assertThat(item.path("candidate").path("id").asText()).isEqualTo("candidate-1");
    assertThat(item.path("candidate").path("fullname").asText()).isEqualTo("Jane Doe");
    assertThat(item.path("explanation").asText()).isEqualTo("Evidence: Java experience.");
    assertThat(item.path("candidate").has("email")).isFalse();
    assertThat(item.path("candidate").has("skills")).isFalse();
    assertThat(item.has("resumeText")).isFalse();
    assertThat(item.has("vector")).isFalse();
  }

  @Test
  void shouldCopyItemsAndDefaultNullItemsToEmpty() {
    CandidateMatchItemResponse item =
        CandidateMatchItemResponse.from(1, 85, candidate(), "Evidence: Java experience.");
    CandidateMatchResponse response = new CandidateMatchResponse("position-1", List.of(item));
    CandidateMatchResponse emptyResponse = new CandidateMatchResponse("position-1", null);

    assertThat(response.items()).containsExactly(item);
    assertThat(emptyResponse.items()).isEmpty();
    assertThrows(UnsupportedOperationException.class, () -> response.items().add(item));
  }

  @Test
  void shouldSerializeStableGenericErrorFields() throws Exception {
    CandidateMatchErrorResponse error =
        new CandidateMatchErrorResponse(
            CandidateMatchErrorResponse.MATCHING_UNAVAILABLE_CODE,
            "Candidate matching is unavailable.");

    JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(error));

    assertThat(json.size()).isEqualTo(2);
    assertThat(json.has("code")).isTrue();
    assertThat(json.has("message")).isTrue();
    assertThat(json.path("code").asText())
        .isEqualTo(CandidateMatchErrorResponse.MATCHING_UNAVAILABLE_CODE);
    assertThat(json.path("message").asText()).isEqualTo("Candidate matching is unavailable.");
  }

  private Candidate candidate() {
    return new Candidate(
        "candidate-1",
        "jane@example.com",
        "Jane Doe",
        "Brazil",
        "São Paulo",
        "Software Engineer",
        "Java, Spring",
        "Remote",
        "5 years and 0 months",
        List.of(
            new CandidateExperience(
                "Engineer", "Example Co.", "2021 – Present", "Built services", "Improved uptime")));
  }
}
