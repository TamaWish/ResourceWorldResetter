package com.lozaine.resourceworldresetter.gui;

import com.lozaine.resourceworldresetter.config.ConfigIssue;
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
