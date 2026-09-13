package io.github.tamawish.rwr.config;

import java.util.Locale;

/** Supported evacuation transports. Overworld and hub are presets, not transports. */
public enum EvacuationDestinationType {
  DEFAULT_WORLD,
  LOCAL_WORLD,
  PROXY_SERVER,
  REGISTERED_PROVIDER;

  public String configKey() {
    return name().toLowerCase(Locale.ROOT).replace('_', '-');
  }

  public static EvacuationDestinationType parse(String value) {
    return valueOf(value.toUpperCase(Locale.ROOT).replace('-', '_'));
  }
}
