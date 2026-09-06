---
title: Delivery and retries
description: How Discord messages survive temporary network and rate-limit failures.
---
Webhook requests run asynchronously and never block a Bukkit or Folia server thread. Pending payloads are kept in bounded `pending-webhooks.json`; the webhook secret is never written to that queue.

- Network errors and HTTP 5xx responses are retried.
- HTTP 429 follows Discord’s exact `Retry-After` value.
- Other HTTP 4xx responses are not retried.
- Exponential backoff with jitter is capped at five minutes.
- Defaults allow eight attempts and discard entries older than 24 hours.
- Pending work resumes after restart.

Warning embeds contain the world and scheduled time. Terminal embeds include the operation ID, outcome, phase, failure classification, detail, and timestamp.
