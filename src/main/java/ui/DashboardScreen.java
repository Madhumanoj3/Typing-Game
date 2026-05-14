package ui;

import db.MongoDBManager;
import game.ThemeManager;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import model.GameResult;
import model.User;
import model.UserInventory;
import model.UserStats;
import service.GamificationService;
import service.GamificationService.DailyChallengeProgress;
import service.StoreService;
import util.SessionManager;

import java.util.List;
import java.util.function.Consumer;

public class DashboardScreen {

    private final String initialPage;

    private BorderPane root;
    private StackPane  contentArea;
    private Button[]   allNavBtns;

    public DashboardScreen() { this("home"); }
    public DashboardScreen(String initialPage) { this.initialPage = initialPage; }

    public Scene buildScene() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #0f0f1a;");

        root.setLeft(buildSidebar());
        root.setTop(buildHeader());

        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #0f0f1a;");
        root.setCenter(contentArea);

        switch (initialPage) {
            case "store":   setActive(allNavBtns[8]); showStore();   break;
            case "profile": setActive(allNavBtns[9]); showProfile(); break;
            default:        showHome(); break;
        }

        Scene scene = new Scene(root, 1200, 750);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        return scene;
    }

    // ── Sidebar ───────────────────────────────────────────────────────────

    private VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setFillWidth(true);

        Label brand = new Label("⌨  TypeMaster");
        brand.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 18px;" +
                       "-fx-font-weight: bold; -fx-padding: 0 0 12 0;");

        Button homeBtn      = sidebarBtn("🏠  Home",             true);
        Button playBtn      = sidebarBtn("🎮  Play",             false);
        Button trainBtn     = sidebarBtn("🎓  Training",         false);
        Button lbBtn        = sidebarBtn("🏆  Leaderboard",      false);
        Button statsBtn     = sidebarBtn("📊  Statistics",       false);
        Button achBtn       = sidebarBtn("🎖  Achievements",     false);
        Button challengeBtn = sidebarBtn("🔥  Daily Challenges", false);
        Button rewardsBtn   = sidebarBtn("🎁  Rewards",          false);
        Button storeBtn     = sidebarBtn("🛒  Store",            false);
        Button profileBtn   = sidebarBtn("👤  Profile",          false);

        allNavBtns = new Button[]{
            homeBtn, playBtn, trainBtn, lbBtn, statsBtn,
            achBtn, challengeBtn, rewardsBtn, storeBtn, profileBtn
        };

        homeBtn.setOnAction(e      -> { setActive(homeBtn);      showHome(); });
        playBtn.setOnAction(e      -> { setActive(playBtn);      showModeSelector(); });
        trainBtn.setOnAction(e     -> { setActive(trainBtn);     showTraining(); });
        lbBtn.setOnAction(e        -> { setActive(lbBtn);        showLeaderboard(); });
        statsBtn.setOnAction(e     -> { setActive(statsBtn);     showStats(); });
        achBtn.setOnAction(e       -> { setActive(achBtn);       showAchievements(); });
        challengeBtn.setOnAction(e -> { setActive(challengeBtn); showDailyChallenges(); });
        rewardsBtn.setOnAction(e   -> { setActive(rewardsBtn);   showRewards(); });
        storeBtn.setOnAction(e     -> { setActive(storeBtn);      showStore(); });
        profileBtn.setOnAction(e   -> { setActive(profileBtn);   showProfile(); });

        // Nav buttons in a scroll pane so they never overflow the window height
        VBox navButtons = new VBox(3,
                homeBtn, playBtn, trainBtn, lbBtn, statsBtn,
                achBtn, challengeBtn, rewardsBtn, storeBtn, profileBtn);
        navButtons.setStyle("-fx-padding: 0;");

        ScrollPane navScroll = new ScrollPane(navButtons);
        navScroll.setFitToWidth(true);
        navScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        navScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        navScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;" +
                           "-fx-border-color: transparent;");
        VBox.setVgrow(navScroll, Priority.ALWAYS);

        sidebar.getChildren().addAll(brand, navScroll, buildUserBox());
        return sidebar;
    }

    private VBox buildUserBox() {
        String    username = SessionManager.getInstance().getUsername();
        UserStats stats    = null;
        try { stats = GamificationService.getInstance().getStats(username); } catch (Exception ignored) {}
        User sidebarUser = null;
        try { sidebarUser = MongoDBManager.getInstance().getUserByUsername(username); } catch (Exception ignored) {}

        int    xp       = stats != null ? stats.getXp()     : 0;
        int    level    = stats != null ? stats.getLevel()   : 1;
        int    streak   = stats != null ? stats.getStreak()  : 0;
        double progress = stats != null ? GamificationService.getInstance().levelProgress(xp) : 0.0;
        int    toNext   = stats != null ? GamificationService.getInstance().xpToNextLevel(xp)  : 100;

        VBox box = new VBox(6);
        box.setStyle("-fx-background-color: rgba(124,58,237,0.1);" +
                     "-fx-background-radius: 10; -fx-padding: 12;");

        // Clickable avatar circle
        String sidebarAvatarId = sidebarUser != null ? sidebarUser.getAvatarId() : null;
        StackPane avatarWrap = buildAvatarCircle(sidebarAvatarId, username, 40);
        avatarWrap.setStyle("-fx-cursor: hand;");
        avatarWrap.setOnMouseClicked(e -> showAvatarPicker());

        Label userName = new Label(username);
        userName.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label levelBadge = new Label("Level " + level + " — " + levelTitle(level));
        levelBadge.setStyle(
            "-fx-background-color: rgba(124,58,237,0.25);" +
            "-fx-text-fill: #a78bfa;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 2 8 2 8;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;");

        ProgressBar xpBar = new ProgressBar(Math.min(progress, 1.0));
        xpBar.getStyleClass().add("progress-bar-xp");
        xpBar.setMaxWidth(Double.MAX_VALUE);
        xpBar.setPrefHeight(7);

        Label xpLbl = new Label(xp + " XP  •  " + toNext + " to Level " + (level + 1));
        xpLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px;");

        String streakIcon = streak >= 7 ? "🏆" : streak >= 3 ? "🔥" : "✨";
        Label streakLbl   = new Label(streakIcon + "  " + streak + " day" + (streak == 1 ? "" : "s") + " streak");
        streakLbl.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 11px; -fx-font-weight: bold;");

        Button logoutBtn = new Button("Sign Out");
        logoutBtn.getStyleClass().add("btn-danger");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setPadding(new Insets(7, 14, 7, 14));
        logoutBtn.setOnAction(e -> {
            SessionManager.getInstance().logout();
            MainUI.showLogin();
        });

        box.getChildren().addAll(avatarWrap, userName, levelBadge, xpBar, xpLbl, streakLbl, logoutBtn);
        return box;
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

        Label tagline = new Label("Type. Level Up. Master.");
        tagline.getStyleClass().add("label-muted");

        header.getChildren().addAll(greeting, spacer, tagline);
        return header;
    }

    // ── HOME: Dashboard Overview ──────────────────────────────────────────

    private void showHome() {
        contentArea.getChildren().clear();

        ScrollPane scroll = new ScrollPane();
        scroll.getStyleClass().add("scroll-dark");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox content = new VBox(24);
        content.setStyle("-fx-padding: 36 40 36 40;");

        String    username = SessionManager.getInstance().getUsername();
        UserStats stats    = null;
        try { stats = GamificationService.getInstance().getStats(username); } catch (Exception ignored) {}

        int    xp      = stats != null ? stats.getXp()     : 0;
        int    level   = stats != null ? stats.getLevel()   : 1;
        int    streak  = stats != null ? stats.getStreak()  : 0;
        double prog    = stats != null ? GamificationService.getInstance().levelProgress(xp) : 0.0;
        int    toNext  = stats != null ? GamificationService.getInstance().xpToNextLevel(xp)  : 100;

        // Welcome banner
        VBox welcomeBox = new VBox(6);
        Label welcome = new Label("Welcome back, " + username + "! 🎉");
        welcome.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label welcomeSub = new Label("Continue your typing journey — every keystroke counts.");
        welcomeSub.getStyleClass().add("label-muted");
        welcomeBox.getChildren().addAll(welcome, welcomeSub);

        // Level / XP / Streak cards row
        VBox levelCard = buildInfoCard("🏅  Current Level", "Level " + level, levelTitle(level), "#a78bfa");

        VBox xpCard = new VBox(8);
        xpCard.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 14; -fx-padding: 20;");
        HBox.setHgrow(xpCard, Priority.ALWAYS);
        Label xpTitle = new Label("⚡  XP Progress");
        xpTitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label xpVal = new Label(xp + " XP");
        xpVal.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 24px; -fx-font-weight: bold;");
        ProgressBar homeXpBar = new ProgressBar(Math.min(prog, 1.0));
        homeXpBar.getStyleClass().add("progress-bar-xp");
        homeXpBar.setMaxWidth(Double.MAX_VALUE);
        homeXpBar.setPrefHeight(8);
        Label xpSub = new Label(toNext + " XP to Level " + (level + 1));
        xpSub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        xpCard.getChildren().addAll(xpTitle, xpVal, homeXpBar, xpSub);

        String streakIcon = streak >= 7 ? "🏆" : streak >= 3 ? "🔥" : "✨";
        String streakMsg  = streak >= 7 ? "On fire!" : streak >= 3 ? "Keep going!" : "Just started!";
        VBox streakCard = buildInfoCard(streakIcon + "  Daily Streak",
                streak + " day" + (streak == 1 ? "" : "s"), streakMsg, "#fbbf24");

        HBox topRow = new HBox(16);
        topRow.getChildren().addAll(levelCard, xpCard, streakCard);

        // Quick performance stats — fetch fresh from DB so post-game values are current
        User user = null;
        try { user = MongoDBManager.getInstance().getUserByUsername(username); } catch (Exception ignored) {}
        if (user == null) user = SessionManager.getInstance().getCurrentUser();
        double bestWpm = user != null ? user.getBestWpm()      : 0;
        double avgWpm  = user != null ? user.getAverageWpm()   : 0;
        double bestAcc = user != null ? user.getBestAccuracy() : 0;
        int    games   = user != null ? user.getTotalGames()   : 0;

        HBox statsRow = new HBox(16);
        statsRow.getChildren().addAll(
            miniStat("Best WPM",  String.format("%.0f",   bestWpm), "stat-number"),
            miniStat("Avg WPM",   String.format("%.0f",   avgWpm),  "stat-number-cyan"),
            miniStat("Best Acc",  String.format("%.0f%%", bestAcc), "stat-number-green"),
            miniStat("Games",     String.valueOf(games),             "stat-number-orange")
        );

        // Recent activity + daily challenges preview
        HBox bottomRow = new HBox(20);
        bottomRow.setAlignment(Pos.TOP_LEFT);

        VBox activityCard = buildRecentActivityCard();
        HBox.setHgrow(activityCard, Priority.ALWAYS);

        VBox miniChallengeCard = buildDailyChallengesCard();
        miniChallengeCard.setMinWidth(320);
        miniChallengeCard.setMaxWidth(360);

        bottomRow.getChildren().addAll(activityCard, miniChallengeCard);

        // Quick action buttons
        HBox quickActions = new HBox(14);
        Button playNow   = new Button("🎮  Play Now");
        playNow.getStyleClass().add("btn-success");
        playNow.setOnAction(e -> { setActive(allNavBtns[1]); showModeSelector(); });
        Button goTrain   = new Button("🎓  Start Training");
        goTrain.getStyleClass().add("btn-primary");
        goTrain.setOnAction(e -> { setActive(allNavBtns[2]); showTraining(); });
        Button goLeader  = new Button("🏆  Leaderboard");
        goLeader.getStyleClass().add("btn-secondary");
        goLeader.setOnAction(e -> { setActive(allNavBtns[3]); showLeaderboard(); });
        quickActions.getChildren().addAll(playNow, goTrain, goLeader);

        content.getChildren().addAll(welcomeBox, topRow, statsRow, bottomRow, quickActions);
        scroll.setContent(content);
        contentArea.getChildren().add(scroll);
    }

    private VBox buildInfoCard(String title, String value, String sub, String color) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 14; -fx-padding: 20;");
        card.setPrefWidth(200);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label subLbl = new Label(sub);
        subLbl.setStyle("-fx-background-color: rgba(124,58,237,0.15);" +
                        "-fx-text-fill: " + color + ";" +
                        "-fx-background-radius: 6; -fx-padding: 2 8 2 8;" +
                        "-fx-font-size: 11px;");
        card.getChildren().addAll(titleLbl, valueLbl, subLbl);
        return card;
    }

    private VBox buildRecentActivityCard() {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 14; -fx-padding: 20;");

        Label title = new Label("📈  Recent Activity");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        card.getChildren().add(title);

        String          username = SessionManager.getInstance().getUsername();
        List<GameResult> recent  = MongoDBManager.getInstance().getResultsForUser(username, 5);

        if (recent.isEmpty()) {
            Label empty = new Label("No games played yet. Start playing to see activity!");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
            card.getChildren().add(empty);
        } else {
            for (GameResult r : recent) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                Label modeIcon = new Label(
                    "Practice".equals(r.getGameMode()) ? "📝" :
                    "Timer".equals(r.getGameMode())    ? "⏱" : "⌨");
                modeIcon.setStyle("-fx-font-size: 16px;");
                VBox info = new VBox(2);
                Label modeLbl = new Label(r.getGameMode() + " — " + r.getDifficulty());
                modeLbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px; -fx-font-weight: bold;");
                Label statsLbl = new Label(String.format("%.0f WPM  •  %.0f%% acc  •  %d errors",
                        r.getWpm(), r.getAccuracy(), r.getErrorCount()));
                statsLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
                info.getChildren().addAll(modeLbl, statsLbl);
                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);
                Label wpmBadge = new Label(String.format("%.0f WPM", r.getWpm()));
                wpmBadge.setStyle(
                    "-fx-background-color: rgba(124,58,237,0.2);" +
                    "-fx-text-fill: #a78bfa;" +
                    "-fx-background-radius: 8; -fx-padding: 4 10 4 10;" +
                    "-fx-font-size: 11px; -fx-font-weight: bold;");
                row.getChildren().addAll(modeIcon, info, sp, wpmBadge);
                card.getChildren().add(row);
                card.getChildren().add(buildSep());
            }
        }
        return card;
    }

    // ── PLAY: Mode Selector ───────────────────────────────────────────────

    private void showModeSelector() {
        contentArea.getChildren().clear();

        ScrollPane scroll = new ScrollPane();
        scroll.getStyleClass().add("scroll-dark");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox content = new VBox(28);
        content.setStyle("-fx-padding: 36 40 36 40;");
        content.setAlignment(Pos.TOP_LEFT);

        Label sectionTitle = new Label("Choose Game Mode");
        sectionTitle.getStyleClass().add("label-section");
        Label sectionSub = new Label("Select how you want to test your typing skills today");
        sectionSub.getStyleClass().add("label-muted");
        VBox headerBox = new VBox(6, sectionTitle, sectionSub);

        HBox modeCards = new HBox(24);
        modeCards.setAlignment(Pos.CENTER_LEFT);
        modeCards.getChildren().addAll(
            buildModeCard("Practice", "📝",
                "Free typing — no timer, no pressure.",
                "Great for warming up", "badge-practice"),
            buildModeCard("Normal", "⌨",
                "Type a 50-word passage as fast as you can.",
                "Score saved to leaderboard", "badge-normal"),
            buildModeCard("Timer", "⏱",
                "Race against the clock — pick your time limit.",
                "15s / 30s / 60s / 120s", "badge-timer")
        );

        content.getChildren().addAll(headerBox, buildStatsRow(), modeCards);
        scroll.setContent(content);
        contentArea.getChildren().add(scroll);
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
        if ("Timer".equals(mode)) showTimerConfig();
        else showDifficultyPicker(mode, 0);
    }

    private void showTimerConfig() {
        contentArea.getChildren().clear();

        VBox content = new VBox(24);
        content.setStyle("-fx-padding: 60 80 40 80;");
        content.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Timer Mode");
        title.getStyleClass().add("label-title");
        Label sub = new Label("How long do you want to run?");
        sub.getStyleClass().add("label-muted");

        int[] times = {15, 30, 60, 120};
        final int[] chosen = {60};
        ToggleGroup tg = new ToggleGroup();
        HBox timeRow = new HBox(14);

        for (int t : times) {
            ToggleButton tb = new ToggleButton(t + "s");
            tb.setToggleGroup(tg);
            tb.getStyleClass().add("diff-btn");
            tb.setPadding(new Insets(10, 26, 10, 26));
            if (t == 60) {
                tb.setSelected(true);
                tb.getStyleClass().setAll("diff-btn-selected");
            }
            int finalT = t;
            tb.selectedProperty().addListener((obs, was, is) -> {
                if (is) {
                    chosen[0] = finalT;
                    tb.getStyleClass().setAll("diff-btn-selected");
                } else {
                    tb.getStyleClass().setAll("diff-btn");
                }
            });
            timeRow.getChildren().add(tb);
        }

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
            tb.selectedProperty().addListener((obs, was, is) -> {
                if (is) {
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

        Label durLabel = new Label("Duration");
        durLabel.getStyleClass().add("label-section");

        content.getChildren().addAll(title, sub, gap(8), durLabel, timeRow, gap(8),
                diffLabel, diffRow, gap(16), start, back);
        contentArea.getChildren().add(content);
    }

    private void showDifficultyPicker(String mode, int timerSecs) {
        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setStyle("-fx-padding: 60 80 40 80;");
        content.setAlignment(Pos.TOP_LEFT);

        Label title = new Label(mode + " Mode — Difficulty");
        title.getStyleClass().add("label-title");
        Label sub = new Label("Choose your challenge level");
        sub.getStyleClass().add("label-muted");

        String[] diffs = {"Easy", "Medium", "Hard"};
        final String[] chosen = {"Easy"};
        ToggleGroup tg = new ToggleGroup();
        HBox row = new HBox(14);

        for (String d : diffs) {
            ToggleButton tb = new ToggleButton(d);
            tb.setToggleGroup(tg);
            tb.getStyleClass().add("diff-btn");
            if ("Easy".equals(d)) {
                tb.setSelected(true);
                tb.getStyleClass().setAll("diff-btn-selected");
            }
            tb.selectedProperty().addListener((obs, was, is) -> {
                if (is) {
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

    // ── DAILY CHALLENGES: Dedicated Page ─────────────────────────────────

    private void showDailyChallenges() {
        contentArea.getChildren().clear();

        ScrollPane scroll = new ScrollPane();
        scroll.getStyleClass().add("scroll-dark");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox content = new VBox(24);
        content.setStyle("-fx-padding: 36 40 36 40;");

        Label title = new Label("🔥  Daily Challenges");
        title.getStyleClass().add("label-title");
        Label sub = new Label("Complete daily and weekly tasks to earn XP and rewards");
        sub.getStyleClass().add("label-muted");

        String              username = SessionManager.getInstance().getUsername();
        DailyChallengeProgress dp;
        try {
            dp = GamificationService.getInstance().getDailyProgress(username);
        } catch (Exception e) {
            dp = new DailyChallengeProgress(0, 0.0, 0);
        }

        // Daily card
        VBox dailyCard = new VBox(16);
        dailyCard.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 16; -fx-padding: 24;");

        HBox cardHeader = new HBox(12);
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        Label dayIcon = new Label("📅");
        dayIcon.setStyle("-fx-font-size: 22px;");
        VBox cardTitleBox = new VBox(2);
        Label dayTitle = new Label("Today's Challenges");
        dayTitle.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label dayDate  = new Label(java.time.LocalDate.now().toString());
        dayDate.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        cardTitleBox.getChildren().addAll(dayTitle, dayDate);
        Region hsp = new Region();
        HBox.setHgrow(hsp, Priority.ALWAYS);
        Label rewardTag = new Label("🏆  +20 XP  +10 Coins");
        rewardTag.setStyle(
            "-fx-background-color: rgba(245,158,11,0.2);" +
            "-fx-text-fill: #fbbf24; -fx-background-radius: 8;" +
            "-fx-padding: 4 12 4 12; -fx-font-size: 12px; -fx-font-weight: bold;");
        cardHeader.getChildren().addAll(dayIcon, cardTitleBox, hsp, rewardTag);

        int completed = (dp.isGamesComplete() ? 1 : 0)
                      + (dp.isWpmComplete()   ? 1 : 0)
                      + (dp.isCharsComplete() ? 1 : 0);
        ProgressBar overallProg = new ProgressBar(completed / 3.0);
        overallProg.getStyleClass().add("progress-bar-custom");
        overallProg.setMaxWidth(Double.MAX_VALUE);
        overallProg.setPrefHeight(10);
        Label progLbl = new Label(completed + " / 3 challenges completed");
        progLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        VBox challengeRows = new VBox(14);
        challengeRows.getChildren().addAll(
            buildExpandedChallenge("🎮", "Play 2 Games",
                "Complete 2 full typing sessions today",
                dp.gamesPlayed(), 2,
                dp.gamesPlayed() + " / 2 games", dp.isGamesComplete()),
            buildSep(),
            buildExpandedChallenge("⚡", "Reach 40 WPM",
                "Hit 40+ WPM in any game session",
                dp.isWpmComplete() ? 100 : (int)(dp.maxWpm() / 40.0 * 100), 100,
                dp.isWpmComplete() ? "✓ Done" : String.format("%.0f / 40 WPM", dp.maxWpm()),
                dp.isWpmComplete()),
            buildSep(),
            buildExpandedChallenge("⌨", "Type 500 Characters",
                "Type at least 500 characters in total today",
                Math.min(dp.charsTyped(), 500), 500,
                dp.charsTyped() + " / 500 chars", dp.isCharsComplete())
        );

        dailyCard.getChildren().addAll(cardHeader, buildSep(), overallProg, progLbl, challengeRows);

        if (dp.isAllComplete()) {
            Label done = new Label("🎉  All daily challenges complete! Come back tomorrow for more.");
            done.setStyle(
                "-fx-text-fill: #10b981; -fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-background-color: rgba(16,185,129,0.1);" +
                "-fx-background-radius: 10; -fx-padding: 12;");
            done.setMaxWidth(Double.MAX_VALUE);
            dailyCard.getChildren().add(done);
        }

        // Weekly challenges card
        VBox weeklyCard = buildWeeklyChallengesCard();

        // Tips card
        VBox tipsCard = new VBox(12);
        tipsCard.setStyle(
            "-fx-background-color: rgba(124,58,237,0.08);" +
            "-fx-background-radius: 14; -fx-padding: 20;");
        Label tipsTitle = new Label("💡  Pro Tips");
        tipsTitle.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 14px; -fx-font-weight: bold;");
        tipsCard.getChildren().add(tipsTitle);
        for (String tip : new String[]{
                "Practice consistently — even 10 minutes daily improves speed.",
                "Focus on accuracy first; speed follows naturally.",
                "Use proper finger placement on home row keys.",
                "Daily streaks compound your XP gains significantly!"}) {
            Label tipLbl = new Label("• " + tip);
            tipLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
            tipsCard.getChildren().add(tipLbl);
        }

        content.getChildren().addAll(title, sub, dailyCard, weeklyCard, tipsCard);
        scroll.setContent(content);
        contentArea.getChildren().add(scroll);
    }

    private VBox buildExpandedChallenge(String icon, String name, String desc,
                                         int current, int total,
                                         String progressText, boolean done) {
        VBox row = new VBox(8);
        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(done ? "✅" : icon);
        iconLbl.setStyle("-fx-font-size: 20px;");

        VBox nameBox = new VBox(2);
        Label nameLbl = new Label(name);
        nameLbl.setStyle("-fx-text-fill: " + (done ? "#10b981" : "white") +
                         "; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        nameBox.getChildren().addAll(nameLbl, descLbl);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label prog = new Label(progressText);
        prog.setStyle("-fx-text-fill: " + (done ? "#10b981" : "#a78bfa") +
                      "; -fx-font-size: 12px; -fx-font-weight: bold;");
        top.getChildren().addAll(iconLbl, nameBox, sp, prog);

        ProgressBar bar = new ProgressBar(Math.min((double) current / total, 1.0));
        bar.getStyleClass().add(done ? "progress-bar-xp" : "progress-bar-custom");
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(6);

        row.getChildren().addAll(top, bar);
        return row;
    }

    private VBox buildWeeklyChallengesCard() {
        VBox card = new VBox(14);
        card.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 16; -fx-padding: 24;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("📆");
        icon.setStyle("-fx-font-size: 20px;");
        Label titleLbl = new Label("Weekly Challenges");
        titleLbl.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label reward = new Label("🏆  +100 XP  +50 Coins");
        reward.setStyle(
            "-fx-background-color: rgba(99,102,241,0.2);" +
            "-fx-text-fill: #818cf8; -fx-background-radius: 8;" +
            "-fx-padding: 4 12 4 12; -fx-font-size: 12px; -fx-font-weight: bold;");
        header.getChildren().addAll(icon, titleLbl, sp, reward);
        card.getChildren().addAll(header, buildSep());

        String[][] challenges = {
            {"🎮", "Play 10 Games This Week",       "10 gaming sessions in 7 days"},
            {"⚡", "Reach 60 WPM",                  "Hit 60+ WPM in any session"},
            {"📖", "Complete 3 Training Lessons",   "Finish 3 training modules"},
            {"🔥", "Maintain a 7-Day Streak",       "Log in and play 7 days in a row"}
        };
        for (String[] ch : challenges) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            Label icoLbl = new Label(ch[0]);
            icoLbl.setStyle("-fx-font-size: 16px;");
            VBox info = new VBox(2);
            Label nameLbl = new Label(ch[1]);
            nameLbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");
            Label descLbl = new Label(ch[2]);
            descLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
            info.getChildren().addAll(nameLbl, descLbl);
            row.getChildren().addAll(icoLbl, info);
            card.getChildren().add(row);
        }
        return card;
    }

    // ── Daily Challenges mini-card (for Home) ─────────────────────────────

    private VBox buildDailyChallengesCard() {
        String              username = SessionManager.getInstance().getUsername();
        DailyChallengeProgress dp;
        try {
            dp = GamificationService.getInstance().getDailyProgress(username);
        } catch (Exception e) {
            dp = new DailyChallengeProgress(0, 0.0, 0);
        }

        VBox card = new VBox(14);
        card.setStyle(
            "-fx-background-color: #1a1a2e;" +
            "-fx-background-radius: 16;" +
            "-fx-padding: 22 24 22 24;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 12, 0, 0, 4);");

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("🔥");
        icon.setStyle("-fx-font-size: 18px;");
        Label titleLbl = new Label("Daily Challenges");
        titleLbl.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label bonusTag = new Label("Bonus: +20 XP + 10 coins");
        bonusTag.setStyle(
            "-fx-background-color: rgba(245,158,11,0.2);" +
            "-fx-text-fill: #fbbf24;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 3 10 3 10;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;");
        titleRow.getChildren().addAll(icon, titleLbl, sp, bonusTag);

        HBox row1 = challengeRow("🎮  Play 2 games",
                dp.gamesPlayed() + " / 2", dp.isGamesComplete());
        HBox row2 = challengeRow("⚡  Reach 40 WPM",
                dp.isWpmComplete() ? "✓ Done" : String.format("%.0f", dp.maxWpm()) + " / 40 WPM",
                dp.isWpmComplete());
        HBox row3 = challengeRow("⌨  Type 500 characters",
                dp.charsTyped() + " / 500", dp.isCharsComplete());

        card.getChildren().addAll(titleRow, buildSep(), row1, row2, row3);

        if (dp.isAllComplete()) {
            Label done = new Label("✅  All challenges complete for today!");
            done.setStyle("-fx-text-fill: #10b981; -fx-font-size: 12px; -fx-font-weight: bold;");
            card.getChildren().add(done);
        }

        Button viewAll = new Button("View All Challenges →");
        viewAll.getStyleClass().add("btn-secondary");
        viewAll.setMaxWidth(Double.MAX_VALUE);
        viewAll.setOnAction(e -> { setActive(allNavBtns[6]); showDailyChallenges(); });
        card.getChildren().add(viewAll);

        return card;
    }

    private HBox challengeRow(String label, String progress, boolean done) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        Label checkIcon = new Label(done ? "✅" : "⬜");
        checkIcon.setStyle("-fx-font-size: 14px;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: " + (done ? "#10b981" : "#cbd5e1") + "; -fx-font-size: 13px;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label prog = new Label(progress);
        prog.setStyle("-fx-text-fill: " + (done ? "#10b981" : "#64748b") +
                      "; -fx-font-size: 12px; -fx-font-weight: bold;");
        row.getChildren().addAll(checkIcon, lbl, sp, prog);
        return row;
    }

    // ── REWARDS / INVENTORY ───────────────────────────────────────────────

    private void showRewards() {
        contentArea.getChildren().clear();

        ScrollPane scroll = new ScrollPane();
        scroll.getStyleClass().add("scroll-dark");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox content = new VBox(28);
        content.setStyle("-fx-padding: 36 40 36 40;");

        Label title = new Label("🎁  Rewards & Inventory");
        title.getStyleClass().add("label-title");
        Label sub = new Label("Your unlocked items, collectibles, and currency");
        sub.getStyleClass().add("label-muted");

        String         username = SessionManager.getInstance().getUsername();
        UserStats      stats    = null;
        UserInventory  inv      = null;
        try {
            stats = GamificationService.getInstance().getStats(username);
            inv   = StoreService.getInstance().getInventory(username);
        } catch (Exception ignored) {}

        int coins = stats != null ? stats.getCoins() : 0;
        int xp    = stats != null ? stats.getXp()    : 0;
        int lvl   = stats != null ? stats.getLevel() : 1;

        // Currency row
        HBox currencyRow = new HBox(16);
        currencyRow.getChildren().addAll(
            buildStatCard("💎  XP Points", String.valueOf(xp),    "#a78bfa", "Experience earned"),
            buildStatCard("💰  Coins",      String.valueOf(coins), "#fbbf24", "Spend in stores"),
            buildStatCard("🏅  Level",      String.valueOf(lvl),   "#10b981", levelTitle(lvl))
        );

        // Unlocked themes
        List<String> themes      = inv != null ? inv.getUnlockedThemes() : List.of("dark_violet");
        String       activeTheme = inv != null ? inv.getActiveTheme()    : "dark_violet";
        VBox themesSection = buildInventorySection(
            "🎨  Unlocked Themes", themes.size() + " theme(s) owned", themes, activeTheme, true);

        // Unlocked fonts
        List<String> fonts      = inv != null ? inv.getUnlockedFonts() : List.of("segoe");
        String       activeFont = inv != null ? inv.getActiveFont()    : "segoe";
        VBox fontsSection = buildInventorySection(
            "🔤  Unlocked Fonts", fonts.size() + " font(s) owned", fonts, activeFont, false);

        // Achievement rewards
        VBox achRewards = buildAchievementRewardsCard();

        content.getChildren().addAll(title, sub, currencyRow, themesSection, fontsSection, achRewards);
        scroll.setContent(content);
        contentArea.getChildren().add(scroll);
    }

    private VBox buildStatCard(String label, String value, String color, String sub) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 14; -fx-padding: 20;");
        card.setPrefWidth(200);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label val = new Label(value);
        val.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 28px; -fx-font-weight: bold;");
        Label subLbl = new Label(sub);
        subLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        card.getChildren().addAll(lbl, val, subLbl);
        return card;
    }

    private VBox buildInventorySection(String titleText, String subText,
                                        List<String> items, String active, boolean isTheme) {
        VBox card = new VBox(14);
        card.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 14; -fx-padding: 20;");
        Label titleLbl = new Label(titleText);
        titleLbl.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        Label subLbl = new Label(subText);
        subLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        card.getChildren().addAll(titleLbl, subLbl, buildSep());

        FlowPane itemFlow = new FlowPane(10, 10);
        for (String item : items) {
            HBox chip = new HBox(8);
            chip.setAlignment(Pos.CENTER_LEFT);
            boolean isActive = item.equals(active);
            chip.setStyle(isActive
                ? "-fx-background-color: rgba(124,58,237,0.3); -fx-background-radius: 20; -fx-padding: 6 14 6 14;"
                : "-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 20; -fx-padding: 6 14 6 14;");
            String display = item.replace("_", " ");
            display = Character.toUpperCase(display.charAt(0)) + display.substring(1);
            Label itemLbl = new Label((isTheme ? "🎨 " : "🔤 ") + display);
            itemLbl.setStyle("-fx-text-fill: " + (isActive ? "#a78bfa" : "#cbd5e1") + "; -fx-font-size: 12px;");
            chip.getChildren().add(itemLbl);
            if (isActive) {
                Label activeBadge = new Label("Active");
                activeBadge.setStyle(
                    "-fx-background-color: #7c3aed; -fx-text-fill: white;" +
                    "-fx-background-radius: 10; -fx-padding: 1 6 1 6; -fx-font-size: 9px;");
                chip.getChildren().add(activeBadge);
            }
            itemFlow.getChildren().add(chip);
        }
        card.getChildren().add(itemFlow);
        return card;
    }

    private VBox buildAchievementRewardsCard() {
        VBox card = new VBox(14);
        card.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 14; -fx-padding: 20;");
        Label titleLbl = new Label("🎖  Achievement Rewards");
        titleLbl.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        Label subLbl = new Label("XP bonuses earned from completing achievements");
        subLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        card.getChildren().addAll(titleLbl, subLbl, buildSep());

        String username = SessionManager.getInstance().getUsername();
        var earned = GamificationService.getInstance().getAchievements(username);
        if (earned.isEmpty()) {
            Label empty = new Label("Complete achievements to earn bonus XP rewards!");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
            card.getChildren().add(empty);
        } else {
            int totalXp = earned.size() * 25;
            Label totalLbl = new Label("Total from achievements: " + totalXp + " XP from " + earned.size() + " badges");
            totalLbl.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 13px; -fx-font-weight: bold;");
            card.getChildren().add(totalLbl);
            FlowPane badges = new FlowPane(8, 8);
            for (var a : earned) {
                Label badge = new Label(a.getBadge());
                badge.setStyle(
                    "-fx-background-color: rgba(124,58,237,0.2);" +
                    "-fx-background-radius: 20; -fx-padding: 6 12 6 12;" +
                    "-fx-text-fill: #a78bfa; -fx-font-size: 13px;");
                badges.getChildren().add(badge);
            }
            card.getChildren().add(badges);
        }
        return card;
    }

    // ── PROFILE ───────────────────────────────────────────────────────────

    private void showProfile() {
        contentArea.getChildren().clear();

        ScrollPane scroll = new ScrollPane();
        scroll.getStyleClass().add("scroll-dark");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox content = new VBox(24);
        content.setStyle("-fx-padding: 36 40 36 40;");

        Label title = new Label("👤  Profile");
        title.getStyleClass().add("label-title");

        String    username = SessionManager.getInstance().getUsername();
        User      user     = null;
        try { user = MongoDBManager.getInstance().getUserByUsername(username); } catch (Exception ignored) {}
        if (user == null) user = SessionManager.getInstance().getCurrentUser();
        UserStats stats    = null;
        try { stats = GamificationService.getInstance().getStats(username); } catch (Exception ignored) {}

        int    xp      = stats != null ? stats.getXp()     : 0;
        int    level   = stats != null ? stats.getLevel()   : 1;
        int    streak  = stats != null ? stats.getStreak()  : 0;
        int    coins   = stats != null ? stats.getCoins()   : 0;
        double prog    = stats != null ? GamificationService.getInstance().levelProgress(xp) : 0.0;
        int    toNext  = stats != null ? GamificationService.getInstance().xpToNextLevel(xp)  : 100;
        String subType = user != null  ? user.getSubscriptionType() : "FREE";

        // Profile header card
        HBox profileHeader = new HBox(24);
        profileHeader.setAlignment(Pos.CENTER_LEFT);
        profileHeader.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 16; -fx-padding: 28;");

        StackPane avatar = buildAvatarCircle(user != null ? user.getAvatarId() : null, username, 80);
        avatar.setStyle("-fx-cursor: hand;");
        Tooltip.install(avatar, new Tooltip("Click to change avatar"));
        avatar.setOnMouseClicked(e -> showAvatarPicker());

        VBox profileInfo = new VBox(6);
        Label usernameLbl = new Label(username);
        usernameLbl.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label emailLbl = new Label(user != null && user.getEmail() != null ? user.getEmail() : "—");
        emailLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
        Label levelBadge = new Label("Level " + level + "  •  " + levelTitle(level));
        levelBadge.setStyle(
            "-fx-background-color: rgba(124,58,237,0.25); -fx-text-fill: #a78bfa;" +
            "-fx-background-radius: 20; -fx-padding: 4 12 4 12; -fx-font-size: 12px;");
        Label subBadge = new Label("PREMIUM".equals(subType) ? "⭐ PREMIUM" : "FREE");
        subBadge.setStyle("PREMIUM".equals(subType)
            ? "-fx-background-color: rgba(245,158,11,0.2); -fx-text-fill: #fbbf24;" +
              "-fx-background-radius: 20; -fx-padding: 4 12 4 12; -fx-font-size: 11px;"
            : "-fx-background-color: rgba(100,116,139,0.2); -fx-text-fill: #94a3b8;" +
              "-fx-background-radius: 20; -fx-padding: 4 12 4 12; -fx-font-size: 11px;");
        profileInfo.getChildren().addAll(usernameLbl, emailLbl, levelBadge, subBadge);

        Region hsp = new Region();
        HBox.setHgrow(hsp, Priority.ALWAYS);

        VBox xpBlock = new VBox(6);
        xpBlock.setAlignment(Pos.CENTER_RIGHT);
        xpBlock.setMinWidth(200);
        Label xpLbl2 = new Label(xp + " XP");
        xpLbl2.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 20px; -fx-font-weight: bold;");
        ProgressBar xpBar2 = new ProgressBar(Math.min(prog, 1.0));
        xpBar2.getStyleClass().add("progress-bar-xp");
        xpBar2.setMaxWidth(Double.MAX_VALUE);
        xpBar2.setPrefHeight(8);
        Label nextLvlLbl = new Label("Level " + (level + 1) + " in " + toNext + " XP");
        nextLvlLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        xpBlock.getChildren().addAll(xpLbl2, xpBar2, nextLvlLbl);

        profileHeader.getChildren().addAll(avatar, profileInfo, hsp, xpBlock);

        // Stats row
        double bestWpm = user != null ? user.getBestWpm()      : 0;
        double avgWpm  = user != null ? user.getAverageWpm()   : 0;
        double bestAcc = user != null ? user.getBestAccuracy() : 0;
        int    games   = user != null ? user.getTotalGames()   : 0;

        HBox statsCards = new HBox(12);
        statsCards.getChildren().addAll(
            buildStatCard("🏆  Best WPM",  String.format("%.0f",   bestWpm), "#a78bfa", "Personal record"),
            buildStatCard("⚡  Avg WPM",   String.format("%.0f",   avgWpm),  "#38bdf8", "Session average"),
            buildStatCard("🎯  Best Acc",  String.format("%.0f%%", bestAcc), "#10b981", "Top accuracy"),
            buildStatCard("🎮  Games",     String.valueOf(games),             "#fbbf24", "Total played"),
            buildStatCard("🔥  Streak",    streak + "d",                      "#f97316", "Current streak"),
            buildStatCard("💰  Coins",     String.valueOf(coins),             "#e879f9", "Available coins")
        );

        // Account details card (read-only view)
        VBox detailsCard = new VBox(14);
        detailsCard.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 14; -fx-padding: 20;");
        HBox detailsHeader = new HBox(12);
        detailsHeader.setAlignment(Pos.CENTER_LEFT);
        Label detailsTitle = new Label("Account Information");
        detailsTitle.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        Region detailsSpacer = new Region();
        HBox.setHgrow(detailsSpacer, Priority.ALWAYS);
        Button editBtn = new Button("✏  Edit Profile");
        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setOnAction(e -> showProfileEdit());
        detailsHeader.getChildren().addAll(detailsTitle, detailsSpacer, editBtn);
        detailsCard.getChildren().addAll(detailsHeader, buildSep());

        String[][] fields = {
            {"👤  Username",      username},
            {"📧  Email",         user != null && user.getEmail()   != null ? user.getEmail()   : "—"},
            {"📱  Phone",         user != null && user.getPhone()   != null ? user.getPhone()   : "—"},
            {"🎂  Date of Birth", user != null && user.getDob()     != null ? user.getDob()     : "—"},
            {"📍  Address",       user != null && user.getAddress() != null ? user.getAddress() : "—"},
            {"🔑  Subscription",  subType},
            {"📅  Member Since",  user != null && user.getCreatedAt() != null
                    ? user.getCreatedAt().toLocalDate().toString() : "—"}
        };
        for (String[] f : fields) {
            HBox row = new HBox(16);
            row.setAlignment(Pos.CENTER_LEFT);
            Label keyLbl = new Label(f[0]);
            keyLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
            keyLbl.setMinWidth(160);
            Label valLbl = new Label(f[1]);
            valLbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
            row.getChildren().addAll(keyLbl, valLbl);
            detailsCard.getChildren().add(row);
        }

        HBox actions = new HBox(14);
        Button upgradeBtn = new Button("💳  Upgrade to Premium");
        upgradeBtn.getStyleClass().add("btn-primary");
        upgradeBtn.setOnAction(e -> MainUI.showSubscription());
        Button signOutBtn = new Button("🚪  Sign Out");
        signOutBtn.getStyleClass().add("btn-danger");
        signOutBtn.setOnAction(e -> {
            SessionManager.getInstance().logout();
            MainUI.showLogin();
        });
        actions.getChildren().addAll(upgradeBtn, signOutBtn);

        content.getChildren().addAll(title, profileHeader, statsCards, detailsCard, actions);
        scroll.setContent(content);
        contentArea.getChildren().add(scroll);
    }

    private void showProfileEdit() {
        contentArea.getChildren().clear();

        ScrollPane scroll = new ScrollPane();
        scroll.getStyleClass().add("scroll-dark");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox content = new VBox(24);
        content.setStyle("-fx-padding: 36 40 36 40;");

        Label title = new Label("✏  Edit Profile");
        title.getStyleClass().add("label-title");
        Label sub = new Label("Update your account information below");
        sub.getStyleClass().add("label-muted");

        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) { showProfile(); return; }
        final String originalUsername = user.getUsername();

        VBox formCard = new VBox(16);
        formCard.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 14; -fx-padding: 24;");
        Label formTitle = new Label("Personal Information");
        formTitle.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        formCard.getChildren().addAll(formTitle, buildSep());

        // Build editable fields
        TextField fUsername = editField(user.getUsername());
        TextField fEmail    = editField(user.getEmail() != null    ? user.getEmail()   : "");
        TextField fPhone    = editField(user.getPhone() != null    ? user.getPhone()   : "");
        TextField fAddress  = editField(user.getAddress() != null  ? user.getAddress() : "");
        TextField fAge      = editField(user.getAge() > 0          ? String.valueOf(user.getAge()) : "");
        TextField fDob      = editField(user.getDob() != null      ? user.getDob()     : "");

        // Phone: digits only, max 10
        fPhone.textProperty().addListener((obs, old, nv) -> {
            if (!nv.matches("\\d*")) fPhone.setText(nv.replaceAll("[^0-9]", ""));
            else if (nv.length() > 10) fPhone.setText(nv.substring(0, 10));
        });
        // Age: digits only
        fAge.textProperty().addListener((obs, old, nv) -> {
            if (!nv.matches("\\d*")) fAge.setText(nv.replaceAll("[^0-9]", ""));
        });

        // Error label
        Label errorLbl = new Label("");
        errorLbl.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
        errorLbl.setWrapText(true);

        Consumer<String> showError = msg -> {
            errorLbl.setText("⚠  " + msg);
            errorLbl.setVisible(true);
        };

        formCard.getChildren().addAll(
            editRow("👤  Username",     fUsername),
            editRow("📧  Email",        fEmail),
            editRow("📱  Phone",        fPhone),
            editRow("📍  Address",      fAddress),
            editRow("🎂  Age",          fAge),
            editRow("📅  Date of Birth (YYYY-MM-DD)", fDob),
            errorLbl
        );

        // Save / Cancel buttons
        HBox btnRow = new HBox(14);
        Button saveBtn   = new Button("💾  Save Changes");
        saveBtn.getStyleClass().add("btn-success");
        Button cancelBtn = new Button("✕  Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> { setActive(allNavBtns[9]); showProfile(); });

        saveBtn.setOnAction(e -> {
            String newUsername = fUsername.getText().trim();
            String newEmail    = fEmail.getText().trim();
            String newPhone    = fPhone.getText().trim();
            String newAddress  = fAddress.getText().trim();
            String newAgeStr   = fAge.getText().trim();
            String newDob      = fDob.getText().trim();

            // Basic validation
            if (newUsername.isEmpty() || newEmail.isEmpty() || newPhone.isEmpty()
                    || newAddress.isEmpty() || newAgeStr.isEmpty()) {
                showError.accept("All fields are required.");
                return;
            }
            if (newUsername.length() < 3) {
                showError.accept("Username must be at least 3 characters.");
                return;
            }
            if (!newEmail.contains("@")) {
                showError.accept("Please enter a valid email address.");
                return;
            }
            if (!newPhone.matches("\\d{10}")) {
                showError.accept("Phone number must be exactly 10 digits.");
                return;
            }
            int newAge;
            try {
                newAge = Integer.parseInt(newAgeStr);
                if (newAge < 5 || newAge > 100) { showError.accept("Age must be between 5 and 100."); return; }
            } catch (NumberFormatException ex) {
                showError.accept("Enter a valid age."); return;
            }

            // Uniqueness checks — exclude the current user
            if (!newUsername.equals(originalUsername)
                    && MongoDBManager.getInstance().isUsernameTakenByOther(newUsername, originalUsername)) {
                showError.accept("Username \"" + newUsername + "\" is already taken. Choose another.");
                return;
            }
            if (!newEmail.equals(user.getEmail())
                    && MongoDBManager.getInstance().isEmailTakenByOther(newEmail, originalUsername)) {
                showError.accept("Email address is already registered by another account.");
                return;
            }

            // Persist
            user.setUsername(newUsername);
            user.setEmail(newEmail);
            user.setPhone(newPhone);
            user.setAddress(newAddress);
            user.setAge(newAge);
            user.setDob(newDob.isEmpty() ? user.getDob() : newDob);

            MongoDBManager.getInstance().updateUserProfile(originalUsername, user);
            SessionManager.getInstance().login(user);   // refresh session with updated user

            setActive(allNavBtns[9]);
            showProfile();
        });

        btnRow.getChildren().addAll(saveBtn, cancelBtn);

        content.getChildren().addAll(title, sub, formCard, btnRow);
        scroll.setContent(content);
        contentArea.getChildren().add(scroll);
    }

    private TextField editField(String value) {
        TextField f = new TextField(value);
        f.getStyleClass().add("field-dark");
        f.setMaxWidth(Double.MAX_VALUE);
        return f;
    }

    private HBox editRow(String label, TextField field) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        lbl.setMinWidth(220);
        HBox.setHgrow(field, Priority.ALWAYS);
        row.getChildren().addAll(lbl, field);
        return row;
    }

    // ── Store (Themes + Fonts combined) ──────────────────────────────────

    private void showStore() {
        contentArea.getChildren().clear();

        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        String bg = isLight ? "#f5f5f5" : "#0f0f1a";

        String activeTabStyle =
            "-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-weight: bold;" +
            "-fx-background-radius: 10; -fx-font-size: 13px; -fx-padding: 9 24 9 24; -fx-cursor: hand;";
        String inactiveTabStyle =
            "-fx-background-color: " + (isLight ? "#e5e7eb" : "#1a1a2e") +
            "; -fx-text-fill: " + (isLight ? "#374151" : "#64748b") + "; -fx-font-weight: bold;" +
            "-fx-background-radius: 10; -fx-font-size: 13px; -fx-padding: 9 24 9 24; -fx-cursor: hand;";

        ToggleButton themeTab = new ToggleButton("🎨  Themes");
        ToggleButton fontTab  = new ToggleButton("🔤  Fonts");
        ToggleGroup tg = new ToggleGroup();
        themeTab.setToggleGroup(tg);
        fontTab.setToggleGroup(tg);
        themeTab.setSelected(true);
        themeTab.setStyle(activeTabStyle);
        fontTab.setStyle(inactiveTabStyle);

        HBox tabBar = new HBox(6, themeTab, fontTab);
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.setStyle("-fx-padding: 8 40 8 40; -fx-background-color: " + bg + ";" +
                "-fx-border-color: " + (isLight ? "#e5e7eb" : "#1e293b") +
                "; -fx-border-width: 0 0 1 0;");

        Label titleLbl = new Label("🛒  Store");
        titleLbl.setStyle("-fx-text-fill: " + (isLight ? "#111827" : "white") +
                "; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label subLbl = new Label("Unlock themes and fonts to personalize your experience");
        subLbl.setStyle("-fx-text-fill: " + (isLight ? "#374151" : "#64748b") + "; -fx-font-size: 13px;");
        VBox titleBlock = new VBox(4, titleLbl, subLbl);
        titleBlock.setStyle("-fx-padding: 24 40 12 40; -fx-background-color: " + bg + ";");

        VBox headerBlock = new VBox(0, titleBlock, tabBar);

        Runnable refresh = () -> MainUI.showDashboardAt("store");
        Node themePanel  = new ThemeStoreScreen().buildContent(refresh);
        Node fontPanel   = new FontStoreScreen().buildContent(refresh);

        BorderPane wrapper = new BorderPane();
        wrapper.setStyle("-fx-background-color: " + bg + ";");
        wrapper.setTop(headerBlock);
        wrapper.setCenter(themePanel);

        themeTab.selectedProperty().addListener((obs, was, is) -> {
            if (is) { themeTab.setStyle(activeTabStyle);   wrapper.setCenter(themePanel); }
            else    { themeTab.setStyle(inactiveTabStyle); }
        });
        fontTab.selectedProperty().addListener((obs, was, is) -> {
            if (is) { fontTab.setStyle(activeTabStyle);    wrapper.setCenter(fontPanel);  }
            else    { fontTab.setStyle(inactiveTabStyle); }
        });

        contentArea.getChildren().add(wrapper);
    }

    // ── Other delegated screens ───────────────────────────────────────────

    private void showLeaderboard() {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(new LeaderboardScreen().buildContent());
    }

    private void showStats() {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(new StatsScreen().buildContent());
    }

    private void showTraining() {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(new TrainingDashboardScreen().buildContent());
    }

    private void showAchievements() {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(new AchievementsScreen().buildContent());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private HBox buildStatsRow() {
        HBox row = new HBox(16);
        String uname = SessionManager.getInstance().getUsername();
        User user = null;
        try { user = MongoDBManager.getInstance().getUserByUsername(uname); } catch (Exception ignored) {}
        if (user == null) user = SessionManager.getInstance().getCurrentUser();
        row.getChildren().addAll(
            miniStat("Best WPM", String.format("%.0f",   user != null ? user.getBestWpm()      : 0), "stat-number"),
            miniStat("Avg WPM",  String.format("%.0f",   user != null ? user.getAverageWpm()   : 0), "stat-number-cyan"),
            miniStat("Best Acc", String.format("%.0f%%", user != null ? user.getBestAccuracy() : 0), "stat-number-green"),
            miniStat("Games",    String.valueOf(          user != null ? user.getTotalGames()   : 0), "stat-number-orange")
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

    private String levelTitle(int level) {
        if (level >= 20) return "Grandmaster";
        if (level >= 15) return "Elite";
        if (level >= 10) return "Expert";
        if (level >= 5)  return "Advanced";
        if (level >= 3)  return "Intermediate";
        return "Beginner";
    }

    private Button sidebarBtn(String text, boolean active) {
        Button b = new Button(text);
        b.getStyleClass().add(active ? "sidebar-btn-active" : "sidebar-btn");
        return b;
    }

    private void setActive(Button active) {
        for (Button b : allNavBtns) b.getStyleClass().setAll("sidebar-btn");
        active.getStyleClass().setAll("sidebar-btn-active");
    }

    private Region buildSep() {
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color: #1e293b;");
        return sep;
    }

    private Region gap(int h) {
        Region r = new Region();
        r.setMinHeight(h);
        return r;
    }

    // ── Avatar helpers ────────────────────────────────────────────────────

    /**
     * Returns a circular avatar node. Shows the user's chosen image if avatarId is set,
     * otherwise falls back to a purple circle with initials.
     */
    private StackPane buildAvatarCircle(String avatarId, String username, double size) {
        StackPane container = new StackPane();
        container.setMinSize(size, size);
        container.setMaxSize(size, size);

        if (avatarId != null && !avatarId.isBlank()) {
            try {
                var stream = getClass().getResourceAsStream("/avatars/" + avatarId);
                if (stream != null) {
                    Image img = new Image(stream, size, size, true, true);
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(size);
                    iv.setFitHeight(size);
                    Circle clip = new Circle(size / 2, size / 2, size / 2);
                    iv.setClip(clip);
                    container.getChildren().add(iv);
                    return container;
                }
            } catch (Exception ignored) {}
        }

        // Fallback: initials circle
        container.setStyle(
            "-fx-background-color: rgba(124,58,237,0.3);" +
            "-fx-background-radius: " + (size / 2) + ";");
        String initials = username.length() >= 2
                ? username.substring(0, 2).toUpperCase() : username.toUpperCase();
        Label lbl = new Label(initials);
        lbl.setStyle("-fx-text-fill: #a78bfa;" +
                     "-fx-font-size: " + (int)(size / 3.2) + "px;" +
                     "-fx-font-weight: bold;");
        container.getChildren().add(lbl);
        return container;
    }

    /** Shows a floating avatar-picker overlay on the content area. */
    private void showAvatarPicker() {
        String username = SessionManager.getInstance().getUsername();
        User pickerUser = null;
        try { pickerUser = MongoDBManager.getInstance().getUserByUsername(username); } catch (Exception ignored) {}
        final String current = pickerUser != null ? pickerUser.getAvatarId() : null;

        // Semi-transparent backdrop
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.65);");

        // Card
        VBox card = new VBox(18);
        card.setStyle(
            "-fx-background-color: #1a1a2e;" +
            "-fx-background-radius: 20;" +
            "-fx-padding: 28 32 28 32;");
        card.setMaxWidth(540);
        card.setAlignment(Pos.CENTER);

        // Title row
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Choose Your Avatar");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 17px; -fx-font-weight: bold;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #64748b;" +
            "-fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 0;");
        closeBtn.setOnAction(e -> contentArea.getChildren().remove(overlay));
        titleRow.getChildren().addAll(title, sp, closeBtn);

        Label sub = new Label("Click an avatar to apply it instantly");
        sub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        // Avatar grid
        FlowPane grid = new FlowPane(10, 10);
        grid.setAlignment(Pos.CENTER);
        grid.setPrefWrapLength(500);

        String[][] avatars = {
            {"avatar1.png"},{"avatar2.png"},{"avatar3.jpg"},{"avatar4.jpg"},{"avatar5.jpg"},
            {"avatar6.jpeg"},{"avatar7.jpeg"},{"avatar8.jpeg"},{"avatar9.jpeg"},{"avatar10.jpeg"},
            {"avatar11.jpeg"},{"avatar12.jpeg"},{"avatar13.jpeg"},{"avatar14.jpeg"},{"avatar15.jpg"},
            {"avatar16.jpg"},{"avatar17.jpg"},{"avatar18.jpg"},{"avatar19.png"},{"avatar20.png"}
        };

        for (String[] av : avatars) {
            String file = av[0];
            StackPane cell = new StackPane();
            cell.setMinSize(70, 70);
            cell.setMaxSize(70, 70);

            boolean selected = file.equals(current);
            String borderColor = selected ? "#a78bfa" : "transparent";
            cell.setStyle(
                "-fx-background-radius: 35;" +
                "-fx-border-radius: 35;" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-width: 3;" +
                "-fx-cursor: hand;");

            try {
                var stream = getClass().getResourceAsStream("/avatars/" + file);
                if (stream != null) {
                    Image img = new Image(stream, 64, 64, true, true);
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(64);
                    iv.setFitHeight(64);
                    Circle clip = new Circle(32, 32, 32);
                    iv.setClip(clip);
                    cell.getChildren().add(iv);
                }
            } catch (Exception ignored) {}

            String baseStyle =
                "-fx-background-radius: 35;" +
                "-fx-border-radius: 35;" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-width: 3;" +
                "-fx-cursor: hand;";
            cell.setOnMouseEntered(e -> cell.setStyle(baseStyle +
                "-fx-effect: dropshadow(gaussian, #a78bfa, 12, 0.4, 0, 0);"));
            cell.setOnMouseExited(e  -> cell.setStyle(baseStyle));
            cell.setOnMouseClicked(e -> {
                MongoDBManager.getInstance().updateUserAvatar(username, file);
                contentArea.getChildren().remove(overlay);
                MainUI.showDashboardAt("profile");
            });
            grid.getChildren().add(cell);
        }

        // Remove avatar option
        StackPane removeCell = new StackPane();
        removeCell.setMinSize(70, 70);
        removeCell.setMaxSize(70, 70);
        removeCell.setStyle(
            "-fx-background-color: #0f172a;" +
            "-fx-background-radius: 35;" +
            "-fx-border-radius: 35;" +
            "-fx-border-color: #334155;" +
            "-fx-border-width: 2;" +
            "-fx-cursor: hand;");
        Label removeLbl = new Label("✕\nReset");
        removeLbl.setStyle(
            "-fx-text-fill: #64748b; -fx-font-size: 10px;" +
            "-fx-text-alignment: center; -fx-alignment: center;");
        removeCell.getChildren().add(removeLbl);
        removeCell.setOnMouseClicked(e -> {
            MongoDBManager.getInstance().updateUserAvatar(username, null);
            contentArea.getChildren().remove(overlay);
            MainUI.showDashboardAt("profile");
        });
        grid.getChildren().add(removeCell);

        card.getChildren().addAll(titleRow, sub, grid);

        // Scroll in case grid is tall
        ScrollPane cardScroll = new ScrollPane(card);
        cardScroll.setFitToWidth(true);
        cardScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cardScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        cardScroll.setMaxWidth(560);
        cardScroll.setMaxHeight(480);

        overlay.getChildren().add(cardScroll);
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) contentArea.getChildren().remove(overlay);
        });

        contentArea.getChildren().add(overlay);
    }
}
