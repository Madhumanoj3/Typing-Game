package model;

import org.bson.types.ObjectId;
import java.time.LocalDateTime;

public class Achievement {

    private ObjectId      id;
    private String        username;
    private String        badge;        // unique code e.g. "WPM_50"
    private String        title;
    private String        description;
    private LocalDateTime earnedAt;

    public Achievement() {}

    public Achievement(String username, String badge, String title, String description) {
        this.username    = username;
        this.badge       = badge;
        this.title       = title;
        this.description = description;
        this.earnedAt    = LocalDateTime.now();
    }

    // ── Getters / Setters ──────────────────────────────────────────────────

    public ObjectId getId()                   { return id; }
    public void setId(ObjectId id)            { this.id = id; }

    public String getUsername()               { return username; }
    public void setUsername(String v)         { this.username = v; }

    public String getBadge()                  { return badge; }
    public void setBadge(String v)            { this.badge = v; }

    public String getTitle()                  { return title; }
    public void setTitle(String v)            { this.title = v; }

    public String getDescription()            { return description; }
    public void setDescription(String v)      { this.description = v; }

    public LocalDateTime getEarnedAt()        { return earnedAt; }
    public void setEarnedAt(LocalDateTime v)  { this.earnedAt = v; }
}
