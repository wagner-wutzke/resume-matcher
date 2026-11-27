package net.wowdev.ai.resume.matcher.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import net.wowdev.ai.resume.matcher.model.Candidate;
import org.junit.jupiter.api.Test;

class CandidateDataValidatorTest {

  private final CandidateDataValidator validator = new CandidateDataValidator(new ObjectMapper());

  @Test
  void shouldNormalizeEmailAndMissingOptionalValues() {
    Candidate candidate = validator.validate("{\"email\":\"  Jane.Doe@Example.com \"}");

    assertEquals("jane.doe@example.com", candidate.email());
    assertEquals("", candidate.fullname());
    assertEquals("", candidate.skills());
    assertEquals("", candidate.yearsOfExperience());
    assertEquals(List.of(), candidate.experiences());
  }

  @Test
  void shouldNormalizeMissingExperienceFields() {
    Candidate candidate =
        validator.validate(
            "{\"email\":\"jane@example.com\",\"experiences\":[{\"title\":\"Engineer\"}]}");

    assertEquals("Engineer", candidate.experiences().getFirst().title());
    assertEquals("", candidate.experiences().getFirst().company());
    assertEquals("", candidate.experiences().getFirst().achievements());
  }

  @Test
  void shouldRejectMalformedJsonAndTrailingContent() {
    assertInvalid("{");
    assertInvalid("{\"email\":\"jane@example.com\"} {\"email\":\"other@example.com\"}");
  }

  @Test
  void shouldRejectMissingOrInvalidEmail() {
    assertInvalid("{}");
    assertInvalid("{\"email\":\"not-an-email\"}");
    assertInvalid("{\"email\":\".john@example.com\"}");
    assertInvalid("{\"email\":\"john.@example.com\"}");
    assertInvalid("{\"email\":\"john..doe@example.com\"}");
  }

  @Test
  void shouldRejectWrongTypesUnknownFieldsAndInvalidExperienceEntries() {
    assertInvalid("{\"email\":\"jane@example.com\",\"city\":7}");
    assertInvalid("{\"email\":\"jane@example.com\",\"other\":\"value\"}");
    assertInvalid("{\"email\":\"jane@example.com\",\"experiences\":[\"Engineer\"]}");
    assertInvalid("{\"email\":\"jane@example.com\",\"experiences\":[{\"title\":false}]}");
  }

  private void assertInvalid(String json) {
    assertThrows(ResumeIngestionException.class, () -> validator.validate(json));
  }
}
