---
title: PlaceholderAPI configuration
description: Configure caching, display formats, fallbacks, and localization.
---
```yaml
locale: en_US
cache-ttl-ms: 1500
datetime-format: "yyyy-MM-dd HH:mm:ss 'UTC'"
worlds-separator: ", "
boolean:
  true: "true"
  false: "false"
fallback: ""
no-api-fallback: ""
```

Configuration lives at `plugins/RWR-PlaceholderAPI/config.yml`. Locale files live under `plugins/RWR-PlaceholderAPI/locales/`; missing locale keys fall back to bundled English. Keep a short cache TTL for rapidly updating displays without querying RWR on every scoreboard tick.
