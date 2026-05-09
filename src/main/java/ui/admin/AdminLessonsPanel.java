package ui.admin;

import db.LessonDAO;
import game.ThemeManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import model.Lesson;

import java.util.List;

/**
 * Admin Lessons Panel - Manage training lessons (add, edit, delete).
 */
public class AdminLessonsPanel {

    private TableView<Lesson> lessonTable;
    private ObservableList<Lesson> lessonList;
    private String currentFilter = "All";

    public ScrollPane buildContent() {
        VBox content = new VBox(20);
        content.setStyle("-fx-padding: 36 40 36 40;");
        content.getStyleClass().add("admin-lessons-panel");

        Label title = new Label("Lesson Management");
        title.getStyleClass().add("label-section");

        Label subtitle = new Label("Create, edit, and organize training lessons by difficulty and access level");
        subtitle.getStyleClass().add("label-muted");

        HBox filterBar = buildFilterBar();
        lessonTable = buildLessonTable();
        HBox actionButtons = buildActionButtons();

        content.getChildren().addAll(title, subtitle, filterBar, lessonTable, actionButtons);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-dark");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        loadLessons();
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
                loadLessons();
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

        Button addBtn = new Button("➕  Add Lesson");
        addBtn.getStyleClass().add("btn-success");
        addBtn.setOnAction(e -> showAddDialog());

        bar.getChildren().addAll(spacer, addBtn);
        return bar;
    }

    // ── Table ─────────────────────────────────────────────────────────────

    private TableView<Lesson> buildLessonTable() {
        TableView<Lesson> table = new TableView<>();
        table.getStyleClass().add("table-dark");
        table.setPrefHeight(420);

        TableColumn<Lesson, Integer> numCol = new TableColumn<>("#");
        numCol.setCellValueFactory(new PropertyValueFactory<>("lessonNumber"));
        numCol.setPrefWidth(50);

        TableColumn<Lesson, String> idCol = new TableColumn<>("Lesson ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("lessonId"));
        idCol.setPrefWidth(110);

        TableColumn<Lesson, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(200);

        TableColumn<Lesson, String> levelCol = new TableColumn<>("Level");
        levelCol.setCellValueFactory(new PropertyValueFactory<>("level"));
        levelCol.setPrefWidth(110);
        levelCol.setCellFactory(col -> new TableCell<>() {
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

        TableColumn<Lesson, Boolean> premCol = new TableColumn<>("Access");
        premCol.setCellValueFactory(new PropertyValueFactory<>("premium"));
        premCol.setPrefWidth(90);
        premCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item ? "💎 Premium" : "🆓 Free");
                setStyle(item ? "-fx-text-fill: #fbbf24;" : "-fx-text-fill: #10b981;");
            }
        });

