---
title: Metrics and PromQL
description: RWR metric names, labels, meaning, and useful Prometheus queries.
---
| Metric | Meaning |
| --- | --- |
| `rwr_reset_attempts_total{world}` | Observed reset attempts |
| `rwr_reset_completions_total{world,result}` | Success, failure, and cancellation outcomes |
| `rwr_reset_duration_seconds{world}` | Reset duration histogram |
| `rwr_reset_in_progress{world}` | Active observed operations |
| `rwr_last_reset_timestamp_seconds{world}` | Latest terminal event time |
| `rwr_next_scheduled_reset_timestamp_seconds{world}` | Next reset learned from a warning |
| `rwr_exporter_build_info{version}` | Exporter build identity |

```text
up{job="rwr"}
sum by (world, result) (rate(rwr_reset_completions_total[15m]))
histogram_quantile(0.95, sum by (world, le) (rate(rwr_reset_duration_seconds_bucket[1h])))
```
Labels use canonical configured world IDs; player names, paths, seeds, and exception text are never labels.
