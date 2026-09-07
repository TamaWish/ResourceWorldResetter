package io.github.tamawish.rwr.reset;

/** Ordered lifecycle phases of a reset operation. */
public enum ResetPhase {
  IDLE,
  PRECHECK,
  EVACUATE,
  REGENERATE,
  VERIFY,
  COMPLETE,
  FAILED,
  INTERRUPTED;

  public boolean blocksIncomingRwrTeleports() {
    return this == PRECHECK || this == EVACUATE || this == REGENERATE || this == VERIFY;
  }
}