        TableColumn<Lesson, Integer> wpmCol = new TableColumn<>("Target WPM");
        wpmCol.setCellValueFactory(new PropertyValueFactory<>("targetWpm"));
        wpmCol.setPrefWidth(100);
        wpmCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item + " WPM");
                setStyle("-fx-text-fill: #38bdf8;");
            }
        });

        table.getColumns().addAll(numCol, idCol, titleCol, levelCol, premCol, wpmCol);
        return table;
    }

    // ── Action Buttons ────────────────────────────────────────────────────

    private HBox buildActionButtons() {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);

        Button viewBtn = new Button("👁  View Content");
        viewBtn.getStyleClass().add("btn-primary");
        viewBtn.setOnAction(e -> viewLessonContent());

        Button editBtn = new Button("✏️  Edit Lesson");
        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setOnAction(e -> showEditDialog());

        Button deleteBtn = new Button("🗑  Delete Lesson");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> deleteLesson());

        box.getChildren().addAll(viewBtn, editBtn, deleteBtn);
        return box;
    }

    // ── Data Loading ──────────────────────────────────────────────────────

    private void loadLessons() {
        LessonDAO dao = LessonDAO.getInstance();
        List<Lesson> items = "All".equals(currentFilter)
                ? dao.getAll() : dao.getByLevel(currentFilter);
        lessonList = FXCollections.observableArrayList(items);
        lessonTable.setItems(lessonList);
    }

    // ── View Content ──────────────────────────────────────────────────────

    private void viewLessonContent() {
        Lesson sel = lessonTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AdminDialogs.showInfo("No Selection", "Please select a lesson to view.");
            return;
        }

        Stage stage = AdminDialogs.buildStage("Lesson Content");

        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + (isLight ? "#ffffff" : "#0c0c1e") + ";");
        root.setPrefWidth(520);

        // Header
        VBox header = new VBox(6);
        header.setStyle(
            "-fx-background-color: " + (isLight ? "linear-gradient(to right, #f0e6ff, #ffffff)" : "linear-gradient(to right, #1e1040, #0c0c1e)") + ";" +
            "-fx-padding: 24 28 18 28;" +
            "-fx-border-color: transparent transparent " + (isLight ? "rgba(124,58,237,0.2)" : "rgba(124,58,237,0.3)") + " transparent;" +
            "-fx-border-width: 0 0 1 0;");
        Label htitle = new Label("📖  " + sel.getTitle());
        htitle.setStyle("-fx-text-fill: " + (isLight ? "#111827" : "white") + "; -fx-font-size: 17px; -fx-font-weight: bold;");
        String lc = switch (sel.getLevel()) {
            case "Beginner" -> "#10b981"; case "Intermediate" -> "#fbbf24"; default -> "#ef4444";
        };
        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_LEFT);
        Label lvlBadge = badge(sel.getLevel(), lc);
        Label accBadge = sel.isPremium() ? badge("💎 Premium", "#fbbf24") : badge("🆓 Free", "#10b981");
        Label wpmBadge = badge("⚡ " + sel.getTargetWpm() + " WPM", "#38bdf8");
        badges.getChildren().addAll(lvlBadge, accBadge, wpmBadge);
        header.getChildren().addAll(htitle, badges);

        // Body
        VBox body = new VBox(18);
        body.setStyle("-fx-padding: 24 28 24 28; -fx-background-color: " + (isLight ? "#ffffff" : "#0c0c1e") + ";");

        body.getChildren().add(AdminDialogs.sectionHeader("📝", "Typing Passage", "#a78bfa"));
        Label contentText = new Label(sel.getContent().isBlank() ? "(no content)" : sel.getContent());
        contentText.setStyle(
            "-fx-text-fill: " + (isLight ? "#111827" : "#e2e8f0") + "; -fx-font-size: 13px; -fx-line-spacing: 4;" +
            "-fx-background-color: " + (isLight ? "#f3f4f6" : "#111128") + "; -fx-padding: 14 16 14 16;" +
            "-fx-background-radius: 10;");
        contentText.setWrapText(true);
        contentText.setMaxWidth(Double.MAX_VALUE);
        body.getChildren().add(contentText);

        body.getChildren().add(AdminDialogs.sectionHeader("🖐", "Finger Hint / Guidance", "#38bdf8"));
        Label hintText = new Label(sel.getFingerHint().isBlank() ? "(no hint provided)" : sel.getFingerHint());
        hintText.setStyle(
            "-fx-text-fill: " + (isLight ? "#374151" : "#94a3b8") + "; -fx-font-size: 13px; -fx-line-spacing: 3;" +
            "-fx-background-color: " + (isLight ? "rgba(59,130,246,0.06)" : "rgba(56,189,248,0.05)") + "; -fx-padding: 12 16 12 16;" +
            "-fx-background-radius: 10; -fx-border-color: rgba(56,189,248,0.15);" +
            "-fx-border-radius: 10; -fx-border-width: 1;");
        hintText.setWrapText(true);
        hintText.setMaxWidth(Double.MAX_VALUE);
        body.getChildren().add(hintText);

        ScrollPane sp = new ScrollPane(body);
        sp.setFitToWidth(true);
        sp.setPrefHeight(360);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background: " + (isLight ? "#ffffff" : "#0c0c1e") + "; -fx-background-color: " + (isLight ? "#ffffff" : "#0c0c1e") + ";");

        HBox btns = new HBox();
        btns.setAlignment(Pos.CENTER_RIGHT);
        btns.setStyle("-fx-padding: 14 28 20 28; -fx-border-color: rgba(255,255,255,0.06) transparent transparent transparent; -fx-border-width: 1 0 0 0;");
        Button close = AdminDialogs.filledBtn("Close", "#7c3aed", "#6d28d9", "#7c3aed66");
        close.setOnAction(e -> stage.close());
        btns.getChildren().add(close);

        root.getChildren().addAll(header, sp, btns);
        AdminDialogs.showModal(stage, root);
    }

    // ── Add Dialog ────────────────────────────────────────────────────────

    private void showAddDialog() {
        Stage stage = AdminDialogs.buildStage("Add New Lesson");

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + (ThemeManager.getInstance().isPrintLightTheme() ? "#ffffff" : "#0c0c1e") + ";");
        root.setPrefWidth(580);

        VBox header = buildFormHeader("➕  Add New Lesson",
                "Fill in the details to create a new training lesson", null);

        VBox form = buildLessonForm(null);
        ScrollPane sp = scrollPane(form, 460);

        HBox btns = new HBox(12);
        btns.setAlignment(Pos.CENTER_RIGHT);
        btns.setStyle("-fx-padding: 16 28 24 28;" +
            "-fx-border-color: rgba(255,255,255,0.06) transparent transparent transparent;" +
            "-fx-border-width: 1 0 0 0;");
        Button cancelBtn = AdminDialogs.ghostBtn("Cancel");
        Button saveBtn   = AdminDialogs.filledBtn("Save Lesson", "#7c3aed", "#6d28d9", "#7c3aed66");
        btns.getChildren().addAll(cancelBtn, saveBtn);

        root.getChildren().addAll(header, sp, btns);

        cancelBtn.setOnAction(e -> stage.close());
        saveBtn.setOnAction(e -> {
            Lesson lesson = extractLesson(form, null);
            if (lesson == null) {
                AdminDialogs.showError("Missing Fields", "Lesson ID and Title cannot be empty.");
                return;
            }
            LessonDAO.getInstance().saveNew(lesson);
            loadLessons();
            stage.close();
            AdminDialogs.showSuccess("Lesson Added",
                "\"" + lesson.getTitle() + "\" has been created successfully.");
        });

        AdminDialogs.showModal(stage, root);
    }

    // ── Edit Dialog ───────────────────────────────────────────────────────

    private void showEditDialog() {
        Lesson sel = lessonTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AdminDialogs.showInfo("No Selection", "Please select a lesson to edit.");
            return;
        }

        Stage stage = AdminDialogs.buildStage("Edit Lesson");

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + (ThemeManager.getInstance().isPrintLightTheme() ? "#ffffff" : "#0c0c1e") + ";");
        root.setPrefWidth(580);

        VBox header = buildFormHeader("✏️  Edit Lesson", sel.getTitle(), sel);

        VBox form = buildLessonForm(sel);
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
            Lesson lesson = extractLesson(form, sel);
            if (lesson == null) {
                AdminDialogs.showError("Missing Fields", "Lesson ID and Title cannot be empty.");
                return;
            }
            LessonDAO.getInstance().updateLesson(lesson);
            loadLessons();
            stage.close();
            AdminDialogs.showSuccess("Lesson Updated",
                "\"" + lesson.getTitle() + "\" has been saved.");
        });

        AdminDialogs.showModal(stage, root);
    }

    // ── Delete ────────────────────────────────────────────────────────────

    private void deleteLesson() {
        Lesson sel = lessonTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AdminDialogs.showInfo("No Selection", "Please select a lesson to delete.");
            return;
        }
        boolean ok = AdminDialogs.showConfirm(
            "Delete Lesson",
            "Are you sure you want to delete this lesson?",
            "📖  " + sel.getTitle() + "  •  " + sel.getLevel(),
            "Delete", true
        );
        if (ok) {
            LessonDAO.getInstance().deleteLesson(sel.getLessonId());
            loadLessons();
            AdminDialogs.showSuccess("Lesson Deleted",
                "\"" + sel.getTitle() + "\" has been removed.");
        }
    }

    // ── Form Builder ──────────────────────────────────────────────────────

    private VBox buildFormHeader(String title, String subtitle, Lesson lesson) {
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        VBox header = new VBox(6);
        header.setStyle(
            "-fx-background-color: " + (isLight ? "linear-gradient(to right, #f0e6ff, #ffffff)" : "linear-gradient(to right, #1e1040, #0c0c1e)") + ";" +
            "-fx-padding: 24 28 18 28;" +
            "-fx-border-color: transparent transparent " + (isLight ? "rgba(124,58,237,0.2)" : "rgba(124,58,237,0.3)") + " transparent;" +
            "-fx-border-width: 0 0 1 0;");

        if (lesson != null) {
            HBox badges = new HBox(8);
            badges.setAlignment(Pos.CENTER_LEFT);
            String lc = switch (lesson.getLevel()) {
                case "Beginner" -> "#10b981"; case "Intermediate" -> "#fbbf24"; default -> "#ef4444";
            };
            badges.getChildren().addAll(
                badge(lesson.getLevel(), lc),
                lesson.isPremium() ? badge("💎 Premium", "#fbbf24") : badge("🆓 Free", "#10b981")
            );
            header.getChildren().add(badges);
        }

        Label htitle = new Label(title);
        htitle.setStyle("-fx-text-fill: " + (isLight ? "#111827" : "white") + "; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label hsub = new Label(subtitle);
        hsub.setStyle("-fx-text-fill: " + (isLight ? "#6b7280" : "#475569") + "; -fx-font-size: 12px;");
        header.getChildren().addAll(htitle, hsub);
        return header;
    }

    private VBox buildLessonForm(Lesson ex) {
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        VBox form = new VBox(18);
        form.setStyle("-fx-padding: 24 28 12 28; -fx-background-color: " + (isLight ? "#ffffff" : "#0c0c1e") + ";");

        // ── Section: Basic Info ───────────────────────────────────────────
        form.getChildren().add(AdminDialogs.sectionHeader("📋", "Basic Information", "#a78bfa"));

        HBox row1 = new HBox(16);
        VBox idBox = new VBox(6);
        idBox.getChildren().add(AdminDialogs.formLabel("LESSON ID"));
        TextField idField = styledField("lesson_0XX", ex != null ? ex.getLessonId() : "", "lessonIdField");
        idBox.getChildren().add(idField);
        HBox.setHgrow(idBox, Priority.ALWAYS);

        VBox numBox = new VBox(6);
        numBox.getChildren().add(AdminDialogs.formLabel("ORDER #"));
        TextField numField = styledField("1", ex != null ? String.valueOf(ex.getLessonNumber()) : "1", "lessonNumField");
        numBox.setMaxWidth(100);
        numBox.getChildren().add(numField);
        row1.getChildren().addAll(idBox, numBox);

        VBox titleBox = new VBox(6);
        titleBox.getChildren().add(AdminDialogs.formLabel("LESSON TITLE"));
        TextField titleField = styledField("e.g. Home Row Keys", ex != null ? ex.getTitle() : "", "titleField");
        titleField.setMaxWidth(Double.MAX_VALUE);
        titleBox.getChildren().add(titleField);

        form.getChildren().addAll(row1, titleBox);

        // ── Section: Difficulty ───────────────────────────────────────────
        form.getChildren().add(AdminDialogs.sectionHeader("🎯", "Difficulty & Target", "#38bdf8"));

        HBox row2 = new HBox(16);
        VBox levelBox = new VBox(6);
        levelBox.getChildren().add(AdminDialogs.formLabel("DIFFICULTY LEVEL"));

        String[]   levels      = {"Beginner", "Intermediate", "Advanced"};
        String[]   levelColors = {"#10b981",  "#fbbf24",       "#ef4444"};
        String[]   selLevel    = {ex != null ? ex.getLevel() : "Beginner"};
        Button[]   levelBtns   = new Button[3];
        HBox       levelBtnRow = new HBox(8);

        for (int i = 0; i < levels.length; i++) {
            final int idx = i;
            Button lb = new Button(levels[i]);
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
        levelBox.getChildren().add(levelBtnRow);
        HBox.setHgrow(levelBox, Priority.ALWAYS);

        VBox wpmBox = new VBox(6);
        wpmBox.getChildren().add(AdminDialogs.formLabel("TARGET WPM"));
        TextField wpmField = styledField("30", ex != null ? String.valueOf(ex.getTargetWpm()) : "30", "targetWpmField");
        wpmBox.setMaxWidth(120);
        wpmBox.getChildren().add(wpmField);
        row2.getChildren().addAll(levelBox, wpmBox);
        form.getChildren().add(row2);

        // ── Section: Access Control ───────────────────────────────────────
        form.getChildren().add(AdminDialogs.sectionHeader("🔐", "Access Control", "#fbbf24"));

        boolean[] isPrem = {ex != null && ex.isPremium()};
        Button freeBtn   = new Button("🆓  Free Access");
        Button premBtn   = new Button("💎  Premium Only");
        freeBtn.setStyle(accessBtnStyle("#10b981", !isPrem[0]));
        premBtn.setStyle(accessBtnStyle("#fbbf24",  isPrem[0]));
        freeBtn.setOnAction(e -> {
            isPrem[0] = false;
            freeBtn.setStyle(accessBtnStyle("#10b981", true));
            premBtn.setStyle(accessBtnStyle("#fbbf24", false));
        });
        premBtn.setOnAction(e -> {
            isPrem[0] = true;
            freeBtn.setStyle(accessBtnStyle("#10b981", false));
            premBtn.setStyle(accessBtnStyle("#fbbf24", true));
        });
        HBox accessRow = new HBox(12, freeBtn, premBtn);
        form.getChildren().add(accessRow);

        // ── Section: Content ──────────────────────────────────────────────
        form.getChildren().add(AdminDialogs.sectionHeader("📝", "Lesson Content", "#34d399"));

        VBox contentBox = new VBox(6);
        contentBox.getChildren().add(AdminDialogs.formLabel("TYPING PASSAGE"));
        TextArea contentArea = styledTextArea(
            "Enter the text the student will type...",
            ex != null ? ex.getContent() : "", 5, "contentArea");
        contentBox.getChildren().add(contentArea);
        form.getChildren().add(contentBox);

        VBox hintBox = new VBox(6);
        hintBox.getChildren().add(AdminDialogs.formLabel("FINGER HINT / GUIDANCE"));
        TextArea hintArea = styledTextArea(
            "Keyboard posture or finger placement tips...",
            ex != null ? ex.getFingerHint() : "", 3, "hintArea");
        hintBox.getChildren().add(hintArea);
        form.getChildren().add(hintBox);

        // Store mutable state for extraction
        form.setUserData(new Object[]{selLevel, isPrem});
        return form;
    }

    private Lesson extractLesson(VBox form, Lesson existing) {
        TextField idField    = (TextField) form.lookup("#lessonIdField");
        TextField titleField = (TextField) form.lookup("#titleField");
        TextField numField   = (TextField) form.lookup("#lessonNumField");
        TextField wpmField   = (TextField) form.lookup("#targetWpmField");
        TextArea  content    = (TextArea)  form.lookup("#contentArea");
        TextArea  hint       = (TextArea)  form.lookup("#hintArea");

        if (idField.getText().isBlank() || titleField.getText().isBlank()) return null;

        Object[] state  = (Object[]) form.getUserData();
        String[] lvl    = (String[])  state[0];
        boolean[] prem  = (boolean[]) state[1];

        Lesson lesson = existing != null ? existing : new Lesson();
        lesson.setLessonId(idField.getText().trim());
        lesson.setTitle(titleField.getText().trim());
        lesson.setLevel(lvl[0]);
        lesson.setContent(content.getText().trim());
        lesson.setFingerHint(hint.getText().trim());
        lesson.setPremium(prem[0]);
        try   { lesson.setLessonNumber(Integer.parseInt(numField.getText().trim())); }
        catch (NumberFormatException e) { lesson.setLessonNumber(1); }
        try   { lesson.setTargetWpm(Integer.parseInt(wpmField.getText().trim())); }
        catch (NumberFormatException e) { lesson.setTargetWpm(30); }
        return lesson;
    }

    // ── Style helpers ─────────────────────────────────────────────────────

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

    private Label badge(String text, String color) {
        Label l = new Label(text);
        l.setStyle(
            "-fx-background-color: " + color + "22; -fx-text-fill: " + color + ";" +
            "-fx-font-size: 10px; -fx-font-weight: bold;" +
            "-fx-padding: 3 8 3 8; -fx-background-radius: 6;");
        return l;
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
