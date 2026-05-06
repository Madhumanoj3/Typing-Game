package ui.admin;

import db.SubscriptionDAO;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import model.Subscription;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Admin Subscriptions Panel - View, verify, and manage user subscriptions.
 * Includes PENDING verification workflow for new subscription requests.
 */
public class AdminSubscriptionsPanel {

    private TableView<Subscription> subsTable;
    private VBox summaryContainer;
    private VBox pendingContainer;
    private String currentFilter = "All";

    public ScrollPane buildContent() {
        VBox content = new VBox(20);
        content.setStyle("-fx-padding: 36 40 36 40;");

        Label title = new Label("Subscription Management");
        title.getStyleClass().add("label-section");

        Label subtitle = new Label("Verify payments and manage user subscription plans  •  ₹199/month  •  ₹1,999/year");
        subtitle.getStyleClass().add("label-muted");

        summaryContainer = new VBox();
        buildSummaryCards();

        pendingContainer = new VBox(12);
        buildPendingSection();

        HBox filterBar = buildFilterBar();
        subsTable = buildSubsTable();
        HBox actionButtons = buildActionButtons();

        content.getChildren().addAll(title, subtitle, summaryContainer, pendingContainer,
                filterBar, subsTable, actionButtons);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-dark");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        loadSubscriptions();
        return scroll;
    }

    // ── Summary Cards ─────────────────────────────────────────────────────

