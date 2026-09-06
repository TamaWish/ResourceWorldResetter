---
title: Discord configuration
description: Configure webhook identity, delivery limits, and RWR events.
---
```yaml
locale: en_US
webhook:
  url: "https://discord.com/api/webhooks/..."
  username: "RWR"
  avatar_url: "https://example.com/rwr.png"
  timeout-ms: 8000
  queue-capacity: 64
  queue-ttl-hours: 24
  max-attempts: 8
  min-interval-seconds: 1
events:
  warnings: true
  success: true
  failures: true
  cancellations: true
  interrupted: true
```
The add-on has its own locale files under `plugins/RWR-Discord-Webhook/locales/`. Mentions are disabled and mention syntax is stripped from localized text.
