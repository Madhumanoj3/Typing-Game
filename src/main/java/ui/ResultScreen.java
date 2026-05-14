package ui;

import game.ThemeManager;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.Achievement;
import model.GameResult;

import java.util.Collections;
import java.util.List;

/**
 * Post-game result screen.
 * Shows WPM, accuracy, errors, words typed, XP earned, and any newly unlocked achievements.
 */
public class ResultScreen {

    private final GameResult        result;
    private final int               xpGained;
    private final int               coinsGained;
    private final boolean           premiumBonus;
    private final List<Achievement> newAchievements;

    public ResultScreen(GameResult result) {
        this(result, 0, 0, false, Collections.emptyList());
    }

    public ResultScreen(GameResult result, int xpGained, List<Achievement> newAchievements) {
        this(result, xpGained, 0, false, newAchievements);
    }

    public ResultScreen(GameResult result, int xpGained, int coinsGained,
                        List<Achievement> newAchievements) {
        this(result, xpGained, coinsGained, false, newAchievements);
    }

    public ResultScreen(GameResult result, int xpGained, int coinsGained,
                        boolean premiumBonus, List<Achievement> newAchievements) {
        this.result          = result;
        this.xpGained        = xpGained;
        this.coinsGained     = coinsGained;
        this.premiumBonus    = premiumBonus;
        this.newAchievements = newAchievements != null ? newAchievements : Collections.emptyList();
    }

    public Scene buildScene() {
        ScrollPane scroll = new ScrollPane(buildCard());
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-dark");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        Scene scene = new Scene(scroll, 1200, 750);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        return scene;
    }

