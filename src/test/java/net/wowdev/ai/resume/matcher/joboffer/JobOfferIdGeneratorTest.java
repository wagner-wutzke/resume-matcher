package net.wowdev.ai.resume.matcher.joboffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import net.wowdev.ai.resume.matcher.model.JobOffer;
import net.wowdev.ai.resume.matcher.model.JobOfferSequence;
import net.wowdev.ai.resume.matcher.repository.JobOfferRepository;
import net.wowdev.ai.resume.matcher.service.JobOfferIdGenerator;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

class JobOfferIdGeneratorTest {

  private final MongoTemplate mongoTemplate = mock(MongoTemplate.class);
  private final JobOfferRepository jobOfferRepository = mock(JobOfferRepository.class);
  private final JobOfferIdGenerator generator =
      new JobOfferIdGenerator(mongoTemplate, jobOfferRepository);

  @Test
  void startsSequenceAtOneWhenNoGeneratedIdsExist() {
    when(jobOfferRepository.findAll()).thenReturn(List.of());
    when(mongoTemplate.findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(JobOfferSequence.class),
            eq("job_offer_sequences")))
        .thenReturn(new JobOfferSequence("job-offer-id", 1));

    assertEquals("JD-00000001", generator.nextJobId());

    var update = ArgumentCaptor.forClass(Update.class);
    verify(mongoTemplate)
        .upsert(
            any(Query.class),
            update.capture(),
            eq(JobOfferSequence.class),
            eq("job_offer_sequences"));
    Document insertedValues =
        update.getValue().getUpdateObject().get("$setOnInsert", Document.class);
    assertEquals(0L, insertedValues.get("value"));
  }

  @Test
  void continuesAfterHighestExistingGeneratedId() {
    when(jobOfferRepository.findAll())
        .thenReturn(List.of(offer("JD-00000007"), offer("JD-00000012"), offer("BE-00100010")));
    when(mongoTemplate.findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(JobOfferSequence.class),
            eq("job_offer_sequences")))
        .thenReturn(new JobOfferSequence("job-offer-id", 13));

    assertEquals("JD-00000013", generator.nextJobId());
  }

  private JobOffer offer(String jobId) {
    return new JobOffer(
        null,
        jobId,
        "Example Company",
        "Engineer",
        "Description",
        "Qualifications",
        5,
        "Dublin",
        "Remote");
  }
}
