package io.github.tamawish.rwr.multiverse;

import java.util.Objects;

/**
 * Requests regeneration of one provider world.
 *
 * @param worldName provider-specific world name
 * @param seedPolicy policy used to select the next seed
 * @param fixedSeed explicit seed required by the fixed policy
 * @param keepWorldConfig whether provider world configuration is retained
 * @param keepGameRules whether game rules are retained
 * @param keepWorldBorder whether world-border settings are retained
 */
public record RegenerationRequest(
    String worldName,
    SeedPolicy seedPolicy,
    Long fixedSeed,
    boolean keepWorldConfig,
    boolean keepGameRules,
    boolean keepWorldBorder) {
  /** Validates seed-policy invariants and the provider world name. */
  public RegenerationRequest {
    Objects.requireNonNull(worldName, "worldName");
    Objects.requireNonNull(seedPolicy, "seedPolicy");
    if (worldName.isBlank()) {
      throw new IllegalArgumentException("worldName must not be blank");
    }
    if (seedPolicy == SeedPolicy.FIXED && fixedSeed == null) {
      throw new IllegalArgumentException("fixedSeed is required for FIXED policy");
    }
    if (seedPolicy != SeedPolicy.FIXED && fixedSeed != null) {
      throw new IllegalArgumentException("fixedSeed is only valid for FIXED policy");
    }
  }
}
