---
title: Grafana and Docker
description: Run the included Prometheus data source and RWR Grafana dashboard.
---
Set the exporter to a Docker-reachable address, then run from the extracted distribution:
```shell
docker compose -f demo/docker-compose.yml up -d
```
Open Prometheus at `http://localhost:9090`, Grafana at `http://localhost:3000`, and verify `up{job="rwr"}` returns `1`. The data source and RWR dashboard are provisioned automatically. The demo credentials are `admin` / `admin`; change them outside local testing.

Stop with `docker compose -f demo/docker-compose.yml stop`; remove the demo containers with `docker compose -f demo/docker-compose.yml down`.
