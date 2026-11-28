package net.wowdev.ai.resume.matcher.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.wowdev.ai.resume.matcher.model.Candidate;
import net.wowdev.ai.resume.matcher.model.JobOffer;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ClassPathResource;

class CandidateMatchRankerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final CandidateMatchPromptBuilder promptBuilder =
      new CandidateMatchPromptBuilder(
          objectMapper, new ClassPathResource("prompts/rank-candidates.st"));
  private final CandidateRankingResponseValidator responseValidator =
      new CandidateRankingResponseValidator(objectMapper);

  @Test
  void shouldRunOneVirtualThreadPerChunkAndJoinResultsInChunkOrder() throws Exception {
    ChatModel chatModel = mock(ChatModel.class);
    CountDownLatch workersEntered = new CountDownLatch(3);
    ConcurrentLinkedQueue<Boolean> virtualThreads = new ConcurrentLinkedQueue<>();
    AtomicInteger calls = new AtomicInteger();
    when(chatModel.call(any(Prompt.class)))
        .thenAnswer(
            invocation -> {
              virtualThreads.add(Thread.currentThread().isVirtual());
              calls.incrementAndGet();
              workersEntered.countDown();
              if (!workersEntered.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Chunk workers did not run in parallel");
              }
              return responseFor(invocation.getArgument(0));
            });
    CandidateMatchRanker ranker = ranker(chatModel, 2);

    List<List<CandidateChunkRanking>> result = ranker.rankChunks(position(), candidates(5));

    assertThat(calls).hasValue(3);
    assertThat(virtualThreads).containsOnly(true);
    assertThat(result).extracting(List::size).containsExactly(2, 2, 1);
    assertThat(result.get(0))
        .extracting(CandidateChunkRanking::candidateId)
        .containsExactly("candidate-1", "candidate-2");
    assertThat(result.get(1))
        .extracting(CandidateChunkRanking::candidateId)
        .containsExactly("candidate-3", "candidate-4");
    assertThat(result.get(2))
        .extracting(CandidateChunkRanking::candidateId)
        .containsExactly("candidate-5");
  }

  @Test
  void shouldCancelOtherWorkersAndFailWithoutReturningPartialRankings() throws Exception {
    ChatModel chatModel = mock(ChatModel.class);
    CountDownLatch otherWorkersEntered = new CountDownLatch(2);
    AtomicInteger interruptedWorkers = new AtomicInteger();
    when(chatModel.call(any(Prompt.class)))
        .thenAnswer(
            invocation -> {
              String prompt = ((Prompt) invocation.getArgument(0)).getContents();
              if (prompt.contains("candidate-1")) {
                if (!otherWorkersEntered.await(5, TimeUnit.SECONDS)) {
                  throw new IllegalStateException("Other chunk workers did not start");
                }
                throw new IllegalStateException("Sensitive provider failure");
              }
              otherWorkersEntered.countDown();
              try {
                new CountDownLatch(1).await();
                return responseFor(invocation.getArgument(0));
              } catch (InterruptedException exception) {
                interruptedWorkers.incrementAndGet();
                Thread.currentThread().interrupt();
                throw exception;
              }
            });
    CandidateMatchRanker ranker = ranker(chatModel, 1);

    assertThatThrownBy(() -> ranker.rankChunks(position(), candidates(3)))
        .isInstanceOf(CandidateChunkRankingException.class)
        .hasMessage("Candidate chunk ranking failed");
    assertThat(interruptedWorkers).hasValue(2);
  }

  @Test
  void shouldSkipModelCallsWhenCandidatePoolIsEmpty() {
    ChatModel chatModel = mock(ChatModel.class);
    CandidateMatchRanker ranker = ranker(chatModel, 2);

    assertThat(ranker.rankChunks(position(), List.of())).isEmpty();

    verifyNoInteractions(chatModel);
  }

  @Test
  void shouldFailWhenModelReturnsNoResponse() {
    ChatModel chatModel = mock(ChatModel.class);
    when(chatModel.call(any(Prompt.class))).thenReturn(null);
    CandidateMatchRanker ranker = ranker(chatModel, 2);

    assertThatThrownBy(() -> ranker.rankChunks(position(), candidates(1)))
        .isInstanceOf(CandidateChunkRankingException.class)
        .hasMessage("Candidate chunk ranking failed");
  }

  private ChatResponse responseFor(Prompt prompt) throws Exception {
    String contents = prompt.getContents();
    String marker = "Ranking input:\n";
    JsonNode context =
        objectMapper.readTree(contents.substring(contents.indexOf(marker) + marker.length()));
    ArrayNode candidates = (ArrayNode) context.path("candidates");
    ObjectNode response = objectMapper.createObjectNode();
    ArrayNode items = response.putArray("items");
    for (int index = 0; index < candidates.size(); index++) {
      ObjectNode item = items.addObject();
      item.put("candidateId", candidates.get(index).path("candidateId").asText());
      item.put("rank", index + 1);
      item.put("matchPercentage", 90 - index * 10);
      item.put("explanation", "Evidence: relevant experience. Gaps: none established.");
    }
    return new ChatResponse(
        List.of(new Generation(new AssistantMessage(objectMapper.writeValueAsString(response)))));
  }

  private CandidateMatchRanker ranker(ChatModel chatModel, int chunkSize) {
    return new CandidateMatchRanker(
        chatModel,
        promptBuilder,
        responseValidator,
        new CandidateMatchingProperties("10", "50", String.valueOf(chunkSize)));
  }

  private JobOffer position() {
    return new JobOffer(
        "position-1",
        "JD-00000001",
        "Example Co.",
        "Software Engineer",
        "Build backend services",
        "Cloud platform engineer",
        5,
        "Remote",
        "Remote");
  }

  private List<Candidate> candidates(int count) {
    return java.util.stream.IntStream.rangeClosed(1, count)
        .mapToObj(index -> candidate("candidate-" + index))
        .toList();
  }

  private Candidate candidate(String id) {
    return new Candidate(
        id,
        id + "@example.com",
        "Candidate " + id,
        "Brazil",
        "São Paulo",
        "Engineer",
        "Java",
        "Remote",
        "5 years and 0 months",
        List.of());
  }
}
