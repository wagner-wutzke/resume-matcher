package net.wowdev.ai.resume.matcher.repository;

import net.wowdev.ai.resume.matcher.model.Candidate;
import net.wowdev.ai.resume.matcher.model.CandidateVectorSnapshot;

public interface CandidateVectorRepository {

  void replaceByEmail(Candidate candidate, String sourceFileName);

  void deleteByEmail(String email);

  CandidateVectorSnapshot snapshotByEmail(String email);

  void restoreByEmail(String email, CandidateVectorSnapshot snapshot);
}
