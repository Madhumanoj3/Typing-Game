package util;

import game.ThemeManager;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

/**
 * Helper utility to apply the current theme to JavaFX dialogs.
 */
public class DialogThemeHelper {

    /**
     * Applies the current theme to a dialog.
     * Call this after creating any Dialog, Alert, or custom dialog.
     */
    public static void applyTheme(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        if (pane == null) return;

        ThemeManager tm = ThemeManager.getInstance();
        boolean isLight = tm.isPrintLightTheme();

        if (isLight) {
            // Light theme styling
            pane.setStyle(
                "-fx-background-color: #ffffff;" +
                "-fx-font-family: '" + ThemeManager.fontFamily() + "';"
            );
            
            // Style header
            pane.lookup(".header-panel").setStyle(
                "-fx-background-color: #fef3c7;" +
                "-fx-text-fill: #111827;"
            );
            
            // Style content
            pane.lookup(".content").setStyle(
                "-fx-background-color: #ffffff;" +
                "-fx-text-fill: #374151;"
            );
            
            // Style buttons
            pane.lookupAll(".button").forEach(node -> {
                String currentStyle = node.getStyle();
                if (currentStyle.contains("#7c3aed") || currentStyle.contains("#a78bfa")) {
                    node.setStyle(currentStyle.replace("#1a1a2e", "#ffffff")
                        .replace("#0f0f1a", "#fff7fb")
                        .replace("#a78bfa", "#7c3aed")
                        .replace("white", "#111827"));
                }
            });
        } else {
            // Dark theme styling
            pane.setStyle(
                "-fx-background-color: " + ThemeManager.card() + ";" +
                "-fx-font-family: '" + ThemeManager.fontFamily() + "';"
            );
        }
    }
}
