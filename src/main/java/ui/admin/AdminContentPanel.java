package ui.admin;

import db.ContentDAO;
import game.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import model.Content;

import java.util.List;

public class AdminContentPanel extends VBox {

    private ContentDAO              contentDAO;
    private TableView<Content>      contentTable;
    private TextField               searchField;
    private ComboBox<String>        typeFilter;
    private ComboBox<String>        difficultyFilter;
    private HBox                    statsRow;

    public AdminContentPanel() {
        this.contentDAO = ContentDAO.getInstance();
        getStyleClass().add("admin-content-panel");
        setupUI();
        loadContent();
    }

    // ── UI construction ───────────────────────────────────────────────────

    private void setupUI() {
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        String bg      = isLight ? "#f5f5f5"  : "#0f0f1a";
        String cardBg  = isLight ? "#ffffff"  : "#1a1a2e";
        String border  = isLight ? "#e5e7eb"  : "#1e293b";
        String text    = isLight ? "#111827"  : "#f1f5f9";
        String muted   = isLight ? "#6b7280"  : "#64748b";

        setPadding(new Insets(32, 36, 32, 36));
        setSpacing(20);
        setStyle("-fx-background-color: " + bg + ";");

        // ── Header ────────────────────────────────────────────────────────
        Label icon  = new Label("📚");
        icon.setStyle("-fx-font-size: 28px;");
        Label titleLbl = new Label("Content Management");
        titleLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + text + ";");
        Label subLbl = new Label("Manage words, sentences and paragraphs used in typing games");
        subLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + muted + ";");
        VBox titleBlock = new VBox(2, titleLbl, subLbl);
        HBox header = new HBox(12, icon, titleBlock);
        header.setAlignment(Pos.CENTER_LEFT);

        // ── Filter / action bar ───────────────────────────────────────────
        searchField = new TextField();
        searchField.setPromptText("🔍  Search content...");
        searchField.setPrefWidth(240);
        searchField.getStyleClass().add("field-dark");
        searchField.textProperty().addListener((obs, o, v) -> loadContent());

        typeFilter = new ComboBox<>();
        typeFilter.getItems().addAll("All Types", "WORD", "SENTENCE");
        typeFilter.setValue("All Types");
        typeFilter.getStyleClass().add("combo-box");
        typeFilter.setPrefWidth(140);
        typeFilter.setOnAction(e -> loadContent());

        difficultyFilter = new ComboBox<>();
        difficultyFilter.getItems().addAll("All Difficulties", "EASY", "MEDIUM", "HARD");
        difficultyFilter.setValue("All Difficulties");
        difficultyFilter.getStyleClass().add("combo-box");
        difficultyFilter.setPrefWidth(160);
        difficultyFilter.setOnAction(e -> loadContent());

        Button addBtn = new Button("＋  Add Content");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setPadding(new Insets(9, 20, 9, 20));
        addBtn.setOnAction(e -> showAddDialog());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label typeLabel = styled("Type:", muted);
        Label diffLabel = styled("Difficulty:", muted);

        HBox filterBar = new HBox(10,
                searchField, typeLabel, typeFilter,
                diffLabel, difficultyFilter,
                spacer, addBtn);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(14, 20, 14, 20));
        filterBar.setStyle(
            "-fx-background-color: " + cardBg + ";" +
            "-fx-background-radius: 14;" +
            "-fx-border-color: " + border + ";" +
            "-fx-border-radius: 14;" +
            "-fx-border-width: 1;");

        // ── Stats row ─────────────────────────────────────────────────────
        statsRow = new HBox(12);

        // ── Table ─────────────────────────────────────────────────────────
        contentTable = new TableView<>();
        contentTable.getStyleClass().add("table-dark");
        contentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        contentTable.setPlaceholder(buildEmptyPlaceholder(muted));

        TableColumn<Content, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(110);
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); return; }
                String display = "SENTENCE".equals(v) ? "PARAGRAPH" : v;
                String color   = "SENTENCE".equals(v) ? "#a78bfa" : "#38bdf8";
                Label badge = new Label(display);
                badge.setStyle(
                    "-fx-background-color: " + color + "22;" +
                    "-fx-text-fill: " + color + ";" +
                    "-fx-font-size: 10px; -fx-font-weight: bold;" +
                    "-fx-padding: 3 10 3 10;" +
                    "-fx-background-radius: 6;");
                setGraphic(badge);
            }
        });

        TableColumn<Content, String> textCol = new TableColumn<>("Text");
        textCol.setCellValueFactory(new PropertyValueFactory<>("text"));
        textCol.setPrefWidth(340);
        textCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                String display = v.length() > 80 ? v.substring(0, 77) + "…" : v;
                setText(display);
                setTooltip(new Tooltip(v));
            }
        });

        TableColumn<Content, String> diffCol = new TableColumn<>("Difficulty");
        diffCol.setCellValueFactory(new PropertyValueFactory<>("difficulty"));
        diffCol.setPrefWidth(110);
        diffCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); return; }
                String color = "EASY".equals(v) ? "#10b981" : "MEDIUM".equals(v) ? "#f59e0b" : "#ef4444";
                String label = "EASY".equals(v) ? "Beginner" : "MEDIUM".equals(v) ? "Intermediate" : "Advanced";
                Label badge = new Label(label);
                badge.setStyle(
                    "-fx-background-color: " + color + "22;" +
                    "-fx-text-fill: " + color + ";" +
                    "-fx-font-size: 10px; -fx-font-weight: bold;" +
                    "-fx-padding: 3 10 3 10;" +
                    "-fx-background-radius: 6;");
                setGraphic(badge);
            }
        });

        TableColumn<Content, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        catCol.setPrefWidth(100);

        TableColumn<Content, Boolean> activeCol = new TableColumn<>("Status");
        activeCol.setCellValueFactory(new PropertyValueFactory<>("active"));
        activeCol.setPrefWidth(90);
        activeCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); return; }
                Label badge = new Label(v ? "● Active" : "● Inactive");
                badge.setStyle(
                    "-fx-text-fill: " + (v ? "#10b981" : "#ef4444") + ";" +
                    "-fx-font-size: 11px; -fx-font-weight: bold;");
                setGraphic(badge);
            }
        });

        TableColumn<Content, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(200);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn   = actionBtn("✏  Edit",   "#38bdf8");
            private final Button toggleBtn = actionBtn("⇄  Toggle", "#f59e0b");
            private final Button deleteBtn = actionBtn("✕  Delete", "#ef4444");

            {
                editBtn.setOnAction(e -> showEditDialog(getTableView().getItems().get(getIndex())));
                toggleBtn.setOnAction(e -> {
                    Content c = getTableView().getItems().get(getIndex());
                    c.setActive(!c.isActive());
                    contentDAO.update(c);
                    loadContent();
                });
                deleteBtn.setOnAction(e -> {
                    Content c = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "Delete \"" + (c.getText().length() > 40 ? c.getText().substring(0,40) + "…" : c.getText()) + "\"?",
                            ButtonType.OK, ButtonType.CANCEL);
                    alert.setTitle("Confirm Delete");
                    alert.setHeaderText(null);
                    alert.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b -> {
                        contentDAO.delete(c.getId());
                        loadContent();
                    });
                });
            }

            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                HBox row = new HBox(6, editBtn, toggleBtn, deleteBtn);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
            }
        });

        contentTable.getColumns().addAll(typeCol, textCol, diffCol, catCol, activeCol, actionsCol);
        VBox.setVgrow(contentTable, Priority.ALWAYS);

        getChildren().addAll(header, filterBar, statsRow, contentTable);
    }

    // ── Data ──────────────────────────────────────────────────────────────

    private void loadContent() {
        String search     = searchField.getText();
        String type       = "All Types".equals(typeFilter.getValue())           ? null : typeFilter.getValue();
        String difficulty = "All Difficulties".equals(difficultyFilter.getValue()) ? null : difficultyFilter.getValue();

        List<Content> contents = contentDAO.search(search, type, difficulty);
        contentTable.getItems().setAll(contents);
        updateStats();
    }

    private void updateStats() {
        long easy      = contentDAO.countByTypeAndDifficulty("WORD",     "EASY");
        long medium    = contentDAO.countByTypeAndDifficulty("WORD",     "MEDIUM");
        long hard      = contentDAO.countByTypeAndDifficulty("WORD",     "HARD");
        long sentences = contentDAO.countByTypeAndDifficulty("SENTENCE", "EASY")
                       + contentDAO.countByTypeAndDifficulty("SENTENCE", "MEDIUM")
                       + contentDAO.countByTypeAndDifficulty("SENTENCE", "HARD");
        long total     = contentTable.getItems().size();

        statsRow.getChildren().setAll(
            statCard("Beginner Words",  easy,      "#10b981", "🟢"),
            statCard("Intermediate",    medium,    "#f59e0b", "🟡"),
            statCard("Advanced Words",  hard,      "#ef4444", "🔴"),
            statCard("Paragraphs",      sentences, "#a78bfa", "📄"),
            statCard("Total Content",   total,     "#38bdf8", "📊")
        );
    }

    // ── Dialogs ───────────────────────────────────────────────────────────

    private void showAddDialog() {
        Stage stage = AdminDialogs.buildStage("Add New Content");
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + (isLight ? "#ffffff" : "#0c0c1e") + ";");
        root.setPrefWidth(580);

        VBox header = buildFormHeader("➕  Add New Content", "Choose content type, difficulty, and enter the text");
        VBox form   = buildContentForm(null);
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
            if (content == null) { AdminDialogs.showError("Missing Fields", "Content text cannot be empty."); return; }
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

        VBox header = buildFormHeader("✏️  Edit Content", "Update the content type, difficulty, and text");
        VBox form   = buildContentForm(content);
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
            if (updatedContent == null) { AdminDialogs.showError("Missing Fields", "Content text cannot be empty."); return; }
            contentDAO.update(updatedContent);
            loadContent();
            stage.close();
            AdminDialogs.showSuccess("Content Updated", "Content has been saved.");
        });

        AdminDialogs.showModal(stage, root);
    }

    // ── Form builder (unchanged logic) ───────────────────────────────────

    private VBox buildFormHeader(String title, String subtitle) {
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        VBox header = new VBox(6);
        header.setStyle(
            "-fx-background-color: " + (isLight ? "linear-gradient(to right,#f0e6ff,#ffffff)" : "linear-gradient(to right,#1e1040,#0c0c1e)") + ";" +
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

        form.getChildren().add(AdminDialogs.sectionHeader("📚", "Content Type", "#38bdf8"));
        HBox typeRow = new HBox(12);
        String[]   types      = {"WORD", "SENTENCE"};
        String[]   typeLabels = {"WORD", "PARAGRAPH"};
        String[]   typeSub    = {"Single word for rapid-fire mode", "Full passage for practice sessions"};
        String[]   typeIcons  = {"🔤", "📄"};
        String[]   selType    = {ex != null ? ex.getType() : "WORD"};
        VBox[]     typeBtns   = new VBox[2];
        for (int i = 0; i < types.length; i++) {
            final int idx = i;
            VBox btn = new VBox(4);
            btn.setAlignment(Pos.CENTER);
            btn.setPrefWidth(240); btn.setPrefHeight(90);
            Label ic = new Label(typeIcons[i]); ic.setStyle("-fx-font-size: 24px;");
            Label tl = new Label(typeLabels[i]); tl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            Label sl = new Label(typeSub[i]); sl.setStyle("-fx-font-size: 10px;");
            btn.getChildren().addAll(ic, tl, sl);
            btn.setStyle(typeBtnStyle("#38bdf8", types[i].equals(selType[0])));
            btn.setOnMouseClicked(e -> {
                selType[0] = types[idx];
                for (int j = 0; j < typeBtns.length; j++)
                    typeBtns[j].setStyle(typeBtnStyle("#38bdf8", types[j].equals(selType[0])));
            });
            typeBtns[i] = btn; typeRow.getChildren().add(btn);
        }
        form.getChildren().add(typeRow);

        form.getChildren().add(AdminDialogs.sectionHeader("🎯", "Difficulty Level", "#fbbf24"));
        String[] levels = {"EASY", "MEDIUM", "HARD"};
        String[] levelLabels = {"Beginner", "Intermediate", "Advanced"};
        String[] levelColors = {"#10b981", "#fbbf24", "#ef4444"};
        String[] selLevel = {ex != null ? ex.getDifficulty() : "EASY"};
        Button[] levelBtns = new Button[3];
        HBox levelBtnRow = new HBox(8);
        for (int i = 0; i < levels.length; i++) {
            final int idx = i;
            Button lb = new Button(levelLabels[i]);
            lb.setStyle(levelBtnStyle(levelColors[i], levels[i].equals(selLevel[0])));
            lb.setOnAction(e -> {
                selLevel[0] = levels[idx];
                for (int j = 0; j < levelBtns.length; j++)
                    levelBtns[j].setStyle(levelBtnStyle(levelColors[j], levels[j].equals(selLevel[0])));
            });
            levelBtns[i] = lb; levelBtnRow.getChildren().add(lb);
        }
        form.getChildren().add(levelBtnRow);

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

        form.getChildren().add(AdminDialogs.sectionHeader("📝", "Content Text", "#10b981"));
        VBox contentBox = new VBox(6);
        contentBox.getChildren().add(AdminDialogs.formLabel("TEXT CONTENT"));
        TextArea contentArea = styledTextArea("Enter word or paragraph content here...",
                ex != null ? ex.getText() : "", 6, "contentArea");
        contentBox.getChildren().add(contentArea);
        form.getChildren().add(contentBox);

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

    // ── UI helpers ────────────────────────────────────────────────────────

    private VBox statCard(String label, long value, String color, String icon) {
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 20, 14, 20));
        card.setStyle(
            "-fx-background-color: " + (isLight ? "#ffffff" : "#1a1a2e") + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: " + color + "44;" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1;");
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox top = new HBox(6);
        top.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 13px;");
        Label labelLbl = new Label(label);
        labelLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (isLight ? "#6b7280" : "#64748b") + ";");
        top.getChildren().addAll(iconLbl, labelLbl);

        Label valueLbl = new Label(String.valueOf(value));
        valueLbl.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        card.getChildren().addAll(top, valueLbl);
        return card;
    }

    private Label styled(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
        return l;
    }

    private Label buildEmptyPlaceholder(String muted) {
        Label l = new Label("No content found. Click  ＋ Add Content  to create some.");
        l.setStyle("-fx-text-fill: " + muted + "; -fx-font-size: 13px;");
        return l;
    }

    private Button actionBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle(
            "-fx-background-color: " + color + "18;" +
            "-fx-text-fill: " + color + ";" +
            "-fx-font-size: 11px; -fx-font-weight: bold;" +
            "-fx-padding: 5 12 5 12;" +
            "-fx-background-radius: 7;" +
            "-fx-border-color: " + color + "55;" +
            "-fx-border-radius: 7;" +
            "-fx-border-width: 1;" +
            "-fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setStyle(
            "-fx-background-color: " + color + "33;" +
            "-fx-text-fill: " + color + ";" +
            "-fx-font-size: 11px; -fx-font-weight: bold;" +
            "-fx-padding: 5 12 5 12;" +
            "-fx-background-radius: 7;" +
            "-fx-border-color: " + color + ";" +
            "-fx-border-radius: 7;" +
            "-fx-border-width: 1;" +
            "-fx-cursor: hand;"));
        b.setOnMouseExited(e -> b.setStyle(
            "-fx-background-color: " + color + "18;" +
            "-fx-text-fill: " + color + ";" +
            "-fx-font-size: 11px; -fx-font-weight: bold;" +
            "-fx-padding: 5 12 5 12;" +
            "-fx-background-radius: 7;" +
            "-fx-border-color: " + color + "55;" +
            "-fx-border-radius: 7;" +
            "-fx-border-width: 1;" +
            "-fx-cursor: hand;"));
        return b;
    }

    // ── Form style helpers (unchanged) ────────────────────────────────────

    private String typeBtnStyle(String color, boolean active) {
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        return active
            ? "-fx-background-color: " + (isLight ? "rgba(56,189,248,0.1)" : "rgba(56,189,248,0.05)") + ";" +
              "-fx-text-fill: " + color + "; -fx-background-radius: 12;" +
              "-fx-border-color: " + color + "; -fx-border-radius: 12; -fx-border-width: 1.5; -fx-cursor: hand;"
            : "-fx-background-color: " + (isLight ? "#f9fafb" : "rgba(255,255,255,0.02)") + ";" +
              "-fx-text-fill: " + (isLight ? "#6b7280" : "#475569") + "; -fx-background-radius: 12;" +
              "-fx-border-color: " + (isLight ? "rgba(124,58,237,0.1)" : "rgba(255,255,255,0.05)") + ";" +
              "-fx-border-radius: 12; -fx-border-width: 1; -fx-cursor: hand;";
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
        f.setId(id); f.setPromptText(prompt); f.setMaxWidth(Double.MAX_VALUE);
        f.setStyle(
            "-fx-control-inner-background: " + (isLight ? "#f9fafb" : "#111128") + ";" +
            "-fx-text-fill: " + (isLight ? "#111827" : "#e2e8f0") + ";" +
            "-fx-font-size: 13px; -fx-background-color: " + (isLight ? "#f9fafb" : "#111128") + ";" +
            "-fx-border-color: " + (isLight ? "rgba(124,58,237,0.2)" : "rgba(255,255,255,0.1)") + ";" +
            "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 10 12 10 12;");
        return f;
    }

    private TextArea styledTextArea(String prompt, String value, int rows, String id) {
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        TextArea a = new TextArea(value);
        a.setId(id); a.setPromptText(prompt); a.setWrapText(true);
        a.setPrefRowCount(rows); a.setMaxWidth(Double.MAX_VALUE);
        a.setStyle(
            "-fx-control-inner-background: " + (isLight ? "#f9fafb" : "#111128") + ";" +
            "-fx-text-fill: " + (isLight ? "#111827" : "#e2e8f0") + ";" +
            "-fx-font-size: 13px; -fx-background-color: " + (isLight ? "#f9fafb" : "#111128") + ";" +
            "-fx-border-color: " + (isLight ? "rgba(124,58,237,0.2)" : "rgba(255,255,255,0.1)") + ";" +
            "-fx-border-radius: 10; -fx-background-radius: 10;");
        return a;
    }

    private ScrollPane scrollPane(javafx.scene.Node content, double height) {
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true); sp.setPrefHeight(height);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background: " + (isLight ? "#ffffff" : "#0c0c1e") +
                    "; -fx-background-color: " + (isLight ? "#ffffff" : "#0c0c1e") + ";");
        return sp;
    }
}
