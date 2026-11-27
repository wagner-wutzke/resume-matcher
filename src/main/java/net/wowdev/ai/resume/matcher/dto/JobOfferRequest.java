package net.wowdev.ai.resume.matcher.dto;

public record JobOfferRequest(
    String company,
    String jobName,
    String jobDescription,
    String requiredQualifications,
    Integer yearsOfExperience,
    String location,
    String allocation) {}
