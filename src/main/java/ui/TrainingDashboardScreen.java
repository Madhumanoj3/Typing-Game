package ui;

import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.Lesson;
import model.Subscription;
import model.TrainingCertificate;
import model.TrainingProgress;
import service.BillPdfService;
import service.CertificationService;
import service.LivePerformanceService;
import service.PaymentService;
import service.TrainingService;
import util.SessionManager;

import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Training module dashboard — shows all lessons grouped by level with progress indicators.
 * Designed to be embedded inside DashboardScreen's content area via {@link #buildContent()},
 * or displayed as a standalone scene via {@link #buildScene()}.
 */
public class TrainingDashboardScreen {

    private final TrainingService trainingService = TrainingService.getInstance();
    private final PaymentService paymentService = PaymentService.getInstance();
    private final CertificationService certificationService = CertificationService.getInstance();
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
        cardColorIndex = 0; // Reset for each build
        VBox body = new VBox(32);
        body.setStyle("-fx-padding: 36 40 40 40;");

        boolean isPremium = trainingService.isPremiumUser(username);
        Subscription subscription = paymentService.getSubscription(username);
        List<Lesson> allLessons = trainingService.getAllLessons();
        Map<String, TrainingProgress> progressMap = trainingService.getProgressMap(username);

        int completed = trainingService.countCompleted(username);
        int total     = trainingService.totalLessons();

        body.getChildren().addAll(
                buildTopSection(isPremium, subscription, completed, total),
                buildInsightsSection(isPremium, progressMap, completed, total),
                buildLevelSection("Beginner",     "🟢", allLessons, progressMap, isPremium),
                buildLevelSection("Intermediate", "🟡", allLessons, progressMap, isPremium),
                buildLevelSection("Advanced",     "🔴", allLessons, progressMap, isPremium)
        );
        return body;
    }

    // ── Top section: title + overall progress + upgrade banner ────────────

    private VBox buildTopSection(boolean isPremium, Subscription subscription, int completed, int total) {
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

            Button viewBillBtn = new Button("📄  View Bill");
            viewBillBtn.getStyleClass().add("btn-secondary");
            viewBillBtn.setOnAction(e -> viewBill(subscription));

            HBox premiumActions = new HBox(10, premiumBadge, viewBillBtn);
            premiumActions.setAlignment(Pos.CENTER_RIGHT);

            titleRow.getChildren().addAll(titleBox, spacer, premiumActions);
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

    private VBox buildInsightsSection(boolean isPremium, Map<String, TrainingProgress> progressMap,
                                      int completed, int total) {
        VBox section = new VBox(16);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Performance Intelligence");
        title.getStyleClass().add("label-section");
        Label tag = new Label("LIVE");
        tag.setStyle(
            "-fx-background-color: rgba(16,185,129,0.15);" +
            "-fx-text-fill: #10b981;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 2 10 2 10;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;");
        header.getChildren().addAll(title, tag);

        HBox cards = new HBox(16);
        cards.setAlignment(Pos.TOP_LEFT);
        cards.getChildren().addAll(
                buildLiveStatsCard(),
                buildHeatmapCard(),
                buildCertificationCard(isPremium, completed, total)
        );

        Label recommendation = new Label(buildRecommendationText(progressMap));
        recommendation.setWrapText(true);
        recommendation.setStyle(
            "-fx-background-color: rgba(6,182,212,0.08);" +
            "-fx-text-fill: #94a3b8;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: rgba(6,182,212,0.25);" +
            "-fx-border-radius: 12;" +
            "-fx-padding: 12 16 12 16;" +
            "-fx-font-size: 12px;");

        section.getChildren().addAll(header, cards, recommendation);
        return section;
    }

    private VBox buildLiveStatsCard() {
        VBox card = insightCard();
        Label value = new Label("0 WPM");
        value.getStyleClass().add("stat-number-cyan");
        value.setStyle(value.getStyle() + "-fx-font-size: 28px;");

        Label title = new Label("Real-time Metrics");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        Label detail = new Label("Waiting for a typing session");
        detail.getStyleClass().add("label-muted");
        detail.setWrapText(true);

        Label notification = new Label("Live achievement notifications enabled");
        notification.setStyle("-fx-text-fill: #10b981; -fx-font-size: 12px;");
        notification.setWrapText(true);

        LivePerformanceService live = LivePerformanceService.getInstance();
        live.addMetricListener(metric -> {
            if (!username.equals(metric.username())) return;
            value.setText(String.format("%.0f WPM", metric.wpm()));
            detail.setText(String.format("%s • %.0f%% accuracy • %d errors",
                    metric.context(), metric.accuracy(), metric.errors()));
        });
        live.addNotificationListener(note -> {
            if (!username.equals(note.username())) return;
            notification.setText(note.title() + ": " + note.message());
        });

        card.getChildren().addAll(value, title, detail, notification);
        return card;
    }

    private VBox buildHeatmapCard() {
        VBox card = insightCard();
        Label title = new Label("Keyboard Heatmap");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        FlowPane keys = new FlowPane(6, 6);
        Map<String, Integer> heatmap = trainingService.aggregateKeyErrors(username);
        if (heatmap.isEmpty()) {
            Label empty = new Label("No key errors tracked yet");
            empty.getStyleClass().add("label-muted");
            keys.getChildren().add(empty);
        } else {
            int max = heatmap.values().stream().max(Integer::compareTo).orElse(1);
            heatmap.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                    .limit(12)
                    .forEach(entry -> keys.getChildren().add(keyChip(entry.getKey(), entry.getValue(), max)));
        }

        Label style = new Label("Typing style: " + trainingService.overallTypingStyle(username));
        style.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 12px; -fx-font-weight: bold;");
        Label drill = new Label(trainingService.targetedPracticeText(username));
        drill.getStyleClass().add("label-muted");
        drill.setWrapText(true);

        card.getChildren().addAll(title, keys, style, drill);
        return card;
    }

    private VBox buildCertificationCard(boolean isPremium, int completed, int total) {
        VBox card = insightCard();
        Label title = new Label("Certification");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label progress = new Label(completed + "/" + total + " lessons complete");
        progress.getStyleClass().add("label-muted");

        certificationService.requestReviewIfEligible(username);
        TrainingCertificate cert = certificationService.getCertificate(username);
        boolean canExport = certificationService.canExport(username);
        Label badge = new Label(canExport ? "Grade " + cert.getGrade() + " approved" : certificationService.statusMessage(username));
        badge.setStyle(
            "-fx-background-color: " + (canExport ? "rgba(16,185,129,0.15);" : "rgba(245,158,11,0.15);") +
            "-fx-text-fill: " + (canExport ? "#10b981;" : "#fbbf24;") +
            "-fx-background-radius: 8;" +
            "-fx-padding: 5 10 5 10;" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;");
        badge.setWrapText(true);

        Button export = new Button(canExport ? "Export Certificate" : (isPremium ? "Awaiting Grade" : "Upgrade"));
        export.getStyleClass().add(canExport ? "btn-gold" : "btn-secondary");
        export.setMaxWidth(Double.MAX_VALUE);
        export.setOnAction(e -> {
            if (!isPremium) {
                MainUI.showSubscription();
                return;
            }
            if (!canExport) {
                AppDialogs.showInfo("Certificate Locked", certificationService.statusMessage(username));
                return;
            }
            exportCertificate();
        });

        card.getChildren().addAll(title, progress, badge, export);
        return card;
    }

    private VBox insightCard() {
        VBox card = new VBox(10);
        card.setPrefWidth(300);
        card.setMinHeight(168);
        card.setStyle(
            "-fx-background-color: #1a1a2e;" +
            "-fx-background-radius: 16;" +
            "-fx-padding: 18;" +
            "-fx-border-color: rgba(255,255,255,0.06);" +
            "-fx-border-radius: 16;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 12, 0, 0, 4);");
        return card;
    }

    private Label keyChip(String key, int count, int max) {
        double heat = max > 0 ? (double) count / max : 0;
        String color = heat > 0.66 ? "#ef4444" : heat > 0.33 ? "#f59e0b" : "#7c3aed";
        Label chip = new Label(key + "  " + count);
        chip.setStyle(
            "-fx-background-color: " + color + "33;" +
            "-fx-text-fill: " + color + ";" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 5 9 5 9;" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;");
        return chip;
    }

    private String buildRecommendationText(Map<String, TrainingProgress> progressMap) {
        String keys = trainingService.topTroubleKeys(username, 5);
        String style = trainingService.overallTypingStyle(username);
        if (!keys.isBlank()) {
            return "Pattern recognition: " + style + " typing style detected. Suggested drill focuses on these keys: " + keys + ".";
        }
        if (progressMap.isEmpty()) {
            return "Pattern recognition starts after your first lesson attempt. Live metrics are already active during games and lessons.";
        }
        return "Pattern recognition: " + style + " typing style detected. Keep building clean repetitions to unlock sharper key suggestions.";
    }

    private void exportCertificate() {
        try {
            Path certificate = certificationService.exportCertificate(username);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(certificate.toFile());
            } else {
                AppDialogs.showInfo("Certificate Exported", "PDF certificate saved at:\n" + certificate.toAbsolutePath());
            }
        } catch (Exception ex) {
            AppDialogs.showError("Certificate Export Failed", ex.getMessage());
        }
    }

    private void viewBill(Subscription subscription) {
        if (subscription == null || !subscription.isActive()) {
            AppDialogs.showInfo("Bill Not Available", "Your premium bill will be available after admin verification.");
            return;
        }

        try {
            Path billPath = subscription.getBillPath() != null && !subscription.getBillPath().isBlank()
                    ? Path.of(subscription.getBillPath())
                    : BillPdfService.getInstance().generateBill(subscription);

            if (!Files.exists(billPath)) {
                billPath = BillPdfService.getInstance().generateBill(subscription);
            }

            if (!Desktop.isDesktopSupported()) {
                AppDialogs.showInfo("Bill Ready", "Your bill PDF is saved at:\n" + billPath);
                return;
            }

            Desktop.getDesktop().open(billPath.toFile());
        } catch (Exception ex) {
            AppDialogs.showError("Unable to Open Bill", "Could not show the bill PDF.\n" + ex.getMessage());
        }
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
        Label line2 = new Label("Monthly plan from Rs.199 · Lifetime access from Rs.1999");
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

    private static final String[] CARD_COLORS = {"card-blue", "card-pink", "card-yellow", "card-red", "card-green", "card-purple"};
    private static int cardColorIndex = 0;

    private VBox buildLessonCard(Lesson lesson, TrainingProgress progress, boolean accessible) {
        VBox card = new VBox(12);
        card.setPrefWidth(260);

        // Apply colorful card style
        String colorClass = CARD_COLORS[cardColorIndex % CARD_COLORS.length];
        card.getStyleClass().add(colorClass);
        cardColorIndex++;

        if (!accessible) {
            card.setStyle(card.getStyle() + "; -fx-opacity: 0.6;");
        }

        // Top row: number badge + lock or check
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label numBadge = new Label("#" + lesson.getLessonNumber());
        numBadge.setStyle(
            "-fx-background-color: rgba(59,130,246,0.2);" +
            "-fx-text-fill: #1a1a1a;" +
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
        titleLabel.setStyle("-fx-text-fill: #1a1a1a; -fx-font-size: 15px; -fx-font-weight: bold;");
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
            best.setStyle("-fx-text-fill: #1a1a1a; -fx-font-size: 12px; -fx-font-weight: bold;");
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
            case "Beginner"     -> "rgba(16,185,129,0.25); -fx-text-fill: #10b981;";
            case "Intermediate" -> "rgba(251,191,36,0.25); -fx-text-fill: #fbbf24;";
            default             -> "rgba(239,68,68,0.25);  -fx-text-fill: #ef4444;";
        };
        return "-fx-background-color: " + color +
               "-fx-background-radius: 8;" +
               "-fx-padding: 2 8 2 8;" +
               "-fx-font-size: 11px;" +
               "-fx-font-weight: bold;";
    }
}
