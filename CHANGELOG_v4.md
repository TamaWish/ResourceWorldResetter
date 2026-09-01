# Changelog (legacy v4)

Historical **v4** (and earlier) release notes. This line is not the predecessor configuration format of v5 and does not receive the v5 platform split or Folia architecture.

Current v5 history: [CHANGELOG.md](CHANGELOG.md)

## 4.2.1 — Legacy: Paper/Spigot 26.2 support and configuration persistence

**Status:** final legacy v4 maintenance release.

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
