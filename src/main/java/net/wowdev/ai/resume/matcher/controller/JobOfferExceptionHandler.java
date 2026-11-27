package net.wowdev.ai.resume.matcher.controller;

import net.wowdev.ai.resume.matcher.dto.JobOfferErrorResponse;
import net.wowdev.ai.resume.matcher.dto.JobOfferUnavailableResponse;
import net.wowdev.ai.resume.matcher.service.JobOfferNotFoundException;
import net.wowdev.ai.resume.matcher.service.JobOfferPersistenceException;
import net.wowdev.ai.resume.matcher.service.JobOfferValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = JobOfferController.class)
public class JobOfferExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(JobOfferExceptionHandler.class);

  @ExceptionHandler(JobOfferValidationException.class)
  public ResponseEntity<JobOfferErrorResponse> handleValidationFailure(
      JobOfferValidationException exception) {
    var response =
        new JobOfferErrorResponse(
            "JOB_OFFER_VALIDATION_FAILED", "The job offer is invalid.", exception.getFieldErrors());
    return ResponseEntity.badRequest().body(response);
  }

  @ExceptionHandler(JobOfferNotFoundException.class)
  public ResponseEntity<JobOfferErrorResponse> handleNotFound() {
    var response =
        new JobOfferErrorResponse("JOB_OFFER_NOT_FOUND", "The job offer was not found.", null);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
  }

  @ExceptionHandler(JobOfferPersistenceException.class)
  public ResponseEntity<JobOfferErrorResponse> handlePersistenceFailure(
      JobOfferPersistenceException exception) {
    logger.warn("Job-offer persistence failed", exception);
    var response =
        new JobOfferErrorResponse(
            "JOB_OFFER_PERSISTENCE_UNAVAILABLE", "The job offer could not be saved.", null);
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
  }

  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<JobOfferUnavailableResponse> handleRetrievalFailure(
      DataAccessException exception) {
    logger.warn("Could not retrieve job offers", exception);
    var response =
        new JobOfferUnavailableResponse(
            "JOB_OFFERS_UNAVAILABLE", "Job offers could not be loaded.");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
  }
}
