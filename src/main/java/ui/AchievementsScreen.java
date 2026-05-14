package ui;

import game.ThemeManager;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.Achievement;
import service.GamificationService;
import service.GamificationService.AchievementDef;
import util.SessionManager;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Achievements screen — shows all defined achievements with earned/locked state.
 * Used both as embedded content (buildContent) and as a standalone scene (buildScene).
 */
public class AchievementsScreen {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy");

    public Scene buildScene() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #0f0f1a;");
        root.getChildren().add(buildContent());
        Scene scene = new Scene(root, 1200, 750);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        return scene;
    }

    public ScrollPane buildContent() {
        cardColorIndex = 0; // Reset for each build
        String username = SessionManager.getInstance().getUsername();
        GamificationService gs = GamificationService.getInstance();

        List<Achievement> earned = gs.getAchievements(username);
        Map<String, Achievement> earnedMap = earned.stream()
                .collect(Collectors.toMap(Achievement::getBadge, a -> a));

        VBox content = new VBox(28);
        content.setStyle("-fx-padding: 36 40 36 40;");

        // ── Header ────────────────────────────────────────────────────────
        Label title = new Label("Achievements");
        title.getStyleClass().add("label-title");

        int total    = GamificationService.ALL_ACHIEVEMENTS.size();
        int unlocked = earnedMap.size();
        Label sub = new Label(unlocked + " / " + total + " unlocked");
        sub.getStyleClass().add("label-muted");

        ProgressBar overallBar = new ProgressBar((double) unlocked / total);
        overallBar.getStyleClass().add("progress-bar-custom");
        overallBar.setMaxWidth(300);
        overallBar.setPrefHeight(10);

        HBox headerRow = new HBox(20);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(6, title, sub, overallBar);
        headerRow.getChildren().add(titleBox);

        // ── Achievement grid ──────────────────────────────────────────────
        FlowPane grid = new FlowPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setPrefWrapLength(1100);

        for (AchievementDef def : GamificationService.ALL_ACHIEVEMENTS) {
            Achievement a = earnedMap.get(def.badge());
            grid.getChildren().add(buildAchievementCard(def, a));
        }

        content.getChildren().addAll(headerRow, grid);

        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().add("scroll-dark");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    // ── Card ──────────────────────────────────────────────────────────────

    private static final String[] CARD_COLORS = {"card-blue", "card-pink", "card-yellow", "card-red", "card-green", "card-purple"};
    private static int cardColorIndex = 0;

    private VBox buildAchievementCard(AchievementDef def, Achievement earned) {
        boolean unlocked = earned != null;

        VBox card = new VBox(10);
        card.setPrefWidth(220);
        card.setMaxWidth(220);
        card.setAlignment(Pos.TOP_LEFT);

        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        String textColor   = isLight ? "#1a1a1a" : "#e2e8f0";
        String mutedColor  = isLight ? "#555555" : "#94a3b8";
        String lockedBg    = isLight ? "#e8e8e8" : "#1e293b";
        String lockedText  = isLight ? "#666666" : "#64748b";

        // Assign colorful card style for unlocked achievements
        if (unlocked) {
            String colorClass = CARD_COLORS[cardColorIndex % CARD_COLORS.length];
            card.getStyleClass().add(colorClass);
            cardColorIndex++;
        } else {
            card.setStyle(
                "-fx-background-color: " + lockedBg + "; -fx-background-radius: 16; -fx-padding: 20;" +
                "-fx-opacity: 0.6;");
        }

        // Icon
        Label icon = new Label(unlocked ? def.icon() : "🔒");
        icon.setStyle("-fx-font-size: 32px;");

        // Title
        Label titleLbl = new Label(def.title());
        titleLbl.setStyle(unlocked
            ? "-fx-text-fill: " + textColor + "; -fx-font-size: 13px; -fx-font-weight: bold;"
            : "-fx-text-fill: " + lockedText + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        titleLbl.setWrapText(true);

        // Description
        Label desc = new Label(def.description());
        desc.setStyle("-fx-text-fill: " + mutedColor + "; -fx-font-size: 11px;");
        desc.setWrapText(true);

        card.getChildren().addAll(icon, titleLbl, desc);

        // Earned date badge
        if (unlocked && earned.getEarnedAt() != null) {
            Label dateLbl = new Label("Earned " + earned.getEarnedAt().format(DATE_FMT));
            dateLbl.setStyle(
                "-fx-background-color: rgba(59,130,246,0.15);" +
                "-fx-text-fill: " + textColor + ";" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 3 8 3 8;" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;");
            card.getChildren().add(dateLbl);
        } else if (!unlocked) {
            Label locked = new Label("LOCKED");
            locked.setStyle(
                "-fx-background-color: rgba(100,116,139,0.15);" +
                "-fx-text-fill: #666666;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 3 8 3 8;" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;");
            card.getChildren().add(locked);
        }

        return card;
    }
}
