package service;

import db.AchievementDAO;
import db.UserStatsDAO;
import model.Achievement;
import model.GameResult;
import model.UserStats;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Core gamification service: XP, levels, streaks, achievements, and daily challenges.
 */
public class GamificationService {

    // ── Result record returned after processing a game ────────────────────
    public record GamificationResult(int xpGained, int coinsGained, List<Achievement> newAchievements) {}

    // ── Daily challenge progress snapshot ────────────────────────────────
    public record DailyChallengeProgress(int gamesPlayed, double maxWpm, int charsTyped) {
        public boolean isGamesComplete() { return gamesPlayed >= 2; }
        public boolean isWpmComplete()   { return maxWpm      >= 40; }
        public boolean isCharsComplete() { return charsTyped  >= 500; }
        public boolean isAllComplete()   { return isGamesComplete() && isWpmComplete() && isCharsComplete(); }
    }

    // ── All defined achievements (for the achievements screen) ────────────
    public record AchievementDef(String badge, String title, String description, String icon) {}

    public static final List<AchievementDef> ALL_ACHIEVEMENTS = List.of(
        new AchievementDef("FIRST_GAME",     "First Game!",       "Completed your first typing game",     "🎮"),
        new AchievementDef("WPM_50",         "Speed Demon",       "Achieved 50 WPM in a single game",     "⚡"),
        new AchievementDef("WPM_80",         "Typing Master",     "Achieved 80 WPM in a single game",     "🔥"),
        new AchievementDef("WPM_100",        "Keyboard Ninja",    "Achieved 100 WPM in a single game",    "🥷"),
        new AchievementDef("ACCURACY_95",    "Precision Pro",     "Achieved 95% accuracy in a game",      "🎯"),
        new AchievementDef("ACCURACY_100",   "Perfect Typist",    "Achieved 100% accuracy in a game",     "💎"),
        new AchievementDef("STREAK_3",       "On a Roll",         "Maintained a 3-day typing streak",     "🔥"),
        new AchievementDef("STREAK_5",       "Dedicated Typist",  "Maintained a 5-day typing streak",     "⭐"),
        new AchievementDef("STREAK_7",       "Week Warrior",      "Maintained a 7-day typing streak",     "🏆"),
        new AchievementDef("LEVEL_5",        "Rising Star",       "Reached Level 5",                      "🌟"),
        new AchievementDef("LEVEL_10",       "Expert Typist",     "Reached Level 10",                     "👑"),
        new AchievementDef("LESSON_1",       "First Lesson",      "Completed your first training lesson", "📖"),
        new AchievementDef("LESSON_5",       "Diligent Student",  "Completed 5 training lessons",         "📚"),
        new AchievementDef("LESSON_10",      "Course Champion",   "Completed all 10 lessons",             "🎓"),
        new AchievementDef("DAILY_COMPLETE", "Daily Champion",    "Completed all daily challenges today", "📅")
    );

    // ── Singleton ─────────────────────────────────────────────────────────

    private static GamificationService instance;
    private final UserStatsDAO   statsDAO;
    private final AchievementDAO achievementDAO;

    private GamificationService() {
        statsDAO       = UserStatsDAO.getInstance();
        achievementDAO = AchievementDAO.getInstance();
    }

    public static GamificationService getInstance() {
        if (instance == null) instance = new GamificationService();
        return instance;
    }

    // ── XP / Level formulas ───────────────────────────────────────────────

    /**
     * XP earned from a single game session.
     * Divided by 100 so an average player (~40 WPM, ~85% acc = 34 XP)
     * needs roughly 3 games to gain 1 level (100 XP threshold).
     */
    public int calculateXP(double wpm, double accuracy) {
        return (int) ((wpm * accuracy) / 100.0);
    }

    /** Level derived from cumulative XP (minimum 1). */
    public int calculateLevel(int totalXP) {
        return Math.max(1, totalXP / 100);
    }

    /**
     * Progress fraction (0.0–1.0) through the current level.
     * Each level requires 100 XP.
     */
    public double levelProgress(int xp) {
        int levelBase = (calculateLevel(xp) - 1) * 100;
        return (double)(xp - levelBase) / 100.0;
    }

    /** XP remaining until the next level boundary. */
    public int xpToNextLevel(int xp) {
        int nextBoundary = calculateLevel(xp) * 100;
        return nextBoundary - xp;
    }

    // ── Core: process a completed game session ────────────────────────────

