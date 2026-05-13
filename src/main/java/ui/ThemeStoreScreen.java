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
        boolean isLight = ThemeManager.getInstance().isPrintLightTheme();
        String bgColor  = isLight ? "#f5f5f5" : "#0f0f1a";

        VBox layout = new VBox(0);
        layout.setStyle("-fx-background-color: " + bgColor + ";");
        layout.getChildren().addAll(buildHeader(isLight), buildContent(isLight));
        VBox.setVgrow((Node) layout.getChildren().get(1), Priority.ALWAYS);

        javafx.scene.Scene scene = new javafx.scene.Scene(layout, 1200, 750);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        return scene;
    }

    private HBox buildHeader(boolean isLight) {
        HBox header = new HBox(16);
        header.getStyleClass().add("header-bar");
        header.setAlignment(Pos.CENTER_LEFT);
        Button back = new Button("← Dashboard");
        back.getStyleClass().add("btn-secondary");
        back.setOnAction(e -> MainUI.showDashboard());
        Label title = new Label("🎨  Theme Store");
        title.setStyle("-fx-text-fill: " + (isLight ? "#5b21b6" : "#a78bfa") +
                "; -fx-font-size: 15px; -fx-font-weight: bold;");
        header.getChildren().addAll(back, title);
        return header;
    }

    public Node buildContent(boolean isLight) {
        ScrollPane scroll = new ScrollPane(buildInner(isLight));
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-dark");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    // Legacy overload keeps external callers working
    public Node buildContent() {
        return buildContent(ThemeManager.getInstance().isPrintLightTheme());
    }

    private VBox buildInner(boolean isLight) {
        String username = SessionManager.getInstance().getUsername();

        UserStats stats = null;
        try { stats = GamificationService.getInstance().getStats(username); }
        catch (Exception ignored) {}
        int coins = stats != null ? stats.getCoins() : 0;
        int level = stats != null ? stats.getLevel()  : 1;

        String bg          = isLight ? "#f5f5f5"  : "#0f0f1a";
        String textPrimary = isLight ? "#111827"  : "white";
        String textMuted   = isLight ? "#374151"  : "#64748b";
        String textYellow  = isLight ? "#d97706"  : "#fbbf24";
        String sectionPurple = isLight ? "#5b21b6" : "#a78bfa";
        String sectionGold   = isLight ? "#b45309" : "#fbbf24";

        VBox root = new VBox(24);
        root.setStyle("-fx-padding: 36 40 36 40; -fx-background-color: " + bg + ";");

        // ── Header ────────────────────────────────────────────────────────
        Label title = new Label("🎨  Theme Store");
        title.setStyle("-fx-text-fill: " + textPrimary + "; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label sub = new Label("Unlock new themes by levelling up or spending coins");
        sub.setStyle("-fx-text-fill: " + textMuted + "; -fx-font-size: 13px;");

        HBox coinsBadge = new HBox(6);
        coinsBadge.setAlignment(Pos.CENTER_LEFT);
        Label coinIcon  = new Label("🪙");
        coinIcon.setStyle("-fx-font-size: 16px;");
        Label coinCount = new Label(coins + " coins available");
        coinCount.setStyle("-fx-text-fill: " + textYellow + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        coinsBadge.getChildren().addAll(coinIcon, coinCount);

        root.getChildren().addAll(title, sub, coinsBadge, buildSep(isLight));

        // ── Level-unlocked section ────────────────────────────────────────
        Label levelHeader = new Label("🔓  Level Unlocks");
        levelHeader.setStyle("-fx-text-fill: " + sectionPurple + "; -fx-font-size: 15px; -fx-font-weight: bold;");
        root.getChildren().add(levelHeader);

        FlowPane levelGrid = new FlowPane(12, 12);
        for (ThemeDef t : ThemeManager.ALL_THEMES) {
            if ("LEVEL".equals(t.unlockType()))
                levelGrid.getChildren().add(buildThemeCard(t, username, level, coins, isLight));
        }
        root.getChildren().add(levelGrid);

        root.getChildren().add(buildSep(isLight));

        // ── Coin-purchase section ─────────────────────────────────────────
        Label coinHeader = new Label("🪙  Buy with Coins");
        coinHeader.setStyle("-fx-text-fill: " + sectionGold + "; -fx-font-size: 15px; -fx-font-weight: bold;");
        root.getChildren().add(coinHeader);

        FlowPane coinGrid = new FlowPane(12, 12);
        for (ThemeDef t : ThemeManager.ALL_THEMES) {
            if ("COINS".equals(t.unlockType()))
                coinGrid.getChildren().add(buildThemeCard(t, username, level, coins, isLight));
        }
        root.getChildren().add(coinGrid);

        return root;
    }

    private VBox buildThemeCard(ThemeDef t, String username, int level, int coins, boolean isLight) {
        boolean owned    = StoreService.getInstance().getInventory(username).hasTheme(t.id());
        boolean active   = ThemeManager.getInstance().getActiveThemeId().equals(t.id());
        boolean canAfford = "COINS".equals(t.unlockType()) && coins >= t.coinCost();
        boolean levelOk   = "LEVEL".equals(t.unlockType()) && level >= t.levelRequired();
        boolean lightCard = ThemeManager.PRINT_LIGHT_THEME_ID.equals(t.id());

        // In light mode, derive a pastel preview background so text stays readable
        String cardBg     = isLight ? lightenCardColor(t.card()) : t.card();
        // In light mode (or the print_light card itself) always use dark text
        String nameFill   = (isLight || lightCard) ? "#111827" : "#ffffff";
        String borderClr  = active   ? t.accent()
                          : lightCard ? "#f9a8d4"
                          : isLight   ? "#d1d5db"
                          : "transparent";

        VBox card = new VBox(10);
        card.setPrefWidth(160);
        card.setAlignment(Pos.CENTER);
        card.setStyle(
            "-fx-background-color: " + cardBg + ";" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 16;" +
            "-fx-border-color: " + borderClr + ";" +
            "-fx-border-radius: 14;" +
            "-fx-border-width: 2;");

        // Colour swatch — always shows the theme's own palette
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
        name.setStyle("-fx-text-fill: " + nameFill + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        name.setWrapText(true);
        name.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Status / action button
        String unlockInfo = "LEVEL".equals(t.unlockType())
                ? "Level " + t.levelRequired()
                : t.coinCost() + " 🪙";

        Button actionBtn;
        if (active) {
            actionBtn = new Button("✓ Equipped");
            actionBtn.setStyle(
                    "-fx-background-color: " + (isLight ? "rgba(124,58,237,0.12)" : "rgba(124,58,237,0.3)") + ";" +
                    "-fx-text-fill: " + (isLight ? "#5b21b6" : "#a78bfa") + ";" +
                    "-fx-background-radius: 8; -fx-font-size: 11px;");
            actionBtn.setDisable(true);
        } else if (owned) {
            actionBtn = new Button("Equip");
            actionBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: #ffffff;" +
                               "-fx-background-radius: 8; -fx-font-size: 11px; -fx-cursor: hand;");
            actionBtn.setOnAction(e -> {
                StoreService.getInstance().equipTheme(username, t.id());
                MainUI.showThemeStore();
            });
        } else if (levelOk || canAfford) {
            String btnLabel = "COINS".equals(t.unlockType()) ? "Buy " + t.coinCost() + " 🪙" : "Unlock";
            actionBtn = new Button(btnLabel);
            actionBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: #ffffff;" +
                               "-fx-background-radius: 8; -fx-font-size: 11px; -fx-cursor: hand;");
            actionBtn.setOnAction(e -> {
                PurchaseResult pr = StoreService.getInstance().buyTheme(username, t.id());
                if (pr == PurchaseResult.SUCCESS) {
                    StoreService.getInstance().equipTheme(username, t.id());
                }
                MainUI.showThemeStore();
            });
        } else {
            actionBtn = new Button("🔒 " + unlockInfo);
            actionBtn.setStyle("-fx-background-color: " + (isLight ? "#e5e7eb" : "#1e293b") + ";" +
                               "-fx-text-fill: " + (isLight ? "#6b7280" : "#64748b") + ";" +
                               "-fx-background-radius: 8; -fx-font-size: 11px;");
            actionBtn.setDisable(true);
        }
        actionBtn.setMaxWidth(Double.MAX_VALUE);

        card.getChildren().addAll(swatch, name, actionBtn);
        return card;
    }

    /** Mix the given dark hex ~85% toward white to produce a pastel preview tile. */
    private static String lightenCardColor(String hex) {
        if (hex == null || hex.length() < 7) return "#f0f0f0";
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            r = Math.min(255, (int)(r * 0.15 + 240));
            g = Math.min(255, (int)(g * 0.15 + 235));
            b = Math.min(255, (int)(b * 0.15 + 240));
            return String.format("#%02x%02x%02x", r, g, b);
        } catch (Exception e) {
            return "#f0f0f0";
        }
    }

    private Region buildSep(boolean isLight) {
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color: " + (isLight ? "#d1d5db" : "#1e293b") + ";");
        return sep;
    }

    // Legacy no-arg overloads kept for compatibility
    private Region buildSep() { return buildSep(false); }
}
