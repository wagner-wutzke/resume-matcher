package net.wowdev.ai.resume.matcher.joboffer;

import net.wowdev.ai.resume.matcher.dto.JobOfferRequest;
import net.wowdev.ai.resume.matcher.service.JobOfferValidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

class JobOfferValidatorTest {

  private final JobOfferValidator validator = new JobOfferValidator();

  @Test
  void acceptsACompleteRequest() {
    var request = requestWith("Remote");

    assertTrue(validator.validate(request).isEmpty());
  }

  @ParameterizedTest
  @MethodSource("requiredFieldCases")
  void reportsMissingAndBlankRequiredFields(String field, String value) {
    var errors = validator.validate(requestWithField(field, value));

    assertEquals("required", errors.get(field));
    assertEquals(1, errors.size());
  }

  @ParameterizedTest
  @MethodSource("supportedAllocations")
  void acceptsSupportedAllocationValues(String allocation) {
    assertTrue(validator.validate(requestWith(allocation)).isEmpty());
  }

  @ParameterizedTest
  @MethodSource("unsupportedAllocations")
  void rejectsUnsupportedAllocationValues(String allocation) {
    var errors = validator.validate(requestWith(allocation));

    assertEquals(Map.of("allocation", "unsupported value"), errors);
  }

  @Test
  void reportsAllFieldsRequiredForNullRequest() {
    assertEquals(Map.of(
        "company", "required",
        "jobName", "required",
        "jobDescription", "required",
        "requiredQualifications", "required",
        "location", "required",
        "allocation", "required"), validator.validate(null));
  }

  private static JobOfferRequest requestWith(String allocation) {
    return new JobOfferRequest("Example Company", "Engineer", "Build services", "Java", "Dublin", allocation);
  }

  private static JobOfferRequest requestWithField(String field, String value) {
    return new JobOfferRequest(
        field.equals("company") ? value : "Example Company",
        field.equals("jobName") ? value : "Engineer",
        field.equals("jobDescription") ? value : "Build services",
        field.equals("requiredQualifications") ? value : "Java",
        field.equals("location") ? value : "Dublin",
        field.equals("allocation") ? value : "Remote");
  }

  private static Stream<Arguments> requiredFieldCases() {
    return Stream.of(
        Arguments.of("company", null),
        Arguments.of("company", "  "),
        Arguments.of("jobName", null),
        Arguments.of("jobName", "  "),
        Arguments.of("jobDescription", null),
        Arguments.of("jobDescription", "  "),
        Arguments.of("requiredQualifications", null),
        Arguments.of("requiredQualifications", "  "),
        Arguments.of("location", null),
        Arguments.of("location", "  "),
        Arguments.of("allocation", null),
        Arguments.of("allocation", "  "));
  }

  private static Stream<String> supportedAllocations() {
    return Stream.of("Remote", "Hybrid", "On-site");
  }

  private static Stream<String> unsupportedAllocations() {
    return Stream.of("remote", "Office", "Remote ");
  }
}
