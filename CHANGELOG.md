# Changelog

All notable changes to this project are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

Pre-release packages currently build as `5.2.0-beta.1`.

### Added

- Added `/rwr version` under `rwr.admin` with a cached, asynchronous GitHub Releases check.
- Added configurable update checks with startup, online-administrator, and administrator-join notifications; RWR never downloads or installs updates.
- Added server-wide locale selection from `locales/<locale>.yml` with bundled English, Simplified Chinese, Japanese, and Korean translations covering commands, notifications, GUIs, teleport outcomes, history values, and failure labels.
- Added English fallback, missing-key diagnostics, and retention of the previous valid locale when a locale reload fails.
- Added locale-aware chat cancel keywords for GUI text input.
- Added a locale key-parity test so bundled translations stay aligned with English.

### Changed

- Moved GUI-managed resource-world definitions and per-world teleport overrides from `config.yml` to `managed-worlds.yml`; existing combined v5 configurations are imported automatically.
- Changed message customization from `messages.yml` to locale files under `locales/`; copy existing custom messages into the selected locale when upgrading.
- Fresh installs extract only `locales/en_US.yml` by default; other bundled locales are written to disk when selected in `config.yml` and the plugin reloads or restarts.
- Fresh Paper/Folia installs set `default-hub-world` to the server default world instead of always writing `world`.
- `/rwr reload` and the administration GUI reload now refresh configuration, locale, and update-checker settings together.
- Manual `/rwr reset` and GUI resets no longer duplicate the global completion or failure broadcasts already shown to the initiating operator.
- Incoming RWR teleports are blocked from `PRECHECK` onward and admitted atomically against reset locks so teleports cannot race into an evacuating or regenerating world.
- Reset-history GUI entries use newest-first order and the configured timezone.
- Changed world protection, evacuation checks, duplicate detection, and teleport overrides to use canonical provider identities, including namespaced Worlds keys.
- Changed automatic scheduling to preserve active resets and operator-review pauses across configuration reloads and server restarts.
- Improved teleport-menu catalog construction while continuing to revalidate permissions, availability, and reset state when a player clicks.

### Fixed

- Fixed world-management GUI saves removing `locale`, update-checker settings, comments, and future extension keys by isolating generated world settings from `config.yml`.
- Fixed concurrent manual reset requests or reloads displacing an active per-world reset.
- Fixed immediate asynchronous regeneration exceptions being classified as safe to retry after the provider may have started work.
- Fixed synchronous third-party reset-event failures interfering with reset completion and lock release.
- Fixed Folia reset-command scheduling, namespaced Worlds lookup, and world-deletion matching.
- Fixed locale rendering racing with locale reloads and restored bundled English defaults when user locale files omit keys.
- Fixed stale on-disk locale files omitting newer keys by merging bundled JAR defaults before applying user overrides.
- Fixed hardcoded English remaining in teleport outcomes, admin/player GUI labels, warning durations, status phases, and history result labels.

## [5.1.0] - 2026-09-02

### Added

- Added a stable read-only Bukkit service exposing immutable managed-world and reset-status snapshots to add-ons.
- Added scheduled-warning, cancellable pre-reset, and terminal post-reset integration events across both platform builds.
- Bundled the public API in each platform JAR so server owners do not install a separate API plugin.

### Changed

- Isolated third-party lifecycle and warning-listener failures so they no longer suppress player warnings or reset cleanup.
- Hardened bounded configuration and state loading, interrupted-operation recovery, Folia callback handling, and plugin shutdown.

### Fixed

- Fixed exceptional reset completion retaining locks, losing terminal history, skipping listener notification, or leaving schedules unsettled.

## [5.0.0] - 2026-08-28

### Breaking

- Replaced the single legacy JAR with separate Spigot/CraftBukkit and Paper/Purpur/Folia artifacts.
- Stopped loading version 4 configuration automatically; migrate settings into a fresh `config-version: 5` file before enabling resets.

### Added

