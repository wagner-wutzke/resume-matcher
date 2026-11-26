package net.wowdev.ai.resume.matcher.repository;

import java.util.List;
import java.util.Optional;
import net.wowdev.ai.resume.matcher.model.JobOffer;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.Repository;

public interface JobOfferRepository extends Repository<JobOffer, String> {

  @Query("{}")
  List<JobOffer> findAll();

  Optional<JobOffer> findById(String id);

  JobOffer save(JobOffer jobOffer);

  void deleteById(String id);
}
