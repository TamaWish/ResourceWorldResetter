# ResourceWorldResetter

Safe, scheduled resource-world regeneration for Spigot, CraftBukkit, Paper, Purpur, and Folia servers.

<div align="center">

![RWR Banner](https://files.catbox.moe/lhrg2i.png)

[![GitHub Release](https://img.shields.io/github/v/release/TamaWish/ResourceWorldResetter?include_prereleases&sort=date&display_name=tag&style=plastic&logo=github&logoColor=white&label=Release&labelColor=1e293b&color=00b4d8)](https://github.com/TamaWish/ResourceWorldResetter/releases)
[![CI](https://img.shields.io/github/actions/workflow/status/TamaWish/ResourceWorldResetter/ci.yml?branch=main&style=plastic&logo=githubactions&logoColor=white&label=CI&labelColor=1e293b&color=06d6a0)](https://github.com/TamaWish/ResourceWorldResetter/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21%2B%20%7C%20Paper--Folia%2025%2B-f59e0b?style=plastic&logo=openjdk&logoColor=white&labelColor=1e293b)](https://www.java.com)
[![License](https://img.shields.io/github/license/TamaWish/ResourceWorldResetter?style=plastic&logo=github&logoColor=white&label=License&labelColor=1e293b&color=64748b)](https://github.com/TamaWish/ResourceWorldResetter/blob/main/LICENSE)
[![Discord](https://img.shields.io/discord/1501244767680467096?style=plastic&logo=discord&logoColor=white&label=Discord&labelColor=1e293b&color=5865f2)](https://discord.gg/kbKZzxDETU)
<br>
[![Supported Version](https://img.shields.io/modrinth/game-versions/PjsJlPJ9?style=plastic&logo=modrinth&logoColor=white&label=Supported%20Version&labelColor=1e293b&color=00b4d8)](https://modrinth.com/plugin/resourceworldresetter)
<br>
[![SpigotMC](https://img.shields.io/spiget/downloads/119878?style=plastic&logo=spigotmc&logoColor=white&label=SpigotMC&labelColor=1e293b&color=e9a825)](https://www.spigotmc.org/resources/resourceworldresetter-1-21-4-26-x.119878/)
[![Modrinth](https://img.shields.io/modrinth/dt/PjsJlPJ9?style=plastic&logo=modrinth&logoColor=white&label=Modrinth&labelColor=1e293b&color=06d6a0)](https://modrinth.com/plugin/resourceworldresetter)
[![Hangar](https://img.shields.io/hangar/dt/ResourceWorldResetter?style=plastic&logo=paper&logoColor=white&label=Hangar&labelColor=1e293b&color=38bdf8)](https://hangar.papermc.io/Lozaine/ResourceWorldResetter)
[![CurseForge](https://img.shields.io/curseforge/dt/1110835?style=plastic&logo=curseforge&logoColor=white&label=CurseForge&labelColor=1e293b&color=f97316)](https://www.curseforge.com/minecraft/bukkit-plugins/resourceworldresetter)

</div>

## Table of contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
- [Configuration](#configuration)
- [Commands and permissions](#commands-and-permissions)
- [Localization](#localization)
- [Metrics](#metrics)
- [Add-ons and API](#add-ons-and-api)
- [Development](#development)
- [Contributing](#contributing)
- [License](#license)

## Features

- Daily, weekly, monthly, and interval reset schedules with IANA time zones.
- Guarded evacuation, provider regeneration, result verification, and per-world locking.
- Configurable countdown warnings and completion broadcasts.
- Persistent reset history and conservative interrupted-operation recovery.
- Administration and player teleport GUIs.
- Transactional configuration and locale reloads with English fallback.
- MiniMessage localization in English, Simplified Chinese, Japanese, and Korean.
- Asynchronous update checks that report releases without downloading them.

## Requirements

Install exactly one platform JAR and its matching world provider.

| Platform | Java | Minecraft | Required world provider |
| --- | --- | --- | --- |
| Spigot / CraftBukkit | 21+ | 1.21.4+ | [Multiverse-Core](https://modrinth.com/plugin/multiverse-core) 5.8.0+ |
| Paper / Purpur / Folia | 25+ | 26.1.2+ | [Worlds](https://modrinth.com/plugin/worlds-1) 4.4.0+ |

The Spigot build uses the Bukkit API version `1.21`. Worlds 4.4.0 targets Paper/Folia 26.1.2 and 26.2, so the Paper/Folia build requires Minecraft 26.1.2 or newer.

## Installation

1. Back up the server and every world that RWR will manage.
2. Install the world provider listed for your platform.
3. Download the matching JAR from [GitHub Releases](https://github.com/TamaWish/ResourceWorldResetter/releases).
4. Place the JAR in the server's `plugins/` directory.
5. Restart the server. Do not use a hot-reload plugin.
6. Configure worlds with `/rwr gui`; generated world definitions are stored in `managed-worlds.yml`.
7. Run one supervised `/rwr reset <id>` before enabling unattended resets.

Use `RWR-Spigot-5.2.0-beta.1.jar` for Spigot/CraftBukkit or `RWR-Paper-Folia-5.2.0-beta.1.jar` for Paper/Purpur/Folia. Do not install both. The shared core and API adapter are bundled and are not separate server plugins. Current builds are pre-release until 5.2.0 is validated for public release.

> [!IMPORTANT]
> RWR 5 requires `config-version: 5`. Version 4 configuration is not migrated automatically. Follow [Operations and Migration](docs/public/OPERATIONS_AND_MIGRATION.md).

## Usage

Open the administration GUI:

```text
/rwr gui
```

After defining a managed world, inspect its schedule and run a supervised reset:

```text
/rwr status resource
/rwr reset resource
/rwr history 10
```

A reset evacuates players, asks the installed world provider to regenerate the world, verifies the result, and records the terminal outcome. Ambiguous failures pause automatic resets until an operator reviews the world and configuration.

## Configuration

The generated configuration is stored in:

- Spigot/CraftBukkit: `plugins/ResourceWorldResetter/config.yml` and `managed-worlds.yml`
- Paper/Purpur/Folia: `plugins/ResourceWorldResetter-Paper-Folia/config.yml` and `managed-worlds.yml`

| Key | Required | Default | Description |
| --- | --- | --- | --- |
| `config-version` | Yes | `5` | Configuration schema version. |
| `locale` | No | `en_US` | Locale filename without `.yml`. |
| `timezone` | No | `Asia/Kuala_Lumpur` | IANA time zone used by schedules. |
| `default-hub-world` | Yes | server default world on first install | Default evacuation destination. |
| `updates.enabled` | No | `true` | Check GitHub Releases at startup. |
| `reset-policy.max-safe-retries` | No | `2` | Automatic retries after a safe failure. |
| `managed-worlds.yml: worlds.<id>.managed` | Yes for resets | No implicit default | Must be `true` before RWR regenerates a world. |
| `managed-worlds.yml: worlds.<id>.schedule.type` | Yes for schedules | — | `DAILY`, `WEEKLY`, `MONTHLY`, or `INTERVAL`. |
| `managed-worlds.yml: worlds.<id>.regeneration.seed-policy` | Yes for resets | — | `SAME`, `FIXED`, or `RANDOM`. |
| `teleport.auto-discover` | No | `true` | Discover provider worlds for the teleport menu. |

Minimal daily schedule:

```yaml
worlds:
  resource:
    multiverse-world: resource
    display-name: "Resource World"
    enabled: true
    managed: true
    schedule:
      type: DAILY
      time: "03:00"
    warning-minutes: [30, 10, 5, 1]
    regeneration:
      seed-policy: RANDOM
      keep-world-config: true
      keep-gamerules: true
      keep-world-border: true
    evacuation:
      enabled: true
      destination: world
```

On Paper/Folia, `multiverse-world` stores a Worlds key such as `worlds:resource`. See the bundled platform `config.yml` files and the [operator configuration guide](website/src/content/docs/operator/configuration.md) for the complete schema.

## Commands and permissions

| Command | Permission | Purpose |
| --- | --- | --- |
| `/rwr help` | Relevant command nodes | Show available commands. |
| `/rwr status [id]` | `rwr.status` | Show reset and schedule status. |
| `/rwr gui` | `rwr.admin` | Open the administration GUI. |
| `/rwr tp` | `rwr.tp` | Open the player teleport menu. |
| `/rwr reset <id>` | `rwr.reset` | Start a guarded reset. |
| `/rwr history [count]` | `rwr.history` | Show recent reset history. |
| `/rwr reload` | `rwr.reload` | Reload configuration, locale, and update-checker settings. |
| `/rwr version` | `rwr.admin` | Show installed and latest versions. |

Administrative permissions default to server operators. `rwr.tp` defaults to all players. `rwr.teleport.world.*` lets operators bypass destination-specific permissions.

## Localization

Bundled locales are `en_US`, `zh_CN`, `ja_JP`, and `ko_KR`. Select one in `config.yml`:

```yaml
locale: ja_JP
```

On a fresh install with the default `locale: en_US`, only `locales/en_US.yml` is written to the plugin data folder. Other bundled languages stay inside the JAR until you select them: set `locale` to `zh_CN`, `ja_JP`, or `ko_KR`, then restart or run `/rwr reload` to extract that file under `locales/`.

Messages use MiniMessage. Keep placeholders such as `<world>`, `<latest>`, and `<permission>` unchanged. Legacy ampersand color codes and `%placeholder%` syntax are accepted. Missing keys fall back to bundled English from the JAR (including keys omitted from older on-disk locale files). An invalid locale reload keeps the previous valid locale active. Both `/rwr reload` and the administration GUI reload refresh configuration, locale, and update-checker settings together.

## Metrics

RWR uses [bStats](https://bstats.org/) for anonymous server usage statistics. The Spigot build uses plugin ID [31502](https://bstats.org/plugin/bukkit/ResourceWorldResetter/31502), and the Paper/Folia build uses plugin ID [33605](https://bstats.org/plugin/bukkit/ResourceWorldResetter-Paper-Folia/33605). Disable collection globally in `plugins/bStats/config.yml` by setting `enabled: false`.

## Add-ons and API

- [RWR-PlaceholderAPI](https://github.com/TamaWish/RWR-PlaceholderAPI) provides machine-oriented placeholders.
- [RWR-Discord Webhook](https://github.com/TamaWish/RWR-Discord-Webhook) publishes reset warnings and outcomes.
- [RWR-Prometheus](https://github.com/TamaWish/RWR-Prometheus) exports reset lifecycle metrics.
- [RWR API](https://github.com/TamaWish/RWR-API) provides read-only snapshots and reset lifecycle events.

Integration authors can compile against `io.github.tamawish:rwr-api:5.1.2` with `provided` scope. See [Integration Development](docs/public/DEVELOPMENT.md).

## Project structure

| Path | Purpose |
| --- | --- |
| `rwr-core/` | Platform-independent configuration, scheduling, reset, history, and teleport policy. |
| `rwr-bukkit-api-adapter/` | Bukkit service registration, API snapshots, and events. |
| `RWR-Spigot/` | Spigot/CraftBukkit and Multiverse-Core integration. |
| `RWR-Paper-Folia/` | Paper/Purpur/Folia and Worlds integration. |
| `website/` | Astro/Starlight documentation site. |

## Development

The full Maven reactor requires JDK 25 and Maven 3.9+.

```bash
git clone https://github.com/TamaWish/ResourceWorldResetter.git
cd ResourceWorldResetter
mvn clean verify
```

Build the two production artifacts:

```bash
mvn -pl RWR-Spigot,RWR-Paper-Folia -am clean package
```

Run focused core tests:

```bash
mvn -pl rwr-core test
```

Check Java formatting:

```bash
mvn spotless:check
```

The resulting server JARs are written under `RWR-Spigot/target/` and `RWR-Paper-Folia/target/`.

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting a change. Bug reports and pull requests belong in the [GitHub repository](https://github.com/TamaWish/ResourceWorldResetter/issues).

## Documentation and support

- [Operator wiki](https://tamawish.github.io/ResourceWorldResetter/wiki.html)
- [Operations and Migration](docs/public/OPERATIONS_AND_MIGRATION.md)
- [Release notes](RELEASE_NOTES.md)
- [Changelog](CHANGELOG.md)
- [Discord](https://discord.gg/kbKZzxDETU)

## License

ResourceWorldResetter is licensed under the [BSD 3-Clause License](LICENSE).

ResourceWorldResetter is developed and maintained solely by TamaWish.
