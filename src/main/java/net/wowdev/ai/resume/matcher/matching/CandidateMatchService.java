package net.wowdev.ai.resume.matcher.matching;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.wowdev.ai.resume.matcher.dto.CandidateMatchItemResponse;
import net.wowdev.ai.resume.matcher.dto.CandidateMatchResponse;
import net.wowdev.ai.resume.matcher.model.Candidate;
import net.wowdev.ai.resume.matcher.model.JobOffer;
import net.wowdev.ai.resume.matcher.repository.JobOfferRepository;
import net.wowdev.ai.resume.matcher.service.JobOfferNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class CandidateMatchService {

  private static final Logger logger = LoggerFactory.getLogger(CandidateMatchService.class);

  private final JobOfferRepository jobOfferRepository;
  private final CandidateSemanticRetriever semanticRetriever;
  private final CandidatePoolResolver poolResolver;
  private final CandidateMatchRanker ranker;
  private final CandidateMatchAggregator aggregator;

  public CandidateMatchService(
      JobOfferRepository jobOfferRepository,
      CandidateSemanticRetriever semanticRetriever,
      CandidatePoolResolver poolResolver,
      CandidateMatchRanker ranker,
      CandidateMatchAggregator aggregator) {
    this.jobOfferRepository = jobOfferRepository;
    this.semanticRetriever = semanticRetriever;
    this.poolResolver = poolResolver;
    this.ranker = ranker;
    this.aggregator = aggregator;
  }

  public CandidateMatchResponse findMatches(String positionId) {
    JobOffer position = loadPosition(positionId);
    Set<String> retrievedEmails;
    try {
      retrievedEmails = semanticRetriever.retrieveCandidateEmails(semanticQuery(position));
      logSuccess("semantic-retrieval", "completed");
    } catch (RuntimeException exception) {
      logFailure("semantic-retrieval", "retrieval-unavailable");
      throw new CandidateMatchingUnavailableException(exception);
    }

    List<Candidate> candidates;
    try {
      candidates = poolResolver.resolve(retrievedEmails);
      logSuccess("candidate-resolution", candidates.isEmpty() ? "empty" : "completed");
    } catch (RuntimeException exception) {
      logFailure("candidate-resolution", "storage-unavailable");
      throw new CandidateMatchingUnavailableException(exception);
    }
    if (candidates.isEmpty()) {
      logSuccess("chunk-ranking", "skipped-empty-pool");
      logSuccess("response", "empty");
      return new CandidateMatchResponse(position.id(), List.of());
    }

    List<List<CandidateChunkRanking>> chunkRankings;
    try {
      chunkRankings = ranker.rankChunks(position, candidates);
      logSuccess("chunk-ranking", "completed");
    } catch (RuntimeException exception) {
      logFailure("chunk-ranking", "ranking-failed");
      throw new CandidateMatchingUnavailableException(exception);
    }

    try {
      List<CandidateMatchItemResponse> items =
          aggregator.aggregate(candidates, chunkRankings).stream()
              .map(
                  ranked ->
                      CandidateMatchItemResponse.from(
                          ranked.rank(),
                          ranked.matchPercentage(),
                          ranked.candidate(),
                          ranked.explanation()))
              .toList();
      logSuccess("result-aggregation", "completed");
      CandidateMatchResponse response = new CandidateMatchResponse(position.id(), items);
      logSuccess("response", "completed");
      return response;
    } catch (RuntimeException exception) {
      logFailure("result-aggregation", "invalid-ranking-result");
      throw new CandidateMatchingUnavailableException(exception);
    }
  }

  private JobOffer loadPosition(String positionId) {
    try {
      var position = jobOfferRepository.findById(positionId);
      if (position.isEmpty()) {
        logSuccess("position-load", "not-found");
        throw new JobOfferNotFoundException(positionId);
      }
      logSuccess("position-load", "completed");
      return position.get();
    } catch (DataAccessException exception) {
      logFailure("position-load", "storage-unavailable");
      throw new CandidateMatchingUnavailableException(exception);
    }
  }

  private void logSuccess(String stage, String outcome) {
    logger.info("Candidate matching stage={} outcome={}", stage, outcome);
  }

  private void logFailure(String stage, String category) {
    logger.warn("Candidate matching stage={} outcome={} category={}", stage, "failed", category);
  }

  private String semanticQuery(JobOffer position) {
    return List.of(
            value(position.jobTitle()),
            value(position.company()),
            value(position.jobDescription()),
            value(position.requiredQualifications()),
            position.yearsOfExperience() == null ? "" : position.yearsOfExperience().toString(),
            value(position.location()),
            value(position.allocation()))
        .stream()
        .filter(field -> !field.isBlank())
        .collect(Collectors.joining(" "));
  }

  private String value(String field) {
    return field == null ? "" : field;
  }
}
