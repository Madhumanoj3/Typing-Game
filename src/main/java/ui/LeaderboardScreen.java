package ui;

import db.MongoDBManager;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.GameResult;
import util.SessionManager;

import java.util.List;

/**
 * Leaderboard screen — shows top 20 scores separately for Normal and Timer modes.
 * Practice and Training sessions are excluded.
 */
public class LeaderboardScreen {

    public Scene buildScene() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0f0f1a;");
        root.setCenter(buildContent());

        Scene scene = new Scene(root, 1200, 750);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        return scene;
    }

    /** Returns just the content node so DashboardScreen can embed it inline. */
    public Node buildContent() {
        ScrollPane scroll = new ScrollPane();
        scroll.getStyleClass().add("scroll-dark");
        scroll.setFitToWidth(true);

        VBox content = new VBox(32);
        content.setStyle("-fx-padding: 36 40 36 40;");

        Label title = new Label("🏆  Global Leaderboard");
        title.getStyleClass().add("label-title");

        Label sub = new Label("Top 20 results for Normal and Timer modes across all players");
        sub.getStyleClass().add("label-muted");

        String me = SessionManager.getInstance().getUsername();

        // ── Normal Mode Section ───────────────────────────────────────────
        VBox normalSection = buildModeSection("Normal Mode", "🎯", "#a78bfa", "Normal", me);

        // ── Timer Mode Section ────────────────────────────────────────────
        VBox timerSection = buildModeSection("Timer Mode", "⏱", "#fbbf24", "Timer", me);

        content.getChildren().addAll(title, sub, gap(4), normalSection, timerSection);
        scroll.setContent(content);
        return scroll;
    }

    private VBox buildModeSection(String sectionTitle, String icon, String accentColor,
                                  String mode, String me) {
        VBox section = new VBox(10);
        section.setStyle(
            "-fx-background-color: #1a1a2e;" +
            "-fx-background-radius: 16;" +
            "-fx-padding: 24;");

        // Section header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 20px;");
        Label sectionLabel = new Label(sectionTitle);
        sectionLabel.setStyle(
            "-fx-text-fill: " + accentColor + ";" +
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;");
        header.getChildren().addAll(iconLabel, sectionLabel);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #1e293b;");

        VBox rankList = new VBox(6);

        List<GameResult> top = MongoDBManager.getInstance().getLeaderboardByMode(20, mode);

        if (top.isEmpty()) {
            Label empty = new Label("No " + mode + " results yet. Play a game to appear here!");
            empty.getStyleClass().add("label-body");
            rankList.getChildren().add(empty);
        } else {
            for (int i = 0; i < top.size(); i++) {
                rankList.getChildren().add(buildRankRow(i + 1, top.get(i), me, accentColor));
            }
        }

        section.getChildren().addAll(header, sep, rankList);
        return section;
    }

    private HBox buildRankRow(int rank, GameResult result, String me, String accentColor) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 10 12 10 12;");

        String rankStyle = switch (rank) {
            case 1 -> "rank-gold";
            case 2 -> "rank-silver";
            case 3 -> "rank-bronze";
            default -> result.getUsername().equals(me) ? "rank-me" : "";
        };
        row.getStyleClass().add(rankStyle);

        Label rankLabel = new Label(rankEmoji(rank) + "  #" + rank);
        rankLabel.setStyle(
            "-fx-text-fill: " + rankColor(rank) + ";" +
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;" +
            "-fx-min-width: 70;");

        Label userLabel = new Label(result.getUsername());
        userLabel.setStyle(
            "-fx-text-fill: " + (result.getUsername().equals(me) ? accentColor : "white") + ";" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-min-width: 160;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label wpmLabel = new Label(String.format("%.0f WPM", result.getWpm()));
        wpmLabel.setStyle(
            "-fx-text-fill: #06b6d4;" +
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;" +
            "-fx-min-width: 100;" +
            "-fx-alignment: center-right;");

        Label accLabel = new Label(String.format("%.0f%% acc", result.getAccuracy()));
        accLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 13px; -fx-min-width: 90;");

        Label diffLabel = new Label(result.getDifficulty());
        diffLabel.getStyleClass().add("label-muted");
        diffLabel.setStyle(diffLabel.getStyle() + "-fx-min-width: 80;");

        row.getChildren().addAll(rankLabel, userLabel, spacer, wpmLabel, accLabel, diffLabel);
        return row;
    }

    private String rankEmoji(int rank) {
        return switch (rank) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> "  ";
        };
    }

    private String rankColor(int rank) {
        return switch (rank) {
            case 1 -> "#f59e0b";
            case 2 -> "#94a3b8";
            case 3 -> "#cd7f32";
            default -> "#cbd5e1";
        };
    }

    private Region gap(int h) {
        Region r = new Region();
        r.setMinHeight(h);
        return r;
    }
}
