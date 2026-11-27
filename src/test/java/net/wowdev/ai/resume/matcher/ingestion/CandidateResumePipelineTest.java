package net.wowdev.ai.resume.matcher.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.wowdev.ai.resume.matcher.model.Candidate;
import net.wowdev.ai.resume.matcher.model.CandidateExperience;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.slf4j.LoggerFactory;

class CandidateResumePipelineTest {

  @Test
  void shouldLogEachPipelineStageWithFilenameAndOutcome() {
    ResumeFileReadinessGate readiness = mock(ResumeFileReadinessGate.class);
    ResumePdfTextExtractor pdf = mock(ResumePdfTextExtractor.class);
    ResumeTextCleaner cleaner = mock(ResumeTextCleaner.class);
    CandidateDataExtractor extractor = mock(CandidateDataExtractor.class);
    CandidateDataValidator validator = mock(CandidateDataValidator.class);
    ExperienceDurationCalculator calculator = mock(ExperienceDurationCalculator.class);
    Path file = Path.of("private-resume.pdf");
    Candidate candidate = candidate("untrusted LLM total", List.of(experience("2020 - 2022")));
    when(pdf.extract(file)).thenReturn("raw text");
    when(cleaner.clean("raw text")).thenReturn("clean text");
    when(extractor.extract("clean text")).thenReturn("structured JSON");
    when(validator.validate("structured JSON")).thenReturn(candidate);
    when(calculator.calculate(candidate.experiences())).thenReturn("2 years and 0 months");
    CandidateResumePipeline pipeline =
        new CandidateResumePipeline(readiness, pdf, cleaner, extractor, validator, calculator);
    Logger logger = (Logger) LoggerFactory.getLogger(ResumeIngestionStageLogger.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);

    try {
      pipeline.prepare(file);

      String logs =
          appender.list.stream()
              .map(ILoggingEvent::getFormattedMessage)
              .reduce("", (left, right) -> left + "\n" + right);
      for (IngestionStage stage :
          List.of(
              IngestionStage.READINESS,
              IngestionStage.PDF_EXTRACTION,
              IngestionStage.TEXT_CLEANUP,
              IngestionStage.LLM_EXTRACTION,
              IngestionStage.VALIDATION,
              IngestionStage.EXPERIENCE_CALCULATION)) {
        String stageCode = stage.code();
        assertTrue(logs.contains("stage started file=private-resume.pdf stage=" + stageCode));
        assertTrue(logs.contains("file=private-resume.pdf stage=" + stageCode));
        assertTrue(logs.contains("stage=" + stageCode + " outcome=succeeded"));
      }
      assertTrue(logs.contains("elapsedMs="));
      assertFalse(logs.contains("jane@example.com"));
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }

  @Test
  void shouldRunExtractionValidationAndExperienceCalculationInOrder() {
    ResumeFileReadinessGate readiness = mock(ResumeFileReadinessGate.class);
    ResumePdfTextExtractor pdf = mock(ResumePdfTextExtractor.class);
    ResumeTextCleaner cleaner = mock(ResumeTextCleaner.class);
    CandidateDataExtractor extractor = mock(CandidateDataExtractor.class);
    CandidateDataValidator validator = mock(CandidateDataValidator.class);
    ExperienceDurationCalculator calculator = mock(ExperienceDurationCalculator.class);
    Path file = Path.of("resume.pdf");
    Candidate candidate = candidate("untrusted LLM total", List.of(experience("2020 - 2022")));
    when(pdf.extract(file)).thenReturn(" raw text ");
    when(cleaner.clean(" raw text ")).thenReturn("clean text");
    when(extractor.extract("clean text")).thenReturn("structured JSON");
    when(validator.validate("structured JSON")).thenReturn(candidate);
    when(calculator.calculate(candidate.experiences())).thenReturn("2 years and 0 months");
    CandidateResumePipeline pipeline =
        new CandidateResumePipeline(readiness, pdf, cleaner, extractor, validator, calculator);

    Candidate result = pipeline.prepare(file);

    assertEquals("2 years and 0 months", result.yearsOfExperience());
    InOrder order = inOrder(readiness, pdf, cleaner, extractor, validator, calculator);
    order.verify(readiness).awaitReady(file);
    order.verify(pdf).extract(file);
    order.verify(cleaner).clean(" raw text ");
    order.verify(extractor).extract("clean text");
    order.verify(validator).validate("structured JSON");
    order.verify(calculator).calculate(candidate.experiences());
  }

  @Test
  void shouldStopBeforeLaterStagesWhenPdfExtractionFails() {
    ResumeFileReadinessGate readiness = mock(ResumeFileReadinessGate.class);
    ResumePdfTextExtractor pdf = mock(ResumePdfTextExtractor.class);
    ResumeTextCleaner cleaner = mock(ResumeTextCleaner.class);
    CandidateDataExtractor extractor = mock(CandidateDataExtractor.class);
    CandidateDataValidator validator = mock(CandidateDataValidator.class);
    ExperienceDurationCalculator calculator = mock(ExperienceDurationCalculator.class);
    when(pdf.extract(Path.of("broken.pdf")))
        .thenThrow(new ResumeIngestionException("PDF extraction failed"));
    CandidateResumePipeline pipeline =
        new CandidateResumePipeline(readiness, pdf, cleaner, extractor, validator, calculator);

    assertThrows(ResumeIngestionException.class, () -> pipeline.prepare(Path.of("broken.pdf")));

    verify(cleaner, never()).clean(org.mockito.ArgumentMatchers.anyString());
    verify(extractor, never()).extract(org.mockito.ArgumentMatchers.anyString());
    verify(validator, never()).validate(org.mockito.ArgumentMatchers.anyString());
    verify(calculator, never()).calculate(org.mockito.ArgumentMatchers.anyList());
  }

  private Candidate candidate(String years, List<CandidateExperience> experiences) {
    return new Candidate(null, "jane@example.com", "Jane", "", "", "", "", "", years, experiences);
  }

  private CandidateExperience experience(String period) {
    return new CandidateExperience("Developer", "Acme", period, "Built", "Improved");
  }
}
