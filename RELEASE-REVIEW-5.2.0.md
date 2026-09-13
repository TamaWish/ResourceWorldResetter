# 5.2.0 code review — updated 2026-09-11

## Current unreleased evacuation implementation — 2026-09-11

- Added typed default-world/local-world/proxy-server/registered-provider destinations, global defaults, legacy scalar migration, timeout controls, and localized paginated selectors on both platforms.
- Added the public API 5.2.0 `EvacuationProvider` contract in the sibling `RWR-API` repository and bundled it in both runtime artifacts. Existing snapshot/event contracts remain compatible.
- Shared orchestration waits for completed transfers and source-world departure; proxy send is only a request. Missing/duplicate/removed providers, failed transfers, and timeouts abort before regeneration.
- Runtime `mvn verify`: **156 tests passed**, zero failures/errors/skips; formatting checks passed.
- API `mvn verify`: **11 tests passed**, zero failures/errors/skips; Javadoc, dependency analysis, and formatting checks passed.
- Both shaded JARs declare `5.2.0` and contain exactly one `EvacuationProvider.class`.
- SHA-256 (`RWR-Spigot-5.2.0.jar`): `227ebb6eb0b4a580038e8a459e4773b0ea6975a65ff74dd5b7db51f5b3f4a06e`.
- SHA-256 (`RWR-Paper-Folia-5.2.0.jar`): `056f9170ba070805993edb6679c5100233d2593963ed3459b44819a681c21a08`.

### Before public release

- Stage real player transfers with Worlds/Folia and Multiverse/Spigot, Velocity, a BungeeCord-compatible proxy, and a sample third-party provider. Include timeout/rejection, disconnect, mixed results, and no-regeneration-while-occupied cases. Automated scheduler/transfer tests passed; these new artifacts have not undergone live player/proxy staging.
- Publish API 5.2.0 before the runtime release so runtime-only CI and add-on builds can resolve the declared dependency. Local verification used the matching API checkout installed in Maven's local cache. Nothing was published by this implementation.
- Prior test counts, hashes, and live results below are historical and do not validate the new evacuation implementation.

Reviewed the shared core, Bukkit API adapter, Spigot/Multiverse integration, and Paper/Folia/Worlds integration. Existing uncommitted 5.2.0 work was preserved.

## Corrections

- Teleport menu construction now enumerates the world catalog once and builds the override index once per request. Known names reuse snapshot identities. Clicks obtain fresh state and revalidate permissions, reset blocking, and world availability. A 100-world regression case verifies the lookup reduction; this is not a live latency benchmark.
- Modernized GameRule capture and restoration in Paper/Folia `WorldsWorldProvider.PreservedWorldState` to use Bukkit's typed `GameRule<?>` and `World.setGameRule()`.
- Standardized Folia evacuation destination resolution in `FoliaPlayerEvacuationService` across both synchronous and asynchronous paths.
- Aligned Spigot `AdminGuiService.reload()` with Paper/Folia by injecting `UpdateService`, reloading message catalogs, and reporting localized status.
- Unified Spigot `MessageService` loading pathway to use a consistent four-layer fallback hierarchy (`bundled en_US` -> `disk en_US` -> `bundled selected locale` -> `disk selected locale`).
- Added missing reload status keys (`reload-success` and `reload-failed`) across all bundled Spigot locale files (`en_US`, `ja_JP`, `ko_KR`, `zh_CN`).
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

- `mvn --batch-mode --no-transfer-progress clean package`: passed across all five reactor projects on 2026-09-10.
- 129 tests passed; zero failures, errors, or skipped tests.
- Spotless code formatting check passed (`mvn spotless:check`).
- Both output JARs declare version 5.2.0 and contain the public API without duplicate anchor classes.
- Release SHA-256 hashes:
  - Spigot (`RWR-Spigot-5.2.0.jar`): `3EC158586D25B9C66A787C4382E88FE20AAF19530F4E40D51E1C60C3BD039566`
  - Paper/Folia (`RWR-Paper-Folia-5.2.0.jar`): `84F7612B504E0BCF2A839B9FB0D79AE7FE06292555BAEDB73811EF7EECD7FEAF`
- `git diff --check`: passed.
- That earlier baseline used API 5.1.2. Current evacuation work uses API 5.2.0; provider dependencies remain Multiverse 5.8.0 and Worlds 4.4.0.

## Live staging — 2026-09-09

