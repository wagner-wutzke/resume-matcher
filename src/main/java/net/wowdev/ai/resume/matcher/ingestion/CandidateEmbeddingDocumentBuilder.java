package net.wowdev.ai.resume.matcher.ingestion;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.wowdev.ai.resume.matcher.model.Candidate;
import net.wowdev.ai.resume.matcher.model.CandidateExperience;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Component
public class CandidateEmbeddingDocumentBuilder {

  public List<Document> build(Candidate candidate, String sourceFileName) {
    Map<String, Object> metadata = metadata(candidate, sourceFileName);
    List<Document> documents = new ArrayList<>();
    documents.add(
        new Document(
            pointId(candidate, "profile"), profileText(candidate), new LinkedHashMap<>(metadata)));
    for (int index = 0; index < candidate.experiences().size(); index++) {
      CandidateExperience experience = candidate.experiences().get(index);
      documents.add(
          new Document(
              pointId(candidate, "experience:" + index),
              experienceText(experience),
              new LinkedHashMap<>(metadata)));
    }
    return List.copyOf(documents);
  }

  private String pointId(Candidate candidate, String role) {
    String source = candidate.id() + ":" + role;
    return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8)).toString();
  }

  private Map<String, Object> metadata(Candidate candidate, String sourceFileName) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("location", location(candidate.city(), candidate.country()));
    metadata.put("email", Candidate.normalizeEmail(candidate.email()));
    metadata.put("fileName", sourceFileName);
    metadata.put("position", candidate.position());
    metadata.put("yearsOfExperience", candidate.yearsOfExperience());
    return metadata;
  }

  private String location(String city, String country) {
    if (city.isBlank()) {
      return country;
    }
    return country.isBlank() ? city : city + ", " + country;
  }

  private String profileText(Candidate candidate) {
    return "email: "
        + candidate.email()
        + "\nfullname: "
        + candidate.fullname()
        + "\ncountry: "
        + candidate.country()
        + "\ncity: "
        + candidate.city()
        + "\nposition: "
        + candidate.position()
        + "\nskills: "
        + candidate.skills()
        + "\nallocation: "
        + candidate.allocation()
        + "\nyearsOfExperience: "
        + candidate.yearsOfExperience();
  }

  private String experienceText(CandidateExperience experience) {
    return "title: "
        + experience.title()
        + "\ncompany: "
        + experience.company()
        + "\nperiod: "
        + experience.period()
        + "\nresponsibilities: "
        + experience.responsibilities()
        + "\nachievements: "
        + experience.achievements();
  }
}
