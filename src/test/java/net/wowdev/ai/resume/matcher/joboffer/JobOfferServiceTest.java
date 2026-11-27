package net.wowdev.ai.resume.matcher.joboffer;

import net.wowdev.ai.resume.matcher.dto.JobOfferRequest;
import net.wowdev.ai.resume.matcher.model.JobOffer;
import net.wowdev.ai.resume.matcher.repository.JobOfferRepository;
import net.wowdev.ai.resume.matcher.service.JobOfferNotFoundException;
import net.wowdev.ai.resume.matcher.service.JobOfferIdGenerator;
import net.wowdev.ai.resume.matcher.service.JobOfferPersistenceException;
import net.wowdev.ai.resume.matcher.service.JobOfferService;
import net.wowdev.ai.resume.matcher.service.JobOfferValidationException;
import net.wowdev.ai.resume.matcher.service.JobOfferValidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataAccessResourceFailureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JobOfferServiceTest {

  private final JobOfferRepository repository = mock(JobOfferRepository.class);
  private final JobOfferIdGenerator idGenerator = mock(JobOfferIdGenerator.class);
  private final JobOfferService service = new JobOfferService(repository, new JobOfferValidator(), idGenerator);

  @BeforeEach
  void resetRepository() {
    org.mockito.Mockito.reset(repository, idGenerator);
  }

  @Test
  void createsOfferWithGeneratedIdentifierFromRepository() {
    var request = validRequest("Platform Engineer");
    when(idGenerator.nextJobId()).thenReturn("JD-00000001");
    var saved = new JobOffer("offer-1", "JD-00000001", "Example Company", "Platform Engineer", "Build systems", "Java", 7, "Dublin", "Hybrid");
    when(repository.save(new JobOffer(null, "JD-00000001", "Example Company", "Platform Engineer", "Build systems", "Java", 7, "Dublin", "Hybrid")))
        .thenReturn(saved);

    var result = service.create(request);

    assertEquals(saved, result);
    verify(repository).save(new JobOffer(
        null, "JD-00000001", "Example Company", "Platform Engineer", "Build systems", "Java", 7, "Dublin", "Hybrid"));
    verify(idGenerator).nextJobId();
    verifyNoMoreInteractions(repository);
  }

  @Test
  void updatesExistingOfferWithoutChangingItsIdentifier() {
    var existing = new JobOffer("offer-1", "JD-00000011", "Example Company", "Engineer", "Old description", "Java", 5, "Dublin", "Remote");
    var updated = new JobOffer("offer-1", "JD-00000011", "Example Company", "Platform Engineer", "Build systems", "Java", 7, "Dublin", "Hybrid");
    when(repository.findById("offer-1")).thenReturn(Optional.of(existing));
    when(repository.save(updated)).thenReturn(updated);

    var result = service.update("offer-1", validRequest("Platform Engineer"));

    assertEquals(updated, result);
    verify(repository).findById("offer-1");
    verify(repository).save(updated);
    org.mockito.Mockito.verifyNoInteractions(idGenerator);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void rejectsUpdateForUnknownIdentifierWithoutUpsert() {
    when(repository.findById("missing")).thenReturn(Optional.empty());

    assertThrows(JobOfferNotFoundException.class,
        () -> service.update("missing", validRequest("Platform Engineer")));

    verify(repository).findById("missing");
    verifyNoMoreInteractions(repository);
  }

  @Test
  void rejectsInvalidRequestBeforePersistence() {
    var invalid = new JobOfferRequest("Example Company", " ", "Build systems", "Java", 7, "Dublin", "Hybrid");

    var exception = assertThrows(JobOfferValidationException.class,
        () -> service.create(invalid));

    assertEquals(Map.of("jobName", "required"), exception.getFieldErrors());
    verifyNoMoreInteractions(repository);
  }

  @Test
  void deletesExistingOffer() {
    when(repository.findById("offer-1")).thenReturn(Optional.of(
        new JobOffer("offer-1", "JD-00000001", "Example Company", "Engineer", "Build services", "Java", 5, "Dublin", "Remote")));

    service.delete("offer-1");

    verify(repository).findById("offer-1");
    verify(repository).deleteById("offer-1");
    verifyNoMoreInteractions(repository);
  }

  @Test
  void rejectsDeleteForUnknownIdentifier() {
    when(repository.findById("missing")).thenReturn(Optional.empty());

    assertThrows(JobOfferNotFoundException.class, () -> service.delete("missing"));

    verify(repository).findById("missing");
    verifyNoMoreInteractions(repository);
  }

  @Test
  void translatesCreatePersistenceFailure() {
    when(repository.save(org.mockito.ArgumentMatchers.any(JobOffer.class)))
        .thenThrow(new DataAccessResourceFailureException("database unavailable"));

    assertThrows(JobOfferPersistenceException.class, () -> service.create(validRequest("Engineer")));
  }

  @Test
  void translatesUpdatePersistenceFailure() {
    when(repository.findById("offer-1")).thenReturn(Optional.of(
        new JobOffer("offer-1", "JD-00000001", "Example Company", "Engineer", "Build services", "Java", 5, "Dublin", "Remote")));
    when(repository.save(org.mockito.ArgumentMatchers.any(JobOffer.class)))
        .thenThrow(new DataAccessResourceFailureException("database unavailable"));

    assertThrows(JobOfferPersistenceException.class,
        () -> service.update("offer-1", validRequest("Engineer")));
  }

  @Test
  void translatesDeletePersistenceFailure() {
    when(repository.findById("offer-1")).thenReturn(Optional.of(
        new JobOffer("offer-1", "JD-00000001", "Example Company", "Engineer", "Build services", "Java", 5, "Dublin", "Remote")));
    org.mockito.Mockito.doThrow(new DataAccessResourceFailureException("database unavailable"))
        .when(repository).deleteById("offer-1");

    assertThrows(JobOfferPersistenceException.class, () -> service.delete("offer-1"));
  }

  private JobOfferRequest validRequest(String jobName) {
    return new JobOfferRequest("Example Company", jobName, "Build systems", "Java", 7, "Dublin", "Hybrid");
  }
}
