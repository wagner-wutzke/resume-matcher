package net.wowdev.ai.resume.matcher.dto;

import net.wowdev.ai.resume.matcher.model.JobOfferCatalogEntry;

public record JobOfferCatalogResponse(
    String id,
    String company,
    String jobName,
    String jobDescription,
    String requiredQualifications,
    String location,
    String allocation) {

  public static JobOfferCatalogResponse from(JobOfferCatalogEntry entry) {
    return new JobOfferCatalogResponse(
        entry.id(),
        entry.company(),
        entry.jobName(),
        entry.jobDescription(),
        entry.requiredQualifications(),
        entry.location(),
        entry.allocation());
  }
}
