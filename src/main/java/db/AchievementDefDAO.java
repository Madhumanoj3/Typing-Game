package db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import model.AchievementDefinition;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

/**
 * Data access for the {@code achievement_defs} collection.
 * Seeds default achievement definitions on first connection from GamificationService constants.
 */
public class AchievementDefDAO {

    private static AchievementDefDAO instance;
    private final MongoCollection<Document> col;

    private AchievementDefDAO() {
        col = MongoDBManager.getInstance().getDatabase().getCollection("achievement_defs");
        seedDefaults();
    }

    public static AchievementDefDAO getInstance() {
        if (instance == null) instance = new AchievementDefDAO();
        return instance;
    }

    // ── Read ──────────────────────────────────────────────────────────────

    public List<AchievementDefinition> getAll() {
        List<AchievementDefinition> list = new ArrayList<>();
        for (Document d : col.find()) {
            list.add(toModel(d));
        }
        return list;
    }

    public AchievementDefinition getByBadge(String badge) {
        Document d = col.find(Filters.eq("badge", badge)).first();
        return d == null ? null : toModel(d);
    }

    // ── Write ─────────────────────────────────────────────────────────────

    public void save(AchievementDefinition def) {
        Document doc = fromModel(def);
        col.insertOne(doc);
        def.setId(doc.getObjectId("_id"));
    }

    public void update(AchievementDefinition def) {
        col.replaceOne(
                Filters.eq("badge", def.getBadge()),
                fromModel(def),
                new ReplaceOptions().upsert(true)
        );
    }

    public void delete(ObjectId id) {
        col.deleteOne(Filters.eq("_id", id));
    }

    // ── Seed defaults ────────────────────────────────────────────────────

    private void seedDefaults() {
        if (col.countDocuments() > 0) return;

        save(new AchievementDefinition("FIRST_GAME",     "First Game!",       "Completed your first typing game",     "🎮", "GAMES",    1,   10));
        save(new AchievementDefinition("WPM_50",         "Speed Demon",       "Achieved 50 WPM in a single game",     "⚡", "WPM",      50,  50));
        save(new AchievementDefinition("WPM_80",         "Typing Master",     "Achieved 80 WPM in a single game",     "🔥", "WPM",      80,  100));
        save(new AchievementDefinition("WPM_100",        "Keyboard Ninja",    "Achieved 100 WPM in a single game",    "🥷", "WPM",      100, 200));
        save(new AchievementDefinition("ACCURACY_95",    "Precision Pro",     "Achieved 95% accuracy in a game",      "🎯", "ACCURACY", 95,  75));
        save(new AchievementDefinition("ACCURACY_100",   "Perfect Typist",    "Achieved 100% accuracy in a game",     "💎", "ACCURACY", 100, 150));
        save(new AchievementDefinition("STREAK_3",       "On a Roll",         "Maintained a 3-day typing streak",     "🔥", "STREAK",   3,   30));
        save(new AchievementDefinition("STREAK_5",       "Dedicated Typist",  "Maintained a 5-day typing streak",     "⭐", "STREAK",   5,   50));
        save(new AchievementDefinition("STREAK_7",       "Week Warrior",      "Maintained a 7-day typing streak",     "🏆", "STREAK",   7,   100));
        save(new AchievementDefinition("LEVEL_5",        "Rising Star",       "Reached Level 5",                      "🌟", "LEVEL",    5,   50));
        save(new AchievementDefinition("LEVEL_10",       "Expert Typist",     "Reached Level 10",                     "👑", "LEVEL",    10,  100));
        save(new AchievementDefinition("LESSON_1",       "First Lesson",      "Completed your first training lesson", "📖", "GAMES",    1,   20));
        save(new AchievementDefinition("LESSON_5",       "Diligent Student",  "Completed 5 training lessons",         "📚", "GAMES",    5,   50));
        save(new AchievementDefinition("LESSON_10",      "Course Champion",   "Completed all 10 lessons",             "🎓", "GAMES",    10,  100));
        save(new AchievementDefinition("DAILY_COMPLETE", "Daily Champion",    "Completed all daily challenges today", "📅", "GAMES",    1,   20));
    }

    // ── Mappers ───────────────────────────────────────────────────────────

    private AchievementDefinition toModel(Document d) {
        AchievementDefinition def = new AchievementDefinition();
        def.setId(d.getObjectId("_id"));
        def.setBadge(d.getString("badge"));
        def.setTitle(d.getString("title"));
        def.setDescription(d.getString("description"));
        def.setIcon(d.getString("icon"));
        def.setConditionType(d.getString("conditionType"));
        Object cv = d.get("conditionValue");
        def.setConditionValue(cv instanceof Number n ? n.doubleValue() : 0);
        def.setXpReward(d.getInteger("xpReward", 0));
        return def;
    }

    private Document fromModel(AchievementDefinition def) {
        return new Document()
                .append("badge",          def.getBadge())
                .append("title",          def.getTitle())
                .append("description",    def.getDescription())
                .append("icon",           def.getIcon())
                .append("conditionType",  def.getConditionType())
                .append("conditionValue", def.getConditionValue())
                .append("xpReward",       def.getXpReward());
    }
}
