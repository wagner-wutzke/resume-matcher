package net.wowdev.ai.resume.matcher.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.wowdev.ai.resume.matcher.model.Candidate;
import net.wowdev.ai.resume.matcher.repository.CandidateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class CandidatePoolResolverTest {

  private final CandidateRepository candidateRepository = mock(CandidateRepository.class);
  private final CandidatePoolResolver resolver =
      new CandidatePoolResolver(
          candidateRepository, new CandidateMatchingProperties("10", "2", "10"));

  @Test
  void shouldNormalizeAndDeduplicateEmailsBeforeResolving() {
    Candidate candidate = candidate("jane@example.com");
    when(candidateRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(candidate));

    List<Candidate> result = resolver.resolve(Set.of("Jane@Example.com", " jane@example.com "));

    assertThat(result).containsExactly(candidate);
    verify(candidateRepository).findByEmail("jane@example.com");
  }

  @Test
  void shouldSkipBlankEmailsAndStaleVectorHits() {
    when(candidateRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

    List<Candidate> result = resolver.resolve(Set.of(" ", "missing@example.com"));

    assertThat(result).isEmpty();
    verify(candidateRepository).findByEmail("missing@example.com");
    verify(candidateRepository, never()).findByEmail("");
  }

  @Test
  void shouldReturnEmptyWithoutRepositoryReadsWhenNoEmailsWereRetrieved() {
    assertThat(resolver.resolve(Set.of())).isEmpty();
    assertThat(resolver.resolve(null)).isEmpty();

    verify(candidateRepository, never()).findByEmail(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void shouldEnforcePoolCapInDeterministicEmailOrder() {
    Candidate first = candidate("a@example.com");
    Candidate second = candidate("b@example.com");
    when(candidateRepository.findByEmail("a@example.com")).thenReturn(Optional.of(first));
    when(candidateRepository.findByEmail("b@example.com")).thenReturn(Optional.of(second));

    List<Candidate> result =
        resolver.resolve(Set.of("c@example.com", "b@example.com", "a@example.com"));

    assertThat(result).containsExactly(first, second);
    verify(candidateRepository).findByEmail("a@example.com");
    verify(candidateRepository).findByEmail("b@example.com");
    verify(candidateRepository, never()).findByEmail("c@example.com");
  }

  @Test
  void shouldSkipUnresolvedHitsAndContinueUntilPoolCapIsFilled() {
    Candidate candidate = candidate("b@example.com");
    when(candidateRepository.findByEmail("a@example.com")).thenReturn(Optional.empty());
    when(candidateRepository.findByEmail("b@example.com")).thenReturn(Optional.of(candidate));
    when(candidateRepository.findByEmail("c@example.com"))
        .thenReturn(Optional.of(candidate("c@example.com")));

    List<Candidate> result =
        resolver.resolve(Set.of("a@example.com", "b@example.com", "c@example.com"));

    assertThat(result)
        .extracting(Candidate::email)
        .containsExactly("b@example.com", "c@example.com");
  }

  @Test
  void shouldWrapMongoReadFailures() {
    when(candidateRepository.findByEmail("jane@example.com"))
        .thenThrow(new DataAccessResourceFailureException("database unavailable"));

    assertThatThrownBy(() -> resolver.resolve(Set.of("jane@example.com")))
        .isInstanceOf(CandidateResolutionException.class)
        .hasMessage("Candidate resolution failed");
  }

  private Candidate candidate(String email) {
    return new Candidate(
        "candidate-" + email,
        email,
        "Jane Doe",
        "Brazil",
        "São Paulo",
        "Engineer",
        "Java",
        "Remote",
        "5 years and 0 months",
        List.of());
  }
}
