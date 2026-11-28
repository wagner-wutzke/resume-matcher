package net.wowdev.ai.resume.matcher.controller;

import net.wowdev.ai.resume.matcher.dto.CandidateMatchErrorResponse;
import net.wowdev.ai.resume.matcher.matching.CandidateMatchingUnavailableException;
import net.wowdev.ai.resume.matcher.service.JobOfferNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CandidateMatchController.class)
public class CandidateMatchExceptionHandler {

  @ExceptionHandler(JobOfferNotFoundException.class)
  public ResponseEntity<CandidateMatchErrorResponse> handlePositionNotFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            new CandidateMatchErrorResponse(
                CandidateMatchErrorResponse.POSITION_NOT_FOUND_CODE,
                "The job offer was not found."));
  }

  @ExceptionHandler(CandidateMatchingUnavailableException.class)
  public ResponseEntity<CandidateMatchErrorResponse> handleMatchingUnavailable() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(
            new CandidateMatchErrorResponse(
                CandidateMatchErrorResponse.MATCHING_UNAVAILABLE_CODE,
                "Candidate matching is temporarily unavailable."));
  }
}
