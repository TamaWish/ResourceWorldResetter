---
title: Service discovery
description: Find the RWR service and handle optional integration safely.
---
```java
RwrApi.find(getServer()).ifPresentOrElse(api -> {
    for (ManagedWorldSnapshot world : api.managedWorlds()) {
        getLogger().info(world.id() + " -> " + world.state());
    }
}, () -> getLogger().warning("RWR is unavailable or not ready"));
```
Use `depend` when RWR is required, or `softdepend` and handle an absent service for optional integration. Runtime names are `ResourceWorldResetter` on Spigot/CraftBukkit and `ResourceWorldResetter-Paper-Folia` on Paper/Purpur/Folia. Reacquire the service after lifecycle changes.
