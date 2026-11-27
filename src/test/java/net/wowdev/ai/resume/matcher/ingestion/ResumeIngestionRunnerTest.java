package net.wowdev.ai.resume.matcher.ingestion;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.springframework.boot.DefaultApplicationArguments;

class ResumeIngestionRunnerTest {

  @TempDir Path temporaryDirectory;

  @Test
  void shouldInitializeIndexesBeforeStartingWatcherAndSharedQueue() throws Exception {
    Path input = Files.createDirectory(temporaryDirectory.resolve("input"));
    ResumeIngestionProperties properties =
        new ResumeIngestionProperties(input.toString(), "PT2S", "PT5S");
    CandidateIndexInitializer mongoIndex = mock(CandidateIndexInitializer.class);
    QdrantCandidateVectorRepository qdrant = mock(QdrantCandidateVectorRepository.class);
    CandidateResumeProcessor processor = mock(CandidateResumeProcessor.class);
    ResumeIngestionDirectories directories = new ResumeIngestionDirectories(properties);
    ResumeIngestionRunner runner =
        new ResumeIngestionRunner(properties, directories, mongoIndex, qdrant, processor);

    runner.run(new DefaultApplicationArguments(new String[0]));

    InOrder order = inOrder(mongoIndex, qdrant);
    order.verify(mongoIndex).ensureEmailUniqueness();
    order.verify(qdrant).ensureEmailPayloadIndex();
    org.junit.jupiter.api.Assertions.assertTrue(
        Files.isDirectory(properties.getHandledDirectory()));
    org.junit.jupiter.api.Assertions.assertTrue(Files.isDirectory(properties.getErrorDirectory()));
    runner.close();
  }
}
