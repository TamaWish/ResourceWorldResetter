---
title: Reset lifecycle
description: Understand the safeguards applied to every resource-world reset.
---

1. **PRECHECK** validates provider registration, loaded state, configuration, and recovery history.
2. **EVACUATE** moves players to a validated destination and blocks unsafe incoming RWR teleports.
3. **REGENERATE** calls the authoritative world provider while RWR retains reset locks.
4. **VERIFY** checks registration, loaded state, identity, and safe spawn.
5. **COMPLETE** or **FAILED** persists a terminal outcome and releases locks. A restart during an active operation produces an **INTERRUPTED** record instead of a replay.

On Folia, entity-region teleports finish asynchronously while the reset stays in `EVACUATE`; RWR must not block the global-region scheduler waiting for them.
