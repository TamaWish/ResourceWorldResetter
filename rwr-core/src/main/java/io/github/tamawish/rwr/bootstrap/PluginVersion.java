package io.github.tamawish.rwr.bootstrap;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Comparable semantic version consisting of three non-negative numeric components.
 *
 * <p>Display text retains any original qualifier such as {@code -beta.1} or {@code -SNAPSHOT}.
 * Numeric comparison ignores that qualifier.
 *
 * @param major compatibility-breaking version component
 * @param minor feature version component
 * @param patch corrective version component
 * @param source original display text without a leading {@code v}
 */
public record PluginVersion(int major, int minor, int patch, String source)
    implements Comparable<PluginVersion> {
  private static final Pattern LEADING_VERSION =
      Pattern.compile("^v?(\\d+)\\.(\\d+)(?:\\.(\\d+))?.*$", Pattern.CASE_INSENSITIVE);

  /**
   * Creates a numeric-only version whose display text is {@code major.minor.patch}.
   *
   * @param major compatibility-breaking version component
   * @param minor feature version component
   * @param patch corrective version component
   */
  public PluginVersion(int major, int minor, int patch) {
    this(major, minor, patch, major + "." + minor + "." + patch);
  }

  /**
   * Parses a version from the leading numeric portion of a tag or version string.
   *
   * @param value version or tag beginning with major and minor components
   * @return parsed semantic version, using zero when the patch component is absent
   * @throws IllegalArgumentException if the value does not begin with a supported version
   */
  public static PluginVersion parse(String value) {
    Objects.requireNonNull(value, "value");
    String trimmed = value.trim();
    Matcher matcher = LEADING_VERSION.matcher(trimmed);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Unrecognised version: " + value);
    }
    int patch = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
    String source = trimmed.regionMatches(true, 0, "v", 0, 1) ? trimmed.substring(1) : trimmed;
    return new PluginVersion(
        Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), patch, source);
  }

  /**
   * Returns whether the original label includes a qualifier after the numeric version.
   *
   * @return {@code true} when the source contains {@code -} after the version numbers
   */
  public boolean isPreRelease() {
    return source.indexOf('-') >= 0;
  }

  @Override
  public int compareTo(PluginVersion other) {
    int majorResult = Integer.compare(major, other.major);
    if (majorResult != 0) {
      return majorResult;
    }
    int minorResult = Integer.compare(minor, other.minor);
    return minorResult != 0 ? minorResult : Integer.compare(patch, other.patch);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof PluginVersion version
        && major == version.major
        && minor == version.minor
        && patch == version.patch;
  }

  @Override
  public int hashCode() {
    return Objects.hash(major, minor, patch);
  }

  @Override
  public String toString() {
    return source;
  }
}
