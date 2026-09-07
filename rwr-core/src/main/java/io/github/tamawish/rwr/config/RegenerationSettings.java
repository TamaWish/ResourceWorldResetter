package io.github.tamawish.rwr.config;

import io.github.tamawish.rwr.multiverse.SeedPolicy;

/**
 * Controls seed selection and metadata preservation during regeneration.
 *
 * @param seedPolicy policy used to select the regenerated seed
 * @param fixedSeed fixed seed used by {@link SeedPolicy#FIXED}, otherwise {@code null}
 * @param keepWorldConfig whether provider configuration is preserved
 * @param keepGameRules whether game rules are restored
 * @param keepWorldBorder whether world-border settings are restored
 */
public record RegenerationSettings(
    SeedPolicy seedPolicy,
    Long fixedSeed,
    boolean keepWorldConfig,
    boolean keepGameRules,
    boolean keepWorldBorder) {}
