package model;

import org.bson.types.ObjectId;
import java.time.LocalDateTime;

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
}