package net.wowdev.ai.resume.matcher.matching;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.wowdev.ai.resume.matcher.model.Candidate;
import net.wowdev.ai.resume.matcher.model.JobOffer;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

@Service
public class CandidateMatchRanker {

  private final ChatModel chatModel;
  private final CandidateMatchPromptBuilder promptBuilder;
  private final CandidateRankingResponseValidator responseValidator;
  private final CandidateMatchingProperties properties;

  public CandidateMatchRanker(
      ChatModel chatModel,
      CandidateMatchPromptBuilder promptBuilder,
      CandidateRankingResponseValidator responseValidator,
      CandidateMatchingProperties properties) {
    this.chatModel = chatModel;
    this.promptBuilder = promptBuilder;
    this.responseValidator = responseValidator;
    this.properties = properties;
  }

  public List<List<CandidateChunkRanking>> rankChunks(
      JobOffer position, List<Candidate> candidates) {
    List<List<Candidate>> chunks = partition(candidates);
    if (chunks.isEmpty()) {
      return List.of();
    }

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      CompletionService<ChunkResult> completionService = new ExecutorCompletionService<>(executor);
      List<Future<ChunkResult>> futures = new ArrayList<>();
      for (int index = 0; index < chunks.size(); index++) {
        int chunkIndex = index;
        List<Candidate> chunk = chunks.get(index);
        futures.add(
            completionService.submit(
                () -> new ChunkResult(chunkIndex, rankChunk(position, chunk))));
      }

      List<List<CandidateChunkRanking>> results =
          new ArrayList<>(java.util.Collections.nCopies(chunks.size(), null));
      try {
        for (int completed = 0; completed < chunks.size(); completed++) {
          ChunkResult result = completionService.take().get();
          results.set(result.index(), result.rankings());
        }
      } catch (InterruptedException exception) {
        cancel(futures);
        Thread.currentThread().interrupt();
        throw new CandidateChunkRankingException();
      } catch (ExecutionException | CancellationException exception) {
        cancel(futures);
        throw new CandidateChunkRankingException();
      }
      return results.stream().map(List::copyOf).toList();
    }
  }

  private List<CandidateChunkRanking> rankChunk(JobOffer position, List<Candidate> chunk) {
    String promptText = promptBuilder.build(position, chunk);
    ChatResponse response = chatModel.call(new Prompt(promptText));
    if (response == null
        || response.getResult() == null
        || response.getResult().getOutput() == null
        || response.getResult().getOutput().getText() == null) {
      throw new CandidateChunkRankingException();
    }
    return responseValidator.validate(response.getResult().getOutput().getText(), chunk);
  }

  private List<List<Candidate>> partition(List<Candidate> candidates) {
    List<List<Candidate>> chunks = new ArrayList<>();
    for (int start = 0; start < candidates.size(); start += properties.getChunkSize()) {
      int end = Math.min(start + properties.getChunkSize(), candidates.size());
      chunks.add(List.copyOf(candidates.subList(start, end)));
    }
    return List.copyOf(chunks);
  }

  private void cancel(List<Future<ChunkResult>> futures) {
    futures.forEach(future -> future.cancel(true));
  }

  private record ChunkResult(int index, List<CandidateChunkRanking> rankings) {}
}
