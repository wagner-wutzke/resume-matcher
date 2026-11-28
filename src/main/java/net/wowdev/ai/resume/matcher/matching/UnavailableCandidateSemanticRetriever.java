package net.wowdev.ai.resume.matcher.matching;

import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "spring.ai.vectorstore.type", havingValue = "none")
public class UnavailableCandidateSemanticRetriever implements CandidateSemanticRetriever {

  @Override
  public Set<String> retrieveCandidateEmails(String semanticQuery) {
    throw new IllegalStateException("Semantic vector store is unavailable");
  }
}
