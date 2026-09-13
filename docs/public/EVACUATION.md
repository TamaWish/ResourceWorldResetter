# Evacuation destinations in 5.2.0

RWR 5.2.0 supports four destination types. Configure them in `/rwr gui` or in a managed world's `evacuation` section in `managed-worlds.yml`:

```yaml
evacuation:
  enabled: true
  timeout-seconds: 30
  destination:
    type: proxy-server
    target: hub
```

| Type | Target | Requirements |
| --- | --- | --- |
| `default-world` | Ignored | Server default loaded world; cannot be the reset world. |
| `local-world` | Bukkit world name or Worlds key | Loaded destination; registered worlds use the world manager's safe destination, other loaded Bukkit worlds use spawn. |
| `proxy-server` | Configured backend name, such as `hub` | Velocity or a BungeeCord-compatible proxy with that backend configured. |
| `registered-provider` | Unique provider destination ID | Add-on implementing the public API 5.2.0 `EvacuationProvider` service. |

“Hub” is a friendly destination name: it can be a local world or a proxy backend. “Default overworld” selects `default-world`. Neither requires special hub-plugin support.

## Defaults and existing settings

The root `evacuation` section in `config.yml` has the same structure. It supplies new GUI-created worlds and loaded configurations without a per-world evacuation section. Without root defaults, RWR preserves the legacy enabled local destination from `default-hub-world`. The protected hub remains a local-world protection setting even when evacuation uses a proxy.

Per-world evacuation sections take precedence. Saving managed settings materializes the effective defaults as explicit per-world settings; later edits to global defaults do not replace those overrides. Partial per-world sections are not field-by-field overrides: supply `enabled` and `destination`; omitted `timeout-seconds` defaults to 30.

Legacy `destination: world` strings remain accepted as `local-world`. A managed-world save writes structured type/target values and the timeout. Configuration version remains 5. Back up configuration before editing as usual.

## GUI

Select a world and open its evacuation destination. Choose the default overworld, local world, proxy server, or plugin destination, or disable evacuation. The timeout editor accepts 1–120 seconds. Global settings expose the same selector for new-world defaults.

Local worlds are paginated and include world-manager registrations plus loaded Bukkit worlds. The source world and unloaded worlds cannot be selected. Proxy menus discover registered backends and also include saved targets and optional `proxy-servers` entries in `config.yml`:

```yaml
proxy-servers: [hub, lobby, survival]
```

Provider menus show enabled plugins' registered IDs and saved targets. Exactly one enabled registration makes an ID available; missing and duplicate IDs remain visible but disabled. Local, proxy, and provider lists each offer custom/manual input. Availability is rechecked before selecting a proxy/provider entry and when a reset starts. Command-based destinations are not implemented.

### Destination discovery

Opening the proxy evacuation selector requests `GetServers` through the connected administrator and immediately shows cached, configured, and saved targets. The matching open selector refreshes after a response while keeping its page and context. Current selections appear first. Missing or duplicate plugin providers are disabled; proxy targets absent from the latest response are unavailable (unverified before the first response). Custom/manual entry remains available, and saved global or managed-world targets remain visible on reopening.

Discovery is an in-memory cache for the plugin lifetime and never rewrites `config.yml`; `proxy-servers` supplies stable menu entries. If discovery times out, existing entries remain visible. It requires a connected player, a responding compatible proxy, and on Velocity `bungee-plugin-message-channel = true`. A listed backend proves proxy registration, not reachability: stopping a backend alone does not remove it from `GetServers`. Removing it from proxy registration and reopening the selector marks a saved target unavailable. Evacuation still waits for actual departure before regeneration. Configuration schema stays at 5 and the public API signatures are unchanged.

## Proxy setup

RWR sends the BungeeCord-compatible `Connect` message through the player being evacuated. No additional RWR proxy plugin is required. Velocity must have `bungee-plugin-message-channel = true`; the target must exactly match a backend name in the proxy configuration. See [Velocity channel compatibility](https://docs.papermc.io/velocity/dev/plugin-messaging/#bungeecord-channel-compatibility) and [Paper plugin messaging](https://docs.papermc.io/paper/dev/plugin-messaging/).

The destination name represents a separate backend server. Velocity does not create that server, and Worlds or Multiverse only creates worlds inside an existing backend. For a first setup, run Velocity, the RWR resource backend, and a separate lobby backend on different ports. On every Paper/Folia backend, set `online-mode=false`, configure the same Velocity forwarding secret, keep `settings.bungeecord: false`, and set `proxies.velocity.enabled: true` in `config/paper-global.yml`. This last setting is easy to miss on a new lobby; when false, Velocity rejects connections because the backend did not send a forwarding request.

Start destination backends before the resource backend and proxy, and wait for each to finish startup. Players connect only through Velocity. When all processes run on one machine, bind backend ports to `127.0.0.1` and expose only the Velocity listener.

A sent message is only a request. RWR polls for departure without blocking a server scheduler. An unavailable backend, a rejected connection, or a standalone server without a proxy leaves the player present and causes a timeout. A disconnect also removes the player from the source world. RWR verifies source departure, not arrival at the remote backend.

## Provider integration

Compile against `io.github.tamawish:rwr-api:5.2.0` with provided scope. Implement `EvacuationProvider.destinationId()` and `evacuate(UUID)`, register using `provider.register(ownerPlugin)`, and optionally remove using `provider.unregister(ownerPlugin)`. Bukkit removes registrations on plugin disable. Use a unique stable ID such as `myplugin:lobby`; duplicates abort rather than choosing an arbitrary provider.

RWR invokes the provider on the player's owning scheduler. Return a `CompletionStage<Boolean>` promptly and schedule subsequent Bukkit work correctly. Complete true when transfer succeeds or false/exceptionally when it fails. RWR also checks departure and provider availability. Provider removal, failure, or stalled completion aborts even if another action emptied the world. See [Integration Development](DEVELOPMENT.md).

## Safety and release validation

Paper/Folia teleports use `teleportAsync`; proxy/provider calls and fall-distance clearing run on player schedulers. Spigot uses the primary thread. Transfers and departure checks have a bounded timeout; no server thread waits for a network response. The coordinator and world provider retain their final occupancy checks before regeneration. Failures abort before regeneration, and existing safe-retry policy still applies.

Automated tests cover configuration migration, type serialization, defaults/overrides, source rejection, transfer failures, provider lifecycle, timeout, departure, and platform scheduling. Actual player transfers through Velocity/BungeeCord and occupied-world resets with Worlds/Folia and Multiverse/Spigot still require live staging of these new 5.2.0 artifacts before public release. Earlier staging results do not validate this feature.
