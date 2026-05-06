package ui.admin;

import db.ContentDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import model.TypingContent;

import java.util.List;

/**
 * Admin Content Panel - Manage typing content (words & paragraphs).
 */
public class AdminContentPanel {

    private TableView<TypingContent> contentTable;
    private ObservableList<TypingContent> contentList;
    private String currentFilter = "All";

    public ScrollPane buildContent() {
        VBox content = new VBox(20);
        content.setStyle("-fx-padding: 36 40 36 40;");

        Label title = new Label("Content Management");
        title.getStyleClass().add("label-section");

        Label subtitle = new Label("Manage typing words and paragraphs used in game modes");
        subtitle.getStyleClass().add("label-muted");

        HBox filterBar = buildFilterBar();
        contentTable = buildContentTable();
        HBox actionButtons = buildActionButtons();

        content.getChildren().addAll(title, subtitle, filterBar, contentTable, actionButtons);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-dark");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        loadContent();
        return scroll;
    }

    // ── Filter Bar ────────────────────────────────────────────────────────

    private HBox buildFilterBar() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);

        String[] filters = {"All", "Beginner", "Intermediate", "Advanced"};
        for (String f : filters) {
            Button btn = new Button(f);
            btn.getStyleClass().add(f.equals(currentFilter) ? "diff-btn-selected" : "diff-btn");
            btn.setOnAction(e -> {
                currentFilter = f;
                loadContent();
                bar.getChildren().forEach(node -> {
                    if (node instanceof Button b) {
                        b.getStyleClass().setAll(
                            b.getText().equals(currentFilter) ? "diff-btn-selected" : "diff-btn"
                        );
                    }
                });
            });
            bar.getChildren().add(btn);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("➕  Add Content");
        addBtn.getStyleClass().add("btn-success");
        addBtn.setOnAction(e -> showAddDialog());

        bar.getChildren().addAll(spacer, addBtn);
        return bar;
    }

    // ── Table ─────────────────────────────────────────────────────────────

    private TableView<TypingContent> buildContentTable() {
        TableView<TypingContent> table = new TableView<>();
        table.getStyleClass().add("table-dark");
        table.setPrefHeight(420);

        TableColumn<TypingContent, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(100);
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("WORD".equals(item)
                    ? "-fx-text-fill: #38bdf8; -fx-font-weight: bold;"
                    : "-fx-text-fill: #a78bfa; -fx-font-weight: bold;");
            }
        });

        TableColumn<TypingContent, String> diffCol = new TableColumn<>("Difficulty");
        diffCol.setCellValueFactory(new PropertyValueFactory<>("difficulty"));
        diffCol.setPrefWidth(120);
        diffCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String color = switch (item) {
                    case "Beginner"     -> "#10b981";
                    case "Intermediate" -> "#fbbf24";
                    default             -> "#ef4444";
                };
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
            }
        });

        TableColumn<TypingContent, String> textCol = new TableColumn<>("Text");
        textCol.setCellValueFactory(new PropertyValueFactory<>("text"));
        textCol.setPrefWidth(460);

        table.getColumns().add(typeCol);
        table.getColumns().add(diffCol);
        table.getColumns().add(textCol);
        return table;
    }

    // ── Action Buttons ────────────────────────────────────────────────────

    private HBox buildActionButtons() {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);

        Button editBtn = new Button("✏️  Edit Selected");
        editBtn.getStyleClass().add("btn-primary");
        editBtn.setOnAction(e -> showEditDialog());

        Button deleteBtn = new Button("🗑  Delete Selected");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> deleteContent());

        Button refreshBtn = new Button("🔄  Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> loadContent());

        box.getChildren().addAll(editBtn, deleteBtn, refreshBtn);
        return box;
    }

    // ── Data Loading ──────────────────────────────────────────────────────

    private void loadContent() {
        ContentDAO dao = ContentDAO.getInstance();
        List<TypingContent> items = "All".equals(currentFilter)
                ? dao.getAll() : dao.getByDifficulty(currentFilter);
        contentList = FXCollections.observableArrayList(items);
        contentTable.setItems(contentList);
    }

    // ── Add Dialog ────────────────────────────────────────────────────────

    private void showAddDialog() {
        Stage stage = AdminDialogs.buildStage("Add New Content");

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #0c0c1e;");
        root.setPrefWidth(560);

        VBox header = buildDialogHeader("➕  Add New Content",
                "Choose content type, difficulty, and enter the text");

        VBox form = buildContentForm(null);
        ScrollPane sp = scrollPane(form, 420);

        HBox btns = footerBtns();
        Button cancelBtn = AdminDialogs.ghostBtn("Cancel");
        Button saveBtn   = AdminDialogs.filledBtn("Save Content", "#7c3aed", "#6d28d9", "#7c3aed66");
        btns.getChildren().addAll(cancelBtn, saveBtn);

        root.getChildren().addAll(header, sp, btns);

        cancelBtn.setOnAction(e -> stage.close());
        saveBtn.setOnAction(e -> {
            TypingContent tc = extractContent(form, null);
            if (tc == null) {
                AdminDialogs.showError("Missing Fields", "Please enter text and select type and difficulty.");
                return;
            }
            ContentDAO.getInstance().save(tc);
            loadContent();
            stage.close();
            AdminDialogs.showSuccess("Content Added",
                tc.getType() + " content has been added successfully.");
        });

        AdminDialogs.showModal(stage, root);
    }

    // ── Edit Dialog ───────────────────────────────────────────────────────

    private void showEditDialog() {
        TypingContent sel = contentTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AdminDialogs.showInfo("No Selection", "Please select a content item to edit.");
            return;
        }

        Stage stage = AdminDialogs.buildStage("Edit Content");

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #0c0c1e;");
        root.setPrefWidth(560);

        VBox header = buildDialogHeader("✏️  Edit Content",
                "Update the type, difficulty, or text of this entry");

        VBox form = buildContentForm(sel);
        ScrollPane sp = scrollPane(form, 420);

        HBox btns = footerBtns();
        Button cancelBtn = AdminDialogs.ghostBtn("Cancel");
        Button saveBtn   = AdminDialogs.filledBtn("Save Changes", "#7c3aed", "#6d28d9", "#7c3aed66");
        btns.getChildren().addAll(cancelBtn, saveBtn);

        root.getChildren().addAll(header, sp, btns);

        cancelBtn.setOnAction(e -> stage.close());
        saveBtn.setOnAction(e -> {
            TypingContent tc = extractContent(form, sel);
            if (tc == null) {
                AdminDialogs.showError("Missing Fields", "Text cannot be empty.");
                return;
            }
            ContentDAO.getInstance().update(tc);
            loadContent();
            stage.close();
            AdminDialogs.showSuccess("Content Updated", "The entry has been saved.");
        });

        AdminDialogs.showModal(stage, root);
    }

    // ── Delete ────────────────────────────────────────────────────────────

    private void deleteContent() {
        TypingContent sel = contentTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AdminDialogs.showInfo("No Selection", "Please select a content item to delete.");
            return;
        }
        String preview = sel.getText().length() > 55
                ? sel.getText().substring(0, 55) + "…"
                : sel.getText();
        boolean ok = AdminDialogs.showConfirm(
            "Delete Content",
            "Are you sure you want to delete this " + sel.getType().toLowerCase() + "?",
            preview,
            "Delete", true
        );
        if (ok) {
            ContentDAO.getInstance().delete(sel.getId());
            loadContent();
            AdminDialogs.showSuccess("Content Deleted", "The entry has been removed.");
        }
    }

    // ── Form Builder ──────────────────────────────────────────────────────

    private VBox buildContentForm(TypingContent ex) {
        VBox form = new VBox(18);
        form.setStyle("-fx-padding: 24 28 12 28; -fx-background-color: #0c0c1e;");

        // ── Section: Content Type ─────────────────────────────────────────
        form.getChildren().add(AdminDialogs.sectionHeader("📂", "Content Type", "#38bdf8"));

        String[]  types      = {"WORD", "PARAGRAPH"};
        String[]  typeColors = {"#38bdf8", "#a78bfa"};
        String[]  typeIcons  = {"🔤", "📄"};
        String[]  typeDescs  = {"Single word for rapid-fire mode", "Full passage for practice sessions"};
        String[]  selType    = {ex != null ? ex.getType() : "WORD"};

        HBox typeRow = new HBox(12);
        Button[] typeBtns = new Button[2];

        for (int i = 0; i < types.length; i++) {
            final int idx = i;
            VBox card = new VBox(4);
            card.setAlignment(Pos.CENTER);
            card.setStyle(typeCardStyle(typeColors[i], types[i].equals(selType[0])));
            card.setPrefWidth(200);

            Label tIcon = new Label(typeIcons[i]);
            tIcon.setStyle("-fx-font-size: 22px;");
            Label tLabel = new Label(types[i]);
            tLabel.setStyle("-fx-text-fill: " + typeColors[i] + "; -fx-font-size: 14px; -fx-font-weight: bold;");
            Label tDesc = new Label(typeDescs[i]);
            tDesc.setStyle("-fx-text-fill: #475569; -fx-font-size: 10px;");
            tDesc.setWrapText(true);
            tDesc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            card.getChildren().addAll(tIcon, tLabel, tDesc);
            card.setId("typeCard_" + types[i]);

            // Wrap in a transparent button overlay
            Button tb = new Button();
            tb.setGraphic(card);
            tb.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-cursor: hand;");
            tb.setOnAction(e -> {
                selType[0] = types[idx];
                for (int j = 0; j < typeBtns.length; j++) {
                    VBox c = (VBox) typeBtns[j].getGraphic();
                    c.setStyle(typeCardStyle(typeColors[j], types[j].equals(selType[0])));
                }
            });
            typeBtns[i] = tb;
            typeRow.getChildren().add(tb);
        }
        form.getChildren().add(typeRow);

        // ── Section: Difficulty ───────────────────────────────────────────
        form.getChildren().add(AdminDialogs.sectionHeader("🎯", "Difficulty Level", "#fbbf24"));

        String[] diffs      = {"Beginner", "Intermediate", "Advanced"};
        String[] diffColors = {"#10b981",  "#fbbf24",       "#ef4444"};
        String[] selDiff    = {ex != null ? ex.getDifficulty() : "Beginner"};
        Button[] diffBtns   = new Button[3];
        HBox     diffRow    = new HBox(8);

        for (int i = 0; i < diffs.length; i++) {
            final int idx = i;
            Button db = new Button(diffs[i]);
            db.setStyle(levelBtnStyle(diffColors[i], diffs[i].equals(selDiff[0])));
            db.setOnAction(e -> {
                selDiff[0] = diffs[idx];
                for (int j = 0; j < diffBtns.length; j++) {
                    diffBtns[j].setStyle(levelBtnStyle(diffColors[j], diffs[j].equals(selDiff[0])));
                }
            });
            diffBtns[i] = db;
            diffRow.getChildren().add(db);
        }
        form.getChildren().add(diffRow);

        // ── Section: Text ─────────────────────────────────────────────────
        form.getChildren().add(AdminDialogs.sectionHeader("✍️", "Content Text", "#34d399"));

        VBox textBox = new VBox(6);
        textBox.getChildren().add(AdminDialogs.formLabel("TEXT CONTENT"));
        TextArea textArea = new TextArea(ex != null ? ex.getText() : "");
        textArea.setId("textArea");
        textArea.setPromptText("Enter word or paragraph content here...");
        textArea.setWrapText(true);
        textArea.setPrefRowCount(6);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setStyle(
            "-fx-control-inner-background: #111128; -fx-text-fill: #e2e8f0;" +
            "-fx-font-size: 13px; -fx-background-color: #111128;" +
            "-fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 10;" +
            "-fx-background-radius: 10;");
        textBox.getChildren().add(textArea);
        form.getChildren().add(textBox);

        form.setUserData(new Object[]{selType, selDiff});
        return form;
    }

    private TypingContent extractContent(VBox form, TypingContent existing) {
        TextArea textArea = (TextArea) form.lookup("#textArea");
        if (textArea == null || textArea.getText().isBlank()) return null;

        Object[] state   = (Object[]) form.getUserData();
        String[] selType = (String[]) state[0];
        String[] selDiff = (String[]) state[1];

        if (existing != null) {
            existing.setType(selType[0]);
            existing.setText(textArea.getText().trim());
            existing.setDifficulty(selDiff[0]);
            return existing;
        }
        return new TypingContent(selType[0], textArea.getText().trim(), selDiff[0]);
    }

    // ── Style helpers ─────────────────────────────────────────────────────

    private VBox buildDialogHeader(String title, String subtitle) {
        VBox header = new VBox(6);
        header.setStyle(
            "-fx-background-color: linear-gradient(to right, #1e1040, #0c0c1e);" +
            "-fx-padding: 24 28 18 28;" +
            "-fx-border-color: transparent transparent rgba(124,58,237,0.3) transparent;" +
            "-fx-border-width: 0 0 1 0;");
        Label htitle = new Label(title);
        htitle.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label hsub = new Label(subtitle);
        hsub.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px;");
        header.getChildren().addAll(htitle, hsub);
        return header;
    }

    private HBox footerBtns() {
        HBox btns = new HBox(12);
        btns.setAlignment(Pos.CENTER_RIGHT);
        btns.setStyle("-fx-padding: 16 28 24 28;" +
            "-fx-border-color: rgba(255,255,255,0.06) transparent transparent transparent;" +
            "-fx-border-width: 1 0 0 0;");
        return btns;
    }

    private String typeCardStyle(String color, boolean active) {
        return active
            ? "-fx-background-color: " + color + "22; -fx-padding: 16 24 16 24;" +
              "-fx-background-radius: 14; -fx-border-color: " + color + "77;" +
              "-fx-border-radius: 14; -fx-border-width: 1.5;"
            : "-fx-background-color: rgba(255,255,255,0.04); -fx-padding: 16 24 16 24;" +
              "-fx-background-radius: 14; -fx-border-color: rgba(255,255,255,0.1);" +
              "-fx-border-radius: 14; -fx-border-width: 1;";
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

    private ScrollPane scrollPane(javafx.scene.Node content, double height) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setPrefHeight(height);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background: #0c0c1e; -fx-background-color: #0c0c1e;");
        return sp;
    }
}
