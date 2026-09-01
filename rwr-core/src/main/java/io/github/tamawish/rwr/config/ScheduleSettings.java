package io.github.tamawish.rwr.config;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleSettings(
        ScheduleType type,
        LocalTime time,
        DayOfWeek dayOfWeek,
        int dayOfMonth,
        int intervalMinutes) {}
