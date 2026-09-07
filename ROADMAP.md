# RWR Suite Roadmap

## 5.2.0 - release readiness and operator polish

- GitHub release checks, cached startup/admin-join notices, and `/rwr version`.
- Server-wide locale selection with bundled `en_US`, fallback, diagnostics, and reload retention.
- Command help and update diagnostics sourced from locale files.
- Configuration remains version 5; the embedded public API remains 5.1.2.
- RWR-PlaceholderAPI and RWR-Discord-Webhook retain independent versions unless compatibility work changes them.

### Implemented for the candidate

- Preserve active resets across reloads and duplicate manual requests; retain ambiguous-failure scheduling holds and restore them from retained history.
- Correct immediate regeneration exception handling, canonical world protection, Folia command scheduling, and Worlds deletion matching.
- Synchronize locale reload/rendering and keep bundled English fallback.
- Build each teleport menu from one world-catalog enumeration and one override index. Reuse names/identities within that request; revalidate permissions, reset state, and availability on each click.
- Update consumer, contributor, integration, and marketplace documentation for 5.2.0.

### Release gate — pending owner smoke tests

| Server | World provider | Runtime Java | Status |
| --- | --- | --- | --- |
| Spigot 1.21.11 | Multiverse-Core 5.8.0+ | 21+ or the server's higher requirement | Pending |
| Spigot 26.2 | Multiverse-Core 5.8.0+ | Server-required Java version | Pending |
| Paper 26.2 | Worlds 4.4.0+ | 25+ or the server's higher requirement | Pending |
| Folia 26.1.2 | Worlds 4.4.0+ | 25+ or the server's higher requirement | Pending |

Record exact server/provider builds, Java versions, candidate JAR hashes, and logs. Test commands and GUI input, evacuation with players present, reset cancellation/failure, reload during reset, recovery after restart, and locale/update behavior. Treat these versions as the intended test matrix until results exist.

Measure idle and reset-time tick duration, including peak and percentile values, with realistic world counts and concurrent menu users. A passing smoke test is not a lag-free guarantee. Fix reproducible release blockers before expanding features.

### Keep 5.2.0 changes focused

Prioritize confirmed correctness issues and small performance fixes backed by regression tests. Defer journal threading changes and broad shared-code refactors until their ordering, crash recovery, and platform-thread behavior can be tested independently.

## 5.2.1 - stabilization

Localization, update-checker, and platform-specific fixes only. Community locale contributions must pass YAML and MiniMessage validation.

- Address reproducible problems from the four-platform smoke matrix and early release reports.
- Use profiling evidence to prioritize performance regressions; avoid speculative caches that can weaken teleport or reset revalidation.

## 5.3.0 - additive capabilities

Consider scheduling, history, and management improvements after 5.2 stabilizes. Public API changes must be additive throughout RWR 5.x.

- Add optional chunk pre-generation for managed worlds after regeneration, using Chunky or an equivalent provider.
- Pre-generation integration must support Paper, Purpur, and Folia without blocking or violating Folia's region-threading model.
- Treat pre-generation as a tracked post-reset operation with configurable enablement, progress/status reporting, safe cancellation, and clear failure handling.
- Profile synchronous journal/configuration writes. Consider ordered asynchronous persistence only if recovery markers are acknowledged before destructive work and shutdown flush/failure behavior is verified.
- Reduce duplication between synchronous and asynchronous reset flows without changing failure classification, event ordering, or lock ownership.
- Consolidate repeated command/locale/GUI logic where practical, keeping Bukkit and Folia scheduling explicit.
- Investigate repeated provider metadata snapshots and canonical-name resolution using measured allocation/latency data; preserve live identity checks.
- Consider durable safety holds independent of the bounded history list, with an explicit operator recovery workflow.

## 6.0.0 - compatibility boundary

Reserve package moves, removals, incompatible API signatures, and configuration-format breaks for RWR 6.

### Proposed major addition: network operations

Cross-server evacuation and network-wide reset coordination belong to v6 or a later major release. They are outside the entire v5 scope. Architecture and delivery timing remain to be decided.

- Add an optional Velocity companion for player routing and coordination with RWR backend plugins. World regeneration remains owned by the backend and its world provider. Standalone servers must remain usable without a proxy.
- Support cross-server evacuation to a configured lobby or fallback server. Check destination readiness and capacity, confirm player arrival, and independently verify the source world is empty before regeneration. Abort safely when transfer confirmation is missing or fails.
- Prevent new arrivals and reconnects from entering a world while it is being evacuated or reset. Coordinate proxy routing with backend admission checks and define disconnect, fallback, and optional return-to-server behavior.
- Coordinate reset schedules across participating servers, with configurable concurrency limits, staggered operations, and a shared view of status and outcomes. Retain local backend safety checks even when a network coordinator authorizes a reset.
- Use authenticated communication, stable server/world identities, and unique operation IDs. Duplicate or delayed messages must not trigger another regeneration; stale coordinators must not retain reset authority.
- Define recovery for proxy/backend restarts, lost connections, and coordinator failover. Persist operation state, reconcile uncertain outcomes, and suspend destructive work when authority or evacuation state cannot be confirmed.
- Validate transfer rejection, unavailable/full lobbies, reconnect races, duplicate messages, network partitions, and failover before advertising network support. Introduce any required API/configuration changes at the major-version boundary.
