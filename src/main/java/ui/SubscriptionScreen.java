package ui;

import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.Subscription;
import service.PaymentService;
import util.SessionManager;

import java.time.format.DateTimeFormatter;

/**
 * Subscription / upgrade screen with simulated payment form.
 * Free users see plan options; premium users see their current plan status.
 */
public class SubscriptionScreen {

    private final PaymentService paymentService = PaymentService.getInstance();
    private final String username = SessionManager.getInstance().getUsername();

    private String selectedPlan = PaymentService.PLAN_MONTHLY;
    private Label statusLabel;

    // ── Scene factory ─────────────────────────────────────────────────────

    public Scene buildScene() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #0f0f1a;");
        root.getChildren().addAll(buildHeader(), buildBody());

        Scene scene = new Scene(root, 1200, 750);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        return scene;
    }

    // ── Header ────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        HBox header = new HBox(12);
        header.getStyleClass().add("header-bar");
        header.setAlignment(Pos.CENTER_LEFT);

        Button back = new Button("← Back");
        back.getStyleClass().add("btn-secondary");
        back.setOnAction(e -> MainUI.showDashboard());

        Label title = new Label("⭐  Premium Subscription");
        title.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 15px; -fx-font-weight: bold;");

        header.getChildren().addAll(back, title);
        return header;
    }

    // ── Body ──────────────────────────────────────────────────────────────

    private ScrollPane buildBody() {
        ScrollPane scroll = new ScrollPane();
        scroll.getStyleClass().add("scroll-dark");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox body = new VBox(36);
        body.setStyle("-fx-padding: 40 120 40 120;");
        body.setAlignment(Pos.TOP_CENTER);

        Subscription existing = paymentService.getSubscription(username);

        if (existing != null && existing.isActive()) {
            body.getChildren().addAll(buildCurrentPlanSection(existing));
        } else {
            body.getChildren().addAll(
                    buildHeroSection(),
                    buildPlanCards(),
                    buildPaymentForm());
        }

        scroll.setContent(body);
        return scroll;
    }

    // ── Active plan view ──────────────────────────────────────────────────

    private VBox buildCurrentPlanSection(Subscription sub) {
        VBox section = new VBox(24);
        section.setAlignment(Pos.CENTER);

        Label icon = new Label("⭐");
        icon.setStyle("-fx-font-size: 64px;");

        Label title = new Label("You have Premium Access!");
        title.getStyleClass().add("label-title");

        Label plan = new Label("Plan: " + sub.getPlan());
        plan.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 16px; -fx-font-weight: bold;");

        String expiryText = sub.getEndDate() == null
                ? "Lifetime access — never expires"
                : "Expires: " + sub.getEndDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        Label expiry = new Label(expiryText);
        expiry.setStyle("-fx-text-fill: #10b981; -fx-font-size: 14px;");

        VBox perks = buildPerksList();

        Button backBtn = new Button("← Go to Training");
        backBtn.getStyleClass().add("btn-primary");
        backBtn.setOnAction(e -> MainUI.showDashboard());

        section.getChildren().addAll(icon, title, plan, expiry, perks, backBtn);
        return section;
    }

    // ── Hero section ──────────────────────────────────────────────────────

    private VBox buildHeroSection() {
        VBox hero = new VBox(10);
        hero.setAlignment(Pos.CENTER);

        Label title = new Label("Unlock Your Full Potential");
        title.getStyleClass().add("label-title");
        Label sub = new Label(
                "Upgrade to Premium and get access to all lessons, advanced drills, and detailed analytics");
        sub.getStyleClass().add("label-muted");
        sub.setWrapText(true);
        sub.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        hero.getChildren().addAll(title, sub, buildPerksList());
        return hero;
    }

    private VBox buildPerksList() {
        VBox perks = new VBox(8);
        perks.setAlignment(Pos.CENTER_LEFT);
        perks.setMaxWidth(500);
        String[] items = {
                "✅  All 10 lessons — Beginner through Advanced",
                "✅  Intermediate coordination and sentence drills",
                "✅  Advanced speed bursts and accuracy challenges",
                "✅  Detailed per-lesson WPM and accuracy tracking",
                "✅  Unlimited practice attempts — no restrictions",
        };
        for (String item : items) {
            Label l = new Label(item);
            l.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 14px;");
            perks.getChildren().add(l);
        }
        return perks;
    }

    // ── Plan cards ────────────────────────────────────────────────────────

    private HBox buildPlanCards() {
        HBox row = new HBox(24);
        row.setAlignment(Pos.CENTER);

        VBox monthly = buildPlanCard(PaymentService.PLAN_MONTHLY, "Monthly", "Rs.199",
                "/month", "Best for trying out", false);
        VBox lifetime = buildPlanCard(PaymentService.PLAN_LIFETIME, "Lifetime", "Rs.1999",
                "one-time", "Best value — forever", true);

        row.getChildren().addAll(monthly, lifetime);
        return row;
    }

    private VBox buildPlanCard(String planId, String name, String price,
            String period, String tagline, boolean popular) {
        VBox card = new VBox(14);
        card.setPrefWidth(260);
        card.setAlignment(Pos.TOP_CENTER);

        String baseStyle = "-fx-background-color: #1a1a2e;" +
                "-fx-background-radius: 20;" +
                "-fx-padding: 32 28 32 28;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 16, 0, 0, 6);";
        String selectedStyle = "-fx-background-color: #1e2942;" +
                "-fx-background-radius: 20;" +
                "-fx-padding: 32 28 32 28;" +
                "-fx-cursor: hand;" +
                "-fx-border-color: #7c3aed;" +
                "-fx-border-radius: 20;" +
                "-fx-border-width: 2;" +
                "-fx-effect: dropshadow(gaussian, rgba(124,58,237,0.45), 22, 0, 0, 6);";

        card.setStyle(selectedPlan.equals(planId) ? selectedStyle : baseStyle);

        if (popular) {
            Label badge = new Label("MOST POPULAR");
            badge.setStyle(
                    "-fx-background-color: rgba(245,158,11,0.2);" +
                            "-fx-text-fill: #fbbf24;" +
                            "-fx-background-radius: 10;" +
                            "-fx-padding: 3 12 3 12;" +
                            "-fx-font-size: 11px;" +
                            "-fx-font-weight: bold;");
            card.getChildren().add(badge);
        }

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label priceLabel = new Label(price);
        priceLabel.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 36px; -fx-font-weight: bold;");

        Label periodLabel = new Label(period);
        periodLabel.getStyleClass().add("label-muted");

        Label tagLabel = new Label(tagline);
        tagLabel.getStyleClass().add("label-body");

        card.getChildren().addAll(nameLabel, priceLabel, periodLabel, tagLabel);

        card.setOnMouseClicked(e -> {
            selectedPlan = planId;
            // Refresh: caller will need to rebuild; simplest approach is to toggle styles
            // here
            String active = selectedPlan.equals(planId) ? selectedStyle : baseStyle;
            card.setStyle(active);
            // Force redraw of sibling by re-querying parent
            if (card.getParent() instanceof HBox parent) {
                parent.getChildren().forEach(child -> {
                    if (child instanceof VBox sibling && sibling != card) {
                        sibling.setStyle(baseStyle);
                    }
                });
                card.setStyle(selectedStyle);
            }
        });

        return card;
    }

    // ── Payment form ──────────────────────────────────────────────────────

    private VBox buildPaymentForm() {
        VBox form = new VBox(18);
        form.setMaxWidth(480);
        form.setAlignment(Pos.CENTER_LEFT);

        Label formTitle = new Label("Payment Details");
        formTitle.getStyleClass().add("label-section");

        Label simNote = new Label("🔒  Simulated payment — enter any 16-digit card number");
        simNote.setStyle(
                "-fx-background-color: rgba(16,185,129,0.1);" +
                        "-fx-text-fill: #10b981;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 8 14 8 14;" +
                        "-fx-font-size: 12px;");

        TextField cardHolder = styledField("Cardholder Name", false);
        TextField cardNumber = styledField("Card Number (16 digits)", false);
        TextField expiry = styledField("MM/YY", false);
        TextField cvv = styledField("CVV", false);

        // Auto-format card number with spaces
        cardNumber.textProperty().addListener((obs, old, val) -> {
            String digits = val.replaceAll("[^0-9]", "");
            if (digits.length() > 16)
                digits = digits.substring(0, 16);
            StringBuilder fmt = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i > 0 && i % 4 == 0)
                    fmt.append(' ');
                fmt.append(digits.charAt(i));
            }
            String formatted = fmt.toString();
            if (!formatted.equals(val)) {
                int caret = cardNumber.getCaretPosition();
                cardNumber.setText(formatted);
                cardNumber.positionCaret(Math.min(caret, formatted.length()));
            }
        });

        statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(460);

        Button payBtn = new Button("Pay " + PaymentService.getDisplayPrice(selectedPlan) + "  →");
        payBtn.getStyleClass().add("btn-success");
        payBtn.setMaxWidth(Double.MAX_VALUE);
        payBtn.setOnAction(e -> {
            // Re-read selected plan price on click
            payBtn.setText("Pay " + PaymentService.getDisplayPrice(selectedPlan) + "  →");
            handlePayment(cardHolder.getText(), cardNumber.getText(),
                    expiry.getText(), cvv.getText(), payBtn);
        });

        form.getChildren().addAll(
                formTitle, simNote,
                fieldGroup("Cardholder Name", cardHolder),
                fieldGroup("Card Number", cardNumber),
                new HBox(16,
                        fieldGroup("Expiry Date", expiry),
                        fieldGroup("CVV", cvv)),
                statusLabel, payBtn);
        return form;
    }

    private void handlePayment(String holder, String cardNum, String expiry,
            String cvv, Button payBtn) {
        String error = paymentService.validate(holder, cardNum, expiry, cvv);
        if (error != null) {
            statusLabel.getStyleClass().setAll("label-error");
            statusLabel.setText("⚠  " + error);
            return;
        }

        statusLabel.getStyleClass().setAll("label-muted");
        statusLabel.setText("Processing payment…");
        payBtn.setDisable(true);

        // Simulate brief processing delay
        javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(
                javafx.util.Duration.millis(900));
        pt.setOnFinished(ev -> {
            try {
                paymentService.processPayment(username, selectedPlan);
                statusLabel.getStyleClass().setAll("label-success");
                statusLabel.setText("✅  Payment successful! Premium unlocked.");
                javafx.animation.PauseTransition wait = new javafx.animation.PauseTransition(
                        javafx.util.Duration.millis(1200));
                wait.setOnFinished(e2 -> MainUI.showDashboard());
                wait.play();
            } catch (Exception ex) {
                statusLabel.getStyleClass().setAll("label-error");
                statusLabel.setText("⚠  Payment failed: " + ex.getMessage());
                payBtn.setDisable(false);
            }
        });
        pt.play();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private TextField styledField(String prompt, boolean isPassword) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("field-dark");
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private VBox fieldGroup(String label, javafx.scene.Node field) {
        VBox group = new VBox(6);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("label-muted");
        group.getChildren().addAll(lbl, field);
        HBox.setHgrow(group, Priority.ALWAYS);
        return group;
    }
}