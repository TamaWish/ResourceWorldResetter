---
title: Threading and lifecycle
description: Use RWR snapshots and events safely across Bukkit, Paper, and Folia.
---
Snapshot queries support concurrent reads, but consumers must still follow their server platform’s threading rules for Bukkit operations. Keep event callbacks short and move network, file, or database I/O away from server-owned threads.

The service appears after RWR enables successfully and is removed when RWR disables. Never retain an API instance across disable, reload, or plugin-manager lifecycle changes; call `RwrApi.find(server)` again.
