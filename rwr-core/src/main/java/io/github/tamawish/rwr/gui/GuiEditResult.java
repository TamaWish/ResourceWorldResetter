package io.github.tamawish.rwr.gui;

import io.github.tamawish.rwr.config.ConfigIssue;
import java.util.List;

/**
 * Reports whether an administrative configuration edit was accepted.
 *
 * @param accepted whether the edit was persisted and activated
 * @param message user-facing outcome message
 * @param issues validation issues that prevented activation
 */
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
