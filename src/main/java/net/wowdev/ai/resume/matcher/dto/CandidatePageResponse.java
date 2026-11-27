package net.wowdev.ai.resume.matcher.dto;

import java.util.List;

public record CandidatePageResponse(
    List<CandidateSummaryResponse> items,
    int page,
    int pageSize,
    long totalElements,
    int totalPages) {

  public CandidatePageResponse {
    items = items == null ? List.of() : List.copyOf(items);
  }
}
