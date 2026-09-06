---
title: Schedules and evacuation
description: Configure safe automation and player movement.
---

RWR supports daily, weekly, monthly, and interval schedules. Calendar schedules use the configured timezone, and warning values are whole minutes that are deduplicated before scheduling.

- Use an empty warning list when countdown broadcasts are unwanted.
- Shutdown cancels one-shot reset and warning tasks; normal startup reconstructs schedules.
- The evacuation destination must resolve to a loaded, safe provider world.
- Regeneration never starts while the remaining-player count is non-zero.

Spigot uses synchronous Bukkit teleports. Paper uses its safe primary-thread path. Folia begins asynchronous entity-region teleports, then resumes the reset from the global scheduler only when they finish.
