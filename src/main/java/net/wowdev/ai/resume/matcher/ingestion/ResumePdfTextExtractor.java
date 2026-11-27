package net.wowdev.ai.resume.matcher.ingestion;

import java.nio.file.Path;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

@Component
public class ResumePdfTextExtractor {

  private final PdfReaderFactory readerFactory;

  public ResumePdfTextExtractor() {
    this(PagePdfDocumentReader::new);
  }

  ResumePdfTextExtractor(PdfReaderFactory readerFactory) {
    this.readerFactory = readerFactory;
  }

  public String extract(Path pdfFile) {
    try {
      List<Document> pages = readerFactory.create(new FileSystemResource(pdfFile)).get();
      String text =
          pages.stream()
              .map(Document::getText)
              .filter(value -> value != null)
              .reduce((left, right) -> left + "\n" + right)
              .orElse("");
      if (text.isBlank()) {
        throw new ResumeIngestionException(
            "PDF contains no extractable text",
            ResumeIngestionException.FailureCause.NO_EXTRACTABLE_TEXT);
      }
      return text;
    } catch (ResumeIngestionException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new ResumeIngestionException(
          "PDF extraction failed", ResumeIngestionException.FailureCause.PDF_UNREADABLE);
    }
  }

  @FunctionalInterface
  interface PdfReaderFactory {
    PagePdfDocumentReader create(FileSystemResource resource);
  }
}
