package net.wowdev.ai.resume.matcher.ingestion;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ResumeIngestionProperties {

  private final Path inputDirectory;
  private final Duration stabilityInterval;
  private final Duration stabilityTimeout;
  private final Path handledDirectory;
  private final Path errorDirectory;

  public ResumeIngestionProperties(
      @Value("${resume.ingestion.input-directory}") String inputDirectory,
      @Value("${resume.ingestion.stability-interval:PT2S}") String stabilityInterval,
      @Value("${resume.ingestion.stability-timeout}") String stabilityTimeout) {
    this.inputDirectory = parseInputDirectory(inputDirectory);
    this.stabilityInterval = parsePositiveDuration(stabilityInterval, "stability interval");
    this.stabilityTimeout = parsePositiveDuration(stabilityTimeout, "stability timeout");
    if (this.stabilityTimeout.compareTo(this.stabilityInterval) <= 0) {
      throw new IllegalStateException("Resume ingestion stability timeout must exceed interval");
    }

    this.handledDirectory = this.inputDirectory.resolveSibling("handled");
    this.errorDirectory = this.inputDirectory.resolveSibling("error");
  }

  public Path getInputDirectory() {
    return inputDirectory;
  }

  public Duration getStabilityInterval() {
    return stabilityInterval;
  }

  public Duration getStabilityTimeout() {
    return stabilityTimeout;
  }

  public Path getHandledDirectory() {
    return handledDirectory;
  }

  public Path getErrorDirectory() {
    return errorDirectory;
  }

  private static Path parseInputDirectory(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Resume ingestion input directory is required");
    }
    Path path;
    try {
      path = Path.of(value).toAbsolutePath().normalize();
    } catch (RuntimeException exception) {
      throw new IllegalStateException("Resume ingestion input directory is invalid");
    }
    if (!Files.isDirectory(path) || !Files.isReadable(path)) {
      throw new IllegalStateException(
          "Resume ingestion input directory must exist and be readable");
    }
    return path;
  }

  private static Duration parsePositiveDuration(String value, String name) {
    try {
      Duration duration = Duration.parse(value);
      if (duration.isZero() || duration.isNegative()) {
        throw new IllegalArgumentException();
      }
      return duration;
    } catch (RuntimeException exception) {
      throw new IllegalStateException("Resume ingestion " + name + " must be a positive duration");
    }
  }
}
