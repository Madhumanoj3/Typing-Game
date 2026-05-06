package ui.admin;

import db.ContentDAO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import model.Content;

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
        Dialog<Content> dialog = new Dialog<>();
        dialog.setTitle("Add Content");
        dialog.setHeaderText("Add New Content");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("WORD", "SENTENCE");
        typeCombo.setValue("WORD");

        TextField textField = new TextField();
        textField.setPromptText("Enter text");

        ComboBox<String> difficultyCombo = new ComboBox<>();
        difficultyCombo.getItems().addAll("EASY", "MEDIUM", "HARD");
        difficultyCombo.setValue("EASY");

        TextField categoryField = new TextField();
        categoryField.setPromptText("Category");

        grid.add(new Label("Type:"), 0, 0);
        grid.add(typeCombo, 1, 0);
        grid.add(new Label("Text:"), 0, 1);
        grid.add(textField, 1, 1);
        grid.add(new Label("Difficulty:"), 0, 2);
        grid.add(difficultyCombo, 1, 2);
        grid.add(new Label("Category:"), 0, 3);
        grid.add(categoryField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                Content content = new Content(
                    typeCombo.getValue(),
                    textField.getText(),
                    difficultyCombo.getValue(),
                    categoryField.getText(),
                    true
                );
                return content;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(content -> {
            contentDAO.create(content);
            loadContent();
        });
    }

    private void showEditDialog(Content content) {
        Dialog<Content> dialog = new Dialog<>();
        dialog.setTitle("Edit Content");
        dialog.setHeaderText("Edit Content");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("WORD", "SENTENCE");
        typeCombo.setValue(content.getType());

        TextField textField = new TextField(content.getText());

        ComboBox<String> difficultyCombo = new ComboBox<>();
        difficultyCombo.getItems().addAll("EASY", "MEDIUM", "HARD");
        difficultyCombo.setValue(content.getDifficulty());

        TextField categoryField = new TextField(content.getCategory());

        grid.add(new Label("Type:"), 0, 0);
        grid.add(typeCombo, 1, 0);
        grid.add(new Label("Text:"), 0, 1);
        grid.add(textField, 1, 1);
        grid.add(new Label("Difficulty:"), 0, 2);
        grid.add(difficultyCombo, 1, 2);
        grid.add(new Label("Category:"), 0, 3);
        grid.add(categoryField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                content.setType(typeCombo.getValue());
                content.setText(textField.getText());
                content.setDifficulty(difficultyCombo.getValue());
                content.setCategory(categoryField.getText());
                return content;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedContent -> {
            contentDAO.update(updatedContent);
            loadContent();
        });
    }
}
