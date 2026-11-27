package net.wowdev.ai.resume.matcher.repository;

import java.util.List;
import java.util.Optional;
import net.wowdev.ai.resume.matcher.dto.CandidateSummaryResponse;
import net.wowdev.ai.resume.matcher.model.Candidate;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Collation.ComparisonLevel;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class MongoCandidateRepository implements CandidateRepository {

  private static final int PAGE_SIZE = 15;
  private static final Collation FULLNAME_COLLATION =
      Collation.of("en").strength(ComparisonLevel.secondary());

  private final MongoTemplate mongoTemplate;

  public MongoCandidateRepository(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public Optional<Candidate> findByEmail(String email) {
    return Optional.ofNullable(mongoTemplate.findOne(emailQuery(email), Candidate.class));
  }

  @Override
  public Candidate replaceByEmail(Candidate candidate) {
    String normalizedEmail = Candidate.normalizeEmail(candidate.email());
    Optional<Candidate> previous = findByEmail(normalizedEmail);
    Candidate replacement = withId(candidate, previous.map(Candidate::id).orElse(candidate.id()));
    if (previous.isPresent()) {
      return mongoTemplate.save(replacement);
    }
    return mongoTemplate.insert(replacement);
  }

  @Override
  public void deleteByEmail(String email) {
    mongoTemplate.remove(emailQuery(email), Candidate.class);
  }

  @Override
  public Optional<Candidate> findById(String id) {
    return Optional.ofNullable(mongoTemplate.findById(id, Candidate.class));
  }

  @Override
  public List<CandidateSummaryResponse> findSummaryPage(int page) {
    Query query =
        new Query()
            .skip((long) page * PAGE_SIZE)
            .limit(PAGE_SIZE)
            .with(Sort.by(Sort.Order.asc("fullname"), Sort.Order.asc("_id")))
            .collation(FULLNAME_COLLATION);
    query
        .fields()
        .include("_id")
        .include("fullname")
        .include("position")
        .include("country")
        .include("city")
        .include("yearsOfExperience");
    return mongoTemplate.find(query, CandidateSummaryResponse.class, "candidates");
  }

  @Override
  public long count() {
    return mongoTemplate.count(new Query(), Candidate.class);
  }

  private Query emailQuery(String email) {
    return Query.query(Criteria.where("email").is(Candidate.normalizeEmail(email)));
  }

  private Candidate withId(Candidate candidate, String id) {
    return new Candidate(
        id,
        candidate.email(),
        candidate.fullname(),
        candidate.country(),
        candidate.city(),
        candidate.position(),
        candidate.skills(),
        candidate.allocation(),
        candidate.yearsOfExperience(),
        candidate.experiences());
  }
}
