package ui;

import db.MongoDBManager;
import game.ThemeManager;
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
        ThemeManager.getInstance().setTheme(ThemeManager.DEFAULT_DARK_THEME_ID);
        LoginScreen login = new LoginScreen();
        setScene(login.buildScene());
    }

    public static void showVideoIntro() {
        VideoIntroScreen intro = new VideoIntroScreen();
        setScene(intro.buildScene());
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
        showResult(result, 0, 0, Collections.emptyList());
    }

    public static void showResult(GameResult result, int xpGained, List<Achievement> newAchievements) {
        showResult(result, xpGained, 0, newAchievements);
    }

    public static void showResult(GameResult result, int xpGained, int coinsGained,
                                  List<Achievement> newAchievements) {
        ResultScreen res = new ResultScreen(result, xpGained, coinsGained, newAchievements);
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

    public static void showThemeStore() {
        ThemeStoreScreen store = new ThemeStoreScreen();
        setScene(store.buildScene());
    }

    public static void showFontStore() {
        FontStoreScreen store = new FontStoreScreen();
        setScene(store.buildScene());
    }

    public static void showLevelUp(int newLevel, model.UserStats stats) {
        LevelUpScreen levelUp = new LevelUpScreen(newLevel, stats);
        setScene(levelUp.buildScene());
    }

    public static void showAdminPanel() {
        ui.admin.AdminDashboardScreen admin = new ui.admin.AdminDashboardScreen();
        setScene(admin.buildScene());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static void setScene(Scene scene) {
        ThemeManager.applyTheme(scene);
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
