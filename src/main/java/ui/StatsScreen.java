package ui;

import db.MongoDBManager;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.GameResult;
import util.SessionManager;

import java.util.List;

/**
 * Statistics dashboard — per-user historical WPM trend, personal bests.
 */
public class StatsScreen {

    public Scene buildScene() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0f0f1a;");
        root.setCenter(buildContent());

        Scene scene = new Scene(root, 1200, 750);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        return scene;
    }

    public Node buildContent() {
        ScrollPane scroll = new ScrollPane();
        scroll.getStyleClass().add("scroll-dark");
        scroll.setFitToWidth(true);

        VBox content = new VBox(28);
        content.setStyle("-fx-padding: 36 40 36 40;");

        String username = SessionManager.getInstance().getUsername();
        var user = SessionManager.getInstance().getCurrentUser();

        Label title = new Label("📊  Your Statistics");
        title.getStyleClass().add("label-title");

        Label sub = new Label("Performance overview for " + username);
        sub.getStyleClass().add("label-muted");

        // ── Personal bests row ────────────────────────────────────────────
        double bestWpm  = user != null ? user.getBestWpm()      : 0;
        double avgWpm   = user != null ? user.getAverageWpm()   : 0;
        double bestAcc  = user != null ? user.getBestAccuracy() : 0;
        int    games    = user != null ? user.getTotalGames()   : 0;

        HBox bestsRow = new HBox(20);
        bestsRow.getChildren().addAll(
                statCard("Best WPM",   String.format("%.0f",  bestWpm),  "stat-number"),
                statCard("Avg WPM",    String.format("%.0f",  avgWpm),   "stat-number-cyan"),
                statCard("Best Acc",   String.format("%.0f%%", bestAcc), "stat-number-green"),
                statCard("Total Games",String.valueOf(games),             "stat-number-orange")
        );

        // ── WPM Chart ────────────────────────────────────────────────────
        List<GameResult> recent = MongoDBManager.getInstance().getResultsForUser(username, 15);

        VBox chartBox = new VBox(10);
        chartBox.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 14; -fx-padding: 24;");

        Label chartTitle = new Label("Recent WPM Trend  (last " + recent.size() + " games)");
        chartTitle.getStyleClass().add("label-section");

        NumberAxis xAxis = new NumberAxis(1, Math.max(recent.size(), 5), 1);
        xAxis.setLabel("Game #");
        xAxis.getStyleClass().add("axis");
        xAxis.setTickLabelFill(javafx.scene.paint.Color.web("#64748b"));

        NumberAxis yAxis = new NumberAxis(0, 150, 20);
        yAxis.setLabel("WPM");
        yAxis.getStyleClass().add("axis");
        yAxis.setTickLabelFill(javafx.scene.paint.Color.web("#64748b"));

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle(null);
        chart.getStyleClass().add("chart");
        chart.setCreateSymbols(true);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setPrefHeight(280);

        XYChart.Series<Number, Number> wpmSeries = new XYChart.Series<>();
        wpmSeries.setName("WPM");

        // Recent list is newest-first; reverse for chronological chart
        for (int i = recent.size() - 1; i >= 0; i--) {
            int idx = recent.size() - i;
            wpmSeries.getData().add(new XYChart.Data<>(idx, recent.get(i).getWpm()));
        }
        chart.getData().add(wpmSeries);

        // Accuracy series
        XYChart.Series<Number, Number> accSeries = new XYChart.Series<>();
        accSeries.setName("Accuracy %");
        for (int i = recent.size() - 1; i >= 0; i--) {
            int idx = recent.size() - i;
            accSeries.getData().add(new XYChart.Data<>(idx, recent.get(i).getAccuracy()));
        }
        chart.getData().add(accSeries);

        chartBox.getChildren().addAll(chartTitle, chart);

        // ── Recent games table ────────────────────────────────────────────
        VBox tableBox = new VBox(10);
        tableBox.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 14; -fx-padding: 24;");

        Label tableTitle = new Label("Game History");
        tableTitle.getStyleClass().add("label-section");

        TableView<GameResult> table = new TableView<>();
        table.getStyleClass().add("table-dark");
        table.setPrefHeight(240);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<GameResult, String> modeCol = new TableColumn<>("Mode");
        modeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getGameMode()));

        TableColumn<GameResult, String> diffCol = new TableColumn<>("Difficulty");
        diffCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getDifficulty()));

        TableColumn<GameResult, String> wpmCol = new TableColumn<>("WPM");
        wpmCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                String.format("%.1f", c.getValue().getWpm())));

        TableColumn<GameResult, String> accCol = new TableColumn<>("Accuracy");
        accCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                String.format("%.1f%%", c.getValue().getAccuracy())));

        TableColumn<GameResult, String> errCol = new TableColumn<>("Errors");
        errCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(c.getValue().getErrorCount())));

        TableColumn<GameResult, String> dateCol = new TableColumn<>("Played At");
        dateCol.setCellValueFactory(c -> {
            var dt = c.getValue().getPlayedAt();
            return new javafx.beans.property.SimpleStringProperty(
                    dt != null ? dt.toString().replace("T", " ").substring(0, 16) : "-");
        });

        table.getColumns().add(modeCol);
        table.getColumns().add(diffCol);
        table.getColumns().add(wpmCol);
        table.getColumns().add(accCol);
        table.getColumns().add(errCol);
        table.getColumns().add(dateCol);
        table.getItems().addAll(recent);

        tableBox.getChildren().addAll(tableTitle, table);

        content.getChildren().addAll(title, sub, gap(8), bestsRow, chartBox, tableBox);
        scroll.setContent(content);
        return scroll;
    }

    private VBox statCard(String label, String value, String numStyle) {
        VBox box = new VBox(4);
        box.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 12; -fx-padding: 18 28 18 28;");
        Label val = new Label(value);
        val.getStyleClass().add(numStyle);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("label-muted");
        box.getChildren().addAll(val, lbl);
        return box;
    }

    private Region gap(int h) {
        Region r = new Region();
        r.setMinHeight(h);
        return r;
    }
}
