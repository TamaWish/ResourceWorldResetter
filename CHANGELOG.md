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
- Paper/Folia history and status output no longer labels the Worlds provider as Multiverse.
- Spigot command output, reset countdown broadcasts, and server-log messages now use native Bukkit delivery and remain visible.
- Paper/Folia reset and teleport GUI paths no longer synchronously wait on platform futures.

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
4. Run `/rwr reset dry-run` before the next scheduled reset.
5. Verify `/rwr status` and `/rwr next`.

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
