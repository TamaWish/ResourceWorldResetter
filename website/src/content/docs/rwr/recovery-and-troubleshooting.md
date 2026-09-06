---
title: Recovery and troubleshooting
description: Diagnose failed or interrupted reset operations without risking world data.
---
Start with `/rwr status`, the operation ID, the current phase, and the first relevant server error. Do not repeatedly retry an unknown failure. Confirm the configured world ID, provider state, evacuation destination, filesystem permissions, free space, and whether another plugin holds the world loaded.

After an interrupted shutdown, preserve the world folders and logs before taking manual action. Use the [Operations & migration](../../reference/operations-and-migration/) guide for detailed recovery procedures.
