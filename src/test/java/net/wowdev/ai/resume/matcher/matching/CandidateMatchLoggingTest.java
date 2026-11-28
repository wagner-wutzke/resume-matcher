package net.wowdev.ai.resume.matcher.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.wowdev.ai.resume.matcher.model.Candidate;
import net.wowdev.ai.resume.matcher.model.JobOffer;
import net.wowdev.ai.resume.matcher.repository.CandidateRepository;
import net.wowdev.ai.resume.matcher.repository.JobOfferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class CandidateMatchLoggingTest {

  @Test
  void shouldLogSuccessStagesWithoutCandidatePositionOrModelData(CapturedOutput output) {
    JobOfferRepository jobOfferRepository = mock(JobOfferRepository.class);
    CandidateSemanticRetriever semanticRetriever = mock(CandidateSemanticRetriever.class);
    CandidateRepository candidateRepository = mock(CandidateRepository.class);
    CandidateMatchRanker ranker = mock(CandidateMatchRanker.class);
    CandidateMatchingProperties properties = new CandidateMatchingProperties("10", "50", "10");
    CandidatePoolResolver resolver = new CandidatePoolResolver(candidateRepository, properties);
    CandidateMatchAggregator aggregator = new CandidateMatchAggregator(properties);
    JobOffer position =
        new JobOffer(
            "POSITION_ID_SECRET",
            "JD-00000001",
            "POSITION_COMPANY_SECRET",
            "POSITION_TITLE_SECRET",
            "POSITION_DESCRIPTION_SECRET",
            "POSITION_REQUIREMENT_SECRET",
            5,
            "POSITION_LOCATION_SECRET",
            "POSITION_ALLOCATION_SECRET");
    Candidate candidate =
        new Candidate(
            "CANDIDATE_ID_SECRET",
            "EMAIL_SECRET@example.com",
            "CANDIDATE_NAME_SECRET",
            "COUNTRY_SECRET",
            "CITY_SECRET",
            "CANDIDATE_POSITION_SECRET",
            "CANDIDATE_SKILLS_SECRET",
            "REMOTE_SECRET",
            "5 years",
            List.of());
    when(jobOfferRepository.findById("POSITION_ID_SECRET")).thenReturn(Optional.of(position));
    when(semanticRetriever.retrieveCandidateEmails(any()))
        .thenReturn(Set.of("EMAIL_SECRET@example.com"));
    when(candidateRepository.findByEmail("email_secret@example.com"))
        .thenReturn(Optional.of(candidate));
    when(ranker.rankChunks(position, List.of(candidate)))
        .thenReturn(
            List.of(
                List.of(
                    new CandidateChunkRanking(
                        "CANDIDATE_ID_SECRET", 1, 90, "MODEL_EXPLANATION_SECRET"))));

    CandidateMatchService service =
        new CandidateMatchService(
            jobOfferRepository, semanticRetriever, resolver, ranker, aggregator);
    service.findMatches("POSITION_ID_SECRET");

    assertThat(output.getAll())
        .contains("stage=position-load outcome=completed")
        .contains("stage=semantic-retrieval outcome=completed")
        .contains("stage=candidate-resolution outcome=completed")
        .contains("stage=chunk-ranking outcome=completed")
        .contains("stage=result-aggregation outcome=completed")
        .contains("stage=response outcome=completed");
    assertThat(output.getAll())
        .doesNotContain(
            "POSITION_ID_SECRET",
            "POSITION_COMPANY_SECRET",
            "POSITION_TITLE_SECRET",
            "POSITION_DESCRIPTION_SECRET",
            "POSITION_REQUIREMENT_SECRET",
            "POSITION_LOCATION_SECRET",
            "POSITION_ALLOCATION_SECRET",
            "CANDIDATE_ID_SECRET",
            "EMAIL_SECRET",
            "CANDIDATE_NAME_SECRET",
            "CANDIDATE_SKILLS_SECRET",
            "MODEL_EXPLANATION_SECRET");
  }

  @Test
  void shouldLogSanitizedFailureCategoryWithoutProviderDetails(CapturedOutput output) {
    JobOfferRepository jobOfferRepository = mock(JobOfferRepository.class);
    CandidateSemanticRetriever semanticRetriever = mock(CandidateSemanticRetriever.class);
    CandidateRepository candidateRepository = mock(CandidateRepository.class);
    CandidateMatchRanker ranker = mock(CandidateMatchRanker.class);
    CandidateMatchingProperties properties = new CandidateMatchingProperties("10", "50", "10");
    when(jobOfferRepository.findById("POSITION_ID_SECRET"))
        .thenReturn(
            Optional.of(
                new JobOffer(
                    "POSITION_ID_SECRET", null, null, null, null, null, null, null, null)));
    when(semanticRetriever.retrieveCandidateEmails(any()))
        .thenThrow(new IllegalStateException("PROVIDER_RESPONSE_SECRET"));

    CandidateMatchService service =
        new CandidateMatchService(
            jobOfferRepository,
            semanticRetriever,
            new CandidatePoolResolver(candidateRepository, properties),
            ranker,
            new CandidateMatchAggregator(properties));

    try {
      service.findMatches("POSITION_ID_SECRET");
    } catch (CandidateMatchingUnavailableException ignored) {
      // Expected dependency failure; only sanitized logging is under test.
    }

    assertThat(output.getAll())
        .contains("stage=semantic-retrieval outcome=failed category=retrieval-unavailable")
        .doesNotContain("POSITION_ID_SECRET", "PROVIDER_RESPONSE_SECRET");
  }
}
