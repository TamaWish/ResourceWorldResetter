package io.github.tamawish.rwr.bootstrap;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Checks the canonical RWR GitHub release feed without blocking a server thread. */
public final class GithubReleaseChecker {
    public static final String MODRINTH_URL = "https://modrinth.com/plugin/resourceworldresetter";
    private static final URI LATEST_RELEASE = URI.create(
            "https://api.github.com/repos/TamaWish/ResourceWorldResetter/releases/latest");

    private final HttpClient client;

    public GithubReleaseChecker() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    GithubReleaseChecker(HttpClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    public CompletableFuture<Result> check(PluginVersion installed) {
        HttpRequest request = HttpRequest.newBuilder(LATEST_RELEASE)
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "ResourceWorldResetter-update-checker")
                .GET()
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> parseResponse(installed, response.statusCode(), response.body()))
                .exceptionally(error -> Result.failure("GitHub update check failed: " + safeMessage(error)));
    }

    static Result parseResponse(PluginVersion installed, int statusCode, String body) {
        if (statusCode != 200) {
            return Result.failure("GitHub update check returned HTTP " + statusCode + '.');
        }
        try {
            JsonObject release = JsonParser.parseString(body).getAsJsonObject();
            if (release.get("draft").getAsBoolean() || release.get("prerelease").getAsBoolean()) {
                return Result.failure("GitHub latest release was not a stable release.");
            }
            String tag = release.get("tag_name").getAsString();
            PluginVersion latest = PluginVersion.parse(tag);
            return latest.compareTo(installed) > 0
                    ? Result.updateAvailable(installed, latest)
                    : Result.upToDate(installed, latest);
        } catch (RuntimeException exception) {
            return Result.failure("GitHub update response could not be read: " + safeMessage(exception));
        }
    }

    private static String safeMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

    public record Result(Status status, PluginVersion installed, PluginVersion latest, String message) {
        static Result updateAvailable(PluginVersion installed, PluginVersion latest) {
            return new Result(Status.UPDATE_AVAILABLE, installed, latest, "Update available");
        }

        static Result upToDate(PluginVersion installed, PluginVersion latest) {
            return new Result(Status.UP_TO_DATE, installed, latest, "Up to date");
        }

        static Result failure(String message) {
            return new Result(Status.FAILURE, null, null, message);
        }
    }

    public enum Status { UPDATE_AVAILABLE, UP_TO_DATE, FAILURE }
}
