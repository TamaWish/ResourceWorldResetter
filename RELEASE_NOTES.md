# Release notes

This file highlights user-facing updates for recent ResourceWorldResetter releases. Full technical history lives in [CHANGELOG.md](CHANGELOG.md).

## Version 5.2.0-beta.1 — pre-release

Current JARs ship as `5.2.0-beta.1` until live validation is complete. The public release will remain `5.2.0` once it is ready.

### Highlights

- GUI-managed resource-world definitions and per-world teleport overrides move from `config.yml` to `managed-worlds.yml`. Existing combined v5 configurations are imported automatically on startup.
- Server owners can select a server-wide message language with `locale` in `config.yml`. Bundled choices are `en_US`, `zh_CN`, `ja_JP`, and `ko_KR`; custom translations can be added under `locales/`.
- Commands, notifications, administration and teleport GUIs, status values, history labels, and teleport outcomes are localized. Missing keys fall back to bundled English from the JAR.
- `/rwr reload` and the administration GUI reload apply configuration, locale, and update-checker settings together without requiring a restart. If a locale cannot be loaded, RWR keeps the previous valid in-game messages active.
- Fresh installs extract only `locales/en_US.yml` by default. Set `locale` to another bundled language and reload or restart to extract that file under `locales/`.
- Fresh Paper/Folia installs set `default-hub-world` to the server default world.
- Manual resets no longer send duplicate completion or failure messages to the operator who started them.
- Incoming teleports are blocked earlier in the reset lifecycle and cannot race into an evacuating or regenerating world.

### Before replacing the JAR

The YAML layout has changed in this release. Replacing the JAR alone does **not** update YAML files that already exist in the plugin data folder, so an existing installation may not receive the new `locale` setting, `locales/` files, or `managed-worlds.yml` layout automatically.

Before upgrading, stop the server and make a restorable backup of the entire RWR plugin data folder. Then choose whichever migration approach you are comfortable with:

- Keep the existing folder, compare it with the new templates, and manually transfer your worlds, schedules, teleport settings, and customized messages into the new YAML layout. On first successful 5.2.0 startup, worlds and teleport overrides already stored in a combined v5 `config.yml` are imported into `managed-worlds.yml` automatically.
- Or, after confirming the backup is usable, remove the RWR plugin data folder and start the server once so the new JAR can generate fresh files. Manually copy only the settings and message text you still need from the backup.

Do not delete the data folder without a verified backup. If upgrading from `messages.yml`, copy customized text into the selected `locales/<locale>.yml`; the old message file is no longer loaded. After selecting a non-English locale, run `/rwr reload` once so that language file is written under `locales/` if it is not already present.

## Version 5.1.0 — 2026-09-02

**Headline:** Add-ons can now read reset state and react to the complete reset lifecycle without compromising reset reliability.

Version 5.1.0 introduced the stable integration service used by compatible add-ons. It also made resets, warnings, recovery, and shutdown more resilient when provider callbacks or third-party listeners fail.

### Highlights

- Add-ons can read immutable managed-world and reset-status snapshots through Bukkit's service registry.
- Integrations can react to scheduled warnings, cancel a pre-reset event, and observe the final reset outcome.
- Both platform JARs include the public API, so server owners do not install a separate API plugin.



### Improvements

- Player warnings and reset cleanup continue when a third-party event listener throws an exception.
- Configuration loading, interrupted-operation recovery, Folia callbacks, and shutdown handling reject unsafe or incomplete work more consistently.



### Fixes

- Exceptional reset completion now releases the world lock, records the terminal result, notifies remaining listeners, and settles the schedule.



### Upgrade notes

Stop the server, back up the RWR data folder and managed worlds, replace the existing platform JAR, and perform a full restart. Version 5.1.0 keeps the version 5 configuration format; no configuration migration is required.

Install exactly one matching artifact:

- Spigot/CraftBukkit requires Java 21+ and Multiverse-Core 5.8.0+.
- Paper/Purpur/Folia requires Java 25+ and Worlds by TheNextLvl 4.4.0+.



### Links

- [Changelog entry](CHANGELOG.md#510---2026-09-02)
- [Operations and migration guide](docs/public/OPERATIONS_AND_MIGRATION.md)
- [Downloads](https://github.com/TamaWish/ResourceWorldResetter/releases)
- [GitHub release](https://github.com/TamaWish/ResourceWorldResetter/releases/tag/v5.1.0)



## Version 5.0.0 — 2026-08-28

**Headline:** ResourceWorldResetter now provides dedicated, safe reset engines for Spigot and modern Paper/Folia servers.

Version 5 replaced the legacy shared JAR with two platform-specific builds. Both builds schedule resets, evacuate players, delegate regeneration to the installed world manager, verify the result, and retain history for operator review.

### Highlights

- Choose a dedicated Spigot/CraftBukkit build powered by Multiverse-Core or a Paper/Purpur/Folia build powered by Worlds.
- Schedule daily, weekly, monthly, or interval resets in an IANA time zone.
- Warn and evacuate players before regeneration, then verify and record the outcome.
- Configure managed worlds through an administration GUI and offer players a permission-aware teleport menu.
- Review active phases, future schedules, completed resets, failures, and interrupted operations.
- Preserve seeds, gamerules, level settings, and world borders according to each managed world's configuration.



### Improvements

- Folia evacuation and player-facing callbacks run asynchronously on the appropriate entity schedulers.
- Safe failures follow the configured retry delay and limit, while ambiguous or interrupted resets pause for operator review.
- World display names are consistent across messages, logs, notifications, status, history, and GUIs.



### Fixes

- Paper resets no longer deadlock while waiting for asynchronous player teleports.
- Folia resets no longer block the global region while waiting for entity-region teleport futures.
- Teleport overrides containing dots are preserved as complete YAML keys.
- Paper/Folia startup rejects missing, unparseable, or older-than-4.4.0 Worlds installations.
- Spigot command output, countdown broadcasts, and log messages remain visible through native Bukkit delivery.



### Breaking changes

- The legacy shared JAR was replaced by `RWR-Spigot-5.0.0.jar` and `RWR-Paper-Folia-5.0.0.jar`. Install exactly one; do not install `rwr-core` separately.
- Version 4 configuration is not loaded automatically. Recreate the configuration from the fresh `config-version: 5` template.
- Paper, Purpur, and Folia use Worlds by TheNextLvl instead of Multiverse-Core.
- Warning intervals use whole-minute `warning-minutes`; sub-minute legacy warnings are unsupported.



### Upgrade notes

Back up the plugin data folder and every managed world. Remove the version 4 JAR, install the correct world provider and version 5 platform JAR, start once to generate a fresh configuration, and migrate world identities, display names, warning minutes, teleport settings, and schedules. Supervise the first reset and confirm it with `/rwr status <id>`.

Follow the full [Operations and Migration guide](docs/public/OPERATIONS_AND_MIGRATION.md) before upgrading from version 4.

### Links

- [Changelog entry](CHANGELOG.md#500---2026-08-28)
- [Operations and migration guide](docs/public/OPERATIONS_AND_MIGRATION.md)
- [Downloads](https://github.com/TamaWish/ResourceWorldResetter/releases)
- [GitHub release](https://github.com/TamaWish/ResourceWorldResetter/releases/tag/v5.0.0)
