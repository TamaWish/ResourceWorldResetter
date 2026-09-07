---
title: Configuration
description: Configure managed worlds, schedules, warnings, and safe destinations.
---

RWR 5.2 uses `config.yml` for global operator settings and `managed-worlds.yml` for GUI-managed world definitions and per-world teleport overrides. Reload validates both files as one snapshot: invalid input is rejected while the previous active settings and schedules remain in service. `/rwr reload` and the administration GUI reload also refresh the selected locale and update-checker settings.

| Setting | Purpose |
| --- | --- |
| `locale` | Server-wide message language. Bundled values: `en_US`, `zh_CN`, `ja_JP`, `ko_KR`. |
| `timezone` | IANA timezone used for schedules and history output. |
| `default-hub-world` | Default safe evacuation destination. Fresh Paper/Folia installs use the server default world. |
| `managed-worlds.yml: worlds.<id>` | Stable identifier used by commands and configuration. |
| `multiverse-world` | Provider identity; this field also holds Worlds keys on Paper/Folia. |
| `display-name` | The presentation name used in messages, status, history, and GUIs. |
| `warning-minutes` | Whole-minute countdown points; use `[]` to disable warnings. |

### Locales

On a clean install with the default `locale: en_US`, only `locales/en_US.yml` is extracted into the plugin data folder. Other bundled languages remain inside the JAR until selected: set `locale` to `zh_CN`, `ja_JP`, or `ko_KR`, then restart or run `/rwr reload` to write that file under `locales/`. Omitted keys fall back to bundled JAR defaults; an invalid locale reload keeps the previous valid messages active.

```yaml
managed-worlds-version: 1

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
      keep-world-config: true
      keep-gamerules: true
      keep-world-border: true
    evacuation:
      enabled: true
      destination: world

teleport-worlds: {}
```

The GUI owns `managed-worlds.yml` and may reformat it. Existing combined v5 configurations are imported automatically when this file is first created. After editing YAML manually, run `/rwr reload` and check the calculated next run with `/rwr status <id>`.
