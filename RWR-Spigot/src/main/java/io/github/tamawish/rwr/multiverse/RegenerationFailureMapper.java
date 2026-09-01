package io.github.tamawish.rwr.multiverse;

public final class RegenerationFailureMapper {
    private RegenerationFailureMapper() {}

    public static RegenerationFailureReason map(
            org.mvplugins.multiverse.core.world.reasons.RegenFailureReason reason) {
        return switch (reason) {
            case DELETE_FAILED -> RegenerationFailureReason.DELETE_FAILED;
            case CREATE_FAILED -> RegenerationFailureReason.CREATE_FAILED;
        };
    }
}
