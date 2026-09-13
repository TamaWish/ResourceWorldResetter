package io.github.tamawish.rwr.reset;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

class ResetFailureTypeTest {
  @Test
  void legacyJournalFailuresReadAsProviderNeutralAndWriteWithNewNames() {
    Gson gson = new Gson();
    for (String suffix :
        new String[] {"REJECTED", "DELETE_FAILED", "CREATE_FAILED", "API_EXCEPTION"}) {
      ResetFailureType failure =
          gson.fromJson("\"MULTIVERSE_" + suffix + "\"", ResetFailureType.class);
      assertThat(failure).isEqualTo(ResetFailureType.valueOf("PROVIDER_" + suffix));
      assertThat(gson.toJson(failure)).isEqualTo("\"PROVIDER_" + suffix + "\"");
    }
  }
}
