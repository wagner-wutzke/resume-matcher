package net.wowdev.ai.resume.matcher.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResumeIngestionPropertiesTest {

  @TempDir Path temporaryDirectory;

  @Test
  void shouldParsePropertiesWithoutCreatingResultDirectories() throws Exception {
    Path input = Files.createDirectory(temporaryDirectory.resolve("incoming"));

    ResumeIngestionProperties properties =
        new ResumeIngestionProperties(input.toString(), "PT2S", "PT5S");

    assertEquals(input.toAbsolutePath(), properties.getInputDirectory());
    assertEquals("PT2S", properties.getStabilityInterval().toString());
    assertEquals("PT5S", properties.getStabilityTimeout().toString());
    assertTrue(Files.notExists(properties.getHandledDirectory()));
    assertTrue(Files.notExists(properties.getErrorDirectory()));
  }

  @Test
  void shouldRejectMissingTimeout() throws Exception {
    Path input = Files.createDirectory(temporaryDirectory.resolve("incoming"));

    assertThrows(
        IllegalStateException.class,
        () -> new ResumeIngestionProperties(input.toString(), "PT2S", ""));
  }

  @Test
  void shouldRejectTimeoutNotGreaterThanInterval() throws Exception {
    Path input = Files.createDirectory(temporaryDirectory.resolve("incoming"));

    assertThrows(
        IllegalStateException.class,
        () -> new ResumeIngestionProperties(input.toString(), "PT2S", "PT2S"));
  }

  @Test
  void shouldRejectInvalidInputDirectory() {
    assertThrows(
        IllegalStateException.class,
        () ->
            new ResumeIngestionProperties(
                temporaryDirectory.resolve("missing").toString(), "PT2S", "PT5S"));
  }

  @Test
  void shouldRejectBlankAndInvalidInputPaths() {
    assertThrows(
        IllegalStateException.class, () -> new ResumeIngestionProperties(" ", "PT2S", "PT5S"));
    assertThrows(
        IllegalStateException.class, () -> new ResumeIngestionProperties("\u0000", "PT2S", "PT5S"));
  }

  @Test
  void shouldRejectAnInputPathThatIsAFile() throws Exception {
    Path inputFile = Files.writeString(temporaryDirectory.resolve("input.pdf"), "pdf");

    assertThrows(
        IllegalStateException.class,
        () -> new ResumeIngestionProperties(inputFile.toString(), "PT2S", "PT5S"));
  }

  @Test
  void shouldRejectInvalidOrNonPositiveDurations() throws Exception {
    Path input = Files.createDirectory(temporaryDirectory.resolve("incoming"));

    assertThrows(
        IllegalStateException.class,
        () -> new ResumeIngestionProperties(input.toString(), "not-a-duration", "PT5S"));
    assertThrows(
        IllegalStateException.class,
        () -> new ResumeIngestionProperties(input.toString(), "PT0S", "PT5S"));
    assertThrows(
        IllegalStateException.class,
        () -> new ResumeIngestionProperties(input.toString(), "PT2S", "invalid"));
  }

  @Test
  void shouldCreateResultDirectoriesOnPreparation() throws Exception {
    Path input = Files.createDirectory(temporaryDirectory.resolve("incoming"));

    ResumeIngestionProperties properties =
        new ResumeIngestionProperties(input.toString(), "PT2S", "PT5S");
    new ResumeIngestionDirectories(properties).prepare();

    assertTrue(Files.isDirectory(properties.getHandledDirectory()));
    assertTrue(Files.isDirectory(properties.getErrorDirectory()));
  }

  @Test
  void shouldFailWhenResultDirectoryCannotBeCreated() throws Exception {
    Path input = Files.createDirectory(temporaryDirectory.resolve("incoming"));
    Files.writeString(temporaryDirectory.resolve("handled"), "blocks directory creation");

    ResumeIngestionProperties properties =
        new ResumeIngestionProperties(input.toString(), "PT2S", "PT5S");

    assertThrows(
        IllegalStateException.class,
        () -> new ResumeIngestionDirectories(properties).prepare());
  }
}
