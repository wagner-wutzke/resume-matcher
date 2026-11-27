package net.wowdev.ai.resume.matcher.ingestion;

import java.nio.file.Path;
import java.util.List;
import net.wowdev.ai.resume.matcher.model.Candidate;
import org.springframework.stereotype.Component;

@Component
@ResumeIngestionEnabled
public class CandidateResumeProcessor {

  private static final List<IngestionStage> STORE_STAGES =
      List.of(
          IngestionStage.MONGO_SNAPSHOT,
          IngestionStage.QDRANT_SNAPSHOT,
          IngestionStage.MONGO_PERSISTENCE,
          IngestionStage.QDRANT_WRITE,
          IngestionStage.ROLLBACK,
          IngestionStage.FILE_MOVEMENT_HANDLED);

  private final CandidateResumePipeline pipeline;
  private final CandidateStoreCoordinator storeCoordinator;
  private final ResumeFileMover fileMover;
  private final ResumeIngestionStageLogger stageLogger = new ResumeIngestionStageLogger();

  public CandidateResumeProcessor(
      CandidateResumePipeline pipeline,
      CandidateStoreCoordinator storeCoordinator,
      ResumeFileMover fileMover) {
    this.pipeline = pipeline;
    this.storeCoordinator = storeCoordinator;
    this.fileMover = fileMover;
  }

  public ResumeProcessingResult process(Path sourceFile) {
    long startedAt = System.nanoTime();
    stageLogger.overallStarted(sourceFile);
    Candidate candidate;
    try {
      candidate = pipeline.prepare(sourceFile);
    } catch (RuntimeException exception) {
      skipStages(sourceFile, 0);
      return failed(sourceFile, startedAt, false);
    }

    CandidateStoreCoordinator.StoreUpdate storeUpdate;
    try {
      storeUpdate = storeCoordinator.persist(candidate, sourceFile);
    } catch (CandidateStoreCoordinator.StoreUpdateException failure) {
      skipFailedPath(sourceFile, failure.failingStage());
      boolean manualRecoveryRequired = !failure.rollbackSucceeded();
      if (manualRecoveryRequired) {
        stageLogger.manualRecoveryRequired(sourceFile);
      }
      return failed(sourceFile, startedAt, manualRecoveryRequired);
    }

    try {
      stageLogger.execute(
          sourceFile, IngestionStage.FILE_MOVEMENT_HANDLED, () -> fileMover.moveToHandled(sourceFile));
      stageLogger.overallCompleted(sourceFile, true, startedAt);
      return new ResumeProcessingResult(true, false);
    } catch (RuntimeException failure) {
      boolean rollbackSucceeded = true;
      try {
        storeCoordinator.rollback(sourceFile, storeUpdate);
      } catch (RuntimeException rollbackFailure) {
        rollbackSucceeded = false;
      }
      if (!rollbackSucceeded) {
        stageLogger.manualRecoveryRequired(sourceFile);
      }
      return failed(sourceFile, startedAt, !rollbackSucceeded);
    }
  }

  private ResumeProcessingResult failed(
      Path sourceFile, long startedAt, boolean manualRecoveryRequired) {
    moveToError(sourceFile);
    stageLogger.overallCompleted(sourceFile, false, startedAt);
    return new ResumeProcessingResult(false, manualRecoveryRequired);
  }

  private void skipStages(Path sourceFile, int firstStage) {
    for (int index = firstStage; index < STORE_STAGES.size(); index++) {
      stageLogger.skipped(sourceFile, STORE_STAGES.get(index));
    }
  }

  private void skipFailedPath(Path sourceFile, IngestionStage failingStage) {
    int index = STORE_STAGES.indexOf(failingStage);
    for (int next = index + 1; next < STORE_STAGES.size(); next++) {
      if (STORE_STAGES.get(next) != IngestionStage.ROLLBACK) {
        stageLogger.skipped(sourceFile, STORE_STAGES.get(next));
      }
    }
  }

  private void moveToError(Path sourceFile) {
    try {
      stageLogger.execute(
          sourceFile,
          IngestionStage.FILE_MOVEMENT_ERROR,
          () -> fileMover.moveToError(sourceFile));
    } catch (RuntimeException exception) {
      // The mover logs the filename, stage, and sanitized failure cause.
    }
  }
}
