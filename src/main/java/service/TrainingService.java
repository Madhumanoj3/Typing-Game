package service;

import db.LessonDAO;
import db.SubscriptionDAO;
import db.TrainingProgressDAO;
import model.Lesson;
import model.TrainingProgress;

import java.time.LocalDateTime;
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

    /**
     * Records a lesson attempt. Updates best WPM / accuracy and marks complete
     * when accuracy >= 70%.
     */
    public void recordAttempt(String username, Lesson lesson, double wpm, double accuracy) {
        TrainingProgress existing = progressDAO.getForLesson(username, lesson.getLessonId());
        TrainingProgress p = existing != null ? existing
                : new TrainingProgress(username, lesson.getLessonId(), lesson.getTitle());

        p.setAttempts(p.getAttempts() + 1);
        p.setLastAttemptAt(LocalDateTime.now());

        if (wpm > p.getBestWpm())           p.setBestWpm(wpm);
        if (accuracy > p.getBestAccuracy()) p.setBestAccuracy(accuracy);

        if (!p.isCompleted() && accuracy >= 70.0) {
            p.setCompleted(true);
            p.setCompletedAt(LocalDateTime.now());
        }

        progressDAO.upsert(p);
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
}