package net.wowdev.ai.resume.matcher.ingestion;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.wowdev.ai.resume.matcher.model.Candidate;
import net.wowdev.ai.resume.matcher.model.CandidateVectorPoint;
import net.wowdev.ai.resume.matcher.model.CandidateVectorSnapshot;
import net.wowdev.ai.resume.matcher.repository.CandidateRepository;
import net.wowdev.ai.resume.matcher.repository.CandidateVectorRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;

class CandidateResumeProcessorTest {

  private static final Path SOURCE = Path.of("/tmp/incoming/jane.pdf");

  @Test
  void shouldRemoveNewCandidateAndEmbeddingsWhenVectorWriteFails() {
    Candidate candidate = candidate("mongo-new", "New candidate");
    CandidateResumePipeline pipeline = Mockito.mock(CandidateResumePipeline.class);
    CandidateRepository candidates = Mockito.mock(CandidateRepository.class);
    CandidateVectorRepository vectors = Mockito.mock(CandidateVectorRepository.class);
    ResumeFileMover mover = Mockito.mock(ResumeFileMover.class);
    when(pipeline.prepare(SOURCE)).thenReturn(candidate);
    when(candidates.findByEmail(candidate.email())).thenReturn(Optional.empty());
    when(vectors.snapshotByEmail(candidate.email())).thenReturn(CandidateVectorSnapshot.empty());
    when(candidates.replaceByEmail(candidate)).thenReturn(candidate);
    doThrow(new ResumeIngestionException("vector write failed"))
        .when(vectors)
        .replaceByEmail(candidate, "jane.pdf");
    when(vectors.snapshotByEmail(candidate.email())).thenReturn(CandidateVectorSnapshot.empty());

    ResumeProcessingResult result =
        new CandidateResumeProcessor(
                pipeline, new CandidateStoreCoordinator(candidates, vectors), mover)
            .process(SOURCE);

    assertFalse(result.successful());
    assertFalse(result.manualRecoveryRequired());
    verify(candidates).deleteByEmail(candidate.email());
    verify(vectors).restoreByEmail(candidate.email(), CandidateVectorSnapshot.empty());
    verify(mover).moveToError(SOURCE);
    verify(mover, never()).moveToHandled(SOURCE);
  }

  @Test
  void shouldRestorePreviousCandidateAndPointsWhenHandledMoveFails() {
    Candidate incoming = candidate("mongo-1", "Updated candidate");
    Candidate previous = candidate("mongo-1", "Previous candidate");
    CandidateVectorSnapshot priorPoints =
        new CandidateVectorSnapshot(
            List.of(new CandidateVectorPoint("point-id", java.util.Map.of(), java.util.Map.of())));
    CandidateResumePipeline pipeline = Mockito.mock(CandidateResumePipeline.class);
    CandidateRepository candidates = Mockito.mock(CandidateRepository.class);
    CandidateVectorRepository vectors = Mockito.mock(CandidateVectorRepository.class);
    ResumeFileMover mover = Mockito.mock(ResumeFileMover.class);
    when(pipeline.prepare(SOURCE)).thenReturn(incoming);
    when(candidates.findByEmail(incoming.email())).thenReturn(Optional.of(previous));
    when(vectors.snapshotByEmail(incoming.email())).thenReturn(priorPoints);
    when(candidates.replaceByEmail(incoming)).thenReturn(incoming);
    when(candidates.replaceByEmail(previous)).thenReturn(previous);
    when(candidates.findByEmail(incoming.email())).thenReturn(Optional.of(previous));
    when(mover.moveToHandled(SOURCE)).thenThrow(new ResumeIngestionException("move failed"));
    when(vectors.snapshotByEmail(incoming.email())).thenReturn(priorPoints);

    ResumeProcessingResult result =
        new CandidateResumeProcessor(
                pipeline, new CandidateStoreCoordinator(candidates, vectors), mover)
            .process(SOURCE);

    assertFalse(result.successful());
    assertFalse(result.manualRecoveryRequired());
    verify(vectors).replaceByEmail(incoming, "jane.pdf");
    verify(candidates).replaceByEmail(previous);
    verify(vectors).restoreByEmail(incoming.email(), priorPoints);
    verify(mover).moveToError(SOURCE);
  }

