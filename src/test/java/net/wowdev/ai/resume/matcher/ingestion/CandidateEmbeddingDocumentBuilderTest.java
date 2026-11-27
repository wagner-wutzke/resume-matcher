package net.wowdev.ai.resume.matcher.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.wowdev.ai.resume.matcher.model.Candidate;
import net.wowdev.ai.resume.matcher.model.CandidateExperience;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class CandidateEmbeddingDocumentBuilderTest {

  private final CandidateEmbeddingDocumentBuilder builder = new CandidateEmbeddingDocumentBuilder();

  @Test
  void shouldBuildProfileIncludingEmailAndOneDocumentPerExperience() {
    Candidate candidate =
        candidate(
            "jane@example.com",
            "Jane Doe",
            "Mexico",
            "Monterrey",
            "Developer",
            "Java, AWS",
            "Remote",
            "5 years and 0 months",
            List.of(
                new CandidateExperience(
                    "Developer", "Acme", "2020 - Present", "Built systems", "Cut costs")));

    List<Document> documents = builder.build(candidate, "jane.pdf");

    assertEquals(2, documents.size());
    assertEquals(
        "email: jane@example.com\nfullname: Jane Doe\ncountry: Mexico\ncity: Monterrey"
            + "\nposition: Developer\nskills: Java, AWS\nallocation: Remote"
            + "\nyearsOfExperience: 5 years and 0 months",
        documents.getFirst().getText());
    assertEquals(
        "title: Developer\ncompany: Acme\nperiod: 2020 - Present"
            + "\nresponsibilities: Built systems\nachievements: Cut costs",
        documents.get(1).getText());
    assertMetadata(documents.getFirst(), "Monterrey, Mexico");
    assertMetadata(documents.get(1), "Monterrey, Mexico");
  }

  @Test
  void shouldBuildOnlyProfileAndEmptyLocationWhenNoExperienceOrAddress() {
    Candidate candidate =
        candidate(
            "jane@example.com", "Jane", "", "", "", "", "", "0 years and 0 months", List.of());

    List<Document> documents = builder.build(candidate, "resume.pdf");

    assertEquals(1, documents.size());
    assertEquals("", documents.getFirst().getMetadata().get("location"));
    assertTrue(documents.getFirst().getText().contains("email: jane@example.com"));
  }

  @Test
  void shouldOmitUnavailableLocationComponent() {
    Candidate candidate =
        candidate("jane@example.com", "Jane", "Mexico", "", "", "", "", "", List.of());

    assertEquals(
        "Mexico", builder.build(candidate, "resume.pdf").getFirst().getMetadata().get("location"));
  }

  private void assertMetadata(Document document, String location) {
    assertEquals(location, document.getMetadata().get("location"));
    assertEquals("jane@example.com", document.getMetadata().get("email"));
    assertEquals("jane.pdf", document.getMetadata().get("fileName"));
    assertEquals("Developer", document.getMetadata().get("position"));
    assertEquals("5 years and 0 months", document.getMetadata().get("yearsOfExperience"));
  }

  private Candidate candidate(
      String email,
      String fullname,
      String country,
      String city,
      String position,
      String skills,
      String allocation,
      String years,
      List<CandidateExperience> experiences) {
    return new Candidate(
        null, email, fullname, country, city, position, skills, allocation, years, experiences);
  }
}
