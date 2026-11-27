package net.wowdev.ai.resume.matcher.ingestion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.wowdev.ai.resume.matcher.model.Candidate;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;

class CandidateIndexInitializerTest {

  @Test
  void shouldEnsureUniqueCandidateEmailIndex() {
    MongoTemplate mongoTemplate = org.mockito.Mockito.mock(MongoTemplate.class);
    IndexOperations indexOperations = org.mockito.Mockito.mock(IndexOperations.class);
    when(mongoTemplate.indexOps(Candidate.class)).thenReturn(indexOperations);
    when(indexOperations.ensureIndex(any())).thenReturn("candidate_email_unique");

    new CandidateIndexInitializer(mongoTemplate).ensureEmailUniqueness();

    verify(indexOperations).ensureIndex(any());
  }
}
