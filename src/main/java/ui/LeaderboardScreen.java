package ui;

import db.MongoDBManager;
import db.UserStatsDAO;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.GameResult;
import model.UserStats;
import util.SessionManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Leaderboard screen — top 20 scores for Normal and Timer modes,
 * followed by a Level Rankings section showing all players ranked by level.
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

    /** Returns the content node so DashboardScreen can embed it inline. */
    public Node buildContent() {
        ScrollPane scroll = new ScrollPane();
        scroll.getStyleClass().add("scroll-dark");
        scroll.setFitToWidth(true);

        VBox content = new VBox(32);
        content.setStyle("-fx-padding: 36 40 36 40;");

        Label title = new Label("🏆  Global Leaderboard");
        title.getStyleClass().add("label-title");

        Label sub = new Label("Best score per player — Normal and Timer modes across all users");
        sub.getStyleClass().add("label-muted");

        String me = SessionManager.getInstance().getUsername();

        // ── Normal Mode Section ───────────────────────────────────────────
        VBox normalSection = buildModeSection("Normal Mode", "🎯", "#a78bfa", "Normal", me);

        // ── Timer Mode Section ────────────────────────────────────────────
        VBox timerSection = buildModeSection("Timer Mode", "⏱", "#fbbf24", "Timer", me);

        // ── Level Rankings Section ────────────────────────────────────────
        VBox levelSection = buildLevelRankSection(me);

        content.getChildren().addAll(title, sub, gap(4), normalSection, timerSection, levelSection);
        scroll.setContent(content);
        return scroll;
    }

    // ── WPM Mode Section ──────────────────────────────────────────────────

    private VBox buildModeSection(String sectionTitle, String icon, String accentColor,
                                  String mode, String me) {
        VBox section = new VBox(10);
        section.setStyle(
            "-fx-background-color: #1a1a2e;" +
            "-fx-background-radius: 16;" +
            "-fx-padding: 24;");

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
        // Fetch extra results then keep only each user's best (first = highest WPM after sort)
        List<GameResult> raw = MongoDBManager.getInstance().getLeaderboardByMode(200, mode);
        Map<String, GameResult> bestByUser = new LinkedHashMap<>();
        for (GameResult r : raw) {
            bestByUser.putIfAbsent(r.getUsername(), r);
        }
        List<GameResult> top = new ArrayList<>(bestByUser.values());
        if (top.size() > 20) top = top.subList(0, 20);

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

    // ── Level Rankings Section ────────────────────────────────────────────

    private VBox buildLevelRankSection(String me) {
        VBox section = new VBox(10);
        section.setStyle(
            "-fx-background-color: #1a1a2e;" +
            "-fx-background-radius: 16;" +
            "-fx-padding: 24;");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel = new Label("🌟");
        iconLabel.setStyle("-fx-font-size: 20px;");
        Label sectionLabel = new Label("Level Rankings");
        sectionLabel.setStyle(
            "-fx-text-fill: #a78bfa;" +
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label hint = new Label("Normal & Timer mode XP only");
        hint.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px;");

        header.getChildren().addAll(iconLabel, sectionLabel, spacer, hint);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #1e293b;");

        // Column headers
        HBox colHeaders = new HBox(16);
        colHeaders.setAlignment(Pos.CENTER_LEFT);
        colHeaders.setStyle("-fx-padding: 4 12 4 12;");
        Label rankH  = colLabel("#", 70);
        Label userH  = colLabel("Player", 160);
        Region s2    = new Region(); HBox.setHgrow(s2, Priority.ALWAYS);
        Label levelH = colLabel("Level", 120);
        Label xpH    = colLabel("Total XP", 110);
        Label streakH = colLabel("Streak", 90);
        Label titleH = colLabel("Title", 120);
        colHeaders.getChildren().addAll(rankH, userH, s2, levelH, xpH, streakH, titleH);

        VBox rankList = new VBox(6);
        List<UserStats> rankings;
        try {
            rankings = UserStatsDAO.getInstance().getAllSortedByLevel(50);
        } catch (Exception e) {
            rankings = List.of();
        }

        if (rankings.isEmpty()) {
            Label empty = new Label("No level data yet. Play Normal or Timer games to earn XP!");
            empty.getStyleClass().add("label-body");
            rankList.getChildren().add(empty);
        } else {
            for (int i = 0; i < rankings.size(); i++) {
                rankList.getChildren().add(buildLevelRankRow(i + 1, rankings.get(i), me));
            }
        }

        section.getChildren().addAll(header, sep, colHeaders, rankList);
        return section;
    }

    private HBox buildLevelRankRow(int rank, UserStats stats, String me) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 10 12 10 12;");

        String rankStyle = switch (rank) {
            case 1 -> "rank-gold";
            case 2 -> "rank-silver";
            case 3 -> "rank-bronze";
            default -> stats.getUsername().equals(me) ? "rank-me" : "";
        };
        row.getStyleClass().add(rankStyle);

        // Rank
        Label rankLabel = new Label(rankEmoji(rank) + "  #" + rank);
        rankLabel.setStyle(
            "-fx-text-fill: " + rankColor(rank) + ";" +
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;" +
            "-fx-min-width: 70;");

        // Username
        boolean isMe = stats.getUsername().equals(me);
        Label userLabel = new Label(stats.getUsername());
        userLabel.setStyle(
            "-fx-text-fill: " + (isMe ? "#a78bfa" : "white") + ";" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-min-width: 160;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Level badge with tier colour
        int level = stats.getLevel();
        Label levelLabel = new Label("Level " + level);
        levelLabel.setStyle(
            "-fx-background-color: " + levelBgColor(level) + ";" +
            "-fx-text-fill: " + levelTextColor(level) + ";" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 3 10 3 10;" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-min-width: 80;");

        // XP
        Label xpLabel = new Label(stats.getXp() + " XP");
        xpLabel.setStyle(
            "-fx-text-fill: #a78bfa;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-min-width: 80;");

        // Streak
        int streak = stats.getStreak();
        String streakIcon = streak >= 7 ? "🏆" : streak >= 3 ? "🔥" : "✨";
        Label streakLabel = new Label(streakIcon + " " + streak + "d");
        streakLabel.setStyle(
            "-fx-text-fill: #fbbf24;" +
            "-fx-font-size: 12px;" +
            "-fx-min-width: 60;");

        // Level title
        Label titleLabel = new Label(levelTitle(level));
        titleLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-min-width: 110;");

        row.getChildren().addAll(rankLabel, userLabel, spacer,
                levelLabel, xpLabel, streakLabel, titleLabel);
        return row;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Label colLabel(String text, int minWidth) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px;" +
                   "-fx-font-weight: bold; -fx-min-width: " + minWidth + ";");
        return l;
    }

    private String levelTitle(int level) {
        if (level >= 20) return "Grandmaster";
        if (level >= 15) return "Elite";
        if (level >= 10) return "Expert";
        if (level >= 5)  return "Advanced";
        if (level >= 3)  return "Intermediate";
        return "Beginner";
    }

    private String levelBgColor(int level) {
        if (level >= 20) return "rgba(245,158,11,0.25)";
        if (level >= 15) return "rgba(239,68,68,0.20)";
        if (level >= 10) return "rgba(124,58,237,0.25)";
        if (level >= 5)  return "rgba(6,182,212,0.20)";
        if (level >= 3)  return "rgba(16,185,129,0.20)";
        return "rgba(100,116,139,0.20)";
    }

    private String levelTextColor(int level) {
        if (level >= 20) return "#fbbf24";
        if (level >= 15) return "#ef4444";
        if (level >= 10) return "#a78bfa";
        if (level >= 5)  return "#06b6d4";
        if (level >= 3)  return "#10b981";
        return "#94a3b8";
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
