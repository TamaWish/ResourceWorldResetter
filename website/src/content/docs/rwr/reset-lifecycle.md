---
title: Reset lifecycle
description: What RWR validates, evacuates, regenerates, and reports during a reset.
---
Every reset passes through validation, preparation, player evacuation, world unload, regeneration, reload, and terminal reporting. RWR refuses unsafe or incomplete work rather than silently continuing. Watch the operation ID and phase in status and logs; they connect warnings, failures, recovery decisions, and API events.

Never manually delete or replace the managed folder while an operation is active.
