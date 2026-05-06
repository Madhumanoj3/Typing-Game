package ui.admin;

import db.AchievementDefDAO;
import db.UserStatsDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import model.AchievementDefinition;
import model.UserStats;

import java.util.List;
import java.util.Optional;

/**
 * Admin Gamification Panel - Manage achievement definitions, XP rules, and view user stats.
 */
public class AdminGamificationPanel {

    private TableView<AchievementDefinition> achievementTable;
    private TableView<UserStats> statsTable;

    public ScrollPane buildContent() {
        VBox content = new VBox(24);
        content.setStyle("-fx-padding: 36 40 36 40;");

        Label title = new Label("Gamification Control");
        title.getStyleClass().add("label-section");

        // XP / Level formula cards
        HBox formulaCards = buildFormulaCards();

        // Achievement definitions section
        Label achTitle = new Label("🏆 Achievement Definitions");
        achTitle.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 16px; -fx-font-weight: bold;");

        achievementTable = buildAchievementTable();
        HBox achActions = buildAchievementActions();

        // Separator
        Region sep = new Region();
        sep.setStyle("-fx-background-color: #1e293b; -fx-pref-height: 1;");
        sep.setMaxWidth(Double.MAX_VALUE);

        // User Stats section
        Label statsTitle = new Label("📊 User Stats Overview");
        statsTitle.setStyle("-fx-text-fill: #06b6d4; -fx-font-size: 16px; -fx-font-weight: bold;");

        statsTable = buildStatsTable();

        content.getChildren().addAll(title, formulaCards, achTitle, achievementTable, achActions,
                sep, statsTitle, statsTable);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-dark");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        loadAchievements();
        loadUserStats();
        return scroll;
    }

    // ── Formula Cards ─────────────────────────────────────────────────────

    private HBox buildFormulaCards() {
        HBox box = new HBox(20);

        VBox xpCard = buildInfoCard("⚡ XP Calculation",
                "XP = (WPM × Accuracy) / 100\n" +
                "Example: 60 WPM × 90% acc = 54 XP",
                "#7c3aed");

        VBox levelCard = buildInfoCard("📈 Level Thresholds",
                "Level = Total XP / 100\n" +
                "Each level requires 100 XP\n" +
                "Level-up bonus: +10 coins",
                "#06b6d4");

        VBox streakCard = buildInfoCard("🔥 Streak Rules",
                "Play daily to maintain streak\n" +
                "Miss a day → streak resets to 1\n" +
                "Streak milestones: 3, 5, 7 days",
                "#f59e0b");

        box.getChildren().addAll(xpCard, levelCard, streakCard);
        return box;
    }

    private VBox buildInfoCard(String heading, String body, String color) {
        VBox card = new VBox(8);
        card.setStyle(
            "-fx-background-color: #1a1a2e;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 20;" +
            "-fx-min-width: 220;");

        Label h = new Label(heading);
        h.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label b = new Label(body);
        b.getStyleClass().add("label-body");
        b.setWrapText(true);

        card.getChildren().addAll(h, b);
        return card;
    }

    // ── Achievement Table ─────────────────────────────────────────────────

    private TableView<AchievementDefinition> buildAchievementTable() {
        TableView<AchievementDefinition> table = new TableView<>();
        table.getStyleClass().add("table-dark");
        table.setPrefHeight(250);

        TableColumn<AchievementDefinition, String> iconCol = new TableColumn<>("Icon");
        iconCol.setCellValueFactory(new PropertyValueFactory<>("icon"));
        iconCol.setPrefWidth(50);

        TableColumn<AchievementDefinition, String> badgeCol = new TableColumn<>("Badge");
        badgeCol.setCellValueFactory(new PropertyValueFactory<>("badge"));
        badgeCol.setPrefWidth(130);

        TableColumn<AchievementDefinition, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(140);

        TableColumn<AchievementDefinition, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(220);

        TableColumn<AchievementDefinition, String> condCol = new TableColumn<>("Condition");
        condCol.setCellValueFactory(data -> {
            AchievementDefinition d = data.getValue();
            String cond = d.getConditionType() + " >= " + (int) d.getConditionValue();
            return new javafx.beans.property.SimpleStringProperty(cond);
        });
        condCol.setPrefWidth(120);

        TableColumn<AchievementDefinition, Integer> xpCol = new TableColumn<>("XP Reward");
        xpCol.setCellValueFactory(new PropertyValueFactory<>("xpReward"));
        xpCol.setPrefWidth(90);

        table.getColumns().addAll(iconCol, badgeCol, titleCol, descCol, condCol, xpCol);
        return table;
    }

