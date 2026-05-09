package ui.admin;

import db.ContentDAO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import model.Content;
import game.ThemeManager;
import javafx.stage.Stage;

import java.util.List;

public class AdminContentPanel extends VBox {
    private ContentDAO contentDAO;
    private TableView<Content> contentTable;
    private TextField searchField;
    private ComboBox<String> typeFilter;
    private ComboBox<String> difficultyFilter;
    private Label statsLabel;

    public AdminContentPanel() {
        this.contentDAO = ContentDAO.getInstance();
        getStyleClass().add("admin-content-panel");
        setupUI();
        loadContent();
    }

    private void setupUI() {
        setPadding(new Insets(20));
        setSpacing(15);

        // Header
        Label title = new Label("Content Management");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        // Search and Filter Bar
        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("Search content...");
        searchField.setPrefWidth(200);
        searchField.textProperty().addListener((obs, old, val) -> loadContent());

        typeFilter = new ComboBox<>();
        typeFilter.getItems().addAll("All Types", "WORD", "SENTENCE");
        typeFilter.setValue("All Types");
        typeFilter.setOnAction(e -> loadContent());

        difficultyFilter = new ComboBox<>();
        difficultyFilter.getItems().addAll("All Difficulties", "EASY", "MEDIUM", "HARD");
        difficultyFilter.setValue("All Difficulties");
        difficultyFilter.setOnAction(e -> loadContent());

        Button addBtn = new Button("Add Content");
        addBtn.setOnAction(e -> showAddDialog());

        filterBar.getChildren().addAll(
            new Label("Search:"), searchField,
            new Label("Type:"), typeFilter,
            new Label("Difficulty:"), difficultyFilter,
            addBtn
        );

        // Stats
        statsLabel = new Label();
        statsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        // Table
        contentTable = new TableView<>();
        contentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Content, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(100);

        TableColumn<Content, String> textCol = new TableColumn<>("Text");
        textCol.setCellValueFactory(new PropertyValueFactory<>("text"));
        textCol.setPrefWidth(300);

        TableColumn<Content, String> difficultyCol = new TableColumn<>("Difficulty");
        difficultyCol.setCellValueFactory(new PropertyValueFactory<>("difficulty"));
        difficultyCol.setPrefWidth(100);

        TableColumn<Content, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(100);

        TableColumn<Content, Boolean> activeCol = new TableColumn<>("Active");
        activeCol.setCellValueFactory(new PropertyValueFactory<>("active"));
        activeCol.setPrefWidth(80);

        TableColumn<Content, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(200);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button toggleBtn = new Button("Toggle");
            private final Button deleteBtn = new Button("Delete");

            {
                editBtn.setOnAction(e -> {
                    Content content = getTableView().getItems().get(getIndex());
                    showEditDialog(content);
                });

                toggleBtn.setOnAction(e -> {
                    Content content = getTableView().getItems().get(getIndex());
                    content.setActive(!content.isActive());
                    contentDAO.update(content);
                    loadContent();
                });

                deleteBtn.setOnAction(e -> {
                    Content content = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Confirm Delete");
                    alert.setHeaderText("Delete Content");
                    alert.setContentText("Are you sure you want to delete this content?");
                    if (alert.showAndWait().get() == ButtonType.OK) {
                        contentDAO.delete(content.getId());
                        loadContent();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, editBtn, toggleBtn, deleteBtn);
                    setGraphic(buttons);
                }
            }
        });

        contentTable.getColumns().addAll(typeCol, textCol, difficultyCol, categoryCol, activeCol, actionsCol);

