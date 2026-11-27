package net.wowdev.ai.resume.matcher.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@ResumeIngestionEnabled
public class ResumeIngestionRunner implements ApplicationRunner, AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(ResumeIngestionRunner.class);

  private final ResumeIngestionProperties properties;
  private final ResumeIngestionDirectories directories;
  private final CandidateIndexInitializer candidateIndexInitializer;
  private final QdrantCandidateVectorRepository vectorRepository;
  private final CandidateResumeProcessor processor;
  private ResumeIngestionQueue queue;
  private ResumeDirectoryWatcher watcher;

  public ResumeIngestionRunner(
      ResumeIngestionProperties properties,
      ResumeIngestionDirectories directories,
      CandidateIndexInitializer candidateIndexInitializer,
      QdrantCandidateVectorRepository vectorRepository,
      CandidateResumeProcessor processor) {
    this.properties = properties;
    this.directories = directories;
    this.candidateIndexInitializer = candidateIndexInitializer;
    this.vectorRepository = vectorRepository;
    this.processor = processor;
  }

  @Override
  public void run(ApplicationArguments arguments) {
    directories.prepare();
    candidateIndexInitializer.ensureEmailUniqueness();
    vectorRepository.ensureEmailPayloadIndex();
    queue = new ResumeIngestionQueue(processor::process);
    watcher = new ResumeDirectoryWatcher(properties.getInputDirectory(), queue);
    watcher.start();
    log.info("Resume ingestion started directory={}", properties.getInputDirectory().getFileName());
  }

  @Override
  public void close() {
    if (watcher != null) {
      watcher.close();
    }
    if (queue != null) {
      queue.close();
    }
  }
}
