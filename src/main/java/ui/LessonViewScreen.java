package ui;

import game.TypingEngine;
import javafx.animation.PauseTransition;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.util.Duration;
import model.Lesson;
import service.GamificationService;
import service.TrainingService;
import util.SessionManager;

/**
 * Full-screen lesson view.
 * Reuses {@link TypingEngine} for WPM/accuracy — no score is recorded.
 * On completion the user sees an in-screen result overlay, then can go back to training.
 */
public class LessonViewScreen {

    private final Lesson         lesson;
    private final TrainingService trainingService = TrainingService.getInstance();
    private final String         username = SessionManager.getInstance().getUsername();

    // ── Engine ────────────────────────────────────────────────────────────
    private TypingEngine typing;

    // ── UI refs ───────────────────────────────────────────────────────────
    private TextFlow    passageFlow;
    private TextArea    inputArea;
    private Label       wpmLabel;
    private Label       accLabel;
    private Label       errorLabel;
    private ProgressBar progressBar;
    private StackPane   root;

    private boolean finished = false;

    public LessonViewScreen(Lesson lesson) {
        this.lesson = lesson;
    }

    // ── Scene factory ─────────────────────────────────────────────────────

    public Scene buildScene() {
        typing = new TypingEngine();
        typing.start(lesson.getContent());

        root = new StackPane();
        VBox layout = new VBox(0);
        layout.setStyle("-fx-background-color: #0f0f1a;");

        ScrollPane scroll = new ScrollPane(buildBody());
        scroll.getStyleClass().add("scroll-dark");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        layout.getChildren().addAll(buildHeader(), scroll);
        root.getChildren().add(layout);

        Scene scene = new Scene(root, 1200, 750);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        scene.windowProperty().addListener((obs, oldW, newW) -> {
            if (newW != null) newW.setOnShown(e -> inputArea.requestFocus());
        });
        return scene;
    }

    // ── Header ────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        HBox header = new HBox(16);
        header.getStyleClass().add("header-bar");
        header.setAlignment(Pos.CENTER_LEFT);

        Button back = new Button("← Training");
        back.getStyleClass().add("btn-secondary");
        back.setPadding(new Insets(7, 16, 7, 16));
        back.setOnAction(e -> MainUI.showDashboard());

        VBox titleBox = new VBox(2);
        Label lessonTitle = new Label(lesson.getTitle());
        lessonTitle.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 15px; -fx-font-weight: bold;");
        Label levelLabel  = new Label(lesson.getLevel() + "  •  Target: " + lesson.getTargetWpm() + " WPM");
        levelLabel.getStyleClass().add("label-muted");
        titleBox.getChildren().addAll(lessonTitle, levelLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label hint = new Label("Training Mode — no score recorded");
        hint.setStyle("-fx-text-fill: #10b981; -fx-font-size: 12px;");

        header.getChildren().addAll(back, titleBox, spacer, hint);
        return header;
    }

    // ── Body ──────────────────────────────────────────────────────────────

    private VBox buildBody() {
        VBox body = new VBox(20);
        body.setStyle("-fx-padding: 24 80 32 80;");
        VBox.setVgrow(body, Priority.ALWAYS);

        // Keyboard guide image
        var kbResource = getClass().getResource("/keyboard_guide.png");
        if (kbResource != null) {
            ImageView kbView = new ImageView(new Image(kbResource.toExternalForm()));
            kbView.setFitWidth(880);
            kbView.setPreserveRatio(true);
            kbView.setSmooth(true);
            HBox imgBox = new HBox(kbView);
            imgBox.setAlignment(Pos.CENTER);
            body.getChildren().add(imgBox);
        }

        // Finger hint card
        HBox hintCard = buildFingerHintCard();

        // Stat cards
        VBox wpmCard  = statCard("WPM",      "0",     "stat-number");
        VBox accCard  = statCard("Accuracy", "100%",  "stat-number-green");
        VBox errCard  = statCard("Errors",   "0",     "stat-number-orange");

        wpmLabel   = (Label) wpmCard.getChildren().get(0);
        accLabel   = (Label) accCard.getChildren().get(0);
        errorLabel = (Label) errCard.getChildren().get(0);

        HBox statsRow = new HBox(16, wpmCard, accCard, errCard);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        // Progress bar
        progressBar = new ProgressBar(0);
        progressBar.getStyleClass().add("progress-bar-custom");
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(10);

        // Passage
        passageFlow = new TextFlow();
        passageFlow.setStyle(
            "-fx-background-color: #1a1a2e;" +
            "-fx-padding: 20;" +
            "-fx-background-radius: 14;");
        passageFlow.setLineSpacing(6);
        passageFlow.setPrefWidth(Double.MAX_VALUE);
        renderPassage("");

        ScrollPane passageScroll = new ScrollPane(passageFlow);
        passageScroll.setFitToWidth(true);
        passageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        passageScroll.setPrefHeight(160);
        passageScroll.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-background: transparent;" +
            "-fx-border-color: transparent;");

