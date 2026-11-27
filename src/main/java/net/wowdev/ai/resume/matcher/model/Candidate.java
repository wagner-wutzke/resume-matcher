package net.wowdev.ai.resume.matcher.model;

import java.util.List;
import java.util.Locale;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "candidates")
public record Candidate(
    @Id String id,
    @Indexed(unique = true, name = "candidate_email_unique") String email,
    String fullname,
    String country,
    String city,
    String position,
    String skills,
    String allocation,
    String yearsOfExperience,
    List<CandidateExperience> experiences) {

  public Candidate {
    email = normalizeEmail(email);
    fullname = valueOrEmpty(fullname);
    country = valueOrEmpty(country);
    city = valueOrEmpty(city);
    position = valueOrEmpty(position);
    skills = valueOrEmpty(skills);
    allocation = valueOrEmpty(allocation);
    yearsOfExperience = valueOrEmpty(yearsOfExperience);
    experiences = experiences == null ? List.of() : List.copyOf(experiences);
  }

  public static String normalizeEmail(String email) {
    return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
  }

  private static String valueOrEmpty(String value) {
    return value == null ? "" : value;
  }
}
