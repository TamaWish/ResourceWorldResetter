package io.github.tamawish.rwr.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GithubReleaseCheckerTest {
  private static final PluginVersion INSTALLED = new PluginVersion(5, 0, 0);

  @Test
  void reportsNewerStableGithubRelease() {
    GithubReleaseChecker.Result result =
        GithubReleaseChecker.parseResponse(
            INSTALLED, 200, "{\"tag_name\":\"v5.0.1\",\"draft\":false,\"prerelease\":false}");

    assertThat(result.status()).isEqualTo(GithubReleaseChecker.Status.UPDATE_AVAILABLE);
    assertThat(result.latest()).isEqualTo(new PluginVersion(5, 0, 1));
    assertThat(GithubReleaseChecker.RELEASES_URL)
        .isEqualTo("https://github.com/TamaWish/ResourceWorldResetter/releases/latest");
  }

  @Test
  void ignoresEqualAndOlderStableGithubReleases() {
    GithubReleaseChecker.Result equal =
        GithubReleaseChecker.parseResponse(
            INSTALLED, 200, "{\"tag_name\":\"5.0.0\",\"draft\":false,\"prerelease\":false}");
    GithubReleaseChecker.Result older =
        GithubReleaseChecker.parseResponse(
            INSTALLED, 200, "{\"tag_name\":\"4.9.9\",\"draft\":false,\"prerelease\":false}");

    assertThat(equal.status()).isEqualTo(GithubReleaseChecker.Status.UP_TO_DATE);
    assertThat(older.status()).isEqualTo(GithubReleaseChecker.Status.UP_TO_DATE);
  }

  @Test
  void treatsMatchingStableReleaseAsUpdateForInstalledPreRelease() {
    PluginVersion beta = PluginVersion.parse("5.2.0-beta.1");
    GithubReleaseChecker.Result result =
        GithubReleaseChecker.parseResponse(
            beta, 200, "{\"tag_name\":\"v5.2.0\",\"draft\":false,\"prerelease\":false}");

    assertThat(result.status()).isEqualTo(GithubReleaseChecker.Status.UPDATE_AVAILABLE);
    assertThat(result.installed().toString()).isEqualTo("5.2.0-beta.1");
    assertThat(result.latest().toString()).isEqualTo("5.2.0");
  }

  @Test
  void rejectsPrereleaseMalformedAndFailedResponses() {
    GithubReleaseChecker.Result prerelease =
        GithubReleaseChecker.parseResponse(
            INSTALLED, 200, "{\"tag_name\":\"5.1.0\",\"draft\":false,\"prerelease\":true}");
    GithubReleaseChecker.Result malformed =
        GithubReleaseChecker.parseResponse(INSTALLED, 200, "not json");
    GithubReleaseChecker.Result failed = GithubReleaseChecker.parseResponse(INSTALLED, 503, "");

    assertThat(prerelease.status()).isEqualTo(GithubReleaseChecker.Status.FAILURE);
    assertThat(malformed.status()).isEqualTo(GithubReleaseChecker.Status.FAILURE);
    assertThat(failed.status()).isEqualTo(GithubReleaseChecker.Status.FAILURE);
  }

  @Test
  void comparesMultiDigitVersionsAndRejectsIncompletePayloads() {
    GithubReleaseChecker.Result multiDigit =
        GithubReleaseChecker.parseResponse(
            new PluginVersion(5, 9, 9),
            200,
            "{\"tag_name\":\"v5.10.0\",\"draft\":false,\"prerelease\":false}");
    GithubReleaseChecker.Result incomplete =
        GithubReleaseChecker.parseResponse(INSTALLED, 200, "{\"tag_name\":\"5.2.0\"}");

    assertThat(multiDigit.status()).isEqualTo(GithubReleaseChecker.Status.UPDATE_AVAILABLE);
    assertThat(incomplete.status()).isEqualTo(GithubReleaseChecker.Status.FAILURE);
  }
}
