package net.wowdev.ai.resume.matcher.dto;

public record CandidateMatchErrorResponse(String code, String message) {

  public static final String POSITION_NOT_FOUND_CODE = "JOB_OFFER_NOT_FOUND";
  public static final String MATCHING_UNAVAILABLE_CODE = "CANDIDATE_MATCHING_UNAVAILABLE";
}
