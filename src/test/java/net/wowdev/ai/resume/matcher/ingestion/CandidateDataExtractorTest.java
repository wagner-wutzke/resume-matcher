package net.wowdev.ai.resume.matcher.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ClassPathResource;

class CandidateDataExtractorTest {

  @Test
  void shouldRequestAndReturnOneJsonCandidateObject() {
    ChatModel chatModel = mock(ChatModel.class);
    String json = "{\"email\":\"jane@example.com\",\"experiences\":[]}";
    when(chatModel.call(any(Prompt.class)))
        .thenReturn(
            new ChatResponse(java.util.List.of(new Generation(new AssistantMessage(json)))));

    String result = extractor(chatModel).extract("clean resume text");

    assertEquals(json, result);
    org.mockito.ArgumentCaptor<Prompt> prompt = org.mockito.ArgumentCaptor.forClass(Prompt.class);
    verify(chatModel).call(prompt.capture());
    org.junit.jupiter.api.Assertions.assertTrue(
        prompt.getValue().getContents().contains("clean resume text"));
    org.junit.jupiter.api.Assertions.assertTrue(
        prompt.getValue().getContents().contains("exactly one JSON object"));
  }

  @Test
  void shouldSanitizeModelFailure() {
    ChatModel chatModel = mock(ChatModel.class);
    when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("sensitive text"));

    ResumeIngestionException failure =
        assertThrows(
            ResumeIngestionException.class,
            () -> extractor(chatModel).extract("resume"));

    assertEquals("Candidate extraction failed", failure.getMessage());
  }

  @Test
  void shouldRejectNullAndBlankModelResponses() {
    ChatModel nullModel = mock(ChatModel.class);
    when(nullModel.call(any(Prompt.class))).thenReturn(new ChatResponse(java.util.List.of()));
    assertThrows(
        ResumeIngestionException.class,
        () -> extractor(nullModel).extract("resume"));

    ChatModel blankModel = mock(ChatModel.class);
    when(blankModel.call(any(Prompt.class)))
        .thenReturn(
            new ChatResponse(java.util.List.of(new Generation(new AssistantMessage("  ")))));
    assertThrows(
        ResumeIngestionException.class,
        () -> extractor(blankModel).extract("resume"));
  }

  private CandidateDataExtractor extractor(ChatModel chatModel) {
    return new CandidateDataExtractor(
        chatModel, new ClassPathResource("prompts/extract-candidate-data.st"));
  }
}
