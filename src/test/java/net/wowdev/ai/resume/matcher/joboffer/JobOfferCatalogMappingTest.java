package net.wowdev.ai.resume.matcher.joboffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.wowdev.ai.resume.matcher.model.JobOfferCatalogEntry;
import net.wowdev.ai.resume.matcher.repository.JobOfferCatalogReadRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

class JobOfferCatalogMappingTest {

  @Test
  void mapsCatalogFieldsAndMongoIdentifier() throws Exception {
    var document = JobOfferCatalogEntry.class.getAnnotation(Document.class);
    var components = JobOfferCatalogEntry.class.getRecordComponents();

    assertNotNull(document);
    assertEquals("job_offers", document.collection());
    assertEquals("id", components[0].getName());
    assertNotNull(JobOfferCatalogEntry.class.getDeclaredField("id").getAnnotation(Id.class));
    assertEquals("company", components[1].getName());
    assertEquals("jobName", components[2].getName());
    assertEquals("jobTitle", JobOfferCatalogEntry.class.getDeclaredField("jobName")
        .getAnnotation(Field.class).value());
    assertEquals("jobDescription", components[3].getName());
    assertEquals("requiredQualifications", components[4].getName());
    assertEquals("location", components[5].getName());
    assertEquals("allocation", components[6].getName());
    assertEquals(7, components.length);
    assertNotNull(JobOfferCatalogReadRepository.class.getDeclaredMethod("findAll"));
    assertEquals(
        java.util.Set.of("findAll"),
        java.util.Arrays.stream(JobOfferCatalogReadRepository.class.getDeclaredMethods())
            .map(java.lang.reflect.Method::getName)
            .collect(java.util.stream.Collectors.toSet()));
  }
}
