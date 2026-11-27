package net.wowdev.ai.resume.matcher.ingestion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.LongSupplier;
import java.util.function.ToLongFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ResumeFileReadinessGate {

  private static final Duration MAX_POLL_INTERVAL = Duration.ofMillis(100);

  private final Duration stabilityInterval;
  private final Duration stabilityTimeout;
  private final LongSupplier nanoTime;
  private final Sleeper sleeper;
  private final ToLongFunction<Path> sizeReader;

  @Autowired
  public ResumeFileReadinessGate(ResumeIngestionProperties properties) {
    this(
        properties.getStabilityInterval(),
        properties.getStabilityTimeout(),
        System::nanoTime,
        Thread::sleep,
        ResumeFileReadinessGate::fileSize);
  }

  ResumeFileReadinessGate(
      Duration stabilityInterval,
      Duration stabilityTimeout,
      LongSupplier nanoTime,
      Sleeper sleeper,
      ToLongFunction<Path> sizeReader) {
    this.stabilityInterval = stabilityInterval;
    this.stabilityTimeout = stabilityTimeout;
    this.nanoTime = nanoTime;
    this.sleeper = sleeper;
    this.sizeReader = sizeReader;
  }

  public void awaitReady(Path file) {
    long startTime = nanoTime.getAsLong();
    long deadline = startTime + stabilityTimeout.toNanos();
    long stableSince = startTime;
    long previousSize = readSize(file);
    while (true) {
      long now = nanoTime.getAsLong();
      if (now - stableSince >= stabilityInterval.toNanos()) {
        return;
      }
      long remaining = deadline - now;
      if (remaining <= 0) {
        throw readinessTimeout();
      }
      long sleepNanos = Math.min(MAX_POLL_INTERVAL.toNanos(), remaining);
      try {
        sleeper.sleep(Duration.ofNanos(sleepNanos));
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new ResumeIngestionException(
            "File readiness wait was interrupted",
            ResumeIngestionException.FailureCause.FILE_READINESS_FAILED);
      }
      long currentSize = readSize(file);
      long sampleTime = nanoTime.getAsLong();
      if (currentSize != previousSize) {
        previousSize = currentSize;
        stableSince = sampleTime;
      }
      if (sampleTime - stableSince >= stabilityInterval.toNanos()) {
        return;
      }
      if (sampleTime >= deadline) {
        throw readinessTimeout();
      }
    }
  }

  private long readSize(Path file) {
    try {
      return sizeReader.applyAsLong(file);
    } catch (RuntimeException exception) {
      throw new ResumeIngestionException(
          "Unable to inspect file readiness",
          ResumeIngestionException.FailureCause.FILE_READINESS_FAILED);
    }
  }

  private ResumeIngestionException readinessTimeout() {
    return new ResumeIngestionException(
        "File readiness timed out", ResumeIngestionException.FailureCause.READINESS_TIMEOUT);
  }

  static long fileSize(Path file) {
    try {
      return Files.size(file);
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot inspect file", exception);
    }
  }

  @FunctionalInterface
  interface Sleeper {
    void sleep(Duration duration) throws InterruptedException;
  }
}
