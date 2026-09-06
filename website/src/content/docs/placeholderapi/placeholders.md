---
title: Placeholder reference
description: Every RWR PlaceholderAPI value and where its data comes from.
---
## Global
| Placeholder | Meaning |
| --- | --- |
| `%rwr_worlds%` | Managed world IDs in configuration order |
| `%rwr_world_names%` | Managed display names in configuration order |

## Per world
Replace `<id>` with the case-insensitive RWR configuration ID.

| Placeholder suffix | Meaning |
| --- | --- |
| `%rwr_world_<id>_id%` / `_name%` / `_world%` | Canonical ID, display name, and platform world name |
| `_state%` / `_phase%` / `_status%` | Operational state, reset phase, and diagnostic status |
| `_can_reset%` / `_resetting%` | Reset capability and active-reset state |
| `_countdown%` / `_next_reset%` | Latest warned countdown and scheduled time |
| `_last_outcome%` | `success`, `failed`, `cancelled`, or `interrupted` |

World IDs containing underscores are supported and matched longest-first.

## Player shortcuts
`%rwr_id%`, `%rwr_name%`, `%rwr_world%`, `%rwr_state%`, `%rwr_phase%`, `%rwr_status%`, `%rwr_can_reset%`, `%rwr_resetting%`, `%rwr_countdown%`, `%rwr_next_reset%`, and `%rwr_last_outcome%` resolve against the requesting player’s current managed world.

Countdown values are event-backed and return the fallback after a restart until the next scheduled warning.
