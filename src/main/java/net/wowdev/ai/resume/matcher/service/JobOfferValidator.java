package net.wowdev.ai.resume.matcher.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.wowdev.ai.resume.matcher.dto.JobOfferRequest;
import org.springframework.stereotype.Component;

@Component
public final class JobOfferValidator {

  private static final Set<String> ALLOCATIONS = Set.of("Remote", "Hybrid", "On-site");

  public Map<String, String> validate(JobOfferRequest request) {
    var errors = new LinkedHashMap<String, String>();
    if (request == null) {
      addAllRequiredErrors(errors);
      return errors;
    }

    addRequiredError(errors, "jobName", request.jobName());
    addRequiredError(errors, "company", request.company());
    addRequiredError(errors, "jobDescription", request.jobDescription());
    addRequiredError(errors, "requiredQualifications", request.requiredQualifications());
    if (request.yearsOfExperience() == null) {
      errors.put("yearsOfExperience", "required");
    }
    addRequiredError(errors, "location", request.location());
    addRequiredError(errors, "allocation", request.allocation());

    if (!isBlank(request.allocation()) && !ALLOCATIONS.contains(request.allocation())) {
      errors.put("allocation", "unsupported value");
    }
    return errors;
  }

  private void addAllRequiredErrors(Map<String, String> errors) {
    errors.put("jobName", "required");
    errors.put("company", "required");
    errors.put("jobDescription", "required");
    errors.put("requiredQualifications", "required");
    errors.put("yearsOfExperience", "required");
    errors.put("location", "required");
    errors.put("allocation", "required");
  }

  private void addRequiredError(Map<String, String> errors, String field, String value) {
    if (isBlank(value)) {
      errors.put(field, "required");
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
