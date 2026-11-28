package net.wowdev.ai.resume.matcher.matching;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.wowdev.ai.resume.matcher.model.Candidate;
import org.springframework.stereotype.Component;

@Component
public class CandidateMatchAggregator {

  private final CandidateMatchingProperties properties;

  public CandidateMatchAggregator(CandidateMatchingProperties properties) {
    this.properties = properties;
  }

  public List<RankedCandidate> aggregate(
      List<Candidate> candidates, List<List<CandidateChunkRanking>> chunkRankings) {
    List<RankedCandidateWithScore> rankedCandidates = new ArrayList<>();
    int candidateOffset = 0;
    for (List<CandidateChunkRanking> chunk : chunkRankings) {
      int chunkSize = chunk.size();
      Map<String, Candidate> candidatesById = new HashMap<>();
      for (int index = candidateOffset; index < candidateOffset + chunkSize; index++) {
        Candidate candidate = candidates.get(index);
        candidatesById.put(candidate.id(), candidate);
      }
      for (CandidateChunkRanking ranking : chunk) {
        Candidate candidate = candidatesById.get(ranking.candidateId());
        rankedCandidates.add(
            new RankedCandidateWithScore(
                candidate,
                ranking.explanation(),
                ranking.matchPercentage(),
                normalizedRank(ranking.rank(), chunkSize)));
      }
      candidateOffset += chunkSize;
    }

    rankedCandidates.sort(
        Comparator.comparingDouble(RankedCandidateWithScore::normalizedRank)
            .reversed()
            .thenComparing(result -> result.candidate().id()));

    List<RankedCandidate> limitedResults = new ArrayList<>();
    for (int index = 0;
        index < Math.min(rankedCandidates.size(), properties.getMaxResults());
        index++) {
      RankedCandidateWithScore result = rankedCandidates.get(index);
      limitedResults.add(
          new RankedCandidate(
              result.candidate(), index + 1, result.matchPercentage(), result.explanation()));
    }
    return List.copyOf(limitedResults);
  }

  private double normalizedRank(int rank, int chunkSize) {
    if (chunkSize == 1) {
      return 1;
    }
    return (double) (chunkSize - rank) / (chunkSize - 1);
  }

  private record RankedCandidateWithScore(
      Candidate candidate, String explanation, int matchPercentage, double normalizedRank) {}
}
