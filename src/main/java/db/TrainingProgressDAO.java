package db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import model.TrainingProgress;
import org.bson.Document;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Data access for the {@code training_progress} collection.
 * Uses upsert (username + lessonId as compound key) so progress accumulates.
 */
public class TrainingProgressDAO {

    private static TrainingProgressDAO instance;
    private final MongoCollection<Document> col;

    private TrainingProgressDAO() {
        col = MongoDBManager.getInstance().getDatabase().getCollection("training_progress");
    }

    public static TrainingProgressDAO getInstance() {
        if (instance == null) instance = new TrainingProgressDAO();
        return instance;
    }

    // ── Write ─────────────────────────────────────────────────────────────

    public void upsert(TrainingProgress p) {
        Document doc = new Document()
                .append("username",     p.getUsername())
                .append("lessonId",     p.getLessonId())
                .append("lessonTitle",  p.getLessonTitle())
                .append("completed",    p.isCompleted())
                .append("bestWpm",      p.getBestWpm())
                .append("bestAccuracy", p.getBestAccuracy())
                .append("attempts",     p.getAttempts())
                .append("lastAttemptAt", toDate(p.getLastAttemptAt()))
                .append("completedAt",  toDate(p.getCompletedAt()))
                .append("keyErrorCounts", new Document(p.getKeyErrorCounts()))
                .append("typingStyle", p.getTypingStyle())
                .append("recommendation", p.getRecommendation());

        col.replaceOne(
                Filters.and(
                        Filters.eq("username", p.getUsername()),
                        Filters.eq("lessonId", p.getLessonId())
                ),
                doc,
                new ReplaceOptions().upsert(true)
        );
    }

    // ── Read ──────────────────────────────────────────────────────────────

    public List<TrainingProgress> getForUser(String username) {
        List<TrainingProgress> list = new ArrayList<>();
        for (Document d : col.find(Filters.eq("username", username))) {
            list.add(toProgress(d));
        }
        return list;
    }

    public TrainingProgress getForLesson(String username, String lessonId) {
        Document d = col.find(Filters.and(
                Filters.eq("username", username),
                Filters.eq("lessonId", lessonId)
        )).first();
        return d == null ? null : toProgress(d);
    }

    // ── Mappers ───────────────────────────────────────────────────────────

    private TrainingProgress toProgress(Document d) {
        TrainingProgress p = new TrainingProgress();
        p.setUsername(d.getString("username"));
        p.setLessonId(d.getString("lessonId"));
        p.setLessonTitle(d.getString("lessonTitle"));
        p.setCompleted(Boolean.TRUE.equals(d.getBoolean("completed")));
        p.setBestWpm(numberAsDouble(d, "bestWpm"));
        p.setBestAccuracy(numberAsDouble(d, "bestAccuracy"));
        p.setAttempts(d.getInteger("attempts", 0));
        p.setLastAttemptAt(toLocal(d.getDate("lastAttemptAt")));
        p.setCompletedAt(toLocal(d.getDate("completedAt")));
        Document keyErrors = d.get("keyErrorCounts", Document.class);
        if (keyErrors != null) {
            java.util.Map<String, Integer> map = new java.util.HashMap<>();
            for (String key : keyErrors.keySet()) {
                Object value = keyErrors.get(key);
                if (value instanceof Number n) map.put(key, n.intValue());
            }
            p.setKeyErrorCounts(map);
        }
        p.setTypingStyle(d.getString("typingStyle") != null ? d.getString("typingStyle") : "Balanced");
        p.setRecommendation(d.getString("recommendation") != null
                ? d.getString("recommendation")
                : "Keep practicing with steady rhythm.");
        return p;
    }

    private Date toDate(LocalDateTime ldt) {
        if (ldt == null) return null;
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

    private LocalDateTime toLocal(Date d) {
        if (d == null) return null;
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private double numberAsDouble(Document doc, String key) {
        Object value = doc.get(key);
        return value instanceof Number n ? n.doubleValue() : 0.0;
    }
}
