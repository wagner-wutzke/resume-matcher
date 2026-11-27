package net.wowdev.ai.resume.matcher.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class ResumeIngestionStageLoggerTest {

  @Test
  void shouldLogStagesAndFailuresWithoutCandidateData() {
    Logger logger = (Logger) LoggerFactory.getLogger(ResumeIngestionStageLogger.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    ResumeIngestionStageLogger stageLogger = new ResumeIngestionStageLogger();

    try {
      stageLogger.overallStarted(Path.of("private-resume.pdf"));
      stageLogger.execute(
          Path.of("private-resume.pdf"), IngestionStage.VALIDATION, () -> "valid");
      org.junit.jupiter.api.Assertions.assertThrows(
          ResumeIngestionException.class,
          () ->
              stageLogger.execute(
                  Path.of("private-resume.pdf"),
                  IngestionStage.LLM_EXTRACTION,
                  () -> {
                    throw new ResumeIngestionException("do-not-log-this-resume-text");
                  }));
      stageLogger.skipped(Path.of("private-resume.pdf"), IngestionStage.QDRANT_WRITE);
      stageLogger.manualRecoveryRequired(Path.of("private-resume.pdf"));

      String logs =
          appender.list.stream()
              .map(ILoggingEvent::getFormattedMessage)
              .reduce("", (left, right) -> left + "\n" + right);
      assertTrue(logs.contains("file=private-resume.pdf"));
      assertTrue(logs.contains("stage=overall outcome=started"));
      assertTrue(logs.contains("stage=validation"));
      assertTrue(logs.contains("elapsedMs="));
      assertTrue(logs.contains("outcome=skipped"));
      assertTrue(logs.contains("manualRecoveryRequired=true"));
      assertTrue(logs.contains("cause=processing-failed"));
      assertTrue(
          appender.list.stream()
              .anyMatch(
                  event ->
                      event.getLevel() == Level.WARN
                          && event.getFormattedMessage().contains("cause=processing-failed")));
      assertTrue(
          appender.list.stream()
              .anyMatch(
                  event ->
                      event.getLevel() == Level.ERROR
                          && event.getFormattedMessage().contains("cause=rollback-failed")));
      assertFalse(logs.contains("do-not-log-this-resume-text"));
      assertFalse(logs.contains("jane@example.com"));
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }

  @Test
  void shouldMapFailuresToFixedSanitizedCauseCategories() {
    assertEquals(
        "readiness-timeout",
        ResumeIngestionStageLogger.safeCause(
            failure(ResumeIngestionException.FailureCause.READINESS_TIMEOUT),
            IngestionStage.READINESS));
    assertEquals(
        "pdf-unreadable",
        ResumeIngestionStageLogger.safeCause(
            new IllegalStateException("sensitive exception text"), IngestionStage.PDF_EXTRACTION));
    assertEquals(
        "no-extractable-text",
        ResumeIngestionStageLogger.safeCause(
            failure(ResumeIngestionException.FailureCause.NO_EXTRACTABLE_TEXT),
            IngestionStage.PDF_EXTRACTION));
    assertEquals(
        "llm-unavailable",
        ResumeIngestionStageLogger.safeCause(
            new IllegalStateException("sensitive exception text"), IngestionStage.LLM_EXTRACTION));
    assertEquals(
        "invalid-candidate-data",
        ResumeIngestionStageLogger.safeCause(
            new IllegalStateException("sensitive exception text"), IngestionStage.VALIDATION));
    assertEquals(
        "mongodb-unavailable",
        ResumeIngestionStageLogger.safeCause(
            new IllegalStateException("sensitive exception text"), IngestionStage.MONGO_PERSISTENCE));
    assertEquals(
        "qdrant-unavailable",
        ResumeIngestionStageLogger.safeCause(
            new IllegalStateException("sensitive exception text"), IngestionStage.QDRANT_WRITE));
    assertEquals(
        "file-move-failed",
        ResumeIngestionStageLogger.safeCause(
            failure(ResumeIngestionException.FailureCause.FILE_MOVE_FAILED),
            IngestionStage.FILE_MOVEMENT_ERROR));
    assertEquals(
        "rollback-failed",
        ResumeIngestionStageLogger.safeCause(
            new IllegalStateException("sensitive exception text"), IngestionStage.ROLLBACK));
    assertEquals(
        "processing-failed",
        ResumeIngestionStageLogger.safeCause(
            new IllegalStateException("sensitive exception text"), IngestionStage.PROCESSING));
  }

  private ResumeIngestionException failure(ResumeIngestionException.FailureCause cause) {
    return new ResumeIngestionException("sensitive exception text", cause);
  }
}
