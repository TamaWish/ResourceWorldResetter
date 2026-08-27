<div align="center">

![ResourceWorldResetter](https://files.catbox.moe/plp3up.png)

[![Release v5.0.0](https://img.shields.io/badge/Release-v5.0.0-brightgreen?style=flat-square)](https://github.com/TamaWish/ResourceWorldResetter/releases)<br>
[![Java](https://img.shields.io/badge/Java-21%2B-orange?style=flat-square&logo=openjdk&logoColor=white)](https://www.java.com)<br>
[![Spigot/Paper](https://img.shields.io/badge/Spigot%2FPaper-1.21.4%2B%20%2F%2026.x%2B-blue?style=flat-square)](https://hub.spigotmc.org/)<br>
[![License](https://img.shields.io/badge/license-BSD--3--Clause-blue?style=flat-square)](https://github.com/TamaWish/ResourceWorldResetter/blob/main/LICENSE)

[![Spigot downloads](https://img.shields.io/spiget/downloads/119878?style=flat-square&label=Spigot%20downloads&color=yellow)](https://www.spigotmc.org/resources/resourceworldresetter.119878/)<br>
[![Modrinth downloads](https://img.shields.io/badge/dynamic/json?style=flat-square&color=1bd96a&label=Modrinth&query=downloads&url=https%3A%2F%2Fapi.modrinth.com%2Fv2%2Fproject%2FPjsJlPJ9&suffix=%20downloads)](https://modrinth.com/project/PjsJlPJ9)<br>
[![GitHub stars](https://img.shields.io/github/stars/TamaWish/ResourceWorldResetter?style=flat-square&logo=github)](https://github.com/TamaWish/ResourceWorldResetter)

[![Hangar](https://img.shields.io/badge/Hangar-ResourceWorldResetter-blue?style=flat-square)](https://hangar.papermc.io/Lozaine)<br>
[![BukkitDev](https://img.shields.io/badge/BukkitDev-Project-blue?style=flat-square)](https://dev.bukkit.org/projects/resourceworldresetter)

</div>

# ResourceWorldResetter 5.0.0

ResourceWorldResetter automates safe resource-world regeneration with scheduling, player evacuation, verification, history, administrative controls, and a player teleport menu.

## Select the correct platform file

| Server | Install | Required world plugin |
|---|---|---|
| Spigot / CraftBukkit | `RWR-Spigot-5.0.0.jar` | [Multiverse-Core 5.8.0+](https://modrinth.com/plugin/multiverse-core) |
| Paper / Purpur / Folia | `RWR-Paper-Folia-5.0.0.jar` | [Worlds by TheNextLvl 4.4.0+](https://modrinth.com/plugin/worlds-1) |

Install exactly one platform JAR. Paper/Folia does **not** use Multiverse-Core, and `rwr-core` is already included in both downloadable plugins.

## Highlights

- Daily, weekly, monthly, and interval schedules with configurable warning minutes
- Safe evacuation followed by authoritative regeneration and independent verification
- Crash journal that records interrupted operations without automatically repeating ambiguous resets
- Admin dashboard, reset confirmation, status, history, and configuration interfaces
- Player teleport GUI with pagination, permissions, counts, and reset-phase protection
- Folia-safe scheduling and fully non-blocking player evacuation
- Public pre-reset and post-reset events
- Java 21+, API baseline 1.21.4, and support for the 26.x server line

## Commands

`/rwr help` · `/rwr status` · `/rwr next` · `/rwr history [count]` · `/rwr reset <world-id>` · `/rwr reload` · `/rwr gui` · `/rwr tp`

Back up the plugin data folder and managed worlds before upgrading. **v4.2.1 and earlier are legacy v4 releases.** v5 uses a new configuration and requires a manual migration.

[Downloads](https://github.com/TamaWish/ResourceWorldResetter/releases) · [Operations & Migration](https://github.com/TamaWish/ResourceWorldResetter/blob/main/OPERATIONS_AND_MIGRATION.md) · [Changelog](https://github.com/TamaWish/ResourceWorldResetter/blob/main/CHANGELOG.md)

Author: **Lozaine** · Copyright: **TamaWish** · BSD 3-Clause
