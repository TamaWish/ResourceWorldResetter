---
title: Recovery and troubleshooting
description: Respond safely when a reset fails or is interrupted.
---

RWR deliberately does not replay an ambiguous interrupted regeneration. A timeout does not prove the upstream provider stopped or completed.

1. Read the failure type and safety classification in `/rwr status` and `/rwr history 10`.
2. Inspect `logs/latest.log` and `reset-history.json` in the matching RWR data folder.
3. Confirm the world exists, is loaded, and has the expected identity in Multiverse-Core or Worlds.
4. Confirm no players remain in the resource world.
5. Retry only when RWR marks the outcome safe to retry.

### Common issues

- **Plugin will not enable:** check Java version, the chosen platform artifact, and its hard dependency.
- **World unavailable:** confirm the exact provider identity is registered and loaded.
- **Players remain:** validate the evacuation destination and inspect plugins that may cancel teleports.
- **Configuration rejected:** fix every reported validation problem; the prior active snapshot stays in service.
