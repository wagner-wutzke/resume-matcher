package net.wowdev.ai.resume.matcher.matching;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class CandidateMatchingPropertiesTest {

  private static final List<String> PROPERTY_NAMES =
      List.of(
          "candidate.matching.max-results",
          "candidate.matching.pool-cap",
          "candidate.matching.chunk-size");

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(TestConfiguration.class);

  @Test
  void shouldUseDefaultValuesWhenPropertiesAreNotSet() {
    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();
          CandidateMatchingProperties properties =
              context.getBean(CandidateMatchingProperties.class);
          assertThat(properties.getMaxResults()).isEqualTo(10);
          assertThat(properties.getPoolCap()).isEqualTo(20);
          assertThat(properties.getChunkSize()).isEqualTo(10);
        });
  }

  @Test
  void shouldUseConfiguredValues() {
    contextRunner
        .withPropertyValues(
            "candidate.matching.max-results=7",
            "candidate.matching.pool-cap=35",
            "candidate.matching.chunk-size=5")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              CandidateMatchingProperties properties =
                  context.getBean(CandidateMatchingProperties.class);
              assertThat(properties.getMaxResults()).isEqualTo(7);
              assertThat(properties.getPoolCap()).isEqualTo(35);
              assertThat(properties.getChunkSize()).isEqualTo(5);
            });
  }

  @Test
  void shouldRejectNonIntegerZeroAndNegativeValuesForEveryProperty() {
    for (String propertyName : PROPERTY_NAMES) {
      for (String value : List.of("invalid", "0", "-1")) {
        contextRunner
            .withPropertyValues(propertyName + "=" + value)
            .run(
                context -> {
                  assertThat(context).hasFailed();
                  assertThat(context.getStartupFailure())
                      .hasRootCauseInstanceOf(IllegalStateException.class)
                      .hasRootCauseMessage(propertyName + " must be a positive integer");
                });
      }
    }
  }

  @Configuration(proxyBeanMethods = false)
  @Import(CandidateMatchingProperties.class)
  static class TestConfiguration {}
}
