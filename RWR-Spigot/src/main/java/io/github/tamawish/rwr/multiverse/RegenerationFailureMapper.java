package io.github.tamawish.rwr.multiverse;

/** Maps Multiverse regeneration failures to provider-neutral failure categories. */
public final class RegenerationFailureMapper {
  private RegenerationFailureMapper() {}

  /**
   * Converts a Multiverse failure reason.
   *
   * @param reason Multiverse failure reason
   * @return provider-neutral failure reason
   */
  public static RegenerationFailureReason map(
      org.mvplugins.multiverse.core.world.reasons.RegenFailureReason reason) {
    return switch (reason) {
      case DELETE_FAILED -> RegenerationFailureReason.DELETE_FAILED;
      case CREATE_FAILED -> RegenerationFailureReason.CREATE_FAILED;
    };
  }
}
