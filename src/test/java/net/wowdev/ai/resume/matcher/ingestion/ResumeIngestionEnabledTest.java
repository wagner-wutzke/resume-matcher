package net.wowdev.ai.resume.matcher.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class ResumeIngestionEnabledTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(EnabledConfiguration.class);

  @Test
  void shouldDisableIngestionWhenPropertyIsMissing() {
    contextRunner.run(context -> assertThat(context).doesNotHaveBean(EnabledMarker.class));
  }

  @Test
  void shouldDisableIngestionWhenExplicitlyDisabled() {
    contextRunner
        .withPropertyValues("resume.ingestion.enabled=false")
        .run(context -> assertThat(context).doesNotHaveBean(EnabledMarker.class));
  }

  @Test
  void shouldEnableIngestionWhenExplicitlyEnabled() {
    contextRunner
        .withPropertyValues("resume.ingestion.enabled=true")
        .run(context -> assertThat(context).hasSingleBean(EnabledMarker.class));
  }

  @Configuration(proxyBeanMethods = false)
  @ResumeIngestionEnabled
  static class EnabledConfiguration {

    @Bean
    EnabledMarker enabledMarker() {
      return new EnabledMarker();
    }
  }

  static class EnabledMarker {}
}
