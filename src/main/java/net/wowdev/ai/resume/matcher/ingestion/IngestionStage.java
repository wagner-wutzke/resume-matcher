package net.wowdev.ai.resume.matcher.ingestion;

enum IngestionStage {
  DISCOVERY("discovery", ResumeIngestionException.FailureCause.PROCESSING_FAILED),
  QUEUE("queue", ResumeIngestionException.FailureCause.PROCESSING_FAILED),
  PROCESSING("processing", ResumeIngestionException.FailureCause.PROCESSING_FAILED),
  READINESS("readiness", ResumeIngestionException.FailureCause.FILE_READINESS_FAILED),
  PDF_EXTRACTION("pdf-extraction", ResumeIngestionException.FailureCause.PDF_UNREADABLE),
  TEXT_CLEANUP("text-cleanup", ResumeIngestionException.FailureCause.PROCESSING_FAILED),
  LLM_EXTRACTION("llm-extraction", ResumeIngestionException.FailureCause.LLM_UNAVAILABLE),
  VALIDATION("validation", ResumeIngestionException.FailureCause.INVALID_CANDIDATE_DATA),
  EXPERIENCE_CALCULATION(
      "experience-calculation", ResumeIngestionException.FailureCause.PROCESSING_FAILED),
  MONGO_SNAPSHOT("mongo-snapshot", ResumeIngestionException.FailureCause.MONGODB_UNAVAILABLE),
  QDRANT_SNAPSHOT("qdrant-snapshot", ResumeIngestionException.FailureCause.QDRANT_UNAVAILABLE),
  MONGO_PERSISTENCE(
      "mongo-persistence", ResumeIngestionException.FailureCause.MONGODB_UNAVAILABLE),
  QDRANT_WRITE("qdrant-write", ResumeIngestionException.FailureCause.QDRANT_UNAVAILABLE),
  ROLLBACK("rollback", ResumeIngestionException.FailureCause.ROLLBACK_FAILED),
  FILE_MOVEMENT_HANDLED(
      "file-movement-handled", ResumeIngestionException.FailureCause.FILE_MOVE_FAILED),
  FILE_MOVEMENT_ERROR(
      "file-movement-error", ResumeIngestionException.FailureCause.FILE_MOVE_FAILED),
  OVERALL("overall", ResumeIngestionException.FailureCause.PROCESSING_FAILED);

  private final String code;
  private final ResumeIngestionException.FailureCause fallbackCause;

  IngestionStage(String code, ResumeIngestionException.FailureCause fallbackCause) {
    this.code = code;
    this.fallbackCause = fallbackCause;
  }

  String code() {
    return code;
  }

  ResumeIngestionException.FailureCause fallbackCause() {
    return fallbackCause;
  }
}
