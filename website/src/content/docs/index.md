---
title: ResourceWorldResetter
description: Safe, observable regeneration for Minecraft resource worlds.
template: splash
hero:
  tagline: Automate resource-world regeneration without treating it as a blind delete-and-recreate task.
  actions:
    - text: Install RWR
      link: /getting-started/
      icon: right-arrow
      variant: primary
    - text: Release notes
      link: /reference/release-notes/
      icon: right-arrow
---

<div class="hero">

# Reset worlds. Keep control.

<p class="tagline">ResourceWorldResetter safely evacuates players, delegates regeneration to the authoritative world provider, verifies the result, and records history for operators.</p>

</div>

## Choose the matching build

| Server | RWR artifact | Required world plugin | Java |
| --- | --- | --- | --- |
| Spigot / CraftBukkit | `RWR-Spigot-5.2.0.jar` | Multiverse-Core 5.8.0+ | 21+ |
| Paper / Purpur / Folia | `RWR-Paper-Folia-5.2.0.jar` | Worlds 4.4.0+ | 25+ |

Install exactly one RWR artifact. The shared core and public API are bundled; do not install `rwr-core` separately. Version 5.2.0 is currently a release candidate awaiting live validation.

## Built for live servers

- **Schedule precisely.** Daily, weekly, monthly, and interval schedules use a configured IANA timezone.
- **Evacuate safely.** RWR validates the destination and prevents regeneration while players remain.
- **Delegate correctly.** Multiverse-Core or Worlds remains the authoritative lifecycle provider.
- **Verify independently.** RWR checks registration, loaded state, identity, and safe spawn after completion.
- **Recover conservatively.** Ambiguous operations are recorded and paused instead of silently replayed.

<div class="card-grid">
  <a class="card" href="./getting-started/"><strong>Install and first reset</strong><span>Choose a platform and run a supervised reset.</span></a>
  <a class="card" href="./operator/configuration/"><strong>Configure worlds</strong><span>Set schedules, warnings, identities, and evacuation.</span></a>
  <a class="card" href="./operator/recovery-and-troubleshooting/"><strong>Recover safely</strong><span>Handle failed and interrupted operations.</span></a>
</div>
