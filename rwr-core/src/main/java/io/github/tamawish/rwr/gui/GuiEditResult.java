package io.github.tamawish.rwr.gui;

import io.github.tamawish.rwr.config.ConfigIssue;
import java.util.List;

public record GuiEditResult(boolean accepted, String message, List<ConfigIssue> issues) {
    public GuiEditResult {
        issues = List.copyOf(issues);
    }

    public static GuiEditResult accepted(String message) {
        return new GuiEditResult(true, message, List.of());
    }

    public static GuiEditResult rejected(String message) {
        return new GuiEditResult(false, message, List.of());
    }
}
