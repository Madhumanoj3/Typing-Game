package model;

import org.bson.types.ObjectId;
import java.time.LocalDateTime;

public class GameResult {

    private ObjectId id;
    private String   username;
    private String   gameMode;      // "Practice", "Normal", "Timer"
    private String   difficulty;    // "Easy", "Medium", "Hard"
    private int      duration;      // seconds (0 = unlimited / Practice)
    private double   wpm;
    private double   accuracy;
    private int      errorCount;
    private int      wordsTyped;
    private LocalDateTime playedAt;

    public GameResult() {
        this.playedAt = LocalDateTime.now();
    }

    public GameResult(String username, String gameMode, String difficulty,
                      int duration, double wpm, double accuracy,
                      int errorCount, int wordsTyped) {
        this();
        this.username   = username;
        this.gameMode   = gameMode;
        this.difficulty = difficulty;
        this.duration   = duration;
        this.wpm        = wpm;
        this.accuracy   = accuracy;
        this.errorCount = errorCount;
        this.wordsTyped = wordsTyped;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────

    public ObjectId getId()                 { return id; }
    public void setId(ObjectId id)          { this.id = id; }

    public String getUsername()             { return username; }
    public void setUsername(String v)       { this.username = v; }

    public String getGameMode()             { return gameMode; }
    public void setGameMode(String v)       { this.gameMode = v; }

    public String getDifficulty()           { return difficulty; }
    public void setDifficulty(String v)     { this.difficulty = v; }

    public int getDuration()                { return duration; }
    public void setDuration(int v)          { this.duration = v; }

    public double getWpm()                  { return wpm; }
    public void setWpm(double v)            { this.wpm = v; }

    public double getAccuracy()             { return accuracy; }
    public void setAccuracy(double v)       { this.accuracy = v; }

    public int getErrorCount()              { return errorCount; }
    public void setErrorCount(int v)        { this.errorCount = v; }

    public int getWordsTyped()              { return wordsTyped; }
    public void setWordsTyped(int v)        { this.wordsTyped = v; }

    public LocalDateTime getPlayedAt()      { return playedAt; }
    public void setPlayedAt(LocalDateTime v){ this.playedAt = v; }

    @Override
    public String toString() {
        return String.format("GameResult{user='%s', mode='%s', wpm=%.1f, accuracy=%.1f%%}",
                username, gameMode, wpm, accuracy);
    }
}
