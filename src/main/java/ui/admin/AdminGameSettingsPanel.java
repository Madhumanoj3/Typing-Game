package ui.admin;

import db.GameConfigDAO;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.GameConfig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin Game Settings Panel - Configure game modes, timers, and difficulty settings.
 */
public class AdminGameSettingsPanel {

    private VBox cardsContainer;

    public ScrollPane buildContent() {
        VBox content = new VBox(24);
        content.setStyle("-fx-padding: 36 40 36 40;");

        Label title = new Label("Game Configuration");
        title.getStyleClass().add("label-section");

        Label subtitle = new Label("Configure game modes, timer durations, and difficulty settings");
        subtitle.getStyleClass().add("label-muted");

        cardsContainer = new VBox(20);
        loadGameModes();

        content.getChildren().addAll(title, subtitle, cardsContainer);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-dark");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private void loadGameModes() {
        cardsContainer.getChildren().clear();
        List<GameConfig> configs = GameConfigDAO.getInstance().getAll();
        for (GameConfig config : configs) {
            cardsContainer.getChildren().add(buildModeCard(config));
        }
    }

    // ── Mode Card ─────────────────────────────────────────────────────────

    private VBox buildModeCard(GameConfig config) {
        VBox card = new VBox(16);
        card.setStyle(
            "-fx-background-color: #1a1a2e;" +
            "-fx-background-radius: 16;" +
            "-fx-padding: 24;");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        String icon = switch (config.getModeName()) {
            case "Practice" -> "📝";
            case "Normal"   -> "🎮";
            case "Timer"    -> "⏱️";
            default         -> "🎯";
        };

        Label modeIcon = new Label(icon);
        modeIcon.setStyle("-fx-font-size: 28px;");

        VBox headerText = new VBox(2);
        Label modeName = new Label(config.getModeName());
        modeName.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label statusBadge = new Label(config.isEnabled() ? "● ENABLED" : "● DISABLED");
        statusBadge.setStyle(config.isEnabled()
                ? "-fx-text-fill: #10b981; -fx-font-size: 12px; -fx-font-weight: bold;"
                : "-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-font-weight: bold;");
        headerText.getChildren().addAll(modeName, statusBadge);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button toggleBtn = new Button(config.isEnabled() ? "Disable" : "Enable");
        toggleBtn.getStyleClass().add(config.isEnabled() ? "btn-danger" : "btn-success");
        toggleBtn.setOnAction(e -> {
            config.setEnabled(!config.isEnabled());
            GameConfigDAO.getInstance().update(config);
            loadGameModes();
        });

        Button editBtn = new Button("✏️ Edit");
        editBtn.getStyleClass().add("btn-primary");
        editBtn.setOnAction(e -> showEditDialog(config));

        header.getChildren().addAll(modeIcon, headerText, spacer, editBtn, toggleBtn);

        // Description
        Label desc = new Label(config.getDescription());
        desc.getStyleClass().add("label-body");
        desc.setWrapText(true);

        // Duration display (for Timer mode mainly)
        HBox durationsBox = new HBox(16);
        durationsBox.setAlignment(Pos.CENTER_LEFT);

        Map<String, Integer> durations = config.getDurations();
        if (durations != null && !durations.isEmpty()) {
            for (Map.Entry<String, Integer> entry : durations.entrySet()) {
                VBox durCard = new VBox(4);
                durCard.setAlignment(Pos.CENTER);
                durCard.setStyle(
                    "-fx-background-color: #16213e;" +
                    "-fx-background-radius: 10;" +
                    "-fx-padding: 12 20 12 20;" +
                    "-fx-min-width: 120;");

                Label diffLabel = new Label(entry.getKey());
                diffLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

                Label valLabel = new Label(entry.getValue() == 0 ? "∞ Unlimited" : entry.getValue() + "s");
                String color = switch (entry.getKey()) {
                    case "Easy"   -> "#10b981";
                    case "Medium" -> "#f59e0b";
                    case "Hard"   -> "#ef4444";
                    default       -> "#7c3aed";
                };
                valLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 18px; -fx-font-weight: bold;");

                durCard.getChildren().addAll(diffLabel, valLabel);
                durationsBox.getChildren().add(durCard);
            }
        }

        card.getChildren().addAll(header, desc, durationsBox);
        return card;
    }

    // ── Edit Dialog ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void showEditDialog(GameConfig config) {
        Dialog<GameConfig> dialog = new Dialog<>();
        dialog.setTitle("Edit Game Mode: " + config.getModeName());
        dialog.setHeaderText("Configure " + config.getModeName() + " mode settings");

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        int row = 0;

        // Description
        Label descLabel = new Label("Description:");
        descLabel.getStyleClass().add("label-body");
        TextArea descArea = new TextArea(config.getDescription());
        descArea.setId("descArea");
        descArea.getStyleClass().add("field-dark");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);
        descArea.setPrefWidth(400);
        grid.add(descLabel, 0, row);
        grid.add(descArea, 1, row++);

        // Duration fields for each difficulty
        Map<String, TextField> durationFields = new LinkedHashMap<>();
        Map<String, Integer> durations = config.getDurations();
        if (durations != null) {
            for (Map.Entry<String, Integer> entry : durations.entrySet()) {
                Label dLabel = new Label(entry.getKey() + " Duration (seconds):");
                dLabel.getStyleClass().add("label-body");
                TextField dField = new TextField(String.valueOf(entry.getValue()));
                dField.getStyleClass().add("field-dark");
                dField.setPrefWidth(200);
                grid.add(dLabel, 0, row);
                grid.add(dField, 1, row++);
                durationFields.put(entry.getKey(), dField);
            }
        }

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setStyle("-fx-background-color: #1a1a2e;");

        dialog.setResultConverter(bt -> {
            if (bt == saveType) {
                config.setDescription(descArea.getText().trim());
                Map<String, Integer> newDurations = new LinkedHashMap<>();
                for (Map.Entry<String, TextField> entry : durationFields.entrySet()) {
                    try {
                        newDurations.put(entry.getKey(),
                                Integer.parseInt(entry.getValue().getText().trim()));
                    } catch (NumberFormatException ex) {
                        newDurations.put(entry.getKey(), 0);
                    }
                }
                config.setDurations(newDurations);
                return config;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(gc -> {
            GameConfigDAO.getInstance().update(gc);
            loadGameModes();
        });
    }
}
