---
title: RWR FAQ
description: Short answers to common ResourceWorldResetter operator questions.
---
## Which jar should I install?
Spigot/CraftBukkit use the Spigot jar. Paper, Purpur, and Folia use the Paper-Folia jar. Never install both.

## Are add-ons built into RWR?
No. PlaceholderAPI, Discord Webhook, and Prometheus are separate plugins. The API is a compile-time dependency supplied at runtime by RWR.

## Can I reset a permanent world?
RWR is designed for disposable resource worlds. Do not configure a world whose contents must survive.

## Why is a countdown missing after restart?
Countdown integrations learn scheduled times from live warning events. They remain unknown until the next warning.
