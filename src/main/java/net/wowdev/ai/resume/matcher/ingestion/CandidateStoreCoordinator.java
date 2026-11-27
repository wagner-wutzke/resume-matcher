package net.wowdev.ai.resume.matcher.ingestion;

import java.nio.file.Path;
import java.util.Optional;
import net.wowdev.ai.resume.matcher.model.Candidate;
import net.wowdev.ai.resume.matcher.model.CandidateVectorPoint;
import net.wowdev.ai.resume.matcher.model.CandidateVectorSnapshot;
import net.wowdev.ai.resume.matcher.repository.CandidateRepository;
import net.wowdev.ai.resume.matcher.repository.CandidateVectorRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

@Component
@ResumeIngestionEnabled
final class CandidateStoreCoordinator {

  private final CandidateRepository candidateRepository;
  private final CandidateVectorRepository vectorRepository;
  private final ResumeIngestionStageLogger stageLogger = new ResumeIngestionStageLogger();

  CandidateStoreCoordinator(
      CandidateRepository candidateRepository, CandidateVectorRepository vectorRepository) {
    this.candidateRepository = candidateRepository;
    this.vectorRepository = vectorRepository;
  }

  StoreUpdate persist(Candidate candidate, Path sourceFile) {
    Optional<Candidate> previousCandidate;
    try {
      previousCandidate =
          stageLogger.execute(
              sourceFile,
              IngestionStage.MONGO_SNAPSHOT,
              () -> candidateRepository.findByEmail(candidate.email()));
    } catch (RuntimeException failure) {
      stageLogger.skipped(sourceFile, IngestionStage.ROLLBACK);
      throw new StoreUpdateException(IngestionStage.MONGO_SNAPSHOT, true, failure);
    }
    CandidateVectorSnapshot previousPoints;
    try {
      previousPoints =
          stageLogger.execute(
              sourceFile,
              IngestionStage.QDRANT_SNAPSHOT,
              () -> vectorRepository.snapshotByEmail(candidate.email()));
    } catch (RuntimeException failure) {
      stageLogger.skipped(sourceFile, IngestionStage.ROLLBACK);
      throw new StoreUpdateException(IngestionStage.QDRANT_SNAPSHOT, true, failure);
    }

    boolean mongoMutationAttempted = false;
    boolean qdrantMutationAttempted = false;
    IngestionStage failingStage = IngestionStage.MONGO_PERSISTENCE;
    try {
      mongoMutationAttempted = true;
      Candidate savedCandidate =
          stageLogger.execute(
              sourceFile,
              failingStage,
              () -> candidateRepository.replaceByEmail(candidate));
      failingStage = IngestionStage.QDRANT_WRITE;
      qdrantMutationAttempted = true;
      stageLogger.execute(
          sourceFile,
          failingStage,
          () ->
              vectorRepository.replaceByEmail(
                  savedCandidate, sourceFile.getFileName().toString()));
      return new StoreUpdate(
          candidate.email(), previousCandidate, previousPoints, true, true);
    } catch (RuntimeException failure) {
      if (failure instanceof DuplicateKeyException) {
        mongoMutationAttempted = false;
        qdrantMutationAttempted = false;
      }
      boolean rollbackSucceeded =
          rollbackIfNeeded(
              sourceFile,
              candidate.email(),
              previousCandidate,
              previousPoints,
              mongoMutationAttempted,
              qdrantMutationAttempted);
      throw new StoreUpdateException(failingStage, rollbackSucceeded, failure);
    }
  }

  void rollback(Path sourceFile, StoreUpdate update) {
    stageLogger.execute(
        sourceFile,
        IngestionStage.ROLLBACK,
        () -> {
          boolean restored =
              rollbackStores(
                  update.email(),
                  update.previousCandidate(),
                  update.previousPoints(),
                  update.mongoMutationAttempted(),
                  update.qdrantMutationAttempted());
          if (!restored) {
            throw new ResumeIngestionException(
                "Store restoration failed",
                ResumeIngestionException.FailureCause.ROLLBACK_FAILED);
          }
          return null;
        });
  }

  private boolean rollbackIfNeeded(
      Path sourceFile,
      String email,
      Optional<Candidate> previousCandidate,
      CandidateVectorSnapshot previousPoints,
      boolean mongoMutationAttempted,
      boolean qdrantMutationAttempted) {
    if (!mongoMutationAttempted && !qdrantMutationAttempted) {
      stageLogger.skipped(sourceFile, IngestionStage.ROLLBACK);
      return true;
    }
    try {
      return stageLogger.execute(
          sourceFile,
          IngestionStage.ROLLBACK,
          () -> {
            boolean restored =
                rollbackStores(
                    email,
                    previousCandidate,
                    previousPoints,
                    mongoMutationAttempted,
                    qdrantMutationAttempted);
            if (!restored) {
              throw new ResumeIngestionException(
                  "Store restoration failed",
                  ResumeIngestionException.FailureCause.ROLLBACK_FAILED);
            }
            return true;
          });
    } catch (RuntimeException exception) {
      return false;
    }
  }

  private boolean rollbackStores(
      String email,
      Optional<Candidate> previousCandidate,
      CandidateVectorSnapshot previousPoints,
      boolean mongoMutationAttempted,
      boolean qdrantMutationAttempted) {
    boolean mongoRestored = !mongoMutationAttempted || restoreMongo(email, previousCandidate);
    boolean qdrantRestored = !qdrantMutationAttempted || restoreQdrant(email, previousPoints);
    return mongoRestored && qdrantRestored;
  }

  private boolean restoreMongo(String email, Optional<Candidate> previousCandidate) {
    try {
      if (previousCandidate.isPresent()) {
        candidateRepository.replaceByEmail(previousCandidate.get());
      } else {
        candidateRepository.deleteByEmail(email);
      }
      return candidateRepository.findByEmail(email).equals(previousCandidate);
    } catch (RuntimeException exception) {
      return false;
    }
  }

  private boolean restoreQdrant(String email, CandidateVectorSnapshot previousPoints) {
    try {
      vectorRepository.restoreByEmail(email, previousPoints);
      return pointsEqual(vectorRepository.snapshotByEmail(email), previousPoints);
    } catch (RuntimeException exception) {
      return false;
    }
  }

  private boolean pointsEqual(CandidateVectorSnapshot actual, CandidateVectorSnapshot expected) {
    java.util.Comparator<CandidateVectorPoint> byId =
        java.util.Comparator.comparing(CandidateVectorPoint::id);
    return actual.points().stream().sorted(byId).toList().equals(
        expected.points().stream().sorted(byId).toList());
  }

  record StoreUpdate(
      String email,
      Optional<Candidate> previousCandidate,
      CandidateVectorSnapshot previousPoints,
      boolean mongoMutationAttempted,
      boolean qdrantMutationAttempted) {}

  static final class StoreUpdateException extends RuntimeException {

    private final IngestionStage failingStage;
    private final boolean rollbackSucceeded;

    StoreUpdateException(
        IngestionStage failingStage, boolean rollbackSucceeded, RuntimeException cause) {
      super("Candidate store update failed", cause);
      this.failingStage = failingStage;
      this.rollbackSucceeded = rollbackSucceeded;
    }

    IngestionStage failingStage() {
      return failingStage;
    }

    boolean rollbackSucceeded() {
      return rollbackSucceeded;
    }
  }
}
