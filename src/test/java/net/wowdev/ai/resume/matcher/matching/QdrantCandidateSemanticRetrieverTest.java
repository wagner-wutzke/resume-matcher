package net.wowdev.ai.resume.matcher.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

class QdrantCandidateSemanticRetrieverTest {

  private final VectorStore vectorStore = mock(VectorStore.class);
  private final CandidateMatchingProperties properties =
      new CandidateMatchingProperties("10", "17", "10");
  private final QdrantCandidateSemanticRetriever retriever =
      new QdrantCandidateSemanticRetriever(vectorStore, properties);

  @Test
  void shouldQueryWithSemanticTextAndConfiguredPoolCap() {
    when(vectorStore.similaritySearch(org.mockito.ArgumentMatchers.any(SearchRequest.class)))
        .thenReturn(List.of());

    retriever.retrieveCandidateEmails("Senior engineer and required qualifications");

    ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
    verify(vectorStore).similaritySearch(requestCaptor.capture());
    assertThat(requestCaptor.getValue().getQuery())
        .isEqualTo("Senior engineer and required qualifications");
    assertThat(requestCaptor.getValue().getTopK()).isEqualTo(17);
  }

  @Test
  void shouldReturnOnlyEmailMetadataWithoutUsingHitScoreOrOrder() {
    Document firstHit = documentWithEmail("first@example.com");
    Document secondHit = documentWithEmail("second@example.com");
    when(vectorStore.similaritySearch(org.mockito.ArgumentMatchers.any(SearchRequest.class)))
        .thenReturn(List.of(firstHit, secondHit));
    Set<String> firstResult = retriever.retrieveCandidateEmails("query");

    when(vectorStore.similaritySearch(org.mockito.ArgumentMatchers.any(SearchRequest.class)))
        .thenReturn(List.of(secondHit, firstHit));
    Set<String> reversedResult = retriever.retrieveCandidateEmails("query");

    assertThat(firstResult).containsExactlyInAnyOrder("first@example.com", "second@example.com");
    assertThat(reversedResult).isEqualTo(firstResult);
    verify(firstHit, never()).getScore();
    verify(secondHit, never()).getScore();
    verify(firstHit, never()).getText();
    verify(secondHit, never()).getText();
  }

  @Test
  void shouldIgnoreMissingBlankAndNonStringEmailMetadata() {
    Document noMetadata = mock(Document.class);
    Document blankEmail = mock(Document.class);
    Document nonStringEmail = mock(Document.class);
    when(noMetadata.getMetadata()).thenReturn(null);
    when(blankEmail.getMetadata()).thenReturn(Map.of("email", "  "));
    when(nonStringEmail.getMetadata()).thenReturn(Map.of("email", 12));
    when(vectorStore.similaritySearch(org.mockito.ArgumentMatchers.any(SearchRequest.class)))
        .thenReturn(List.of(noMetadata, blankEmail, nonStringEmail));

    assertThat(retriever.retrieveCandidateEmails("query")).isEmpty();
  }

  @Test
  void shouldFailWhenVectorStoreReturnsAnUnusableNullResponse() {
    when(vectorStore.similaritySearch(org.mockito.ArgumentMatchers.any(SearchRequest.class)))
        .thenReturn(null);

    assertThatThrownBy(() -> retriever.retrieveCandidateEmails("query"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Qdrant returned no semantic retrieval response");
  }

  private Document documentWithEmail(String email) {
    Document document = mock(Document.class);
    when(document.getMetadata()).thenReturn(Map.of("email", email));
    return document;
  }
}
