package io.github.tamawish.rwr.reset;

public sealed interface EvacuationResult permits EvacuationResult.Success, EvacuationResult.Failed {
    record Success(int evacuatedPlayers) implements EvacuationResult {}

    record Failed(ResetFailureType reason, int remainingPlayers, String message) implements EvacuationResult {}
}
