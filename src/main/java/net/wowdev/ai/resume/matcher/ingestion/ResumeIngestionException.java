package net.wowdev.ai.resume.matcher.ingestion;

public class ResumeIngestionException extends RuntimeException {

  public enum FailureCause {
    READINESS_TIMEOUT("readiness-timeout"),
    FILE_READINESS_FAILED("file-readiness-failed"),
    PDF_UNREADABLE("pdf-unreadable"),
    NO_EXTRACTABLE_TEXT("no-extractable-text"),
    LLM_UNAVAILABLE("llm-unavailable"),
    INVALID_CANDIDATE_DATA("invalid-candidate-data"),
    MONGODB_UNAVAILABLE("mongodb-unavailable"),
    QDRANT_UNAVAILABLE("qdrant-unavailable"),
    FILE_MOVE_FAILED("file-move-failed"),
    ROLLBACK_FAILED("rollback-failed"),
    PROCESSING_FAILED("processing-failed");

    private final String code;

    FailureCause(String code) {
      this.code = code;
    }

    public String code() {
      return code;
    }
  }

  private final FailureCause failureCause;

  public ResumeIngestionException(String message) {
    this(message, FailureCause.PROCESSING_FAILED);
  }

  public ResumeIngestionException(String message, FailureCause failureCause) {
    super(message);
    this.failureCause = failureCause;
  }

  public FailureCause failureCause() {
    return failureCause;
  }
}
