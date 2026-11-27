package net.wowdev.ai.resume.matcher.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ResumeTextCleanerTest {

  private final ResumeTextCleaner cleaner = new ResumeTextCleaner();

  @Test
  void shouldTrimAndCollapseHorizontalWhitespaceWithinLines() {
    assertEquals(
        "Name: Jane Doe\nRole: Engineer", cleaner.clean("  Name:   Jane\tDoe\nRole:  Engineer  "));
  }

  @Test
  void shouldReduceThreeOrMoreLineBreaksToTwoAndPreserveSingleBreaks() {
    assertEquals(
        "first\nsecond\n\nthird\n\nfourth",
        cleaner.clean("first\nsecond\n\n\n\nthird\n\n\nfourth"));
  }

  @Test
  void shouldNormalizeWindowsLineBreaksAndHandleEmptyInput() {
    assertEquals("one\ntwo", cleaner.clean("one\r\ntwo"));
    assertEquals("", cleaner.clean(" \t\r\n "));
    assertEquals("", cleaner.clean(null));
  }
}
