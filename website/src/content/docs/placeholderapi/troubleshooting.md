---
title: Commands and troubleshooting
description: Diagnose missing placeholders, stale countdowns, and unavailable integrations.
---
| Command | Permission | Purpose |
| --- | --- | --- |
| `/rwrplaceholder reload` | `rwrplaceholder.admin` | Reload config and locale |
| `/rwrplaceholder status` | `rwrplaceholder.admin` | Report API, expansion, and cache status |

Aliases: `/rwrpapi`, `/rwrph`.

If a value is blank, check status first, confirm PlaceholderAPI and the matching RWR runtime are enabled, verify the world ID, then inspect `fallback` and `no-api-fallback`. A missing countdown immediately after restart is expected until RWR publishes a new warning event.
