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
                .append("completedAt",  toDate(p.getCompletedAt()));

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
        p.setBestWpm(d.getDouble("bestWpm") != null ? d.getDouble("bestWpm") : 0);
        p.setBestAccuracy(d.getDouble("bestAccuracy") != null ? d.getDouble("bestAccuracy") : 0);
        p.setAttempts(d.getInteger("attempts", 0));
        p.setLastAttemptAt(toLocal(d.getDate("lastAttemptAt")));
        p.setCompletedAt(toLocal(d.getDate("completedAt")));
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
}