# Changelog

Public release history for ResourceWorldResetter. Versions 4.2.1 and earlier are the **legacy v4 line**. Version 5 begins the split-platform architecture and requires a manual configuration migration.

## 5.0.0 — Split-platform release

### Added

- Separate production artifacts:
  - `RWR-Spigot-5.0.0.jar` for Spigot/CraftBukkit with Multiverse-Core 5.8.0+.
  - `RWR-Paper-Folia-5.0.0.jar` for Paper/Purpur/Folia with Worlds by TheNextLvl 4.4.0+.
- Shared `rwr-core` reset coordinator, packaged inside both platform artifacts.
- Stable world IDs, unified display names, and explicit platform world-provider identities.
- Daily, weekly, monthly, and interval schedules with timezone-aware next-run calculations.
- Whole-minute warning configuration and configurable countdown broadcasts.
- Reset phases for precheck, evacuation, regeneration, verification, completion, failure, and interrupted recovery.
- Persistent reset history and active-operation journaling.
- Admin dashboard, world configuration, global settings, teleport administration, history, and confirmation screens.
- Player teleport GUI with pagination, permissions, player counts, locked visibility, and reset-phase protection.
- Cancellable pre-reset and terminal post-reset API events.
- MiniMessage messages and relocated bStats integration.

### Changed

- RWR delegates the authoritative lifecycle operation to the selected world plugin. It does not own a direct unload/delete/create pipeline.
- Paper/Purpur/Folia now use Worlds by TheNextLvl and do not use Multiverse-Core.
- Spigot/CraftBukkit continue to use Multiverse-Core through the dedicated Spigot artifact.
- `display-name` is the single presentation name used by chat, logs, notifications, status, history, and GUIs.
- Warning values are stored as `warning-minutes`; sub-minute legacy warnings are not supported.

### Fixed

- Paper reset deadlock caused by waiting for asynchronous teleports on the primary/global scheduler.
- Folia global-region watchdog stalls caused by blocking on entity-region teleport futures.
- Folia evacuation is now fully asynchronous; regeneration begins only after teleport completion and a remaining-player check.
- Paper/Folia player GUI, teleport, command-completion, and administrator notification callbacks now run on the owning entity scheduler.
- Paper/Folia Worlds regeneration now applies same, fixed, and random seed policies and honors level-configuration, gamerule, and world-border preservation settings.
- Safe automatic failures now use the configured retry delay and maximum retry count; ambiguous and interrupted outcomes remain unscheduled for operator review.
- Schedule listeners now refresh after lifecycle reconciliation changes.
- Teleport overrides whose world names contain dots now round-trip through YAML without being split into nested paths.
- Paper/Folia startup now rejects missing, unparseable, or older-than-4.4.0 Worlds installations.
- Paper/Folia history and status output no longer labels the Worlds provider as Multiverse.
- Spigot command output, reset countdown broadcasts, and server-log messages now use native Bukkit delivery and remain visible.
- Paper/Folia reset and teleport GUI paths no longer synchronously wait on platform futures.

### Commands

- `/rwr status [id]` is the single command for current phase and next-schedule information.

### Release validation

- `mvn clean verify` passed across the complete reactor with **83 tests**: 59 core, 18 Spigot, and 6 Paper/Folia.
- Live-tested the release artifact on Folia 26.1.2 build 8, Java 25, and Worlds 4.4.0.
- Live-tested the same artifact on Paper 26.2 build 119, Java 25, and Worlds 4.4.0.
- Verified clean startup, status/history, admin GUI, teleport GUI, and entity-scheduled completion messages.
- Completed a supervised Worlds regeneration of `worlds_two`; independent verification passed, the same seed and a test world border were preserved, and the daily schedule was restored.
- Completed a supervised Paper regeneration of `worlds_rainforest` with a player initially inside; evacuation, the same seed, the default world border, independent verification, and schedule restoration all passed.
- No RWR exception, Folia thread-access violation, watchdog stall, or reset warning appeared during the test.
- No RWR error, warning, exception, or thread-access failure appeared during the Paper test.
- Purpur shares the validated Paper API path and is expected to be compatible, but was not separately exercised.

### Migration warning

v4 configuration is not loaded automatically by v5. Back up the server, install exactly one matching platform artifact, and migrate values into a fresh `config-version: 5` configuration. See [Operations & Migration](OPERATIONS_AND_MIGRATION.md).

## 4.2.1 — Legacy: Paper/Spigot 26.2 support and configuration persistence

**Status:** final legacy v4 maintenance release. It is not the predecessor configuration format of v5 and receives no v5 platform split or Folia architecture.

### Added / improved

- Paper/Spigot 26.2 compatibility.
- Internal cleanup without user-facing command changes.

### Fixed

- GUI and command configuration changes now persist across restart.
- Configuration changes are no longer lost after `/rwr reload`.
- Configuration reads and writes use the active Bukkit/Paper configuration instance.

### Legacy upgrade notes

1. Back up `plugins/ResourceWorldResetter/` and managed resource worlds.
2. Replace the existing legacy JAR with v4.2.1.
3. Restart the server.
4. Run one supervised `/rwr reset <id>` before enabling unattended resets.
5. Verify the phase and next schedule with `/rwr status`.

Users remaining on v4.2.0 should upgrade to v4.2.1. New installations and supported migrations should use v5.

## 4.2.0 — Legacy: Paper/Spigot 26.2 compatibility

- Added intended compatibility with the 26.2 server line.
- Included internal code-quality cleanup.
- Retained the v4 API, command, and configuration formats.

## 4.1.1 — Legacy: Paper 26.1 world-folder fix

- Corrected Paper dimension-backed world-folder resolution.
- Aligned disk preflight, reset deletion, and history snapshot paths.
- Added safety checks for multi-dimension save roots.

## 4.1.0 — Legacy: safety gates and operator tooling

- Added configurable TPS, player-count, and disk-space preflight gates.
- Added dry-run resets, reset history, improved status output, and multi-interval warnings.
- Expanded admin GUI, logging, and interrupted-reset recovery behavior.

## 4.0.0 — Legacy: unified command tree

- Introduced the unified `/rwr` command tree.
- Added phase-aware persisted reset state.
- Removed legacy command aliases.
