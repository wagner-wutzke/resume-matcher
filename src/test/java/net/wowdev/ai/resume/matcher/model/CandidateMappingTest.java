package net.wowdev.ai.resume.matcher.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

class CandidateMappingTest {

  @Test
  void shouldNormalizeEmailAndDefaultMissingValues() {
    Candidate candidate =
        new Candidate(
            null, "  User.Name@Example.COM ", null, null, null, null, null, null, null, null);

    assertEquals("user.name@example.com", candidate.email());
    assertEquals("", candidate.fullname());
    assertEquals("", candidate.yearsOfExperience());
    assertEquals(List.of(), candidate.experiences());
  }

  @Test
  void shouldMapCandidateCollectionAndUniqueEmailIndex() throws Exception {
    assertEquals("candidates", Candidate.class.getAnnotation(Document.class).collection());
    Indexed emailIndex = Candidate.class.getDeclaredField("email").getAnnotation(Indexed.class);

    assertTrue(emailIndex.unique());
    assertEquals("candidate_email_unique", emailIndex.name());
  }

  @Test
  void shouldKeepExperienceFieldsAsStringsWithEmptyDefaults() {
    CandidateExperience experience =
        new CandidateExperience("Engineer", null, "2020-2022", null, null);

    assertEquals("Engineer", experience.title());
    assertEquals("", experience.company());
    assertEquals("2020-2022", experience.period());
    assertEquals("", experience.responsibilities());
    assertEquals("", experience.achievements());
  }
}
