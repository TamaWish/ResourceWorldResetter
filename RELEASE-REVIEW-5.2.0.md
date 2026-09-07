# 5.2.0 code review — 2026-09-05

Reviewed the shared core, Bukkit API adapter, Spigot/Multiverse integration, and Paper/Folia/Worlds integration. Existing uncommitted 5.2.0 work was preserved. No release was published.

## Corrections

- Teleport menu construction now enumerates the world catalog once and builds the override index once per request. Known names reuse snapshot identities. Clicks obtain fresh state and revalidate permissions, reset blocking, and world availability. A 100-world regression case verifies the lookup reduction; this is not a live latency benchmark.

- An immediate exception from `regenerateAsync` now requires review rather than permitting an automatic retry after regeneration may have started.
- Synchronous reset event failures no longer replace a successful reset result or escape the coordinator.
- Reload and lifecycle reconciliation preserve active schedule entries. Duplicate manual requests return busy without replacing the active operation's schedule token.
- Exceptional executor calls produce terminal outcomes. Ambiguous failures stop automation across reloads. Startup restores holds from the latest retained history entries; an explicit manual reset can resume scheduling.
- World protection, duplicate configuration detection, evacuation validation, and teleport overrides compare canonical identities. Namespaced overrides now control the discovered destination without duplicating it or bypassing its permission.
- Paper/Folia reset commands dispatch on the global scheduler. Worlds regeneration also independently rejects the server default world.
- Worlds lookup resolves actual Bukkit names and namespaced keys without falling back from an arbitrary custom namespace to an unrelated bare name. Unloaded registry snapshots retain the matched key.
- Worlds deletion matching no longer uses unrelated RWR IDs or drops namespace distinctions. Deletion events during an active RWR reset do not remove its managed configuration.
- Locale reload and rendering are synchronized. Bundled English defaults supply missing keys, and unused message-loading methods were removed.
- CI discovers current platform JARs instead of hardcoding 5.1.0 and resolves the declared published API dependency instead of installing another repository's moving main branch.

## Verification

- `mvn --batch-mode --no-transfer-progress clean verify`: passed across all five reactor projects on 2026-09-07.
- 121 tests passed; zero failures, errors, or skipped tests.
- Both output JARs declare version 5.2.0-beta.1 and contain the public API without duplicate anchor classes.
- `git diff --check`: passed.
- API dependency remains 5.1.2. Provider dependencies remain Multiverse 5.8.0 and Worlds 4.4.0.

## Live staging — 2026-09-07

- The Spigot artifact started on Paper 1.21.4 with Multiverse-Core 5.8.0, loaded one managed world, completed a RANDOM regeneration, retained readable history, reloaded configuration, preserved its next schedule, and shut down cleanly.
- The Paper/Folia artifact completed the same startup, regeneration, history, reload, schedule, and shutdown sequence on Paper 26.1.2 with Worlds 4.4.0.
- The Paper/Folia artifact completed the same sequence on Folia 26.1.2 with Worlds 4.4.0 without a region-thread or watchdog error.
- A negative compatibility run proved that Worlds 4.4.0 cannot load on Paper 1.21.4 (`Unsupported class file major version 69`). Documentation now sets the Paper/Folia floor to Minecraft 26.1.2 and Java 25, matching the provider's published metadata.
- Staging also exposed and corrected an invalid managed-world documentation example that omitted the required `keep-world-config`, `keep-gamerules`, and `keep-world-border` fields.

## Before publishing

Console-driven live reset behavior is validated on all three runtime paths. Player-dependent checks still require a disposable client harness: players in the source world, cancelled teleport handling, GUI input, and locale reload from multiple Folia regions. Reload-during-regeneration and forced restart-recovery fault injection also remain destructive/manual staging scenarios.

Safety holds restored at startup are limited to retained reset history (currently 100 entries). A regeneration timeout reports an ambiguous result; it does not prove that the upstream operation has stopped. Review upstream world state before manually retrying.

## References

The scheduler review used [Paper's Folia support documentation](https://docs.papermc.io/paper/dev/folia-support/) and [teleportation documentation](https://docs.papermc.io/paper/dev/entity-teleport/). Integration context came from the [Worlds API documentation](https://thenextlvl.net/docs/worlds/api). The supplied Maven directory and Worlds 4.4.0 Javadoc URLs could not be opened by the web tool; compilation used the locally available Maven dependencies.
