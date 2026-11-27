package net.wowdev.ai.resume.matcher.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResumeDirectoryWatcherTest {

  @TempDir Path temporaryDirectory;

  @Test
  void shouldScanOnlyDirectRegularPdfFiles() throws Exception {
    Path input = Files.createDirectory(temporaryDirectory.resolve("input"));
    Path nested = Files.createDirectory(input.resolve("nested"));
    Path handled = Files.createDirectory(input.resolve("handled"));
    Path error = Files.createDirectory(input.resolve("error"));
    Path direct = Files.writeString(input.resolve("resume.PDF"), "resume");
    Files.writeString(input.resolve("notes.txt"), "ignore");
    Files.writeString(nested.resolve("nested.pdf"), "ignore");
    Files.writeString(handled.resolve("handled.pdf"), "ignore");
    Files.writeString(error.resolve("error.pdf"), "ignore");
    List<Path> processed = new CopyOnWriteArrayList<>();
    CountDownLatch completed = new CountDownLatch(1);
    ResumeIngestionQueue queue =
        new ResumeIngestionQueue(
            path -> {
              processed.add(path);
              completed.countDown();
            });
    ResumeDirectoryWatcher watcher = new ResumeDirectoryWatcher(input, queue);

    try {
      watcher.scanInputDirectory();
      assertTrue(completed.await(2, TimeUnit.SECONDS));
      assertEquals(List.of(direct.toAbsolutePath().normalize()), processed);
    } finally {
      watcher.close();
      queue.close();
    }
  }

  @Test
  void shouldIgnoreSymbolicLinksToPdfFiles() throws Exception {
    Path input = Files.createDirectory(temporaryDirectory.resolve("input"));
    Path externalPdf = Files.writeString(temporaryDirectory.resolve("external.pdf"), "resume");
    Path linkedPdf = Files.createSymbolicLink(input.resolve("linked.pdf"), externalPdf);
    ResumeIngestionQueue queue = new ResumeIngestionQueue(path -> {});
    ResumeDirectoryWatcher watcher = new ResumeDirectoryWatcher(input, queue);

    try {
      watcher.scanInputDirectory();

      assertFalse(queue.isInFlight(linkedPdf));
    } finally {
      watcher.close();
      queue.close();
    }
  }

  @Test
  void shouldWatchNewDirectPdfAndIgnoreNonPdf() throws Exception {
    Path input = Files.createDirectory(temporaryDirectory.resolve("input"));
    List<Path> processed = new CopyOnWriteArrayList<>();
    CountDownLatch completed = new CountDownLatch(1);
    ResumeIngestionQueue queue =
        new ResumeIngestionQueue(
            path -> {
              processed.add(path);
              completed.countDown();
            });
    ResumeDirectoryWatcher watcher = new ResumeDirectoryWatcher(input, queue);

    try {
      watcher.start();
      Files.writeString(input.resolve("notes.txt"), "ignore");
      Path added = Files.writeString(input.resolve("candidate.pdf"), "resume");

      assertTrue(completed.await(2, TimeUnit.SECONDS));
      assertEquals(List.of(added.toAbsolutePath().normalize()), processed);
    } finally {
      watcher.close();
      queue.close();
    }
  }

  @Test
  void shouldSuppressDuplicatePathWhileProcessorIsInFlight() throws Exception {
    Path input = Files.createDirectory(temporaryDirectory.resolve("input"));
    Path pdf = Files.writeString(input.resolve("candidate.pdf"), "resume");
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    java.util.concurrent.atomic.AtomicInteger calls =
        new java.util.concurrent.atomic.AtomicInteger();
    ResumeIngestionQueue queue =
        new ResumeIngestionQueue(
            path -> {
              calls.incrementAndGet();
              started.countDown();
              try {
                release.await(2, TimeUnit.SECONDS);
              } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
              }
            });

    try {
      assertTrue(queue.enqueue(pdf));
      assertTrue(started.await(2, TimeUnit.SECONDS));
      assertTrue(queue.isInFlight(pdf));
      assertTrue(!queue.enqueue(pdf.toAbsolutePath().normalize()));
    } finally {
      release.countDown();
      queue.close();
    }
    assertEquals(1, calls.get());
  }

  @Test
  void shouldContinueWorkerAfterProcessorThrows() throws Exception {
    Path first = temporaryDirectory.resolve("first.pdf");
    Path second = temporaryDirectory.resolve("second.pdf");
    CountDownLatch secondCompleted = new CountDownLatch(1);
    java.util.concurrent.atomic.AtomicInteger calls =
        new java.util.concurrent.atomic.AtomicInteger();
    ResumeIngestionQueue queue =
        new ResumeIngestionQueue(
            path -> {
              calls.incrementAndGet();
              if (path.equals(first)) {
                throw new IllegalStateException("private failure details");
              }
              secondCompleted.countDown();
            });

    try {
      queue.enqueue(first);
      queue.enqueue(second);
      assertTrue(secondCompleted.await(2, TimeUnit.SECONDS));
    } finally {
      queue.close();
    }
    assertEquals(2, calls.get());
  }

  @Test
  void shouldRescanOnOverflowAndStopWhenWatchKeyBecomesInvalid() throws Exception {
    Path input = Files.createDirectory(temporaryDirectory.resolve("input"));
    Path resume = Files.writeString(input.resolve("overflow.pdf"), "resume");
    CountDownLatch processed = new CountDownLatch(1);
    java.util.List<Path> seen = new CopyOnWriteArrayList<>();
    ResumeIngestionQueue queue =
        new ResumeIngestionQueue(
            path -> {
              seen.add(path);
              processed.countDown();
            });
    ResumeDirectoryWatcher watcher = new ResumeDirectoryWatcher(input, queue);
    @SuppressWarnings("unchecked")
    WatchEvent<Object> overflow = org.mockito.Mockito.mock(WatchEvent.class);
    when(overflow.kind()).thenReturn(StandardWatchEventKinds.OVERFLOW);
    WatchKey key = org.mockito.Mockito.mock(WatchKey.class);
    when(key.pollEvents()).thenReturn(List.of(overflow));
    when(key.reset()).thenReturn(false);

    try {
      assertTrue(!watcher.processWatchEvents(key));
      assertTrue(processed.await(2, TimeUnit.SECONDS));
      assertEquals(List.of(resume.toAbsolutePath().normalize()), seen);
    } finally {
      watcher.close();
      queue.close();
    }
  }

  @Test
  void shouldIgnoreNestedPathsReportedAsEvents() throws Exception {
    Path input = Files.createDirectory(temporaryDirectory.resolve("input"));
    Path nested = Files.createDirectory(input.resolve("nested"));
    Path resume = Files.writeString(nested.resolve("nested.pdf"), "resume");
    java.util.List<Path> seen = new CopyOnWriteArrayList<>();
    ResumeIngestionQueue queue = new ResumeIngestionQueue(seen::add);
    ResumeDirectoryWatcher watcher = new ResumeDirectoryWatcher(input, queue);
    @SuppressWarnings("unchecked")
    WatchEvent<Path> event = org.mockito.Mockito.mock(WatchEvent.class);
    when(event.kind()).thenReturn(StandardWatchEventKinds.ENTRY_CREATE);
    when(event.context()).thenReturn(Path.of("nested", "nested.pdf"));
    WatchKey key = org.mockito.Mockito.mock(WatchKey.class);
    when(key.pollEvents()).thenReturn(List.of(event));
    when(key.reset()).thenReturn(true);

    try {
      assertTrue(watcher.processWatchEvents(key));
      assertTrue(seen.isEmpty());
    } finally {
      watcher.close();
      queue.close();
    }
  }

  @Test
  void shouldFailCleanlyWhenInputDirectoryCannotBeWatchedOrScanned() throws Exception {
    Path inputFile = Files.writeString(temporaryDirectory.resolve("input"), "not a directory");
    ResumeIngestionQueue queue = new ResumeIngestionQueue(path -> {});
    ResumeDirectoryWatcher watcher = new ResumeDirectoryWatcher(inputFile, queue);

    try {
      org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, watcher::start);
      org.junit.jupiter.api.Assertions.assertThrows(
          IllegalStateException.class, watcher::scanInputDirectory);
    } finally {
      watcher.close();
      queue.close();
    }
  }

  @Test
  void shouldCloseWatchServiceWhenStartupScanFails() throws Exception {
    Path input = Files.createDirectory(temporaryDirectory.resolve("input"));
    ResumeIngestionQueue queue = new ResumeIngestionQueue(path -> {});
    ResumeDirectoryWatcher watcher = spy(new ResumeDirectoryWatcher(input, queue));
    doThrow(new IllegalStateException("scan failed")).when(watcher).scanInputDirectory();

    try {
      assertThrows(IllegalStateException.class, watcher::start);

      verify(watcher).close();
    } finally {
      watcher.close();
      queue.close();
    }
  }
}
