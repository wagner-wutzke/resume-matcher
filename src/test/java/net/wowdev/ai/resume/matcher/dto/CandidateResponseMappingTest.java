package net.wowdev.ai.resume.matcher.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import net.wowdev.ai.resume.matcher.model.Candidate;
import net.wowdev.ai.resume.matcher.model.CandidateExperience;
import org.junit.jupiter.api.Test;

class CandidateResponseMappingTest {

  @Test
  void mapsOnlyApprovedSummaryFields() {
    var candidate = candidate();

    var summary = CandidateSummaryResponse.from(candidate);

    assertEquals(
        new CandidateSummaryResponse(
            "candidate-1", "Jane Doe", "Engineer", "Brazil", "São Paulo", "5 years and 0 months"),
        summary);
    assertFalse(
        List.of(summary.getClass().getRecordComponents()).stream()
            .anyMatch(component -> component.getName().equals("email")));
  }

  @Test
  void mapsStructuredDetailsAndExperiences() {
    var details = CandidateDetailsResponse.from(candidate());

    assertEquals("candidate-1", details.id());
    assertEquals("jane@example.com", details.email());
    assertEquals("Java", details.skills());
    assertEquals(
        List.of(
            new CandidateExperienceResponse(
                "Engineer", "Example Co.", "2021 – Present", "Built services", "Improved uptime")),
        details.experiences());
  }

  @Test
  void pageResponseCopiesItemsAndDefaultsNullToEmpty() {
    var item = CandidateSummaryResponse.from(candidate());

    var page = new CandidatePageResponse(List.of(item), 0, 15, 1, 1);
    var emptyPage = new CandidatePageResponse(null, 0, 15, 0, 0);

    assertEquals(List.of(item), page.items());
    assertEquals(List.of(), emptyPage.items());
    assertThrows(UnsupportedOperationException.class, () -> page.items().add(item));
  }

  private Candidate candidate() {
    return new Candidate(
        "candidate-1",
        "Jane@Example.com",
        "Jane Doe",
        "Brazil",
        "São Paulo",
        "Engineer",
        "Java",
        "Remote",
        "5 years and 0 months",
        List.of(
            new CandidateExperience(
                "Engineer", "Example Co.", "2021 – Present", "Built services", "Improved uptime")));
  }
}
