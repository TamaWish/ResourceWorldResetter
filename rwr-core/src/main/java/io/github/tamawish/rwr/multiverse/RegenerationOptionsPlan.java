package io.github.tamawish.rwr.multiverse;

/**
 * Provider-neutral options retained while regenerating a world.
 *
 * @param seedPolicy policy used to select the next seed
 * @param fixedSeed explicit seed used by the fixed policy
 * @param keepWorldConfig whether provider world configuration is retained
 * @param keepGameRules whether game rules are retained
 * @param keepWorldBorder whether world-border settings are retained
 */
public record RegenerationOptionsPlan(
    SeedPolicy seedPolicy,
    Long fixedSeed,
    boolean keepWorldConfig,
    boolean keepGameRules,
    boolean keepWorldBorder) {
  /**
   * Copies provider options from a validated regeneration request.
   *
   * @param request source request
   * @return the provider options plan
   */
  public static RegenerationOptionsPlan from(RegenerationRequest request) {
    return new RegenerationOptionsPlan(
        request.seedPolicy(),
        request.fixedSeed(),
        request.keepWorldConfig(),
        request.keepGameRules(),
        request.keepWorldBorder());
  }
}
