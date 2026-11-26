package net.wowdev.ai.resume.matcher.joboffer;

import net.wowdev.ai.resume.matcher.model.JobOffer;
import net.wowdev.ai.resume.matcher.repository.JobOfferRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

class JobOfferMappingTest {

  @Test
  void mapsMongoIdentifierAndCollection() throws Exception {
    var document = JobOffer.class.getAnnotation(Document.class);
    var id = JobOffer.class.getRecordComponents()[0];

    assertNotNull(document);
    assertEquals("job_offers", document.collection());
    assertEquals("id", id.getName());
    assertNotNull(JobOffer.class.getDeclaredField("id").getAnnotation(Id.class));
    assertEquals(6, JobOffer.class.getRecordComponents().length);
    assertEquals("jobName", JobOffer.class.getRecordComponents()[1].getName());
    assertEquals("jobDescription", JobOffer.class.getRecordComponents()[2].getName());
    assertEquals("requiredQualifications", JobOffer.class.getRecordComponents()[3].getName());
    assertEquals("location", JobOffer.class.getRecordComponents()[4].getName());
    assertEquals("allocation", JobOffer.class.getRecordComponents()[5].getName());
    assertNotNull(JobOfferRepository.class.getDeclaredMethod("findAll"));
    assertNotNull(JobOfferRepository.class.getDeclaredMethod("findById", String.class));
    assertNotNull(JobOfferRepository.class.getDeclaredMethod("save", JobOffer.class));
    assertNotNull(JobOfferRepository.class.getDeclaredMethod("deleteById", String.class));
  }
}
