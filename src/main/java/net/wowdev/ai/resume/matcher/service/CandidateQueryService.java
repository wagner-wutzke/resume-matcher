package net.wowdev.ai.resume.matcher.service;

import net.wowdev.ai.resume.matcher.dto.CandidateDetailsResponse;
import net.wowdev.ai.resume.matcher.dto.CandidatePageResponse;
import net.wowdev.ai.resume.matcher.repository.CandidateRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class CandidateQueryService {

  public static final int PAGE_SIZE = 15;

  private final CandidateRepository repository;

  public CandidateQueryService(CandidateRepository repository) {
    this.repository = repository;
  }

  public CandidatePageResponse findPage(Integer requestedPage) {
    int page = requestedPage == null ? 0 : requestedPage;
    if (page < 0) {
      throw new CandidatePageInvalidException();
    }

    try {
      long totalElements = repository.count();
      int totalPages = (int) ((totalElements + PAGE_SIZE - 1) / PAGE_SIZE);
      if (page > 0 && (totalPages == 0 || page >= totalPages)) {
        throw new CandidatePageOutOfRangeException();
      }
      return new CandidatePageResponse(
          repository.findSummaryPage(page), page, PAGE_SIZE, totalElements, totalPages);
    } catch (DataAccessException exception) {
      throw new CandidateListReadException(exception);
    }
  }

  public CandidateDetailsResponse findDetails(String id) {
    try {
      return repository
          .findById(id)
          .map(CandidateDetailsResponse::from)
          .orElseThrow(CandidateNotFoundException::new);
    } catch (DataAccessException exception) {
      throw new CandidateDetailsReadException(exception);
    }
  }
}
