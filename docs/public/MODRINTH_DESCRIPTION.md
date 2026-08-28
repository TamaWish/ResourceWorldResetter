<div align="center">

![ResourceWorldResetter](https://files.catbox.moe/lhrg2i.png)

[![Release v5.0.0](https://img.shields.io/badge/Release-v5.0.0-brightgreen?style=flat-square)](https://github.com/TamaWish/ResourceWorldResetter/releases)
[![Java](https://img.shields.io/badge/Java-21%2B-orange?style=flat-square&logo=openjdk&logoColor=white)](https://www.java.com)
[![Spigot/Paper](https://img.shields.io/badge/Spigot%2FPaper-1.21.4%2B%20%2F%2026.x%2B-blue?style=flat-square)](https://hub.spigotmc.org/)
[![License](https://img.shields.io/badge/license-BSD--3--Clause-blue?style=flat-square)](https://github.com/TamaWish/ResourceWorldResetter/blob/main/LICENSE)
<br>
[![Spigot downloads](https://img.shields.io/spiget/downloads/119878?style=flat-square&label=Spigot%20downloads&color=yellow)](https://www.spigotmc.org/resources/resourceworldresetter.119878/)
[![Modrinth downloads](https://img.shields.io/badge/dynamic/json?style=flat-square&color=1bd96a&label=Modrinth&query=downloads&url=https%3A%2F%2Fapi.modrinth.com%2Fv2%2Fproject%2FPjsJlPJ9&suffix=%20downloads)](https://modrinth.com/project/PjsJlPJ9)
[![GitHub stars](https://img.shields.io/github/stars/TamaWish/ResourceWorldResetter?style=flat-square&logo=github)](https://github.com/TamaWish/ResourceWorldResetter)
<br>
[![Hangar](https://img.shields.io/badge/Hangar-ResourceWorldResetter-blue?style=flat-square)](https://hangar.papermc.io/Lozaine)
[![BukkitDev](https://img.shields.io/badge/BukkitDev-Project-blue?style=flat-square)](https://dev.bukkit.org/projects/resourceworldresetter)

</div>

<iframe width="560" height="315" src="https://www.youtube-nocookie.com/embed/9RhO29M5yuI" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>

![PRE](https://files.catbox.moe/39wc63.png)

![V5](https://files.catbox.moe/347t1s.png)

Automated resource-world regeneration for **Spigot**, **CraftBukkit**, **Paper**, **Purpur**, and **Folia**.

ResourceWorldResetter schedules resets, evacuates players, regenerates through the matching world plugin, then verifies the result. Operators get status, history, and an admin GUI. Players get a teleport menu with permissions and reset-phase protection.

![BUILD](https://files.catbox.moe/cja623.png)

Install **exactly one** platform JAR. The shared core is already included — do not install a separate `rwr-core` file.

| Server | Install | Required world plugin |
|--------|---------|------------------------|
| Spigot / CraftBukkit | `RWR-Spigot-5.0.0.jar` | [Multiverse-Core 5.8.0+](https://modrinth.com/plugin/multiverse-core) |
| Paper / Purpur / Folia | `RWR-Paper-Folia-5.0.0.jar` | [Worlds by TheNextLvl 4.4.0+](https://modrinth.com/plugin/worlds-1) |

Paper, Purpur, and Folia do **not** use Multiverse-Core. Multiverse-Core does not support Folia.

![FEATURES](https://files.catbox.moe/nczm04.png)

- Daily, weekly, monthly, and interval schedules with timezone-aware next-run calculation
- Configurable warning minutes and countdown broadcasts
- Safe player evacuation before regeneration
- Authoritative regeneration through Multiverse-Core or Worlds, then independent verification
- Crash journal that records interrupted operations without automatically repeating ambiguous resets
- Admin dashboard, world configuration, global settings, history, and confirmation screens
- Player teleport GUI with pagination, permissions, player counts, locked visibility, and reset-phase blocking
- Folia-safe scheduling and fully non-blocking player evacuation
- Public cancellable pre-reset and terminal post-reset events
- MiniMessage `messages.yml`

![REQ](https://files.catbox.moe/j4bfr7.png)

- Java **21+**
- Minecraft API **1.21.4+**, including the **26.x** server line
- The world plugin that matches the selected RWR build (see table above)

![COMMANDS](https://files.catbox.moe/m4mpnm.png)

| Command | Permission | Description |
|---------|------------|-------------|
| `/rwr help` | (none / relevant nodes) | Permission-aware help |
| `/rwr status [id]` | `rwr.status` | Live reset / schedule status |
| `/rwr gui` | `rwr.admin` | Admin configuration GUI |
| `/rwr tp` | `rwr.tp` | Player teleport menu |
| `/rwr reset <id>` | `rwr.reset` | Guarded immediate reset |
| `/rwr history [n]` | `rwr.history` | Recent reset history |
| `/rwr reload` | `rwr.reload` | Transactional config reload |

![PERMISSIONS](https://files.catbox.moe/nwrda9.png)

| Permission | Default | Description |
|------------|---------|-------------|
| `rwr.admin` | op | Full admin GUI and admin actions |
| `rwr.reload` | op | Reload configuration |
| `rwr.reset` | op | Force a guarded reset |
| `rwr.status` | op | View status |
| `rwr.history` | op | View reset history |
| `rwr.tp` | true | Open the teleport GUI |
| `rwr.teleport.world.*` | op | Bypass per-destination teleport permissions |

Leave a destination `permission` blank to make it public to anyone with `rwr.tp`. Set it to any Bukkit node (for example a LuckPerms group) to restrict that destination.

![NOTE](https://files.catbox.moe/tlbdkl.png)

Back up the plugin data folder and managed worlds before upgrading. **v4.2.1 and earlier are legacy v4 releases.** v5 uses a new configuration (`config-version: 5`) and requires a manual migration.

[Downloads](https://github.com/TamaWish/ResourceWorldResetter/releases) · [Operations & Migration](https://github.com/TamaWish/ResourceWorldResetter/blob/main/docs/public/OPERATIONS_AND_MIGRATION.md) · [Changelog](https://github.com/TamaWish/ResourceWorldResetter/blob/main/CHANGELOG.md)

Author: **Lozaine** · Copyright: **TamaWish** · BSD 3-Clause
