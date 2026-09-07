package io.github.tamawish.rwr.reset;

/** Result of attempting to evacuate players before a reset. */
public sealed interface EvacuationResult permits EvacuationResult.Success, EvacuationResult.Failed {
  /**
   * Successful evacuation result.
   *
   * @param evacuatedPlayers number of players moved successfully
   */
  record Success(int evacuatedPlayers) implements EvacuationResult {}

  /**
   * An evacuation that left the reset unsafe to continue.
   *
   * @param reason normalized reset failure reason
   * @param remainingPlayers number of players still in the source world
   * @param message diagnostic explanation
   */
  record Failed(ResetFailureType reason, int remainingPlayers, String message)
      implements EvacuationResult {}
}
