package net.wowdev.ai.resume.matcher.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import net.wowdev.ai.resume.matcher.dto.CandidateSummaryResponse;
import net.wowdev.ai.resume.matcher.model.Candidate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

  @Test
  void shouldReadCandidateByPersistedId() {
    MongoTemplate template = org.mockito.Mockito.mock(MongoTemplate.class);
    MongoCandidateRepository repository = new MongoCandidateRepository(template);
    Candidate expected = candidate("mongo-id", "person@example.com", "Jane Doe");
    when(template.findById("mongo-id", Candidate.class)).thenReturn(expected);

    Optional<Candidate> result = repository.findById("mongo-id");

    assertEquals(Optional.of(expected), result);
  }

  @Test
  void shouldReadBoundedSummaryProjectionWithSkipAndOrdering() {
    MongoTemplate template = org.mockito.Mockito.mock(MongoTemplate.class);
    MongoCandidateRepository repository = new MongoCandidateRepository(template);
    CandidateSummaryResponse summary =
        new CandidateSummaryResponse(
            "id", "Jane Doe", "Engineer", "Brazil", "São Paulo", "5 years");
    when(template.find(any(Query.class), eq(CandidateSummaryResponse.class), eq("candidates")))
        .thenReturn(List.of(summary));
    ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);

    List<CandidateSummaryResponse> result = repository.findSummaryPage(2);

    assertEquals(List.of(summary), result);
    verify(template)
        .find(queryCaptor.capture(), eq(CandidateSummaryResponse.class), eq("candidates"));
    Query query = queryCaptor.getValue();
    assertEquals(30, query.getSkip());
    assertEquals(15, query.getLimit());
    assertEquals(List.of("fullname", "_id"), List.copyOf(query.getSortObject().keySet()));
    assertEquals(
        java.util.Set.of("_id", "fullname", "position", "country", "city", "yearsOfExperience"),
        query.getFieldsObject().keySet());
    assertEquals(true, query.getCollation().isPresent());
  }

  @Test
  void shouldCountCandidates() {
    MongoTemplate template = org.mockito.Mockito.mock(MongoTemplate.class);
    MongoCandidateRepository repository = new MongoCandidateRepository(template);
    when(template.count(any(Query.class), eq(Candidate.class))).thenReturn(19L);

    assertEquals(19L, repository.count());
  }

  private Candidate candidate(String id, String email, String fullname) {
    return new Candidate(
        id, email, fullname, "", "", "", "", "", "0 years and 0 months", java.util.List.of());
  }
}
