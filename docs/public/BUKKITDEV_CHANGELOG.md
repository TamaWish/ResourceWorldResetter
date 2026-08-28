## ResourceWorldResetter 5.0.0

Split-platform release for **Spigot**, **CraftBukkit**, **Paper**, **Purpur**, and **Folia**.

This is a **breaking release**. v4 configuration is not loaded automatically.

### Downloads

Install **exactly one** platform JAR. Do not install a separate `rwr-core` file.

| Server | Artifact | Required world plugin |
|--------|----------|------------------------|
| Spigot / CraftBukkit | `RWR-Spigot-5.0.0.jar` | [Multiverse-Core 5.8.0+](https://modrinth.com/plugin/multiverse-core) |
| Paper / Purpur / Folia | `RWR-Paper-Folia-5.0.0.jar` | [Worlds by TheNextLvl 4.4.0+](https://modrinth.com/plugin/worlds-1) |

Paper, Purpur, and Folia do **not** use Multiverse-Core. Multiverse-Core does not support Folia.

Java 21+ · Minecraft API 1.21.4+, including the 26.x server line

### Added

- Separate Spigot/Multiverse-Core and Paper-Folia/Worlds artifacts, with the shared core packaged inside each JAR
- Stable world IDs, unified display names, and explicit platform world-provider identities
- Daily, weekly, monthly, and interval schedules with timezone-aware next-run calculation
- Whole-minute warning broadcasts (`warning-minutes`)
- Reset phases: precheck, evacuation, regeneration, verification, completion, failure, and interrupted recovery
- Persistent reset history and a crash journal that records interrupted operations without auto-replaying ambiguous ones
- Admin GUI: dashboard, add world, world config, global settings, teleport admin, history, and reset confirmation
- Player teleport GUI: pagination, permissions, player counts, locked visibility, and reset-phase protection
- Cancellable pre-reset and terminal post-reset API events
- MiniMessage `messages.yml` and relocated bStats metrics

### Changed

- RWR delegates regeneration to the selected world plugin. It does not own a direct unload/delete/create pipeline
- Paper / Purpur / Folia now use Worlds and do not use Multiverse-Core
- Spigot / CraftBukkit continue to use Multiverse-Core through the dedicated Spigot artifact
- `display-name` is the single presentation name used by chat, logs, notifications, status, history, and GUIs
- Warning values are stored as `warning-minutes`; sub-minute legacy warnings are not supported
- `/rwr status [id]` is the single command for current phase and next-schedule information

### Fixed

- Paper reset deadlocks from waiting on asynchronous teleports on the primary scheduler
- Folia global-region watchdog stalls from blocking on entity-region teleport futures
- Fully asynchronous Folia evacuation; regeneration starts only after teleports complete and remaining players are checked
- Paper/Folia GUI, teleport, command-completion, and admin notification callbacks now run on the owning entity scheduler
- Worlds regeneration applies same, fixed, and random seed policies and honors level-configuration, gamerule, and world-border preservation
- Safe automatic failures use the configured retry delay and maximum retry count; ambiguous and interrupted outcomes stay unscheduled for operator review
- Schedule listeners refresh after lifecycle reconciliation
- Teleport overrides whose world names contain dots now round-trip through YAML
- Paper/Folia startup rejects missing, unparseable, or older-than-4.4.0 Worlds installations
- Paper/Folia status and history no longer label Worlds as Multiverse
- Spigot command output, countdown broadcasts, and server-log messages remain visible
- Paper/Folia reset and teleport GUI paths no longer wait synchronously on platform futures

### Upgrade from v4

v4.2.1 and earlier are **legacy**. v5 uses `config-version: 5` and does not auto-migrate.

1. Stop the server and back up the plugin data folder plus every managed resource world
2. Install the world plugin that matches the chosen RWR artifact
3. Remove the legacy JAR. Do not run v4 and v5 together
4. Start once to generate a fresh v5 configuration, then recreate worlds, display names, warning minutes, teleports, and schedules
5. Run one supervised `/rwr reset <id>` and confirm `/rwr status` before enabling unattended resets

[Operations & Migration](https://github.com/TamaWish/ResourceWorldResetter/blob/main/docs/public/OPERATIONS_AND_MIGRATION.md)

### Release validation

- `mvn clean verify`: 83 tests passed (59 core, 18 Spigot, 6 Paper/Folia)
- Live-tested on Folia 26.1.2 build 8, Java 25, Worlds 4.4.0
- Live-tested on Paper 26.2 build 119, Java 25, Worlds 4.4.0
- Supervised Worlds regenerations preserved the configured seed and world border, then restored the daily schedule
- No RWR exception, Folia thread-access violation, watchdog stall, or reset warning during those runs
- Purpur shares the validated Paper API path and is expected to be compatible, but was not separately exercised
