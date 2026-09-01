package io.github.tamawish.rwr.multiverse;

public sealed interface RegenerationOutcome
        permits RegenerationOutcome.Success, RegenerationOutcome.Rejected, RegenerationOutcome.Failed {
    record Success(WorldSnapshot world) implements RegenerationOutcome {}

    record Rejected(RegenerationRejectionReason reason, String message) implements RegenerationOutcome {}

    record Failed(RegenerationFailureReason reason, String upstreamReason, String message)
            implements RegenerationOutcome {}
}