    // ── Achievement Actions ───────────────────────────────────────────────

    private HBox buildAchievementActions() {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = new Button("➕ Add Achievement");
        addBtn.getStyleClass().add("btn-success");
        addBtn.setOnAction(e -> showAddAchievementDialog());

        Button editBtn = new Button("✏️ Edit Selected");
        editBtn.getStyleClass().add("btn-primary");
        editBtn.setOnAction(e -> showEditAchievementDialog());

        Button deleteBtn = new Button("🗑 Delete Selected");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> deleteAchievement());

        box.getChildren().addAll(addBtn, editBtn, deleteBtn);
        return box;
    }

    // ── User Stats Table ──────────────────────────────────────────────────

    private TableView<UserStats> buildStatsTable() {
        TableView<UserStats> table = new TableView<>();
        table.getStyleClass().add("table-dark");
        table.setPrefHeight(250);

        TableColumn<UserStats, String> userCol = new TableColumn<>("Username");
        userCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        userCol.setPrefWidth(150);

        TableColumn<UserStats, Integer> xpCol = new TableColumn<>("XP");
        xpCol.setCellValueFactory(new PropertyValueFactory<>("xp"));
        xpCol.setPrefWidth(80);

        TableColumn<UserStats, Integer> levelCol = new TableColumn<>("Level");
        levelCol.setCellValueFactory(new PropertyValueFactory<>("level"));
        levelCol.setPrefWidth(80);

        TableColumn<UserStats, Integer> streakCol = new TableColumn<>("Streak");
        streakCol.setCellValueFactory(new PropertyValueFactory<>("streak"));
        streakCol.setPrefWidth(80);

        TableColumn<UserStats, Integer> coinsCol = new TableColumn<>("Coins");
        coinsCol.setCellValueFactory(new PropertyValueFactory<>("coins"));
        coinsCol.setPrefWidth(80);

        TableColumn<UserStats, Integer> dailyCol = new TableColumn<>("Daily Games");
        dailyCol.setCellValueFactory(new PropertyValueFactory<>("dailyGamesPlayed"));
        dailyCol.setPrefWidth(100);

        TableColumn<UserStats, String> lastActiveCol = new TableColumn<>("Last Active");
        lastActiveCol.setCellValueFactory(data -> {
            var d = data.getValue().getLastActiveDate();
            return new javafx.beans.property.SimpleStringProperty(d != null ? d.toString() : "Never");
        });
        lastActiveCol.setPrefWidth(120);

        table.getColumns().addAll(userCol, xpCol, levelCol, streakCol, coinsCol, dailyCol, lastActiveCol);
        return table;
    }

    // ── Data Loading ──────────────────────────────────────────────────────

    private void loadAchievements() {
        List<AchievementDefinition> defs = AchievementDefDAO.getInstance().getAll();
        achievementTable.setItems(FXCollections.observableArrayList(defs));
    }

    private void loadUserStats() {
        List<UserStats> stats = UserStatsDAO.getInstance().getAll();
        statsTable.setItems(FXCollections.observableArrayList(stats));
    }

    // ── Add Achievement Dialog ────────────────────────────────────────────

    private void showAddAchievementDialog() {
        Dialog<AchievementDefinition> dialog = new Dialog<>();
        dialog.setTitle("Add Achievement");
        dialog.setHeaderText("Create a new achievement definition");

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        GridPane grid = buildAchievementForm(null);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setStyle("-fx-background-color: #1a1a2e;");

        dialog.setResultConverter(bt -> bt == saveType ? extractAchievementFromForm(grid, null) : null);

        dialog.showAndWait().ifPresent(def -> {
            AchievementDefDAO.getInstance().save(def);
            loadAchievements();
            showAlert("Success", "Achievement created.");
        });
    }

    // ── Edit Achievement Dialog ───────────────────────────────────────────

    private void showEditAchievementDialog() {
        AchievementDefinition selected = achievementTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("No Selection", "Select an achievement to edit."); return; }

        Dialog<AchievementDefinition> dialog = new Dialog<>();
        dialog.setTitle("Edit Achievement");
        dialog.setHeaderText("Edit: " + selected.getTitle());

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        GridPane grid = buildAchievementForm(selected);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setStyle("-fx-background-color: #1a1a2e;");

        dialog.setResultConverter(bt -> bt == saveType ? extractAchievementFromForm(grid, selected) : null);

        dialog.showAndWait().ifPresent(def -> {
            AchievementDefDAO.getInstance().update(def);
            loadAchievements();
            showAlert("Success", "Achievement updated.");
        });
    }

    // ── Delete Achievement ────────────────────────────────────────────────

    private void deleteAchievement() {
        AchievementDefinition selected = achievementTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("No Selection", "Select an achievement to delete."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete achievement: " + selected.getTitle() + "?");

        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                AchievementDefDAO.getInstance().delete(selected.getId());
                loadAchievements();
                showAlert("Success", "Achievement deleted.");
            }
        });
    }

    // ── Achievement Form ──────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private GridPane buildAchievementForm(AchievementDefinition existing) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        int row = 0;

        TextField badgeField = field("badgeField", existing != null ? existing.getBadge() : "");
        grid.add(lbl("Badge Code:"), 0, row); grid.add(badgeField, 1, row++);

        TextField titleField = field("titleField", existing != null ? existing.getTitle() : "");
        grid.add(lbl("Title:"), 0, row); grid.add(titleField, 1, row++);

        TextField descField = field("descField", existing != null ? existing.getDescription() : "");
        grid.add(lbl("Description:"), 0, row); grid.add(descField, 1, row++);

        TextField iconField = field("iconField", existing != null ? existing.getIcon() : "🏅");
        grid.add(lbl("Icon (emoji):"), 0, row); grid.add(iconField, 1, row++);

        ComboBox<String> condCombo = new ComboBox<>(
                FXCollections.observableArrayList("WPM", "ACCURACY", "STREAK", "LEVEL", "GAMES"));
        condCombo.setId("condCombo");
        condCombo.getStyleClass().add("field-dark");
        condCombo.setValue(existing != null ? existing.getConditionType() : "WPM");
        grid.add(lbl("Condition Type:"), 0, row); grid.add(condCombo, 1, row++);

        TextField condValField = field("condValField",
                existing != null ? String.valueOf((int) existing.getConditionValue()) : "50");
        grid.add(lbl("Condition Value:"), 0, row); grid.add(condValField, 1, row++);

        TextField xpField = field("xpField",
                existing != null ? String.valueOf(existing.getXpReward()) : "50");
        grid.add(lbl("XP Reward:"), 0, row); grid.add(xpField, 1, row);

        return grid;
    }

    @SuppressWarnings("unchecked")
    private AchievementDefinition extractAchievementFromForm(GridPane grid, AchievementDefinition existing) {
        TextField badgeField = (TextField) grid.lookup("#badgeField");
        TextField titleField = (TextField) grid.lookup("#titleField");
        TextField descField = (TextField) grid.lookup("#descField");
        TextField iconField = (TextField) grid.lookup("#iconField");
        ComboBox<String> condCombo = (ComboBox<String>) grid.lookup("#condCombo");
        TextField condValField = (TextField) grid.lookup("#condValField");
        TextField xpField = (TextField) grid.lookup("#xpField");

        if (badgeField.getText().isBlank() || titleField.getText().isBlank()) return null;

        AchievementDefinition def = existing != null ? existing : new AchievementDefinition();
        def.setBadge(badgeField.getText().trim());
        def.setTitle(titleField.getText().trim());
        def.setDescription(descField.getText().trim());
        def.setIcon(iconField.getText().trim());
        def.setConditionType(condCombo.getValue());
        try { def.setConditionValue(Double.parseDouble(condValField.getText().trim())); }
        catch (NumberFormatException ex) { def.setConditionValue(0); }
        try { def.setXpReward(Integer.parseInt(xpField.getText().trim())); }
        catch (NumberFormatException ex) { def.setXpReward(0); }
        return def;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Label lbl(String text) { Label l = new Label(text); l.getStyleClass().add("label-body"); return l; }

    private TextField field(String id, String value) {
        TextField f = new TextField(value);
        f.setId(id);
        f.getStyleClass().add("field-dark");
        f.setPrefWidth(300);
        return f;
    }

    private void showAlert(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }
}
