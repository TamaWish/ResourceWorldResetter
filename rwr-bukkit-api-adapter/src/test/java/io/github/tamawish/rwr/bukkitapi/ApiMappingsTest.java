package io.github.tamawish.rwr.bukkitapi;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.tamawish.rwr.reset.FailureSafety;
import io.github.tamawish.rwr.reset.ResetFailureType;
import io.github.tamawish.rwr.reset.ResetOutcome;
import io.github.tamawish.rwr.reset.ResetPhase;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ApiMappingsTest {
    @Test
    void mapsEveryOperationalState() {
        Set<io.github.tamawish.rwr.api.model.ManagedWorldState> mapped =
                Arrays.stream(io.github.tamawish.rwr.config.WorldOperationalState.values())
                        .map(ApiMappings::state)
                        .collect(Collectors.toSet());
        assertThat(mapped).containsExactlyInAnyOrder(
                io.github.tamawish.rwr.api.model.ManagedWorldState.values());
    }

    @Test
    void mapsEveryPhaseAndSafety() {
        assertThat(Arrays.stream(ResetPhase.values()).map(ApiMappings::phase).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(io.github.tamawish.rwr.api.model.ResetPhase.values());
        assertThat(Arrays.stream(FailureSafety.values()).map(ApiMappings::safety).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(io.github.tamawish.rwr.api.model.FailureSafety.values());
    }

    @Test
    void mapsEveryFailureWithoutGenericFallback() {
        Set<io.github.tamawish.rwr.api.model.ResetFailureType> mapped =
                Arrays.stream(ResetFailureType.values()).map(ApiMappings::failure).collect(Collectors.toSet());
        assertThat(mapped).containsExactlyInAnyOrder(
                io.github.tamawish.rwr.api.model.ResetFailureType.values());
        assertThat(ApiMappings.failure(ResetFailureType.MULTIVERSE_REJECTED))
                .isEqualTo(io.github.tamawish.rwr.api.model.ResetFailureType.PROVIDER_REJECTED);
        assertThat(ApiMappings.failure(ResetFailureType.MULTIVERSE_API_EXCEPTION))
                .isEqualTo(io.github.tamawish.rwr.api.model.ResetFailureType.PROVIDER_API_EXCEPTION);
    }

    @Test
    void createsTerminalPostEventWithMappedOutcome() {
        ResetOutcome outcome = new ResetOutcome(
                "operation",
                "resource",
                "resource_world",
                ResetPhase.FAILED,
                ResetFailureType.MULTIVERSE_CREATE_FAILED,
                FailureSafety.AMBIGUOUS_REVIEW_REQUIRED,
                "Provider failed");
        var event = ApiMappings.postEvent(outcome);
        assertThat(event.getWorldName()).isEqualTo("resource_world");
        assertThat(event.getFailure())
                .contains(io.github.tamawish.rwr.api.model.ResetFailureType.WORLD_CREATE_FAILED);
    }
}
