package net.wowdev.ai.resume.matcher.matching;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.wowdev.ai.resume.matcher.model.Candidate;
import net.wowdev.ai.resume.matcher.repository.CandidateRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class CandidatePoolResolver {

  private final CandidateRepository candidateRepository;
  private final CandidateMatchingProperties properties;

  public CandidatePoolResolver(
      CandidateRepository candidateRepository, CandidateMatchingProperties properties) {
    this.candidateRepository = candidateRepository;
    this.properties = properties;
  }

  public List<Candidate> resolve(Set<String> retrievedEmails) {
    if (retrievedEmails == null || retrievedEmails.isEmpty()) {
      return List.of();
    }

    Set<String> normalizedEmails = new TreeSet<>();
    for (String email : retrievedEmails) {
      String normalizedEmail = Candidate.normalizeEmail(email);
      if (!normalizedEmail.isBlank()) {
        normalizedEmails.add(normalizedEmail);
      }
    }

    List<Candidate> candidates = new ArrayList<>();
    Set<String> resolvedEmails = new HashSet<>();
    for (String email : normalizedEmails) {
      if (candidates.size() >= properties.getPoolCap()) {
        break;
      }
      Optional<Candidate> candidate = findCandidate(email);
      candidate.ifPresent(
          resolved -> {
            String normalizedCandidateEmail = Candidate.normalizeEmail(resolved.email());
            if (!normalizedCandidateEmail.isBlank()
                && resolvedEmails.add(normalizedCandidateEmail)) {
              candidates.add(resolved);
            }
          });
    }
    return List.copyOf(candidates);
  }

  private Optional<Candidate> findCandidate(String email) {
    try {
      return candidateRepository.findByEmail(email);
    } catch (DataAccessException exception) {
      throw new CandidateResolutionException(exception);
    }
  }
}
