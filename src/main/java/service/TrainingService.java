package service;

import db.LessonDAO;
import db.SubscriptionDAO;
import db.TrainingProgressDAO;
import model.Lesson;
import model.TrainingProgress;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Business logic for the training module.
 * Decides which lessons are accessible and persists lesson completion data.
 */
public class TrainingService {

    private static TrainingService instance;

    private final LessonDAO          lessonDAO;
    private final SubscriptionDAO    subscriptionDAO;
    private final TrainingProgressDAO progressDAO;

    private TrainingService() {
        lessonDAO       = LessonDAO.getInstance();
        subscriptionDAO = SubscriptionDAO.getInstance();
        progressDAO     = TrainingProgressDAO.getInstance();
    }

    public static TrainingService getInstance() {
        if (instance == null) instance = new TrainingService();
        return instance;
    }

    // ── Lesson access ─────────────────────────────────────────────────────

    public List<Lesson> getAllLessons() {
        return lessonDAO.getAll();
    }

    /** Returns true when the user may open the given lesson. */
    public boolean canAccess(String username, Lesson lesson) {
        if (!lesson.isPremium()) return true;
        return subscriptionDAO.isPremium(username);
    }

    public boolean isPremiumUser(String username) {
        return subscriptionDAO.isPremium(username);
    }

    // ── Progress ──────────────────────────────────────────────────────────

    /** Returns a map of lessonId → TrainingProgress for quick look-up by the UI. */
    public Map<String, TrainingProgress> getProgressMap(String username) {
        return progressDAO.getForUser(username).stream()
                .collect(Collectors.toMap(TrainingProgress::getLessonId, p -> p));
    }

    public List<TrainingProgress> getProgress(String username) {
        return progressDAO.getForUser(username);
    }

    /**
     * Records a lesson attempt. Updates best WPM / accuracy and marks complete
     * when accuracy >= 70%.
     */
    public void recordAttempt(String username, Lesson lesson, double wpm, double accuracy) {
        recordAttempt(username, lesson, wpm, accuracy, Map.of());
    }

    public void recordAttempt(String username, Lesson lesson, double wpm, double accuracy,
                              Map<String, Integer> keyErrors) {
        TrainingProgress existing = progressDAO.getForLesson(username, lesson.getLessonId());
        TrainingProgress p = existing != null ? existing
                : new TrainingProgress(username, lesson.getLessonId(), lesson.getTitle());

        p.setAttempts(p.getAttempts() + 1);
        p.setLastAttemptAt(LocalDateTime.now());

        if (wpm > p.getBestWpm())           p.setBestWpm(wpm);
        if (accuracy > p.getBestAccuracy()) p.setBestAccuracy(accuracy);
        mergeKeyErrors(p, keyErrors);
        p.setTypingStyle(analyzeTypingStyle(wpm, accuracy));
        p.setRecommendation(buildRecommendation(p));

        if (!p.isCompleted() && accuracy >= 70.0) {
            p.setCompleted(true);
            p.setCompletedAt(LocalDateTime.now());
        }

        progressDAO.upsert(p);
    }

    public Map<String, Integer> aggregateKeyErrors(String username) {
        Map<String, Integer> totals = new HashMap<>();
        for (TrainingProgress p : progressDAO.getForUser(username)) {
            p.getKeyErrorCounts().forEach((key, count) -> totals.merge(key, count, Integer::sum));
        }
        return totals;
    }

    public String topTroubleKeys(String username, int limit) {
        return aggregateKeyErrors(username).entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(" "));
    }

    public String overallTypingStyle(String username) {
        List<TrainingProgress> progress = progressDAO.getForUser(username);
        if (progress.isEmpty()) return "Balanced";
        double wpm = progress.stream().mapToDouble(TrainingProgress::getBestWpm).average().orElse(0);
        double accuracy = progress.stream().mapToDouble(TrainingProgress::getBestAccuracy).average().orElse(0);
        return analyzeTypingStyle(wpm, accuracy);
    }

    public String targetedPracticeText(String username) {
        String keys = topTroubleKeys(username, 8);
        if (keys.isBlank()) {
            return "Focus on home-row control: asdf jkl; asdf jkl;";
        }
        return ("Target drill: " + keys + " " + keys.toLowerCase() + " " + keys.toUpperCase()).trim();
    }

    /** How many lessons the user has completed (used for progress bar). */
    public int countCompleted(String username) {
        return (int) progressDAO.getForUser(username).stream()
                .filter(TrainingProgress::isCompleted)
                .count();
    }

    public int totalLessons() {
        return lessonDAO.getAll().size();
    }

    private void mergeKeyErrors(TrainingProgress progress, Map<String, Integer> keyErrors) {
        Map<String, Integer> merged = new HashMap<>(progress.getKeyErrorCounts());
        if (keyErrors != null) {
            keyErrors.forEach((key, count) -> {
                if (key != null && !key.isBlank() && count != null && count > 0) {
                    merged.merge(key, count, Integer::sum);
                }
            });
        }
        progress.setKeyErrorCounts(merged);
    }

    private String analyzeTypingStyle(double wpm, double accuracy) {
        if (wpm >= 55 && accuracy < 88) return "Speed-first";
        if (accuracy >= 95 && wpm < 35) return "Accuracy-first";
        if (wpm >= 45 && accuracy >= 92) return "Fluent";
        return "Balanced";
    }

    private String buildRecommendation(TrainingProgress progress) {
        String troubleKeys = progress.getKeyErrorCounts().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(4)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(" "));
        if (!troubleKeys.isBlank()) return "Practice these keys slowly: " + troubleKeys;
        if ("Speed-first".equals(progress.getTypingStyle())) return "Slow down slightly and aim for 95% accuracy.";
        if ("Accuracy-first".equals(progress.getTypingStyle())) return "Try short timed bursts to build speed.";
        return "Keep practicing with steady rhythm.";
    }
}
