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

  public JobOfferService(JobOfferRepository repository, JobOfferValidator validator) {
    this.repository = repository;
    this.validator = validator;
  }

  public JobOffer create(JobOfferRequest request) {
    validate(request);
    return save(toJobOffer(null, request));
  }

  public List<JobOffer> findAll() {
    return repository.findAll();
  }

  public JobOffer update(String id, JobOfferRequest request) {
    validate(request);
    var existing = findById(id)
        .orElseThrow(() -> new JobOfferNotFoundException(id));
    return save(toJobOffer(existing.id(), request));
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

  private JobOffer toJobOffer(String id, JobOfferRequest request) {
    return new JobOffer(
        id,
        request.jobName(),
        request.jobDescription(),
        request.requiredQualifications(),
        request.location(),
        request.allocation());
  }
}
