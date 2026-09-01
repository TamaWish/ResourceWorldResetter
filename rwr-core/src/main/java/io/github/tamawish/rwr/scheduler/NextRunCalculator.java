package io.github.tamawish.rwr.scheduler;

import io.github.tamawish.rwr.config.ScheduleSettings;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

public final class NextRunCalculator {
    public ZonedDateTime nextRun(
            ScheduleSettings schedule,
            ZoneId zone,
            ZonedDateTime afterExclusive) {
        ZonedDateTime after = afterExclusive.withZoneSameInstant(zone);
        return switch (schedule.type()) {
            case DAILY -> nextDaily(schedule, zone, after);
            case WEEKLY -> nextWeekly(schedule, zone, after);
            case MONTHLY -> nextMonthly(schedule, zone, after);
            case INTERVAL -> after.plusMinutes(schedule.intervalMinutes());
        };
    }

    private static ZonedDateTime nextDaily(
            ScheduleSettings schedule,
            ZoneId zone,
            ZonedDateTime after) {
        LocalDate date = after.toLocalDate();
        ZonedDateTime candidate = at(date, schedule, zone);
        return candidate.isAfter(after) ? candidate : at(date.plusDays(1), schedule, zone);
    }

    private static ZonedDateTime nextWeekly(
            ScheduleSettings schedule,
            ZoneId zone,
            ZonedDateTime after) {
        LocalDate date = after.toLocalDate().with(TemporalAdjusters.nextOrSame(schedule.dayOfWeek()));
        ZonedDateTime candidate = at(date, schedule, zone);
        return candidate.isAfter(after) ? candidate : at(date.plusWeeks(1), schedule, zone);
    }

    private static ZonedDateTime nextMonthly(
            ScheduleSettings schedule,
            ZoneId zone,
            ZonedDateTime after) {
        YearMonth month = YearMonth.from(after);
        ZonedDateTime candidate = at(month, schedule, zone);
        return candidate.isAfter(after) ? candidate : at(month.plusMonths(1), schedule, zone);
    }

    private static ZonedDateTime at(
            YearMonth month,
            ScheduleSettings schedule,
            ZoneId zone) {
        int day = Math.min(schedule.dayOfMonth(), month.lengthOfMonth());
        return at(month.atDay(day), schedule, zone);
    }

    private static ZonedDateTime at(
            LocalDate date,
            ScheduleSettings schedule,
            ZoneId zone) {
        return LocalDateTime.of(date, schedule.time()).atZone(zone);
    }
}
