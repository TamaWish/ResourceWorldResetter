package com.lozaine.resourceworldresetter.multiverse;

import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;
import org.mvplugins.multiverse.core.world.options.RegenWorldOptions;

public final class RegenerationOptionsMapper {
    public RegenWorldOptions map(LoadedMultiverseWorld world, RegenerationRequest request) {
        RegenerationOptionsPlan plan = RegenerationOptionsPlan.from(request);
        RegenWorldOptions options = RegenWorldOptions.world(world)
                .keepWorldConfig(plan.keepWorldConfig())
                .keepGameRule(plan.keepGameRules())
                .keepWorldBorder(plan.keepWorldBorder());
        switch (plan.seedPolicy()) {
            case SAME -> {
                // Leaving the seed unset tells Multiverse to reuse the current seed.
            }
            case RANDOM -> options.randomSeed(true);
            case FIXED -> options.seed(plan.fixedSeed());
        }
        return options;
    }
}
