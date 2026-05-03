package ui;

import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ui.TrainingDashboardScreen;
import util.SessionManager;

/**
 * Main dashboard screen.
 * Contains a sidebar and a content area that shows the game-mode selector by default.
 */
public class DashboardScreen {

    private BorderPane root;
    private StackPane  contentArea;

    public Scene buildScene() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #0f0f1a;");

        root.setLeft(buildSidebar());
        root.setTop(buildHeader());

        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #0f0f1a;");
        root.setCenter(contentArea);

        showModeSelector();   // default view

        Scene scene = new Scene(root, 1200, 750);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        return scene;
    }

    // ── Sidebar ───────────────────────────────────────────────────────────

    private VBox buildSidebar() {
        VBox sidebar = new VBox(4);
        sidebar.getStyleClass().add("sidebar");

        // Brand
        Label brand = new Label("⌨  TypeMaster");
        brand.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 0 0 24 0;");

        Button homeBtn     = sidebarBtn("🏠  Home",        true);
        Button lbBtn       = sidebarBtn("🏆  Leaderboard",  false);
        Button statsBtn    = sidebarBtn("📊  Statistics",   false);
        Button trainingBtn = sidebarBtn("📖  Training",     false);

        homeBtn.setOnAction(e -> {
            setActive(homeBtn, lbBtn, statsBtn, trainingBtn);
            showModeSelector();
        });
        lbBtn.setOnAction(e -> {
            setActive(lbBtn, homeBtn, statsBtn, trainingBtn);
            showLeaderboard();
        });
        statsBtn.setOnAction(e -> {
            setActive(statsBtn, homeBtn, lbBtn, trainingBtn);
            showStats();
        });
        trainingBtn.setOnAction(e -> {
            setActive(trainingBtn, homeBtn, lbBtn, statsBtn);
            showTraining();
        });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // User info
        VBox userBox = new VBox(4);
        userBox.setStyle("-fx-background-color: rgba(124,58,237,0.1); -fx-background-radius: 10; -fx-padding: 12;");
        Label userIcon = new Label("👤");
        userIcon.setStyle("-fx-font-size: 22px;");
        Label userName = new Label(SessionManager.getInstance().getUsername());
        userName.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        Button logoutBtn = new Button("Sign Out");
        logoutBtn.getStyleClass().add("btn-danger");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setPadding(new Insets(7, 14, 7, 14));
        logoutBtn.setOnAction(e -> {
            SessionManager.getInstance().logout();
            MainUI.showLogin();
        });
        userBox.getChildren().addAll(userIcon, userName, logoutBtn);

        sidebar.getChildren().addAll(brand, homeBtn, lbBtn, statsBtn, trainingBtn, spacer, userBox);
        return sidebar;
    }

    // ── Header ────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        HBox header = new HBox();
        header.getStyleClass().add("header-bar");
        header.setAlignment(Pos.CENTER_LEFT);

        Label greeting = new Label("Good day, " + SessionManager.getInstance().getUsername() + " 👋");
        greeting.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label tagline = new Label("Type. Improve. Master.");
        tagline.getStyleClass().add("label-muted");

        header.getChildren().addAll(greeting, spacer, tagline);
        return header;
    }

    // ── Content: Mode Selector ────────────────────────────────────────────

    private void showModeSelector() {
        contentArea.getChildren().clear();

        ScrollPane scroll = new ScrollPane();
        scroll.getStyleClass().add("scroll-dark");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox content = new VBox(32);
        content.setStyle("-fx-padding: 36 40 36 40;");
        content.setAlignment(Pos.TOP_LEFT);

        // Section title
        Label sectionTitle = new Label("Choose Game Mode");
        sectionTitle.getStyleClass().add("label-section");

        Label sectionSub = new Label("Select how you want to test your typing skills today");
        sectionSub.getStyleClass().add("label-muted");

        VBox headerBox = new VBox(6, sectionTitle, sectionSub);

        // Stats summary row
        HBox statsRow = buildStatsRow();

        // Mode cards
        HBox modeCards = new HBox(24);
        modeCards.setAlignment(Pos.CENTER_LEFT);
        modeCards.getChildren().addAll(
                buildModeCard("Practice",
                        "📝", "Free typing — no timer, no pressure.",
                        "Great for warming up", "badge-practice"),
                buildModeCard("Normal",
                        "⌨", "Type a 50-word passage as fast as you can.",
                        "Score saved to leaderboard", "badge-normal"),
                buildModeCard("Timer",
                        "⏱", "Race against the clock — pick your time limit.",
                        "15s / 30s / 60s / 120s", "badge-timer")
        );

        content.getChildren().addAll(headerBox, statsRow, modeCards);
        scroll.setContent(content);
        contentArea.getChildren().add(scroll);
    }

    private HBox buildStatsRow() {
        HBox row = new HBox(16);
        var user = SessionManager.getInstance().getCurrentUser();

        double bestWpm  = user != null ? user.getBestWpm()      : 0;
        double avgWpm   = user != null ? user.getAverageWpm()   : 0;
        double bestAcc  = user != null ? user.getBestAccuracy() : 0;
        int    games    = user != null ? user.getTotalGames()   : 0;

        row.getChildren().addAll(
                miniStat("Best WPM",   String.format("%.0f", bestWpm),  "stat-number"),
                miniStat("Avg WPM",    String.format("%.0f", avgWpm),   "stat-number-cyan"),
                miniStat("Best Acc",   String.format("%.0f%%", bestAcc),"stat-number-green"),
                miniStat("Games",      String.valueOf(games),            "stat-number-orange")
        );
        return row;
    }

    private VBox miniStat(String label, String value, String numStyle) {
        VBox box = new VBox(4);
        box.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 12; -fx-padding: 18 24 18 24;");
        Label val = new Label(value);
        val.getStyleClass().add(numStyle);
        val.setStyle(val.getStyle() + "-fx-font-size: 26px;");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("label-muted");
        box.getChildren().addAll(val, lbl);
        return box;
    }

    private VBox buildModeCard(String mode, String icon, String desc, String note, String badgeStyle) {
        VBox card = new VBox(16);
        card.getStyleClass().add("mode-card");

        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("mode-icon");

        Label nameLabel = new Label(mode);
        nameLabel.getStyleClass().add("label-section");

        Label descLabel = new Label(desc);
        descLabel.getStyleClass().add("label-body");
        descLabel.setWrapText(true);

        Label badge = new Label(note.toUpperCase());
        badge.getStyleClass().add(badgeStyle);

        Button playBtn = new Button("Play " + mode);
        playBtn.getStyleClass().add("btn-primary");
        playBtn.setMaxWidth(Double.MAX_VALUE);
        playBtn.setOnAction(e -> handleModeSelected(mode));

        card.getChildren().addAll(iconLabel, nameLabel, descLabel, badge, playBtn);
        return card;
    }

    private void handleModeSelected(String mode) {
        if ("Timer".equals(mode)) {
            showTimerConfig();
        } else {
            showDifficultyPicker(mode, 0);
        }
    }

    // ── Content: Timer Config ─────────────────────────────────────────────

    private void showTimerConfig() {
        contentArea.getChildren().clear();

        VBox content = new VBox(24);
        content.setStyle("-fx-padding: 60 80 40 80;");
        content.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Timer Mode");
        title.getStyleClass().add("label-title");
        Label sub = new Label("How long do you want to run?");
        sub.getStyleClass().add("label-muted");

        // Time buttons
        HBox timeRow = new HBox(14);
        int[] times = {15, 30, 60, 120};
        final int[] chosen = {60};
        ToggleGroup tg = new ToggleGroup();

        for (int t : times) {
            ToggleButton tb = new ToggleButton(t + "s");
            tb.setToggleGroup(tg);
            tb.getStyleClass().add("diff-btn");
            tb.setPadding(new Insets(10, 26, 10, 26));
            if (t == chosen[0]) {
                tb.setSelected(true);
                tb.getStyleClass().setAll("diff-btn-selected");
            }
            int finalT = t;
            tb.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    chosen[0] = finalT;
                    tb.getStyleClass().setAll("diff-btn-selected");
                } else {
                    tb.getStyleClass().setAll("diff-btn");
                }
            });
            timeRow.getChildren().add(tb);
        }

        // Difficulty row
        Label diffLabel = new Label("Difficulty");
        diffLabel.getStyleClass().add("label-section");
        String[] diffs = {"Easy", "Medium", "Hard"};
        final String[] chosenDiff = {"Easy"};
        ToggleGroup tg2 = new ToggleGroup();
        HBox diffRow = new HBox(10);
        for (String d : diffs) {
            ToggleButton tb = new ToggleButton(d);
            tb.setToggleGroup(tg2);
            tb.getStyleClass().add("diff-btn");
            if ("Easy".equals(d)) {
                tb.setSelected(true);
                tb.getStyleClass().setAll("diff-btn-selected");
            }
            tb.selectedProperty().addListener((obs, oldSel, isSel) -> {
                if (isSel) {
                    chosenDiff[0] = d;
                    tb.getStyleClass().setAll("diff-btn-selected");
                } else {
                    tb.getStyleClass().setAll("diff-btn");
                }
            });
            diffRow.getChildren().add(tb);
        }

        Button start = new Button("Start Timer Game ▶");
        start.getStyleClass().add("btn-success");
        start.setOnAction(e -> MainUI.showGame("Timer", chosenDiff[0], chosen[0]));

        Button back = new Button("← Back");
        back.getStyleClass().add("btn-secondary");
        back.setOnAction(e -> showModeSelector());

        content.getChildren().addAll(
                title, sub, gap(8),
                new Label("Duration") {{ getStyleClass().add("label-section"); }},
                timeRow, gap(8),
                diffLabel, diffRow,
                gap(16), start, back
        );
        contentArea.getChildren().add(content);
    }

    // ── Content: Difficulty Picker ────────────────────────────────────────

    private void showDifficultyPicker(String mode, int timerSecs) {
        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setStyle("-fx-padding: 60 80 40 80;");
        content.setAlignment(Pos.TOP_LEFT);

        Label title = new Label(mode + " Mode — Difficulty");
        title.getStyleClass().add("label-title");
        Label sub = new Label("Choose your challenge level");
        sub.getStyleClass().add("label-muted");

        HBox row = new HBox(14);
        String[] diffs = {"Easy", "Medium", "Hard"};
        final String[] chosen = {"Easy"};
        ToggleGroup tg = new ToggleGroup();

        for (String d : diffs) {
            ToggleButton tb = new ToggleButton(d);
            tb.setToggleGroup(tg);
            tb.getStyleClass().add("diff-btn");
            if ("Easy".equals(d)) {
                tb.setSelected(true);
                tb.getStyleClass().setAll("diff-btn-selected");
            }
            tb.selectedProperty().addListener((obs, old, isSel) -> {
                if (isSel) {
                    chosen[0] = d;
                    tb.getStyleClass().setAll("diff-btn-selected");
                } else {
                    tb.getStyleClass().setAll("diff-btn");
                }
            });
            row.getChildren().add(tb);
        }

        Button start = new Button("Start Game  ▶");
        start.getStyleClass().add("btn-success");
        start.setOnAction(e -> MainUI.showGame(mode, chosen[0], timerSecs));

        Button back = new Button("← Back");
        back.getStyleClass().add("btn-secondary");
        back.setOnAction(e -> showModeSelector());

        content.getChildren().addAll(title, sub, gap(8), row, gap(16), start, back);
        contentArea.getChildren().add(content);
    }

    // ── Content: Leaderboard / Stats (delegate to full screens) ──────────

    private void showLeaderboard() {
        contentArea.getChildren().clear();
        LeaderboardScreen lbScreen = new LeaderboardScreen();
        contentArea.getChildren().add(lbScreen.buildContent());
    }

    private void showStats() {
        contentArea.getChildren().clear();
        StatsScreen statsScreen = new StatsScreen();
        contentArea.getChildren().add(statsScreen.buildContent());
    }

    private void showTraining() {
        contentArea.getChildren().clear();
        TrainingDashboardScreen trainingScreen = new TrainingDashboardScreen();
        contentArea.getChildren().add(trainingScreen.buildContent());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Button sidebarBtn(String text, boolean active) {
        Button b = new Button(text);
        b.getStyleClass().add(active ? "sidebar-btn-active" : "sidebar-btn");
        return b;
    }

    private void setActive(Button active, Button... others) {
        active.getStyleClass().setAll("sidebar-btn-active");
        for (Button b : others) b.getStyleClass().setAll("sidebar-btn");
    }

    private Region gap(int h) {
        Region r = new Region();
        r.setMinHeight(h);
        return r;
    }
}
