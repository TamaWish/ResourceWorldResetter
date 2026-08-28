package com.lozaine.resourceworldresetter.bootstrap;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record PluginVersion(int major, int minor, int patch) implements Comparable<PluginVersion> {
    private static final Pattern LEADING_VERSION = Pattern.compile("^v?(\\d+)\\.(\\d+)(?:\\.(\\d+))?.*$", Pattern.CASE_INSENSITIVE);

    public static PluginVersion parse(String value) {
        Objects.requireNonNull(value, "value");
        Matcher matcher = LEADING_VERSION.matcher(value.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unrecognised version: " + value);
        }
        int patch = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
        return new PluginVersion(
                Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), patch);
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
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
