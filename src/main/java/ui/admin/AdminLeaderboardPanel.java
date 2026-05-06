package ui.admin;

import db.MongoDBManager;
import db.UserStatsDAO;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import model.GameResult;
import model.UserStats;

import java.util.List;
import java.util.Optional;

/**
 * Admin Leaderboard Panel - View rankings by WPM/XP, remove invalid scores.
 */
public class AdminLeaderboardPanel {

    private TableView<GameResult> wpmTable;
    private TableView<UserStats> xpTable;
    private String currentTab = "WPM";

    public ScrollPane buildContent() {
        VBox content = new VBox(20);
        content.setStyle("-fx-padding: 36 40 36 40;");

        Label title = new Label("Leaderboard & Ranking Management");
        title.getStyleClass().add("label-section");

        Label subtitle = new Label("View global rankings and remove invalid or fake scores");
        subtitle.getStyleClass().add("label-muted");

        HBox tabBar = buildTabBar();

        // WPM leaderboard
        wpmTable = buildWpmTable();
        xpTable = buildXpTable();

        // Initially show WPM table
        xpTable.setVisible(false);
        xpTable.setManaged(false);

        HBox actionButtons = buildActionButtons();

        content.getChildren().addAll(title, subtitle, tabBar, wpmTable, xpTable, actionButtons);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-dark");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        loadWpmLeaderboard();
        loadXpLeaderboard();
        return scroll;
    }

    // ── Tab Bar ───────────────────────────────────────────────────────────