    /**
     * Called once per completed game. Updates XP, level, streak, daily
     * challenge counters, and checks for new achievements.
     *
     * @return GamificationResult carrying XP gained and any newly earned achievements.
     */
    public GamificationResult processGameResult(String username, GameResult result) {
        UserStats stats = statsDAO.getOrCreate(username);
        List<Achievement> newAchievements = new ArrayList<>();

        int xpGained = calculateXP(result.getWpm(), result.getAccuracy());

        // Coin rewards: Normal/Timer modes only, accuracy > 80%
        int coinsGained = 0;
        String gameMode = result.getGameMode();
        if (("Normal".equals(gameMode) || "Timer".equals(gameMode)) && result.getAccuracy() > 80) {
            coinsGained = switch (result.getDifficulty()) {
                case "Easy"   -> 2;
                case "Medium" -> 3;
                case "Hard"   -> 5;
                default       -> 0;
            };
            if (result.getAccuracy() >= 100.0) coinsGained *= 2;
        }
        stats.setCoins(stats.getCoins() + coinsGained);

        // Reset daily challenge counters when the calendar day rolls over
        LocalDate today = LocalDate.now();
        if (!today.equals(stats.getChallengeDate())) {
            stats.setDailyGamesPlayed(0);
            stats.setDailyMaxWpm(0.0);
            stats.setDailyCharsTyped(0);
            stats.setChallengeDate(today);
        }

        // Apply XP and derive level
        stats.setXp(stats.getXp() + xpGained);
        stats.setLevel(calculateLevel(stats.getXp()));

        // Update login/play streak
        updateStreak(stats);

        // Accumulate daily challenge progress
        stats.setDailyGamesPlayed(stats.getDailyGamesPlayed() + 1);
        stats.setDailyMaxWpm(Math.max(stats.getDailyMaxWpm(), result.getWpm()));
        // Approximate chars typed: words × average 5 chars/word
        stats.setDailyCharsTyped(stats.getDailyCharsTyped() + result.getWordsTyped() * 5);

        statsDAO.save(stats);

        // Check and grant game-based achievements
        newAchievements.addAll(checkGameAchievements(username, stats, result));

        // Bonus XP + coins when all daily challenges are done for the first time today
        DailyChallengeProgress dp = getDailyProgress(username);
        if (dp.isAllComplete()) {
            Achievement bonus = grantIfNew(username, "DAILY_COMPLETE",
                "Daily Champion", "Completed all daily challenges today");
            if (bonus != null) {
                newAchievements.add(bonus);
                stats.setXp(stats.getXp() + 20);
                stats.setCoins(stats.getCoins() + 10);
                xpGained += 20;
                coinsGained += 10;
                statsDAO.save(stats);
            }
        }

        return new GamificationResult(xpGained, coinsGained, newAchievements);
    }

    // ── Lesson achievements (called from LessonViewScreen) ───────────────

    public void checkLessonAchievements(String username, int completedCount) {
        if (completedCount >= 1)
            grantIfNew(username, "LESSON_1",  "First Lesson",     "Completed your first training lesson");
        if (completedCount >= 5)
            grantIfNew(username, "LESSON_5",  "Diligent Student", "Completed 5 training lessons");
        if (completedCount >= 10)
            grantIfNew(username, "LESSON_10", "Course Champion",  "Completed all 10 lessons");
    }

    // ── Queries ───────────────────────────────────────────────────────────

    public UserStats getStats(String username) {
        return statsDAO.getOrCreate(username);
    }

    public List<Achievement> getAchievements(String username) {
        return achievementDAO.getForUser(username);
    }

    /**
     * Returns today's daily challenge progress (zeroed out if today is a new day
     * and the user has not played yet).
     */
    public DailyChallengeProgress getDailyProgress(String username) {
        UserStats stats = statsDAO.getOrCreate(username);
        LocalDate today = LocalDate.now();
        if (!today.equals(stats.getChallengeDate())) {
            return new DailyChallengeProgress(0, 0.0, 0);
        }
        return new DailyChallengeProgress(
            stats.getDailyGamesPlayed(),
            stats.getDailyMaxWpm(),
            stats.getDailyCharsTyped()
        );
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    private void updateStreak(UserStats stats) {
        LocalDate today = LocalDate.now();
        LocalDate last  = stats.getLastActiveDate();

        if (last == null) {
            stats.setStreak(1);
        } else if (last.equals(today)) {
            // already recorded for today — don't double-count
        } else if (last.equals(today.minusDays(1))) {
            stats.setStreak(stats.getStreak() + 1);
        } else {
            stats.setStreak(1); // streak broken
        }
        stats.setLastActiveDate(today);
    }

    private List<Achievement> checkGameAchievements(String username,
                                                     UserStats stats,
                                                     GameResult result) {
        List<Achievement> earned = new ArrayList<>();

        addIfNew(earned, grantIfNew(username, "FIRST_GAME",
            "First Game!", "Completed your first typing game"));

        if (result.getWpm() >= 50)
            addIfNew(earned, grantIfNew(username, "WPM_50",
                "Speed Demon", "Achieved 50 WPM in a single game"));
        if (result.getWpm() >= 80)
            addIfNew(earned, grantIfNew(username, "WPM_80",
                "Typing Master", "Achieved 80 WPM in a single game"));
        if (result.getWpm() >= 100)
            addIfNew(earned, grantIfNew(username, "WPM_100",
                "Keyboard Ninja", "Achieved 100 WPM in a single game"));

        if (result.getAccuracy() >= 95)
            addIfNew(earned, grantIfNew(username, "ACCURACY_95",
                "Precision Pro", "Achieved 95% accuracy in a game"));
        if (result.getAccuracy() >= 100)
            addIfNew(earned, grantIfNew(username, "ACCURACY_100",
                "Perfect Typist", "Achieved 100% accuracy in a game"));

        if (stats.getStreak() >= 3)
            addIfNew(earned, grantIfNew(username, "STREAK_3",
                "On a Roll", "Maintained a 3-day typing streak"));
        if (stats.getStreak() >= 5)
            addIfNew(earned, grantIfNew(username, "STREAK_5",
                "Dedicated Typist", "Maintained a 5-day typing streak"));
        if (stats.getStreak() >= 7)
            addIfNew(earned, grantIfNew(username, "STREAK_7",
                "Week Warrior", "Maintained a 7-day typing streak"));

        if (stats.getLevel() >= 5)
            addIfNew(earned, grantIfNew(username, "LEVEL_5",
                "Rising Star", "Reached Level 5"));
        if (stats.getLevel() >= 10)
            addIfNew(earned, grantIfNew(username, "LEVEL_10",
                "Expert Typist", "Reached Level 10"));

        return earned;
    }

    private Achievement grantIfNew(String username, String badge, String title, String desc) {
        if (achievementDAO.hasAchievement(username, badge)) return null;
        Achievement a = new Achievement(username, badge, title, desc);
        achievementDAO.save(a);
        return a;
    }

    private void addIfNew(List<Achievement> list, Achievement a) {
        if (a != null) list.add(a);
    }
}
