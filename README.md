<div align="center">

![ResourceWorldResetter](https://files.catbox.moe/plp3up.png)

[![Release v5.0.0](https://img.shields.io/badge/Release-v5.0.0-brightgreen?style=flat-square)](https://github.com/TamaWish/ResourceWorldResetter/releases)<br>
[![Java](https://img.shields.io/badge/Java-21%2B-orange?style=flat-square&logo=openjdk&logoColor=white)](https://www.java.com)<br>
[![Spigot/Paper](https://img.shields.io/badge/Spigot%2FPaper-1.21.4%2B%20%2F%2026.x%2B-blue?style=flat-square)](https://hub.spigotmc.org/)<br>
[![License](https://img.shields.io/badge/license-BSD--3--Clause-blue?style=flat-square)](https://github.com/TamaWish/ResourceWorldResetter/blob/main/LICENSE)

<br>

[![Spigot downloads](https://img.shields.io/spiget/downloads/119878?style=flat-square&label=Spigot%20downloads&color=yellow)](https://www.spigotmc.org/resources/resourceworldresetter.119878/)<br>
[![Modrinth downloads](https://img.shields.io/badge/dynamic/json?style=flat-square&color=1bd96a&label=Modrinth&query=downloads&url=https%3A%2F%2Fapi.modrinth.com%2Fv2%2Fproject%2FPjsJlPJ9&suffix=%20downloads)](https://modrinth.com/project/PjsJlPJ9)<br>
[![GitHub stars](https://img.shields.io/github/stars/TamaWish/ResourceWorldResetter?style=flat-square&logo=github)](https://github.com/TamaWish/ResourceWorldResetter)

<br>

[![Hangar](https://img.shields.io/badge/Hangar-ResourceWorldResetter-blue?style=flat-square)](https://hangar.papermc.io/Lozaine)<br>
[![BukkitDev](https://img.shields.io/badge/BukkitDev-Project-blue?style=flat-square)](https://dev.bukkit.org/projects/resourceworldresetter)

</div>

# ResourceWorldResetter 5.0.0

Monorepo for ResourceWorldResetter. Marketing author **Lozaine**; copyright **TamaWish**. License: **BSD 3-Clause**.

## Download

| Channel | Link |
|---------|------|
| GitHub Releases | [https://github.com/TamaWish/ResourceWorldResetter/releases](https://github.com/TamaWish/ResourceWorldResetter/releases) |
| SpigotMC | [https://www.spigotmc.org/resources/resourceworldresetter.119878/](https://www.spigotmc.org/resources/resourceworldresetter.119878/) |
| Modrinth | [https://modrinth.com/project/PjsJlPJ9](https://modrinth.com/project/PjsJlPJ9) |
| Hangar | [https://hangar.papermc.io/Lozaine](https://hangar.papermc.io/Lozaine) |
| BukkitDev | [https://dev.bukkit.org/projects/resourceworldresetter](https://dev.bukkit.org/projects/resourceworldresetter) |
| CurseForge (legacy) | [https://legacy.curseforge.com/minecraft/bukkit-plugins/resourceworldresetter](https://legacy.curseforge.com/minecraft/bukkit-plugins/resourceworldresetter) |

## Required dependencies

Install the world plugin that matches your platform **before** RWR. These are separate downloads (not part of the RWR jar).

| Platform | Dependency | Download |
|----------|------------|----------|
| Spigot / CraftBukkit | **Multiverse-Core** | [Modrinth](https://modrinth.com/plugin/multiverse-core) |
| Paper / Purpur / Folia | **Worlds** (TheNextLvl) | [Modrinth](https://modrinth.com/plugin/worlds-1) |

| Module | Download | Server | World plugin |
|--------|----------|--------|--------------|
| [RWR-Spigot](RWR-Spigot/) | `RWR-Spigot-5.0.0.jar` | Spigot / CraftBukkit | [Multiverse-Core](https://modrinth.com/plugin/multiverse-core) |
| [RWR-Paper-Folia](RWR-Paper-Folia/) | `RWR-Paper-Folia-5.0.0.jar` | Paper / Purpur / Folia | [Worlds](https://modrinth.com/plugin/worlds-1) |
| [rwr-core](rwr-core/) | **Not a separate download** (shaded into each platform jar) | — | Shared domain |

Install **exactly one** platform jar. You never need to download `rwr-core` for production servers.

## Compatibility

- Java **21+**
- API baseline **1.21.4**, tested through **26.2**
- Spigot/CraftBukkit → [Multiverse-Core](https://modrinth.com/plugin/multiverse-core) 5.x
- Paper/Purpur/Folia → [Worlds](https://modrinth.com/plugin/worlds-1) ≥ 4.4.0 (`folia-supported`)

Both platforms ship as a **fresh v5.0.0** release.

## Build

```bash
mvn -pl RWR-Spigot,RWR-Paper-Folia -am clean package
```

See each module README for features, permissions, credits, and license text.

## Public documentation

- [Operations & Migration](OPERATIONS_AND_MIGRATION.md)
- [Changelog](CHANGELOG.md)
- [SpigotMC BBCode description](docs/public/SPIGOT_DESCRIPTION.bbcode)
- [Modrinth Markdown description](docs/public/MODRINTH_DESCRIPTION.md)
- [BukkitDev Markdown description](docs/public/BUKKITDEV_DESCRIPTION.md)
- [Hangar Markdown description](docs/public/HANGAR_DESCRIPTION.md)

Internal roadmaps, rollback notes, implementation notes, and test matrices are intentionally excluded by `.gitignore`.
