package ui;

import game.SoundManager;
import game.ThemeManager;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.UserStats;

/**
 * Level up celebration screen shown when player reaches a new level.
 */
public class LevelUpScreen {

    private final int newLevel;
    private final UserStats stats;

    public LevelUpScreen(int newLevel, UserStats stats) {
        this.newLevel = newLevel;
        this.stats = stats;
    }

    public Scene buildScene() {
        SoundManager.getInstance().playFinish();

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
        card.getStyleClass().add("levelup-card");

        // ── Trophy / icon ─────────────────────────────────────────────────
        Label trophy = new Label("🎉");
        trophy.setStyle("-fx-font-size: 60px;");

        Label title = new Label("Level Up!");
        title.getStyleClass().add("label-title");

        Label levelLabel = new Label("Level " + newLevel + " — " + getLevelTitle(newLevel));
        levelLabel.setStyle("-fx-text-fill: " + (isLight ? "#7c3aed" : "#a78bfa") + "; -fx-font-size: 20px; -fx-font-weight: bold;");

        // ── Stats display ─────────────────────────────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(16);
        grid.setAlignment(Pos.CENTER);

        addStatCell(grid, 0, 0, "Total XP",  String.valueOf(stats.getXp()),     "stat-number");
        addStatCell(grid, 1, 0, "Coins",     String.valueOf(stats.getCoins()),  "stat-number-gold");
        addStatCell(grid, 0, 1, "Streak",    stats.getStreak() + " days",       "stat-number-orange");
        addStatCell(grid, 1, 1, "Level",     String.valueOf(newLevel),          "stat-number-cyan");

        card.getChildren().addAll(trophy, title, levelLabel, grid);

        // ── Reward banner ─────────────────────────────────────────────────
        HBox rewardBanner = new HBox(10);
        rewardBanner.setAlignment(Pos.CENTER);
        rewardBanner.setStyle(
            "-fx-background-color: rgba(245,158,11,0.15);" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 12 24 12 24;");
        Label rewardIcon  = new Label("🎁");
        rewardIcon.setStyle("-fx-font-size: 18px;");
        Label rewardLabel = new Label("+10 coins bonus reward!");
        rewardLabel.setStyle(
            "-fx-text-fill: #fbbf24;" +
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;");
        rewardBanner.getChildren().addAll(rewardIcon, rewardLabel);
        card.getChildren().add(rewardBanner);

        // ── Motivational message ──────────────────────────────────────────
        Label message = new Label(getMotivationalMessage(newLevel));
        message.setStyle("-fx-text-fill: " + (isLight ? "#7c3aed" : "#a78bfa") + "; -fx-font-size: 15px;");
        message.setWrapText(true);
        message.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        message.setMaxWidth(480);
        card.getChildren().add(message);

        // ── Buttons ───────────────────────────────────────────────────────
        HBox btnRow = new HBox(16);
        btnRow.setAlignment(Pos.CENTER);

        Button continueBtn = new Button("Continue");
        continueBtn.getStyleClass().add("btn-primary");
        continueBtn.setOnAction(e -> MainUI.showDashboard());

        Button themeStoreBtn = new Button("🎨  Theme Store");
        themeStoreBtn.getStyleClass().add("btn-secondary");
        themeStoreBtn.setOnAction(e -> MainUI.showThemeStore());

        btnRow.getChildren().addAll(continueBtn, themeStoreBtn);
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

    private String getLevelTitle(int level) {
        if (level >= 20) return "Grandmaster";
        if (level >= 15) return "Elite";
        if (level >= 10) return "Expert";
        if (level >= 5)  return "Advanced";
        if (level >= 3)  return "Intermediate";
        return "Beginner";
    }

    private String getMotivationalMessage(int level) {
        if (level >= 20) return "You've reached the pinnacle! You're a true TypeMaster! 🏆";
        if (level >= 15) return "Elite status achieved! Your dedication is inspiring! ⭐";
        if (level >= 10) return "Expert level unlocked! You're among the best! 🔥";
        if (level >= 5)  return "Advanced typist! Keep pushing your limits! 💪";
        return "Great progress! Keep practicing to unlock more rewards! 🚀";
    }
}
