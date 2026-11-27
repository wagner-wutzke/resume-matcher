package net.wowdev.ai.resume.matcher.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import net.wowdev.ai.resume.matcher.model.CandidateExperience;
import org.junit.jupiter.api.Test;

class ExperienceDurationCalculatorTest {

  private final ExperienceDurationCalculator calculator =
      new ExperienceDurationCalculator(
          Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneOffset.UTC));

  @Test
  void shouldCalculateYearOnlyPeriodsAndOngoingMarkers() {
    assertEquals("10 years and 0 months", calculate("2016 – Present"));
    assertEquals("5 years and 0 months", calculate("2021 - Today"));
    assertEquals("6 years and 0 months", calculate("2020 - current"));
  }

  @Test
  void shouldCalculateYearOnlyPeriodsWithWordSeparators() {
    assertEquals("1 year and 0 months", calculate("2020 to 2021"));
    assertEquals("1 year and 0 months", calculate("2020 until 2021"));
    assertEquals("6 years and 0 months", calculate("2020 to Present"));
  }

  @Test
  void shouldCalculateElapsedMonthsForMonthPrecisionDates() {
    assertEquals("1 year and 0 months", calculate("January 2020 - January 2021"));
    assertEquals("1 year and 5 months", calculate("03/2020 to 08/2021"));
    assertEquals("6 years and 3 months", calculate("Mar 2020 – Present"));
  }

  @Test
  void shouldSumEveryExperienceIncludingOverlaps() {
    assertEquals(
        "4 years and 0 months",
        calculator.calculate(List.of(experience("2020 - 2022"), experience("2021 - 2023"))));
  }

  @Test
  void shouldIgnoreUnusableOrNegativePeriodsAndReturnZeroWhenNoneAreUsable() {
    assertEquals("0 years and 0 months", calculate("unknown dates"));
    assertEquals("0 years and 0 months", calculate("2022 - 2020"));
    assertEquals("0 years and 0 months", calculator.calculate(List.of()));
  }

  private String calculate(String period) {
    return calculator.calculate(List.of(experience(period)));
  }

  private CandidateExperience experience(String period) {
    return new CandidateExperience("", "", period, "", "");
  }
}
