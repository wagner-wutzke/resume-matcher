package net.wowdev.ai.resume.matcher.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;

class ResumePdfTextExtractorTest {

  @Test
  void shouldJoinExtractedPagesInPageOrder() {
    PagePdfDocumentReader reader = mock(PagePdfDocumentReader.class);
    when(reader.get()).thenReturn(List.of(new Document("Page one"), new Document("Page two")));
    ResumePdfTextExtractor extractor = new ResumePdfTextExtractor(resource -> reader);

    assertEquals("Page one\nPage two", extractor.extract(Path.of("resume.pdf")));
  }

  @Test
  void shouldRejectPdfWithoutExtractableText() {
    PagePdfDocumentReader reader = mock(PagePdfDocumentReader.class);
    when(reader.get()).thenReturn(List.of(new Document("  ")));
    ResumePdfTextExtractor extractor = new ResumePdfTextExtractor(resource -> reader);

    assertThrows(
        ResumeIngestionException.class, () -> extractor.extract(Path.of("image-only.pdf")));
  }

  @Test
  void shouldConvertReaderFailureToSanitizedProcessingError() {
    PagePdfDocumentReader reader = mock(PagePdfDocumentReader.class);
    when(reader.get()).thenThrow(new IllegalArgumentException("private resume content"));
    ResumePdfTextExtractor extractor = new ResumePdfTextExtractor(resource -> reader);

    ResumeIngestionException failure =
        assertThrows(
            ResumeIngestionException.class, () -> extractor.extract(Path.of("broken.pdf")));

    assertEquals("PDF extraction failed", failure.getMessage());
  }
}
