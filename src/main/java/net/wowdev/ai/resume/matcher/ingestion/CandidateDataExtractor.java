package net.wowdev.ai.resume.matcher.ingestion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@ResumeIngestionEnabled
public class CandidateDataExtractor {

  private final ChatModel chatModel;
  private final String instructions;

  public CandidateDataExtractor(
      ChatModel chatModel,
      @Value("classpath:prompts/extract-candidate-data.st") Resource promptResource) {
    this.chatModel = chatModel;
    try {
      this.instructions = promptResource.getContentAsString(StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot load candidate extraction prompt");
    }
  }

  public String extract(String cleanedResumeText) {
    try {
      ChatResponse response = chatModel.call(new Prompt(instructions.formatted(cleanedResumeText)));
      if (response == null
          || response.getResult() == null
          || response.getResult().getOutput() == null) {
        throw llmUnavailable();
      }
      String result = response.getResult().getOutput().getText();
      if (result == null || result.isBlank()) {
        throw llmUnavailable();
      }
      return result;
    } catch (ResumeIngestionException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw llmUnavailable();
    }
  }

  private ResumeIngestionException llmUnavailable() {
    return new ResumeIngestionException(
        "Candidate extraction failed", ResumeIngestionException.FailureCause.LLM_UNAVAILABLE);
  }
}
