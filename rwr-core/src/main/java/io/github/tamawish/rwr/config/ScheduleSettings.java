package io.github.tamawish.rwr.config;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Calendar or interval schedule for a managed world.
 *
 * @param type schedule calculation strategy
 * @param time local reset time for calendar schedules
 * @param dayOfWeek weekday used by weekly schedules
 * @param dayOfMonth calendar day used by monthly schedules
 * @param intervalMinutes delay used by interval schedules
 */
public record ScheduleSettings(
    ScheduleType type, LocalTime time, DayOfWeek dayOfWeek, int dayOfMonth, int intervalMinutes) {}
