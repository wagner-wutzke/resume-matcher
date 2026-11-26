package net.wowdev.ai.resume.matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;

class MongoConfigurationTest {

  private static final String PROPERTY = "spring.mongodb.uri";
  private static final String DEFAULT_URI = "mongodb://localhost:27017/resume_matcher";

  @Test
  void usesLocalUriWhenEnvironmentVariableIsNotSet() throws IOException {
    var resolver = resolverForApplicationProperties(java.util.Map.of());

    assertEquals(DEFAULT_URI, resolver.getProperty(PROPERTY));
  }

  @Test
  void usesMongoUriEnvironmentVariableWhenSet() throws IOException {
    var resolver = resolverForApplicationProperties(
        java.util.Map.of("MONGODB_URI", "mongodb://db.example/resume"));

    assertEquals("mongodb://db.example/resume", resolver.getProperty(PROPERTY));
  }

  private PropertySourcesPropertyResolver resolverForApplicationProperties(
      java.util.Map<String, Object> environment) throws IOException {
    var properties = new Properties();
    try (var input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
      if (input == null) {
        throw new IllegalStateException("application.properties is missing from the test classpath");
      }
      properties.load(input);
    }

    var sources = new MutablePropertySources();
    sources.addLast(new MapPropertySource("application", toMap(properties)));
    if (!environment.isEmpty()) {
      sources.addFirst(new MapPropertySource("environment", environment));
    }
    return new PropertySourcesPropertyResolver(sources);
  }

  private java.util.Map<String, Object> toMap(Properties properties) {
    var values = new java.util.HashMap<String, Object>();
    properties.forEach((key, value) -> values.put(key.toString(), value));
    return values;
  }
}
