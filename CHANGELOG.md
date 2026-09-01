# Changelog

Public **v5** release history for ResourceWorldResetter. All **5.x.x** versions stay in this file. When v6 begins, start `CHANGELOG_v6.md`.

Legacy v4 history: [CHANGELOG_v4.md](CHANGELOG_v4.md)

## 5.1.0 — Public API integration (2026-09-02)

- Added the stable read-only integration contract and bundled API 5.1.2 warning-event addition.
- Added immutable managed-world and reset-status snapshots through Bukkit's `ServicesManager`.
- Unified scheduled-warning, pre-reset, and post-reset events across both platform JARs.
- Added one public warning event alongside each configured player warning that passes scheduler safety checks.
- Added provider-neutral failure mappings across both platform JARs.
- Kept the API embedded unrelocated so server owners do not install a separate API JAR.
- Scoped 5.1.0 to API integration and warning events. Commands, locale files, fallback rules, and
  complete localization are planned for 5.2.0.

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

v4 configuration is not loaded automatically by v5. Back up the server, install exactly one matching platform artifact, and migrate values into a fresh `config-version: 5` configuration. See [Operations & Migration](docs/public/OPERATIONS_AND_MIGRATION.md).
