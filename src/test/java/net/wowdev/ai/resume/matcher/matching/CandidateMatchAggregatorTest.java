package net.wowdev.ai.resume.matcher.matching;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import net.wowdev.ai.resume.matcher.model.Candidate;
import org.junit.jupiter.api.Test;

class CandidateMatchAggregatorTest {

  @Test
  void shouldNormalizeRanksAcrossDifferentChunkSizesAndApplyDeterministicTieBreak() {
    CandidateMatchAggregator aggregator = aggregator(10);
    List<Candidate> candidates =
        List.of(candidate("z"), candidate("a"), candidate("m"), candidate("b"), candidate("c"));
    List<List<CandidateChunkRanking>> chunkRankings =
        List.of(
            List.of(ranking("z", 1, 5), ranking("a", 2, 99), ranking("m", 3, 100)),
            List.of(ranking("b", 1, 0), ranking("c", 2, 100)));

    List<RankedCandidate> result = aggregator.aggregate(candidates, chunkRankings);

    assertThat(result).extracting(RankedCandidate::rank).containsExactly(1, 2, 3, 4, 5);
    assertThat(result)
        .extracting(item -> item.candidate().id())
        .containsExactly("b", "z", "a", "c", "m");
    assertThat(result)
        .extracting(RankedCandidate::matchPercentage)
        .containsExactly(0, 5, 99, 100, 100);
  }

  @Test
  void shouldNormalizeSingleCandidateChunkAsFirstPlaceAndTruncateAfterOrdering() {
    CandidateMatchAggregator aggregator = aggregator(2);
    List<Candidate> candidates = List.of(candidate("solo"), candidate("later"));
    List<List<CandidateChunkRanking>> chunkRankings =
        List.of(List.of(ranking("solo", 1, 1)), List.of(ranking("later", 1, 99)));

    List<RankedCandidate> result = aggregator.aggregate(candidates, chunkRankings);

    assertThat(result).extracting(RankedCandidate::rank).containsExactly(1, 2);
    assertThat(result).extracting(item -> item.candidate().id()).containsExactly("later", "solo");
  }

  @Test
  void shouldAssignFinalRanksAfterTruncatingToConfiguredLimit() {
    CandidateMatchAggregator aggregator = aggregator(2);
    List<Candidate> candidates =
        List.of(candidate("first"), candidate("second"), candidate("third"));
    List<List<CandidateChunkRanking>> chunkRankings =
        List.of(
            List.of(ranking("first", 1, 1), ranking("second", 2, 100)),
            List.of(ranking("third", 1, 0)));

    List<RankedCandidate> result = aggregator.aggregate(candidates, chunkRankings);

    assertThat(result).extracting(RankedCandidate::rank).containsExactly(1, 2);
    assertThat(result).extracting(item -> item.candidate().id()).containsExactly("first", "third");
    assertThat(result).extracting(RankedCandidate::matchPercentage).containsExactly(1, 0);
  }

  private CandidateMatchAggregator aggregator(int maxResults) {
    return new CandidateMatchAggregator(
        new CandidateMatchingProperties("" + maxResults, "50", "10"));
  }

  private CandidateChunkRanking ranking(String candidateId, int rank) {
    return ranking(candidateId, rank, 80);
  }

  private CandidateChunkRanking ranking(String candidateId, int rank, int matchPercentage) {
    return new CandidateChunkRanking(
        candidateId, rank, matchPercentage, "Evidence: relevant. Gaps: none established.");
  }

  private Candidate candidate(String id) {
    return new Candidate(
        id,
        id + "@example.com",
        id,
        "Brazil",
        "São Paulo",
        "Engineer",
        "Java",
        "Remote",
        "5 years",
        List.of());
  }
}
