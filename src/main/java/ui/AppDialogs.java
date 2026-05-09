package ui;

import game.ThemeManager;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.stage.*;

/**
 * Attractive custom themed modal dialogs for all user-facing screens.
 * Automatically adapts to light/dark theme.
 */
public final class AppDialogs {

    private AppDialogs() {}

    // ── Public API ────────────────────────────────────────────────────────────

    /** Red "!" exclamatory error dialog. */
    public static void showError(String title, String message) {
        showStatus("!", title, message, "#ef4444", "rgba(239,68,68,0.14)", "rgba(239,68,68,0.40)");
    }

    /** Green "✓" success dialog. */
    public static void showSuccess(String title, String message) {
        showStatus("✓", title, message, "#34d399", "rgba(52,211,153,0.14)", "rgba(52,211,153,0.40)");
    }

    /** Cyan "i" info dialog. */
    public static void showInfo(String title, String message) {
        showStatus("i", title, message, "#38bdf8", "rgba(56,189,248,0.14)", "rgba(56,189,248,0.40)");
    }

    /** Confirmation dialog — returns true if user confirms. */
    public static boolean showConfirm(String title, String message,
                                      String detail, String confirmText, boolean danger) {
        Stage stage = buildStage(title);
        boolean[] result = {false};

        VBox root = buildRoot();

        StackPane iconWrap = buildIconCircle(danger ? "⚠" : "?",
                danger ? "#ef4444" : "#a78bfa",
                danger ? "rgba(239,68,68,0.14)" : "rgba(124,58,237,0.14)",
                danger ? "rgba(239,68,68,0.35)" : "rgba(124,58,237,0.35)", 36, 30);

        Label titleLbl = centeredLabel(title, getTextColor("white"), 18, true);
        Label msgLbl   = centeredLabel(message, getTextColor("#94a3b8"), 13, false);
        root.getChildren().addAll(iconWrap, titleLbl, msgLbl);

        if (detail != null && !detail.isBlank()) {
            root.getChildren().add(detailBox(detail, danger));
        }

        HBox btns = new HBox(14);
        btns.setAlignment(Pos.CENTER);
        Button cancelBtn = ghostBtn("Cancel");
        cancelBtn.setOnAction(e -> { result[0] = false; stage.close(); });
        Button okBtn = filledBtn(confirmText,
                danger ? "#ef4444" : "#7c3aed",
                danger ? "#dc2626" : "#6d28d9",
                danger ? "rgba(239,68,68,0.45)" : "rgba(124,58,237,0.45)");
        okBtn.setOnAction(e -> { result[0] = true; stage.close(); });
        btns.getChildren().addAll(cancelBtn, okBtn);
        root.getChildren().add(btns);

        showModal(stage, root);
        return result[0];
    }

    // ── Core dialog builder ───────────────────────────────────────────────────

    private static void showStatus(String iconStr, String title, String message,
                                    String accent, String bgColor, String borderColor) {
        Stage stage = buildStage(title);

        VBox root = buildRoot();

        // Accent bar at top
        Rectangle topBar = new Rectangle(56, 4);
        topBar.setArcWidth(4);
        topBar.setArcHeight(4);
        topBar.setFill(Color.web(accent));
        DropShadow barGlow = new DropShadow(12, Color.web(accent));
        topBar.setEffect(barGlow);

        // Glowing icon circle
        StackPane iconWrap = buildIconCircle(iconStr, accent, bgColor, borderColor, 38, 26);

        // Title
        Label titleLbl = centeredLabel(title, getTextColor("white"), 18, true);

        // Thin divider
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setPrefWidth(280);
        divider.setStyle("-fx-background-color: " + getDividerColor() + ";");

        // Message
        Label msgLbl = centeredLabel(message, getTextColor("#94a3b8"), 13, false);

        // Close button — colored to match accent for error, purple otherwise
        String btnColor  = accent.equals("#ef4444") ? "#ef4444" : "#7c3aed";
        String btnColor2 = accent.equals("#ef4444") ? "#dc2626" : "#6d28d9";
        String btnShadow = accent.equals("#ef4444") ? "rgba(239,68,68,0.45)" : "rgba(124,58,237,0.45)";
        Button okBtn = filledBtn("  OK  ", btnColor, btnColor2, btnShadow);
        okBtn.setMinWidth(110);
        okBtn.setOnAction(e -> stage.close());

        root.getChildren().addAll(topBar, iconWrap, titleLbl, divider, msgLbl, okBtn);
        showModal(stage, root);
    }

    // ── Component helpers ─────────────────────────────────────────────────────

    private static VBox buildRoot() {
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        VBox v = new VBox(20);
        v.setStyle(
            "-fx-background-color: " + (isLight ? "#ffffff" : "#0c0c1e") + ";" +
            "-fx-padding: 36 32 32 32;" +
            "-fx-border-color: " + (isLight ? "rgba(124,58,237,0.2)" : "rgba(255,255,255,0.06)") + ";" +
            "-fx-border-radius: 18;" +
            "-fx-background-radius: 18;");
        v.setAlignment(Pos.CENTER);
        v.setPrefWidth(410);
        return v;
    }

