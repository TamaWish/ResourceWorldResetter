---
title: Install ResourceWorldResetter
description: Install the right build, configure it, and validate a supervised reset.
---

## Before you start

Back up the server, the matching RWR data folder, and every managed resource world. Use full restarts when replacing RWR or its world provider; do not use plugin hot-reload tools.

## Choose one platform

| Server | Artifact | Required provider | Data folder |
| --- | --- | --- | --- |
| Spigot / CraftBukkit | `RWR-Spigot-5.2.0.jar` | Multiverse-Core 5.8.0+ | `plugins/ResourceWorldResetter/` |
| Paper / Purpur / Folia | `RWR-Paper-Folia-5.2.0.jar` | Worlds 4.4.0+ | `plugins/ResourceWorldResetter-Paper-Folia/` |

Spigot requires Java 21+. Paper, Purpur, and Folia require Java 25+ because Worlds 4.4.0+ requires it. Do not install both RWR platform JARs, and do not install `rwr-core` separately.

## Installation

1. Stop the server and install the required world-management plugin.
2. Place the matching RWR JAR in `plugins/`; remove any other RWR platform JAR.
3. Start the server and confirm both the provider and RWR enable successfully.
4. Review the generated `config.yml` and `locales/en_US.yml`, or open `/rwr gui`.
5. Run `/rwr status` and `/rwr history 10`.
6. Run one supervised `/rwr reset <world-id>` before enabling unattended schedules.

See [Operations & migration](../reference/operations-and-migration/) for the complete runbook.
