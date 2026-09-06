---
title: Frequently asked questions
description: Short answers to common RWR operating questions.
---

## Does Paper/Folia use Multiverse-Core?

No. Paper, Purpur, and Folia use Worlds by TheNextLvl. Multiverse-Core belongs to the Spigot/CraftBukkit build.

## Should I install rwr-core or a separate API JAR?

No. The shared core and public API are bundled into each RWR platform artifact.

## Can RWR 5 load a v4 configuration?

No. Recreate values manually in a fresh v5 configuration. See [Operations & migration](../../reference/operations-and-migration/).

## Does RWR delete world folders directly?

No. It asks the selected provider to perform the authoritative lifecycle operation, then independently verifies the result.

## Can I hot reload the plugin?

No. Replace JARs and dependencies while the server is stopped, then do a full restart.
