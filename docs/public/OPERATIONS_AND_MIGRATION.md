# Operations & Migration

This is the global public runbook for both ResourceWorldResetter 5 platform artifacts.

## Choose one platform

| Server | RWR artifact | Required world plugin | Data folder |
|---|---|---|---|
| Spigot / CraftBukkit | `RWR-Spigot-5.2.0-beta.1.jar` | Multiverse-Core 5.8.0+ | `plugins/ResourceWorldResetter/` |
| Paper / Purpur / Folia | `RWR-Paper-Folia-5.2.0-beta.1.jar` | Worlds by TheNextLvl 4.4.0+ | `plugins/ResourceWorldResetter-Paper-Folia/` |

Install exactly one RWR artifact. Do not install `rwr-core` separately. Paper/Folia does not use Multiverse-Core. Worlds 4.4.0 supports Minecraft 26.1.2 and 26.2, so it cannot be used on Paper/Folia 1.21.4.

Spigot/CraftBukkit requires Java 21+. Paper/Purpur/Folia requires Java 25+ and Minecraft 26.1.2+ because of Worlds 4.4.0. Current packages are `5.2.0-beta.1` pre-release builds.

## Installation

1. Stop the server.
2. Back up the server, the matching RWR data folder, and every managed resource world.
3. Install the required world-management plugin for the platform.
4. Place the matching RWR JAR in `plugins/` and remove any other RWR platform JAR.
5. Start the server and confirm both the world plugin and RWR enable successfully.
6. Review the generated `config.yml`, `managed-worlds.yml`, and `locales/en_US.yml` or use `/rwr gui`. Fresh Paper/Folia installs set `default-hub-world` to the server default world.
7. Run `/rwr status` and `/rwr history 10` before enabling unattended resets.

Avoid plugin hot-reload tools. Use a full server restart when replacing the JAR or changing platform builds.

## Locales

Bundled languages are `en_US`, `zh_CN`, `ja_JP`, and `ko_KR`. Set `locale` in `config.yml` to the filename without `.yml`.

On a clean data folder with the default `locale: en_US`, only `locales/en_US.yml` is extracted. Other bundled locale files remain inside the JAR until selected: change `locale`, then restart or run `/rwr reload` to write that language under `locales/`. Custom edits in on-disk locale files override the matching keys; omitted keys still fall back to the bundled English and selected-language defaults from the JAR.

## Routine operations

- Use `/rwr status` to inspect current reset phases and operational state.
- Use `/rwr status [id]` to verify the calculated schedule.
- Use `/rwr history 10` to inspect recent terminal and interrupted operations.
- Use `/rwr reload` after manual YAML edits. Reload refreshes configuration, locale, and update-checker settings together. Invalid configuration leaves the previous active configuration and schedules in place; an invalid locale keeps the previous valid messages active.
- Before a manual reset, ensure the evacuation destination is loaded, safe, and registered with the platform's world plugin.
- Use `/rwr reset <world-id>` or the dashboard confirmation screen to start a reset.

RWR evacuates players, delegates regeneration to the authoritative world plugin, then independently verifies registration, loaded state, identity, and safe spawn. Incoming RWR teleports are blocked from precheck through verification so players cannot teleport into an unsafe reset window.

## Interrupted or failed resets

RWR deliberately does not replay an ambiguous interrupted regeneration automatically.

In 5.2.0, reloads preserve active resets and their eventual outcomes. A second reset request for an active world returns busy. Ambiguous outcomes stay paused across reloads; startup restores pauses from retained history, currently limited to 100 entries. After checking the world manager, an explicit manual reset can resume scheduling. A timeout does not cancel or prove completion of the upstream operation.

1. Read the terminal failure type and safety classification in `/rwr status` and `/rwr history 10`.
2. Inspect the server log and `reset-history.json` in the RWR data folder.
3. Confirm the world exists, is loaded, and has the expected identity in Multiverse-Core or Worlds.
4. Confirm no players remain inside the resource world.
5. Retry only when the result is marked safe to retry. Review ambiguous failures manually first.

On Folia, a reset should remain in `EVACUATE` while entity-region teleport futures finish. That waiting is asynchronous and must not produce global-region watchdog stalls.

## Upgrade from 5.0 or 5.1

Keep `config-version: 5` and the same platform/provider. Replace the JAR while the server is stopped. On first successful startup, 5.2.0 imports existing `worlds` and `teleport.worlds` entries into the generated `managed-worlds.yml`; the original `config.yml` remains available as the migration source. Version 5.2.0 uses `locale: en_US` and extracts `locales/en_US.yml` into the platform's data folder by default; manually copy custom text from an older `messages.yml`. To use Simplified Chinese, Japanese, or Korean, set `locale` to `zh_CN`, `ja_JP`, or `ko_KR` and reload so that file is extracted if missing.

Use `/rwr reload` or the administration GUI reload to refresh configuration, locale, and update-checker settings, and `/rwr version` to show cached update status. An accepted configuration can remain applied even if the locale reload fails and retains its previous messages. Set `updates.enabled: false` to disable update checks. RWR never downloads or installs an update.

## Migration from v4.2.1 or earlier

Version 4.2.1 and earlier are **legacy v4 releases**. v5 does not automatically load a v4 configuration.

1. Stop the server and make a restorable backup.
2. Save the old v4 `config.yml`, messages, reset history, and any operational notes outside the plugin directory.
3. Choose the v5 artifact matching the server platform and install its required world plugin.
4. Move the legacy JAR out of `plugins/`; do not run v4 and v5 together.
5. Start v5 once to generate a fresh `config-version: 5` configuration.
6. Recreate each managed world entry using stable IDs and the exact world identity reported by the selected provider.
7. Copy user-facing names into each world's `display-name`.
8. Convert warning values to `warning-minutes`. Values below one minute are intentionally unsupported.
9. Recreate teleport permissions and visibility settings. Remove legacy teleport-entry display names; the managed world's display name is authoritative.
10. Recreate schedules and verify the configured timezone.
11. Validate evacuation destinations and perform a supervised manual reset before restoring unattended schedules.

Example `managed-worlds.yml` world and teleport entries:

```yaml
managed-worlds-version: 1

worlds:
  rainforest:
    multiverse-world: rainforest
    display-name: Rainforest
    warning-minutes: [30, 10, 5, 1]

teleport:
  worlds:
    rainforest:
      enabled: true
      permission: ""
```

The configuration field may retain a compatibility-oriented name such as `multiverse-world`, but Paper/Folia status and lifecycle operations refer to the Worlds provider and do not use Multiverse-Core.

## Switching server platforms

Moving between Spigot and Paper/Folia is not a JAR-only replacement because the authoritative world provider changes.

1. Back up all worlds and provider configuration.
2. Remove the old RWR artifact and its incompatible world-provider plugin.
3. Install the new platform's RWR artifact and required provider.
4. Import or register worlds using that provider's documented process.
5. Generate and manually validate the new RWR configuration.
6. Confirm world identities and safe destinations before running a reset.

Never assume a Multiverse-Core name and a Worlds namespaced key are interchangeable.

## Before reporting a problem

Include the complete `logs/latest.log`, exact server build, Java version, RWR artifact filename, world-plugin name/version, relevant configuration with secrets removed, and the command or GUI action that triggered the issue.
