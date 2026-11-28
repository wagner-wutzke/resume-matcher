package net.wowdev.ai.resume.matcher.matching;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.wowdev.ai.resume.matcher.model.Candidate;
import org.springframework.stereotype.Component;

@Component
public class CandidateRankingResponseValidator {

  private final ObjectMapper objectMapper;

  public CandidateRankingResponseValidator(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public List<CandidateChunkRanking> validate(String response, List<Candidate> candidates) {
    Set<String> expectedIds = expectedCandidateIds(candidates);
    JsonNode root = parse(response);
    if (!root.isObject() || root.size() != 1 || !root.path("items").isArray()) {
      throw invalidResponse();
    }

    JsonNode items = root.path("items");
    if (items.size() != expectedIds.size()) {
      throw invalidResponse();
    }

    List<CandidateChunkRanking> rankings = new ArrayList<>();
    Set<String> seenCandidateIds = new HashSet<>();
    Set<Integer> seenRanks = new HashSet<>();
    for (JsonNode item : items) {
      CandidateChunkRanking ranking = parseItem(item, expectedIds, seenCandidateIds, seenRanks);
      rankings.add(ranking);
    }
    if (!seenCandidateIds.equals(expectedIds)
        || !hasContiguousRanks(seenRanks, expectedIds.size())) {
      throw invalidResponse();
    }
    return rankings.stream()
        .sorted((left, right) -> Integer.compare(left.rank(), right.rank()))
        .toList();
  }

  private Set<String> expectedCandidateIds(List<Candidate> candidates) {
    Set<String> expectedIds = new HashSet<>();
    for (Candidate candidate : candidates) {
      String candidateId = candidate.id();
      if (candidateId == null || candidateId.isBlank() || !expectedIds.add(candidateId)) {
        throw invalidResponse();
      }
    }
    return expectedIds;
  }

  private JsonNode parse(String response) {
    if (response == null || response.isBlank()) {
      throw invalidResponse();
    }
    try {
      return objectMapper.readTree(response);
    } catch (JsonProcessingException exception) {
      throw invalidResponse();
    }
  }

  private CandidateChunkRanking parseItem(
      JsonNode item,
      Set<String> expectedIds,
      Set<String> seenCandidateIds,
      Set<Integer> seenRanks) {
    if (!item.isObject()
        || item.size() != 4
        || !item.path("candidateId").isTextual()
        || !item.path("rank").isIntegralNumber()
        || !item.path("rank").canConvertToInt()
        || !item.path("matchPercentage").isIntegralNumber()
        || !item.path("matchPercentage").canConvertToInt()
        || !item.path("explanation").isTextual()) {
      throw invalidResponse();
    }

    String candidateId = item.path("candidateId").asText();
    int rank = item.path("rank").intValue();
    int matchPercentage = item.path("matchPercentage").intValue();
    String explanation = item.path("explanation").asText();
    if (!expectedIds.contains(candidateId)
        || !seenCandidateIds.add(candidateId)
        || rank < 1
        || !seenRanks.add(rank)
        || matchPercentage < 0
        || matchPercentage > 100
        || explanation.isBlank()) {
      throw invalidResponse();
    }
    return new CandidateChunkRanking(candidateId, rank, matchPercentage, explanation);
  }

  private boolean hasContiguousRanks(Set<Integer> ranks, int count) {
    if (ranks.size() != count) {
      return false;
    }
    for (int rank = 1; rank <= count; rank++) {
      if (!ranks.contains(rank)) {
        return false;
      }
    }
    return true;
  }

  private CandidateRankingResponseException invalidResponse() {
    return new CandidateRankingResponseException();
  }
}
