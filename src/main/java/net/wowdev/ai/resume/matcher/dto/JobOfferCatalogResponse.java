package net.wowdev.ai.resume.matcher.dto;

import net.wowdev.ai.resume.matcher.model.JobOfferCatalogEntry;

public record JobOfferCatalogResponse(
    String id,
    String jobId,
    String company,
    String jobTitle,
    String jobDescription,
    String requiredQualifications,
    Integer yearsOfExperience,
    String location,
    String allocation) {

  public static JobOfferCatalogResponse from(JobOfferCatalogEntry entry) {
    return new JobOfferCatalogResponse(
        entry.id(),
        entry.jobId(),
        entry.company(),
        entry.jobTitle(),
        entry.jobDescription(),
        entry.requiredQualifications(),
        entry.yearsOfExperience(),
        entry.location(),
        entry.allocation());
  }
}
