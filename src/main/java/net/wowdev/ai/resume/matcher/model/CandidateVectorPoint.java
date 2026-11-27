package net.wowdev.ai.resume.matcher.model;

import java.util.List;
import java.util.Map;

public record CandidateVectorPoint(
    String id, Map<String, List<Float>> vectors, Map<String, String> payload) {

  public CandidateVectorPoint {
    vectors = vectors.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
        Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    payload = Map.copyOf(payload);
  }
}
