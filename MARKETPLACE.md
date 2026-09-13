<!--
  Canonical marketplace paste source. Keep in sync with README.md consumer sections.
  Channel files: MODRINTH_DESCRIPTION.md, HANGAR_DESCRIPTION.md,
  CURSEFORGE_DESCRIPTION.md, SPIGOT_DESCRIPTION.bbcode
-->

### Markdown (Modrinth / CurseForge / Hangar)

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

## Features

- Daily, weekly, monthly, and interval reset schedules with IANA time zones.
- Guarded evacuation to local worlds, the default overworld, Velocity/BungeeCord servers, or registered plugin destinations, with departure verification and per-world locking.
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

Use `RWR-Spigot-5.2.0.jar` for Spigot/CraftBukkit or `RWR-Paper-Folia-5.2.0.jar` for Paper/Purpur/Folia. Do not install both. The shared core and API adapter are bundled and are not separate server plugins.

> [!IMPORTANT]
> RWR 5 requires `config-version: 5`. Version 4 configuration is not migrated automatically. Follow [Operations and Migration](https://github.com/TamaWish/ResourceWorldResetter/blob/main/docs/public/OPERATIONS_AND_MIGRATION.md).

## Usage

```text
/rwr gui
/rwr status resource
/rwr reset resource
/rwr history 10
```

A reset evacuates players, asks the installed world provider to regenerate the world, verifies the result, and records the terminal outcome. Ambiguous failures pause automatic resets until an operator reviews the world and configuration.

## Configuration

- Spigot/CraftBukkit: `plugins/ResourceWorldResetter/config.yml` and `managed-worlds.yml`
- Paper/Purpur/Folia: `plugins/ResourceWorldResetter-Paper-Folia/config.yml` and `managed-worlds.yml`

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
      timeout-seconds: 30
      destination:
        type: local-world
        target: world
```

On Paper/Folia, `multiverse-world` stores a Worlds key such as `worlds:resource`.

### Evacuation destinations (5.2.0)

Use `/rwr gui` → world → evacuation destination to select **Default overworld**, **Local world**, **Proxy server**, or **Plugin destination**. Local lists mark the reset world and unloaded worlds unavailable. Each list also offers manual input. Global settings provide defaults for new world configurations; existing overrides remain explicit.

Set `destination.type` to `default-world`, `local-world`, `proxy-server`, or `registered-provider`. `destination.target` is the world name/key, proxy backend name (for example `hub`), or registered provider ID. Add proxy menu choices with `proxy-servers: [hub, lobby]` in `config.yml`. The global `evacuation` section uses the same fields as per-world settings.

Transfers have a configurable 1–120 second timeout (default 30). RWR checks that players have left before regenerating; sending a proxy message alone is insufficient. Velocity needs `bungee-plugin-message-channel = true`. Legacy scalar `destination: world` values remain accepted and save in the structured format. No separate proxy plugin or arbitrary command destination is required.

#### First-time Velocity network setup

`lobby` is a separate running Paper/Folia backend server, not a world created by Worlds or Multiverse. A minimal setup runs Velocity on `25565`, the resource backend on `25566`, and a separate lobby backend on `25567`:

```toml
# velocity.toml
bind = "0.0.0.0:25565"
online-mode = true
player-info-forwarding-mode = "modern"
forwarding-secret-file = "forwarding.secret"

[servers]
resource = "127.0.0.1:25566"
lobby = "127.0.0.1:25567"
try = ["resource", "lobby"]

[advanced]
bungee-plugin-message-channel = true
```

On **both backends**, set `online-mode=false` and the appropriate unique port in `server.properties`, keep `settings.bungeecord: false` in `spigot.yml`, and configure `config/paper-global.yml`:

```yaml
proxies:
  velocity:
    enabled: true
    online-mode: true
    secret: "COPY_THE_EXACT_CONTENTS_OF_VELOCITY_FORWARDING.SECRET"
```

Modern forwarding must be enabled with the same secret on every backend, including the lobby. If `enabled` remains `false`, Velocity rejects the transfer with “Your server did not send a forwarding request to the proxy.” Start the lobby, resource backend, and Velocity in that order, wait for each backend to report `Done`, and connect players only to Velocity on port `25565`.

Set RWR's `target: lobby` to exactly match the `lobby` key under `[servers]`, and add `proxy-servers: [lobby]` for the GUI. When everything runs on one machine, keep backend ports bound to `127.0.0.1` and expose only Velocity's port.

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

Administrative permissions default to server operators. `rwr.tp` defaults to all players. `rwr.teleport.world.*` lets operators bypass destination-specific permissions. Leave a destination `permission` blank to make it public to anyone with `rwr.tp`.

## Localization

Bundled locales are `en_US`, `zh_CN`, `ja_JP`, and `ko_KR`. Select one with `locale` in `config.yml`. Fresh installs extract only `locales/en_US.yml` by default; other bundled languages are extracted when selected, then restarted or reloaded. Missing keys fall back to bundled English.

## Metrics

RWR uses [bStats](https://bstats.org/) for anonymous server usage statistics. The Spigot build uses plugin ID [31502](https://bstats.org/plugin/bukkit/ResourceWorldResetter/31502), and the Paper/Folia build uses plugin ID [33605](https://bstats.org/plugin/bukkit/ResourceWorldResetter-Paper-Folia/33605). Disable collection globally in `plugins/bStats/config.yml` by setting `enabled: false`.

Messages use MiniMessage. The Spigot/CraftBukkit artifact bundles and isolates `adventure-platform-bukkit` for component delivery; Paper/Purpur/Folia uses the server's native Adventure API. Legacy ampersand colors and `%placeholder%` syntax remain accepted.

## Add-ons and API

- [RWR-PlaceholderAPI](https://github.com/TamaWish/RWR-PlaceholderAPI) provides machine-oriented placeholders.
- [RWR-Discord Webhook](https://github.com/TamaWish/RWR-Discord-Webhook) publishes reset warnings and outcomes.
- [RWR-Prometheus](https://github.com/TamaWish/RWR-Prometheus) exports reset lifecycle metrics.
- [RWR API](https://github.com/TamaWish/RWR-API) provides read-only snapshots and reset lifecycle events.

## Links

- [Downloads and source](https://github.com/TamaWish/ResourceWorldResetter)
- [Operator wiki](https://tamawish.github.io/ResourceWorldResetter/wiki.html)
- [Operations and Migration](https://github.com/TamaWish/ResourceWorldResetter/blob/main/docs/public/OPERATIONS_AND_MIGRATION.md)
- [Release notes](https://github.com/TamaWish/ResourceWorldResetter/blob/main/RELEASE_NOTES.md)
- [Changelog](https://github.com/TamaWish/ResourceWorldResetter/blob/main/CHANGELOG.md)
- [Discord](https://discord.gg/kbKZzxDETU)
- [Issue tracker](https://github.com/TamaWish/ResourceWorldResetter/issues)

ResourceWorldResetter is licensed under the BSD 3-Clause License. Developed and maintained by TamaWish.


### Destination discovery

Opening the proxy evacuation selector requests `GetServers` through the connected administrator and immediately shows cached, configured, and saved targets. The matching open selector refreshes after a response while keeping its page and context. Current selections appear first. Missing or duplicate plugin providers are disabled; proxy targets absent from the latest response are unavailable (unverified before the first response). Custom/manual entry remains available, and saved global or managed-world targets remain visible on reopening.

Discovery is an in-memory cache for the plugin lifetime and never rewrites `config.yml`; `proxy-servers` supplies stable menu entries. If discovery times out, existing entries remain visible. It requires a connected player, a responding compatible proxy, and on Velocity `bungee-plugin-message-channel = true`. A listed backend proves proxy registration, not reachability: stopping a backend alone does not remove it from `GetServers`. Removing it from proxy registration and reopening the selector marks a saved target unavailable. Evacuation still waits for actual departure before regeneration. Configuration schema stays at 5 and the public API signatures are unchanged.

### BBCode (Spigot)

[SIZE=7][B]ResourceWorldResetter[/B][/SIZE]

Safe, scheduled resource-world regeneration for Spigot, CraftBukkit, Paper, Purpur, and Folia servers.

[CENTER]
[IMG]https://files.catbox.moe/lhrg2i.png[/IMG]

[URL='https://github.com/TamaWish/ResourceWorldResetter/releases'][IMG]https://img.shields.io/github/v/release/TamaWish/ResourceWorldResetter?include_prereleases&sort=date&display_name=tag&style=plastic&logo=github&logoColor=white&label=Release&labelColor=1e293b&color=00b4d8[/IMG][/URL]
[URL='https://github.com/TamaWish/ResourceWorldResetter/actions/workflows/ci.yml'][IMG]https://img.shields.io/github/actions/workflow/status/TamaWish/ResourceWorldResetter/ci.yml?branch=main&style=plastic&logo=githubactions&logoColor=white&label=CI&labelColor=1e293b&color=06d6a0[/IMG][/URL]
[URL='https://www.java.com'][IMG]https://img.shields.io/badge/Java-21%2B%20%7C%20Paper--Folia%2025%2B-f59e0b?style=plastic&logo=openjdk&logoColor=white&labelColor=1e293b[/IMG][/URL]
[URL='https://github.com/TamaWish/ResourceWorldResetter/blob/main/LICENSE'][IMG]https://img.shields.io/github/license/TamaWish/ResourceWorldResetter?style=plastic&logo=github&logoColor=white&label=License&labelColor=1e293b&color=64748b[/IMG][/URL]
[URL='https://discord.gg/kbKZzxDETU'][IMG]https://img.shields.io/discord/1501244767680467096?style=plastic&logo=discord&logoColor=white&label=Discord&labelColor=1e293b&color=5865f2[/IMG][/URL]
[URL='https://modrinth.com/plugin/resourceworldresetter'][IMG]https://img.shields.io/modrinth/game-versions/PjsJlPJ9?style=plastic&logo=modrinth&logoColor=white&label=Supported%20Version&labelColor=1e293b&color=00b4d8[/IMG][/URL]
[URL='https://www.spigotmc.org/resources/resourceworldresetter-1-21-4-26-x.119878/'][IMG]https://img.shields.io/spiget/downloads/119878?style=plastic&logo=spigotmc&logoColor=white&label=SpigotMC&labelColor=1e293b&color=e9a825[/IMG][/URL]
[URL='https://modrinth.com/plugin/resourceworldresetter'][IMG]https://img.shields.io/modrinth/dt/PjsJlPJ9?style=plastic&logo=modrinth&logoColor=white&label=Modrinth&labelColor=1e293b&color=06d6a0[/IMG][/URL]
[URL='https://hangar.papermc.io/Lozaine/ResourceWorldResetter'][IMG]https://img.shields.io/hangar/dt/ResourceWorldResetter?style=plastic&logo=paper&logoColor=white&label=Hangar&labelColor=1e293b&color=38bdf8[/IMG][/URL]
[URL='https://www.curseforge.com/minecraft/bukkit-plugins/resourceworldresetter'][IMG]https://img.shields.io/curseforge/dt/1110835?style=plastic&logo=curseforge&logoColor=white&label=CurseForge&labelColor=1e293b&color=f97316[/IMG][/URL]
[/CENTER]

[SIZE=6][B]Features[/B][/SIZE]

[LIST]
[*]Daily, weekly, monthly, and interval reset schedules with IANA time zones.
[*]Guarded evacuation to local worlds, the default overworld, Velocity/BungeeCord servers, or registered plugin destinations, with departure verification and per-world locking.
[*]Configurable countdown warnings and completion broadcasts.
[*]Persistent reset history and conservative interrupted-operation recovery.
[*]Administration and player teleport GUIs.
[*]Transactional configuration and locale reloads with English fallback.
[*]MiniMessage localization in English, Simplified Chinese, Japanese, and Korean.
[*]Asynchronous update checks that report releases without downloading them.
[/LIST]

[SIZE=6][B]Requirements[/B][/SIZE]

Install exactly one platform JAR and its matching world provider.

[LIST]
[*][B]Spigot / CraftBukkit:[/B] Java 21+, Minecraft 1.21.4+, and [URL='https://modrinth.com/plugin/multiverse-core']Multiverse-Core 5.8.0+[/URL].
[*][B]Paper / Purpur / Folia:[/B] Java 25+, Minecraft 26.1.2+, and [URL='https://modrinth.com/plugin/worlds-1']Worlds 4.4.0+[/URL].
[/LIST]

The Spigot build uses the Bukkit API version 1.21. Worlds 4.4.0 targets Paper/Folia 26.1.2 and 26.2, so the Paper/Folia build requires Minecraft 26.1.2 or newer.

[SIZE=6][B]Installation[/B][/SIZE]

[LIST=1]
[*]Back up the server and every world that RWR will manage.
[*]Install the world provider listed for your platform.
[*]Download the matching JAR from [URL='https://github.com/TamaWish/ResourceWorldResetter/releases']GitHub Releases[/URL].
[*]Place the JAR in the server's plugins/ directory.
[*]Restart the server. Do not use a hot-reload plugin.
[*]Configure worlds with /rwr gui; generated world definitions are stored in managed-worlds.yml.
[*]Run one supervised /rwr reset <id> before enabling unattended resets.
[/LIST]

Use [B]RWR-Spigot-5.2.0.jar[/B] for Spigot/CraftBukkit or [B]RWR-Paper-Folia-5.2.0.jar[/B] for Paper/Purpur/Folia. Do not install both. The shared core and API adapter are bundled.

[QUOTE][B]Important:[/B] RWR 5 requires config-version: 5. Version 4 configuration is not migrated automatically. Follow [URL='https://github.com/TamaWish/ResourceWorldResetter/blob/main/docs/public/OPERATIONS_AND_MIGRATION.md']Operations and Migration[/URL].[/QUOTE]

[SIZE=6][B]Usage[/B][/SIZE]

[CODE]/rwr gui
/rwr status resource
/rwr reset resource
/rwr history 10[/CODE]

A reset evacuates players, asks the installed world provider to regenerate the world, verifies the result, and records the terminal outcome. Ambiguous failures pause automatic resets until an operator reviews the world and configuration.

[SIZE=6][B]Configuration[/B][/SIZE]

[LIST]
[*]Spigot/CraftBukkit: plugins/ResourceWorldResetter/config.yml and managed-worlds.yml
[*]Paper/Purpur/Folia: plugins/ResourceWorldResetter-Paper-Folia/config.yml and managed-worlds.yml
[/LIST]

Minimal daily schedule:

[CODE]worlds:
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
      timeout-seconds: 30
      destination:
        type: local-world
        target: world[/CODE]

On Paper/Folia, multiverse-world stores a Worlds key such as worlds:resource.

[SIZE=5][B]Evacuation destinations (5.2.0)[/B][/SIZE]
Use /rwr gui to choose the default overworld, a loaded local world, a named proxy server, or a registered plugin destination. Lists support paging and manual entry; unavailable local worlds cannot be selected. Global settings provide defaults for newly added worlds.
Use destination.type (default-world, local-world, proxy-server, registered-provider) and destination.target. Configure proxy menu entries with proxy-servers: [hub, lobby]. Transfer timeout defaults to 30 seconds (range 1–120). RWR verifies player departure before regeneration. Velocity requires bungee-plugin-message-channel = true. Legacy destination: world values remain supported and save in the new structured format.

[SIZE=5][B]First-time Velocity network setup[/B][/SIZE]
The lobby is a separate running Paper/Folia backend server, not a world created by Worlds or Multiverse. A minimal network uses Velocity on port 25565, the resource backend on 25566, and the lobby backend on 25567.

[CODE]# velocity.toml
bind = "0.0.0.0:25565"
online-mode = true
player-info-forwarding-mode = "modern"
forwarding-secret-file = "forwarding.secret"

[servers]
resource = "127.0.0.1:25566"
lobby = "127.0.0.1:25567"
try = ["resource", "lobby"]

[advanced]
bungee-plugin-message-channel = true[/CODE]

On both backends, set online-mode=false and a unique server-port in server.properties, and keep settings.bungeecord: false in spigot.yml. Configure this in config/paper-global.yml on [B]every backend[/B], including the lobby:

[CODE]proxies:
  velocity:
    enabled: true
    online-mode: true
    secret: "COPY_THE_EXACT_CONTENTS_OF_VELOCITY_FORWARDING.SECRET"[/CODE]

The same secret and enabled: true are required on every backend. If enabled remains false, Velocity reports that the server did not send a forwarding request. Start the lobby, resource backend, and Velocity in that order; wait for each backend to report Done. Players connect only to Velocity on port 25565. Keep local backend ports bound to 127.0.0.1 and do not expose them publicly.

The RWR proxy target must exactly match the Velocity server key: use target: lobby and proxy-servers: [lobby].

[SIZE=6][B]Commands and permissions[/B][/SIZE]

[LIST]
[*][B]/rwr help[/B] — Show available commands.
[*][B]/rwr status [id][/B] ([B]rwr.status[/B]) — Show reset and schedule status.
[*][B]/rwr gui[/B] ([B]rwr.admin[/B]) — Open the administration GUI.
[*][B]/rwr tp[/B] ([B]rwr.tp[/B]) — Open the player teleport menu.
[*][B]/rwr reset <id>[/B] ([B]rwr.reset[/B]) — Start a guarded reset.
[*][B]/rwr history [count][/B] ([B]rwr.history[/B]) — Show recent reset history.
[*][B]/rwr reload[/B] ([B]rwr.reload[/B]) — Reload configuration, locale, and update-checker settings.
[*][B]/rwr version[/B] ([B]rwr.admin[/B]) — Show installed and latest versions.
[/LIST]

Administrative permissions default to server operators. [B]rwr.tp[/B] defaults to all players. [B]rwr.teleport.world.*[/B] lets operators bypass destination-specific permissions. Leave a destination permission blank to make it public to anyone with [B]rwr.tp[/B].

[SIZE=6][B]Localization[/B][/SIZE]

Bundled locales are en_US, zh_CN, ja_JP, and ko_KR. Select one with locale in config.yml. Fresh installs extract only locales/en_US.yml by default; other bundled languages are extracted when selected, then restarted or reloaded. Missing keys fall back to bundled English.

[SIZE=6][B]Metrics[/B][/SIZE]

RWR uses [URL='https://bstats.org/']bStats[/URL] for anonymous server usage statistics. The Spigot build uses plugin ID [URL='https://bstats.org/plugin/bukkit/ResourceWorldResetter/31502']31502[/URL], and the Paper/Folia build uses plugin ID [URL='https://bstats.org/plugin/bukkit/ResourceWorldResetter-Paper-Folia/33605']33605[/URL]. Disable collection globally in plugins/bStats/config.yml by setting enabled: false.

Messages use MiniMessage. The Spigot/CraftBukkit artifact bundles and isolates adventure-platform-bukkit for component delivery; Paper/Purpur/Folia uses the server's native Adventure API. Legacy ampersand colors and %placeholder% syntax remain accepted.

[SIZE=6][B]Add-ons and API[/B][/SIZE]

[LIST]
[*][URL='https://github.com/TamaWish/RWR-PlaceholderAPI']RWR-PlaceholderAPI[/URL] provides machine-oriented placeholders.
[*][URL='https://github.com/TamaWish/RWR-Discord-Webhook']RWR-Discord Webhook[/URL] publishes reset warnings and outcomes.
[*][URL='https://github.com/TamaWish/RWR-Prometheus']RWR-Prometheus[/URL] exports reset lifecycle metrics.
[*][URL='https://github.com/TamaWish/RWR-API']RWR API[/URL] provides read-only snapshots and reset lifecycle events.
[/LIST]

[SIZE=6][B]Links[/B][/SIZE]

[LIST]
[*][URL='https://github.com/TamaWish/ResourceWorldResetter']Downloads and source[/URL]
[*][URL='https://tamawish.github.io/ResourceWorldResetter/wiki.html']Operator wiki[/URL]
[*][URL='https://github.com/TamaWish/ResourceWorldResetter/blob/main/docs/public/OPERATIONS_AND_MIGRATION.md']Operations and Migration[/URL]
[*][URL='https://github.com/TamaWish/ResourceWorldResetter/blob/main/RELEASE_NOTES.md']Release notes[/URL]
[*][URL='https://github.com/TamaWish/ResourceWorldResetter/blob/main/CHANGELOG.md']Changelog[/URL]
[*][URL='https://discord.gg/kbKZzxDETU']Discord[/URL]
[*][URL='https://github.com/TamaWish/ResourceWorldResetter/issues']Issue tracker[/URL]
[/LIST]

ResourceWorldResetter is licensed under the BSD 3-Clause License. Developed and maintained by TamaWish.

[B]Destination discovery[/B]

Opening the proxy evacuation selector requests GetServers through the connected administrator and immediately shows cached, configured, and saved targets. The matching open selector refreshes after a response while keeping its page and context. Current selections appear first. Missing or duplicate plugin providers are disabled; proxy targets absent from the latest response are unavailable (unverified before the first response). Custom/manual entry remains available, and saved global or managed-world targets remain visible on reopening.

Discovery is an in-memory cache for the plugin lifetime and never rewrites config.yml; proxy-servers supplies stable menu entries. If discovery times out, existing entries remain visible. It requires a connected player, a responding compatible proxy, and on Velocity bungee-plugin-message-channel = true. A listed backend proves proxy registration, not reachability: stopping a backend alone does not remove it from GetServers. Removing it from proxy registration and reopening the selector marks a saved target unavailable. Evacuation still waits for actual departure before regeneration. Configuration schema stays at 5 and the public API signatures are unchanged.