    private VBox buildCard() {
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        VBox outer = new VBox();
        outer.setAlignment(Pos.CENTER);
        outer.setStyle("-fx-background-color: " + ThemeManager.bg() + "; -fx-padding: 40;");
        outer.setFillWidth(true);

        VBox card = new VBox(24);
        card.setMaxWidth(600);
        card.setMinWidth(520);
        card.setStyle("-fx-background-color: " + ThemeManager.card() + "; -fx-background-radius: 20; -fx-padding: 44;");
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("result-card");

        // ── Star rating ───────────────────────────────────────────────────
        Label stars = new Label(getStarRating());
        stars.setStyle("-fx-font-size: 32px;");

        // ── Trophy / icon ─────────────────────────────────────────────────
        Label trophy = new Label(getTrophy());
        trophy.setStyle("-fx-font-size: 60px;");

        Label title = new Label("Game Over!");
        title.getStyleClass().add("label-title");

        Label subtitle = new Label(result.getGameMode() + "  •  " + result.getDifficulty());
        subtitle.getStyleClass().add("label-muted");

        // ── Stat grid ─────────────────────────────────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(16);
        grid.setAlignment(Pos.CENTER);

        addStatCell(grid, 0, 0, "WPM",      String.format("%.1f",  result.getWpm()),       "stat-number");
        addStatCell(grid, 1, 0, "Accuracy", String.format("%.1f%%", result.getAccuracy()),  "stat-number-green");
        addStatCell(grid, 0, 1, "Errors",   String.valueOf(result.getErrorCount()),          "stat-number-orange");
        addStatCell(grid, 1, 1, "Words",    String.valueOf(result.getWordsTyped()),           "stat-number-cyan");

        card.getChildren().addAll(stars, trophy, title, subtitle, grid);

        // ── Coins earned banner ───────────────────────────────────────────
        if (coinsGained > 0) {
            VBox coinsBanner = new VBox(4);
            coinsBanner.setAlignment(Pos.CENTER);
            coinsBanner.setStyle(
                "-fx-background-color: rgba(245,158,11,0.15);" +
                "-fx-background-radius: 12;" +
                "-fx-padding: 12 24 12 24;");

            HBox mainRow = new HBox(10);
            mainRow.setAlignment(Pos.CENTER);
            Label coinIcon  = new Label("💰");
            coinIcon.setStyle("-fx-font-size: 18px;");
            Label coinLabel = new Label("+" + coinsGained + " coins earned!");
            coinLabel.setStyle(
                "-fx-text-fill: #fbbf24;" +
                "-fx-font-size: 15px;" +
                "-fx-font-weight: bold;");
            mainRow.getChildren().addAll(coinIcon, coinLabel);
            coinsBanner.getChildren().add(mainRow);

            if (premiumBonus) {
                Label bonusLabel = new Label("👑  Includes +5 Premium Bonus");
                bonusLabel.setStyle(
                    "-fx-text-fill: #fcd34d;" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-style: italic;");
                coinsBanner.getChildren().add(bonusLabel);
            }

            card.getChildren().add(coinsBanner);
        }

        // ── XP earned banner ──────────────────────────────────────────────
        if (xpGained > 0) {
            HBox xpBanner = new HBox(10);
            xpBanner.setAlignment(Pos.CENTER);
            xpBanner.setStyle(
                "-fx-background-color: rgba(124,58,237,0.18);" +
                "-fx-background-radius: 12;" +
                "-fx-padding: 12 24 12 24;");

            Label xpIcon  = new Label("⚡");
            xpIcon.setStyle("-fx-font-size: 18px;");

            Label xpLabel = new Label("+" + xpGained + " XP earned!");
            xpLabel.setStyle(
                "-fx-text-fill: #a78bfa;" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;");

            xpBanner.getChildren().addAll(xpIcon, xpLabel);
            card.getChildren().add(xpBanner);
        }

        // ── New achievements unlocked ─────────────────────────────────────
        if (!newAchievements.isEmpty()) {
            VBox achBox = new VBox(8);
            achBox.setAlignment(Pos.CENTER_LEFT);
            achBox.setStyle(
                "-fx-background-color: rgba(16,185,129,0.08);" +
                "-fx-background-radius: 12;" +
                "-fx-padding: 14 18 14 18;" +
                "-fx-border-color: rgba(16,185,129,0.25);" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: 1;");

            Label achTitle = new Label("🏅  Achievement" + (newAchievements.size() > 1 ? "s" : "") + " Unlocked!");
            achTitle.setStyle(
                "-fx-text-fill: #10b981;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;");
            achBox.getChildren().add(achTitle);

            for (Achievement a : newAchievements) {
                Label lbl = new Label("  ✦  " + a.getTitle() + " — " + a.getDescription());
                lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
                lbl.setWrapText(true);
                achBox.getChildren().add(lbl);
            }
            card.getChildren().add(achBox);
        }

        // ── Feedback message ──────────────────────────────────────────────
        Label feedback = new Label(getFeedback());
        feedback.setStyle("-fx-text-fill: " + (isLight ? "#7c3aed" : "#a78bfa") + "; -fx-font-size: 15px;");
        feedback.setWrapText(true);
        feedback.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        feedback.setMaxWidth(480);
        card.getChildren().add(feedback);

        // ── Buttons ───────────────────────────────────────────────────────
        HBox btnRow = new HBox(16);
        btnRow.setAlignment(Pos.CENTER);

        Button playAgain = new Button("▶  Play Again");
        playAgain.getStyleClass().add("btn-primary");
        playAgain.setOnAction(e -> MainUI.showGame(
                result.getGameMode(), result.getDifficulty(), result.getDuration()));

        Button dashboard = new Button("⌂  Dashboard");
        dashboard.getStyleClass().add("btn-secondary");
        dashboard.setOnAction(e -> MainUI.showDashboard());

        Button leaderboard = new Button("🏆  Leaderboard");
        leaderboard.getStyleClass().add("btn-icon");
        leaderboard.setOnAction(e -> MainUI.showLeaderboard());

        btnRow.getChildren().addAll(playAgain, dashboard, leaderboard);
        card.getChildren().add(btnRow);

        outer.getChildren().add(card);
        return outer;
    }

    private void addStatCell(GridPane grid, int col, int row,
                              String label, String value, String style) {
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        VBox cell = new VBox(4);
        cell.setStyle("-fx-background-color: " + (isLight ? "#fef3c7" : "#0f172a") + "; -fx-background-radius: 12; -fx-padding: 16 30 16 30;");
        cell.setAlignment(Pos.CENTER);
        Label val = new Label(value);
        val.getStyleClass().add(style);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("label-muted");
        cell.getChildren().addAll(val, lbl);
        grid.add(cell, col, row);
    }

    private String getStarRating() {
        double wpm = result.getWpm();
        double acc = result.getAccuracy();
        int stars;
        if      (wpm >= 80 && acc >= 95) stars = 5;
        else if (wpm >= 60 && acc >= 90) stars = 4;
        else if (wpm >= 40 && acc >= 80) stars = 3;
        else if (wpm >= 20 && acc >= 70) stars = 2;
        else                             stars = 1;
        return "⭐".repeat(stars) + "☆".repeat(5 - stars);
    }

    private String getTrophy() {
        double wpm = result.getWpm();
        if      (wpm >= 80) return "🏆";
        else if (wpm >= 50) return "⭐";
        else if (wpm >= 30) return "✅";
        else                 return "💪";
    }

    private String getFeedback() {
        double wpm = result.getWpm();
        double acc = result.getAccuracy();
        if (wpm >= 80 && acc >= 95) return "Outstanding performance! You're a TypeMaster! 🎉";
        if (wpm >= 60 && acc >= 90) return "Excellent typing! Keep pushing for the top!";
        if (wpm >= 40 && acc >= 85) return "Good job! Consistent practice will get you further.";
        if (acc < 80)               return "Focus on accuracy first — speed will follow naturally.";
        return "Keep it up! Every session makes you better.";
    }
}
