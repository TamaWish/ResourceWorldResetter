---
title: Commands and permissions
description: Inspect, manage, and operate resource-world resets.
---

| Command | Permission | Purpose |
| --- | --- | --- |
| `/rwr help` | Relevant nodes | Show available commands. |
| `/rwr status [id]` | `rwr.status` | Show state, current phase, and next schedule. |
| `/rwr history [count]` | `rwr.history` | Read persisted terminal and interrupted operations. |
| `/rwr reset <id>` | `rwr.reset` | Start a guarded immediate reset. |
| `/rwr reload` | `rwr.reload` | Transactionally validate and activate configuration and locale changes. |
| `/rwr gui` | `rwr.admin` | Open the administration dashboard. |
| `/rwr tp` | `rwr.tp` | Open the player teleport menu. |
| `/rwr version` | `rwr.admin` | Show cached update status. |

Administrative permissions default to operators. `rwr.tp` defaults to all players. A destination can have its own Bukkit permission; `rwr.teleport.world.*` bypasses those destination restrictions.
