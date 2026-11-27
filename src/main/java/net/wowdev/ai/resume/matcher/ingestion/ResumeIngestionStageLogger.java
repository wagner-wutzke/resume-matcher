package net.wowdev.ai.resume.matcher.ingestion;

import java.nio.file.Path;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ResumeIngestionStageLogger {

  private static final Logger log = LoggerFactory.getLogger(ResumeIngestionStageLogger.class);

  <T> T execute(Path file, IngestionStage stage, Supplier<T> operation) {
    long startedAt = System.nanoTime();
    log.info(
        "Resume ingestion stage started file={} stage={}", filename(file), stage.code());
    try {
      T value = operation.get();
      completed(file, stage, "succeeded", startedAt);
      return value;
    } catch (RuntimeException exception) {
      long elapsedMillis = elapsedMillis(startedAt);
      log.warn(
          "Resume ingestion stage completed file={} stage={} outcome={} elapsedMs={} cause={}",
          filename(file),
          stage.code(),
          "failed",
          elapsedMillis,
          safeCause(exception, stage));
      throw exception;
    }
  }

  void execute(Path file, IngestionStage stage, Runnable operation) {
    execute(
        file,
        stage,
        () -> {
          operation.run();
          return null;
        });
  }

  void skipped(Path file, IngestionStage stage) {
    log.info(
        "Resume ingestion stage completed file={} stage={} outcome={}",
        filename(file),
        stage.code(),
        "skipped");
  }

  void overallStarted(Path file) {
    log.info(
        "Resume ingestion stage started file={} stage={} outcome={}",
        filename(file),
        IngestionStage.OVERALL.code(),
        "started");
  }

  void overallCompleted(Path file, boolean successful, long startedAt) {
    log.info(
        "Resume ingestion completed file={} stage={} outcome={} elapsedMs={}",
        filename(file),
        IngestionStage.OVERALL.code(),
        successful ? "succeeded" : "failed",
        elapsedMillis(startedAt));
  }

  void manualRecoveryRequired(Path file) {
    log.error(
        "Resume ingestion stage completed file={} stage={} outcome={} cause={} manualRecoveryRequired=true",
        filename(file),
        IngestionStage.ROLLBACK.code(),
        "failed",
        ResumeIngestionException.FailureCause.ROLLBACK_FAILED.code(),
        new ResumeIngestionException(
            "Manual recovery is required",
            ResumeIngestionException.FailureCause.ROLLBACK_FAILED));
  }

  private void completed(Path file, IngestionStage stage, String outcome, long startedAt) {
    log.info(
        "Resume ingestion stage completed file={} stage={} outcome={} elapsedMs={}",
        filename(file),
        stage.code(),
        outcome,
        elapsedMillis(startedAt));
  }

  private long elapsedMillis(long startedAt) {
    return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
  }

  private String filename(Path file) {
    return file.getFileName().toString();
  }

  static String safeCause(RuntimeException exception, IngestionStage stage) {
    if (exception instanceof ResumeIngestionException ingestionException) {
      return ingestionException.failureCause().code();
    }
    return stage.fallbackCause().code();
  }
}
