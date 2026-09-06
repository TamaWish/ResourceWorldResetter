---
title: Snapshots
description: Read immutable managed-world and reset-status information.
---
The API exposes configuration IDs, platform world names, display names, operational states, reset capability, and case-insensitive lookup. Reset status includes its world, operation ID, phase, message, and active or terminal helpers.

Returned records and lists are immutable point-in-time snapshots. The API intentionally does not expose reset triggering, configuration mutation, reset history, scheduler control, Multiverse-Core objects, or Worlds objects.
