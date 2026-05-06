package ui.admin;

import db.MongoDBManager;
import db.SubscriptionDAO;
import db.UserStatsDAO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import model.GameResult;
import model.Subscription;
import model.User;
import model.UserStats;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Admin Analytics Panel — rich metrics, vibrant pie charts, bar chart, and session table.
 */
public class AdminAnalyticsPanel {

    // Vibrant palette — each slice gets one of these
    private static final String[] CHART_COLORS = {
        "#a78bfa", "#38bdf8", "#34d399", "#fbbf24", "#f472b6",
        "#fb923c", "#2dd4bf", "#c084fc", "#ef4444", "#84cc16"
    };

    public ScrollPane buildContent() {
        VBox content = new VBox(28);
        content.setStyle("-fx-padding: 34 38 34 38; -fx-background-color: #080818;");

        // ── Page header ────────────────────────────────────────────────────
        VBox header = new VBox(4);
        Label title = new Label("Analytics Dashboard");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label sub = new Label("System-wide performance metrics, user insights and session analytics");
        sub.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");
        header.getChildren().addAll(title, sub);

        // ── Top metrics grid (8 cards) ─────────────────────────────────────
        GridPane metricsGrid = buildMetricsGrid();

        // ── Charts row: two big pie charts ────────────────────────────────
        HBox chartsRow = buildChartsRow();

        // ── Difficulty bar chart ───────────────────────────────────────────
        VBox barChartSection = buildDifficultyBarChart();

        // ── WPM distribution bar chart ─────────────────────────────────────
        VBox wpmBarSection = buildWpmDistributionChart();

        // ── Recent sessions table ──────────────────────────────────────────
        VBox sessionsTable = buildRecentSessionsTable();

        content.getChildren().addAll(header, metricsGrid, chartsRow,
                buildHBox(barChartSection, wpmBarSection), sessionsTable);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-dark");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    // ── Metrics grid ──────────────────────────────────────────────────────────

    private GridPane buildMetricsGrid() {
        MongoDBManager db = MongoDBManager.getInstance();
        List<User> users   = db.getAllUsers();
        int totalUsers  = users.size();
        int activeUsers = (int) users.stream().filter(u -> u.getTotalGames() > 0).count();
        long totalSessions = db.getTotalSessions();
        double avgWpm = users.stream().mapToDouble(User::getAverageWpm).average().orElse(0);

        List<Subscription> subs = SubscriptionDAO.getInstance().getAllSubscriptions();
        long premium  = subs.stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()) && !"FREE".equals(s.getPlan()))
                .map(Subscription::getUsername).distinct().count();
        long pending  = subs.stream().filter(s -> "PENDING".equals(s.getStatus())).count();
        String popularMode = db.getMostPopularGameMode();
        double revenue = subs.stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()) && !"FREE".equals(s.getPlan()))
                .mapToDouble(s -> "LIFETIME".equals(s.getPlan()) ? 1999.0 : 199.0).sum();

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);

        grid.add(analyticsCard("👥", "Total Users",    String.valueOf(totalUsers),        "#a78bfa", "#7c3aed"), 0, 0);
        grid.add(analyticsCard("✅", "Active Users",   String.valueOf(activeUsers),        "#34d399", "#059669"), 1, 0);
        grid.add(analyticsCard("🎮", "Total Sessions", String.valueOf(totalSessions),      "#fbbf24", "#b45309"), 2, 0);
        grid.add(analyticsCard("⚡", "Avg WPM",        String.format("%.1f", avgWpm),      "#38bdf8", "#0284c7"), 3, 0);
        grid.add(analyticsCard("💎", "Premium Users",  String.valueOf(premium),            "#f472b6", "#be185d"), 0, 1);
        grid.add(analyticsCard("🔔", "Pending",        String.valueOf(pending),            "#fb923c", "#c2410c"), 1, 1);
        grid.add(analyticsCard("🏆", "Popular Mode",   popularMode,                       "#c084fc", "#7c3aed"), 2, 1);
        grid.add(analyticsCard("💰", "Revenue",        String.format("₹%.0f", revenue),   "#2dd4bf", "#0e7490"), 3, 1);
        return grid;
    }

    private VBox analyticsCard(String icon, String label, String value, String light, String dark) {
        VBox card = new VBox(6);
        card.setStyle(
            "-fx-background-color: #111128;" +
            "-fx-background-radius: 16; -fx-padding: 18 22 18 22;" +
            "-fx-border-color: " + dark + "55;" +
            "-fx-border-radius: 16; -fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian," + dark + "44, 14, 0, 0, 4);");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMinWidth(155);

        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size: 24px;");

        Label val = new Label(value);
        val.setStyle("-fx-text-fill: " + light + "; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px;");

        card.getChildren().addAll(ico, val, lbl);
        return card;
    }

    // ── Pie Charts Row ────────────────────────────────────────────────────────

    private HBox buildChartsRow() {
        HBox row = new HBox(20);

        VBox modeBox = buildPieCard(
            "🎮  Game Mode Distribution",
            buildModeChart()
        );
        VBox subBox = buildPieCard(
            "💳  Subscription Breakdown",
            buildSubChart()
        );

        HBox.setHgrow(modeBox, Priority.ALWAYS);
        HBox.setHgrow(subBox, Priority.ALWAYS);
        row.getChildren().addAll(modeBox, subBox);
        return row;
    }

    private VBox buildPieCard(String heading, PieChart chart) {
        VBox card = new VBox(14);
        card.setStyle(
            "-fx-background-color: #111128;" +
            "-fx-background-radius: 18; -fx-padding: 22 24 22 24;" +
            "-fx-border-color: rgba(124,58,237,0.2);" +
            "-fx-border-radius: 18; -fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian,rgba(124,58,237,0.2),18,0,0,6);");

        Label h = new Label(heading);
        h.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");

        // Chart setup - simplified
        chart.setLabelsVisible(true);
        chart.setLegendVisible(true);
        chart.setLegendSide(javafx.geometry.Side.BOTTOM);
        chart.setMinHeight(300);
        chart.setPrefHeight(320);
        chart.setMaxHeight(360);
        chart.setAnimated(true);

        card.getChildren().addAll(h, chart);
        return card;
    }

    private PieChart buildModeChart() {
        PieChart chart = new PieChart();
        
        List<GameResult> results = MongoDBManager.getInstance().getLeaderboard(10000);
        
        if (results.isEmpty()) {
            chart.getData().add(new PieChart.Data("No Data Yet", 1));
        } else {
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (GameResult r : results) {
                String mode = r.getGameMode() != null ? r.getGameMode() : "Unknown";
                counts.merge(mode, 1, Integer::sum);
            }
            counts.forEach((mode, cnt) ->
                chart.getData().add(new PieChart.Data(mode + " (" + cnt + ")", cnt)));
        }

        return chart;
    }

    private PieChart buildSubChart() {
        PieChart chart = new PieChart();
        
        long totalUsers = MongoDBManager.getInstance().getAllUsers().size();
        
        if (totalUsers == 0) {
            chart.getData().add(new PieChart.Data("No Users Yet", 1));
        } else {
            List<Subscription> subs = SubscriptionDAO.getInstance().getAllSubscriptions();
            Map<String, Integer> counts = new LinkedHashMap<>();
            
            for (Subscription s : subs) {
                String key = s.getPlan() + " · " + s.getStatus();
                counts.merge(key, 1, Integer::sum);
            }
            
            long subUsers = subs.stream().map(Subscription::getUsername).distinct().count();
            long freeUsers = totalUsers - subUsers;
            if (freeUsers > 0) {
                counts.put("FREE (" + freeUsers + ")", (int) freeUsers);
            }
            
            counts.forEach((key, cnt) ->
                chart.getData().add(new PieChart.Data(key, cnt)));
        }

        return chart;
    }

    // ── Difficulty Bar Chart ──────────────────────────────────────────────────

    private VBox buildDifficultyBarChart() {
        VBox card = new VBox(14);
        card.setStyle(
            "-fx-background-color: #111128;" +
            "-fx-background-radius: 16; -fx-padding: 22 24 22 24;" +
            "-fx-border-color: rgba(56,189,248,0.2);" +
            "-fx-border-radius: 16; -fx-border-width: 1;");

        Label h = new Label("📊  Games by Difficulty");
        h.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");

        List<GameResult> all = MongoDBManager.getInstance().getLeaderboard(10000);
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("Easy",   all.stream().filter(r -> "Easy".equals(r.getDifficulty())).count());
        counts.put("Medium", all.stream().filter(r -> "Medium".equals(r.getDifficulty())).count());
        counts.put("Hard",   all.stream().filter(r -> "Hard".equals(r.getDifficulty())).count());

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setStyle("-fx-tick-label-fill: #64748b; -fx-tick-mark-visible: false;");
        yAxis.setStyle("-fx-tick-label-fill: #64748b;");
        yAxis.setLabel("Games Played");

        BarChart<String, Number> bar = new BarChart<>(xAxis, yAxis);
        bar.setStyle("-fx-background-color: transparent; -fx-plot-background-color: #0e0e22;");
        bar.setLegendVisible(false);
        bar.setAnimated(false);
        bar.setPrefHeight(220);
        bar.setBarGap(6);
        bar.setCategoryGap(30);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        String[] barColors = {"#34d399", "#fbbf24", "#f472b6"};
        int ci = 0;
        for (Map.Entry<String, Long> e : counts.entrySet()) {
            XYChart.Data<String, Number> d = new XYChart.Data<>(e.getKey(), e.getValue());
            series.getData().add(d);
            final String col = barColors[ci++ % barColors.length];
            Platform.runLater(() -> {
                if (d.getNode() != null)
                    d.getNode().setStyle(
                        "-fx-bar-fill: " + col + ";" +
                        "-fx-background-radius: 6 6 0 0;");
            });
        }
        bar.getData().add(series);

        // Color bars after layout
        Platform.runLater(() -> {
            var data = series.getData();
            for (int i = 0; i < data.size(); i++) {
                String col = barColors[i % barColors.length];
                if (data.get(i).getNode() != null)
                    data.get(i).getNode().setStyle(
                        "-fx-bar-fill: " + col + ";" +
                        "-fx-background-radius: 6 6 0 0;");
            }
        });

        card.getChildren().addAll(h, bar);
        return card;
    }

    // ── WPM Distribution Chart ────────────────────────────────────────────────

    private VBox buildWpmDistributionChart() {
        VBox card = new VBox(14);
        card.setStyle(
            "-fx-background-color: #111128;" +
            "-fx-background-radius: 16; -fx-padding: 22 24 22 24;" +
            "-fx-border-color: rgba(167,139,250,0.2);" +
            "-fx-border-radius: 16; -fx-border-width: 1;");

        Label h = new Label("⚡  WPM Distribution");
        h.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");

        List<GameResult> all = MongoDBManager.getInstance().getLeaderboard(10000);
        // Bucket into WPM ranges
        Map<String, Long> buckets = new LinkedHashMap<>();
        buckets.put("0-20",   all.stream().filter(r -> r.getWpm() <  20).count());
        buckets.put("20-40",  all.stream().filter(r -> r.getWpm() >= 20 && r.getWpm() < 40).count());
        buckets.put("40-60",  all.stream().filter(r -> r.getWpm() >= 40 && r.getWpm() < 60).count());
        buckets.put("60-80",  all.stream().filter(r -> r.getWpm() >= 60 && r.getWpm() < 80).count());
        buckets.put("80-100", all.stream().filter(r -> r.getWpm() >= 80 && r.getWpm() < 100).count());
        buckets.put("100+",   all.stream().filter(r -> r.getWpm() >= 100).count());

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setStyle("-fx-tick-label-fill: #64748b; -fx-tick-mark-visible: false;");
        yAxis.setStyle("-fx-tick-label-fill: #64748b;");
        xAxis.setLabel("WPM Range");

        BarChart<String, Number> bar = new BarChart<>(xAxis, yAxis);
        bar.setStyle("-fx-background-color: transparent; -fx-plot-background-color: #0e0e22;");
        bar.setLegendVisible(false);
        bar.setAnimated(false);
        bar.setPrefHeight(220);
        bar.setBarGap(4);
        bar.setCategoryGap(22);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        String[] wpmColors = {"#ef4444", "#fb923c", "#fbbf24", "#a3e635", "#34d399", "#38bdf8"};
        int ci = 0;
        for (Map.Entry<String, Long> e : buckets.entrySet()) {
            XYChart.Data<String, Number> d = new XYChart.Data<>(e.getKey(), e.getValue());
            series.getData().add(d);
        }
        bar.getData().add(series);

        Platform.runLater(() -> {
            var data = series.getData();
            for (int i = 0; i < data.size(); i++) {
                String col = wpmColors[i % wpmColors.length];
                if (data.get(i).getNode() != null)
                    data.get(i).getNode().setStyle(
                        "-fx-bar-fill: " + col + ";" +
                        "-fx-background-radius: 6 6 0 0;");
            }
        });

        card.getChildren().addAll(h, bar);
        return card;
    }

    // ── Recent Sessions Table ─────────────────────────────────────────────────

    private VBox buildRecentSessionsTable() {
        VBox card = new VBox(14);
        card.setStyle(
            "-fx-background-color: #111128;" +
            "-fx-background-radius: 16; -fx-padding: 22 24 22 24;" +
            "-fx-border-color: rgba(124,58,237,0.18);" +
            "-fx-border-radius: 16; -fx-border-width: 1;");

        Label h = new Label("🕒  Recent Game Sessions");
        h.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");

        TableView<GameResult> table = new TableView<>();
        table.getStyleClass().add("table-dark");
        table.setPrefHeight(260);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        TableColumn<GameResult, String> userCol = col("User", "username", 140);
        TableColumn<GameResult, String> modeCol = col("Mode", "gameMode", 90);

        TableColumn<GameResult, Double> wpmCol = new TableColumn<>("WPM");
        wpmCol.setCellValueFactory(new PropertyValueFactory<>("wpm"));
        wpmCol.setPrefWidth(90);
        wpmCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(String.format("%.1f", v));
                setStyle(v >= 80 ? "-fx-text-fill: #34d399; -fx-font-weight: bold;" :
                         v >= 50 ? "-fx-text-fill: #fbbf24; -fx-font-weight: bold;" :
                                   "-fx-text-fill: #94a3b8;");
            }
        });

        TableColumn<GameResult, Double> accCol = new TableColumn<>("Accuracy");
        accCol.setCellValueFactory(new PropertyValueFactory<>("accuracy"));
        accCol.setPrefWidth(95);
        accCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(String.format("%.1f%%", v));
                setStyle(v >= 95 ? "-fx-text-fill: #34d399; -fx-font-weight: bold;" :
                         v >= 80 ? "-fx-text-fill: #fbbf24;" :
                                   "-fx-text-fill: #94a3b8;");
            }
        });

        TableColumn<GameResult, String> diffCol = col("Difficulty", "difficulty", 95);

        TableColumn<GameResult, Integer> errCol = new TableColumn<>("Errors");
        errCol.setCellValueFactory(new PropertyValueFactory<>("errorCount"));
        errCol.setPrefWidth(70);
        errCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(String.valueOf(v));
                setStyle(v == 0 ? "-fx-text-fill: #34d399; -fx-font-weight: bold;" :
                         v <= 3  ? "-fx-text-fill: #fbbf24;" :
                                   "-fx-text-fill: #ef4444;");
            }
        });

        TableColumn<GameResult, String> timeCol = new TableColumn<>("Played At");
        timeCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getPlayedAt() != null ? d.getValue().getPlayedAt().format(fmt) : "—"));
        timeCol.setPrefWidth(155);

        table.getColumns().addAll(userCol, modeCol, wpmCol, accCol, diffCol, errCol, timeCol);
        table.setItems(FXCollections.observableArrayList(
                MongoDBManager.getInstance().getRecentSessions(30)));

        card.getChildren().addAll(h, table);
        return card;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private <T> TableColumn<GameResult, T> col(String title, String prop, int width) {
        TableColumn<GameResult, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(width);
        return c;
    }

    private HBox buildHBox(javafx.scene.Node... nodes) {
        HBox box = new HBox(20);
        for (javafx.scene.Node n : nodes) {
            HBox.setHgrow(n, Priority.ALWAYS);
            box.getChildren().add(n);
        }
        return box;
    }
}
