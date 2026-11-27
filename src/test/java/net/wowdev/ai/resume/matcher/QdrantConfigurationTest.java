package net.wowdev.ai.resume.matcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class QdrantConfigurationTest {

  @Test
  void shouldConfigureQdrantVectorStoreWithoutConnectingToServer() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(QdrantVectorStoreAutoConfiguration.class))
        .withUserConfiguration(EmbeddingConfiguration.class)
        .withPropertyValues(
            "spring.ai.vectorstore.qdrant.host=localhost",
            "spring.ai.vectorstore.qdrant.port=6334",
            "spring.ai.vectorstore.qdrant.collection-name=candidate-resumes",
            "spring.ai.vectorstore.qdrant.initialize-schema=false")
        .run(context -> assertThat(context).hasSingleBean(VectorStore.class));
  }

  @Test
  void shouldConfigureQdrantWithoutConnectingToIt() throws IOException {
    Properties properties = new Properties();
    try (InputStream stream =
        getClass().getClassLoader().getResourceAsStream("application.properties")) {
      properties.load(stream);
    }

    assertEquals("qdrant", properties.getProperty("spring.ai.vectorstore.type"));
    assertEquals(
        "${QDRANT_HOST:localhost}", properties.getProperty("spring.ai.vectorstore.qdrant.host"));
    assertEquals(
        "${QDRANT_PORT:6334}", properties.getProperty("spring.ai.vectorstore.qdrant.port"));
    assertEquals(
        "${QDRANT_COLLECTION:candidate-resumes}",
        properties.getProperty("spring.ai.vectorstore.qdrant.collection-name"));
    assertEquals("false", properties.getProperty("spring.ai.vectorstore.qdrant.initialize-schema"));
  }

  @Configuration(proxyBeanMethods = false)
  static class EmbeddingConfiguration {

    @Bean
    EmbeddingModel embeddingModel() {
      return mock(EmbeddingModel.class);
    }
  }
}
