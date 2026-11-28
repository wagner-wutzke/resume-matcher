package net.wowdev.ai.resume.matcher.matching;

public class CandidateMatchingUnavailableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public CandidateMatchingUnavailableException(Throwable cause) {
    super("Candidate matching is unavailable", cause);
  }
}
