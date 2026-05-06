package ui.admin;

import db.MongoDBManager;
import db.SubscriptionDAO;
import db.UserStatsDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import model.Subscription;
import model.User;
import model.UserStats;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin Users Panel — split Active / Blocked tabs, real subscription data,
 * custom-styled confirmation dialogs, and colour-coded table rows.
 */
public class AdminUsersPanel {

    // Master data
    private List<User> allUsers = new ArrayList<>();
    private Map<String, String> planMap = new HashMap<>();  // username → active plan

    // Active-tab controls
    private TableView<User> activeTable;
    private TextField       activeSearch;
    private Label           activeCount;

    // Blocked-tab controls
    private TableView<User> blockedTable;
    private TextField       blockedSearch;
    private Label           blockedCount;

    // Tab pane reference for refreshing headers
    private TabPane tabPane;

    // ── Entry point ───────────────────────────────────────────────────────────

    public ScrollPane buildContent() {
        VBox page = new VBox(22);
        page.setStyle("-fx-padding: 34 38 34 38; -fx-background-color: #080818;");

        // ── Page header ────────────────────────────────────────────────────
        VBox titleRow = new VBox(4);
        Label pageTitle = new Label("User Management");
        pageTitle.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label pageSub = new Label("Manage all registered players — view details, block, or remove accounts");
        pageSub.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");
        titleRow.getChildren().addAll(pageTitle, pageSub);

        // ── Summary cards ──────────────────────────────────────────────────
        HBox summaryRow = buildSummaryCards();

        // ── Tab pane ───────────────────────────────────────────────────────
        tabPane = buildTabPane();

        page.getChildren().addAll(titleRow, summaryRow, tabPane);

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-dark");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        loadAll();
        return scroll;
    }

    // ── Summary cards ─────────────────────────────────────────────────────────

