package io.github.tamawish.rwr.multiverse;

import io.github.tamawish.rwr.world.SafeLocation;

public sealed interface DestinationResult permits DestinationResult.Available, DestinationResult.Unavailable {
    record Available(SafeLocation location, boolean adjusted) implements DestinationResult {}

    record Unavailable(DestinationFailureReason reason, String message) implements DestinationResult {}
}