    private static StackPane buildIconCircle(String iconStr, String fgColor,
                                              String bgColor, String borderColor,
                                              double radius, double fontSize) {
        StackPane wrap = new StackPane();

        // Outer glow ring
        Circle outerRing = new Circle(radius + 10);
        outerRing.setFill(Color.TRANSPARENT);
        outerRing.setStroke(Color.web(borderColor.replace("0.40", "0.12").replace("0.35", "0.12")));
        outerRing.setStrokeWidth(1);

        Circle bg = new Circle(radius);
        bg.setFill(Color.web(bgColor));
        bg.setStroke(Color.web(borderColor));
        bg.setStrokeWidth(2);

        Label icon = new Label(iconStr);
        icon.setStyle("-fx-font-size: " + fontSize + "px; -fx-font-weight: bold;" +
                      "-fx-text-fill: " + fgColor + ";");

        wrap.getChildren().addAll(outerRing, bg, icon);

        DropShadow glow = new DropShadow(22, Color.web(fgColor));
        glow.setSpread(0.08);
        wrap.setEffect(glow);

        return wrap;
    }

    private static Label centeredLabel(String text, String color, double size, boolean bold) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: " + size + "px;" +
                   (bold ? " -fx-font-weight: bold;" : "") +
                   " -fx-line-spacing: 2;");
        l.setWrapText(true);
        l.setTextAlignment(TextAlignment.CENTER);
        l.setMaxWidth(350);
        return l;
    }

    private static VBox detailBox(String detail, boolean danger) {
        VBox box = new VBox(4);
        box.setStyle(
            "-fx-background-color: " + (danger ? "rgba(239,68,68,0.07)" : "rgba(124,58,237,0.07)") + ";" +
            "-fx-background-radius: 12; -fx-padding: 14 18 14 18;" +
            "-fx-border-color: " + (danger ? "rgba(239,68,68,0.22)" : "rgba(124,58,237,0.22)") + ";" +
            "-fx-border-radius: 12; -fx-border-width: 1;");
        Label l = new Label(detail);
        l.setStyle("-fx-text-fill: " + (danger ? "#fca5a5" : "#a78bfa") +
                   "; -fx-font-size: 13px; -fx-font-weight: bold;");
        l.setWrapText(true);
        box.getChildren().add(l);
        box.setMaxWidth(350);
        return box;
    }

    // ── Theme helpers ─────────────────────────────────────────────────────────

    private static String getTextColor(String darkColor) {
        if (!ThemeManager.getInstance().isPrintLightTheme()) return darkColor;
        if (darkColor.equals("white")) return "#111827";
        if (darkColor.equals("#94a3b8")) return "#374151";
        return darkColor;
    }

    private static String getDividerColor() {
        return ThemeManager.getInstance().isPrintLightTheme() 
            ? "rgba(124,58,237,0.15)" 
            : "rgba(255,255,255,0.07)";
    }

    // ── Stage / Scene ─────────────────────────────────────────────────────────

    static Stage buildStage(String title) {
        Stage s = new Stage();
        s.initModality(Modality.APPLICATION_MODAL);
        s.setResizable(false);
        s.setTitle(title);
        return s;
    }

    static void showModal(Stage stage, Region root) {
        Scene scene = new Scene(root);
        scene.getStylesheets().add(AppDialogs.class.getResource("/style.css").toExternalForm());
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.showAndWait();
    }

    // ── Button styles ─────────────────────────────────────────────────────────

    static Button ghostBtn(String text) {
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        Button b = new Button(text);
        String base = isLight
            ? "-fx-background-color: rgba(124,58,237,0.08);" +
              "-fx-text-fill: #6b7280; -fx-font-size: 13px; -fx-font-weight: bold;" +
              "-fx-padding: 10 28 10 28; -fx-background-radius: 10; -fx-cursor: hand;"
            : "-fx-background-color: rgba(255,255,255,0.06);" +
              "-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-font-weight: bold;" +
              "-fx-padding: 10 28 10 28; -fx-background-radius: 10; -fx-cursor: hand;";
        String hover = isLight
            ? "-fx-background-color: rgba(124,58,237,0.15);" +
              "-fx-text-fill: #111827; -fx-font-size: 13px; -fx-font-weight: bold;" +
              "-fx-padding: 10 28 10 28; -fx-background-radius: 10; -fx-cursor: hand;"
            : "-fx-background-color: rgba(255,255,255,0.12);" +
              "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
              "-fx-padding: 10 28 10 28; -fx-background-radius: 10; -fx-cursor: hand;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(base));
        return b;
    }

    static Button filledBtn(String text, String c1, String c2, String shadow) {
        Button b = new Button(text);
        b.setStyle(
            "-fx-background-color: linear-gradient(to right, " + c1 + ", " + c2 + ");" +
            "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-padding: 10 28 10 28; -fx-background-radius: 10; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian," + shadow + ",12,0,0,4);");
        return b;
    }
}
