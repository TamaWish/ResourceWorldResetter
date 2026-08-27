package com.lozaine.resourceworldresetter.reset;

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
        return this == EVACUATE || this == REGENERATE || this == VERIFY;
    }
}
