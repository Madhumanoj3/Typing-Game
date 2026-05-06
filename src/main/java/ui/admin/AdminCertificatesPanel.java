package ui.admin;

import db.TrainingCertificateDAO;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import model.TrainingCertificate;
import model.TrainingProgress;
import service.TrainingService;
import util.SessionManager;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminCertificatesPanel {

    private final TrainingCertificateDAO certificateDAO = TrainingCertificateDAO.getInstance();
    private final TrainingService trainingService = TrainingService.getInstance();
    private TableView<TrainingCertificate> table;

    public ScrollPane buildContent() {
        VBox content = new VBox(20);
        content.setStyle("-fx-padding: 36 40 36 40;");

        Label title = new Label("Training Certificate Reviews");
        title.getStyleClass().add("label-section");
        Label subtitle = new Label("Review completed lesson history, assign grades, and approve export-ready certificates");
        subtitle.getStyleClass().add("label-muted");

        table = buildTable();
        HBox actions = buildActions();

        content.getChildren().addAll(title, subtitle, table, actions);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-dark");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        load();
        return scroll;
    }

    private TableView<TrainingCertificate> buildTable() {
        TableView<TrainingCertificate> t = new TableView<>();
        t.getStyleClass().add("table-dark");
        t.setPrefHeight(440);

        TableColumn<TrainingCertificate, String> userCol = new TableColumn<>("User");
        userCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        userCol.setPrefWidth(190);

        TableColumn<TrainingCertificate, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(110);

        TableColumn<TrainingCertificate, String> gradeCol = new TableColumn<>("Grade");
        gradeCol.setCellValueFactory(new PropertyValueFactory<>("grade"));
        gradeCol.setPrefWidth(90);

        TableColumn<TrainingCertificate, String> lessonsCol = new TableColumn<>("Lessons");
        lessonsCol.setCellValueFactory(data -> {
            TrainingCertificate c = data.getValue();
            return new javafx.beans.property.SimpleStringProperty(c.getCompletedLessons() + "/" + c.getTotalLessons());
        });
        lessonsCol.setPrefWidth(100);

        TableColumn<TrainingCertificate, Double> wpmCol = new TableColumn<>("Avg WPM");
        wpmCol.setCellValueFactory(new PropertyValueFactory<>("averageWpm"));
        wpmCol.setPrefWidth(100);

        TableColumn<TrainingCertificate, Double> accCol = new TableColumn<>("Avg Accuracy");
        accCol.setCellValueFactory(new PropertyValueFactory<>("averageAccuracy"));
        accCol.setPrefWidth(120);

        TableColumn<TrainingCertificate, String> requestedCol = new TableColumn<>("Requested");
        requestedCol.setCellValueFactory(data -> {
            var at = data.getValue().getRequestedAt();
            String text = at != null ? at.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "";
            return new javafx.beans.property.SimpleStringProperty(text);
        });
        requestedCol.setPrefWidth(130);

        t.getColumns().addAll(userCol, statusCol, gradeCol, lessonsCol, wpmCol, accCol, requestedCol);
        return t;
    }

    private HBox buildActions() {
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button refresh = new Button("Refresh");
        refresh.getStyleClass().add("btn-secondary");
        refresh.setOnAction(e -> load());

        Button history = new Button("View Training History");
        history.getStyleClass().add("btn-primary");
        history.setOnAction(e -> showHistory());

        Button grade = new Button("Set Grade");
        grade.getStyleClass().add("btn-gold");
        grade.setOnAction(e -> setGrade());

        actions.getChildren().addAll(refresh, history, grade);
        return actions;
    }

    private void load() {
        table.setItems(FXCollections.observableArrayList(certificateDAO.getAll()));
    }

    private void showHistory() {
        TrainingCertificate selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AdminDialogs.showInfo("No Selection", "Select a certificate request first.");
            return;
        }

        List<TrainingProgress> progress = trainingService.getProgress(selected.getUsername());
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Training History");
        dialog.setHeaderText(selected.getUsername());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox body = new VBox(12);
        body.setPadding(new Insets(18));
        body.setStyle("-fx-background-color: #0e0e22;");

        for (TrainingProgress p : progress) {
            Label row = new Label(String.format("%s  |  %.0f WPM  |  %.1f%% accuracy  |  %d attempts  |  %s",
                    p.getLessonTitle(), p.getBestWpm(), p.getBestAccuracy(), p.getAttempts(), p.getRecommendation()));
            row.setWrapText(true);
            row.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
            body.getChildren().add(row);
        }

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setPrefSize(760, 440);
        scroll.getStyleClass().add("scroll-dark");
        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().setStyle("-fx-background-color: #0e0e22;");
        dialog.showAndWait();
    }

    private void setGrade() {
        TrainingCertificate selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AdminDialogs.showInfo("No Selection", "Select a certificate request first.");
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Set Certificate Grade");
        dialog.setHeaderText("Approve certificate for " + selected.getUsername());
        ButtonType approveType = new ButtonType("Approve", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(approveType, ButtonType.CANCEL);

        ComboBox<String> gradeBox = new ComboBox<>(FXCollections.observableArrayList("A+", "A", "B+", "B", "C"));
        gradeBox.setValue(selected.getGrade() != null ? selected.getGrade() : suggestGrade(selected));
        gradeBox.setPrefWidth(220);
        gradeBox.getStyleClass().add("field-dark");

        VBox body = new VBox(10,
                AdminDialogs.formLabel("GRADE"),
                gradeBox,
                AdminDialogs.formLabel("Average: " + String.format("%.1f WPM, %.1f%% accuracy",
                        selected.getAverageWpm(), selected.getAverageAccuracy())));
        body.setPadding(new Insets(18));
        body.setStyle("-fx-background-color: #0e0e22;");
        dialog.getDialogPane().setContent(body);
        dialog.getDialogPane().setStyle("-fx-background-color: #0e0e22;");
        dialog.setResultConverter(bt -> bt == approveType ? gradeBox.getValue() : null);

        dialog.showAndWait().ifPresent(grade -> {
            certificateDAO.approveWithGrade(selected.getUsername(), grade, SessionManager.getInstance().getUsername());
            load();
            AdminDialogs.showSuccess("Certificate Approved", selected.getUsername() + " has been assigned grade " + grade + ".");
        });
    }

    private String suggestGrade(TrainingCertificate c) {
        if (c.getAverageWpm() >= 60 && c.getAverageAccuracy() >= 95) return "A+";
        if (c.getAverageWpm() >= 50 && c.getAverageAccuracy() >= 90) return "A";
        if (c.getAverageWpm() >= 40 && c.getAverageAccuracy() >= 85) return "B+";
        if (c.getAverageAccuracy() >= 80) return "B";
        return "C";
    }
}
