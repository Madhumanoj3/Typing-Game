package ui;

import db.MongoDBManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.Achievement;
import model.GameResult;
import model.Lesson;

import java.util.Collections;
import java.util.List;

/**
 * JavaFX Application entry point.
 * Hosts a single Stage and swaps scenes between screens.
 */
public class MainUI extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        stage.setTitle("TypeMaster");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setWidth(1200);
        stage.setHeight(750);

        // Load the stylesheet once; all scenes share it
        showLogin();
        stage.show();
    }

    // ── Scene navigation ──────────────────────────────────────────────────

    public static void showLogin() {
        LoginScreen login = new LoginScreen();
        setScene(login.buildScene());
    }

    public static void showDashboard() {
        DashboardScreen dashboard = new DashboardScreen();
        setScene(dashboard.buildScene());
    }

    public static void showGame(String mode, String difficulty, int timerSeconds) {
        GameScreen game = new GameScreen(mode, difficulty, timerSeconds);
        setScene(game.buildScene());
    }

    public static void showResult(GameResult result) {
        showResult(result, 0, Collections.emptyList());
    }

    public static void showResult(GameResult result, int xpGained, List<Achievement> newAchievements) {
        ResultScreen res = new ResultScreen(result, xpGained, newAchievements);
        setScene(res.buildScene());
    }

    public static void showLeaderboard() {
        LeaderboardScreen lb = new LeaderboardScreen();
        setScene(lb.buildScene());
    }

    public static void showStats() {
        StatsScreen stats = new StatsScreen();
        setScene(stats.buildScene());
    }

    public static void showLesson(Lesson lesson) {
        LessonViewScreen view = new LessonViewScreen(lesson);
        setScene(view.buildScene());
    }

    public static void showSubscription() {
        SubscriptionScreen sub = new SubscriptionScreen();
        setScene(sub.buildScene());
    }

    public static void showAchievements() {
        AchievementsScreen ach = new AchievementsScreen();
        setScene(ach.buildScene());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static void setScene(Scene scene) {
        primaryStage.setScene(scene);
    }

    public static Stage getStage() {
        return primaryStage;
    }

    @Override
    public void stop() {
        try {
            MongoDBManager.getInstance().close();
        } catch (Exception ignored) { }
    }

    // ── Main ──────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        launch(args);
    }
}
