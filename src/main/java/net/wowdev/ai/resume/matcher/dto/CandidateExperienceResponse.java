package net.wowdev.ai.resume.matcher.dto;

import net.wowdev.ai.resume.matcher.model.CandidateExperience;

public record CandidateExperienceResponse(
    String title, String company, String period, String responsibilities, String achievements) {

  public static CandidateExperienceResponse from(CandidateExperience experience) {
    return new CandidateExperienceResponse(
        experience.title(),
        experience.company(),
        experience.period(),
        experience.responsibilities(),
        experience.achievements());
  }
}
