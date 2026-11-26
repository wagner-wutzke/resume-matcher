package net.wowdev.ai.resume.matcher.service;

public class JobOfferNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public JobOfferNotFoundException(String id) {
    super("Job offer not found: " + id);
  }
}
