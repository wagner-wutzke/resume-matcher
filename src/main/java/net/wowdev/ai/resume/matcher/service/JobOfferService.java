package net.wowdev.ai.resume.matcher.service;

import java.util.Map;
import java.util.Optional;
import java.util.List;
import net.wowdev.ai.resume.matcher.dto.JobOfferRequest;
import net.wowdev.ai.resume.matcher.model.JobOffer;
import net.wowdev.ai.resume.matcher.repository.JobOfferRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class JobOfferService {

  private final JobOfferRepository repository;
  private final JobOfferValidator validator;
  private final JobOfferIdGenerator idGenerator;

  public JobOfferService(
      JobOfferRepository repository,
      JobOfferValidator validator,
      JobOfferIdGenerator idGenerator) {
    this.repository = repository;
    this.validator = validator;
    this.idGenerator = idGenerator;
  }

  public JobOffer create(JobOfferRequest request) {
    validate(request);
    return save(toJobOffer(null, idGenerator.nextJobId(), request));
  }

  public List<JobOffer> findAll() {
    return repository.findAll();
  }

  public JobOffer update(String id, JobOfferRequest request) {
    validate(request);
    var existing = findById(id)
        .orElseThrow(() -> new JobOfferNotFoundException(id));
    return save(toJobOffer(existing.id(), existing.jobId(), request));
  }

  public void delete(String id) {
    findById(id)
        .orElseThrow(() -> new JobOfferNotFoundException(id));
    try {
      repository.deleteById(id);
    } catch (DataAccessException exception) {
      throw new JobOfferPersistenceException(exception);
    }
  }

  private Optional<JobOffer> findById(String id) {
    try {
      return repository.findById(id);
    } catch (DataAccessException exception) {
      throw new JobOfferPersistenceException(exception);
    }
  }

  private JobOffer save(JobOffer offer) {
    try {
      return repository.save(offer);
    } catch (DataAccessException exception) {
      throw new JobOfferPersistenceException(exception);
    }
  }

  private void validate(JobOfferRequest request) {
    Map<String, String> errors = validator.validate(request);
    if (!errors.isEmpty()) {
      throw new JobOfferValidationException(errors);
    }
  }

  private JobOffer toJobOffer(String id, String jobId, JobOfferRequest request) {
    return new JobOffer(
        id,
        jobId,
        request.company(),
        request.jobName(),
        request.jobDescription(),
        request.requiredQualifications(),
        request.yearsOfExperience(),
        request.location(),
        request.allocation());
  }
}
