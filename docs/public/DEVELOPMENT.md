# Integrating with RWR 5.2.0

The runtime bundles public API **5.1.2**, unchanged for 5.2.0. It exposes immutable snapshots and warning/reset events. It does not provide reset execution or configuration mutation.

## Dependency and discovery

```xml
<dependency>
  <groupId>io.github.tamawish</groupId>
  <artifactId>rwr-api</artifactId>
  <version>5.1.2</version>
  <scope>provided</scope>
</dependency>
```

Do not shade or relocate the API into an add-on. Server owners install one main RWR platform JAR and no separate API JAR.

For an optional integration supporting either runtime, declare:

```yaml
softdepend: [ResourceWorldResetter, ResourceWorldResetter-Paper-Folia]
```

These are two alternative plugin names. Listing both in `depend` would require both, although only one should be installed. A plugin targeting one required platform can depend on that platform's exact name.

Discover the service when RWR is available:

```java
RwrApi api = getServer().getServicesManager().load(RwrApi.class);
if (api == null) {
    return; // Optional integration is unavailable.
}
```

Import `io.github.tamawish.rwr.api.RwrApi`. Reacquire the service after provider registration or enable changes and discard it on unregister/disable. Avoid keeping an old service instance through a platform lifecycle change.

## Events and threads

- `ResourceWorldResetWarningEvent`: a scheduled warning, before an operation ID exists.
- `ResourceWorldPreResetEvent`: cancellable; cancel during the event callback to prevent that reset.
- `ResourceWorldPostResetEvent`: terminal outcome for an attempted operation. Inspect failure and safety information rather than assuming a post event means success.

Use the event/snapshot fields rather than parsing translated chat text. Keep callbacks short. Dispatch external I/O asynchronously, and use the correct server/entity scheduler before accessing mutable Bukkit state. Do not block an event callback waiting for another scheduler or future. A snapshot is a point-in-time view, not permission to access the corresponding live world from any thread.

In 5.2.0, active resets survive schedule reloads, and namespaced references are compared by world identity. Ambiguous outcomes pause automation; a timeout does not establish that upstream work stopped. These behaviors do not add mutating methods to the API.

See the [API repository](https://github.com/TamaWish/RWR-API) for full signatures and contract examples. To change the main plugin, start with [Contributing](../../CONTRIBUTING.md).
