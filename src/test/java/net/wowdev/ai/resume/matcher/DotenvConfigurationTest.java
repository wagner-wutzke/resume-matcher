package net.wowdev.ai.resume.matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

class DotenvConfigurationTest {

  @TempDir Path temporaryDirectory;

  @Test
  void shouldConfigureOptionalDotenvImport() throws Exception {
    Properties properties = new Properties();
    try (InputStream stream =
        getClass().getClassLoader().getResourceAsStream("application.properties")) {
      properties.load(stream);
    }

    assertEquals("optional:file:.env[.properties]", properties.getProperty("spring.config.import"));
  }

  @Test
  void shouldLoadEnvironmentVariablesFromDotenvPropertiesFile() throws Exception {
    Files.writeString(temporaryDirectory.resolve(".env"), "DOTENV_TEST_VALUE=loaded\n");
    Path applicationProperties = temporaryDirectory.resolve("application.properties");
    Files.writeString(
        applicationProperties,
        "spring.config.import=optional:" + temporaryDirectory.resolve(".env").toUri() + "[.properties]\n");

    SpringApplication application = new SpringApplication(DotenvTestConfiguration.class);
    application.setWebApplicationType(WebApplicationType.NONE);
    ConfigurableApplicationContext context =
        application.run("--spring.config.location=" + temporaryDirectory.toUri());

    try {
      assertEquals("loaded", context.getEnvironment().getProperty("DOTENV_TEST_VALUE"));
    } finally {
      context.close();
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class DotenvTestConfiguration {}
}
