package ui;

import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.GameResult;

/**
 * Post-game result screen — shows WPM, accuracy, errors, and words typed.
 */
public class ResultScreen {

    private final GameResult result;

    public ResultScreen(GameResult result) {
        this.result = result;
    }

    public Scene buildScene() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #0f0f1a;");
        root.getChildren().add(buildCard());

        Scene scene = new Scene(root, 1200, 750);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        return scene;
    }

    private VBox buildCard() {
        VBox card = new VBox(28);
        card.setMaxWidth(580);
        card.setMinWidth(580);
        card.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 20; -fx-padding: 48;");
        card.setAlignment(Pos.CENTER);
        StackPane.setAlignment(card, Pos.CENTER);

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

        addStatCell(grid, 0, 0, "WPM",      String.format("%.1f", result.getWpm()),      "stat-number");
        addStatCell(grid, 1, 0, "Accuracy", String.format("%.1f%%", result.getAccuracy()), "stat-number-green");
        addStatCell(grid, 0, 1, "Errors",   String.valueOf(result.getErrorCount()),        "stat-number-orange");
        addStatCell(grid, 1, 1, "Words",    String.valueOf(result.getWordsTyped()),         "stat-number-cyan");

        // ── Feedback message ──────────────────────────────────────────────
        Label feedback = new Label(getFeedback());
        feedback.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 15px;");
        feedback.setWrapText(true);
        feedback.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

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

        card.getChildren().addAll(trophy, title, subtitle, grid, feedback, btnRow);
        return card;
    }

    private void addStatCell(GridPane grid, int col, int row, String label, String value, String style) {
        VBox cell = new VBox(4);
        cell.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 12; -fx-padding: 16 30 16 30;");
        cell.setAlignment(Pos.CENTER);

        Label val = new Label(value);
        val.getStyleClass().add(style);

        Label lbl = new Label(label);
        lbl.getStyleClass().add("label-muted");

        cell.getChildren().addAll(val, lbl);
        grid.add(cell, col, row);
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
