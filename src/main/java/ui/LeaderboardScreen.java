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
 * Leaderboard screen — shows top 20 scores from all players.
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

        VBox content = new VBox(20);
        content.setStyle("-fx-padding: 36 40 36 40;");

        Label title = new Label("🏆  Global Leaderboard");
        title.getStyleClass().add("label-title");

        Label sub = new Label("Top 20 results across all players");
        sub.getStyleClass().add("label-muted");

        VBox rankList = new VBox(10);

        List<GameResult> top = MongoDBManager.getInstance().getLeaderboard(20);
        String me = SessionManager.getInstance().getUsername();

        if (top.isEmpty()) {
            Label empty = new Label("No results yet. Play a game to appear here!");
            empty.getStyleClass().add("label-body");
            rankList.getChildren().add(empty);
        } else {
            for (int i = 0; i < top.size(); i++) {
                rankList.getChildren().add(buildRankRow(i + 1, top.get(i), me));
            }
        }

        content.getChildren().addAll(title, sub, gap(8), rankList);
        scroll.setContent(content);
        return scroll;
    }

    private HBox buildRankRow(int rank, GameResult result, String me) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 14 20 14 20;");

        // Rank badge
        String rankStyle = switch (rank) {
            case 1 -> "rank-gold";
            case 2 -> "rank-silver";
            case 3 -> "rank-bronze";
            default -> result.getUsername().equals(me) ? "rank-me" : "";
        };
        row.getStyleClass().add(rankStyle);

        Label rankLabel = new Label(rankEmoji(rank) + "  #" + rank);
        rankLabel.setStyle("-fx-text-fill: " + rankColor(rank) + "; -fx-font-size: 15px; -fx-font-weight: bold; -fx-min-width: 70;");

        Label userLabel = new Label(result.getUsername());
        userLabel.setStyle("-fx-text-fill: " + (result.getUsername().equals(me) ? "#a78bfa" : "white") +
                "; -fx-font-size: 14px; -fx-font-weight: bold; -fx-min-width: 160;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label wpmLabel = new Label(String.format("%.0f WPM", result.getWpm()));
        wpmLabel.setStyle("-fx-text-fill: #06b6d4; -fx-font-size: 15px; -fx-font-weight: bold; -fx-min-width: 100; -fx-alignment: center-right;");

        Label accLabel = new Label(String.format("%.0f%% acc", result.getAccuracy()));
        accLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 13px; -fx-min-width: 90;");

        Label modeLabel = new Label(result.getGameMode() + " / " + result.getDifficulty());
        modeLabel.getStyleClass().add("label-muted");
        modeLabel.setStyle(modeLabel.getStyle() + "-fx-min-width: 110;");

        row.getChildren().addAll(rankLabel, userLabel, spacer, wpmLabel, accLabel, modeLabel);
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
