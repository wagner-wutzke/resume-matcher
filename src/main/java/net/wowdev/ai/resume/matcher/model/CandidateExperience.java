package net.wowdev.ai.resume.matcher.model;

public record CandidateExperience(
    String title, String company, String period, String responsibilities, String achievements) {

  public CandidateExperience {
    title = valueOrEmpty(title);
    company = valueOrEmpty(company);
    period = valueOrEmpty(period);
    responsibilities = valueOrEmpty(responsibilities);
    achievements = valueOrEmpty(achievements);
  }

  private static String valueOrEmpty(String value) {
    return value == null ? "" : value;
  }
}
