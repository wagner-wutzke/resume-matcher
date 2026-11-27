package net.wowdev.ai.resume.matcher.ingestion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ResumeFileMover {

  private static final Logger log = LoggerFactory.getLogger(ResumeFileMover.class);

  private final Path handledDirectory;
  private final Path errorDirectory;

  @Autowired
  public ResumeFileMover(ResumeIngestionProperties properties) {
    this(properties.getHandledDirectory(), properties.getErrorDirectory());
  }

  ResumeFileMover(Path handledDirectory, Path errorDirectory) {
    this.handledDirectory = handledDirectory;
    this.errorDirectory = errorDirectory;
  }

  public Path moveToHandled(Path source) {
    return move(source, handledDirectory, IngestionStage.FILE_MOVEMENT_HANDLED);
  }

  public Path moveToError(Path source) {
    return move(source, errorDirectory, IngestionStage.FILE_MOVEMENT_ERROR);
  }

  private Path move(Path source, Path destinationDirectory, IngestionStage stage) {
    Path destination = destinationDirectory.resolve(source.getFileName());
    try {
      Files.createDirectories(destinationDirectory);
      return Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException | RuntimeException exception) {
      String cause = ResumeIngestionException.FailureCause.FILE_MOVE_FAILED.code();
      log.warn(
          "Resume ingestion file move failed file={} stage={} outcome={} cause={}",
          source.getFileName(),
          stage.code(),
          "failed",
          cause);
      String destinationName = stage == IngestionStage.FILE_MOVEMENT_HANDLED ? "handled" : "error";
      throw new ResumeIngestionException(
          "Resume file move failed at " + destinationName,
          ResumeIngestionException.FailureCause.FILE_MOVE_FAILED);
    }
  }
}
