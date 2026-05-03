package model;

import org.bson.types.ObjectId;
import java.time.LocalDate;

public class UserStats {

    private ObjectId  id;
    private String    username;
    private int       xp;
    private int       level;
    private int       streak;
    private LocalDate lastActiveDate;

    // Daily challenge progress (reset each new day)
    private int       dailyGamesPlayed;
    private double    dailyMaxWpm;
    private int       dailyCharsTyped;
    private LocalDate challengeDate;   // which day the counters belong to

    private int       coins;           // future extension

    public UserStats() {}

    public UserStats(String username) {
        this.username         = username;
        this.xp               = 0;
        this.level            = 1;
        this.streak           = 0;
        this.dailyGamesPlayed = 0;
        this.dailyMaxWpm      = 0.0;
        this.dailyCharsTyped  = 0;
        this.coins            = 0;
    }

    // ── Getters / Setters ──────────────────────────────────────────────────

    public ObjectId getId()                    { return id; }
    public void setId(ObjectId id)             { this.id = id; }

    public String getUsername()                { return username; }
    public void setUsername(String v)          { this.username = v; }

    public int getXp()                         { return xp; }
    public void setXp(int v)                   { this.xp = v; }

    public int getLevel()                      { return level; }
    public void setLevel(int v)                { this.level = v; }

    public int getStreak()                     { return streak; }
    public void setStreak(int v)               { this.streak = v; }

    public LocalDate getLastActiveDate()       { return lastActiveDate; }
    public void setLastActiveDate(LocalDate v) { this.lastActiveDate = v; }

    public int getDailyGamesPlayed()           { return dailyGamesPlayed; }
    public void setDailyGamesPlayed(int v)     { this.dailyGamesPlayed = v; }

    public double getDailyMaxWpm()             { return dailyMaxWpm; }
    public void setDailyMaxWpm(double v)       { this.dailyMaxWpm = v; }

    public int getDailyCharsTyped()            { return dailyCharsTyped; }
    public void setDailyCharsTyped(int v)      { this.dailyCharsTyped = v; }

    public LocalDate getChallengeDate()        { return challengeDate; }
    public void setChallengeDate(LocalDate v)  { this.challengeDate = v; }

    public int getCoins()                      { return coins; }
    public void setCoins(int v)                { this.coins = v; }
}
