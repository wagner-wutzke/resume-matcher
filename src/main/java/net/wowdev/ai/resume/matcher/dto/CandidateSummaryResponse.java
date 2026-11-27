package net.wowdev.ai.resume.matcher.dto;

import net.wowdev.ai.resume.matcher.model.Candidate;

public record CandidateSummaryResponse(
    String id,
    String fullname,
    String position,
    String country,
    String city,
    String yearsOfExperience) {

  public static CandidateSummaryResponse from(Candidate candidate) {
    return new CandidateSummaryResponse(
        candidate.id(),
        candidate.fullname(),
        candidate.position(),
        candidate.country(),
        candidate.city(),
        candidate.yearsOfExperience());
  }
}
