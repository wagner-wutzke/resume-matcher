package net.wowdev.ai.resume.matcher.matching;

public record CandidateChunkRanking(
    String candidateId, int rank, int matchPercentage, String explanation) {}
