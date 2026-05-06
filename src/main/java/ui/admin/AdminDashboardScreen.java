package ui.admin;

import db.MongoDBManager;
import db.SubscriptionDAO;
import db.TrainingCertificateDAO;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.GameResult;
import model.Subscription;
import model.User;
import util.SessionManager;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Admin Dashboard — primary control panel with sidebar navigation and rich metrics.
 */
public class AdminDashboardScreen {

    private StackPane contentArea;

    // Keep sidebar button references so we can toggle active state
    private Button dashBtn, usersBtn, contentBtn, lessonsBtn,
                   gameBtn, leaderBtn, subsBtn, certBtn, analyticsBtn;

    // ── Scene ────────────────────────────────────────────────────────────────

    public Scene buildScene() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #080818;");

        root.setLeft(buildSidebar());
        root.setTop(buildHeader());

        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #080818;");
        root.setCenter(contentArea);

        showDashboard();

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        return scene;
    }

    // ── Sidebar ──────────────────────────────────────────────────────────────

    private VBox buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #0e0e24, #0a0a1c);" +
            "-fx-min-width: 255; -fx-max-width: 255;" +
            "-fx-border-color: transparent rgba(124,58,237,0.25) transparent transparent;" +
            "-fx-border-width: 0 1 0 0;");

        // ── Brand ────────────────────────────────────────────────────────────
        VBox brandBox = new VBox(5);
        brandBox.setStyle("-fx-padding: 24 18 18 18;");

        Label brand = new Label("⌨  TypeMaster");
        brand.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 19px; -fx-font-weight: bold;");

        Label adminSub = new Label("⚙  Admin Control Panel");
        adminSub.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px;");

        brandBox.getChildren().addAll(brand, adminSub);

        Region div1 = divider();

        // ── Navigation ────────────────────────────────────────────────────────
        VBox navBox = new VBox(3);
        navBox.setStyle("-fx-padding: 10 12 10 12;");
        VBox.setVgrow(navBox, Priority.ALWAYS);

        Label navLabel = new Label("M A I N  M E N U");
        navLabel.setStyle("-fx-text-fill: #334155; -fx-font-size: 10px; -fx-font-weight: bold;" +
                          "-fx-padding: 4 6 8 6;");

        dashBtn     = navBtn("📊", "Dashboard",     true);
        usersBtn    = navBtn("👥", "Users",          false);
        contentBtn  = navBtn("📝", "Content",        false);
        lessonsBtn  = navBtn("📖", "Lessons",        false);
        gameBtn     = navBtn("🎮", "Game Settings",  false);
        leaderBtn   = navBtn("🏆", "Leaderboard",   false);
        subsBtn     = navBtn("💳", "Subscriptions",  false);
        certBtn     = navBtn("📜", "Certificates",   false);
        analyticsBtn= navBtn("📈", "Analytics",     false);

        wireNavActions();
        navBox.getChildren().addAll(navLabel, dashBtn, usersBtn, contentBtn,
                lessonsBtn, gameBtn, leaderBtn, subsBtn, certBtn, analyticsBtn);

        // ── Bottom: user info + sign-out ────────────────────────────────────
        VBox bottomBox = new VBox(10);
        bottomBox.setStyle("-fx-padding: 12 12 22 12;");

        Region div2 = divider();

        HBox adminRow = new HBox(10);
        adminRow.setAlignment(Pos.CENTER_LEFT);
        adminRow.setStyle("-fx-padding: 8 6 8 6;");

        Label avatar = new Label("👤");
        avatar.setStyle("-fx-font-size: 20px;");

        VBox adminInfo = new VBox(2);
        Label adminName = new Label(SessionManager.getInstance().getUsername());
        adminName.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label adminRole = new Label("Administrator");
        adminRole.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px;");
        adminInfo.getChildren().addAll(adminName, adminRole);
        adminRow.getChildren().addAll(avatar, adminInfo);

        Button sidebarSignOut = signOutButton(true);
        bottomBox.getChildren().addAll(div2, adminRow, sidebarSignOut);

        sidebar.getChildren().addAll(brandBox, div1, navBox, bottomBox);
        return sidebar;
    }

    private void wireNavActions() {
        dashBtn.setOnAction(e -> {
            activateNav(dashBtn);
            showDashboard();
        });
        usersBtn.setOnAction(e -> {
            activateNav(usersBtn);
            contentArea.getChildren().setAll(new AdminUsersPanel().buildContent());
        });
        contentBtn.setOnAction(e -> {
            activateNav(contentBtn);
            contentArea.getChildren().setAll(new AdminContentPanel());
        });
        lessonsBtn.setOnAction(e -> {
            activateNav(lessonsBtn);
            contentArea.getChildren().setAll(new AdminLessonsPanel().buildContent());
        });
        gameBtn.setOnAction(e -> {
            activateNav(gameBtn);
            contentArea.getChildren().setAll(new AdminGameSettingsPanel().buildContent());
        });
        leaderBtn.setOnAction(e -> {
            activateNav(leaderBtn);
            contentArea.getChildren().setAll(new AdminLeaderboardPanel().buildContent());
        });
        subsBtn.setOnAction(e -> {
            activateNav(subsBtn);
            contentArea.getChildren().setAll(new AdminSubscriptionsPanel().buildContent());
        });
        certBtn.setOnAction(e -> {
            activateNav(certBtn);
            contentArea.getChildren().setAll(new AdminCertificatesPanel().buildContent());
        });
        analyticsBtn.setOnAction(e -> {
            activateNav(analyticsBtn);
            contentArea.getChildren().setAll(new AdminAnalyticsPanel().buildContent());
        });
    }

    // ── Header ───────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
            "-fx-background-color: #0b0b1e;" +
            "-fx-padding: 14 26 14 26;" +
            "-fx-border-color: transparent transparent rgba(124,58,237,0.2) transparent;" +
            "-fx-border-width: 0 0 1 0;");

        // Breadcrumb
        Label breadcrumb = new Label("Admin Control Panel");
        breadcrumb.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Notification badge
        long pendingCount = SubscriptionDAO.getInstance().getAllSubscriptions().stream()
                .filter(s -> "PENDING".equals(s.getStatus())).count();
        pendingCount += TrainingCertificateDAO.getInstance().countPending();
        if (pendingCount > 0) {
            StackPane notif = new StackPane();
            Label bell = new Label("🔔");
            bell.setStyle("-fx-font-size: 17px;");
            Label cnt = new Label(String.valueOf(pendingCount));
            cnt.setStyle(
                "-fx-background-color: #ef4444; -fx-text-fill: white;" +
                "-fx-font-size: 9px; -fx-font-weight: bold;" +
                "-fx-background-radius: 8; -fx-padding: 1 5 1 5;");
            StackPane.setAlignment(cnt, Pos.TOP_RIGHT);
            notif.getChildren().addAll(bell, cnt);
            header.getChildren().add(notif);
        }

        // Admin pill
        HBox adminPill = new HBox(8);
        adminPill.setAlignment(Pos.CENTER);
        adminPill.setStyle(
            "-fx-background-color: rgba(124,58,237,0.15);" +
            "-fx-background-radius: 20; -fx-padding: 6 14 6 10;" +
            "-fx-border-color: rgba(124,58,237,0.35); -fx-border-radius: 20; -fx-border-width: 1;");
        Label aIcon = new Label("👤");
        aIcon.setStyle("-fx-font-size: 13px;");
        Label aName = new Label(SessionManager.getInstance().getUsername());
        aName.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 13px; -fx-font-weight: bold;");
        adminPill.getChildren().addAll(aIcon, aName);

        // Header sign-out
        Button headerSignOut = signOutButton(false);

        header.getChildren().addAll(breadcrumb, spacer, adminPill, headerSignOut);
        return header;
    }

    // ── Dashboard content ────────────────────────────────────────────────────

    private void showDashboard() {
        ScrollPane scroll = new ScrollPane();
        scroll.getStyleClass().add("scroll-dark");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox content = new VBox(26);
        content.setStyle("-fx-padding: 34 38 34 38; -fx-background-color: #080818;");

        // Page title
        VBox titleRow = new VBox(4);
        Label pageTitle = new Label("Dashboard Overview");
        pageTitle.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label pageSub = new Label("Welcome back! Here's a real-time snapshot of TypeMaster.");
        pageSub.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");
        titleRow.getChildren().addAll(pageTitle, pageSub);

        // Row 1: primary metrics
        HBox row1 = buildMetricsRow1();
        // Row 2: secondary metrics
        HBox row2 = buildMetricsRow2();

        // Middle: pending verifications + recent activity side by side
        HBox middleRow = new HBox(20);
        VBox pendingCard = buildPendingVerifications();
        VBox activityCard = buildRecentActivity();
        HBox.setHgrow(pendingCard, Priority.ALWAYS);
        HBox.setHgrow(activityCard, Priority.ALWAYS);
        middleRow.getChildren().addAll(pendingCard, activityCard);

        // Bottom: top performers side by side
        HBox bottomRow = new HBox(20);
        VBox topWpm    = buildTopCard("⚡ Top WPM Players",    buildTopWpmRows());
        VBox topActive = buildTopCard("🔥 Most Active Players", buildTopActiveRows());
        HBox.setHgrow(topWpm, Priority.ALWAYS);
        HBox.setHgrow(topActive, Priority.ALWAYS);
        bottomRow.getChildren().addAll(topWpm, topActive);

        content.getChildren().addAll(titleRow, row1, row2, middleRow, bottomRow);
        scroll.setContent(content);
        contentArea.getChildren().setAll(scroll);
    }

    // ── Metric rows ───────────────────────────────────────────────────────────

    private HBox buildMetricsRow1() {
        List<User> allUsers = MongoDBManager.getInstance().getAllUsers();
        int total  = allUsers.size();
        int active = (int) allUsers.stream().filter(u -> u.getTotalGames() > 0).count();
        double avg = allUsers.stream().mapToDouble(User::getAverageWpm).average().orElse(0.0);
        long sessions = MongoDBManager.getInstance().getTotalSessions();

        HBox row = new HBox(16);
        row.getChildren().addAll(
            metricCard("Total Users",    String.valueOf(total),             "👥", "#7c3aed", "#a78bfa"),
            metricCard("Active Users",   String.valueOf(active),            "✅", "#059669", "#34d399"),
            metricCard("Avg WPM",        String.format("%.1f", avg),        "⚡", "#0284c7", "#38bdf8"),
            metricCard("Total Sessions", String.valueOf(sessions),          "🎮", "#b45309", "#fbbf24")
        );
        row.getChildren().forEach(n -> HBox.setHgrow((javafx.scene.Node) n, Priority.ALWAYS));
        return row;
    }

    private HBox buildMetricsRow2() {
        List<Subscription> subs = SubscriptionDAO.getInstance().getAllSubscriptions();
        long premium = subs.stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()) && !"FREE".equals(s.getPlan()))
                .map(Subscription::getUsername).distinct().count();
        long pending = subs.stream().filter(s -> "PENDING".equals(s.getStatus())).count();
        double revenue = subs.stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()) && !"FREE".equals(s.getPlan()))
                .mapToDouble(s -> "LIFETIME".equals(s.getPlan()) ? 1999.0 : 199.0).sum();
        String mode = MongoDBManager.getInstance().getMostPopularGameMode();

        HBox row = new HBox(16);
        row.getChildren().addAll(
            metricCard("Premium Users",  String.valueOf(premium),           "💎", "#92400e", "#fbbf24"),
            metricCard("Pending Verify", String.valueOf(pending),           "🔔", "#be185d", "#f472b6"),
            metricCard("Revenue",        String.format("₹%.0f", revenue),  "💰", "#0e7490", "#2dd4bf"),
            metricCard("Popular Mode",   mode,                              "🏆", "#5b21b6", "#c084fc")
        );
        row.getChildren().forEach(n -> HBox.setHgrow((javafx.scene.Node) n, Priority.ALWAYS));
        return row;
    }

    /** Metric card with a vertical accent bar and gradient border. */
    private VBox metricCard(String label, String value, String icon,
                             String dark, String light) {
        VBox card = new VBox(8);
        card.setStyle(
            "-fx-background-color: #111128;" +
            "-fx-background-radius: 16;" +
            "-fx-padding: 18 20 18 18;" +
            "-fx-border-color: " + dark + "55;" +
            "-fx-border-radius: 16; -fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, " + dark + "44, 14, 0, 0, 4);");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMinWidth(155);

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Vertical accent bar
        Region bar = new Region();
        bar.setPrefWidth(3);
        bar.setPrefHeight(32);
        bar.setStyle(
            "-fx-background-color: linear-gradient(to bottom, " + light + ", " + dark + ");" +
            "-fx-background-radius: 2;");

        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size: 22px;");
        topRow.getChildren().addAll(bar, ico);

        Label val = new Label(value);
        val.setStyle("-fx-text-fill: " + light + "; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px;");

        card.getChildren().addAll(topRow, val, lbl);
        return card;
    }

    // ── Pending Verifications ────────────────────────────────────────────────

    private VBox buildPendingVerifications() {
        VBox box = new VBox(12);
        box.setStyle(
            "-fx-background-color: #111128;" +
            "-fx-background-radius: 16; -fx-padding: 20 22 20 22;" +
            "-fx-border-color: rgba(251,191,36,0.25);" +
            "-fx-border-radius: 16; -fx-border-width: 1;");

        Label title = new Label("🔔  Pending Verifications");
        title.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 15px; -fx-font-weight: bold;");
        box.getChildren().add(title);

        List<Subscription> pending = SubscriptionDAO.getInstance().getAllSubscriptions()
                .stream().filter(s -> "PENDING".equals(s.getStatus())).toList();
        long pendingCerts = TrainingCertificateDAO.getInstance().countPending();

        if (pending.isEmpty() && pendingCerts == 0) {
            Label ok = new Label("✅  No pending verifications — all clear!");
            ok.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");
            box.getChildren().add(ok);
        } else {
            if (pendingCerts > 0) {
                Label certRow = new Label("📜  " + pendingCerts + " training certificate request" +
                        (pendingCerts == 1 ? "" : "s") + " waiting for grade.");
                certRow.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 13px; -fx-font-weight: bold;");
                box.getChildren().add(certRow);
            }
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM, HH:mm");
            for (Subscription s : pending) {
                String price = "MONTHLY".equals(s.getPlan()) ? "₹199/mo" : "₹1,999/yr";
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle(
                    "-fx-background-color: rgba(251,191,36,0.07);" +
                    "-fx-background-radius: 10; -fx-padding: 10 12 10 12;");

                VBox info = new VBox(3);
                Label name = new Label("👤 " + s.getUsername() + "  •  " + s.getPlan());
                name.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
                Label detail = new Label(price + "  •  " +
                        (s.getStartDate() != null ? s.getStartDate().format(fmt) : "—"));
                detail.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px;");
                info.getChildren().addAll(name, detail);
                HBox.setHgrow(info, Priority.ALWAYS);

                Button verifyBtn = tinyBtn("✅ Verify", "#10b981", "rgba(16,185,129,0.15)");
                verifyBtn.setOnAction(e -> {
                    SubscriptionDAO.getInstance().updateStatus(s.getId(), "ACTIVE");
                    showDashboard();
                });
                Button rejectBtn = tinyBtn("✕ Reject", "#ef4444", "rgba(239,68,68,0.15)");
                rejectBtn.setOnAction(e -> {
                    SubscriptionDAO.getInstance().updateStatus(s.getId(), "CANCELLED");
                    showDashboard();
                });
                row.getChildren().addAll(info, verifyBtn, rejectBtn);
                box.getChildren().add(row);
            }
        }
        return box;
    }

    // ── Recent Activity ──────────────────────────────────────────────────────

    private VBox buildRecentActivity() {
        VBox box = new VBox(10);
        box.setStyle(
            "-fx-background-color: #111128;" +
            "-fx-background-radius: 16; -fx-padding: 20 22 20 22;" +
            "-fx-border-color: rgba(124,58,237,0.2);" +
            "-fx-border-radius: 16; -fx-border-width: 1;");

        Label title = new Label("🕒  Recent Activity");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
        box.getChildren().add(title);

        List<GameResult> recent = MongoDBManager.getInstance().getRecentSessions(10);
        if (recent.isEmpty()) {
            Label none = new Label("No activity yet.");
            none.setStyle("-fx-text-fill: #475569;");
            box.getChildren().add(none);
        } else {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, HH:mm");
            for (GameResult r : recent) {
                String time = r.getPlayedAt() != null ? r.getPlayedAt().format(fmt) : "";
                String dotColor = switch (r.getGameMode() != null ? r.getGameMode() : "") {
                    case "Practice" -> "#38bdf8";
                    case "Timer"    -> "#fbbf24";
                    default         -> "#a78bfa";
                };
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 5 0 5 0;" +
                    "-fx-border-color: transparent transparent rgba(255,255,255,0.05) transparent;" +
                    "-fx-border-width: 0 0 1 0;");

                Label dot = new Label("●");
                dot.setStyle("-fx-text-fill: " + dotColor + "; -fx-font-size: 9px;");

                Label info = new Label(String.format("%-12s  %s  —  %.0f WPM  %.0f%%",
                        r.getUsername(), r.getGameMode(), r.getWpm(), r.getAccuracy()));
                info.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
                HBox.setHgrow(info, Priority.ALWAYS);

                Label tl = new Label(time);
                tl.setStyle("-fx-text-fill: #334155; -fx-font-size: 11px;");

                row.getChildren().addAll(dot, info, tl);
                box.getChildren().add(row);
            }
        }
        return box;
    }

    // ── Top Performers ────────────────────────────────────────────────────────

    private VBox buildTopCard(String heading, VBox rows) {
        VBox card = new VBox(10);
        card.setStyle(
            "-fx-background-color: #111128;" +
            "-fx-background-radius: 16; -fx-padding: 20 22 20 22;" +
            "-fx-border-color: rgba(124,58,237,0.18);" +
            "-fx-border-radius: 16; -fx-border-width: 1;");
        Label title = new Label(heading);
        title.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
        card.getChildren().addAll(title, rows);
        return card;
    }

    private VBox buildTopWpmRows() {
        VBox box = new VBox(6);
        List<GameResult> top = MongoDBManager.getInstance().getLeaderboard(5);
        String[] medals = {"🥇", "🥈", "🥉", "#4", "#5"};
        if (top.isEmpty()) {
            box.getChildren().add(noDataLabel());
            return box;
        }
        for (int i = 0; i < Math.min(5, top.size()); i++) {
            GameResult r = top.get(i);
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 7 10 7 10; -fx-background-radius: 8;" +
                (i == 0 ? "-fx-background-color: rgba(251,191,36,0.07);" : ""));

            Label medal = new Label(i < 3 ? medals[i] : medals[i]);
            medal.setStyle("-fx-font-size: " + (i < 3 ? "18" : "13") + "px; -fx-min-width: 28;");
            Label name = new Label(r.getUsername());
            name.setStyle("-fx-text-fill: " + (i == 0 ? "#fbbf24" : "#cbd5e1") +
                "; -fx-font-size: 13px; -fx-font-weight: " + (i == 0 ? "bold" : "normal") + ";");
            HBox.setHgrow(name, Priority.ALWAYS);
            Label wpm = new Label(String.format("%.1f WPM", r.getWpm()));
            wpm.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 13px; -fx-font-weight: bold;");
            row.getChildren().addAll(medal, name, wpm);
            box.getChildren().add(row);
        }
        return box;
    }

    private VBox buildTopActiveRows() {
        VBox box = new VBox(6);
        List<User> users = MongoDBManager.getInstance().getAllUsers();
        users.sort((a, b) -> Integer.compare(b.getTotalGames(), a.getTotalGames()));
        String[] medals = {"🥇", "🥈", "🥉", "#4", "#5"};
        if (users.isEmpty()) {
            box.getChildren().add(noDataLabel());
            return box;
        }
        for (int i = 0; i < Math.min(5, users.size()); i++) {
            User u = users.get(i);
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 7 10 7 10; -fx-background-radius: 8;" +
                (i == 0 ? "-fx-background-color: rgba(52,211,153,0.07);" : ""));

            Label medal = new Label(i < 3 ? medals[i] : medals[i]);
            medal.setStyle("-fx-font-size: " + (i < 3 ? "18" : "13") + "px; -fx-min-width: 28;");
            Label name = new Label(u.getUsername());
            name.setStyle("-fx-text-fill: " + (i == 0 ? "#fbbf24" : "#cbd5e1") +
                "; -fx-font-size: 13px; -fx-font-weight: " + (i == 0 ? "bold" : "normal") + ";");
            HBox.setHgrow(name, Priority.ALWAYS);
            Label games = new Label(u.getTotalGames() + " games");
            games.setStyle("-fx-text-fill: #34d399; -fx-font-size: 13px; -fx-font-weight: bold;");
            row.getChildren().addAll(medal, name, games);
            box.getChildren().add(row);
        }
        return box;
    }

    // ── Sign-Out ──────────────────────────────────────────────────────────────

    /** Creates a sign-out button. fullWidth = sidebar variant. */
    private Button signOutButton(boolean fullWidth) {
        Button btn = new Button("⏻  Sign Out");
        if (fullWidth) btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(
            "-fx-background-color: rgba(239,68,68,0.14);" +
            "-fx-text-fill: #ef4444;" +
            "-fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-padding: " + (fullWidth ? "10 18 10 18" : "8 14 8 14") + ";" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: rgba(239,68,68,0.28); -fx-border-radius: 10; -fx-border-width: 1;" +
            "-fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: rgba(239,68,68,0.28);" +
            "-fx-text-fill: #fca5a5;" +
            "-fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-padding: " + (fullWidth ? "10 18 10 18" : "8 14 8 14") + ";" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: rgba(239,68,68,0.5); -fx-border-radius: 10; -fx-border-width: 1;" +
            "-fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: rgba(239,68,68,0.14);" +
            "-fx-text-fill: #ef4444;" +
            "-fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-padding: " + (fullWidth ? "10 18 10 18" : "8 14 8 14") + ";" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: rgba(239,68,68,0.28); -fx-border-radius: 10; -fx-border-width: 1;" +
            "-fx-cursor: hand;"));
        btn.setOnAction(e -> handleSignOut());
        return btn;
    }

    private void handleSignOut() {
        boolean ok = AdminDialogs.showConfirm(
            "Sign Out",
            "Are you sure you want to sign out of the Admin Panel?",
            "You will be returned to the login screen.",
            "Sign Out", true
        );
        if (ok) {
            SessionManager.getInstance().logout();
            ui.MainUI.showLogin();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Button navBtn(String icon, String text, boolean active) {
        Button b = new Button(icon + "  " + text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setStyle(active ? activeNavStyle() : inactiveNavStyle());
        b.setOnMouseEntered(e -> {
            if (!b.getStyle().contains("#a78bfa")) {
                b.setStyle(hoverNavStyle());
            }
        });
        b.setOnMouseExited(e -> {
            if (!b.getStyle().contains("#a78bfa")) {
                b.setStyle(inactiveNavStyle());
            }
        });
        return b;
    }

    private void activateNav(Button active) {
        for (Button b : new Button[]{dashBtn, usersBtn, contentBtn, lessonsBtn,
                                      gameBtn, leaderBtn, subsBtn, certBtn, analyticsBtn}) {
            b.setStyle(b == active ? activeNavStyle() : inactiveNavStyle());
        }
    }

    private String activeNavStyle() {
        return "-fx-background-color: linear-gradient(to right, rgba(124,58,237,0.35), rgba(124,58,237,0.08));" +
               "-fx-text-fill: #a78bfa; -fx-font-size: 13px; -fx-font-weight: bold;" +
               "-fx-padding: 12 16 12 16; -fx-background-radius: 10;" +
               "-fx-border-color: rgba(124,58,237,0.5) transparent rgba(124,58,237,0.1) rgba(124,58,237,0.8);" +
               "-fx-border-radius: 10; -fx-border-width: 0 0 0 3; -fx-cursor: hand;";
    }

    private String inactiveNavStyle() {
        return "-fx-background-color: transparent; -fx-text-fill: #475569;" +
               "-fx-font-size: 13px; -fx-padding: 12 16 12 16; -fx-background-radius: 10; -fx-cursor: hand;";
    }

    private String hoverNavStyle() {
        return "-fx-background-color: rgba(255,255,255,0.04); -fx-text-fill: #94a3b8;" +
               "-fx-font-size: 13px; -fx-padding: 12 16 12 16; -fx-background-radius: 10; -fx-cursor: hand;";
    }

    private Button tinyBtn(String text, String color, String bg) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + color +
                   "; -fx-font-size: 11px; -fx-font-weight: bold;" +
                   "-fx-padding: 5 10 5 10; -fx-background-radius: 8; -fx-cursor: hand;");
        return b;
    }

    private Region divider() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-margin: 0 0 0 0;");
        return r;
    }

    private Label noDataLabel() {
        Label l = new Label("No data yet.");
        l.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");
        return l;
    }
}
