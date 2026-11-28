package net.wowdev.ai.resume.matcher.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.wowdev.ai.resume.matcher.dto.CandidateMatchResponse;
import net.wowdev.ai.resume.matcher.model.Candidate;
import net.wowdev.ai.resume.matcher.model.JobOffer;
import net.wowdev.ai.resume.matcher.repository.CandidateRepository;
import net.wowdev.ai.resume.matcher.repository.JobOfferRepository;
import net.wowdev.ai.resume.matcher.service.JobOfferNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class CandidateMatchServiceTest {

  private final JobOfferRepository jobOfferRepository = mock(JobOfferRepository.class);
  private final CandidateSemanticRetriever semanticRetriever =
      mock(CandidateSemanticRetriever.class);
  private final CandidateRepository candidateRepository = mock(CandidateRepository.class);
  private final CandidateMatchRanker ranker = mock(CandidateMatchRanker.class);
  private final CandidateMatchingProperties properties =
      new CandidateMatchingProperties("10", "50", "10");
  private final CandidatePoolResolver poolResolver =
      new CandidatePoolResolver(candidateRepository, properties);
  private final CandidateMatchAggregator aggregator = new CandidateMatchAggregator(properties);
  private final CandidateMatchService service =
      new CandidateMatchService(
          jobOfferRepository, semanticRetriever, poolResolver, ranker, aggregator);

  @Test
  void shouldLoadPositionRetrieveResolveRankAggregateAndReturnSummaries() {
    JobOffer position = position();
    Candidate candidate = candidate("candidate-1", "candidate@example.com");
    when(jobOfferRepository.findById("position-1")).thenReturn(Optional.of(position));
    when(semanticRetriever.retrieveCandidateEmails(any()))
        .thenReturn(Set.of("candidate@example.com"));
    when(candidateRepository.findByEmail("candidate@example.com"))
        .thenReturn(Optional.of(candidate));
    when(ranker.rankChunks(position, List.of(candidate)))
        .thenReturn(
            List.of(
                List.of(
                    new CandidateChunkRanking(
                        "candidate-1", 1, 85, "Evidence: Java. Gaps: none."))));

    CandidateMatchResponse response = service.findMatches("position-1");

    assertThat(response.positionId()).isEqualTo("position-1");
    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().rank()).isEqualTo(1);
    assertThat(response.items().getFirst().matchPercentage()).isEqualTo(85);
    assertThat(response.items().getFirst().candidate().id()).isEqualTo("candidate-1");
    assertThat(response.items().getFirst().explanation()).isEqualTo("Evidence: Java. Gaps: none.");
    verify(semanticRetriever)
        .retrieveCandidateEmails(
            "Software Engineer Example Co. Build backend services Java and Spring 5 Remote Full-time");
    verify(candidateRepository, never()).replaceByEmail(any());
    verify(candidateRepository, never()).deleteByEmail(any());
    verify(jobOfferRepository, never()).save(any());
    verify(jobOfferRepository, never()).deleteById(any());
  }

  @Test
  void shouldReturnEmptyItemsAndSkipRankerWhenNoCandidateResolves() {
    when(jobOfferRepository.findById("position-1")).thenReturn(Optional.of(position()));
    when(semanticRetriever.retrieveCandidateEmails(any())).thenReturn(Set.of());

    CandidateMatchResponse response = service.findMatches("position-1");

    assertThat(response.items()).isEmpty();
    verifyNoInteractions(ranker);
  }

  @Test
  void shouldFailNotFoundBeforeRetrievalWhenPositionIsMissing() {
    when(jobOfferRepository.findById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findMatches("missing"))
        .isInstanceOf(JobOfferNotFoundException.class);

    verifyNoInteractions(semanticRetriever, candidateRepository, ranker);
  }

  @Test
  void shouldReportPositionReadFailureAsMatchingUnavailable() {
    when(jobOfferRepository.findById("position-1"))
        .thenThrow(new DataAccessResourceFailureException("database unavailable"));

    assertThatThrownBy(() -> service.findMatches("position-1"))
        .isInstanceOf(CandidateMatchingUnavailableException.class)
        .hasMessage("Candidate matching is unavailable");

    verifyNoInteractions(semanticRetriever, candidateRepository, ranker);
  }

  @Test
  void shouldReportRetrievalFailureAsMatchingUnavailable() {
    when(jobOfferRepository.findById("position-1")).thenReturn(Optional.of(position()));
    when(semanticRetriever.retrieveCandidateEmails(any()))
        .thenThrow(new IllegalStateException("provider detail"));

    assertThatThrownBy(() -> service.findMatches("position-1"))
        .isInstanceOf(CandidateMatchingUnavailableException.class)
        .hasMessage("Candidate matching is unavailable");
  }

  @Test
  void shouldReportCandidateResolutionFailureAsMatchingUnavailable() {
    when(jobOfferRepository.findById("position-1")).thenReturn(Optional.of(position()));
    when(semanticRetriever.retrieveCandidateEmails(any()))
        .thenReturn(Set.of("candidate@example.com"));
    when(candidateRepository.findByEmail("candidate@example.com"))
        .thenThrow(new DataAccessResourceFailureException("database unavailable"));

    assertThatThrownBy(() -> service.findMatches("position-1"))
        .isInstanceOf(CandidateMatchingUnavailableException.class)
        .hasMessage("Candidate matching is unavailable");

    verifyNoInteractions(ranker);
  }

  @Test
  void shouldReportRankingFailureAsMatchingUnavailable() {
    JobOffer position = position();
    Candidate candidate = candidate("candidate-1", "candidate@example.com");
    when(jobOfferRepository.findById("position-1")).thenReturn(Optional.of(position));
    when(semanticRetriever.retrieveCandidateEmails(any()))
        .thenReturn(Set.of("candidate@example.com"));
    when(candidateRepository.findByEmail("candidate@example.com"))
        .thenReturn(Optional.of(candidate));
    when(ranker.rankChunks(position, List.of(candidate)))
        .thenThrow(new CandidateChunkRankingException());

    assertThatThrownBy(() -> service.findMatches("position-1"))
        .isInstanceOf(CandidateMatchingUnavailableException.class)
        .hasMessage("Candidate matching is unavailable");
  }

  private JobOffer position() {
    return new JobOffer(
        "position-1",
        "JD-00000001",
        "Example Co.",
        "Software Engineer",
        "Build backend services",
        "Java and Spring",
        5,
        "Remote",
        "Full-time");
  }

  private Candidate candidate(String id, String email) {
    return new Candidate(
        id,
        email,
        "Candidate One",
        "Brazil",
        "São Paulo",
        "Engineer",
        "Java",
        "Remote",
        "5 years and 0 months",
        List.of());
  }
}