- Added `RWR-Spigot-5.0.0.jar` for Spigot/CraftBukkit with Multiverse-Core 5.8.0 or newer.
- Added `RWR-Paper-Folia-5.0.0.jar` for Paper, Purpur, and Folia with Worlds by TheNextLvl 4.4.0 or newer.
- Added daily, weekly, monthly, and interval schedules with time-zone-aware next-run calculations.
- Added whole-minute warning broadcasts through `warning-minutes`.
- Added guarded reset phases for precheck, evacuation, regeneration, verification, completion, failure, and interrupted recovery.
- Added persistent reset history and active-operation journaling without automatically replaying ambiguous interrupted resets.
- Added administration screens for worlds, schedules, global settings, teleport destinations, history, and reset confirmation.
- Added a paginated player teleport menu with destination permissions, player counts, locked visibility, and reset-phase protection.
- Added cancellable pre-reset and terminal post-reset API events.
- Added MiniMessage-based message customization.
- Added bStats usage metrics to both platform artifacts; opt out with `enabled: false` in `plugins/bStats/config.yml`.

### Changed

- Delegated authoritative world regeneration to Multiverse-Core on Spigot/CraftBukkit and Worlds on Paper/Purpur/Folia instead of owning a direct unload/delete/create pipeline.
- Changed Paper, Purpur, and Folia integration from Multiverse-Core to Worlds by TheNextLvl.
- Changed chat, logs, notifications, status, history, and GUIs to use one configured `display-name`.
- Changed warning configuration to whole-minute `warning-minutes`; sub-minute legacy warnings are unsupported.
- Changed safe automatic failures to use the configured retry delay and retry limit while leaving ambiguous and interrupted outcomes paused for operator review.

### Removed

- Removed automatic loading of version 4 configuration.
- Removed support for running one shared platform JAR across both Multiverse-Core and Worlds providers.

### Fixed

- Fixed Paper reset deadlocks caused by waiting for asynchronous teleports on the primary scheduler.
- Fixed Folia watchdog stalls caused by blocking the global region while waiting for entity-region teleport futures.
- Fixed Folia evacuation so regeneration starts only after asynchronous teleports finish and remaining players are checked.
- Fixed Paper/Folia GUI, teleport, command-completion, and administrator-notification callbacks to run on the owning entity scheduler.
- Fixed Worlds regeneration to apply same, fixed, and random seed policies and preserve configured level settings, gamerules, and world borders.
- Fixed schedule listeners not refreshing after lifecycle reconciliation.
- Fixed teleport overrides containing dots being split into nested YAML paths.
- Fixed Paper/Folia startup accepting missing, unparseable, or older-than-4.4.0 Worlds installations.
- Fixed Paper/Folia status and history identifying the Worlds provider as Multiverse.
- Fixed Spigot command output, countdown broadcasts, and server-log messages being hidden by incompatible message delivery.

## [4.2.1]

### Fixed

- Fixed GUI and command configuration changes being lost after restart or `/rwr reload`.
- Fixed configuration reads and writes using an inactive Bukkit/Paper configuration instance.

## [4.2.0]

### Changed

- Added compatibility with the 26.2 server line while retaining the version 4 API, commands, and configuration format.

## [4.1.1]

### Fixed

- Fixed Paper dimension-backed world-folder resolution used by disk preflight, reset deletion, and history snapshots.
- Added safety checks for save roots containing multiple dimensions.

## [4.1.0]

### Added

- Added configurable TPS, player-count, and disk-space preflight gates.
- Added dry-run resets, reset history, expanded status output, and multiple warning intervals.
- Added administration GUI controls, improved reset logging, and interrupted-reset recovery.

## [4.0.0]

### Breaking

- Replaced the previous command layout with the unified `/rwr` command tree and removed legacy command aliases.

### Added

- Added the unified `/rwr` command tree.
- Added phase-aware persisted reset state.

### Removed

- Removed legacy command aliases; use the corresponding `/rwr` subcommands.

[Unreleased]: https://github.com/TamaWish/ResourceWorldResetter/compare/v5.1.0...HEAD
[5.1.0]: https://github.com/TamaWish/ResourceWorldResetter/compare/v5.0.0...v5.1.0
[5.0.0]: https://github.com/TamaWish/ResourceWorldResetter/releases/tag/v5.0.0
