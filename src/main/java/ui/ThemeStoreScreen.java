package ui;

import game.ThemeManager;
import game.ThemeManager.ThemeDef;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.UserStats;
import service.GamificationService;
import service.StoreService;
import service.StoreService.PurchaseResult;
import util.SessionManager;

/**
 * Theme store screen — browse, buy, and equip themes.
 */
public class ThemeStoreScreen {

    public javafx.scene.Scene buildScene() {
        VBox layout = new VBox(0);
        layout.setStyle("-fx-background-color: #0f0f1a;");
        layout.getChildren().addAll(buildHeader(), buildContent());
        VBox.setVgrow((Node) layout.getChildren().get(1), Priority.ALWAYS);
        javafx.scene.Scene scene = new javafx.scene.Scene(layout, 1200, 750);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        return scene;
    }

    private HBox buildHeader() {
        HBox header = new HBox(16);
        header.getStyleClass().add("header-bar");
        header.setAlignment(Pos.CENTER_LEFT);
        Button back = new Button("← Dashboard");
        back.getStyleClass().add("btn-secondary");
        back.setOnAction(e -> MainUI.showDashboard());
        Label title = new Label("🎨  Theme Store");
        title.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 15px; -fx-font-weight: bold;");
        header.getChildren().addAll(back, title);
        return header;
    }

    public Node buildContent() {
        ScrollPane scroll = new ScrollPane(buildInner());
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-dark");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private VBox buildInner() {
        String username = SessionManager.getInstance().getUsername();

        UserStats stats = null;
        try { stats = GamificationService.getInstance().getStats(username); }
        catch (Exception ignored) {}
        int coins = stats != null ? stats.getCoins() : 0;
        int level = stats != null ? stats.getLevel()  : 1;

        VBox root = new VBox(24);
        root.setStyle("-fx-padding: 36 40 36 40; -fx-background-color: #0f0f1a;");

        // ── Header ────────────────────────────────────────────────────────
        Label title = new Label("🎨  Theme Store");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label sub = new Label("Unlock new themes by levelling up or spending coins");
        sub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");

        HBox coinsBadge = new HBox(6);
        coinsBadge.setAlignment(Pos.CENTER_LEFT);
        Label coinIcon = new Label("🪙");
        coinIcon.setStyle("-fx-font-size: 16px;");
        Label coinCount = new Label(coins + " coins available");
        coinCount.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 13px; -fx-font-weight: bold;");
        coinsBadge.getChildren().addAll(coinIcon, coinCount);

        root.getChildren().addAll(title, sub, coinsBadge, buildSep());

        // ── Level-unlocked section ────────────────────────────────────────
        Label levelHeader = new Label("🔓  Level Unlocks");
        levelHeader.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 15px; -fx-font-weight: bold;");
        root.getChildren().add(levelHeader);

        FlowPane levelGrid = new FlowPane(12, 12);
        for (ThemeDef t : ThemeManager.ALL_THEMES) {
            if ("LEVEL".equals(t.unlockType()))
                levelGrid.getChildren().add(buildThemeCard(t, username, level, coins));
        }
        root.getChildren().add(levelGrid);

        root.getChildren().add(buildSep());

        // ── Coin-purchase section ─────────────────────────────────────────
        Label coinHeader = new Label("🪙  Buy with Coins");
        coinHeader.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 15px; -fx-font-weight: bold;");
        root.getChildren().add(coinHeader);

        FlowPane coinGrid = new FlowPane(12, 12);
        for (ThemeDef t : ThemeManager.ALL_THEMES) {
            if ("COINS".equals(t.unlockType()))
                coinGrid.getChildren().add(buildThemeCard(t, username, level, coins));
        }
        root.getChildren().add(coinGrid);

        return root;
    }

    private VBox buildThemeCard(ThemeDef t, String username, int level, int coins) {
        boolean owned = StoreService.getInstance().getInventory(username).hasTheme(t.id());
        boolean active = ThemeManager.getInstance().getActiveThemeId().equals(t.id());
        boolean canAfford = "COINS".equals(t.unlockType()) && coins >= t.coinCost();
        boolean levelOk   = "LEVEL".equals(t.unlockType()) && level >= t.levelRequired();

        VBox card = new VBox(10);
        card.setPrefWidth(160);
        card.setAlignment(Pos.CENTER);
        card.setStyle(
            "-fx-background-color: " + t.card() + ";" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 16;" +
            "-fx-border-color: " + (active ? t.accent() : "transparent") + ";" +
            "-fx-border-radius: 14;" +
            "-fx-border-width: 2;");

        // Colour swatch
        HBox swatch = new HBox(4);
        swatch.setAlignment(Pos.CENTER);
        swatch.setStyle("-fx-background-color: " + t.bg() + ";" +
                        "-fx-background-radius: 8; -fx-padding: 8;");
        for (String color : new String[]{ t.bg(), t.accent(), t.card() }) {
            Region dot = new Region();
            dot.setPrefSize(14, 14);
            dot.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 7;");
            swatch.getChildren().add(dot);
        }

        Label name = new Label(t.name());
        name.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
        name.setWrapText(true);
        name.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Status / action
        String unlockInfo = "LEVEL".equals(t.unlockType())
                ? "Level " + t.levelRequired()
                : t.coinCost() + " 🪙";

        Button actionBtn;
        if (active) {
            actionBtn = new Button("✓ Equipped");
            actionBtn.setStyle("-fx-background-color: rgba(124,58,237,0.3); -fx-text-fill: #a78bfa;" +
                               "-fx-background-radius: 8; -fx-font-size: 11px;");
            actionBtn.setDisable(true);
        } else if (owned) {
            actionBtn = new Button("Equip");
            actionBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white;" +
                               "-fx-background-radius: 8; -fx-font-size: 11px; -fx-cursor: hand;");
            actionBtn.setOnAction(e -> {
                StoreService.getInstance().equipTheme(username, t.id());
                MainUI.showThemeStore();
            });
        } else if (levelOk || canAfford) {
            String btnLabel = "COINS".equals(t.unlockType()) ? "Buy " + t.coinCost() + " 🪙" : "Unlock";
            actionBtn = new Button(btnLabel);
            actionBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white;" +
                               "-fx-background-radius: 8; -fx-font-size: 11px; -fx-cursor: hand;");
            actionBtn.setOnAction(e -> {
                PurchaseResult pr = "COINS".equals(t.unlockType())
                        ? StoreService.getInstance().buyTheme(username, t.id())
                        : StoreService.getInstance().buyTheme(username, t.id());
                if (pr == PurchaseResult.SUCCESS) {
                    StoreService.getInstance().equipTheme(username, t.id());
                }
                MainUI.showThemeStore();
            });
        } else {
            actionBtn = new Button("🔒 " + unlockInfo);
            actionBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #64748b;" +
                               "-fx-background-radius: 8; -fx-font-size: 11px;");
            actionBtn.setDisable(true);
        }
        actionBtn.setMaxWidth(Double.MAX_VALUE);

        card.getChildren().addAll(swatch, name, actionBtn);
        return card;
    }

    private Region buildSep() {
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color: #1e293b;");
        return sep;
    }
}
