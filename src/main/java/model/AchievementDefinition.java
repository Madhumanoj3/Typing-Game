package model;

import org.bson.types.ObjectId;

/**
 * Admin-editable achievement definition stored in the {@code achievement_defs} collection.
 * Separate from the user-earned {@link Achievement} model.
 */
public class AchievementDefinition {

    private ObjectId id;
    private String   badge;           // unique code e.g. "WPM_50"
    private String   title;
    private String   description;
    private String   icon;            // emoji icon
    private String   conditionType;   // "WPM" | "ACCURACY" | "STREAK" | "LEVEL" | "GAMES"
    private double   conditionValue;  // threshold value
    private int      xpReward;

    public AchievementDefinition() {}

    public AchievementDefinition(String badge, String title, String description, String icon,
                                  String conditionType, double conditionValue, int xpReward) {
        this.badge          = badge;
        this.title          = title;
        this.description    = description;
        this.icon           = icon;
        this.conditionType  = conditionType;
        this.conditionValue = conditionValue;
        this.xpReward       = xpReward;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────

    public ObjectId getId()                      { return id; }
    public void setId(ObjectId id)               { this.id = id; }

    public String getBadge()                     { return badge; }
    public void setBadge(String v)               { this.badge = v; }

    public String getTitle()                     { return title; }
    public void setTitle(String v)               { this.title = v; }

    public String getDescription()               { return description; }
    public void setDescription(String v)         { this.description = v; }

    public String getIcon()                      { return icon; }
    public void setIcon(String v)                { this.icon = v; }

    public String getConditionType()             { return conditionType; }
    public void setConditionType(String v)       { this.conditionType = v; }

    public double getConditionValue()            { return conditionValue; }
    public void setConditionValue(double v)      { this.conditionValue = v; }

    public int getXpReward()                     { return xpReward; }
    public void setXpReward(int v)               { this.xpReward = v; }

    @Override
    public String toString() {
        return String.format("AchievementDef{badge='%s', title='%s', condition=%s>=%.0f, xp=%d}",
                badge, title, conditionType, conditionValue, xpReward);
    }
}
