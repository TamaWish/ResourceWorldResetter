<div align="center">

![ResourceWorldResetter](https://files.catbox.moe/lhrg2i.png)

![GitHub Release](https://img.shields.io/github/v/release/TamaWish/ResourceWorldResetter?include_prereleases&sort=date&display_name=tag&style=plastic&logo=github&logoColor=white&label=Release&color=violet&link=https%3A%2F%2Fgithub.com%2FTamaWish%2FResourceWorldResetter%2Freleases)
[![Java](https://img.shields.io/badge/Java-25%2B-orange?style=plastic-square&logo=openjdk&logoColor=white)](https://www.java.com)
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

# ResourceWorldResetter 5.1.0 — Paper / Purpur / Folia

Automated resource-world resets for **Paper**, **Purpur**, and **Folia**.

Marketing / plugin author name: **Lozaine**<br>
Legal copyright: **TamaWish**

Shaded release JAR: `RWR-Paper-Folia-5.1.0.jar`<br>
Plugin / data folder: `plugins/ResourceWorldResetter-Paper-Folia/`

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
| **Worlds** (TheNextLvl) ≥ 4.4.0 | [https://modrinth.com/plugin/worlds-1](https://modrinth.com/plugin/worlds-1) |

Spigot/CraftBukkit servers should use [RWR-Spigot](../RWR-Spigot/) with [Multiverse-Core](https://modrinth.com/plugin/multiverse-core) instead.

---

## Installation

1. Install **[Worlds](https://modrinth.com/plugin/worlds-1)** ≥ 4.4.0.
2. Download **only** `RWR-Paper-Folia-5.1.0.jar` into `plugins/`.
3. **Do not** download or install a separate `rwr-core` JAR. Core is compiled into this plugin jar.
4. Restart or load the plugin, then edit `config.yml` / `messages.yml` (or use `/rwr gui`).

Use **one** RWR platform jar per server. Do not install Spigot and Paper-Folia jars together. Multiverse-Core is **not** used on this platform (and does not support Folia).

### Building from source

```bash
mvn -pl RWR-Paper-Folia -am clean package
```

Output: `RWR-Paper-Folia/target/RWR-Paper-Folia-5.1.0.jar`

---

## Compatibility

| Requirement | Supported |
|-------------|-----------|
| Java | **25+** (required by Worlds 4.4.0+) |
| Minecraft API baseline | **1.21.4** (`api-version: 1.21`, `folia-supported: true`) |
| Tested / supported range | **1.21.4 → 26.2** (author-tested) |
| Server software | Paper, Purpur, Folia |
| World plugin (hard depend) | **[Worlds](https://modrinth.com/plugin/worlds-1)** (TheNextLvl) ≥ 4.4.0 |

Version 5.1 retains `config-version: 5`; pre-v5 configurations still require manual migration.

### v5.0.0 release validation

The v5.0.0 release candidate was validated on **Folia 26.1.2 build 8** and **Paper 26.2 build 119** with **Worlds 4.4.0** and Java 25. Each deployed JAR matched the locally verified release artifact.

- Clean enable with two managed and scheduled Worlds dimensions
- `/rwr status`, `/rwr history`, `/rwr gui`, and `/rwr tp` exercised in game
- `/rwr status` displayed each current phase and next reset
- Player teleport GUI and asynchronous completion messages completed without Folia thread-access errors
- Supervised `worlds_two` regeneration completed successfully through `WorldsAccess.regenerate`
- `SAME` seed policy preserved seed `-8489897642303906936` across the live reset
- A 1,234-block world border was preserved across the live reset
- The manual reset returned to its configured daily schedule after completion
- No RWR exceptions, Folia thread violations, watchdog stalls, or reset warnings were logged

Paper 26.2 validation used the same release artifact with one managed `worlds_rainforest` dimension:

- Clean startup, `/rwr status`, `/rwr gui`, `/rwr tp`, and player teleport feedback passed
- A player inside the managed world was synchronously evacuated to `world` before regeneration
- Supervised Worlds regeneration and independent verification completed successfully in about three seconds
- `SAME` seed policy preserved seed `16481743560746433`
- The default 59,999,968-block world border was preserved
- The configured daily schedule returned after the manual reset
- No RWR error, warning, exception, or thread-access failure was logged

Automated coverage also verifies `SAME`, `FIXED`, and `RANDOM` seed planning, level-data policy mapping, dotted teleport-world keys, schedule reconciliation, safe retry limits, and ambiguous-failure suspension. The full reactor completed `mvn clean verify` with **83 passing tests**: 59 core, 18 Spigot, and 6 Paper/Folia. Gamerule preservation is implemented through the same captured-state restoration path as the live-tested world border, but it was not separately exercised in this runtime session.

Purpur uses the same Paper API and scheduler path validated above and is expected to be compatible, but Purpur was not separately exercised in this validation run.

---

## Features

- Shared **rwr-core** reset coordinator: evacuate → regenerate → verify, with typed failures
- Worlds regenerate-only path via `WorldsAccess.regenerate` (no Multiverse unload/delete/create pipeline)
- Regeneration policies for same, fixed, or random seeds, with optional preservation of level configuration, gamerules, and world border
- Folia-safe scheduling (global / region / entity / async helpers) and non-blocking `teleportAsync` evacuation
- Configured safe-failure retries with a bounded retry count and delay; ambiguous or interrupted results stop automatic scheduling for operator review
- Daily / weekly / monthly / interval schedules with timezone-aware next-run calculation
- Configurable multi-interval warnings (`warning-minutes`)
- Crash journal with conservative recovery — interrupted ops are recorded, not auto-replayed
- Admin GUI (`/rwr gui`): dashboard, add world (Worlds key auto-detect), world config, global settings, teleport admin, history, confirmations
- Player teleport GUI (`/rwr tp`): auto-discover loaded dimensions, pagination, public/custom permissions, locked visibility, reset-phase blocking
- Auto-remove managed config entries when a world is deleted through Worlds
- Anvil text input with chat fallback
- MiniMessage `messages.yml` (gradient `[RWR]` styling)
- Public API events: scheduled warning, cancellable pre-reset, and terminal post-reset
- Version 5.1.0 is limited to API integration and warning events; localization and related commands
  are reserved for 5.2.0.
- **bStats** metrics (plugin id **33605**). No world names or player identities are included. To disable metrics, set `enabled: false` in `plugins/bStats/config.yml`; this setting is managed by bStats, not the RWR plugin folder

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
| `rwr.teleport.world.<key>` | false | Access a specific destination (when configured) |

Destination permissions can be left blank (public to anyone with `rwr.tp`) or set to any Bukkit node (e.g. LuckPerms ranks).

---

## Configuration notes

- `config-version: 5` is required.
- `worlds.<id>.multiverse-world` — Worlds key string (e.g. `worlds:resource` or `minecraft:resource`). Field name is shared with the Spigot schema.
- `worlds.<id>.managed: true` — required for schedules/resets (missing/`false` = fail-closed / hub protection).
- Messages live in `messages.yml` (MiniMessage).
- See bundled `config.yml` for the full schema.

---

## Credits

- **TamaWish** (copyright) — published as **Lozaine**
- **TheNextLvl Worlds** — TheNextLvl — [Modrinth](https://modrinth.com/plugin/worlds-1)
- Adventure / MiniMessage — Kyori (provided by Paper)
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

Worlds remains under its own license (© TheNextLvl).
