package net.wowdev.ai.resume.matcher.matching;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import net.wowdev.ai.resume.matcher.model.Candidate;
import net.wowdev.ai.resume.matcher.model.CandidateExperience;
import net.wowdev.ai.resume.matcher.model.JobOffer;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class CandidateMatchPromptBuilderTest {

  private final CandidateMatchPromptBuilder promptBuilder =
      new CandidateMatchPromptBuilder(
          new ObjectMapper(), new ClassPathResource("prompts/rank-candidates.st"));

  @Test
  void shouldIncludePositionAndStructuredCandidateDataWithoutEmail() {
    String prompt = promptBuilder.build(position(), List.of(candidate()));

    assertThat(prompt)
        .contains("requiredQualifications")
        .contains("Cloud platform engineer")
        .contains("Software Engineer")
        .contains("Java, Spring")
        .contains("Built reliable services")
        .contains("candidate-1")
        .contains("integer matchPercentage from 0 through 100")
        .contains("not a probability")
        .contains("must not affect rank")
        .doesNotContain("jane@example.com")
        .doesNotContain("similarityScore")
        .doesNotContain("\"score\"");
  }

  private JobOffer position() {
    return new JobOffer(
        "position-1",
        "JD-00000001",
        "Example Co.",
        "Software Engineer",
        "Build backend services",
        "Cloud platform engineer",
        5,
        "Remote",
        "Remote");
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
                "Engineer",
                "Example Co.",
                "2021–Present",
                "Built reliable services",
                "Improved uptime")));
  }
}
