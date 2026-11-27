package net.wowdev.ai.resume.matcher.repository;

import java.util.List;
import java.util.Optional;
import net.wowdev.ai.resume.matcher.dto.CandidateSummaryResponse;
import net.wowdev.ai.resume.matcher.model.Candidate;

public interface CandidateRepository {

  Optional<Candidate> findByEmail(String email);

  Candidate replaceByEmail(Candidate candidate);

  void deleteByEmail(String email);

  Optional<Candidate> findById(String id);

  List<CandidateSummaryResponse> findSummaryPage(int page);

  long count();
}