    private void buildSummaryCards() {
        summaryContainer.getChildren().clear();
        HBox box = new HBox(16);

        List<Subscription> all = SubscriptionDAO.getInstance().getAllSubscriptions();
        long total = all.size();
        long active = all.stream().filter(s -> "ACTIVE".equals(s.getStatus())).count();
        long pending = all.stream().filter(s -> "PENDING".equals(s.getStatus())).count();
        long premium = all.stream().filter(s -> !"FREE".equals(s.getPlan()) && "ACTIVE".equals(s.getStatus())).count();

        double revenue = all.stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()) && !"FREE".equals(s.getPlan()))
                .mapToDouble(s -> "LIFETIME".equals(s.getPlan()) ? 1999.0 : 199.0)
                .sum();

        box.getChildren().addAll(
            metricCard("Total Subs", String.valueOf(total), "📊", "#7c3aed"),
            metricCard("Active", String.valueOf(active), "✅", "#10b981"),
            metricCard("Pending", String.valueOf(pending), "🔔", "#ef4444"),
            metricCard("Premium", String.valueOf(premium), "💎", "#fbbf24"),
            metricCard("Revenue", String.format("₹%.0f", revenue), "💰", "#06b6d4")
        );
        summaryContainer.getChildren().add(box);
    }

    private VBox metricCard(String label, String value, String icon, String color) {
        VBox card = new VBox(6);
        card.setStyle(
            "-fx-background-color: #1a1a2e;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 16 22 16 22;" +
            "-fx-min-width: 130;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 10, 0, 0, 3);");
        card.setAlignment(Pos.CENTER_LEFT);

        Label i = new Label(icon);
        i.setStyle("-fx-font-size: 20px;");

        Label v = new Label(value);
        v.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 20px; -fx-font-weight: bold;");

        Label l = new Label(label);
        l.getStyleClass().add("label-muted");
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        card.getChildren().addAll(i, v, l);
        return card;
    }

    // ── Pending Verifications ─────────────────────────────────────────────

    private void buildPendingSection() {
        pendingContainer.getChildren().clear();

        List<Subscription> pending = SubscriptionDAO.getInstance().getAllSubscriptions().stream()
                .filter(s -> "PENDING".equals(s.getStatus())).toList();

        if (pending.isEmpty()) return;

        VBox card = new VBox(12);
        card.setStyle(
            "-fx-background-color: #1a1a2e;" +
            "-fx-background-radius: 16;" +
            "-fx-padding: 20 24 20 24;" +
            "-fx-border-color: rgba(251,191,36,0.3);" +
            "-fx-border-radius: 16;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(251,191,36,0.1), 10, 0, 0, 4);");

        Label header = new Label("🔔 Pending Payment Verifications (" + pending.size() + ")");
        header.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 15px; -fx-font-weight: bold;");

        Label hint = new Label("Verify the payment amount and subscription plan before activating");
        hint.getStyleClass().add("label-muted");

        card.getChildren().addAll(header, hint);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
        for (Subscription s : pending) {
            String price = "MONTHLY".equals(s.getPlan()) ? "₹199/month" : "₹1,999/year";

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle(
                "-fx-background-color: rgba(251,191,36,0.06);" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 12 16 12 16;");

            VBox info = new VBox(4);
            Label userLine = new Label("👤 " + s.getUsername() + "  —  " + s.getPlan());
            userLine.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

            Label detailLine = new Label("💰 Amount: " + price + "  •  📅 Requested: " +
                    (s.getStartDate() != null ? s.getStartDate().format(fmt) : "—"));
            detailLine.getStyleClass().add("label-muted");

            info.getChildren().addAll(userLine, detailLine);
            HBox.setHgrow(info, Priority.ALWAYS);

            Button verifyBtn = new Button("✅ Verify & Activate");
            verifyBtn.getStyleClass().add("btn-success");
            verifyBtn.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Verify Payment");
                confirm.setHeaderText("Verify payment for " + s.getUsername() + "?");
                confirm.setContentText("Plan: " + s.getPlan() + "\nAmount: " + price +
                        "\n\nConfirm that payment has been received?");
                confirm.showAndWait().ifPresent(bt -> {
                    if (bt == ButtonType.OK) {
                        SubscriptionDAO.getInstance().updateStatus(s.getId(), "ACTIVE");
                        refreshAll();
                    }
                });
            });

            Button rejectBtn = new Button("❌ Reject");
            rejectBtn.getStyleClass().add("btn-danger");
            rejectBtn.setOnAction(e -> {
                SubscriptionDAO.getInstance().updateStatus(s.getId(), "CANCELLED");
                refreshAll();
            });

            row.getChildren().addAll(info, verifyBtn, rejectBtn);
            card.getChildren().add(row);
        }

        pendingContainer.getChildren().add(card);
    }

    // ── Filter Bar ────────────────────────────────────────────────────────

    private HBox buildFilterBar() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);

        for (String f : new String[]{"All", "PENDING", "ACTIVE", "EXPIRED", "CANCELLED"}) {
            Button btn = new Button(f);
            btn.getStyleClass().add(f.equals(currentFilter) ? "diff-btn-selected" : "diff-btn");
            btn.setOnAction(e -> {
                currentFilter = f;
                loadSubscriptions();
                bar.getChildren().forEach(n -> {
                    if (n instanceof Button b)
                        b.getStyleClass().setAll(b.getText().equals(currentFilter)
                                ? "diff-btn-selected" : "diff-btn");
                });
            });
            bar.getChildren().add(btn);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> refreshAll());

        bar.getChildren().addAll(spacer, refreshBtn);
        return bar;
    }

    // ── Table ─────────────────────────────────────────────────────────────

    private TableView<Subscription> buildSubsTable() {
        TableView<Subscription> table = new TableView<>();
        table.getStyleClass().add("table-dark");
        table.setPrefHeight(320);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        TableColumn<Subscription, String> userCol = new TableColumn<>("Username");
        userCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        userCol.setPrefWidth(140);

        TableColumn<Subscription, String> planCol = new TableColumn<>("Plan");
        planCol.setCellValueFactory(new PropertyValueFactory<>("plan"));
        planCol.setPrefWidth(100);
        planCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                String price = switch (item) {
                    case "MONTHLY"  -> item + " (₹199)";
                    case "LIFETIME" -> item + " (₹1,999)";
                    default -> item;
                };
                setText(price);
                setStyle(switch (item) {
                    case "MONTHLY"  -> "-fx-text-fill: #06b6d4; -fx-font-weight: bold;";
                    case "LIFETIME" -> "-fx-text-fill: #fbbf24; -fx-font-weight: bold;";
                    default -> "-fx-text-fill: #94a3b8;";
                });
            }
        });

        TableColumn<Subscription, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(switch (item) {
                    case "ACTIVE"    -> "-fx-text-fill: #10b981; -fx-font-weight: bold;";
                    case "PENDING"   -> "-fx-text-fill: #fbbf24; -fx-font-weight: bold;";
                    case "EXPIRED"   -> "-fx-text-fill: #ef4444; -fx-font-weight: bold;";
                    case "CANCELLED" -> "-fx-text-fill: #64748b; -fx-font-weight: bold;";
                    default          -> "-fx-text-fill: #94a3b8;";
                });
            }
        });

        TableColumn<Subscription, String> startCol = new TableColumn<>("Start Date");
        startCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getStartDate() != null ? d.getValue().getStartDate().format(fmt) : "—"));
        startCol.setPrefWidth(145);

        TableColumn<Subscription, String> endCol = new TableColumn<>("End Date");
        endCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getEndDate() != null ? d.getValue().getEndDate().format(fmt) : "—"));
        endCol.setPrefWidth(145);

        table.getColumns().addAll(userCol, planCol, statusCol, startCol, endCol);
        return table;
    }

    // ── Action Buttons ────────────────────────────────────────────────────

    private HBox buildActionButtons() {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);

        Button verifyBtn = new Button("✅ Verify & Activate");
        verifyBtn.getStyleClass().add("btn-success");
        verifyBtn.setOnAction(e -> updateStatus("ACTIVE"));

        Button deactivateBtn = new Button("❌ Deactivate");
        deactivateBtn.getStyleClass().add("btn-danger");
        deactivateBtn.setOnAction(e -> updateStatus("EXPIRED"));

        Button cancelBtn = new Button("🚫 Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> updateStatus("CANCELLED"));

        Button changePlanBtn = new Button("📝 Change Plan");
        changePlanBtn.getStyleClass().add("btn-primary");
        changePlanBtn.setOnAction(e -> changePlan());

        box.getChildren().addAll(verifyBtn, deactivateBtn, cancelBtn, changePlanBtn);
        return box;
    }

    // ── Data Loading ──────────────────────────────────────────────────────

    private void loadSubscriptions() {
        List<Subscription> all = SubscriptionDAO.getInstance().getAllSubscriptions();
        List<Subscription> filtered = "All".equals(currentFilter) ? all :
                all.stream().filter(s -> currentFilter.equals(s.getStatus())).toList();
        subsTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void refreshAll() {
        buildSummaryCards();
        buildPendingSection();
        loadSubscriptions();
    }

    // ── Actions ───────────────────────────────────────────────────────────

    private void updateStatus(String newStatus) {
        Subscription sel = subsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("No Selection", "Select a subscription first."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Update Status");
        confirm.setHeaderText("Set status to " + newStatus + "?");
        confirm.setContentText("User: " + sel.getUsername() + "\nCurrent: " + sel.getStatus());

        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                SubscriptionDAO.getInstance().updateStatus(sel.getId(), newStatus);
                refreshAll();
                showAlert("Success", "Status updated to " + newStatus);
            }
        });
    }

    private void changePlan() {
        Subscription sel = subsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("No Selection", "Select a subscription first."); return; }

        ChoiceDialog<String> d = new ChoiceDialog<>(sel.getPlan(), "FREE", "MONTHLY", "LIFETIME");
        d.setTitle("Change Plan");
        d.setHeaderText("Change plan for: " + sel.getUsername());
        d.setContentText("Select new plan:");

        d.showAndWait().ifPresent(plan -> {
            SubscriptionDAO.getInstance().updatePlan(sel.getId(), plan);
            refreshAll();
            showAlert("Success", "Plan changed to " + plan);
        });
    }

    private void showAlert(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t);
        a.setHeaderText(null);
        a.setContentText(m);
        a.showAndWait();
    }
}
