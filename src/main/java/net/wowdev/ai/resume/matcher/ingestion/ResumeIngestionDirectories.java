package net.wowdev.ai.resume.matcher.ingestion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

@Component
final class ResumeIngestionDirectories {

  private final ResumeIngestionProperties properties;

  ResumeIngestionDirectories(ResumeIngestionProperties properties) {
    this.properties = properties;
  }

  void prepare() {
    createWritableDirectory(properties.getHandledDirectory());
    createWritableDirectory(properties.getErrorDirectory());
  }

  private void createWritableDirectory(Path directory) {
    try {
      Files.createDirectories(directory);
      if (!Files.isDirectory(directory) || !Files.isWritable(directory)) {
        throw new IOException("not a writable directory");
      }
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot create resume ingestion result directory");
    }
  }
}
