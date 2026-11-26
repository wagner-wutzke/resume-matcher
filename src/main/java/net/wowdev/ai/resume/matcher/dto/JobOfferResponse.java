package net.wowdev.ai.resume.matcher.dto;

import net.wowdev.ai.resume.matcher.model.JobOffer;

public record JobOfferResponse(
    String id,
    String jobName,
    String jobDescription,
    String requiredQualifications,
    String location,
    String allocation) {

  public static JobOfferResponse from(JobOffer offer) {
    return new JobOfferResponse(
        offer.id(),
        offer.jobName(),
        offer.jobDescription(),
        offer.requiredQualifications(),
        offer.location(),
        offer.allocation());
  }
}
