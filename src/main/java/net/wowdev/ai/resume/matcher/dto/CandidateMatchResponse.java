package net.wowdev.ai.resume.matcher.dto;

import java.util.List;

public record CandidateMatchResponse(String positionId, List<CandidateMatchItemResponse> items) {

  public CandidateMatchResponse {
    items = items == null ? List.of() : List.copyOf(items);
  }
}
