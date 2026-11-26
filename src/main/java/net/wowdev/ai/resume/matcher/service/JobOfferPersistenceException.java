package net.wowdev.ai.resume.matcher.service;

public class JobOfferPersistenceException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public JobOfferPersistenceException(Throwable cause) {
    super("Job-offer storage is unavailable.", cause);
  }
}
