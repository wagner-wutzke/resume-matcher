package net.wowdev.ai.resume.matcher.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResumeFileMoverTest {

  @TempDir Path temporaryDirectory;

  @Test
  void shouldMoveSuccessfulPdfIntoHandledDirectory() throws Exception {
    Path input = Files.createDirectory(temporaryDirectory.resolve("input"));
    Path handled = temporaryDirectory.resolve("handled");
    Path error = temporaryDirectory.resolve("error");
    Path source = Files.writeString(input.resolve("resume.pdf"), "new resume");

    Path moved = new ResumeFileMover(handled, error).moveToHandled(source);

    assertEquals(handled.resolve("resume.pdf"), moved);
    assertFalse(Files.exists(source));
    assertEquals("new resume", Files.readString(moved));
  }

  @Test
  void shouldMoveFailedPdfIntoErrorDirectoryAndOverwriteCollision() throws Exception {
    Path input = Files.createDirectory(temporaryDirectory.resolve("input"));
    Path error = Files.createDirectory(temporaryDirectory.resolve("error"));
    Path handled = temporaryDirectory.resolve("handled");
    Files.writeString(error.resolve("resume.pdf"), "old file");
    Path source = Files.writeString(input.resolve("resume.pdf"), "new file");

    Path moved = new ResumeFileMover(handled, error).moveToError(source);

    assertEquals("new file", Files.readString(moved));
    assertFalse(Files.exists(source));
  }

  @Test
  void shouldLeaveSourceWhenMoveFails() throws Exception {
    Path input = Files.createDirectory(temporaryDirectory.resolve("input"));
    Path handled = Files.createDirectory(temporaryDirectory.resolve("handled"));
    Path error = temporaryDirectory.resolve("error");
    Path source = Files.writeString(input.resolve("resume.pdf"), "private resume text");
    Path blockingDirectory = Files.createDirectory(handled.resolve("resume.pdf"));
    Files.writeString(blockingDirectory.resolve("keep.txt"), "blocking");

    ResumeIngestionException failure =
        assertThrows(
            ResumeIngestionException.class,
            () -> new ResumeFileMover(handled, error).moveToHandled(source));

    assertTrue(Files.exists(source));
    assertEquals("Resume file move failed at handled", failure.getMessage());
  }
}
