package io.github.tamawish.rwr.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GithubReleaseCheckerTest {
    private static final PluginVersion INSTALLED = new PluginVersion(5, 0, 0);

    @Test
    void reportsNewerStableGithubRelease() {
        GithubReleaseChecker.Result result = GithubReleaseChecker.parseResponse(
                INSTALLED, 200, "{\"tag_name\":\"v5.0.1\",\"draft\":false,\"prerelease\":false}");

        assertThat(result.status()).isEqualTo(GithubReleaseChecker.Status.UPDATE_AVAILABLE);
        assertThat(result.latest()).isEqualTo(new PluginVersion(5, 0, 1));
        assertThat(GithubReleaseChecker.MODRINTH_URL).isEqualTo("https://modrinth.com/plugin/resourceworldresetter");
    }

    @Test
    void ignoresEqualAndOlderStableGithubReleases() {
        GithubReleaseChecker.Result equal = GithubReleaseChecker.parseResponse(
                INSTALLED, 200, "{\"tag_name\":\"5.0.0\",\"draft\":false,\"prerelease\":false}");
        GithubReleaseChecker.Result older = GithubReleaseChecker.parseResponse(
                INSTALLED, 200, "{\"tag_name\":\"4.9.9\",\"draft\":false,\"prerelease\":false}");

        assertThat(equal.status()).isEqualTo(GithubReleaseChecker.Status.UP_TO_DATE);
        assertThat(older.status()).isEqualTo(GithubReleaseChecker.Status.UP_TO_DATE);
    }

    @Test
    void rejectsPrereleaseMalformedAndFailedResponses() {
        GithubReleaseChecker.Result prerelease = GithubReleaseChecker.parseResponse(
                INSTALLED, 200, "{\"tag_name\":\"5.1.0\",\"draft\":false,\"prerelease\":true}");
        GithubReleaseChecker.Result malformed = GithubReleaseChecker.parseResponse(INSTALLED, 200, "not json");
        GithubReleaseChecker.Result failed = GithubReleaseChecker.parseResponse(INSTALLED, 503, "");

        assertThat(prerelease.status()).isEqualTo(GithubReleaseChecker.Status.FAILURE);
        assertThat(malformed.status()).isEqualTo(GithubReleaseChecker.Status.FAILURE);
        assertThat(failed.status()).isEqualTo(GithubReleaseChecker.Status.FAILURE);
    }
}
