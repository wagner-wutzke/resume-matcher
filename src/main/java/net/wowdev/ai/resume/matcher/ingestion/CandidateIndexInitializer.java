package net.wowdev.ai.resume.matcher.ingestion;

import net.wowdev.ai.resume.matcher.model.Candidate;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

@Component
public class CandidateIndexInitializer {

  private final MongoTemplate mongoTemplate;

  public CandidateIndexInitializer(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  public void ensureEmailUniqueness() {
    mongoTemplate
        .indexOps(Candidate.class)
        .ensureIndex(
            new Index().on("email", Direction.ASC).unique().named("candidate_email_unique"));
  }
}
