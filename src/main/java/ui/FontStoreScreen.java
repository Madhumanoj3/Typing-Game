package ui;

import game.ThemeManager;
import game.ThemeManager.FontDef;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import model.UserStats;
import service.GamificationService;
import service.StoreService;
import service.StoreService.PurchaseResult;
import util.SessionManager;

/**
 * Font store screen — browse, buy, and equip fonts.
 */
public class FontStoreScreen {

    public Scene buildScene() {
        VBox layout = new VBox(0);
        layout.setStyle("-fx-background-color: #0f0f1a;");
        Node content = buildContent();
        layout.getChildren().addAll(buildHeader(), content);
        VBox.setVgrow(content, Priority.ALWAYS);
        Scene scene = new Scene(layout, 1200, 750);
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
        Label title = new Label("✍  Font Store");
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
        Label title = new Label("✍  Font Store");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label sub = new Label("Unlock new fonts by levelling up or spending coins");
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
        for (FontDef f : ThemeManager.ALL_FONTS) {
            if ("LEVEL".equals(f.unlockType()))
                levelGrid.getChildren().add(buildFontCard(f, username, level, coins));
        }
        root.getChildren().add(levelGrid);

        root.getChildren().add(buildSep());

        // ── Coin-purchase section ─────────────────────────────────────────
        Label coinHeader = new Label("🪙  Buy with Coins");
        coinHeader.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 15px; -fx-font-weight: bold;");
        root.getChildren().add(coinHeader);

        FlowPane coinGrid = new FlowPane(12, 12);
        for (FontDef f : ThemeManager.ALL_FONTS) {
            if ("COINS".equals(f.unlockType()))
                coinGrid.getChildren().add(buildFontCard(f, username, level, coins));
        }
        root.getChildren().add(coinGrid);

        return root;
    }

    private VBox buildFontCard(FontDef f, String username, int level, int coins) {
        boolean owned = StoreService.getInstance().getInventory(username).hasFont(f.id());
        boolean active = ThemeManager.getInstance().getActiveFontId().equals(f.id());
        boolean canAfford = "COINS".equals(f.unlockType()) && coins >= f.coinCost();
        boolean levelOk   = "LEVEL".equals(f.unlockType()) && level >= f.levelRequired();

        VBox card = new VBox(10);
        card.setPrefWidth(160);
        card.setAlignment(Pos.CENTER);
        card.setStyle(
            "-fx-background-color: #1a1a2e;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 16;" +
            "-fx-border-color: " + (active ? "#7c3aed" : "transparent") + ";" +
            "-fx-border-radius: 14;" +
            "-fx-border-width: 2;");

        // Font preview
        Label preview = new Label("AaBbCc");
        try { preview.setFont(Font.font(f.family(), 18)); }
        catch (Exception ignored) {}
        preview.setStyle("-fx-text-fill: #a78bfa;");

        Label name = new Label(f.name());
        name.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11px;");
        name.setWrapText(true);
        name.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        String unlockInfo = "LEVEL".equals(f.unlockType())
                ? "Level " + f.levelRequired()
                : f.coinCost() + " 🪙";

        Button actionBtn;
        if (active) {
            actionBtn = new Button("✓ Active");
            actionBtn.setStyle("-fx-background-color: rgba(124,58,237,0.3); -fx-text-fill: #a78bfa;" +
                               "-fx-background-radius: 8; -fx-font-size: 11px;");
            actionBtn.setDisable(true);
        } else if (owned) {
            actionBtn = new Button("Equip");
            actionBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white;" +
                               "-fx-background-radius: 8; -fx-font-size: 11px; -fx-cursor: hand;");
            actionBtn.setOnAction(e -> {
                StoreService.getInstance().equipFont(username, f.id());
                MainUI.showFontStore();
            });
        } else if (levelOk || canAfford) {
            String btnLabel = "COINS".equals(f.unlockType()) ? "Buy " + f.coinCost() + " 🪙" : "Unlock";
            actionBtn = new Button(btnLabel);
            actionBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white;" +
                               "-fx-background-radius: 8; -fx-font-size: 11px; -fx-cursor: hand;");
            actionBtn.setOnAction(e -> {
                PurchaseResult pr = StoreService.getInstance().buyFont(username, f.id());
                if (pr == PurchaseResult.SUCCESS) {
                    StoreService.getInstance().equipFont(username, f.id());
                }
                MainUI.showFontStore();
            });
        } else {
            actionBtn = new Button("🔒 " + unlockInfo);
            actionBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #64748b;" +
                               "-fx-background-radius: 8; -fx-font-size: 11px;");
            actionBtn.setDisable(true);
        }
        actionBtn.setMaxWidth(Double.MAX_VALUE);

        card.getChildren().addAll(preview, name, actionBtn);
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
