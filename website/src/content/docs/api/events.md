---
title: Events
description: Observe scheduled warnings and reset outcomes or cancel before regeneration begins.
---
`ResourceWorldResetWarningEvent` reports the world, whole minutes remaining, and scheduled time. It has no operation ID because the reset has not started.
```java
@EventHandler
public void onResetWarning(ResourceWorldResetWarningEvent event) {
    getLogger().info(event.getWorldName() + " resets in " + event.getMinutesRemaining() + " minutes");
}
```
`ResourceWorldPreResetEvent` is cancellable before evacuation or regeneration. `ResourceWorldPostResetEvent` is terminal and reports phase, outcome, optional failure, retry safety, and a diagnostic message. Observe outcomes at `EventPriority.MONITOR` and branch on enums instead of parsing messages.
