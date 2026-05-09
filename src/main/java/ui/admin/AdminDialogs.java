package ui.admin;

import game.ThemeManager;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.*;

/**
 * Custom dark-themed modal dialogs for all admin panel operations.
 * Replaces plain JavaFX Alert so every confirmation/info popup
 * matches the admin panel's visual language.
 */
public final class AdminDialogs {

    private AdminDialogs() {}

    // ── Confirmation Dialog ───────────────────────────────────────────────────

    /**
     * Shows a beautifully styled confirmation modal.
     *
     * @param title       Bold heading shown inside the dialog
     * @param message     Descriptive message
     * @param detail      Extra context shown in a coloured box (nullable)
     * @param confirmText Label for the confirm button
     * @param danger      true → red confirm button; false → purple
     * @return            true when user clicks the confirm button
     */
    public static boolean showConfirm(String title, String message,
                                      String detail, String confirmText, boolean danger) {
        Stage stage = buildStage(title);
        boolean[] result = {false};
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();

        VBox root = new VBox(22);
        root.setStyle("-fx-background-color: " + (isLight ? "#ffffff" : "#0c0c1e") + "; -fx-padding: 36 32 32 32;");
        root.setAlignment(Pos.CENTER);
        root.setPrefWidth(440);

        // ── Icon circle ──────────────────────────────────────────────────
        StackPane iconWrap = new StackPane();
        Circle bg = new Circle(36);
        bg.setFill(Color.web(danger ? "#ef444422" : "#7c3aed22"));
        bg.setStroke(Color.web(danger ? "#ef444455" : "#7c3aed55"));
        bg.setStrokeWidth(1.5);
        Label icon = new Label(danger ? "⚠" : "❓");
        icon.setStyle("-fx-font-size: 30px;");
        iconWrap.getChildren().addAll(bg, icon);

        // ── Title ────────────────────────────────────────────────────────
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: " + (isLight ? "#111827" : "white") + "; -fx-font-size: 18px; -fx-font-weight: bold;");
        titleLbl.setWrapText(true);
        titleLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        titleLbl.setMaxWidth(360);

        // ── Message ──────────────────────────────────────────────────────
        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-text-fill: " + (isLight ? "#374151" : "#94a3b8") + "; -fx-font-size: 13px;");
        msgLbl.setWrapText(true);
        msgLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        msgLbl.setMaxWidth(360);

        root.getChildren().addAll(iconWrap, titleLbl, msgLbl);

        // ── Detail box ───────────────────────────────────────────────────
        if (detail != null && !detail.isBlank()) {
            VBox detailBox = new VBox(4);
            detailBox.setStyle(
                "-fx-background-color: " + (danger ? "rgba(239,68,68,0.07)" : "rgba(124,58,237,0.07)") + ";" +
                "-fx-background-radius: 12;" +
                "-fx-padding: 14 18 14 18;" +
                "-fx-border-color: " + (danger ? "rgba(239,68,68,0.22)" : "rgba(124,58,237,0.22)") + ";" +
                "-fx-border-radius: 12; -fx-border-width: 1;");
            Label detailLbl = new Label(detail);
            detailLbl.setStyle(
                "-fx-text-fill: " + (danger ? "#fca5a5" : "#a78bfa") + ";" +
                "-fx-font-size: 13px; -fx-font-weight: bold;");
            detailLbl.setWrapText(true);
            detailBox.getChildren().add(detailLbl);
            detailBox.setMaxWidth(360);
            root.getChildren().add(detailBox);
        }

        // ── Buttons ──────────────────────────────────────────────────────
        HBox btns = new HBox(14);
        btns.setAlignment(Pos.CENTER);

        Button cancelBtn = ghostBtn("Cancel");
        cancelBtn.setOnAction(e -> { result[0] = false; stage.close(); });

        Button okBtn = filledBtn(confirmText, danger ? "#ef4444" : "#7c3aed",
                danger ? "#dc2626" : "#6d28d9",
                danger ? "#ef444466" : "#7c3aed66");
        okBtn.setOnAction(e -> { result[0] = true; stage.close(); });

        btns.getChildren().addAll(cancelBtn, okBtn);
        root.getChildren().add(btns);

        showModal(stage, root);
        return result[0];
    }

    // ── Success / Info Dialogs ────────────────────────────────────────────────

    public static void showSuccess(String title, String message) {
        showStatus("✅", title, message, "#34d399", "#064e3b22", "#34d39944");
    }

    public static void showInfo(String title, String message) {
        showStatus("ℹ", title, message, "#38bdf8", "#0c4a6e22", "#38bdf844");
    }

    public static void showError(String title, String message) {
        showStatus("✕", title, message, "#ef4444", "#450a0a22", "#ef444444");
    }

