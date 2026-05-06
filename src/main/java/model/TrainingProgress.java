package model;

import org.bson.types.ObjectId;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class TrainingProgress {

    private ObjectId      id;
    private String        username;
    private String        lessonId;
    private String        lessonTitle;
    private boolean       completed;
    private double        bestWpm;
    private double        bestAccuracy;
    private int           attempts;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime completedAt;   // null until first completion
    private Map<String, Integer> keyErrorCounts = new HashMap<>();
    private String        typingStyle = "Balanced";
    private String        recommendation = "Keep practicing with steady rhythm.";

    public TrainingProgress() {}

    public TrainingProgress(String username, String lessonId, String lessonTitle) {
        this.username    = username;
        this.lessonId    = lessonId;
        this.lessonTitle = lessonTitle;
        this.completed   = false;
        this.bestWpm     = 0;
        this.bestAccuracy = 0;
        this.attempts    = 0;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────

    public ObjectId getId()                    { return id; }
    public void setId(ObjectId id)             { this.id = id; }

    public String getUsername()                { return username; }
    public void setUsername(String v)          { this.username = v; }

    public String getLessonId()                { return lessonId; }
    public void setLessonId(String v)          { this.lessonId = v; }

    public String getLessonTitle()             { return lessonTitle; }
    public void setLessonTitle(String v)       { this.lessonTitle = v; }

    public boolean isCompleted()               { return completed; }
    public void setCompleted(boolean v)        { this.completed = v; }

    public double getBestWpm()                 { return bestWpm; }
    public void setBestWpm(double v)           { this.bestWpm = v; }

    public double getBestAccuracy()            { return bestAccuracy; }
    public void setBestAccuracy(double v)      { this.bestAccuracy = v; }

    public int getAttempts()                   { return attempts; }
    public void setAttempts(int v)             { this.attempts = v; }

    public LocalDateTime getLastAttemptAt()    { return lastAttemptAt; }
    public void setLastAttemptAt(LocalDateTime v) { this.lastAttemptAt = v; }

    public LocalDateTime getCompletedAt()      { return completedAt; }
    public void setCompletedAt(LocalDateTime v){ this.completedAt = v; }

    public Map<String, Integer> getKeyErrorCounts() { return keyErrorCounts; }
    public void setKeyErrorCounts(Map<String, Integer> v) {
        this.keyErrorCounts = v != null ? v : new HashMap<>();
    }

    public String getTypingStyle()             { return typingStyle; }
    public void setTypingStyle(String v)       { this.typingStyle = v; }

    public String getRecommendation()          { return recommendation; }
    public void setRecommendation(String v)    { this.recommendation = v; }
}
