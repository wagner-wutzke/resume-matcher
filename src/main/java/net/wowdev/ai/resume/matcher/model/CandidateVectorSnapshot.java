package net.wowdev.ai.resume.matcher.model;

import java.util.List;

public record CandidateVectorSnapshot(List<CandidateVectorPoint> points) {

  public CandidateVectorSnapshot {
    points = List.copyOf(points);
  }

  public static CandidateVectorSnapshot empty() {
    return new CandidateVectorSnapshot(List.of());
  }
}
