package db;

import com.mongodb.client.*;
import com.mongodb.client.model.*;
import model.GameResult;
import model.User;
import org.bson.Document;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.util.*;

/**
 * Singleton MongoDB manager.
 * Handles all database interactions for the TypeMaster game.
 */
public class MongoDBManager {

    private static MongoDBManager instance;

    private final MongoClient     client;
    private final MongoDatabase   db;
    private final MongoCollection<Document> users;
    private final MongoCollection<Document> results;

    // ── Singleton ─────────────────────────────────────────────────────────

    private MongoDBManager() {
        Properties props = loadConfig();
        String uri      = props.getProperty("mongodb.uri",      "mongodb://localhost:27017");
        String dbName   = props.getProperty("mongodb.database", "typinggame2");

        client  = MongoClients.create(uri);
        db      = client.getDatabase(dbName);
        users   = db.getCollection("users");
        results = db.getCollection("game_results");

        // Ensure unique index on email
        users.createIndex(Indexes.ascending("email"),
                new IndexOptions().unique(true));
        users.createIndex(Indexes.ascending("username"),
                new IndexOptions().unique(true));
    }

    public static MongoDBManager getInstance() {
        if (instance == null) {
            instance = new MongoDBManager();
        }
        return instance;
    }

    public void close() {
        client.close();
        instance = null;
    }

    /** Exposes the underlying database so additional DAOs can share this connection. */
    public MongoDatabase getDatabase() {
        return db;
    }

    // ── Config ────────────────────────────────────────────────────────────

    private Properties loadConfig() {
        Properties p = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/config.properties")) {
            if (is != null) p.load(is);
        } catch (IOException ignored) { }
        return p;
    }

    // ── USER operations ───────────────────────────────────────────────────

    /** Inserts a new user. Returns false if username/email is already taken. */
    public boolean registerUser(User user) {
        try {
            Document doc = new Document()
                    .append("phone",        user.getPhone())
                    .append("address",      user.getAddress())
                    .append("age",          user.getAge())
                    .append("dob",          user.getDob())
                    .append("username",     user.getUsername())
                    .append("email",        user.getEmail())
                    .append("passwordHash", user.getPasswordHash())
                    .append("totalGames",   user.getTotalGames())
                    .append("bestWpm",      user.getBestWpm())
                    .append("averageWpm",   user.getAverageWpm())
                    .append("bestAccuracy", user.getBestAccuracy())
                    .append("createdAt",    new Date());
            users.insertOne(doc);
            user.setId(doc.getObjectId("_id"));
            return true;
        } catch (Exception e) {
            return false;   // duplicate key
        }
    }

    /**
     * Looks up a user by email and verifies the password hash.
     * Returns the User object on success, null on failure.
     */
    public User loginUser(String email, String passwordHash) {
        Document doc = users.find(Filters.and(
                Filters.eq("email",        email),
                Filters.eq("passwordHash", passwordHash)
        )).first();
        return doc == null ? null : documentToUser(doc);
    }

    /** Returns a User by username, or null if not found. */
    public User getUserByUsername(String username) {
        Document doc = users.find(Filters.eq("username", username)).first();
        return doc == null ? null : documentToUser(doc);
    }

    public boolean isEmailTaken(String email) {
        return users.find(Filters.eq("email", email)).first() != null;
    }

    public boolean isUsernameTaken(String username) {
        return users.find(Filters.eq("username", username)).first() != null;
    }

    /** Updates aggregated stats for the given user after a game. */
    public void updateUserStats(String username, double newWpm, double newAccuracy) {
        User existing = getUserByUsername(username);
        if (existing == null) return;

        int    games   = existing.getTotalGames() + 1;
        double best    = Math.max(existing.getBestWpm(), newWpm);
        double avgWpm  = ((existing.getAverageWpm() * existing.getTotalGames()) + newWpm) / games;
        double bestAcc = Math.max(existing.getBestAccuracy(), newAccuracy);

        users.updateOne(
                Filters.eq("username", username),
                Updates.combine(
                        Updates.set("totalGames",   games),
                        Updates.set("bestWpm",      best),
                        Updates.set("averageWpm",   avgWpm),
                        Updates.set("bestAccuracy", bestAcc)
                )
        );
    }

    // ── GAME RESULT operations ────────────────────────────────────────────

    /** Persists a completed game result. */
    public void saveResult(GameResult result) {
        Document doc = new Document()
                .append("username",   result.getUsername())
                .append("gameMode",   result.getGameMode())
                .append("difficulty", result.getDifficulty())
                .append("duration",   result.getDuration())
                .append("wpm",        result.getWpm())
                .append("accuracy",   result.getAccuracy())
                .append("errorCount", result.getErrorCount())
                .append("wordsTyped", result.getWordsTyped())
                .append("playedAt",   new Date());
        results.insertOne(doc);
        result.setId(doc.getObjectId("_id"));
    }

    /** Returns the last {@code limit} results for a given user, newest first. */
    public List<GameResult> getResultsForUser(String username, int limit) {
        List<GameResult> list = new ArrayList<>();
        FindIterable<Document> it = results
                .find(Filters.eq("username", username))
                .sort(Sorts.descending("playedAt"))
                .limit(limit);
        for (Document doc : it) {
            list.add(documentToResult(doc));
        }
        return list;
    }

    /** Returns the top {@code limit} scores from all users, ordered by WPM desc. */
    public List<GameResult> getLeaderboard(int limit) {
        List<GameResult> list = new ArrayList<>();
        FindIterable<Document> it = results
                .find()
                .sort(Sorts.descending("wpm"))
                .limit(limit);
        for (Document doc : it) {
            list.add(documentToResult(doc));
        }
        return list;
    }

    // ── Mappers ───────────────────────────────────────────────────────────

    private User documentToUser(Document doc) {
        User u = new User();
        u.setId(doc.getObjectId("_id"));
        u.setPhone(doc.getString("phone"));
        u.setAddress(doc.getString("address"));
        u.setAge(doc.getInteger("age", 0));
        u.setDob(doc.getString("dob"));
        u.setUsername(doc.getString("username"));
        u.setEmail(doc.getString("email"));
        u.setPasswordHash(doc.getString("passwordHash"));
        u.setTotalGames(doc.getInteger("totalGames", 0));
        u.setBestWpm(doc.getDouble("bestWpm") != null ? doc.getDouble("bestWpm") : 0.0);
        u.setAverageWpm(doc.getDouble("averageWpm") != null ? doc.getDouble("averageWpm") : 0.0);
        u.setBestAccuracy(doc.getDouble("bestAccuracy") != null ? doc.getDouble("bestAccuracy") : 0.0);
        Date created = doc.getDate("createdAt");
        if (created != null) {
            u.setCreatedAt(created.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        return u;
    }

    private GameResult documentToResult(Document doc) {
        GameResult r = new GameResult();
        r.setId(doc.getObjectId("_id"));
        r.setUsername(doc.getString("username"));
        r.setGameMode(doc.getString("gameMode"));
        r.setDifficulty(doc.getString("difficulty"));
        r.setDuration(doc.getInteger("duration", 0));
        r.setWpm(doc.getDouble("wpm") != null ? doc.getDouble("wpm") : 0.0);
        r.setAccuracy(doc.getDouble("accuracy") != null ? doc.getDouble("accuracy") : 0.0);
        r.setErrorCount(doc.getInteger("errorCount", 0));
        r.setWordsTyped(doc.getInteger("wordsTyped", 0));
        Date played = doc.getDate("playedAt");
        if (played != null) {
            r.setPlayedAt(played.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        return r;
    }
}
