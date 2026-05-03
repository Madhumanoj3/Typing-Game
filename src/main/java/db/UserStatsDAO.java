package db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import model.UserStats;
import org.bson.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserStatsDAO {

    private static UserStatsDAO instance;
    private final MongoCollection<Document> col;

    private UserStatsDAO() {
        MongoDatabase db = MongoDBManager.getInstance().getDatabase();
        col = db.getCollection("user_stats");
    }

    public static UserStatsDAO getInstance() {
        if (instance == null) instance = new UserStatsDAO();
        return instance;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────

    public UserStats getOrCreate(String username) {
        Document doc = col.find(Filters.eq("username", username)).first();
        if (doc == null) {
            UserStats stats = new UserStats(username);
            save(stats);
            return stats;
        }
        return docToStats(doc);
    }

    public void save(UserStats stats) {
        col.replaceOne(
            Filters.eq("username", stats.getUsername()),
            statsToDoc(stats),
            new ReplaceOptions().upsert(true)
        );
    }

    /** Returns all user stats sorted by level desc, then XP desc (for level rankings). */
    public List<UserStats> getAllSortedByLevel(int limit) {
        List<UserStats> list = new ArrayList<>();
        for (Document doc : col.find()
                .sort(Sorts.orderBy(Sorts.descending("level"), Sorts.descending("xp")))
                .limit(limit)) {
            list.add(docToStats(doc));
        }
        return list;
    }

    // ── Mappers ───────────────────────────────────────────────────────────

    private Document statsToDoc(UserStats s) {
        Document doc = new Document()
            .append("username",         s.getUsername())
            .append("xp",               s.getXp())
            .append("level",            s.getLevel())
            .append("streak",           s.getStreak())
            .append("dailyGamesPlayed", s.getDailyGamesPlayed())
            .append("dailyMaxWpm",      s.getDailyMaxWpm())
            .append("dailyCharsTyped",  s.getDailyCharsTyped())
            .append("coins",            s.getCoins());
        if (s.getLastActiveDate() != null)
            doc.append("lastActiveDate", s.getLastActiveDate().toString());
        if (s.getChallengeDate() != null)
            doc.append("challengeDate", s.getChallengeDate().toString());
        return doc;
    }

    private UserStats docToStats(Document doc) {
        UserStats s = new UserStats();
        s.setUsername(doc.getString("username"));
        s.setXp(doc.getInteger("xp", 0));
        s.setLevel(doc.getInteger("level", 1));
        s.setStreak(doc.getInteger("streak", 0));
        s.setDailyGamesPlayed(doc.getInteger("dailyGamesPlayed", 0));
        Object rawWpm = doc.get("dailyMaxWpm");
        s.setDailyMaxWpm(rawWpm instanceof Number ? ((Number) rawWpm).doubleValue() : 0.0);
        s.setDailyCharsTyped(doc.getInteger("dailyCharsTyped", 0));
        s.setCoins(doc.getInteger("coins", 0));

        String lastActive = doc.getString("lastActiveDate");
        if (lastActive != null) s.setLastActiveDate(LocalDate.parse(lastActive));
        String challengeDate = doc.getString("challengeDate");
        if (challengeDate != null) s.setChallengeDate(LocalDate.parse(challengeDate));
        return s;
    }
}
