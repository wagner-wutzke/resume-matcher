package net.wowdev.ai.resume.matcher.matching;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UnavailableCandidateSemanticRetrieverTest {

  @Test
  void shouldFailRetrievalWhenVectorStoreIsDisabled() {
    UnavailableCandidateSemanticRetriever retriever = new UnavailableCandidateSemanticRetriever();

    assertThatThrownBy(() -> retriever.retrieveCandidateEmails("position query"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Semantic vector store is unavailable");
  }
}