    private HBox buildTabBar() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);

        Button wpmBtn = new Button("⚡ By WPM");
        wpmBtn.getStyleClass().add("diff-btn-selected");

        Button xpBtn = new Button("🌟 By XP / Level");
        xpBtn.getStyleClass().add("diff-btn");

        wpmBtn.setOnAction(e -> {
            currentTab = "WPM";
            wpmBtn.getStyleClass().setAll("diff-btn-selected");
            xpBtn.getStyleClass().setAll("diff-btn");
            wpmTable.setVisible(true); wpmTable.setManaged(true);
            xpTable.setVisible(false); xpTable.setManaged(false);
        });

        xpBtn.setOnAction(e -> {
            currentTab = "XP";
            xpBtn.getStyleClass().setAll("diff-btn-selected");
            wpmBtn.getStyleClass().setAll("diff-btn");
            xpTable.setVisible(true); xpTable.setManaged(true);
            wpmTable.setVisible(false); wpmTable.setManaged(false);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> { loadWpmLeaderboard(); loadXpLeaderboard(); });

        bar.getChildren().addAll(wpmBtn, xpBtn, spacer, refreshBtn);
        return bar;
    }

    // ── WPM Table ─────────────────────────────────────────────────────────

    private TableView<GameResult> buildWpmTable() {
        TableView<GameResult> table = new TableView<>();
        table.getStyleClass().add("table-dark");
        table.setPrefHeight(450);

        TableColumn<GameResult, String> rankCol = new TableColumn<>("Rank");
        rankCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); return; }
                int idx = getIndex() + 1;
                String medal = switch (idx) {
                    case 1 -> "🥇";
                    case 2 -> "🥈";
                    case 3 -> "🥉";
                    default -> "#" + idx;
                };
                setText(medal);
            }
        });
        rankCol.setPrefWidth(60);

        TableColumn<GameResult, String> userCol = new TableColumn<>("Username");
        userCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        userCol.setPrefWidth(150);

        TableColumn<GameResult, Double> wpmCol = new TableColumn<>("WPM");
        wpmCol.setCellValueFactory(new PropertyValueFactory<>("wpm"));
        wpmCol.setPrefWidth(100);
        wpmCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.1f", item));
            }
        });

        TableColumn<GameResult, Double> accCol = new TableColumn<>("Accuracy");
        accCol.setCellValueFactory(new PropertyValueFactory<>("accuracy"));
        accCol.setPrefWidth(100);
        accCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.1f%%", item));
            }
        });

        TableColumn<GameResult, String> modeCol = new TableColumn<>("Mode");
        modeCol.setCellValueFactory(new PropertyValueFactory<>("gameMode"));
        modeCol.setPrefWidth(100);

        TableColumn<GameResult, String> diffCol = new TableColumn<>("Difficulty");
        diffCol.setCellValueFactory(new PropertyValueFactory<>("difficulty"));
        diffCol.setPrefWidth(100);

        TableColumn<GameResult, Integer> wordsCol = new TableColumn<>("Words");
        wordsCol.setCellValueFactory(new PropertyValueFactory<>("wordsTyped"));
        wordsCol.setPrefWidth(80);

        table.getColumns().addAll(rankCol, userCol, wpmCol, accCol, modeCol, diffCol, wordsCol);
        return table;
    }

    // ── XP Table ──────────────────────────────────────────────────────────

    private TableView<UserStats> buildXpTable() {
        TableView<UserStats> table = new TableView<>();
        table.getStyleClass().add("table-dark");
        table.setPrefHeight(450);

        TableColumn<UserStats, String> rankCol = new TableColumn<>("Rank");
        rankCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); return; }
                int idx = getIndex() + 1;
                setText(switch (idx) { case 1 -> "🥇"; case 2 -> "🥈"; case 3 -> "🥉"; default -> "#" + idx; });
            }
        });
        rankCol.setPrefWidth(60);

        TableColumn<UserStats, String> userCol = new TableColumn<>("Username");
        userCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        userCol.setPrefWidth(150);

        TableColumn<UserStats, Integer> levelCol = new TableColumn<>("Level");
        levelCol.setCellValueFactory(new PropertyValueFactory<>("level"));
        levelCol.setPrefWidth(80);

        TableColumn<UserStats, Integer> xpCol = new TableColumn<>("XP");
        xpCol.setCellValueFactory(new PropertyValueFactory<>("xp"));
        xpCol.setPrefWidth(100);

        TableColumn<UserStats, Integer> streakCol = new TableColumn<>("Streak");
        streakCol.setCellValueFactory(new PropertyValueFactory<>("streak"));
        streakCol.setPrefWidth(80);

        TableColumn<UserStats, Integer> coinsCol = new TableColumn<>("Coins");
        coinsCol.setCellValueFactory(new PropertyValueFactory<>("coins"));
        coinsCol.setPrefWidth(80);

        table.getColumns().addAll(rankCol, userCol, levelCol, xpCol, streakCol, coinsCol);
        return table;
    }

    // ── Action Buttons ────────────────────────────────────────────────────

    private HBox buildActionButtons() {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);

        Button removeBtn = new Button("🗑 Remove Selected Score");
        removeBtn.getStyleClass().add("btn-danger");
        removeBtn.setOnAction(e -> removeScore());

        Button resetBtn = new Button("⚠️ Reset All Rankings");
        resetBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, #991b1b, #7f1d1d);" +
            "-fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 10 22 10 22;" +
            "-fx-background-radius: 8; -fx-cursor: hand;");
        resetBtn.setOnAction(e -> resetRankings());

        box.getChildren().addAll(removeBtn, resetBtn);
        return box;
    }

    // ── Data Loading ──────────────────────────────────────────────────────

    private void loadWpmLeaderboard() {
        List<GameResult> results = MongoDBManager.getInstance().getLeaderboard(100);
        wpmTable.setItems(FXCollections.observableArrayList(results));
    }

    private void loadXpLeaderboard() {
        List<UserStats> stats = UserStatsDAO.getInstance().getAllSortedByLevel(100);
        xpTable.setItems(FXCollections.observableArrayList(stats));
    }

    // ── Remove Score ──────────────────────────────────────────────────────

    private void removeScore() {
        if (!"WPM".equals(currentTab)) {
            showAlert("Info", "Score removal is only available on the WPM leaderboard.");
            return;
        }
        GameResult selected = wpmTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Select a score to remove.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Score");
        confirm.setHeaderText("Remove this score?");
        confirm.setContentText(String.format("User: %s | WPM: %.1f | Mode: %s",
                selected.getUsername(), selected.getWpm(), selected.getGameMode()));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            MongoDBManager.getInstance().deleteGameResult(selected.getId());
            loadWpmLeaderboard();
            showAlert("Success", "Score removed from leaderboard.");
        }
    }

    // ── Reset Rankings ────────────────────────────────────────────────────

    private void resetRankings() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("⚠️ Reset All Rankings");
        confirm.setHeaderText("Are you absolutely sure?");
        confirm.setContentText("This will DELETE ALL game results from the database.\nThis action cannot be undone!");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Double confirmation
            Alert confirm2 = new Alert(Alert.AlertType.CONFIRMATION);
            confirm2.setTitle("Final Confirmation");
            confirm2.setHeaderText("Last chance — this will erase ALL scores!");
            Optional<ButtonType> result2 = confirm2.showAndWait();
            if (result2.isPresent() && result2.get() == ButtonType.OK) {
                MongoDBManager.getInstance().deleteAllGameResults();
                loadWpmLeaderboard();
                loadXpLeaderboard();
                showAlert("Done", "All game results have been cleared.");
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
