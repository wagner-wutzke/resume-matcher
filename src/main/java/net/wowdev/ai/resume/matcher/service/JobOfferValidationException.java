package net.wowdev.ai.resume.matcher.service;

import java.util.Map;

public class JobOfferValidationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final Map<String, String> fieldErrors;

  public JobOfferValidationException(Map<String, String> fieldErrors) {
    super("The job offer is invalid.");
    this.fieldErrors = Map.copyOf(fieldErrors);
  }

  public Map<String, String> getFieldErrors() {
    return fieldErrors;
  }
}
