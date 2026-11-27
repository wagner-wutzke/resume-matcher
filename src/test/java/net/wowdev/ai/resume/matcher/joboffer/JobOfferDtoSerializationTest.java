package net.wowdev.ai.resume.matcher.joboffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import net.wowdev.ai.resume.matcher.dto.JobOfferCatalogResponse;
import net.wowdev.ai.resume.matcher.dto.JobOfferErrorResponse;
import net.wowdev.ai.resume.matcher.dto.JobOfferRequest;
import net.wowdev.ai.resume.matcher.dto.JobOfferResponse;
import org.junit.jupiter.api.Test;

class JobOfferDtoSerializationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void serializesCreateAndUpdateRequestFields() throws Exception {
    var request =
        new JobOfferRequest(
            "Example Company", "Engineer", "Build services", "Java", 7, "Dublin", "Remote");

    var json = objectMapper.readTree(objectMapper.writeValueAsString(request));

    assertEquals("Example Company", json.get("company").asText());
    assertEquals("Engineer", json.get("jobName").asText());
    assertEquals("Build services", json.get("jobDescription").asText());
    assertEquals("Java", json.get("requiredQualifications").asText());
    assertEquals(7, json.get("yearsOfExperience").asInt());
    assertEquals("Dublin", json.get("location").asText());
    assertEquals("Remote", json.get("allocation").asText());
    assertFalse(json.has("jobId"));
    assertEquals(7, json.size());
  }

  @Test
  void serializesJobOfferResponseFields() throws Exception {
    var response =
        new JobOfferResponse(
            "offer-1",
            "JD-00000001",
            "Example Company",
            "Engineer",
            "Build services",
            "Java",
            7,
            "Dublin",
            "Remote");

    var json = objectMapper.readTree(objectMapper.writeValueAsString(response));

    assertEquals("offer-1", json.get("id").asText());
    assertEquals("JD-00000001", json.get("jobId").asText());
    assertEquals("Example Company", json.get("company").asText());
    assertEquals("Engineer", json.get("jobTitle").asText());
    assertEquals("Build services", json.get("jobDescription").asText());
    assertEquals("Java", json.get("requiredQualifications").asText());
    assertEquals(7, json.get("yearsOfExperience").asInt());
    assertEquals("Dublin", json.get("location").asText());
    assertEquals("Remote", json.get("allocation").asText());
    assertEquals(9, json.size());
  }

  @Test
  void serializesCatalogFieldsAndNumericExperience() throws Exception {
    var response =
        new JobOfferCatalogResponse(
            "offer-1",
            "BE-00100010",
            "Example Company",
            "Backend Engineer",
            "Build services",
            "Java and Spring",
            7,
            "Dublin",
            "Remote");

    var json = objectMapper.readTree(objectMapper.writeValueAsString(response));

    assertEquals("offer-1", json.get("id").asText());
    assertEquals("BE-00100010", json.get("jobId").asText());
    assertEquals("Example Company", json.get("company").asText());
    assertEquals("Backend Engineer", json.get("jobTitle").asText());
    assertEquals("Build services", json.get("jobDescription").asText());
    assertEquals("Java and Spring", json.get("requiredQualifications").asText());
    assertEquals(7, json.get("yearsOfExperience").asInt());
    assertEquals("number", json.get("yearsOfExperience").getNodeType().name().toLowerCase());
    assertEquals("Dublin", json.get("location").asText());
    assertEquals("Remote", json.get("allocation").asText());
    assertEquals(9, json.size());
  }

  @Test
  void serializesStableValidationErrorFieldsWithoutNulls() throws Exception {
    var error =
        new JobOfferErrorResponse(
            "JOB_OFFER_VALIDATION_FAILED",
            "The job offer is invalid.",
            Map.of("jobName", "required", "allocation", "unsupported value"));

    var json = objectMapper.readTree(objectMapper.writeValueAsString(error));

    assertEquals("JOB_OFFER_VALIDATION_FAILED", json.get("code").asText());
    assertEquals("The job offer is invalid.", json.get("message").asText());
    assertEquals("required", json.get("fieldErrors").get("jobName").asText());
    assertEquals("unsupported value", json.get("fieldErrors").get("allocation").asText());
    assertFalse(json.has("stackTrace"));
  }

  @Test
  void omitsFieldErrorsWhenNotApplicable() throws Exception {
    var error =
        new JobOfferErrorResponse("JOB_OFFER_NOT_FOUND", "The job offer was not found.", null);

    var json = objectMapper.readTree(objectMapper.writeValueAsString(error));

    assertEquals("JOB_OFFER_NOT_FOUND", json.get("code").asText());
    assertFalse(json.has("fieldErrors"));
  }
}
