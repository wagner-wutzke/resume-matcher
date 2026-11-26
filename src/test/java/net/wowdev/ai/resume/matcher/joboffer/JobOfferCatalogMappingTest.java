package net.wowdev.ai.resume.matcher.joboffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.wowdev.ai.resume.matcher.model.JobOfferCatalogEntry;
import net.wowdev.ai.resume.matcher.repository.JobOfferCatalogReadRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

class JobOfferCatalogMappingTest {

  @Test
  void mapsCatalogFieldsAndMongoIdentifier() throws Exception {
    var document = JobOfferCatalogEntry.class.getAnnotation(Document.class);
    var components = JobOfferCatalogEntry.class.getRecordComponents();

    assertNotNull(document);
    assertEquals("job_offers", document.collection());
    assertEquals("id", components[0].getName());
    assertNotNull(JobOfferCatalogEntry.class.getDeclaredField("id").getAnnotation(Id.class));
    assertEquals("jobId", components[1].getName());
    assertEquals("company", components[2].getName());
    assertEquals("jobTitle", components[3].getName());
    assertEquals("jobDescription", components[4].getName());
    assertEquals("requiredQualifications", components[5].getName());
    assertEquals("Integer", components[6].getType().getSimpleName());
    assertEquals("yearsOfExperience", components[6].getName());
    assertEquals("location", components[7].getName());
    assertEquals("allocation", components[8].getName());
    assertEquals(9, components.length);
    assertNotNull(JobOfferCatalogReadRepository.class.getDeclaredMethod("findAll"));
    assertEquals(
        java.util.Set.of("findAll"),
        java.util.Arrays.stream(JobOfferCatalogReadRepository.class.getDeclaredMethods())
            .map(java.lang.reflect.Method::getName)
            .collect(java.util.stream.Collectors.toSet()));
  }
}
