package net.wowdev.ai.resume.matcher.matching;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CandidateMatchingProperties {

  private final int maxResults;
  private final int poolCap;
  private final int chunkSize;

  public CandidateMatchingProperties(
      @Value("${candidate.matching.max-results:10}") String maxResults,
      @Value("${candidate.matching.pool-cap:20}") String poolCap,
      @Value("${candidate.matching.chunk-size:10}") String chunkSize) {
    this.maxResults = parsePositiveInteger(maxResults, "candidate.matching.max-results");
    this.poolCap = parsePositiveInteger(poolCap, "candidate.matching.pool-cap");
    this.chunkSize = parsePositiveInteger(chunkSize, "candidate.matching.chunk-size");
  }

  public int getMaxResults() {
    return maxResults;
  }

  public int getPoolCap() {
    return poolCap;
  }

  public int getChunkSize() {
    return chunkSize;
  }

  private static int parsePositiveInteger(String value, String propertyName) {
    try {
      int parsed = Integer.parseInt(value.trim());
      if (parsed > 0) {
        return parsed;
      }
    } catch (NumberFormatException ignored) {
      // Report configuration errors consistently without exposing a conversion exception.
    }
    throw new IllegalStateException(propertyName + " must be a positive integer");
  }
}
