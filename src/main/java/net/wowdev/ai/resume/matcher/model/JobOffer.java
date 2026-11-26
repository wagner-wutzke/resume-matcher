package net.wowdev.ai.resume.matcher.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "job_offers")
public record JobOffer(
    @Id String id,
    String company,
    @Field("jobTitle")
    String jobName,
    String jobDescription,
    String requiredQualifications,
    String location,
    String allocation) {
}
