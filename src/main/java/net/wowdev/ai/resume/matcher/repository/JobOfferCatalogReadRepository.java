package net.wowdev.ai.resume.matcher.repository;

import java.util.List;
import net.wowdev.ai.resume.matcher.model.JobOfferCatalogEntry;
import org.springframework.data.repository.Repository;

public interface JobOfferCatalogReadRepository extends Repository<JobOfferCatalogEntry, String> {

  List<JobOfferCatalogEntry> findAll();
}
