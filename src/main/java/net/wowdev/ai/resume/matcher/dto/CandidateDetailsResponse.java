package net.wowdev.ai.resume.matcher.dto;

import java.util.List;
import net.wowdev.ai.resume.matcher.model.Candidate;

public record CandidateDetailsResponse(
    String id,
    String email,
    String fullname,
    String country,
    String city,
    String position,
    String skills,
    String allocation,
    String yearsOfExperience,
    List<CandidateExperienceResponse> experiences) {

  public CandidateDetailsResponse {
    experiences = experiences == null ? List.of() : List.copyOf(experiences);
  }

  public static CandidateDetailsResponse from(Candidate candidate) {
    return new CandidateDetailsResponse(
        candidate.id(),
        candidate.email(),
        candidate.fullname(),
        candidate.country(),
        candidate.city(),
        candidate.position(),
        candidate.skills(),
        candidate.allocation(),
        candidate.yearsOfExperience(),
        candidate.experiences().stream().map(CandidateExperienceResponse::from).toList());
  }
}
