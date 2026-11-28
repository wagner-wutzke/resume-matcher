package net.wowdev.ai.resume.matcher.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import net.wowdev.ai.resume.matcher.model.Candidate;
import org.junit.jupiter.api.Test;

class CandidateRankingResponseValidatorTest {

  private final CandidateRankingResponseValidator validator =
      new CandidateRankingResponseValidator(new ObjectMapper());

  @Test
  void shouldValidateCompleteChunkAndReturnRankOrder() {
    String response =
        """
        {"items":[
          {"candidateId":"candidate-2","rank":2,"matchPercentage":100,"explanation":"Evidence: Java work. Gaps: none."},
          {"candidateId":"candidate-1","rank":1,"matchPercentage":0,"explanation":"Evidence: Spring work. Gaps: none."}
        ]}
        """;

    List<CandidateChunkRanking> result = validator.validate(response, candidates());

    assertThat(result)
        .containsExactly(
            new CandidateChunkRanking("candidate-1", 1, 0, "Evidence: Spring work. Gaps: none."),
            new CandidateChunkRanking("candidate-2", 2, 100, "Evidence: Java work. Gaps: none."));
  }

  @Test
  void shouldRejectMalformedJsonAndTextAroundTheJsonObject() {
    assertInvalid("not JSON");
    assertInvalid("Here is the ranking: {\"items\":[]}");
    assertInvalid("{\"items\":[]} trailing text");
  }

  @Test
  void shouldRejectIncompleteChunkCoverage() {
    assertInvalid(
        "{\"items\":[{\"candidateId\":\"candidate-1\",\"rank\":1,"
            + "\"explanation\":\"Evidence: Spring. Gaps: none.\"}]}");
  }

  @Test
  void shouldRejectDuplicateCandidateIds() {
    assertInvalid(
        "{\"items\":["
            + "{\"candidateId\":\"candidate-1\",\"rank\":1,\"explanation\":\"Evidence.\"},"
            + "{\"candidateId\":\"candidate-1\",\"rank\":2,\"explanation\":\"Evidence.\"}"
            + "]}");
  }

  @Test
  void shouldRejectCandidateIdFromAnotherChunk() {
    assertInvalid(
        "{\"items\":["
            + "{\"candidateId\":\"candidate-1\",\"rank\":1,\"explanation\":\"Evidence.\"},"
            + "{\"candidateId\":\"candidate-3\",\"rank\":2,\"explanation\":\"Evidence.\"}"
            + "]}");
  }

  @Test
  void shouldRejectDuplicateOrNonContiguousRanks() {
    assertInvalid(
        "{\"items\":["
            + "{\"candidateId\":\"candidate-1\",\"rank\":1,\"explanation\":\"Evidence.\"},"
            + "{\"candidateId\":\"candidate-2\",\"rank\":1,\"explanation\":\"Evidence.\"}"
            + "]}");
    assertInvalid(
        "{\"items\":["
            + "{\"candidateId\":\"candidate-1\",\"rank\":1,\"explanation\":\"Evidence.\"},"
            + "{\"candidateId\":\"candidate-2\",\"rank\":3,\"explanation\":\"Evidence.\"}"
            + "]}");
  }

  @Test
  void shouldRejectMissingExplanationAndUnexpectedFields() {
    assertInvalid(
        "{\"items\":["
            + "{\"candidateId\":\"candidate-1\",\"rank\":1,\"explanation\":\" \"},"
            + "{\"candidateId\":\"candidate-2\",\"rank\":2,\"explanation\":\"Evidence.\"}"
            + "]}");
    assertInvalid(
        "{\"items\":["
            + "{\"candidateId\":\"candidate-1\",\"rank\":1,\"explanation\":\"Evidence.\",\"score\":0.9},"
            + "{\"candidateId\":\"candidate-2\",\"rank\":2,\"explanation\":\"Evidence.\"}"
            + "]}");
  }

  @Test
  void shouldRejectMissingNonIntegerAndOutOfRangeMatchPercentages() {
    assertInvalid(
        "{\"items\":["
            + rankingItem("candidate-1", 1, null)
            + ","
            + rankingItem("candidate-2", 2, "80")
            + "]}");
    assertInvalid(
        "{\"items\":["
            + rankingItem("candidate-1", 1, "85.5")
            + ","
            + rankingItem("candidate-2", 2, "80")
            + "]}");
    assertInvalid(
        "{\"items\":["
            + rankingItem("candidate-1", 1, "-1")
            + ","
            + rankingItem("candidate-2", 2, "80")
            + "]}");
    assertInvalid(
        "{\"items\":["
            + rankingItem("candidate-1", 1, "101")
            + ","
            + rankingItem("candidate-2", 2, "80")
            + "]}");
  }

  @Test
  void shouldAcceptInclusiveMatchPercentageBounds() {
    String response =
        "{\"items\":["
            + rankingItem("candidate-1", 1, "100")
            + ","
            + rankingItem("candidate-2", 2, "0")
            + "]}";

    assertThat(validator.validate(response, candidates()))
        .extracting(CandidateChunkRanking::matchPercentage)
        .containsExactly(100, 0);
  }

  private String rankingItem(String candidateId, int rank, String matchPercentage) {
    String percentageField =
        matchPercentage == null ? "" : "\"matchPercentage\":" + matchPercentage + ",";
    return "{\"candidateId\":\""
        + candidateId
        + "\",\"rank\":"
        + rank
        + ","
        + percentageField
        + "\"explanation\":\"Evidence.\"}";
  }

  private void assertInvalid(String response) {
    assertThatThrownBy(() -> validator.validate(response, candidates()))
        .isInstanceOf(CandidateRankingResponseException.class)
        .hasMessage("Candidate ranking response is invalid");
  }

  private List<Candidate> candidates() {
    return List.of(candidate("candidate-1"), candidate("candidate-2"));
  }

  private Candidate candidate(String id) {
    return new Candidate(
        id,
        id + "@example.com",
        "Candidate " + id,
        "Brazil",
        "São Paulo",
        "Engineer",
        "Java",
        "Remote",
        "5 years and 0 months",
        List.of());
  }
}
