<div align="center">

![ResourceWorldResetter](https://files.catbox.moe/lhrg2i.png)

![GitHub Release](https://img.shields.io/github/v/release/TamaWish/ResourceWorldResetter?include_prereleases&sort=date&display_name=tag&style=plastic&logo=github&logoColor=white&label=Release&color=violet&link=https%3A%2F%2Fgithub.com%2FTamaWish%2FResourceWorldResetter%2Freleases)
[![Java](https://img.shields.io/badge/Java-21%2B-orange?style=plastic-square&logo=openjdk&logoColor=white)](https://www.java.com)
![GitHub License](https://img.shields.io/github/license/TamaWish/ResourceWorldResetter?style=plastic&logo=github&logoColor=white&label=License&color=red&link=https%3A%2F%2Fgithub.com%2FTamaWish%2FResourceWorldResetter%2Ftree%2Fmain%3Ftab%3DBSD-3-Clause-1-ov-file)
![Discord](https://img.shields.io/discord/1501244767680467096?style=plastic&logo=discord&logoColor=blue&label=Discord&color=blue&link=https%3A%2F%2Fdiscord.gg%2FkbKZzxDETU)
<br>
![Modrinth Game Versions](https://img.shields.io/modrinth/game-versions/PjsJlPJ9?style=plastic&logo=modrinth&logoColor=green&label=Supported%20Version&color=orange)
<br>
![Spiget Downloads](https://img.shields.io/spiget/downloads/119878?style=plastic&logo=spigotmc&logoColor=yellow&label=SpigotMC&labelColor=grey&color=yellow&link=https%3A%2F%2Fwww.spigotmc.org%2Fresources%2Fresourceworldresetter-1-21-4-26-x.119878%2F)
![Modrinth Downloads](https://img.shields.io/modrinth/dt/PjsJlPJ9?style=plastic&logo=modrinth&logoColor=green&label=Modrinth&link=https%3A%2F%2Fmodrinth.com%2Fplugin%2Fresourceworldresetter)
![Hangar Downloads](https://img.shields.io/hangar/dt/ResourceWorldResetter?style=plastic&label=Hangar&color=grey&link=https%3A%2F%2Fhangar.papermc.io%2FLozaine%2FResourceWorldResetter)
![CurseForge Downloads](https://img.shields.io/curseforge/dt/1110835?style=plastic&logo=curseforge&logoColor=orange&label=CurseForge&color=orange&link=https%3A%2F%2Fwww.curseforge.com%2Fminecraft%2Fbukkit-plugins%2Fresourceworldresetter)

</div>

# ResourceWorldResetter 5.1.0

Monorepo for ResourceWorldResetter. Marketing author **Lozaine**; copyright **TamaWish**. License: **BSD 3-Clause**.

> **5.1.0 is ready for release on September 2, 2026.** Final Spigot, Paper, and Folia smoke testing is complete. The latest public runtime remains 5.0.0 until publication.

## Download

| Channel | Link |
|---------|------|
| GitHub Releases | [https://github.com/TamaWish/ResourceWorldResetter/releases](https://github.com/TamaWish/ResourceWorldResetter/releases) |
| SpigotMC | [https://www.spigotmc.org/resources/resourceworldresetter.119878/](https://www.spigotmc.org/resources/resourceworldresetter.119878/) |
| Modrinth | [https://modrinth.com/project/PjsJlPJ9](https://modrinth.com/project/PjsJlPJ9) |
| Hangar | [https://hangar.papermc.io/Lozaine](https://hangar.papermc.io/Lozaine) |
| CurseForge | [https://www.curseforge.com/minecraft/bukkit-plugins/resourceworldresetter](https://www.curseforge.com/minecraft/bukkit-plugins/resourceworldresetter) |

## Required dependencies

Install the world plugin that matches your platform **before** RWR. These are separate downloads (not part of the RWR jar).

| Platform | Dependency | Download |
|----------|------------|----------|
| Spigot / CraftBukkit | **Multiverse-Core** | [Modrinth](https://modrinth.com/plugin/multiverse-core) |
| Paper / Purpur / Folia | **Worlds** (TheNextLvl) | [Modrinth](https://modrinth.com/plugin/worlds-1) |

| Module | Download | Server | World plugin |
|--------|----------|--------|--------------|
| [RWR-Spigot](RWR-Spigot/) | `RWR-Spigot-5.1.0.jar` | Spigot / CraftBukkit | [Multiverse-Core](https://modrinth.com/plugin/multiverse-core) |
| [RWR-Paper-Folia](RWR-Paper-Folia/) | `RWR-Paper-Folia-5.1.0.jar` | Paper / Purpur / Folia | [Worlds](https://modrinth.com/plugin/worlds-1) |
| [rwr-core](rwr-core/) | **Not a separate download** (shaded into each platform jar) | — | Shared domain |

Install **exactly one** platform jar. You never need to download `rwr-core` for production servers.

## Compatibility

- Java **21+**
- API baseline **1.21.4**, tested through **26.2**
- Spigot/CraftBukkit → [Multiverse-Core](https://modrinth.com/plugin/multiverse-core) 5.x
- Paper/Purpur/Folia → [Worlds](https://modrinth.com/plugin/worlds-1) ≥ 4.4.0 (`folia-supported`)

Version 5.1 retains the v5 configuration format and adds the embedded public integration API.

## Metrics (bStats)

RWR uses bStats to collect anonymous usage metrics. No world names or player identities are included. To disable metrics, open `plugins/bStats/config.yml` and set `enabled: false`; this setting is managed by bStats and is not located in the RWR plugin folder.

## Integrating with RWR

Third-party plugins compile against `io.github.tamawish:rwr-api:5.1.2` and discover the live read-only
service through Bukkit's `ServicesManager`. The contract, examples, sources, and Javadocs live in
[TamaWish/RWR-API](https://github.com/TamaWish/RWR-API). Use `compileOnly`/`provided`; do not shade the
API into consumer plugins and do not install a separate API JAR on the server.

The query service is available starting with RWR runtime **5.1.0**. RWR **5.0.0 does not provide it**;
keep integrations optional until server owners have upgraded. The API exposes managed-world snapshots,
reset status, and reset lifecycle events, but does not let other plugins trigger resets or change RWR's
configuration. API 5.1.0 contains snapshots, status, and pre/post reset events; scheduled-warning
listeners compile against the compatible API 5.1.2 addition.

RWR 5.1.0 is intentionally limited to public API integration and scheduled-warning events. New
commands, locale files, fallback rules, and complete localization are reserved for RWR 5.2.0.

## Commands and permissions

| Command | Permission | Description |
|---------|------------|-------------|
| `/rwr help` | (none / relevant nodes) | Permission-aware help |
| `/rwr status [id]` | `rwr.status` | Live reset / schedule status |
| `/rwr gui` | `rwr.admin` | Admin configuration GUI |
| `/rwr tp` | `rwr.tp` | Player teleport menu |
| `/rwr reset <id>` | `rwr.reset` | Guarded immediate reset |
| `/rwr history [n]` | `rwr.history` | Recent reset history |
| `/rwr reload` | `rwr.reload` | Transactional config reload |

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

## Build

```bash
mvn -pl RWR-Spigot,RWR-Paper-Folia -am clean package
```

See each module README for features, credits, and license text.

## Public documentation

- [Operator wiki](https://tamawish.github.io/ResourceWorldResetter/wiki.html)
- [Operations & Migration](docs/public/OPERATIONS_AND_MIGRATION.md)
- [v5 changelog](CHANGELOG.md)
- [v5 release notes](docs/public/RELEASE_NOTES.md)
- [Legacy v4 changelog](CHANGELOG_v4.md)
