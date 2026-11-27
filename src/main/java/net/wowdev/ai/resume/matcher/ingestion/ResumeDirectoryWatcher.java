package net.wowdev.ai.resume.matcher.ingestion;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResumeDirectoryWatcher implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(ResumeDirectoryWatcher.class);

  private final Path inputDirectory;
  private final ResumeIngestionQueue queue;
  private WatchService watchService;
  private Thread watcherThread;

  public ResumeDirectoryWatcher(Path inputDirectory, ResumeIngestionQueue queue) {
    this.inputDirectory = inputDirectory.toAbsolutePath().normalize();
    this.queue = queue;
  }

  public void start() {
    try {
      watchService = inputDirectory.getFileSystem().newWatchService();
      inputDirectory.register(
          watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
      watcherThread = new Thread(this::watch, "resume-directory-watcher");
      watcherThread.setDaemon(true);
      watcherThread.start();
      scanInputDirectory();
    } catch (IOException exception) {
      close();
      throw new IllegalStateException("Cannot watch resume input directory");
    } catch (RuntimeException exception) {
      close();
      throw exception;
    }
  }

  public void scanInputDirectory() {
    try (DirectoryStream<Path> entries = Files.newDirectoryStream(inputDirectory)) {
      for (Path entry : entries) {
        if (isEligiblePdf(entry)) {
          queue.enqueue(entry);
        }
      }
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot scan resume input directory");
    }
  }

  private void watch() {
    while (!Thread.currentThread().isInterrupted()) {
      WatchKey key;
      try {
        key = watchService.take();
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        return;
      } catch (RuntimeException exception) {
        return;
      }
      if (!processWatchEvents(key)) {
        return;
      }
    }
  }

  boolean processWatchEvents(WatchKey key) {
    for (WatchEvent<?> event : key.pollEvents()) {
      if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
        scanInputDirectory();
        continue;
      }
      Path child = inputDirectory.resolve((Path) event.context());
      if (isEligiblePdf(child)) {
        queue.enqueue(child);
      }
    }
    if (!key.reset()) {
      log.warn("Resume ingestion directory watch stopped cause={}", "watch key invalid");
      return false;
    }
    return true;
  }

  private boolean isEligiblePdf(Path path) {
    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
      return false;
    }
    Path parent = path.toAbsolutePath().normalize().getParent();
    if (!inputDirectory.equals(parent)) {
      return false;
    }
    String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
    return filename.endsWith(".pdf");
  }

  @Override
  public void close() {
    if (watcherThread != null) {
      watcherThread.interrupt();
    }
    if (watchService != null) {
      try {
        watchService.close();
      } catch (IOException exception) {
        log.debug("Resume ingestion directory watcher closed with cleanup issue");
      }
    }
  }
}
