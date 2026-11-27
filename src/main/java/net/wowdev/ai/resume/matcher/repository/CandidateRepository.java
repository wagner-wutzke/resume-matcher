package net.wowdev.ai.resume.matcher.repository;

import java.util.Optional;
import net.wowdev.ai.resume.matcher.model.Candidate;

public interface CandidateRepository {

  Optional<Candidate> findByEmail(String email);

  Candidate replaceByEmail(Candidate candidate);

  void deleteByEmail(String email);
}