  @Test
  void shouldMarkManualRecoveryWhenBothCompensationsFail() {
    Candidate candidate = candidate("mongo-new", "New candidate");
    CandidateResumePipeline pipeline = Mockito.mock(CandidateResumePipeline.class);
    CandidateRepository candidates = Mockito.mock(CandidateRepository.class);
    CandidateVectorRepository vectors = Mockito.mock(CandidateVectorRepository.class);
    ResumeFileMover mover = Mockito.mock(ResumeFileMover.class);
    when(pipeline.prepare(SOURCE)).thenReturn(candidate);
    when(candidates.findByEmail(candidate.email())).thenReturn(Optional.empty());
    when(vectors.snapshotByEmail(candidate.email())).thenReturn(CandidateVectorSnapshot.empty());
    when(candidates.replaceByEmail(candidate)).thenReturn(candidate);
    doThrow(new ResumeIngestionException("vector write failed"))
        .when(vectors)
        .replaceByEmail(candidate, "jane.pdf");
    doThrow(new ResumeIngestionException("mongo restore failed"))
        .when(candidates)
        .deleteByEmail(candidate.email());
    doThrow(new ResumeIngestionException("vector restore failed"))
        .when(vectors)
        .restoreByEmail(candidate.email(), CandidateVectorSnapshot.empty());

    ResumeProcessingResult result =
        new CandidateResumeProcessor(
                pipeline, new CandidateStoreCoordinator(candidates, vectors), mover)
            .process(SOURCE);

    assertFalse(result.successful());
    assertTrue(result.manualRecoveryRequired());
    verify(vectors).restoreByEmail(candidate.email(), CandidateVectorSnapshot.empty());
    verify(mover).moveToError(SOURCE);
  }

  @Test
  void shouldCompleteProcessingAfterBothStoresAndFileMovementSucceed() {
    Candidate candidate = candidate("mongo-1", "Jane");
    CandidateResumePipeline pipeline = Mockito.mock(CandidateResumePipeline.class);
    CandidateRepository candidates = Mockito.mock(CandidateRepository.class);
    CandidateVectorRepository vectors = Mockito.mock(CandidateVectorRepository.class);
    ResumeFileMover mover = Mockito.mock(ResumeFileMover.class);
    when(pipeline.prepare(SOURCE)).thenReturn(candidate);
    when(candidates.findByEmail(candidate.email())).thenReturn(Optional.empty());
    when(vectors.snapshotByEmail(candidate.email())).thenReturn(CandidateVectorSnapshot.empty());
    when(candidates.replaceByEmail(candidate)).thenReturn(candidate);

    ResumeProcessingResult result =
        new CandidateResumeProcessor(
                pipeline, new CandidateStoreCoordinator(candidates, vectors), mover)
            .process(SOURCE);

    assertTrue(result.successful());
    assertFalse(result.manualRecoveryRequired());
    verify(vectors).replaceByEmail(candidate, "jane.pdf");
    verify(mover).moveToHandled(SOURCE);
    verify(mover, never()).moveToError(SOURCE);
  }

