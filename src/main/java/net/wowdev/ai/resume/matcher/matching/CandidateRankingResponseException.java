package net.wowdev.ai.resume.matcher.matching;

public class CandidateRankingResponseException extends RuntimeException {

  public CandidateRankingResponseException() {
    super("Candidate ranking response is invalid");
  }
}
