package net.wowdev.ai.resume.matcher.ingestion;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ResumeFileReadinessGateTest {

  @Test
  void shouldWaitUntilFileSizeHasBeenStableForTheConfiguredInterval() {
    AtomicLong time = new AtomicLong();
    AtomicInteger reads = new AtomicInteger();
    ResumeFileReadinessGate gate =
        new ResumeFileReadinessGate(
            Duration.ofMillis(200),
            Duration.ofMillis(600),
            time::get,
            duration -> time.addAndGet(duration.toNanos()),
            path -> reads.incrementAndGet() < 3 ? 10 : 20);

    assertDoesNotThrow(() -> gate.awaitReady(Path.of("resume.pdf")));

    org.junit.jupiter.api.Assertions.assertTrue(reads.get() >= 3);
  }

  @Test
  void shouldFailWithoutReadingWhenFileNeverStabilizesBeforeTimeout() {
    AtomicLong time = new AtomicLong();
    AtomicInteger reads = new AtomicInteger();
    ResumeFileReadinessGate gate =
        new ResumeFileReadinessGate(
            Duration.ofMillis(200),
            Duration.ofMillis(500),
            time::get,
            duration -> time.addAndGet(duration.toNanos()),
            path -> reads.getAndIncrement());

    ResumeIngestionException failure =
        assertThrows(
            ResumeIngestionException.class, () -> gate.awaitReady(Path.of("changing.pdf")));

    assertEquals("File readiness timed out", failure.getMessage());
  }

  @Test
  void shouldSanitizeFileStatErrors() {
    ResumeFileReadinessGate gate =
        new ResumeFileReadinessGate(
            Duration.ofMillis(200),
            Duration.ofMillis(500),
            () -> 0L,
            duration -> {},
            path -> {
              throw new IllegalStateException("private path details");
            });

    ResumeIngestionException failure =
        assertThrows(
            ResumeIngestionException.class, () -> gate.awaitReady(Path.of("unreadable.pdf")));

    assertEquals("Unable to inspect file readiness", failure.getMessage());
  }

  @Test
  void shouldRestoreInterruptStatusWhenWaitIsInterrupted() {
    ResumeFileReadinessGate gate =
        new ResumeFileReadinessGate(
            Duration.ofMillis(200),
            Duration.ofMillis(500),
            () -> 0L,
            duration -> {
              throw new InterruptedException();
            },
            path -> 10L);

    try {
      ResumeIngestionException failure =
          assertThrows(
              ResumeIngestionException.class, () -> gate.awaitReady(Path.of("resume.pdf")));
      assertEquals("File readiness wait was interrupted", failure.getMessage());
      org.junit.jupiter.api.Assertions.assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void shouldSanitizeMissingFileStatFailure() {
    ResumeFileReadinessGate gate =
        new ResumeFileReadinessGate(
            Duration.ofMillis(200),
            Duration.ofMillis(500),
            () -> 0L,
            duration -> {},
            ResumeFileReadinessGate::fileSize);

    assertThrows(
        ResumeIngestionException.class,
        () -> gate.awaitReady(Path.of("target/missing-resume.pdf")));
  }
}
