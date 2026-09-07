package io.github.tamawish.rwr.worlds;

import io.github.tamawish.rwr.multiverse.RegenerationRequest;
import java.util.concurrent.ThreadLocalRandom;

record WorldsRegenerationPlan(Long seed, boolean ignoreLevelData) {
  static WorldsRegenerationPlan from(RegenerationRequest request, long currentSeed) {
    Long seed;
    switch (request.seedPolicy()) {
      case SAME -> seed = null;
      case FIXED -> seed = request.fixedSeed();
      case RANDOM -> seed = randomSeedOtherThan(currentSeed);
      default -> throw new AssertionError("Unhandled seed policy: " + request.seedPolicy());
    }
    return new WorldsRegenerationPlan(seed, !request.keepWorldConfig());
  }

  private static long randomSeedOtherThan(long currentSeed) {
    long candidate;
    do {
      candidate = ThreadLocalRandom.current().nextLong();
    } while (candidate == currentSeed);
    return candidate;
  }
}
