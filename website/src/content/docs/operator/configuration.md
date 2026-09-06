---
title: Configuration
description: Configure managed worlds, schedules, warnings, and safe destinations.
---

RWR 5 uses `config-version: 5`. Reload is transactional: invalid input is rejected while the previous active settings and schedules remain in service.

| Setting | Purpose |
| --- | --- |
| `timezone` | IANA timezone used for schedules and history output. |
| `default-hub-world` | Default safe evacuation destination. |
| `worlds.<id>` | Stable identifier used by commands and configuration. |
| `multiverse-world` | Provider identity; this field also holds Worlds keys on Paper/Folia. |
| `display-name` | The presentation name used in messages, status, history, and GUIs. |
| `warning-minutes` | Whole-minute countdown points; use `[]` to disable warnings. |

```yaml
worlds:
  rainforest:
    multiverse-world: rainforest
    display-name: Rainforest
    enabled: true
    managed: true
    schedule:
      type: DAILY
      time: '03:00'
    warning-minutes: [30, 10, 5, 1]
    regeneration:
      seed-policy: RANDOM
    evacuation:
      enabled: true
      destination: world
```

After editing YAML, run `/rwr reload` and check the calculated next run with `/rwr status <id>`.
