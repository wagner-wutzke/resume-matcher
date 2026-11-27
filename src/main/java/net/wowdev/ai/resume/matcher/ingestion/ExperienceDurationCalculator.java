package net.wowdev.ai.resume.matcher.ingestion;

import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.wowdev.ai.resume.matcher.model.CandidateExperience;
import org.springframework.stereotype.Component;

@Component
public class ExperienceDurationCalculator {

  private static final String YEAR = "\\d{4}";
  private static final String MONTH_DATE = "(?:[A-Za-z]{3,9}\\s+\\d{4}|\\d{1,2}/\\d{4})";
  private static final String END_MARKER = "(?:present|today|current|now|ongoing)";
  private static final Pattern YEAR_RANGE =
      Pattern.compile(
          "^(\\d{4})\\s*(?:[-–—]|to|until)\\s*(\\d{4}|" + END_MARKER + ")$",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern MONTH_RANGE =
      Pattern.compile(
          "^((?:"
              + MONTH_DATE
              + "|"
              + YEAR
              + "))\\s+(?:-|–|—|to|until)\\s+"
              + "((?:"
              + MONTH_DATE
              + "|"
              + YEAR
              + "|"
              + END_MARKER
              + "))$",
          Pattern.CASE_INSENSITIVE);

  private final Clock clock;

  public ExperienceDurationCalculator() {
    this(Clock.systemDefaultZone());
  }

  ExperienceDurationCalculator(Clock clock) {
    this.clock = clock;
  }

  public String calculate(List<CandidateExperience> experiences) {
    long totalMonths = 0;
    if (experiences != null) {
      for (CandidateExperience experience : experiences) {
        totalMonths += durationMonths(experience.period()).orElse(0);
      }
    }
    long years = totalMonths / 12;
    long months = totalMonths % 12;
    return "%d %s and %d %s"
        .formatted(years, years == 1 ? "year" : "years", months, months == 1 ? "month" : "months");
  }

  OptionalLong durationMonths(String period) {
    if (period == null || period.isBlank()) {
      return OptionalLong.empty();
    }
    Matcher yearMatcher = YEAR_RANGE.matcher(period.strip());
    if (yearMatcher.matches()) {
      int startYear = Integer.parseInt(yearMatcher.group(1));
      int endYear =
          ongoing(yearMatcher.group(2))
              ? YearMonth.now(clock).getYear()
              : Integer.parseInt(yearMatcher.group(2));
      return nonNegativeMonths((long) (endYear - startYear) * 12);
    }

    Matcher monthMatcher = MONTH_RANGE.matcher(period.strip());
    if (!monthMatcher.matches()) {
      return OptionalLong.empty();
    }
    Optional<YearMonth> start = parseMonthDate(monthMatcher.group(1));
    Optional<YearMonth> end =
        ongoing(monthMatcher.group(2))
            ? Optional.of(YearMonth.now(clock))
            : parseMonthDate(monthMatcher.group(2));
    if (start.isEmpty() || end.isEmpty()) {
      return OptionalLong.empty();
    }
    long elapsed =
        (long) (end.get().getYear() - start.get().getYear()) * 12
            + end.get().getMonthValue()
            - start.get().getMonthValue();
    return nonNegativeMonths(elapsed);
  }

  private Optional<YearMonth> parseMonthDate(String value) {
    String normalized = value.strip();
    try {
      if (normalized.matches("\\d{1,2}/\\d{4}")) {
        String[] parts = normalized.split("/");
        int month = Integer.parseInt(parts[0]);
        int year = Integer.parseInt(parts[1]);
        if (month < 1 || month > 12) {
          return Optional.empty();
        }
        return Optional.of(YearMonth.of(year, month));
      }
      String[] parts = normalized.split("\\s+");
      int year = Integer.parseInt(parts[parts.length - 1]);
      int month = monthNumber(parts[0]);
      return Optional.of(YearMonth.of(year, month));
    } catch (RuntimeException exception) {
      return Optional.empty();
    }
  }

  private int monthNumber(String monthName) {
    String normalized = monthName.toLowerCase(Locale.ROOT);
    for (int month = 1; month <= 12; month++) {
      String fullName = java.time.Month.of(month).name().toLowerCase(Locale.ROOT);
      if (fullName.startsWith(normalized) || normalized.startsWith(fullName.substring(0, 3))) {
        return month;
      }
    }
    throw new IllegalArgumentException("Unsupported month");
  }

  private boolean ongoing(String end) {
    return Pattern.compile(END_MARKER, Pattern.CASE_INSENSITIVE).matcher(end).matches();
  }

  private OptionalLong nonNegativeMonths(long months) {
    return months < 0 ? OptionalLong.empty() : OptionalLong.of(months);
  }
}