        // Input area
        inputArea = new TextArea();
        inputArea.setPromptText("Start typing the passage above…");
        inputArea.getStyleClass().add("typing-input");
        inputArea.setWrapText(true);
        inputArea.setPrefHeight(110);
        inputArea.setMaxWidth(Double.MAX_VALUE);
        inputArea.textProperty().addListener((obs, oldVal, newVal) -> onInput(newVal));

        // Controls
        Button restart = new Button("↺  Restart");
        restart.getStyleClass().add("btn-icon");
        restart.setOnAction(e -> MainUI.showLesson(lesson));

        Button backBtn = new Button("⌂  Back to Training");
        backBtn.getStyleClass().add("btn-secondary");
        backBtn.setOnAction(e -> MainUI.showDashboard());

        HBox controls = new HBox(12, backBtn, restart);
        controls.setAlignment(Pos.CENTER_RIGHT);

        body.getChildren().addAll(hintCard, statsRow, progressBar, passageScroll, inputArea, controls);
        return body;
    }

    // ── Finger hint card ──────────────────────────────────────────────────

    private HBox buildFingerHintCard() {
        HBox card = new HBox(14);
        card.setStyle(
            "-fx-background-color: rgba(6,182,212,0.08);" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 14 18 14 18;" +
            "-fx-border-color: rgba(6,182,212,0.25);" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1;");
        card.setAlignment(Pos.TOP_LEFT);

        Label icon = new Label("💡");
        icon.setStyle("-fx-font-size: 20px;");

        VBox textBox = new VBox(4);
        Label heading = new Label("Finger Placement Guide");
        heading.setStyle("-fx-text-fill: #06b6d4; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label hint = new Label(lesson.getFingerHint());
        hint.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        hint.setWrapText(true);
        textBox.getChildren().addAll(heading, hint);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        card.getChildren().addAll(icon, textBox);
        return card;
    }

    // ── Stat card factory ─────────────────────────────────────────────────

    private VBox statCard(String label, String value, String valueStyle) {
        VBox card = new VBox(4);
        card.setStyle(
            "-fx-background-color: #1a1a2e;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 14 22 14 22;");
        card.setAlignment(Pos.CENTER_LEFT);
        Label val = new Label(value);
        val.getStyleClass().add(valueStyle);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("label-muted");
        card.getChildren().addAll(val, lbl);
        return card;
    }

    // ── Passage renderer ──────────────────────────────────────────────────

    private void renderPassage(String typed) {
        String passage = lesson.getContent();
        passageFlow.getChildren().clear();
        for (int i = 0; i < passage.length(); i++) {
            Text t = new Text(String.valueOf(passage.charAt(i)));
            t.setFont(Font.font("Consolas", FontWeight.NORMAL, 16));
            if (i < typed.length()) {
                if (typed.charAt(i) == passage.charAt(i)) {
                    t.setFill(Color.web("#a78bfa"));
                } else {
                    t.setFill(Color.web("#ef4444"));
                    t.setUnderline(true);
                }
            } else if (i == typed.length()) {
                t.setFill(Color.web("#06b6d4"));
                t.setUnderline(true);
            } else {
                t.setFill(Color.web("#475569"));
            }
            passageFlow.getChildren().add(t);
        }
    }

    // ── Input handler ─────────────────────────────────────────────────────

    private void onInput(String typed) {
        if (finished) return;

        typing.update(typed);
        renderPassage(typed);

        wpmLabel.setText(String.format("%.0f", typing.getWpm()));
        accLabel.setText(String.format("%.0f%%", typing.getAccuracy()));
        errorLabel.setText(String.valueOf(typing.getErrors()));
        progressBar.setProgress(typing.getProgress(typed));

        if (typing.isComplete(typed)) {
            finishLesson(typed);
        }
    }

    // ── Completion ────────────────────────────────────────────────────────

    private void finishLesson(String typed) {
        if (finished) return;
        finished = true;

        typing.finish();
        double wpm      = typing.getWpm();
        double accuracy = typing.getAccuracy();

        try {
            trainingService.recordAttempt(username, lesson, wpm, accuracy);
            int completed = trainingService.countCompleted(username);
            GamificationService.getInstance().checkLessonAchievements(username, completed);
        } catch (Exception ex) {
            System.err.println("Training progress save failed: " + ex.getMessage());
        }

        PauseTransition pause = new PauseTransition(Duration.millis(400));
        pause.setOnFinished(e -> showResultOverlay(wpm, accuracy));
        pause.play();
    }

    private void showResultOverlay(double wpm, double accuracy) {
        boolean passed  = accuracy >= 70.0;
        boolean hitGoal = wpm >= lesson.getTargetWpm();

        VBox overlay = new VBox(24);
        overlay.setAlignment(Pos.CENTER);
        overlay.setStyle(
            "-fx-background-color: rgba(15,15,26,0.94);" +
            "-fx-padding: 60;");

        // Result card
        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setStyle(
            "-fx-background-color: #1a1a2e;" +
            "-fx-background-radius: 24;" +
            "-fx-padding: 48 56 48 56;" +
            "-fx-max-width: 520;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 30, 0, 0, 8);");

        Label trophy = new Label(passed ? (hitGoal ? "🏆" : "⭐") : "💪");
        trophy.setStyle("-fx-font-size: 56px;");

        Label resultTitle = new Label(passed ? "Lesson Complete!" : "Keep Practising!");
        resultTitle.getStyleClass().add("label-title");

        // Stats grid
        GridPane grid = new GridPane();
        grid.setHgap(32);
        grid.setVgap(12);
        grid.setAlignment(Pos.CENTER);
        addResultRow(grid, 0, "WPM",      String.format("%.0f", wpm), "stat-number");
        addResultRow(grid, 1, "Accuracy", String.format("%.1f%%", accuracy), "stat-number-green");
        addResultRow(grid, 2, "Target",   lesson.getTargetWpm() + " WPM",
                hitGoal ? "stat-number-cyan" : "stat-number-orange");

        // Feedback message
        String feedback = buildFeedback(wpm, accuracy, passed, hitGoal);
        Label feedbackLabel = new Label(feedback);
        feedbackLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        feedbackLabel.setWrapText(true);
        feedbackLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        feedbackLabel.setMaxWidth(400);

        // Completion badge
        if (passed) {
            Label completedBadge = new Label("✅  Lesson marked as completed");
            completedBadge.setStyle(
                "-fx-background-color: rgba(16,185,129,0.15);" +
                "-fx-text-fill: #10b981;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 6 14 6 14;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;");
            card.getChildren().add(completedBadge);
        }

        // Action buttons
        Button tryAgain = new Button("↺  Try Again");
        tryAgain.getStyleClass().add("btn-secondary");
        tryAgain.setOnAction(e -> MainUI.showLesson(lesson));

        Button nextBtn = new Button("← Back to Training");
        nextBtn.getStyleClass().add("btn-primary");
        nextBtn.setOnAction(e -> MainUI.showDashboard());

        HBox btnRow = new HBox(14, tryAgain, nextBtn);
        btnRow.setAlignment(Pos.CENTER);

        card.getChildren().addAll(trophy, resultTitle, grid, feedbackLabel, btnRow);
        overlay.getChildren().add(card);

        root.getChildren().add(overlay);
        inputArea.setEditable(false);
    }

    private void addResultRow(GridPane grid, int row, String label, String value, String styleClass) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("label-muted");
        Label val = new Label(value);
        val.getStyleClass().add(styleClass);
        val.setStyle(val.getStyle() + "-fx-font-size: 26px;");
        grid.add(lbl, row, 0);
        grid.add(val, row, 1);
    }

    private String buildFeedback(double wpm, double accuracy, boolean passed, boolean hitGoal) {
        if (!passed) return "Accuracy below 70% — focus on typing correctly before increasing speed. Slow down and try again!";
        if (hitGoal && accuracy >= 95) return "Outstanding! Perfect speed and accuracy. You have truly mastered this lesson.";
        if (hitGoal) return "Speed goal reached! Work on raising your accuracy above 95% for full mastery.";
        if (accuracy >= 95) return "Excellent accuracy! Keep working on your speed to hit the " + lesson.getTargetWpm() + " WPM target.";
        return "Good effort! Aim for " + lesson.getTargetWpm() + " WPM with 95%+ accuracy for mastery.";
    }
}