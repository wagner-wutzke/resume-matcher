package net.wowdev.ai.resume.matcher.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.wowdev.ai.resume.matcher.model.Candidate;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

class MongoCandidateRepositoryTest {

  @Test
  void shouldInsertCandidateForNewNormalizedEmail() {
    MongoTemplate template = org.mockito.Mockito.mock(MongoTemplate.class);
    MongoCandidateRepository repository = new MongoCandidateRepository(template);
    Candidate candidate = candidate(null, " Person@Example.com ", "New Name");
    when(template.findOne(any(Query.class), eq(Candidate.class))).thenReturn(null);
    when(template.insert(candidate)).thenReturn(candidate);

    Candidate saved = repository.replaceByEmail(candidate);

    assertSame(candidate, saved);
    verify(template).findOne(any(Query.class), eq(Candidate.class));
    verify(template).insert(candidate);
  }

  @Test
  void shouldReplaceCandidateAndRetainExistingMongoId() {
    MongoTemplate template = org.mockito.Mockito.mock(MongoTemplate.class);
    MongoCandidateRepository repository = new MongoCandidateRepository(template);
    Candidate previous = candidate("mongo-id", "person@example.com", "Old Name");
    Candidate incoming = candidate(null, "PERSON@example.com", "New Name");
    when(template.findOne(any(Query.class), eq(Candidate.class))).thenReturn(previous);
    when(template.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Candidate saved = repository.replaceByEmail(incoming);

    assertEquals("mongo-id", saved.id());
    assertEquals("person@example.com", saved.email());
    assertEquals("New Name", saved.fullname());
    verify(template).save(saved);
  }

  @Test
  void shouldPropagateUniqueIndexRaceFailure() {
    MongoTemplate template = org.mockito.Mockito.mock(MongoTemplate.class);
    MongoCandidateRepository repository = new MongoCandidateRepository(template);
    Candidate candidate = candidate(null, "person@example.com", "New Name");
    when(template.findOne(any(Query.class), eq(Candidate.class))).thenReturn(null);
    DuplicateKeyException raceFailure = new DuplicateKeyException("duplicate");
    when(template.insert(candidate)).thenThrow(raceFailure);

    org.junit.jupiter.api.Assertions.assertSame(
        raceFailure,
        org.junit.jupiter.api.Assertions.assertThrows(
            DuplicateKeyException.class, () -> repository.replaceByEmail(candidate)));
  }

  private Candidate candidate(String id, String email, String fullname) {
    return new Candidate(
        id, email, fullname, "", "", "", "", "", "0 years and 0 months", java.util.List.of());
  }
}