        getChildren().addAll(title, filterBar, statsLabel, contentTable);
        VBox.setVgrow(contentTable, Priority.ALWAYS);
    }

    private void loadContent() {
        String search = searchField.getText();
        String type = typeFilter.getValue().equals("All Types") ? null : typeFilter.getValue();
        String difficulty = difficultyFilter.getValue().equals("All Difficulties") ? null : difficultyFilter.getValue();

        List<Content> contents = contentDAO.search(search, type, difficulty);
        contentTable.getItems().setAll(contents);

        updateStats();
    }

    private void updateStats() {
        long easyWords = contentDAO.countByTypeAndDifficulty("WORD", "EASY");
        long mediumWords = contentDAO.countByTypeAndDifficulty("WORD", "MEDIUM");
        long hardWords = contentDAO.countByTypeAndDifficulty("WORD", "HARD");
        long sentences = contentDAO.countByTypeAndDifficulty("SENTENCE", "EASY") +
                        contentDAO.countByTypeAndDifficulty("SENTENCE", "MEDIUM") +
                        contentDAO.countByTypeAndDifficulty("SENTENCE", "HARD");

        statsLabel.setText(String.format("Easy: %d | Medium: %d | Hard: %d | Sentences: %d | Total: %d",
                easyWords, mediumWords, hardWords, sentences, contentTable.getItems().size()));
    }

    private void showAddDialog() {
        Stage stage = AdminDialogs.buildStage("Add New Content");

        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + (isLight ? "#ffffff" : "#0c0c1e") + ";");
        root.setPrefWidth(580);

        VBox header = buildFormHeader("➕  Add New Content",
                "Choose content type, difficulty, and enter the text");

        VBox form = buildContentForm(null);
        ScrollPane sp = scrollPane(form, 460);

        HBox btns = new HBox(12);
        btns.setAlignment(Pos.CENTER_RIGHT);
        btns.setStyle("-fx-padding: 16 28 24 28;" +
            "-fx-border-color: rgba(255,255,255,0.06) transparent transparent transparent;" +
            "-fx-border-width: 1 0 0 0;");
        Button cancelBtn = AdminDialogs.ghostBtn("Cancel");
        Button saveBtn   = AdminDialogs.filledBtn("Save Content", "#7c3aed", "#6d28d9", "#7c3aed66");
        btns.getChildren().addAll(cancelBtn, saveBtn);

        root.getChildren().addAll(header, sp, btns);

        cancelBtn.setOnAction(e -> stage.close());
        saveBtn.setOnAction(e -> {
            Content content = extractContent(form, null);
            if (content == null) {
                AdminDialogs.showError("Missing Fields", "Content text cannot be empty.");
                return;
            }
            contentDAO.create(content);
            loadContent();
            stage.close();
            AdminDialogs.showSuccess("Content Added", "New content has been created successfully.");
        });

        AdminDialogs.showModal(stage, root);
    }

    private void showEditDialog(Content content) {
        Stage stage = AdminDialogs.buildStage("Edit Content");

        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + (isLight ? "#ffffff" : "#0c0c1e") + ";");
        root.setPrefWidth(580);

        VBox header = buildFormHeader("✏️  Edit Content",
                "Update the content type, difficulty, and text");

        VBox form = buildContentForm(content);
        ScrollPane sp = scrollPane(form, 460);

        HBox btns = new HBox(12);
        btns.setAlignment(Pos.CENTER_RIGHT);
        btns.setStyle("-fx-padding: 16 28 24 28;" +
            "-fx-border-color: rgba(255,255,255,0.06) transparent transparent transparent;" +
            "-fx-border-width: 1 0 0 0;");
        Button cancelBtn = AdminDialogs.ghostBtn("Cancel");
        Button saveBtn   = AdminDialogs.filledBtn("Save Changes", "#7c3aed", "#6d28d9", "#7c3aed66");
        btns.getChildren().addAll(cancelBtn, saveBtn);

        root.getChildren().addAll(header, sp, btns);

        cancelBtn.setOnAction(e -> stage.close());
        saveBtn.setOnAction(e -> {
            Content updatedContent = extractContent(form, content);
            if (updatedContent == null) {
                AdminDialogs.showError("Missing Fields", "Content text cannot be empty.");
                return;
            }
            contentDAO.update(updatedContent);
            loadContent();
            stage.close();
            AdminDialogs.showSuccess("Content Updated", "Content has been saved.");
        });

        AdminDialogs.showModal(stage, root);
    }

    // ── Form Builder ──────────────────────────────────────────────────────

    private VBox buildFormHeader(String title, String subtitle) {
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        VBox header = new VBox(6);
        header.setStyle(
            "-fx-background-color: " + (isLight ? "linear-gradient(to right, #f0e6ff, #ffffff)" : "linear-gradient(to right, #1e1040, #0c0c1e)") + ";" +
            "-fx-padding: 24 28 18 28;" +
            "-fx-border-color: transparent transparent " + (isLight ? "rgba(124,58,237,0.2)" : "rgba(124,58,237,0.3)") + " transparent;" +
            "-fx-border-width: 0 0 1 0;");

        Label htitle = new Label(title);
        htitle.setStyle("-fx-text-fill: " + (isLight ? "#111827" : "white") + "; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label hsub = new Label(subtitle);
        hsub.setStyle("-fx-text-fill: " + (isLight ? "#6b7280" : "#475569") + "; -fx-font-size: 12px;");
        header.getChildren().addAll(htitle, hsub);
        return header;
    }

    private VBox buildContentForm(Content ex) {
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        VBox form = new VBox(18);
        form.setStyle("-fx-padding: 24 28 12 28; -fx-background-color: " + (isLight ? "#ffffff" : "#0c0c1e") + ";");

        // ── Section: Content Type ─────────────────────────────────────────
        form.getChildren().add(AdminDialogs.sectionHeader("📚", "Content Type", "#38bdf8"));

        HBox typeRow = new HBox(12);
        
        String[]   types      = {"WORD", "SENTENCE"}; // Data model uses SENTENCE
        String[]   typeLabels = {"WORD", "PARAGRAPH"}; // UI displays PARAGRAPH
        String[]   typeSub    = {"Single word for rapid-fire mode", "Full passage for practice sessions"};
        String[]   typeIcons  = {"🔤", "📄"};
        String[]   selType    = {ex != null ? ex.getType() : "WORD"};
        VBox[]   typeBtns   = new VBox[2];

        for (int i = 0; i < types.length; i++) {
            final int idx = i;
            VBox btn = new VBox(4);
            btn.setAlignment(Pos.CENTER);
            btn.setPrefWidth(240);
            btn.setPrefHeight(90);
            
            Label icon = new Label(typeIcons[i]);
            icon.setStyle("-fx-font-size: 24px;");
            Label title = new Label(typeLabels[i]);
            title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            Label sub = new Label(typeSub[i]);
            sub.setStyle("-fx-font-size: 10px;");
            
            btn.getChildren().addAll(icon, title, sub);
            btn.setStyle(typeBtnStyle("#38bdf8", types[i].equals(selType[0])));
            btn.setOnMouseClicked(e -> {
                selType[0] = types[idx];
                for (int j = 0; j < typeBtns.length; j++) {
                    typeBtns[j].setStyle(typeBtnStyle("#38bdf8", types[j].equals(selType[0])));
                }
            });
            typeBtns[i] = btn;
            typeRow.getChildren().add(btn);
        }
        form.getChildren().add(typeRow);

        // ── Section: Difficulty ───────────────────────────────────────────
        form.getChildren().add(AdminDialogs.sectionHeader("🎯", "Difficulty Level", "#fbbf24"));

        String[]   levels      = {"EASY", "MEDIUM", "HARD"}; // Data model
        String[]   levelLabels = {"Beginner", "Intermediate", "Advanced"}; // UI
        String[]   levelColors = {"#10b981",  "#fbbf24",       "#ef4444"};
        String[]   selLevel    = {ex != null ? ex.getDifficulty() : "EASY"};
        Button[]   levelBtns   = new Button[3];
        HBox       levelBtnRow = new HBox(8);

        for (int i = 0; i < levels.length; i++) {
            final int idx = i;
            Button lb = new Button(levelLabels[i]);
            lb.setStyle(levelBtnStyle(levelColors[i], levels[i].equals(selLevel[0])));
            lb.setOnAction(e -> {
                selLevel[0] = levels[idx];
                for (int j = 0; j < levelBtns.length; j++) {
                    levelBtns[j].setStyle(levelBtnStyle(levelColors[j], levels[j].equals(selLevel[0])));
                }
            });
            levelBtns[i] = lb;
            levelBtnRow.getChildren().add(lb);
        }
        form.getChildren().add(levelBtnRow);

        // ── Section: Category & Active ────────────────────────────────────
        HBox rowInfo = new HBox(16);
        VBox catBox = new VBox(6);
        catBox.getChildren().add(AdminDialogs.formLabel("CATEGORY"));
        TextField catField = styledField("General", ex != null && ex.getCategory() != null ? ex.getCategory() : "General", "catField");
        catBox.getChildren().add(catField);
        HBox.setHgrow(catBox, Priority.ALWAYS);

        VBox activeBox = new VBox(6);
        activeBox.getChildren().add(AdminDialogs.formLabel("STATUS"));
        boolean[] isActive = {ex == null || ex.isActive()};
        Button activeBtn = new Button(isActive[0] ? "✅ Active" : "❌ Inactive");
        activeBtn.setStyle(accessBtnStyle(isActive[0] ? "#10b981" : "#ef4444", true));
        activeBtn.setOnAction(e -> {
            isActive[0] = !isActive[0];
            activeBtn.setText(isActive[0] ? "✅ Active" : "❌ Inactive");
            activeBtn.setStyle(accessBtnStyle(isActive[0] ? "#10b981" : "#ef4444", true));
        });
        activeBox.getChildren().add(activeBtn);
        
        rowInfo.getChildren().addAll(catBox, activeBox);
        form.getChildren().add(rowInfo);

        // ── Section: Content Text ─────────────────────────────────────────
        form.getChildren().add(AdminDialogs.sectionHeader("📝", "Content Text", "#10b981"));

        VBox contentBox = new VBox(6);
        contentBox.getChildren().add(AdminDialogs.formLabel("TEXT CONTENT"));
        TextArea contentArea = styledTextArea(
            "Enter word or paragraph content here...",
            ex != null ? ex.getText() : "", 6, "contentArea");
        contentBox.getChildren().add(contentArea);
        form.getChildren().add(contentBox);

        // Store mutable state for extraction
        form.setUserData(new Object[]{selType, selLevel, isActive});
        return form;
    }

    private Content extractContent(VBox form, Content existing) {
        TextArea  contentArea = (TextArea)  form.lookup("#contentArea");
        TextField catField    = (TextField) form.lookup("#catField");

        if (contentArea.getText().trim().isBlank()) return null;

        Object[] state  = (Object[]) form.getUserData();
        String[] type   = (String[])  state[0];
        String[] lvl    = (String[])  state[1];
        boolean[] active= (boolean[]) state[2];

        Content content = existing != null ? existing : new Content();
        content.setType(type[0]);
        content.setDifficulty(lvl[0]);
        content.setText(contentArea.getText().trim());
        content.setCategory(catField.getText().trim());
        content.setActive(active[0]);
        
        return content;
    }

    // ── Style helpers ─────────────────────────────────────────────────────

    private String typeBtnStyle(String color, boolean active) {
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        if (active) {
            return "-fx-background-color: " + (isLight ? "rgba(56,189,248,0.1)" : "rgba(56,189,248,0.05)") + ";" +
                   "-fx-text-fill: " + color + ";" +
                   "-fx-background-radius: 12; -fx-border-color: " + color + ";" +
                   "-fx-border-radius: 12; -fx-border-width: 1.5; -fx-cursor: hand;";
        } else {
            return "-fx-background-color: " + (isLight ? "#f9fafb" : "rgba(255,255,255,0.02)") + ";" +
                   "-fx-text-fill: " + (isLight ? "#6b7280" : "#475569") + ";" +
                   "-fx-background-radius: 12; -fx-border-color: " + (isLight ? "rgba(124,58,237,0.1)" : "rgba(255,255,255,0.05)") + ";" +
                   "-fx-border-radius: 12; -fx-border-width: 1; -fx-cursor: hand;";
        }
    }

    private String levelBtnStyle(String color, boolean active) {
        return active
            ? "-fx-background-color: " + color + "33; -fx-text-fill: " + color + ";" +
              "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 16 8 16;" +
              "-fx-background-radius: 10; -fx-border-color: " + color + "88;" +
              "-fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand;"
            : "-fx-background-color: rgba(255,255,255,0.04); -fx-text-fill: #475569;" +
              "-fx-font-size: 12px; -fx-padding: 8 16 8 16;" +
              "-fx-background-radius: 10; -fx-border-color: rgba(255,255,255,0.1);" +
              "-fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand;";
    }

    private String accessBtnStyle(String color, boolean active) {
        return active
            ? "-fx-background-color: " + color + "22; -fx-text-fill: " + color + ";" +
              "-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 9 20 9 20;" +
              "-fx-background-radius: 10; -fx-border-color: " + color + "77;" +
              "-fx-border-radius: 10; -fx-border-width: 1.5; -fx-cursor: hand;"
            : "-fx-background-color: rgba(255,255,255,0.04); -fx-text-fill: #475569;" +
              "-fx-font-size: 13px; -fx-padding: 9 20 9 20;" +
              "-fx-background-radius: 10; -fx-border-color: rgba(255,255,255,0.08);" +
              "-fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand;";
    }

    private TextField styledField(String prompt, String value, String id) {
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        TextField f = new TextField(value);
        f.setId(id);
        f.setPromptText(prompt);
        f.setMaxWidth(Double.MAX_VALUE);
        f.setStyle(
            "-fx-control-inner-background: " + (isLight ? "#f9fafb" : "#111128") + "; -fx-text-fill: " + (isLight ? "#111827" : "#e2e8f0") + ";" +
            "-fx-font-size: 13px; -fx-background-color: " + (isLight ? "#f9fafb" : "#111128") + ";" +
            "-fx-border-color: " + (isLight ? "rgba(124,58,237,0.2)" : "rgba(255,255,255,0.1)") + "; -fx-border-radius: 10;" +
            "-fx-background-radius: 10; -fx-padding: 10 12 10 12;");
        return f;
    }

    private TextArea styledTextArea(String prompt, String value, int rows, String id) {
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        TextArea a = new TextArea(value);
        a.setId(id);
        a.setPromptText(prompt);
        a.setWrapText(true);
        a.setPrefRowCount(rows);
        a.setMaxWidth(Double.MAX_VALUE);
        a.setStyle(
            "-fx-control-inner-background: " + (isLight ? "#f9fafb" : "#111128") + "; -fx-text-fill: " + (isLight ? "#111827" : "#e2e8f0") + ";" +
            "-fx-font-size: 13px; -fx-background-color: " + (isLight ? "#f9fafb" : "#111128") + ";" +
            "-fx-border-color: " + (isLight ? "rgba(124,58,237,0.2)" : "rgba(255,255,255,0.1)") + "; -fx-border-radius: 10;" +
            "-fx-background-radius: 10;");
        return a;
    }

    private ScrollPane scrollPane(javafx.scene.Node content, double height) {
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setPrefHeight(height);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background: " + (isLight ? "#ffffff" : "#0c0c1e") + "; -fx-background-color: " + (isLight ? "#ffffff" : "#0c0c1e") + ";");
        return sp;
    }
}
