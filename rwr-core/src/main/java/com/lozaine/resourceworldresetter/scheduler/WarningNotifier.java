package com.lozaine.resourceworldresetter.scheduler;

import com.lozaine.resourceworldresetter.config.ManagedWorldSettings;
import java.time.ZonedDateTime;

public interface WarningNotifier {
    void warn(ManagedWorldSettings world, int minutesRemaining, ZonedDateTime resetAt);
}
