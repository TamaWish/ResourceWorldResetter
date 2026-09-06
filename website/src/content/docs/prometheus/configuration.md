---
title: Exporter configuration
description: Configure the exporter address, port, and metrics path.
---
```yaml
server:
  address: 127.0.0.1
  port: 9225
  metrics-path: /metrics
```
Use loopback when Prometheus runs directly on the Minecraft host. Docker usually requires a Docker-facing private address or `0.0.0.0`; if you use the latter, enforce access with the host firewall. An invalid address, port, path, or occupied port prevents the exporter from enabling.
