package net.wowdev.ai.resume.matcher.matching;

public class CandidateResolutionException extends RuntimeException {

  public CandidateResolutionException(Throwable cause) {
    super("Candidate resolution failed", cause);
  }
}
