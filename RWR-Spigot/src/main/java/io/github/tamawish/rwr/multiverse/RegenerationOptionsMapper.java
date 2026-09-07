package io.github.tamawish.rwr.multiverse;

import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;
import org.mvplugins.multiverse.core.world.options.RegenWorldOptions;

/** Maps provider-neutral regeneration requests to Multiverse regeneration options. */
public final class RegenerationOptionsMapper {
  /**
   * Builds Multiverse regeneration options from a validated core request.
   *
   * @param world loaded Multiverse world
   * @param request provider-neutral regeneration request
   * @return mapped Multiverse options
   */
  public RegenWorldOptions map(LoadedMultiverseWorld world, RegenerationRequest request) {
    RegenerationOptionsPlan plan = RegenerationOptionsPlan.from(request);
    RegenWorldOptions options =
        RegenWorldOptions.world(world)
            .keepWorldConfig(plan.keepWorldConfig())
            .keepGameRule(plan.keepGameRules())
            .keepWorldBorder(plan.keepWorldBorder());
    switch (plan.seedPolicy()) {
      case SAME -> {
        // Leaving the seed unset tells Multiverse to reuse the current seed.
      }
      case RANDOM -> options.randomSeed(true);
      case FIXED -> options.seed(plan.fixedSeed());
      default -> throw new AssertionError("Unhandled seed policy: " + plan.seedPolicy());
    }
    return options;
  }
}
