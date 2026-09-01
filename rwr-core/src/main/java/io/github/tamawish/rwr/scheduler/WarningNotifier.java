package io.github.tamawish.rwr.scheduler;

import io.github.tamawish.rwr.config.ManagedWorldSettings;
import java.time.ZonedDateTime;

public interface WarningNotifier {
    void warn(ManagedWorldSettings world, int minutesRemaining, ZonedDateTime resetAt);
}
