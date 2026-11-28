package net.wowdev.ai.resume.matcher.dto;

import net.wowdev.ai.resume.matcher.model.Candidate;

public record CandidateMatchItemResponse(
    int rank, int matchPercentage, CandidateSummaryResponse candidate, String explanation) {

  public static CandidateMatchItemResponse from(
      int rank, int matchPercentage, Candidate candidate, String explanation) {
    return new CandidateMatchItemResponse(
        rank, matchPercentage, CandidateSummaryResponse.from(candidate), explanation);
  }
}