- Paper 26.2 build 121 with Worlds 4.4.0 passed startup, provider discovery, a console-driven RANDOM regeneration, independent verification, history, reload with schedule preservation, and clean shutdown. The reset ran from 21:53:34 to 21:53:36 without a tick-stall warning.
- Folia 26.2 build 7 with Worlds 4.4.0 passed the same cycle. The reset ran from 21:58:55 to 21:58:56 without a region-thread violation, watchdog error, or tick-stall warning.
- Spigot 26.2 Jenkins build 4647 with Multiverse-Core 5.8.0 passed the same functional cycle, but its synchronous provider regeneration blocked the server thread from 22:44:30 to 22:44:36. Spigot reported `Running 5410ms or 108 ticks behind`. Compatibility passes; a zero-lag claim does not.
- All three tests used Eclipse Temurin Java 25.0.4.1. The staged `5.2.0-rc.1` RWR hashes were `3B6789E4523570BE68642772E7397E37F485E2F00EBD4214806918D1015474CB` (Spigot) and `6C2F758858E702D0C7BF25C3FDE007E8ACFAFCA67BC22D3B74668E26CC8A7B2C` (Paper/Folia); they are historical and do not validate `5.2.0-rc.2`.
- Server hashes were `65D0848A22B9451A460948CBDF1199B78649EA6F52BDDDB0D0434846C783727C` (Spigot), `0DE30EFB024BC8B83C9C7D507D11802897AD8056B6110EC09FE1A91D126CCB54` (Paper), and `128A634192261CD38BB4A5DC54075018A0F896FD6C6F529E37DCA6E99E32B3B3` (Folia). Provider hashes were `C527D9E21A25A71CB2442AC1F1BFD3A8A1EFB7D89E0CB0E6A94F600304FDE6C1` (Multiverse-Core) and `EDCE34008999B6BEA547F700F2D1E6EBB67A7AEE86CE290009FD5D58E858415E` (Worlds).
- Paper, Folia, and Spigot emitted Windows OSHI/Perflib system-information warnings unrelated to RWR. Multiverse-Core also logged harmless missing-Paper-event warnings on Spigot.

## Earlier live staging — 2026-09-07

- The Spigot artifact started on Paper 1.21.4 with Multiverse-Core 5.8.0, loaded one managed world, completed a RANDOM regeneration, retained readable history, reloaded configuration, preserved its next schedule, and shut down cleanly.
- The Paper/Folia artifact completed the same startup, regeneration, history, reload, schedule, and shutdown sequence on Paper 26.1.2 with Worlds 4.4.0.
- The Paper/Folia artifact completed the same sequence on Folia 26.1.2 with Worlds 4.4.0 without a region-thread or watchdog error.
- A negative compatibility run proved that Worlds 4.4.0 cannot load on Paper 1.21.4 (`Unsupported class file major version 69`). Documentation now sets the Paper/Folia floor to Minecraft 26.1.2 and Java 25, matching the provider's published metadata.
- Staging also exposed and corrected an invalid managed-world documentation example that omitted the required `keep-world-config`, `keep-gamerules`, and `keep-world-border` fields.

## Historical pre-publication build — 2026-09-10

The clean `5.2.0` release build passed all 129 tests on 2026-09-10. Its SHA-256 hashes are `0399D455ED235149348C62FA4B2A4873887F4047F2C2E74D55246A443945181B` (Spigot) and `E0FCF7D114FCC9D0E3D833B36B618547556B3B3C29C9A0BC0EE86700B2E0CC58` (Paper/Folia). Both embedded plugin descriptors declare `5.2.0`.

The `5.2.0` code-level gates cover Spigot's full teleport-permit lifetime, Paper/Folia's asynchronous destination-resolution permit lifetime, and the coordinator's asynchronous remaining-player barrier. Failed/null Worlds regeneration callbacks complete directly instead of trying to schedule against a possibly deleted source world. Safe evacuation fallbacks are active across both platforms.

Console-driven live reset behavior is validated on all three runtime paths. Player-dependent checks still require a disposable client harness: players in the source world, cancelled teleport handling, GUI input, and locale reload from multiple Folia regions. Reload-during-regeneration and forced restart-recovery fault injection also remain destructive/manual staging scenarios.

Safety holds restored at startup are limited to retained reset history (currently 100 entries). A regeneration timeout reports an ambiguous result; it does not prove that the upstream operation has stopped. Review upstream world state before manually retrying.

## References

The scheduler review used [Paper's Folia support documentation](https://docs.papermc.io/paper/dev/folia-support/) and [teleportation documentation](https://docs.papermc.io/paper/dev/entity-teleport/). Integration context came from the [Worlds API documentation](https://thenextlvl.net/docs/worlds/api). The supplied Maven directory and Worlds 4.4.0 Javadoc URLs could not be opened by the web tool; compilation used the locally available Maven dependencies.
