package db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import model.Achievement;
import org.bson.Document;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AchievementDAO {

    private static AchievementDAO instance;
    private final MongoCollection<Document> col;

    private AchievementDAO() {
        MongoDatabase db = MongoDBManager.getInstance().getDatabase();
        col = db.getCollection("achievements");
    }

    public static AchievementDAO getInstance() {
        if (instance == null) instance = new AchievementDAO();
        return instance;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────

    public boolean hasAchievement(String username, String badge) {
        return col.find(Filters.and(
            Filters.eq("username", username),
            Filters.eq("badge",    badge)
        )).first() != null;
    }

    public void save(Achievement a) {
        Document doc = new Document()
            .append("username",    a.getUsername())
            .append("badge",       a.getBadge())
            .append("title",       a.getTitle())
            .append("description", a.getDescription())
            .append("earnedAt",    new Date());
        col.insertOne(doc);
        a.setId(doc.getObjectId("_id"));
    }

    /** Returns all earned achievements for admin view. */
    public List<Achievement> getAll() {
        List<Achievement> list = new ArrayList<>();
        for (Document doc : col.find()) {
            Achievement a = new Achievement();
            a.setId(doc.getObjectId("_id"));
            a.setUsername(doc.getString("username"));
            a.setBadge(doc.getString("badge"));
            a.setTitle(doc.getString("title"));
            a.setDescription(doc.getString("description"));
            Date earned = doc.getDate("earnedAt");
            if (earned != null)
                a.setEarnedAt(earned.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            list.add(a);
        }
        return list;
    }

    public List<Achievement> getForUser(String username) {
        List<Achievement> list = new ArrayList<>();
        for (Document doc : col.find(Filters.eq("username", username))) {
            Achievement a = new Achievement();
            a.setId(doc.getObjectId("_id"));
            a.setUsername(doc.getString("username"));
            a.setBadge(doc.getString("badge"));
            a.setTitle(doc.getString("title"));
            a.setDescription(doc.getString("description"));
            Date earned = doc.getDate("earnedAt");
            if (earned != null)
                a.setEarnedAt(earned.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            list.add(a);
        }
        return list;
    }
}
