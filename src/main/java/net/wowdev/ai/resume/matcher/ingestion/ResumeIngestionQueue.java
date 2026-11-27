package net.wowdev.ai.resume.matcher.ingestion;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResumeIngestionQueue implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(ResumeIngestionQueue.class);

  private final Set<Path> inFlightPaths = ConcurrentHashMap.newKeySet();
  private final ExecutorService worker;
  private final Consumer<Path> processor;

  public ResumeIngestionQueue(Consumer<Path> processor) {
    this.processor = processor;
    ThreadFactory threadFactory =
        task -> {
          Thread thread = new Thread(task, "resume-ingestion-worker");
          thread.setDaemon(true);
          return thread;
        };
    this.worker = Executors.newSingleThreadExecutor(threadFactory);
  }

  public boolean enqueue(Path path) {
    Path normalizedPath = path.toAbsolutePath().normalize();
    if (!inFlightPaths.add(normalizedPath)) {
      return false;
    }
    log.info(
        "Resume ingestion file discovered file={} stage={} outcome={}",
        normalizedPath.getFileName(),
        IngestionStage.DISCOVERY.code(),
        "discovered");
    log.info(
        "Resume ingestion file queued file={} stage={} outcome={}",
        normalizedPath.getFileName(),
        IngestionStage.QUEUE.code(),
        "queued");
    worker.submit(
        () -> {
          try {
            processor.accept(normalizedPath);
          } catch (RuntimeException exception) {
            log.warn(
                "Resume ingestion worker failed file={} stage={} outcome={} cause={}",
                normalizedPath.getFileName(),
                IngestionStage.PROCESSING.code(),
                "failed",
                ResumeIngestionStageLogger.safeCause(exception, IngestionStage.PROCESSING));
          } finally {
            inFlightPaths.remove(normalizedPath);
          }
        });
    return true;
  }

  boolean isInFlight(Path path) {
    return inFlightPaths.contains(path.toAbsolutePath().normalize());
  }

  @Override
  public void close() {
    worker.shutdownNow();
  }
}
