package ui;

import db.MongoDBManager;
import game.SoundManager;
import game.TimerEngine;
import game.TypingEngine;
import game.WordBank;
import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.util.Duration;
import model.Achievement;
import model.GameResult;
import service.GamificationService;
import service.GamificationService.GamificationResult;
import util.SessionManager;

import java.util.Collections;
import java.util.List;

/**
 * The core typing game screen.
 * Supports Practice, Normal, and Timer modes with real-time WPM / accuracy display.
 */
public class GameScreen {

    private final String mode;
    private final String difficulty;
    private final int    timerSeconds;   // 0 = unlimited

    // ── Engines ───────────────────────────────────────────────────────────
    private TypingEngine typing;
    private TimerEngine  timer;
    private String       passage;

    // ── UI refs (updated every keystroke) ─────────────────────────────────
    private TextFlow    passageFlow;
    private TextArea    inputArea;
    private Label       wpmLabel;
    private Label       accLabel;
    private Label       errorLabel;
    private Label       wordsLabel;
    private Label       timerLabel;
    private ProgressBar progressBar;

    // ── State ─────────────────────────────────────────────────────────────
    private boolean gameOver   = false;
    private int     prevErrors = 0;
    private int     prevWords  = 0;

    public GameScreen(String mode, String difficulty, int timerSeconds) {
        this.mode         = mode;
        this.difficulty   = difficulty;
        this.timerSeconds = timerSeconds;
    }

    // ── Scene factory ─────────────────────────────────────────────────────

    public Scene buildScene() {
        typing = new TypingEngine();

        // Generate the passage for this mode
        passage = switch (mode) {
            case "Practice" -> WordBank.getRandomSentence() + " " + WordBank.buildPassage(difficulty, 30);
            case "Timer"    -> WordBank.buildPassage(difficulty, 200);
            default         -> WordBank.buildPassage(difficulty, 50);   // Normal
        };

        typing.start(passage);

        if ("Timer".equals(mode) && timerSeconds > 0) {
            timer = new TimerEngine(timerSeconds, this::onTick, this::onTimerFinished);
        }

        VBox layout = buildLayout();
        Scene scene = new Scene(layout, 1200, 750);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        // Auto-focus the input field once the window is shown
        scene.windowProperty().addListener((obs, oldWin, newWin) -> {
            if (newWin != null) {
                newWin.setOnShown(e -> inputArea.requestFocus());
            }
        });

        return scene;
    }

    // ── Layout ────────────────────────────────────────────────────────────

    private VBox buildLayout() {
        VBox layout = new VBox(0);
        layout.setStyle("-fx-background-color: #0f0f1a;");
        layout.getChildren().addAll(buildHeader(), buildBody());
        return layout;
    }

    // ── Header ────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        HBox header = new HBox(16);
        header.getStyleClass().add("header-bar");
        header.setAlignment(Pos.CENTER_LEFT);

        Button back = new Button("← Dashboard");
        back.getStyleClass().add("btn-secondary");
        back.setPadding(new Insets(7, 16, 7, 16));
        back.setOnAction(e -> {
            if (timer != null) timer.stop();
            MainUI.showDashboard();
        });

        Label modeTag = new Label(mode + "  •  " + difficulty);
        modeTag.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Timer display (only used for Timer mode)
        timerLabel = new Label(timer != null ? timer.getFormattedTime() : "");
        timerLabel.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 22px; -fx-font-weight: bold;");

