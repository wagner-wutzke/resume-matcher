package net.wowdev.ai.resume.matcher.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "job_offers")
public record JobOffer(
    @Id String id,
    String jobId,
    String company,
    String jobTitle,
    String jobDescription,
    String requiredQualifications,
    Integer yearsOfExperience,
    String location,
    String allocation) {}
