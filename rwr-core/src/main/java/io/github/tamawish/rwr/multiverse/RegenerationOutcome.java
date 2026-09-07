package io.github.tamawish.rwr.multiverse;

/** Result of requesting world regeneration from a provider. */
public sealed interface RegenerationOutcome
    permits RegenerationOutcome.Success, RegenerationOutcome.Rejected, RegenerationOutcome.Failed {
  /**
   * Successful regeneration result.
   *
   * @param world snapshot of the regenerated world
   */
  record Success(WorldSnapshot world) implements RegenerationOutcome {}

  /**
   * A request rejected before mutation.
   *
   * @param reason machine-readable rejection reason
   * @param message diagnostic explanation
   */
  record Rejected(RegenerationRejectionReason reason, String message)
      implements RegenerationOutcome {}

  /**
   * A request that failed during provider mutation.
   *
   * @param reason normalized failure reason
   * @param upstreamReason provider-specific failure detail
   * @param message diagnostic explanation
   */
  record Failed(RegenerationFailureReason reason, String upstreamReason, String message)
      implements RegenerationOutcome {}
}
