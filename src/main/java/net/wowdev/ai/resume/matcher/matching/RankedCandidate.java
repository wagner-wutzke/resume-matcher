package net.wowdev.ai.resume.matcher.matching;

import net.wowdev.ai.resume.matcher.model.Candidate;

public record RankedCandidate(
    Candidate candidate, int rank, int matchPercentage, String explanation) {}
