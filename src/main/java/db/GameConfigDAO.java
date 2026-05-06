package db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import model.GameConfig;
import org.bson.Document;

import java.util.*;

/**
 * Data access for the {@code game_config} collection.
 * Seeds default game mode configurations on first connection.
 */
public class GameConfigDAO {

    private static GameConfigDAO instance;
    private final MongoCollection<Document> col;

    private GameConfigDAO() {
        col = MongoDBManager.getInstance().getDatabase().getCollection("game_config");
        seedDefaults();
    }

    public static GameConfigDAO getInstance() {
        if (instance == null) instance = new GameConfigDAO();
        return instance;
    }

    // ── Read ──────────────────────────────────────────────────────────────

    public List<GameConfig> getAll() {
        List<GameConfig> list = new ArrayList<>();
        for (Document d : col.find()) {
            list.add(toModel(d));
        }
        return list;
    }

    public GameConfig getByMode(String modeName) {
        Document d = col.find(Filters.eq("modeName", modeName)).first();
        return d == null ? null : toModel(d);
    }

    // ── Write ─────────────────────────────────────────────────────────────

    public void save(GameConfig config) {
        Document doc = fromModel(config);
        col.insertOne(doc);
        config.setId(doc.getObjectId("_id"));
    }

    public void update(GameConfig config) {
        col.replaceOne(
                Filters.eq("modeName", config.getModeName()),
                fromModel(config),
                new ReplaceOptions().upsert(true)
        );
    }

    // ── Seed defaults ────────────────────────────────────────────────────

    private void seedDefaults() {
        if (col.countDocuments() > 0) return;

        // Practice mode — no timer
        Map<String, Integer> practiceDurations = new LinkedHashMap<>();
        practiceDurations.put("Easy", 0);
        practiceDurations.put("Medium", 0);
        practiceDurations.put("Hard", 0);
        save(new GameConfig("Practice",
                "Free-form typing practice with no time pressure. Type at your own pace.",
                true, practiceDurations));

        // Normal mode — fixed word count
        Map<String, Integer> normalDurations = new LinkedHashMap<>();
        normalDurations.put("Easy", 0);
        normalDurations.put("Medium", 0);
        normalDurations.put("Hard", 0);
        save(new GameConfig("Normal",
                "Type a set passage as quickly and accurately as possible.",
                true, normalDurations));

        // Timer mode — timed challenges
        Map<String, Integer> timerDurations = new LinkedHashMap<>();
        timerDurations.put("Easy", 60);
        timerDurations.put("Medium", 120);
        timerDurations.put("Hard", 180);
        save(new GameConfig("Timer",
                "Race against the clock! Type as many words as you can before time runs out.",
                true, timerDurations));
    }

    // ── Mappers ───────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private GameConfig toModel(Document d) {
        GameConfig c = new GameConfig();
        c.setId(d.getObjectId("_id"));
        c.setModeName(d.getString("modeName"));
        c.setEnabled(d.getBoolean("enabled", true));
        c.setDescription(d.getString("description"));

        Map<String, Integer> durations = new LinkedHashMap<>();
        Object raw = d.get("durations");
        if (raw instanceof Document doc) {
            for (Map.Entry<String, Object> entry : doc.entrySet()) {
                durations.put(entry.getKey(),
                        entry.getValue() instanceof Number n ? n.intValue() : 0);
            }
        }
        c.setDurations(durations);
        return c;
    }

    private Document fromModel(GameConfig c) {
        Document durDoc = new Document();
        if (c.getDurations() != null) {
            c.getDurations().forEach(durDoc::append);
        }
        return new Document()
                .append("modeName",    c.getModeName())
                .append("enabled",     c.isEnabled())
                .append("description", c.getDescription())
                .append("durations",   durDoc);
    }
}
