package net.wowdev.ai.resume.matcher.controller;

import net.wowdev.ai.resume.matcher.dto.CandidateApiErrorResponse;
import net.wowdev.ai.resume.matcher.service.CandidateDetailsReadException;
import net.wowdev.ai.resume.matcher.service.CandidateListReadException;
import net.wowdev.ai.resume.matcher.service.CandidateNotFoundException;
import net.wowdev.ai.resume.matcher.service.CandidatePageInvalidException;
import net.wowdev.ai.resume.matcher.service.CandidatePageOutOfRangeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = CandidateController.class)
public class CandidateControllerExceptionHandler {

  private static final Logger logger =
      LoggerFactory.getLogger(CandidateControllerExceptionHandler.class);

  @ExceptionHandler({
    CandidatePageInvalidException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<CandidateApiErrorResponse> handleInvalidPage() {
    return ResponseEntity.badRequest()
        .body(
            new CandidateApiErrorResponse("CANDIDATE_PAGE_INVALID", "The page value is invalid."));
  }

  @ExceptionHandler(CandidatePageOutOfRangeException.class)
  public ResponseEntity<CandidateApiErrorResponse> handlePageOutOfRange() {
    return ResponseEntity.badRequest()
        .body(
            new CandidateApiErrorResponse(
                "CANDIDATE_PAGE_OUT_OF_RANGE", "The requested page is unavailable."));
  }

  @ExceptionHandler(CandidateNotFoundException.class)
  public ResponseEntity<CandidateApiErrorResponse> handleNotFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new CandidateApiErrorResponse("CANDIDATE_NOT_FOUND", "The candidate was not found."));
  }

  @ExceptionHandler(CandidateListReadException.class)
  public ResponseEntity<CandidateApiErrorResponse> handleListReadFailure() {
    logger.warn("Candidate retrieval failed: {}", "list-storage-unavailable");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(
            new CandidateApiErrorResponse(
                "CANDIDATES_UNAVAILABLE", "Candidate data could not be loaded."));
  }

  @ExceptionHandler(CandidateDetailsReadException.class)
  public ResponseEntity<CandidateApiErrorResponse> handleDetailsReadFailure() {
    logger.warn("Candidate retrieval failed: {}", "details-storage-unavailable");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(
            new CandidateApiErrorResponse(
                "CANDIDATE_DETAILS_UNAVAILABLE", "Candidate details could not be loaded."));
  }
}