        header.getChildren().addAll(back, modeTag, spacer, timerLabel);
        return header;
    }

    // ── Body ──────────────────────────────────────────────────────────────

    private VBox buildBody() {
        VBox body = new VBox(20);
        body.setStyle("-fx-padding: 32 80 32 80;");
        VBox.setVgrow(body, Priority.ALWAYS);

        // ── 1. Stat cards row ─────────────────────────────────────────────
        VBox wpmCard   = buildStatCard("WPM",      "0",     "stat-number");
        VBox accCard   = buildStatCard("Accuracy", "100%",  "stat-number-green");
        VBox errCard   = buildStatCard("Errors",   "0",     "stat-number-orange");
        VBox wordsCard = buildStatCard("Words",    "0",     "stat-number-cyan");

        // Keep refs so we can update them live
        wpmLabel   = (Label) wpmCard.getChildren().get(0);
        accLabel   = (Label) accCard.getChildren().get(0);
        errorLabel = (Label) errCard.getChildren().get(0);
        wordsLabel = (Label) wordsCard.getChildren().get(0);

        HBox statsRow = new HBox(16);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.getChildren().addAll(wpmCard, accCard, errCard, wordsCard);

        // ── 2. Progress bar ───────────────────────────────────────────────
        progressBar = new ProgressBar(0);
        progressBar.getStyleClass().add("progress-bar-custom");
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(10);

        // ── 3. Passage display ────────────────────────────────────────────
        passageFlow = new TextFlow();
        passageFlow.setStyle(
            "-fx-background-color: #1a1a2e;" +
            "-fx-padding: 20;" +
            "-fx-background-radius: 14;");
        passageFlow.setLineSpacing(6);
        passageFlow.setPrefWidth(Double.MAX_VALUE);
        renderPassage("");   // initial render

        ScrollPane passageScroll = new ScrollPane(passageFlow);
        passageScroll.setFitToWidth(true);
        passageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        passageScroll.setPrefHeight(160);
        passageScroll.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-background: transparent;" +
            "-fx-border-color: transparent;");

        // ── 4. Typing input ───────────────────────────────────────────────
        inputArea = new TextArea();
        inputArea.setPromptText("Start typing here to begin the game…");
        inputArea.getStyleClass().add("typing-input");
        inputArea.setWrapText(true);
        inputArea.setPrefHeight(120);
        inputArea.setMaxWidth(Double.MAX_VALUE);
        inputArea.textProperty().addListener((obs, oldVal, newVal) -> onInputChanged(newVal));

        // ── 5. Controls row ───────────────────────────────────────────────
        Button restart = new Button("↺  Restart");
        restart.getStyleClass().add("btn-icon");
        restart.setOnAction(e -> restartGame());

        Button dashBtn = new Button("⌂  Dashboard");
        dashBtn.getStyleClass().add("btn-secondary");
        dashBtn.setPadding(new Insets(8, 18, 8, 18));
        dashBtn.setOnAction(e -> {
            if (timer != null) timer.stop();
            MainUI.showDashboard();
        });

        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER_RIGHT);
        controls.getChildren().addAll(dashBtn, restart);

        // ── Hint label ────────────────────────────────────────────────────
        Label hint = new Label("🎯  Tip: Type the passage above exactly. Timer starts when you type your first character.");
        hint.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 12px; -fx-font-weight: bold;");

        body.getChildren().addAll(statsRow, progressBar, passageScroll, inputArea, hint, controls);
        return body;
    }

    // ── Stat card factory ─────────────────────────────────────────────────

    private VBox buildStatCard(String label, String value, String valueStyle) {
        VBox card = new VBox(4);
        card.setStyle(
            "-fx-background-color: #1a1a2e;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 16 24 16 24;");
        card.setAlignment(Pos.CENTER_LEFT);

        Label val = new Label(value);
        val.getStyleClass().add(valueStyle);

        Label lbl = new Label(label);
        lbl.getStyleClass().add("label-muted");

        card.getChildren().addAll(val, lbl);
        return card;
    }

    // ── Passage renderer (color-coded) ────────────────────────────────────

    private void renderPassage(String typed) {
        passageFlow.getChildren().clear();
        for (int i = 0; i < passage.length(); i++) {
            Text t = new Text(String.valueOf(passage.charAt(i)));
            t.setFont(Font.font("Consolas", FontWeight.NORMAL, 16));

            if (i < typed.length()) {
                if (typed.charAt(i) == passage.charAt(i)) {
                    t.setFill(Color.web("#a78bfa"));      // correct  → purple
                } else {
                    t.setFill(Color.web("#ef4444"));      // wrong    → red
                    t.setUnderline(true);
                }
            } else if (i == typed.length()) {
                t.setFill(Color.web("#06b6d4"));          // cursor   → cyan
                t.setUnderline(true);
            } else {
                t.setFill(Color.web("#94a3b8"));          // upcoming → bleak light grey
            }
            passageFlow.getChildren().add(t);
        }
    }

    // ── Input handler ─────────────────────────────────────────────────────

    private void onInputChanged(String typed) {
        if (gameOver) return;

        // Start countdown on first keystroke (Timer mode)
        if (timer != null && !timer.isRunning() && !typed.isEmpty()) {
            timer.start();
        }

        // Update engine
        typing.update(typed);

        // Sound feedback
        int curErrors = typing.getErrors();
        int curWords  = typing.getWordsTyped();
        if (curErrors > prevErrors)      SoundManager.getInstance().playWrong();
        else if (curWords > prevWords)   SoundManager.getInstance().playCorrect();
        prevErrors = curErrors;
        prevWords  = curWords;

        // Re-render passage with color coding
        renderPassage(typed);

        // Update live stat labels
        wpmLabel.setText(String.format("%.0f", typing.getWpm()));
        accLabel.setText(String.format("%.0f%%", typing.getAccuracy()));
        errorLabel.setText(String.valueOf(typing.getErrors()));
        wordsLabel.setText(String.valueOf(typing.getWordsTyped()));
        progressBar.setProgress(typing.getProgress(typed));

        // Finish condition for Practice and Normal modes
        if (!"Timer".equals(mode) && typing.isComplete(typed)) {
            finishGame(typed);
        }
    }

    // ── Timer callbacks ───────────────────────────────────────────────────

    private void onTick(int remaining) {
        timerLabel.setText(String.format("%02d:%02d", remaining / 60, remaining % 60));
        if (remaining <= 10) {
            timerLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 22px; -fx-font-weight: bold;");
        }
    }

    private void onTimerFinished() {
        finishGame(inputArea.getText());
    }

    // ── Game over ─────────────────────────────────────────────────────────

    private void finishGame(String typed) {
        if (gameOver) return;
        gameOver = true;

        if (timer != null) timer.stop();
        typing.finish();
        SoundManager.getInstance().playFinish();

        GameResult result = new GameResult(
                SessionManager.getInstance().getUsername(),
                mode,
                difficulty,
                timerSeconds,
                typing.getWpm(),
                typing.getAccuracy(),
                typing.getErrors(),
                typing.getWordsTyped()
        );

        // Persist game result and update user stats
        try {
            MongoDBManager.getInstance().saveResult(result);
            MongoDBManager.getInstance().updateUserStats(
                    SessionManager.getInstance().getUsername(),
                    result.getWpm(),
                    result.getAccuracy()
            );
        } catch (Exception ex) {
            System.err.println("DB save failed: " + ex.getMessage());
        }

        // Gamification applies only to Normal and Timer modes
        int xpGained    = 0;
        int coinsGained = 0;
        List<Achievement> newAchievements = Collections.emptyList();
        if (!"Practice".equals(mode)) {
            try {
                GamificationResult gr = GamificationService.getInstance()
                        .processGameResult(SessionManager.getInstance().getUsername(), result);
                xpGained        = gr.xpGained();
                coinsGained     = gr.coinsGained();
                newAchievements = gr.newAchievements();
            } catch (Exception ex) {
                System.err.println("Gamification failed: " + ex.getMessage());
            }
        }

        final int finalXp     = xpGained;
        final int finalCoins  = coinsGained;
        final List<Achievement> finalAch = newAchievements;

        PauseTransition pause = new PauseTransition(Duration.millis(300));
        pause.setOnFinished(e -> MainUI.showResult(result, finalXp, finalCoins, finalAch));
        pause.play();
    }

    private void restartGame() {
        if (timer != null) timer.stop();
        MainUI.showGame(mode, difficulty, timerSeconds);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    @SuppressWarnings("unused")
    private Region gap(int width) {
        Region r = new Region();
        r.setMinWidth(width);
        return r;
    }
}
