package io.github.tamawish.rwr.multiverse;

public record RegenerationOptionsPlan(
        SeedPolicy seedPolicy,
        Long fixedSeed,
        boolean keepWorldConfig,
        boolean keepGameRules,
        boolean keepWorldBorder) {
    public static RegenerationOptionsPlan from(RegenerationRequest request) {
        return new RegenerationOptionsPlan(
                request.seedPolicy(),
                request.fixedSeed(),
                request.keepWorldConfig(),
                request.keepGameRules(),
                request.keepWorldBorder());
    }
}
