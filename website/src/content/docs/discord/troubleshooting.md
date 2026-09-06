---
title: Commands and troubleshooting
description: Check Discord delivery state, retries, and RWR API availability.
---
| Command | Permission | Purpose |
| --- | --- | --- |
| `/rwr discord reload` | `rwrdiscord.admin` | Reload config, locale, and webhook client |
| `/rwr discord status` | `rwrdiscord.admin` | Show API state, queue size, last result, and retries |

The legacy `/rwrdiscord reload|status` alias remains available, including when the main RWR command namespace is unavailable. For missing messages, check status, event toggles, Discord’s webhook validity, and outbound HTTPS access in that order.
