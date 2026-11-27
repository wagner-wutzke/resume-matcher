package net.wowdev.ai.resume.matcher.service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.wowdev.ai.resume.matcher.model.JobOffer;
import net.wowdev.ai.resume.matcher.model.JobOfferSequence;
import net.wowdev.ai.resume.matcher.repository.JobOfferRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
public class JobOfferIdGenerator {

  private static final String COUNTER_ID = "job-offer-id";
  private static final String COUNTER_COLLECTION = "job_offer_sequences";
  private static final Pattern GENERATED_ID = Pattern.compile("^JD-(\\d{8})$");
  private static final long MAX_SEQUENCE = 99_999_999;

  private final MongoTemplate mongoTemplate;
  private final JobOfferRepository jobOfferRepository;
  private volatile Long initialValue;
  private volatile boolean counterInitialized;

  public JobOfferIdGenerator(MongoTemplate mongoTemplate, JobOfferRepository jobOfferRepository) {
    this.mongoTemplate = mongoTemplate;
    this.jobOfferRepository = jobOfferRepository;
  }

  public String nextJobId() {
    try {
      ensureCounterInitialized();
      var query = Query.query(Criteria.where("_id").is(COUNTER_ID));
      var update = new Update().inc("value", 1);
      var options = FindAndModifyOptions.options().returnNew(true);
      JobOfferSequence sequence = mongoTemplate.findAndModify(
          query, update, options, JobOfferSequence.class, COUNTER_COLLECTION);
      if (sequence == null) throw new IllegalStateException("Job ID sequence update returned no value.");
      if (sequence.value() > MAX_SEQUENCE) {
        throw new IllegalStateException("The Job ID sequence is exhausted.");
      }
      return String.format(Locale.ROOT, "JD-%08d", sequence.value());
    } catch (DuplicateKeyException exception) {
      return incrementExistingCounter();
    } catch (DataAccessException exception) {
      throw new JobOfferPersistenceException(exception);
    }
  }

  private void ensureCounterInitialized() {
    if (counterInitialized) return;
    synchronized (this) {
      if (counterInitialized) return;
      var query = Query.query(Criteria.where("_id").is(COUNTER_ID));
      var update = new Update().setOnInsert("value", initialValue());
      try {
        mongoTemplate.upsert(query, update, JobOfferSequence.class, COUNTER_COLLECTION);
      } catch (DuplicateKeyException ignored) {
        // Another application instance initialized the same counter concurrently.
      }
      counterInitialized = true;
    }
  }

  private String incrementExistingCounter() {
    try {
      var query = Query.query(Criteria.where("_id").is(COUNTER_ID));
      var update = new Update().inc("value", 1);
      var options = FindAndModifyOptions.options().returnNew(true);
      JobOfferSequence sequence = mongoTemplate.findAndModify(
          query, update, options, JobOfferSequence.class, COUNTER_COLLECTION);
      if (sequence == null || sequence.value() > MAX_SEQUENCE) {
        throw new IllegalStateException("The Job ID sequence is unavailable.");
      }
      return String.format(Locale.ROOT, "JD-%08d", sequence.value());
    } catch (DataAccessException exception) {
      throw new JobOfferPersistenceException(exception);
    }
  }

  private long initialValue() {
    Long value = initialValue;
    if (value != null) return value;
    synchronized (this) {
      if (initialValue == null) initialValue = highestExistingGeneratedId();
      return initialValue;
    }
  }

  private long highestExistingGeneratedId() {
    try {
      return jobOfferRepository.findAll().stream()
          .map(JobOffer::jobId)
          .map(GENERATED_ID::matcher)
          .filter(Matcher::matches)
          .mapToLong(matcher -> Long.parseLong(matcher.group(1)))
          .max()
          .orElse(0);
    } catch (DataAccessException exception) {
      throw new JobOfferPersistenceException(exception);
    }
  }
}
