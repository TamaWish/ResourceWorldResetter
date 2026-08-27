package com.lozaine.resourceworldresetter.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.lozaine.resourceworldresetter.config.ScheduleSettings;
import com.lozaine.resourceworldresetter.config.ScheduleType;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class NextRunCalculatorTest {
    private final NextRunCalculator calculator = new NextRunCalculator();

    @Test
    void calculatesDailyAndWeeklyRunsInConfiguredZone() {
        ZoneId zone = ZoneId.of("Asia/Kuala_Lumpur");
        ZonedDateTime after = ZonedDateTime.of(2026, 8, 27, 4, 0, 0, 0, zone);

        ZonedDateTime daily = calculator.nextRun(
                new ScheduleSettings(ScheduleType.DAILY, LocalTime.of(3, 0), null, 0, 0),
                zone,
                after);
        ZonedDateTime weekly = calculator.nextRun(
                new ScheduleSettings(ScheduleType.WEEKLY, LocalTime.of(5, 30), DayOfWeek.MONDAY, 0, 0),
                zone,
                after);

        assertThat(daily).isEqualTo(ZonedDateTime.of(2026, 8, 28, 3, 0, 0, 0, zone));
        assertThat(weekly).isEqualTo(ZonedDateTime.of(2026, 8, 31, 5, 30, 0, 0, zone));
    }

    @Test
    void clampsMonthlyDayToShortMonthThenRestoresRequestedDay() {
        ZoneId zone = ZoneId.of("UTC");
        ScheduleSettings schedule =
                new ScheduleSettings(ScheduleType.MONTHLY, LocalTime.of(2, 0), null, 31, 0);

        ZonedDateTime february = calculator.nextRun(
                schedule,
                zone,
                ZonedDateTime.of(2026, 1, 31, 3, 0, 0, 0, zone));
        ZonedDateTime march = calculator.nextRun(schedule, zone, february);

        assertThat(february).isEqualTo(ZonedDateTime.of(2026, 2, 28, 2, 0, 0, 0, zone));
        assertThat(march).isEqualTo(ZonedDateTime.of(2026, 3, 31, 2, 0, 0, 0, zone));
    }

    @Test
    void resolvesDailyTimeInsideDaylightSavingGap() {
        ZoneId zone = ZoneId.of("Europe/Berlin");
        ZonedDateTime result = calculator.nextRun(
                new ScheduleSettings(ScheduleType.DAILY, LocalTime.of(2, 30), null, 0, 0),
                zone,
                ZonedDateTime.of(2026, 3, 28, 23, 0, 0, 0, zone));

        assertThat(result.toLocalDate().toString()).isEqualTo("2026-03-29");
        assertThat(result.toLocalTime()).isEqualTo(LocalTime.of(3, 30));
        assertThat(result.getOffset()).isEqualTo(ZoneOffset.ofHours(2));
    }

    @Test
    void selectsFirstOccurrenceDuringDaylightSavingOverlap() {
        ZoneId zone = ZoneId.of("Europe/Berlin");
        ZonedDateTime result = calculator.nextRun(
                new ScheduleSettings(ScheduleType.DAILY, LocalTime.of(2, 30), null, 0, 0),
                zone,
                ZonedDateTime.of(2026, 10, 24, 23, 0, 0, 0, zone));

        assertThat(result.toLocalDate().toString()).isEqualTo("2026-10-25");
        assertThat(result.getOffset()).isEqualTo(ZoneOffset.ofHours(2));
    }

    @Test
    void intervalUsesElapsedTimeAcrossDaylightSavingTransition() {
        ZoneId zone = ZoneId.of("Europe/Berlin");
        ZonedDateTime after = ZonedDateTime.of(2026, 3, 29, 1, 50, 0, 0, zone);
        ZonedDateTime result = calculator.nextRun(
                new ScheduleSettings(ScheduleType.INTERVAL, null, null, 0, 30),
                zone,
                after);

        assertThat(Duration.between(after.toInstant(), result.toInstant())).isEqualTo(Duration.ofMinutes(30));
        assertThat(result.toLocalTime()).isEqualTo(LocalTime.of(3, 20));
    }
}
