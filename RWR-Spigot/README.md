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

# ResourceWorldResetter 5.1.0 — Spigot / CraftBukkit

Automated resource-world resets for **Spigot** and **CraftBukkit**.

Marketing / plugin author name: **Lozaine**<br>
Legal copyright: **TamaWish**

Shaded release JAR: `RWR-Spigot-5.1.0.jar`<br>
Plugin / data folder: `plugins/ResourceWorldResetter/`

## Download

| Channel | Link |
|---------|------|
| GitHub Releases | [https://github.com/TamaWish/ResourceWorldResetter/releases](https://github.com/TamaWish/ResourceWorldResetter/releases) |
| SpigotMC | [https://www.spigotmc.org/resources/resourceworldresetter.119878/](https://www.spigotmc.org/resources/resourceworldresetter.119878/) |
| Modrinth | [https://modrinth.com/project/PjsJlPJ9](https://modrinth.com/project/PjsJlPJ9) |
| Hangar | [https://hangar.papermc.io/Lozaine](https://hangar.papermc.io/Lozaine) |
| CurseForge | [https://www.curseforge.com/minecraft/bukkit-plugins/resourceworldresetter](https://www.curseforge.com/minecraft/bukkit-plugins/resourceworldresetter) |

## Required dependency

| Dependency | Download |
|------------|----------|
| **Multiverse-Core** 5.8.0+ (5.x) | [https://modrinth.com/plugin/multiverse-core](https://modrinth.com/plugin/multiverse-core) |

Paper/Folia servers should use [RWR-Paper-Folia](../RWR-Paper-Folia/) with [Worlds](https://modrinth.com/plugin/worlds-1) instead — Multiverse-Core does not support Folia.

---

## Installation

1. Install **[Multiverse-Core](https://modrinth.com/plugin/multiverse-core)** 5.8.0+ (5.x).
2. Download **only** `RWR-Spigot-5.1.0.jar` into `plugins/`.
3. **Do not** download or install a separate `rwr-core` JAR. Core is compiled into this plugin jar.
4. Restart or load the plugin, then edit `config.yml` / `messages.yml` (or use `/rwr gui`).

Use **one** RWR platform jar per server. Do not install Spigot and Paper-Folia jars together.

### Building from source

```bash
mvn -pl RWR-Spigot -am clean package
```

Output: `RWR-Spigot/target/RWR-Spigot-5.1.0.jar`

---

## Compatibility

| Requirement | Supported |
|-------------|-----------|
| Java | **21+** |
| Minecraft API baseline | **1.21.4** (`api-version: 1.21`) |
| Tested / supported range | **1.21.4 → 26.2** (author-tested) |
| Server software | Spigot, CraftBukkit |
| World plugin (hard depend) | **[Multiverse-Core](https://modrinth.com/plugin/multiverse-core)** 5.8.0 through 5.x |

Version 5.1 retains `config-version: 5`. Older v4 configs are not auto-migrated.

---

## Features

- Shared **rwr-core** reset coordinator: evacuate → regenerate → verify, with typed failures
- Atomic per-world locks and guarded Multiverse regeneration (no unload/delete/create pipeline owned by RWR)
- Daily / weekly / monthly / interval schedules with timezone-aware next-run calculation
- Configurable multi-interval warnings (`warning-minutes`)
- Crash journal (`reset-history` / active-reset markers) with conservative recovery — interrupted ops are recorded, not auto-replayed
- Admin GUI (`/rwr gui`): dashboard, add world, world config, global settings, teleport admin, history, confirmations
- Player teleport GUI (`/rwr tp`): auto-discover, pagination, public/custom permissions, locked visibility, reset-phase blocking
- Anvil text input with chat fallback
- MiniMessage `messages.yml` (gradient `[RWR]` styling; legacy `&` / `%placeholders%` normalized on load)
- Public API events: scheduled warning, cancellable pre-reset, and terminal post-reset
- Version 5.1.0 is limited to API integration and warning events; localization and related commands
  are reserved for 5.2.0.
- **bStats** metrics (plugin id **31502**). No world names or player identities are included. To disable metrics, set `enabled: false` in `plugins/bStats/config.yml`; this setting is managed by bStats, not the RWR plugin folder

---

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/rwr help` | (none / relevant nodes) | Permission-aware help |
| `/rwr status [id]` | `rwr.status` | Live reset / schedule status |
| `/rwr gui` | `rwr.admin` | Admin configuration GUI |
| `/rwr tp` | `rwr.tp` | Player teleport menu |
| `/rwr reset <id>` | `rwr.reset` | Guarded immediate reset |
| `/rwr history [n]` | `rwr.history` | Recent reset history |
| `/rwr reload` | `rwr.reload` | Transactional config reload |

---

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `rwr.admin` | op | Full admin GUI and admin actions |
| `rwr.reload` | op | Reload configuration |
| `rwr.reset` | op | Force a guarded reset |
| `rwr.status` | op | View status |
| `rwr.history` | op | View reset history |
| `rwr.tp` | true | Open the teleport GUI |
| `rwr.teleport.world.*` | op | Bypass per-destination teleport permissions |
| `rwr.teleport.world.<name>` | false | Access a specific destination (when configured) |

Destination permissions can be left blank (public to anyone with `rwr.tp`) or set to any Bukkit node (e.g. LuckPerms ranks).

---

## Configuration notes

- `config-version: 5` is required.
- `worlds.<id>.multiverse-world` — Multiverse legacy world name (e.g. `resource`).
- `worlds.<id>.managed: true` — required for schedules/resets (missing/`false` = fail-closed / teleport-only style protection for hub).
- Messages live in `messages.yml` (MiniMessage).
- See bundled `config.yml` for the full schema.

---

## Credits

- **TamaWish** (copyright) — published as **Lozaine**
- **Multiverse-Core** — Multiverse team / OnARandomBox — [Modrinth](https://modrinth.com/plugin/multiverse-core)
- Adventure / MiniMessage — Kyori
- AnvilGUI — WesJD
- bStats — bStats team
- Shared domain — ResourceWorldResetter **rwr-core** (shaded; not a separate download)

---

## License

**BSD 3-Clause License**

Copyright (c) TamaWish. All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

Multiverse-Core remains under its own license (© Multiverse / OnARandomBox).
