# Operations & Migration

This is the global public runbook for both ResourceWorldResetter 5 platform artifacts.

## Choose one platform

| Server | RWR artifact | Required world plugin | Data folder |
|---|---|---|---|
| Spigot / CraftBukkit | `RWR-Spigot-5.0.0.jar` | Multiverse-Core 5.8.0+ | `plugins/ResourceWorldResetter/` |
| Paper / Purpur / Folia | `RWR-Paper-Folia-5.0.0.jar` | Worlds by TheNextLvl 4.4.0+ | `plugins/ResourceWorldResetter-Paper-Folia/` |

Install exactly one RWR artifact. Do not install `rwr-core` separately. Paper/Folia does not use Multiverse-Core.

## Installation

1. Stop the server.
2. Back up the server, the matching RWR data folder, and every managed resource world.
3. Install the required world-management plugin for the platform.
4. Place the matching RWR JAR in `plugins/` and remove any other RWR platform JAR.
5. Start the server and confirm both the world plugin and RWR enable successfully.
6. Review the generated `config.yml` and `messages.yml` or use `/rwr gui`.
7. Run `/rwr status`, `/rwr next`, and `/rwr history 10` before enabling unattended resets.

Avoid plugin hot-reload tools. Use a full server restart when replacing the JAR or changing platform builds.

## Routine operations

- Use `/rwr status` to inspect current reset phases and operational state.
- Use `/rwr next` to verify the calculated schedule.
- Use `/rwr history 10` to inspect recent terminal and interrupted operations.
- Use `/rwr reload` after manual YAML edits. Reload is transactional: invalid configuration leaves the previous active configuration and schedules in place.
- Before a manual reset, ensure the evacuation destination is loaded, safe, and registered with the platform's world plugin.
- Use `/rwr reset <world-id>` or the dashboard confirmation screen to start a reset.

RWR evacuates players, delegates regeneration to the authoritative world plugin, then independently verifies registration, loaded state, identity, and safe spawn. Incoming RWR teleports are blocked during the unsafe reset phases.

## Interrupted or failed resets

RWR deliberately does not replay an ambiguous interrupted regeneration automatically.

1. Read the terminal failure type and safety classification in `/rwr status` and `/rwr history 10`.
2. Inspect the server log and `reset-history.json` in the RWR data folder.
3. Confirm the world exists, is loaded, and has the expected identity in Multiverse-Core or Worlds.
4. Confirm no players remain inside the resource world.
5. Retry only when the result is marked safe to retry. Review ambiguous failures manually first.

On Folia, a reset should remain in `EVACUATE` while entity-region teleport futures finish. That waiting is asynchronous and must not produce global-region watchdog stalls.

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

Example v5 world and teleport entries:

```yaml
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
