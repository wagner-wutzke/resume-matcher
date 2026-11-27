package net.wowdev.ai.resume.matcher.dto;

import net.wowdev.ai.resume.matcher.model.JobOffer;

public record JobOfferResponse(
    String id,
    String jobId,
    String company,
    String jobTitle,
    String jobDescription,
    String requiredQualifications,
    Integer yearsOfExperience,
    String location,
    String allocation) {

  public static JobOfferResponse from(JobOffer offer) {
    return new JobOfferResponse(
        offer.id(),
        offer.jobId(),
        offer.company(),
        offer.jobTitle(),
        offer.jobDescription(),
        offer.requiredQualifications(),
        offer.yearsOfExperience(),
        offer.location(),
        offer.allocation());
  }
}
