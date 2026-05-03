package ui;

import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.Lesson;
import model.TrainingProgress;
import service.TrainingService;
import util.SessionManager;

import java.util.List;
import java.util.Map;

/**
 * Training module dashboard — shows all lessons grouped by level with progress indicators.
 * Designed to be embedded inside DashboardScreen's content area via {@link #buildContent()},
 * or displayed as a standalone scene via {@link #buildScene()}.
 */
public class TrainingDashboardScreen {

    private final TrainingService trainingService = TrainingService.getInstance();
    private final String username = SessionManager.getInstance().getUsername();

    // ── Standalone scene ──────────────────────────────────────────────────

    public Scene buildScene() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #0f0f1a;");
        root.getChildren().addAll(buildHeader(), buildContent());
        VBox.setVgrow(buildContent(), Priority.ALWAYS);
        Scene scene = new Scene(root, 1200, 750);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        return scene;
    }

    // ── Embeddable content ────────────────────────────────────────────────

    public Node buildContent() {
        ScrollPane scroll = new ScrollPane();
        scroll.getStyleClass().add("scroll-dark");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setContent(buildBody());
        return scroll;
    }

    // ── Header (standalone only) ──────────────────────────────────────────

    private HBox buildHeader() {
        HBox header = new HBox(12);
        header.getStyleClass().add("header-bar");
        header.setAlignment(Pos.CENTER_LEFT);

        Button back = new Button("← Dashboard");
        back.getStyleClass().add("btn-secondary");
        back.setOnAction(e -> MainUI.showDashboard());

        Label title = new Label("Training Centre");
        title.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 15px; -fx-font-weight: bold;");

        header.getChildren().addAll(back, title);
        return header;
    }

    // ── Body ──────────────────────────────────────────────────────────────

    private VBox buildBody() {
        VBox body = new VBox(32);
        body.setStyle("-fx-padding: 36 40 40 40;");

        boolean isPremium = trainingService.isPremiumUser(username);
        List<Lesson> allLessons = trainingService.getAllLessons();
        Map<String, TrainingProgress> progressMap = trainingService.getProgressMap(username);

        int completed = trainingService.countCompleted(username);
        int total     = trainingService.totalLessons();

        body.getChildren().addAll(
                buildTopSection(isPremium, completed, total),
                buildLevelSection("Beginner",     "🟢", allLessons, progressMap, isPremium),
                buildLevelSection("Intermediate", "🟡", allLessons, progressMap, isPremium),
                buildLevelSection("Advanced",     "🔴", allLessons, progressMap, isPremium)
        );
        return body;
    }

    // ── Top section: title + overall progress + upgrade banner ────────────

    private VBox buildTopSection(boolean isPremium, int completed, int total) {
        VBox section = new VBox(16);

        HBox titleRow = new HBox(16);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Typing Training");
        title.getStyleClass().add("label-title");
        Label sub = new Label("Guided lessons from finger placement to speed drills");
        sub.getStyleClass().add("label-muted");
        titleBox.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (!isPremium) {
            Button upgradeBtn = new Button("⭐  Upgrade to Premium");
            upgradeBtn.getStyleClass().add("btn-primary");
            upgradeBtn.setOnAction(e -> MainUI.showSubscription());
            titleRow.getChildren().addAll(titleBox, spacer, upgradeBtn);
        } else {
            Label premiumBadge = new Label("⭐  PREMIUM");
            premiumBadge.setStyle(
                "-fx-background-color: rgba(245,158,11,0.2);" +
                "-fx-text-fill: #fbbf24;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 6 14 6 14;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 13px;");
            titleRow.getChildren().addAll(titleBox, spacer, premiumBadge);
        }

        // Overall progress bar
        VBox progressBox = new VBox(6);
        double pct = total > 0 ? (double) completed / total : 0;
        Label progressLabel = new Label(String.format("Overall Progress — %d / %d lessons completed  (%.0f%%)",
                completed, total, pct * 100));
        progressLabel.getStyleClass().add("label-muted");
        ProgressBar progressBar = new ProgressBar(pct);
        progressBar.getStyleClass().add("progress-bar-custom");
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(10);
        progressBox.getChildren().addAll(progressLabel, progressBar);

        section.getChildren().addAll(titleRow, progressBox);
        if (!isPremium) section.getChildren().add(buildFreeBanner());
        return section;
    }

    private HBox buildFreeBanner() {
        HBox banner = new HBox(12);
        banner.setStyle(
            "-fx-background-color: rgba(124,58,237,0.12);" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 14 20 14 20;" +
            "-fx-border-color: rgba(124,58,237,0.3);" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1;");
        banner.setAlignment(Pos.CENTER_LEFT);

        Label icon  = new Label("🔒");
        icon.setStyle("-fx-font-size: 20px;");
        VBox text   = new VBox(2);
        Label line1 = new Label("Unlock Intermediate & Advanced lessons with Premium");
        line1.setStyle("-fx-text-fill: #a78bfa; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label line2 = new Label("Monthly plan from $4.99 · Lifetime access from $19.99");
        line2.getStyleClass().add("label-muted");
        text.getChildren().addAll(line1, line2);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btn = new Button("See Plans →");
        btn.getStyleClass().add("btn-secondary");
        btn.setOnAction(e -> MainUI.showSubscription());

        banner.getChildren().addAll(icon, text, sp, btn);
        return banner;
    }

    // ── Level section ─────────────────────────────────────────────────────

    private VBox buildLevelSection(String level, String dot, List<Lesson> all,
                                   Map<String, TrainingProgress> progressMap, boolean isPremium) {
        VBox section = new VBox(16);

        // Section header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label dotLabel = new Label(dot);
        dotLabel.setStyle("-fx-font-size: 14px;");
        Label levelLabel = new Label(level + " Lessons");
        levelLabel.getStyleClass().add("label-section");

        long levelCompleted = all.stream()
                .filter(l -> l.getLevel().equals(level))
                .filter(l -> {
                    TrainingProgress p = progressMap.get(l.getLessonId());
                    return p != null && p.isCompleted();
                }).count();
        long levelTotal = all.stream().filter(l -> l.getLevel().equals(level)).count();

        Label countBadge = new Label(levelCompleted + "/" + levelTotal + " done");
        countBadge.setStyle(
            "-fx-background-color: #1e293b;" +
            "-fx-text-fill: #64748b;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 2 10 2 10;" +
            "-fx-font-size: 12px;");

        if ("Intermediate".equals(level) || "Advanced".equals(level)) {
            Label lockTag = new Label(isPremium ? "" : "🔒 Premium");
            lockTag.setStyle(
                "-fx-background-color: rgba(245,158,11,0.15);" +
                "-fx-text-fill: #fbbf24;" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 2 10 2 10;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;");
            if (!isPremium) header.getChildren().addAll(dotLabel, levelLabel, countBadge, lockTag);
            else            header.getChildren().addAll(dotLabel, levelLabel, countBadge);
        } else {
            Label freeTag = new Label("FREE");
            freeTag.setStyle(
                "-fx-background-color: rgba(16,185,129,0.15);" +
                "-fx-text-fill: #10b981;" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 2 10 2 10;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;");
            header.getChildren().addAll(dotLabel, levelLabel, countBadge, freeTag);
        }

        // Lesson cards row (wrapping)
        FlowPane cards = new FlowPane(16, 16);
        cards.setAlignment(Pos.TOP_LEFT);

        all.stream()
           .filter(l -> l.getLevel().equals(level))
           .forEach(lesson -> {
               TrainingProgress progress = progressMap.get(lesson.getLessonId());
               boolean accessible = trainingService.canAccess(username, lesson);
               cards.getChildren().add(buildLessonCard(lesson, progress, accessible));
           });

        section.getChildren().addAll(header, cards);
        return section;
    }

    // ── Lesson card ───────────────────────────────────────────────────────

    private VBox buildLessonCard(Lesson lesson, TrainingProgress progress, boolean accessible) {
        VBox card = new VBox(12);
        card.setPrefWidth(260);
        card.setStyle(
            "-fx-background-color: #1a1a2e;" +
            "-fx-background-radius: 16;" +
            "-fx-padding: 20;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 12, 0, 0, 4);");

        // Top row: number badge + lock or check
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label numBadge = new Label("#" + lesson.getLessonNumber());
        numBadge.setStyle(
            "-fx-background-color: #0f172a;" +
            "-fx-text-fill: #7c3aed;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 2 8 2 8;" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        if (!accessible) {
            Label lockIcon = new Label("🔒");
            lockIcon.setStyle("-fx-font-size: 16px;");
            topRow.getChildren().addAll(numBadge, sp, lockIcon);
        } else if (progress != null && progress.isCompleted()) {
            Label checkIcon = new Label("✅");
            checkIcon.setStyle("-fx-font-size: 16px;");
            topRow.getChildren().addAll(numBadge, sp, checkIcon);
        } else if (progress != null && progress.getAttempts() > 0) {
            Label inProgress = new Label("▶");
            inProgress.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 14px;");
            topRow.getChildren().addAll(numBadge, sp, inProgress);
        } else {
            topRow.getChildren().add(numBadge);
        }

        // Title
        Label titleLabel = new Label(lesson.getTitle());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
        titleLabel.setWrapText(true);

        // Level badge + target WPM
        HBox badgeRow = new HBox(8);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        Label levelBadge = new Label(lesson.getLevel().toUpperCase());
        levelBadge.setStyle(levelBadgeStyle(lesson.getLevel()));
        Label targetLabel = new Label("Target: " + lesson.getTargetWpm() + " WPM");
        targetLabel.getStyleClass().add("label-muted");
        badgeRow.getChildren().addAll(levelBadge, targetLabel);

        // Best WPM / accuracy (if any attempts)
        VBox statsBox = new VBox(2);
        if (progress != null && progress.getAttempts() > 0) {
            Label best = new Label(String.format("Best: %.0f WPM  •  %.0f%% acc",
                    progress.getBestWpm(), progress.getBestAccuracy()));
            best.setStyle("-fx-text-fill: #06b6d4; -fx-font-size: 12px;");
            Label attempts = new Label(progress.getAttempts() + " attempt" +
                    (progress.getAttempts() == 1 ? "" : "s"));
            attempts.getStyleClass().add("label-muted");
            statsBox.getChildren().addAll(best, attempts);
        } else {
            Label notStarted = new Label("Not started yet");
            notStarted.getStyleClass().add("label-muted");
            statsBox.getChildren().add(notStarted);
        }

        // Action button
        Button actionBtn;
        if (!accessible) {
            actionBtn = new Button("🔒  Upgrade to Unlock");
            actionBtn.setStyle(
                "-fx-background-color: rgba(245,158,11,0.15);" +
                "-fx-text-fill: #fbbf24;" +
                "-fx-font-size: 13px;" +
                "-fx-padding: 9 16 9 16;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;");
            actionBtn.setMaxWidth(Double.MAX_VALUE);
            actionBtn.setOnAction(e -> MainUI.showSubscription());
        } else {
            String btnText = (progress != null && progress.isCompleted()) ? "▶  Practice Again" : "▶  Start Lesson";
            actionBtn = new Button(btnText);
            actionBtn.getStyleClass().add("btn-primary");
            actionBtn.setMaxWidth(Double.MAX_VALUE);
            actionBtn.setOnAction(e -> MainUI.showLesson(lesson));
        }

        card.getChildren().addAll(topRow, titleLabel, badgeRow, statsBox, actionBtn);

        // Dim locked cards slightly
        if (!accessible) card.setStyle(card.getStyle() + "-fx-opacity: 0.75;");

        return card;
    }

    private String levelBadgeStyle(String level) {
        String color = switch (level) {
            case "Beginner"     -> "rgba(16,185,129,0.2); -fx-text-fill: #10b981;";
            case "Intermediate" -> "rgba(245,158,11,0.2); -fx-text-fill: #fbbf24;";
            default             -> "rgba(239,68,68,0.2);  -fx-text-fill: #ef4444;";
        };
        return "-fx-background-color: " + color +
               "-fx-background-radius: 8;" +
               "-fx-padding: 2 8 2 8;" +
               "-fx-font-size: 11px;" +
               "-fx-font-weight: bold;";
    }
}