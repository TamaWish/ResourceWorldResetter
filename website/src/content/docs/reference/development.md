---
title: Integration development
description: Build an RWR add-on using the public, read-only API.
---

RWR 5.2.0 bundles public API 5.1.2. The API exposes immutable snapshots and reset lifecycle events; it does not allow integrations to trigger resets or mutate RWR configuration.

```xml
<dependency>
  <groupId>io.github.tamawish</groupId>
  <artifactId>rwr-api</artifactId>
  <version>5.1.2</version>
  <scope>provided</scope>
</dependency>
```

Do not shade or relocate `rwr-api`. For optional support across both runtimes, declare:

```yaml
softdepend: [ResourceWorldResetter, ResourceWorldResetter-Paper-Folia]
```

Discover the service after RWR enables:

```java
RwrApi api = getServer().getServicesManager().load(RwrApi.class);
if (api == null) return;
```

## Events and threading

- `ResourceWorldResetWarningEvent` reports an eligible scheduled warning.
- `ResourceWorldPreResetEvent` is cancellable during its callback.
- `ResourceWorldPostResetEvent` reports the terminal outcome; inspect failure and safety fields instead of assuming success.

Keep listeners short, send external I/O asynchronously, and use the correct server or entity scheduler before accessing mutable Bukkit state. Reacquire the service after RWR lifecycle changes; snapshots are immutable point-in-time values, not permission to access a live world from any thread.

For full signatures and examples, see the [RWR API repository](https://github.com/TamaWish/RWR-API).