    private HBox buildSummaryCards() {
        HBox row = new HBox(16);

        List<User> all  = MongoDBManager.getInstance().getAllUsers();
        int total   = all.size();
        int active  = (int) all.stream().filter(u -> !u.isBlocked()).count();
        int blocked = (int) all.stream().filter(User::isBlocked).count();

        // Real premium count from subscriptions
        long premium = SubscriptionDAO.getInstance().getAllSubscriptions().stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()) && !"FREE".equals(s.getPlan()))
                .map(Subscription::getUsername).distinct().count();

        row.getChildren().addAll(
            sumCard("Total Users",    String.valueOf(total),   "👥", "#a78bfa", "#7c3aed"),
            sumCard("Active Players", String.valueOf(active),  "✅", "#34d399", "#059669"),
            sumCard("Blocked",        String.valueOf(blocked), "🚫", "#f472b6", "#be185d"),
            sumCard("Premium",        String.valueOf(premium), "💎", "#fbbf24", "#b45309")
        );
        row.getChildren().forEach(n -> HBox.setHgrow((javafx.scene.Node) n, Priority.ALWAYS));
        return row;
    }

    private VBox sumCard(String label, String value, String icon, String light, String dark) {
        VBox c = new VBox(6);
        c.setStyle(
            "-fx-background-color: #111128; -fx-background-radius: 14;" +
            "-fx-padding: 16 20 16 20;" +
            "-fx-border-color: " + dark + "55;" +
            "-fx-border-radius: 14; -fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian," + dark + "44,12,0,0,3);");
        Label ico = new Label(icon); ico.setStyle("-fx-font-size: 20px;");
        Label val = new Label(value); val.setStyle("-fx-text-fill:" + light + ";-fx-font-size:22px;-fx-font-weight:bold;");
        Label lbl = new Label(label); lbl.setStyle("-fx-text-fill:#475569;-fx-font-size:11px;");
        c.getChildren().addAll(ico, val, lbl);
        return c;
    }

    // ── Tab pane ──────────────────────────────────────────────────────────────

    private TabPane buildTabPane() {
        TabPane tp = new TabPane();
        tp.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tp.setStyle("-fx-background-color: transparent; -fx-tab-min-width: 160;");

        Tab activeTab  = new Tab();
        Tab blockedTab = new Tab();

        activeTab.setContent(buildActiveContent());
        blockedTab.setContent(buildBlockedContent());

        tp.getTabs().addAll(activeTab, blockedTab);
        updateTabHeaders(activeTab, blockedTab);
        return tp;
    }

    private void updateTabHeaders(Tab activeTab, Tab blockedTab) {
        long aCount = allUsers.stream().filter(u -> !u.isBlocked()).count();
        long bCount = allUsers.stream().filter(User::isBlocked).count();

        activeTab.setGraphic(tabHeader("✅  Active Users", String.valueOf(aCount), "#34d399"));
        blockedTab.setGraphic(tabHeader("🚫  Blocked Users", String.valueOf(bCount), "#ef4444"));
    }

    private HBox tabHeader(String label, String count, String color) {
        HBox h = new HBox(8);
        h.setAlignment(Pos.CENTER);
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        Label c = new Label(count);
        c.setStyle(
            "-fx-background-color: " + color + "33;" +
            "-fx-text-fill: " + color + ";" +
            "-fx-font-size: 11px; -fx-font-weight: bold;" +
            "-fx-background-radius: 10; -fx-padding: 2 8 2 8;");
        h.getChildren().addAll(l, c);
        return h;
    }

    // ── Active Users content ─────────────────────────────────────────────────

    private VBox buildActiveContent() {
        VBox box = new VBox(14);
        box.setStyle("-fx-padding: 20 0 0 0; -fx-background-color: #080818;");

        // Toolbar
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle(
            "-fx-background-color: #111128; -fx-background-radius: 12;" +
            "-fx-padding: 12 16 12 16;" +
            "-fx-border-color: rgba(52,211,153,0.15);" +
            "-fx-border-radius: 12; -fx-border-width: 1;");

        Label searchIco = new Label("🔍");
        searchIco.setStyle("-fx-font-size: 14px;");
        activeSearch = searchField("Search active users…");
        activeSearch.textProperty().addListener((obs, o, nv) -> filterActive(nv));
        HBox.setHgrow(activeSearch, Priority.ALWAYS);

        activeCount = new Label();
        activeCount.setStyle("-fx-text-fill: #334155; -fx-font-size: 12px;");

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = tinyBtn("🔄 Refresh", "#34d399", "rgba(52,211,153,0.15)");
        refreshBtn.setOnAction(e -> loadAll());

        toolbar.getChildren().addAll(searchIco, activeSearch, spacer, activeCount, refreshBtn);

        // Table card
        activeTable = buildUserTable(false);
        VBox tableCard = wrapInCard(activeTable, "rgba(52,211,153,0.15)");

        // Actions
        HBox actions = buildActiveActions();

        box.getChildren().addAll(toolbar, tableCard, actions);
        return box;
    }

    // ── Blocked Users content ─────────────────────────────────────────────────

    private VBox buildBlockedContent() {
        VBox box = new VBox(14);
        box.setStyle("-fx-padding: 20 0 0 0; -fx-background-color: #080818;");

        // Warning banner
        HBox banner = new HBox(10);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setStyle(
            "-fx-background-color: rgba(239,68,68,0.08);" +
            "-fx-background-radius: 12; -fx-padding: 12 18 12 18;" +
            "-fx-border-color: rgba(239,68,68,0.25);" +
            "-fx-border-radius: 12; -fx-border-width: 1;");
        Label warnIco = new Label("🚫");
        warnIco.setStyle("-fx-font-size: 16px;");
        Label warnMsg = new Label(
            "Blocked accounts cannot log in. Select a user and click Unblock to restore access.");
        warnMsg.setStyle("-fx-text-fill: #fca5a5; -fx-font-size: 13px;");
        banner.getChildren().addAll(warnIco, warnMsg);

        // Toolbar
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle(
            "-fx-background-color: #111128; -fx-background-radius: 12;" +
            "-fx-padding: 12 16 12 16;" +
            "-fx-border-color: rgba(239,68,68,0.15);" +
            "-fx-border-radius: 12; -fx-border-width: 1;");

        Label searchIco = new Label("🔍");
        searchIco.setStyle("-fx-font-size: 14px;");
        blockedSearch = searchField("Search blocked users…");
        blockedSearch.textProperty().addListener((obs, o, nv) -> filterBlocked(nv));
        HBox.setHgrow(blockedSearch, Priority.ALWAYS);

        blockedCount = new Label();
        blockedCount.setStyle("-fx-text-fill: #334155; -fx-font-size: 12px;");

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = tinyBtn("🔄 Refresh", "#ef4444", "rgba(239,68,68,0.15)");
        refreshBtn.setOnAction(e -> loadAll());

        toolbar.getChildren().addAll(searchIco, blockedSearch, spacer, blockedCount, refreshBtn);

        // Table — blocked rows get a red tint via cell factory
        blockedTable = buildUserTable(true);
        VBox tableCard = wrapInCard(blockedTable, "rgba(239,68,68,0.2)");

        // Actions
        HBox actions = buildBlockedActions();

        box.getChildren().addAll(banner, toolbar, tableCard, actions);
        return box;
    }

    // ── Shared table builder ──────────────────────────────────────────────────

    private TableView<User> buildUserTable(boolean blockedMode) {
        TableView<User> table = new TableView<>();
        table.getStyleClass().add("table-dark");
        table.setPrefHeight(380);
        table.setPlaceholder(new Label(blockedMode ? "No blocked users." : "No active users."));

        // Row factory — red tint for blocked table
        if (blockedMode) {
            table.setRowFactory(tv -> new TableRow<>() {
                @Override protected void updateItem(User u, boolean empty) {
                    super.updateItem(u, empty);
                    setStyle(empty || u == null ? "" :
                        "-fx-background-color: rgba(239,68,68,0.07);");
                }
            });
        }

        // # rank
        TableColumn<User, Void> rankCol = new TableColumn<>("#");
        rankCol.setPrefWidth(44);
        rankCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean e) {
                super.updateItem(v, e);
                setText(e ? null : String.valueOf(getIndex() + 1));
                setStyle("-fx-text-fill:#475569;-fx-font-size:12px;");
            }
        });

        // Username
        TableColumn<User, String> userCol = col("Username", "username", 140, "#e2e8f0", true);

        // Email
        TableColumn<User, String> emailCol = col("Email", "email", 190, "#94a3b8", false);

        // Games
        TableColumn<User, Integer> gamesCol = new TableColumn<>("Games");
        gamesCol.setCellValueFactory(new PropertyValueFactory<>("totalGames"));
        gamesCol.setPrefWidth(70);
        gamesCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(String.valueOf(v));
                setStyle(v > 0 ? "-fx-text-fill:#34d399;-fx-font-weight:bold;" : "-fx-text-fill:#475569;");
            }
        });

        // Best WPM
        TableColumn<User, Double> wpmCol = new TableColumn<>("Best WPM");
        wpmCol.setCellValueFactory(new PropertyValueFactory<>("bestWpm"));
        wpmCol.setPrefWidth(90);
        wpmCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(String.format("%.1f", v));
                setStyle(v >= 80 ? "-fx-text-fill:#fbbf24;-fx-font-weight:bold;" :
                         v >= 50 ? "-fx-text-fill:#38bdf8;" : "-fx-text-fill:#94a3b8;");
            }
        });

        // Avg WPM
        TableColumn<User, Double> avgCol = new TableColumn<>("Avg WPM");
        avgCol.setCellValueFactory(new PropertyValueFactory<>("averageWpm"));
        avgCol.setPrefWidth(85);
        avgCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.1f", v));
            }
        });

        // Plan — pulled from SubscriptionDAO, not user.subscriptionType
        TableColumn<User, String> planCol = new TableColumn<>("Plan");
        planCol.setPrefWidth(95);
        planCol.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(
                planMap.getOrDefault(d.getValue().getUsername(), "FREE")));
        planCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                setStyle(switch (v) {
                    case "MONTHLY"  -> "-fx-text-fill:#38bdf8;-fx-font-weight:bold;";
                    case "LIFETIME" -> "-fx-text-fill:#fbbf24;-fx-font-weight:bold;";
                    default         -> "-fx-text-fill:#475569;";
                });
            }
        });

        // Status badge
        TableColumn<User, String> statusCol = new TableColumn<>("Status");
        statusCol.setPrefWidth(95);
        statusCol.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(
                d.getValue().isBlocked() ? "BLOCKED" : "ACTIVE"));
        statusCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                setStyle("BLOCKED".equals(v)
                    ? "-fx-text-fill:#ef4444;-fx-font-weight:bold;"
                    : "-fx-text-fill:#34d399;-fx-font-weight:bold;");
            }
        });

        table.getColumns().addAll(rankCol, userCol, emailCol, gamesCol, wpmCol, avgCol, planCol, statusCol);
        return table;
    }

    // ── Action bars ───────────────────────────────────────────────────────────

    private HBox buildActiveActions() {
        HBox box = actionsBar("rgba(52,211,153,0.12)");

        Button viewBtn  = actionBtn("👁  View Details",  "#a78bfa", "rgba(124,58,237,0.15)");
        Button blockBtn = actionBtn("🚫  Block User",    "#f472b6", "rgba(244,114,182,0.13)");
        Button deleteBtn= actionBtn("🗑  Delete User",   "#ef4444", "rgba(239,68,68,0.13)");

        viewBtn.setOnAction(e -> viewDetails(activeTable));
        blockBtn.setOnAction(e -> blockUser(activeTable, true));
        deleteBtn.setOnAction(e -> deleteUser(activeTable));

        Label hint = new Label("← Select a row first");
        hint.setStyle("-fx-text-fill: #334155; -fx-font-size: 12px;");

        box.getChildren().addAll(viewBtn, blockBtn, deleteBtn, hint);
        return box;
    }

    private HBox buildBlockedActions() {
        HBox box = actionsBar("rgba(239,68,68,0.1)");

        Button viewBtn    = actionBtn("👁  View Details",   "#a78bfa", "rgba(124,58,237,0.15)");
        Button unblockBtn = actionBtn("✅  Unblock User",   "#34d399", "rgba(52,211,153,0.13)");
        Button deleteBtn  = actionBtn("🗑  Delete User",    "#ef4444", "rgba(239,68,68,0.13)");

        viewBtn.setOnAction(e -> viewDetails(blockedTable));
        unblockBtn.setOnAction(e -> blockUser(blockedTable, false));
        deleteBtn.setOnAction(e -> deleteUser(blockedTable));

        box.getChildren().addAll(viewBtn, unblockBtn, deleteBtn);
        return box;
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadAll() {
        // Refresh user list
        allUsers = MongoDBManager.getInstance().getAllUsers();

        // Build username → active plan map from SubscriptionDAO
        planMap.clear();
        for (Subscription s : SubscriptionDAO.getInstance().getAllSubscriptions()) {
            if ("ACTIVE".equals(s.getStatus()) && !"FREE".equals(s.getPlan())) {
                planMap.put(s.getUsername(), s.getPlan());
            }
        }

        populateTables("");

        // Update tab headers
        if (tabPane != null) {
            Tab aTab = tabPane.getTabs().get(0);
            Tab bTab = tabPane.getTabs().get(1);
            updateTabHeaders(aTab, bTab);
        }
    }

    private void populateTables(String query) {
        String q = query == null ? "" : query.toLowerCase();

        List<User> active = allUsers.stream()
                .filter(u -> !u.isBlocked())
                .filter(u -> q.isBlank() ||
                             u.getUsername().toLowerCase().contains(q) ||
                             u.getEmail().toLowerCase().contains(q))
                .collect(Collectors.toList());

        List<User> blocked = allUsers.stream()
                .filter(User::isBlocked)
                .filter(u -> q.isBlank() ||
                             u.getUsername().toLowerCase().contains(q) ||
                             u.getEmail().toLowerCase().contains(q))
                .collect(Collectors.toList());

        if (activeTable  != null) activeTable.setItems(FXCollections.observableArrayList(active));
        if (blockedTable != null) blockedTable.setItems(FXCollections.observableArrayList(blocked));
        if (activeCount  != null) activeCount.setText(active.size() + " user(s)");
        if (blockedCount != null) blockedCount.setText(blocked.size() + " user(s)");
    }

    private void filterActive(String q)  { populateTables(q); }
    private void filterBlocked(String q) { populateTables(q); }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void viewDetails(TableView<User> table) {
        User u = table.getSelectionModel().getSelectedItem();
        if (u == null) { AdminDialogs.showInfo("No selection", "Please select a user first."); return; }

        UserStats stats = UserStatsDAO.getInstance().getOrCreate(u.getUsername());
        Subscription sub = SubscriptionDAO.getInstance().getLatest(u.getUsername());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        String subInfo = sub != null
            ? sub.getPlan() + " · " + sub.getStatus() +
              (sub.getEndDate() != null ? " (exp " + sub.getEndDate().format(fmt) + ")" : "")
            : "FREE";

        // Custom detail Stage
        Stage stage = AdminDialogs.buildStage("User Details — " + u.getUsername());
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #0c0c1e;");
        root.setPrefWidth(480);

        // Header gradient strip
        VBox header = new VBox(6);
        header.setStyle(
            "-fx-background-color: linear-gradient(to right, #1a103a, #0f0f22);" +
            "-fx-padding: 24 28 24 28;" +
            "-fx-border-color: transparent transparent rgba(124,58,237,0.25) transparent;" +
            "-fx-border-width: 0 0 1 0;");
        Label name = new Label("👤  " + u.getUsername());
        name.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label email = new Label(u.getEmail());
        email.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
        Label statusBadge = new Label(u.isBlocked() ? "🚫 BLOCKED" : "✅ ACTIVE");
        statusBadge.setStyle(
            "-fx-background-color: " + (u.isBlocked() ? "rgba(239,68,68,0.18)" : "rgba(52,211,153,0.15)") + ";" +
            "-fx-text-fill: " + (u.isBlocked() ? "#ef4444" : "#34d399") + ";" +
            "-fx-font-size: 11px; -fx-font-weight: bold;" +
            "-fx-background-radius: 10; -fx-padding: 3 10 3 10;");
        header.getChildren().addAll(name, email, statusBadge);

        // Body
        VBox body = new VBox(6);
        body.setStyle("-fx-padding: 22 28 24 28;");

        body.getChildren().addAll(
            detailSection("👤 Profile", new String[][]{
                {"Phone",   nvl(u.getPhoneNumber())},
                {"Address", nvl(u.getAddress())},
                {"Age",     String.valueOf(u.getAge())},
                {"DOB",     nvl(u.getDateOfBirth())}
            }),
            spacer(14),
            detailSection("🎮 Game Stats", new String[][]{
                {"Total Games",    String.valueOf(u.getTotalGames())},
                {"Best WPM",       String.format("%.1f", u.getBestWpm())},
                {"Avg WPM",        String.format("%.1f", u.getAverageWpm())},
                {"Best Accuracy",  String.format("%.1f%%", u.getBestAccuracy())}
            }),
            spacer(14),
            detailSection("🌟 Progression", new String[][]{
                {"Level",  String.valueOf(stats.getLevel())},
                {"XP",     String.valueOf(stats.getXp())},
                {"Coins",  String.valueOf(stats.getCoins())},
                {"Streak", stats.getStreak() + " days"}
            }),
            spacer(14),
            detailSection("💳 Subscription", new String[][]{
                {"Plan", subInfo}
            })
        );

        Button closeBtn = AdminDialogs.filledBtn("Close", "#7c3aed", "#6d28d9", "#7c3aed66");
        closeBtn.setOnAction(e -> stage.close());
        HBox btnRow = new HBox(); btnRow.setAlignment(Pos.CENTER);
        btnRow.setStyle("-fx-padding: 0 28 24 28;");
        btnRow.getChildren().add(closeBtn);

        root.getChildren().addAll(header, body, btnRow);
        AdminDialogs.showModal(stage, root);
    }

    private void blockUser(TableView<User> table, boolean blocking) {
        User u = table.getSelectionModel().getSelectedItem();
        if (u == null) { AdminDialogs.showInfo("No selection", "Please select a user first."); return; }

        String action = blocking ? "Block" : "Unblock";
        String msg    = blocking
            ? "This user will not be able to log in until unblocked."
            : "This user will regain access to the platform.";

        boolean confirmed = AdminDialogs.showConfirm(
            action + " User",
            msg,
            "Username: " + u.getUsername() + "\nEmail: " + u.getEmail(),
            action + " User",
            blocking
        );
        if (confirmed) {
            u.setBlocked(blocking);
            MongoDBManager.getInstance().updateUser(u);
            loadAll();
            AdminDialogs.showSuccess(
                "Done",
                "User " + u.getUsername() + " has been " + (blocking ? "blocked." : "unblocked.")
            );
        }
    }

    private void deleteUser(TableView<User> table) {
        User u = table.getSelectionModel().getSelectedItem();
        if (u == null) { AdminDialogs.showInfo("No selection", "Please select a user first."); return; }

        boolean confirmed = AdminDialogs.showConfirm(
            "Delete User",
            "This will permanently remove the user and all their game history.",
            "Username: " + u.getUsername() + "\nThis action cannot be undone!",
            "Delete Permanently",
            true
        );
        if (confirmed) {
            MongoDBManager.getInstance().deleteUser(u.getEmail());
            loadAll();
            AdminDialogs.showSuccess("Deleted", "User has been removed from the system.");
        }
    }

    // ── Small helpers ─────────────────────────────────────────────────────────

    private VBox detailSection(String heading, String[][] rows) {
        VBox section = new VBox(8);
        Label h = new Label(heading);
        h.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: bold;");
        section.getChildren().add(h);

        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(8);
        for (int i = 0; i < rows.length; i++) {
            Label key = new Label(rows[i][0]);
            key.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px; -fx-min-width: 120;");
            Label val = new Label(rows[i][1]);
            val.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px; -fx-font-weight: bold;");
            grid.add(key, 0, i); grid.add(val, 1, i);
        }
        section.getChildren().add(grid);
        return section;
    }

    @SuppressWarnings("unchecked")
    private <T> TableColumn<User, T> col(String title, String prop, int w, String color, boolean bold) {
        TableColumn<User, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        c.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(T v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v.toString());
                setStyle("-fx-text-fill:" + color + ";" + (bold ? "-fx-font-weight:bold;" : ""));
            }
        });
        return c;
    }

    private TextField searchField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.getStyleClass().add("field-dark");
        f.setPrefWidth(260);
        return f;
    }

    private VBox wrapInCard(javafx.scene.Node node, String borderColor) {
        VBox card = new VBox();
        card.setStyle(
            "-fx-background-color: #111128; -fx-background-radius: 16;" +
            "-fx-padding: 16 18 16 18;" +
            "-fx-border-color: " + borderColor + ";" +
            "-fx-border-radius: 16; -fx-border-width: 1;");
        card.getChildren().add(node);
        return card;
    }

    private HBox actionsBar(String bg) {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle(
            "-fx-background-color: #111128; -fx-background-radius: 12;" +
            "-fx-padding: 12 16 12 16;" +
            "-fx-border-color: " + bg + ";" +
            "-fx-border-radius: 12; -fx-border-width: 1;");
        return box;
    }

    private Button actionBtn(String text, String fg, String bg) {
        Button b = new Button(text);
        String base = "-fx-background-color:" + bg + ";-fx-text-fill:" + fg +
            ";-fx-font-size:13px;-fx-font-weight:bold;" +
            "-fx-padding:9 18 9 18;-fx-background-radius:10;-fx-cursor:hand;";
        b.setStyle(base);
        return b;
    }

    private Button tinyBtn(String text, String fg, String bg) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg +
            ";-fx-font-size:12px;-fx-padding:7 14 7 14;-fx-background-radius:8;-fx-cursor:hand;");
        return b;
    }

    private Region spacer(int h) {
        Region r = new Region(); r.setPrefHeight(h); return r;
    }

    private String nvl(String s) { return (s != null && !s.isBlank()) ? s : "—"; }
}
