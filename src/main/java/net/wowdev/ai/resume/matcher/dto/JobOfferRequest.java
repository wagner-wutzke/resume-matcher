package net.wowdev.ai.resume.matcher.dto;

public record JobOfferRequest(
    String company,
    String jobName,
    String jobDescription,
    String requiredQualifications,
    String location,
    String allocation) {
}
