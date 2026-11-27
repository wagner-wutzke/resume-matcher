package net.wowdev.ai.resume.matcher.ingestion;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import net.wowdev.ai.resume.matcher.model.Candidate;
import net.wowdev.ai.resume.matcher.model.CandidateExperience;
import org.springframework.stereotype.Component;

@Component
public class CandidateDataValidator {

  private static final String EMAIL_FIELD = "email";
  private static final String FULLNAME_FIELD = "fullname";
  private static final String COUNTRY_FIELD = "country";
  private static final String CITY_FIELD = "city";
  private static final String POSITION_FIELD = "position";
  private static final String SKILLS_FIELD = "skills";
  private static final String ALLOCATION_FIELD = "allocation";
  private static final String YEARS_OF_EXPERIENCE_FIELD = "yearsOfExperience";
  private static final String EXPERIENCES_FIELD = "experiences";
  private static final String TITLE_FIELD = "title";
  private static final String COMPANY_FIELD = "company";
  private static final String PERIOD_FIELD = "period";
  private static final String RESPONSIBILITIES_FIELD = "responsibilities";
  private static final String ACHIEVEMENTS_FIELD = "achievements";

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile(
          "^[A-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Z0-9!#$%&'*+/=?^_`{|}~-]+)*"
              + "@[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?"
              + "(?:\\.[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?)+$",
          Pattern.CASE_INSENSITIVE);
  private static final Set<String> CANDIDATE_FIELDS =
      Set.of(
          EMAIL_FIELD,
          FULLNAME_FIELD,
          COUNTRY_FIELD,
          CITY_FIELD,
          POSITION_FIELD,
          SKILLS_FIELD,
          ALLOCATION_FIELD,
          YEARS_OF_EXPERIENCE_FIELD,
          EXPERIENCES_FIELD);
  private static final Set<String> EXPERIENCE_FIELDS =
      Set.of(
          TITLE_FIELD,
          COMPANY_FIELD,
          PERIOD_FIELD,
          RESPONSIBILITIES_FIELD,
          ACHIEVEMENTS_FIELD);

  private final ObjectMapper objectMapper;

  public CandidateDataValidator(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Candidate validate(String json) {
    try {
      JsonNode root =
          objectMapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(json);
      if (root == null || !root.isObject()) {
        throw invalidResponse();
      }
      ensureAllowedFields(root, CANDIDATE_FIELDS);
      String email = scalar(root, EMAIL_FIELD).trim();
      if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
        throw invalidResponse();
      }
      return new Candidate(
          null,
          email,
          scalar(root, FULLNAME_FIELD),
          scalar(root, COUNTRY_FIELD),
          scalar(root, CITY_FIELD),
          scalar(root, POSITION_FIELD),
          scalar(root, SKILLS_FIELD),
          scalar(root, ALLOCATION_FIELD),
          scalar(root, YEARS_OF_EXPERIENCE_FIELD),
          experiences(root));
    } catch (ResumeIngestionException exception) {
      throw exception;
    } catch (Exception exception) {
      throw invalidResponse();
    }
  }

  private List<CandidateExperience> experiences(JsonNode root) {
    JsonNode experiences = root.get(EXPERIENCES_FIELD);
    if (experiences == null) {
      return List.of();
    }
    if (!experiences.isArray()) {
      throw invalidResponse();
    }
    List<CandidateExperience> values = new ArrayList<>();
    for (JsonNode experience : experiences) {
      if (!experience.isObject()) {
        throw invalidResponse();
      }
      ensureAllowedFields(experience, EXPERIENCE_FIELDS);
      values.add(
          new CandidateExperience(
              scalar(experience, TITLE_FIELD),
              scalar(experience, COMPANY_FIELD),
              scalar(experience, PERIOD_FIELD),
              scalar(experience, RESPONSIBILITIES_FIELD),
              scalar(experience, ACHIEVEMENTS_FIELD)));
    }
    return List.copyOf(values);
  }

  private String scalar(JsonNode object, String name) {
    JsonNode value = object.get(name);
    if (value == null) {
      return "";
    }
    if (!value.isTextual()) {
      throw invalidResponse();
    }
    return value.textValue();
  }

  private void ensureAllowedFields(JsonNode object, Set<String> allowedFields) {
    Iterator<String> names = object.fieldNames();
    while (names.hasNext()) {
      if (!allowedFields.contains(names.next())) {
        throw invalidResponse();
      }
    }
  }

  private ResumeIngestionException invalidResponse() {
    return new ResumeIngestionException(
        "Candidate extraction returned invalid structured data",
        ResumeIngestionException.FailureCause.INVALID_CANDIDATE_DATA);
  }
}
