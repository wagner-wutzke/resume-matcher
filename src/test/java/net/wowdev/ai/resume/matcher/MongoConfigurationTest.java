package net.wowdev.ai.resume.matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.env.SystemEnvironmentPropertySource;

class MongoConfigurationTest {

  private static final String HOST = "spring.mongodb.host";
  private static final String PORT = "spring.mongodb.port";
  private static final String DATABASE = "spring.mongodb.database";
  private static final String USERNAME = "spring.mongodb.username";
  private static final String PASSWORD = "spring.mongodb.password";
  private static final String AUTHENTICATION_DATABASE = "spring.mongodb.authentication-database";

  @Test
  void usesIndividualLocalDefaultsWhenEnvironmentVariablesAreNotSet() throws IOException {
    var resolver = resolverForApplicationProperties(java.util.Map.of());

    assertEquals("localhost", resolver.getProperty(HOST));
    assertEquals("27017", resolver.getProperty(PORT));
    assertEquals("resume_matcher", resolver.getProperty(DATABASE));
    assertEquals("admin", resolver.getProperty(USERNAME));
    assertEquals("secretpassword", resolver.getProperty(PASSWORD));
    assertEquals("admin", resolver.getProperty(AUTHENTICATION_DATABASE));
    assertNull(resolver.getProperty("spring.mongodb.uri"));
  }

  @Test
  void resolvesIndividualSettingsFromEnvironmentWhenSet() throws IOException {
    var resolver =
        resolverForApplicationProperties(
            java.util.Map.of(
                "MONGODB_HOST", "db.example",
                "MONGODB_PORT", "27018",
                "MONGODB_DATABASE", "resume_test",
                "MONGODB_AUTHENTICATION_DATABASE", "admin",
                "SPRING_MONGODB_USERNAME", "resume_user",
                "SPRING_MONGODB_PASSWORD", "test-secret"));

    assertEquals("db.example", resolver.getProperty(HOST));
    assertEquals("27018", resolver.getProperty(PORT));
    assertEquals("resume_test", resolver.getProperty(DATABASE));
    assertEquals("resume_user", resolver.getProperty(USERNAME));
    assertEquals("test-secret", resolver.getProperty(PASSWORD));
    assertEquals("admin", resolver.getProperty(AUTHENTICATION_DATABASE));
    assertNull(resolver.getProperty("spring.mongodb.uri"));
  }

  private PropertySourcesPropertyResolver resolverForApplicationProperties(
      java.util.Map<String, Object> environment) throws IOException {
    var properties = new Properties();
    try (var input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
      if (input == null) {
        throw new IllegalStateException(
            "application.properties is missing from the test classpath");
      }
      properties.load(input);
    }

    var sources = new MutablePropertySources();
    sources.addLast(new MapPropertySource("application", toMap(properties)));
    if (!environment.isEmpty()) {
      sources.addFirst(new SystemEnvironmentPropertySource("environment", environment));
    }
    return new PropertySourcesPropertyResolver(sources);
  }

  private java.util.Map<String, Object> toMap(Properties properties) {
    var values = new java.util.HashMap<String, Object>();
    properties.forEach((key, value) -> values.put(key.toString(), value));
    return values;
  }
}
