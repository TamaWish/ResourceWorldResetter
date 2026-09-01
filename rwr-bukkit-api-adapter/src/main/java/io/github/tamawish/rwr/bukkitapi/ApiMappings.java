package io.github.tamawish.rwr.bukkitapi;

import io.github.tamawish.rwr.api.model.ManagedWorldSnapshot;
import io.github.tamawish.rwr.api.model.ResetStatusSnapshot;
import io.github.tamawish.rwr.config.ManagedWorldSettings;
import io.github.tamawish.rwr.reset.ResetOutcome;
import io.github.tamawish.rwr.reset.ResetStatus;
import java.util.Optional;

final class ApiMappings {
    private ApiMappings() {}

    static ManagedWorldSnapshot world(ManagedWorldSettings world) {
        return new ManagedWorldSnapshot(
                world.id(),
                world.multiverseWorld(),
                world.displayName(),
                state(world.state()),
                world.canReset());
    }

    static ResetStatusSnapshot status(ResetStatus status) {
        io.github.tamawish.rwr.api.model.ResetPhase phase = phase(status.phase());
        Optional<String> operationId = phase == io.github.tamawish.rwr.api.model.ResetPhase.IDLE
                ? Optional.empty()
                : Optional.of(status.operationId());
        return new ResetStatusSnapshot(
                status.worldId(), status.multiverseWorld(), phase, operationId, status.message());
    }

    static io.github.tamawish.rwr.api.model.ManagedWorldState state(
            io.github.tamawish.rwr.config.WorldOperationalState state) {
        return switch (state) {
            case MANAGED -> io.github.tamawish.rwr.api.model.ManagedWorldState.MANAGED;
            case DISABLED -> io.github.tamawish.rwr.api.model.ManagedWorldState.DISABLED;
            case PROTECTED -> io.github.tamawish.rwr.api.model.ManagedWorldState.PROTECTED;
            case ORPHANED -> io.github.tamawish.rwr.api.model.ManagedWorldState.ORPHANED;
        };
    }

    static io.github.tamawish.rwr.api.model.ResetPhase phase(
            io.github.tamawish.rwr.reset.ResetPhase phase) {
        return switch (phase) {
            case IDLE -> io.github.tamawish.rwr.api.model.ResetPhase.IDLE;
            case PRECHECK -> io.github.tamawish.rwr.api.model.ResetPhase.PRECHECK;
            case EVACUATE -> io.github.tamawish.rwr.api.model.ResetPhase.EVACUATE;
            case REGENERATE -> io.github.tamawish.rwr.api.model.ResetPhase.REGENERATE;
            case VERIFY -> io.github.tamawish.rwr.api.model.ResetPhase.VERIFY;
            case COMPLETE -> io.github.tamawish.rwr.api.model.ResetPhase.COMPLETE;
            case FAILED -> io.github.tamawish.rwr.api.model.ResetPhase.FAILED;
            case INTERRUPTED -> io.github.tamawish.rwr.api.model.ResetPhase.INTERRUPTED;
        };
    }

    static io.github.tamawish.rwr.api.model.ResetFailureType failure(
            io.github.tamawish.rwr.reset.ResetFailureType failure) {
        return switch (failure) {
            case UNKNOWN_WORLD_ID -> io.github.tamawish.rwr.api.model.ResetFailureType.UNKNOWN_WORLD_ID;
            case WORLD_NOT_MANAGED -> io.github.tamawish.rwr.api.model.ResetFailureType.WORLD_NOT_MANAGED;
            case WORLD_BUSY -> io.github.tamawish.rwr.api.model.ResetFailureType.WORLD_BUSY;
            case GLOBAL_RESET_BUSY -> io.github.tamawish.rwr.api.model.ResetFailureType.GLOBAL_RESET_BUSY;
            case EVENT_CANCELLED -> io.github.tamawish.rwr.api.model.ResetFailureType.EVENT_CANCELLED;
            case WORLD_NOT_REGISTERED ->
                    io.github.tamawish.rwr.api.model.ResetFailureType.WORLD_NOT_REGISTERED;
            case WORLD_NOT_LOADED -> io.github.tamawish.rwr.api.model.ResetFailureType.WORLD_NOT_LOADED;
            case WORLD_IDENTITY_CHANGED ->
                    io.github.tamawish.rwr.api.model.ResetFailureType.WORLD_IDENTITY_CHANGED;
            case EVACUATION_DISABLED ->
                    io.github.tamawish.rwr.api.model.ResetFailureType.EVACUATION_DISABLED;
            case EVACUATION_DESTINATION_UNAVAILABLE ->
                    io.github.tamawish.rwr.api.model.ResetFailureType.EVACUATION_DESTINATION_UNAVAILABLE;
            case EVACUATION_FAILED -> io.github.tamawish.rwr.api.model.ResetFailureType.EVACUATION_FAILED;
            case PLAYERS_REMAINING -> io.github.tamawish.rwr.api.model.ResetFailureType.PLAYERS_REMAINING;
            case MULTIVERSE_REJECTED -> io.github.tamawish.rwr.api.model.ResetFailureType.PROVIDER_REJECTED;
            case MULTIVERSE_DELETE_FAILED ->
                    io.github.tamawish.rwr.api.model.ResetFailureType.WORLD_DELETE_FAILED;
            case MULTIVERSE_CREATE_FAILED ->
                    io.github.tamawish.rwr.api.model.ResetFailureType.WORLD_CREATE_FAILED;
            case MULTIVERSE_API_EXCEPTION ->
                    io.github.tamawish.rwr.api.model.ResetFailureType.PROVIDER_API_EXCEPTION;
            case VERIFICATION_FAILED ->
                    io.github.tamawish.rwr.api.model.ResetFailureType.VERIFICATION_FAILED;
            case JOURNAL_UNAVAILABLE ->
                    io.github.tamawish.rwr.api.model.ResetFailureType.JOURNAL_UNAVAILABLE;
            case INTERRUPTED_OPERATION ->
                    io.github.tamawish.rwr.api.model.ResetFailureType.INTERRUPTED_OPERATION;
        };
    }

    static io.github.tamawish.rwr.api.model.FailureSafety safety(
            io.github.tamawish.rwr.reset.FailureSafety safety) {
        return switch (safety) {
            case SAFE_TO_RETRY -> io.github.tamawish.rwr.api.model.FailureSafety.SAFE_TO_RETRY;
            case AMBIGUOUS_REVIEW_REQUIRED ->
                    io.github.tamawish.rwr.api.model.FailureSafety.AMBIGUOUS_REVIEW_REQUIRED;
            case NOT_RETRYABLE -> io.github.tamawish.rwr.api.model.FailureSafety.NOT_RETRYABLE;
        };
    }

    static io.github.tamawish.rwr.api.event.ResourceWorldPostResetEvent postEvent(ResetOutcome outcome) {
        return new io.github.tamawish.rwr.api.event.ResourceWorldPostResetEvent(
                outcome.operationId(),
                outcome.worldId(),
                outcome.multiverseWorld(),
                phase(outcome.phase()),
                outcome.failure() == null ? null : failure(outcome.failure()),
                safety(outcome.safety()),
                outcome.message());
    }
}
