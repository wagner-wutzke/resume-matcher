package net.wowdev.ai.resume.matcher.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import net.wowdev.ai.resume.matcher.dto.CandidateSummaryResponse;
import net.wowdev.ai.resume.matcher.model.Candidate;
import net.wowdev.ai.resume.matcher.repository.CandidateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class CandidateQueryServiceTest {

  private final CandidateRepository repository = mock(CandidateRepository.class);
  private final CandidateQueryService service = new CandidateQueryService(repository);

  @Test
  void defaultsMissingPageToFirstPage() {
    when(repository.count()).thenReturn(1L);
    when(repository.findSummaryPage(0)).thenReturn(List.of(summary()));

    var response = service.findPage(null);

    assertEquals(0, response.page());
    assertEquals(15, response.pageSize());
    assertEquals(1, response.totalElements());
    assertEquals(1, response.totalPages());
    assertEquals(List.of(summary()), response.items());
  }

  @Test
  void handlesEmptyPageAndComputesPageCount() {
    when(repository.count()).thenReturn(0L);
    when(repository.findSummaryPage(0)).thenReturn(List.of());

    var empty = service.findPage(0);
    assertEquals(0, empty.totalPages());
    assertEquals(List.of(), empty.items());

    when(repository.count()).thenReturn(31L);
    when(repository.findSummaryPage(1)).thenReturn(List.of(summary()));
    var intermediate = service.findPage(1);
    assertEquals(3, intermediate.totalPages());
    verify(repository).findSummaryPage(1);
  }

  @Test
  void acceptsLastPageAndRejectsOutOfRangePage() {
    when(repository.count()).thenReturn(31L);
    when(repository.findSummaryPage(2)).thenReturn(List.of(summary()));

    assertEquals(2, service.findPage(2).page());
    assertThrows(CandidatePageOutOfRangeException.class, () -> service.findPage(3));
  }

  @Test
  void rejectsNegativePageBeforeRepositoryAccess() {
    assertThrows(CandidatePageInvalidException.class, () -> service.findPage(-1));
    verifyNoInteractions(repository);
  }

  @Test
  void returnsDetailsAndReportsMissingCandidate() {
    Candidate candidate = candidate();
    when(repository.findById("id")).thenReturn(Optional.of(candidate));

    assertEquals("jane@example.com", service.findDetails("id").email());

    when(repository.findById("missing")).thenReturn(Optional.empty());
    assertThrows(CandidateNotFoundException.class, () -> service.findDetails("missing"));
  }

  @Test
  void wrapsStorageFailuresWithoutLeakingThemAsResponseData() {
    var databaseFailure = new DataAccessResourceFailureException("private database details");
    when(repository.count()).thenThrow(databaseFailure);

    var exception = assertThrows(CandidateReadException.class, () -> service.findPage(0));

    assertEquals(databaseFailure, exception.getCause());
  }

  private CandidateSummaryResponse summary() {
    return new CandidateSummaryResponse("id", "Jane Doe", "Engineer", "Brazil", "SP", "5 years");
  }

  private Candidate candidate() {
    return new Candidate("id", "jane@example.com", "Jane Doe", "", "", "", "", "", "", List.of());
  }
}