    private static void showStatus(String iconStr, String title, String message,
                                    String fgColor, String bgColor, String borderColor) {
        Stage stage = buildStage(title);
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();

        VBox root = new VBox(18);
        root.setStyle("-fx-background-color: " + (isLight ? "#ffffff" : "#0c0c1e") + "; -fx-padding: 34 30 30 30;");
        root.setAlignment(Pos.CENTER);
        root.setPrefWidth(380);

        // Icon circle
        StackPane iconWrap = new StackPane();
        Circle bg = new Circle(30);
        bg.setFill(Color.web(bgColor));
        bg.setStroke(Color.web(borderColor));
        bg.setStrokeWidth(1.5);
        Label icon = new Label(iconStr);
        icon.setStyle("-fx-font-size: 22px; -fx-text-fill: " + fgColor + ";");
        iconWrap.getChildren().addAll(bg, icon);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: " + (isLight ? "#111827" : "white") + "; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-text-fill: " + fgColor + "; -fx-font-size: 13px;");
        msgLbl.setWrapText(true);
        msgLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        msgLbl.setMaxWidth(310);

        Button okBtn = filledBtn("OK", "#7c3aed", "#6d28d9", "#7c3aed66");
        okBtn.setOnAction(e -> stage.close());

        root.getChildren().addAll(iconWrap, titleLbl, msgLbl, okBtn);
        showModal(stage, root);
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    static Stage buildStage(String title) {
        Stage s = new Stage();
        s.initModality(Modality.APPLICATION_MODAL);
        s.setResizable(false);
        s.setTitle(title);
        return s;
    }

    static void showModal(Stage stage, Region root) {
        Scene scene = new Scene(root);
        scene.getStylesheets().add(AdminDialogs.class.getResource("/style.css").toExternalForm());
        scene.setFill(Color.TRANSPARENT);
        ThemeManager.applyTheme(scene);
        stage.setScene(scene);
        stage.showAndWait();
    }

    static Button ghostBtn(String text) {
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        Button b = new Button(text);
        String base = isLight
            ? "-fx-background-color: rgba(124,58,237,0.08);" +
              "-fx-text-fill: #6b7280; -fx-font-size: 13px; -fx-font-weight: bold;" +
              "-fx-padding: 10 26 10 26; -fx-background-radius: 10; -fx-cursor: hand;"
            : "-fx-background-color: rgba(255,255,255,0.06);" +
              "-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-font-weight: bold;" +
              "-fx-padding: 10 26 10 26; -fx-background-radius: 10; -fx-cursor: hand;";
        String hover = isLight
            ? "-fx-background-color: rgba(124,58,237,0.15);" +
              "-fx-text-fill: #111827; -fx-font-size: 13px; -fx-font-weight: bold;" +
              "-fx-padding: 10 26 10 26; -fx-background-radius: 10; -fx-cursor: hand;"
            : "-fx-background-color: rgba(255,255,255,0.1);" +
              "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
              "-fx-padding: 10 26 10 26; -fx-background-radius: 10; -fx-cursor: hand;";
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
            "-fx-padding: 10 26 10 26; -fx-background-radius: 10; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian," + shadow + ",10,0,0,3);");
        return b;
    }

    // ── Plan Choice Dialog ────────────────────────────────────────────────────

    /**
     * Shows a styled plan-selection dialog with a ComboBox.
     * Returns the chosen plan string, or null if cancelled.
     */
    public static String showPlanChoice(String username, String currentPlan) {
        Stage stage = buildStage("Change Plan");
        String[] result = {null};
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();

        VBox root = new VBox(20);
        root.setStyle("-fx-background-color: " + (isLight ? "#ffffff" : "#0c0c1e") + "; -fx-padding: 34 30 28 30;");
        root.setAlignment(Pos.CENTER);
        root.setPrefWidth(400);

        StackPane iconWrap = new StackPane();
        Circle bg = new Circle(32);
        bg.setFill(Color.web("rgba(124,58,237,0.14)"));
        bg.setStroke(Color.web("rgba(124,58,237,0.40)"));
        bg.setStrokeWidth(1.5);
        Label icon = new Label("📦");
        icon.setStyle("-fx-font-size: 22px;");
        iconWrap.getChildren().addAll(bg, icon);

        Label titleLbl = new Label("Change Plan");
        titleLbl.setStyle("-fx-text-fill: " + (isLight ? "#111827" : "white") + "; -fx-font-size: 17px; -fx-font-weight: bold;");

        Label subLbl = new Label("Select a new plan for: " + username);
        subLbl.setStyle("-fx-text-fill: " + (isLight ? "#374151" : "#94a3b8") + "; -fx-font-size: 13px;");

        javafx.scene.control.ComboBox<String> planCombo = new javafx.scene.control.ComboBox<>();
        planCombo.getItems().addAll("FREE", "MONTHLY", "LIFETIME");
        planCombo.setValue(currentPlan);
        planCombo.getStyleClass().add("field-dark");
        planCombo.setPrefWidth(240);

        HBox btns = new HBox(14);
        btns.setAlignment(Pos.CENTER);
        Button cancelBtn = ghostBtn("Cancel");
        cancelBtn.setOnAction(e -> stage.close());
        Button okBtn = filledBtn("Apply", "#7c3aed", "#6d28d9", "#7c3aed66");
        okBtn.setOnAction(e -> { result[0] = planCombo.getValue(); stage.close(); });
        btns.getChildren().addAll(cancelBtn, okBtn);

        root.getChildren().addAll(iconWrap, titleLbl, subLbl, planCombo, btns);
        showModal(stage, root);
        return result[0];
    }

    /** Styled form label */
    public static Label formLabel(String text) {
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + (isLight ? "#374151" : "#64748b") + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        return l;
    }

    /** Styled section header inside a form */
    public static VBox sectionHeader(String icon, String title, String accent) {
        VBox box = new VBox(2);
        Label h = new Label(icon + "  " + title);
        h.setStyle("-fx-text-fill: " + accent + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        Region line = new Region();
        line.setPrefHeight(1);
        line.setStyle("-fx-background-color: " + accent + "33;");
        box.getChildren().addAll(h, line);
        return box;
    }
}