  @Test
  void shouldLogStoreAndFileStagesWithFilenameAndOutcome() {
    Candidate candidate = candidate("mongo-1", "Jane");
    CandidateResumePipeline pipeline = Mockito.mock(CandidateResumePipeline.class);
    CandidateRepository candidates = Mockito.mock(CandidateRepository.class);
    CandidateVectorRepository vectors = Mockito.mock(CandidateVectorRepository.class);
    ResumeFileMover mover = Mockito.mock(ResumeFileMover.class);
    when(pipeline.prepare(SOURCE)).thenReturn(candidate);
    when(candidates.findByEmail(candidate.email())).thenReturn(Optional.empty());
    when(vectors.snapshotByEmail(candidate.email())).thenReturn(CandidateVectorSnapshot.empty());
    when(candidates.replaceByEmail(candidate)).thenReturn(candidate);
    Logger logger = (Logger) LoggerFactory.getLogger(ResumeIngestionStageLogger.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);

    try {
      ResumeProcessingResult result =
          new CandidateResumeProcessor(
                  pipeline, new CandidateStoreCoordinator(candidates, vectors), mover)
              .process(SOURCE);

      assertTrue(result.successful());
      String logs =
          appender.list.stream()
              .map(ILoggingEvent::getFormattedMessage)
              .reduce("", (left, right) -> left + "\n" + right);
      for (IngestionStage stage :
          List.of(
              IngestionStage.MONGO_SNAPSHOT,
              IngestionStage.QDRANT_SNAPSHOT,
              IngestionStage.MONGO_PERSISTENCE,
              IngestionStage.QDRANT_WRITE,
              IngestionStage.FILE_MOVEMENT_HANDLED)) {
        String stageCode = stage.code();
        assertTrue(logs.contains("file=jane.pdf stage=" + stageCode));
        assertTrue(logs.contains("stage=" + stageCode + " outcome=succeeded"));
      }
      assertTrue(logs.contains("file=jane.pdf stage=overall outcome=succeeded"));
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }

  @Test
  void shouldRouteToErrorWithoutWritesWhenVectorSnapshotFails() {
    Candidate candidate = candidate("mongo-1", "Jane");
    CandidateResumePipeline pipeline = Mockito.mock(CandidateResumePipeline.class);
    CandidateRepository candidates = Mockito.mock(CandidateRepository.class);
    CandidateVectorRepository vectors = Mockito.mock(CandidateVectorRepository.class);
    ResumeFileMover mover = Mockito.mock(ResumeFileMover.class);
    when(pipeline.prepare(SOURCE)).thenReturn(candidate);
    when(candidates.findByEmail(candidate.email())).thenReturn(Optional.empty());
    when(vectors.snapshotByEmail(candidate.email()))
        .thenThrow(new ResumeIngestionException("snapshot failed"));

    ResumeProcessingResult result =
        new CandidateResumeProcessor(
                pipeline, new CandidateStoreCoordinator(candidates, vectors), mover)
            .process(SOURCE);

    assertFalse(result.successful());
    verify(candidates, never()).replaceByEmail(any(Candidate.class));
    verify(vectors, never()).replaceByEmail(any(Candidate.class), any());
    verify(mover).moveToError(SOURCE);
  }

  @Test
  void shouldNotDeleteConcurrentCandidateAfterDuplicateKeyRace() {
    Candidate candidate = candidate(null, "Jane");
    CandidateResumePipeline pipeline = Mockito.mock(CandidateResumePipeline.class);
    CandidateRepository candidates = Mockito.mock(CandidateRepository.class);
    CandidateVectorRepository vectors = Mockito.mock(CandidateVectorRepository.class);
    ResumeFileMover mover = Mockito.mock(ResumeFileMover.class);
    when(pipeline.prepare(SOURCE)).thenReturn(candidate);
    when(candidates.findByEmail(candidate.email())).thenReturn(Optional.empty());
    when(vectors.snapshotByEmail(candidate.email())).thenReturn(CandidateVectorSnapshot.empty());
    when(candidates.replaceByEmail(candidate)).thenThrow(new DuplicateKeyException("duplicate"));

    ResumeProcessingResult result =
        new CandidateResumeProcessor(
                pipeline, new CandidateStoreCoordinator(candidates, vectors), mover)
            .process(SOURCE);

    assertFalse(result.successful());
    assertFalse(result.manualRecoveryRequired());
    verify(candidates, never()).deleteByEmail(candidate.email());
    verify(vectors, never())
        .restoreByEmail(eq(candidate.email()), any(CandidateVectorSnapshot.class));
    verify(mover).moveToError(SOURCE);
  }

  @Test
  void shouldRoutePipelineFailureToErrorWithoutStoreAccess() {
    CandidateResumePipeline pipeline = Mockito.mock(CandidateResumePipeline.class);
    CandidateRepository candidates = Mockito.mock(CandidateRepository.class);
    CandidateVectorRepository vectors = Mockito.mock(CandidateVectorRepository.class);
    ResumeFileMover mover = Mockito.mock(ResumeFileMover.class);
    when(pipeline.prepare(SOURCE)).thenThrow(new ResumeIngestionException("invalid PDF"));

    ResumeProcessingResult result =
        new CandidateResumeProcessor(
                pipeline, new CandidateStoreCoordinator(candidates, vectors), mover)
            .process(SOURCE);

    assertFalse(result.successful());
    verify(candidates, never()).findByEmail(any());
    verify(vectors, never()).snapshotByEmail(any());
    verify(mover).moveToError(SOURCE);
  }

  private Candidate candidate(String id, String fullname) {
    return new Candidate(
        id, "jane@example.com", fullname, "", "", "", "", "", "2 years and 0 months", List.of());
  }
}
