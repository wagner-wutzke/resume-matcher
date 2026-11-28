package net.wowdev.ai.resume.matcher.matching;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "spring.ai.vectorstore.type", havingValue = "qdrant")
public class QdrantCandidateSemanticRetriever implements CandidateSemanticRetriever {

  private static final String EMAIL_METADATA_KEY = "email";

  private final VectorStore vectorStore;
  private final CandidateMatchingProperties properties;

  public QdrantCandidateSemanticRetriever(
      VectorStore vectorStore, CandidateMatchingProperties properties) {
    this.vectorStore = vectorStore;
    this.properties = properties;
  }

  @Override
  public Set<String> retrieveCandidateEmails(String semanticQuery) {
    SearchRequest request =
        SearchRequest.builder().query(semanticQuery).topK(properties.getPoolCap()).build();
    List<Document> hits = vectorStore.similaritySearch(request);
    if (hits == null) {
      throw new IllegalStateException("Qdrant returned no semantic retrieval response");
    }

    Set<String> candidateEmails = new HashSet<>();
    for (Document hit : hits) {
      String email = emailFrom(hit.getMetadata());
      if (email != null && !email.isBlank()) {
        candidateEmails.add(email);
      }
    }
    return Set.copyOf(candidateEmails);
  }

  private String emailFrom(Map<String, Object> metadata) {
    if (metadata == null) {
      return null;
    }
    Object value = metadata.get(EMAIL_METADATA_KEY);
    return value instanceof String email ? email : null;
  }
}
