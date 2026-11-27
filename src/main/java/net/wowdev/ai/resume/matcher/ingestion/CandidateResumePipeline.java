package net.wowdev.ai.resume.matcher.ingestion;

import java.nio.file.Path;
import net.wowdev.ai.resume.matcher.model.Candidate;
import org.springframework.stereotype.Component;

@Component
@ResumeIngestionEnabled
public class CandidateResumePipeline {

  private static final java.util.List<IngestionStage> STAGES =
      java.util.List.of(
          IngestionStage.READINESS,
          IngestionStage.PDF_EXTRACTION,
          IngestionStage.TEXT_CLEANUP,
          IngestionStage.LLM_EXTRACTION,
          IngestionStage.VALIDATION,
          IngestionStage.EXPERIENCE_CALCULATION);

  private final ResumeFileReadinessGate readinessGate;
  private final ResumePdfTextExtractor pdfTextExtractor;
  private final ResumeTextCleaner textCleaner;
  private final CandidateDataExtractor dataExtractor;
  private final CandidateDataValidator dataValidator;
  private final ExperienceDurationCalculator durationCalculator;
  private final ResumeIngestionStageLogger stageLogger = new ResumeIngestionStageLogger();

  public CandidateResumePipeline(
      ResumeFileReadinessGate readinessGate,
      ResumePdfTextExtractor pdfTextExtractor,
      ResumeTextCleaner textCleaner,
      CandidateDataExtractor dataExtractor,
      CandidateDataValidator dataValidator,
      ExperienceDurationCalculator durationCalculator) {
    this.readinessGate = readinessGate;
    this.pdfTextExtractor = pdfTextExtractor;
    this.textCleaner = textCleaner;
    this.dataExtractor = dataExtractor;
    this.dataValidator = dataValidator;
    this.durationCalculator = durationCalculator;
  }

  public Candidate prepare(Path pdfFile) {
    int activeStage = 0;
    try {
      stageLogger.execute(
          pdfFile, STAGES.get(activeStage), () -> readinessGate.awaitReady(pdfFile));
      activeStage++;
      String extractedText =
          stageLogger.execute(
              pdfFile, STAGES.get(activeStage), () -> pdfTextExtractor.extract(pdfFile));
      activeStage++;
      String cleanedText =
          stageLogger.execute(
              pdfFile, STAGES.get(activeStage), () -> cleanUsableText(extractedText));
      activeStage++;
      String structuredData =
          stageLogger.execute(
              pdfFile, STAGES.get(activeStage), () -> dataExtractor.extract(cleanedText));
      activeStage++;
      Candidate extractedCandidate =
          stageLogger.execute(
              pdfFile, STAGES.get(activeStage), () -> dataValidator.validate(structuredData));
      activeStage++;
      String yearsOfExperience =
          stageLogger.execute(
              pdfFile,
              STAGES.get(activeStage),
              () -> durationCalculator.calculate(extractedCandidate.experiences()));
      return withDerivedExperience(extractedCandidate, yearsOfExperience);
    } catch (RuntimeException exception) {
      for (int index = activeStage + 1; index < STAGES.size(); index++) {
        stageLogger.skipped(pdfFile, STAGES.get(index));
      }
      throw exception;
    }
  }

  private String cleanUsableText(String extractedText) {
    String cleanedText = textCleaner.clean(extractedText);
    if (cleanedText.isBlank()) {
      throw new ResumeIngestionException(
          "PDF contains no usable text",
          ResumeIngestionException.FailureCause.NO_EXTRACTABLE_TEXT);
    }
    return cleanedText;
  }

  private Candidate withDerivedExperience(Candidate candidate, String yearsOfExperience) {
    return new Candidate(
        candidate.id(),
        candidate.email(),
        candidate.fullname(),
        candidate.country(),
        candidate.city(),
        candidate.position(),
        candidate.skills(),
        candidate.allocation(),
        yearsOfExperience,
        candidate.experiences());
  }
}
