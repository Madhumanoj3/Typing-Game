package model;

import org.bson.types.ObjectId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a game mode configuration stored in the {@code game_config} collection.
 * Each document corresponds to one game mode (Practice, Normal, Timer).
 */
public class GameConfig {

    private ObjectId             id;
    private String               modeName;        // "Practice" | "Normal" | "Timer"
    private boolean              enabled;
    private String               description;
    private Map<String, Integer> durations;        // difficulty → seconds (e.g. "Easy"→60)

    public GameConfig() {
        this.enabled   = true;
        this.durations = new LinkedHashMap<>();
    }

    public GameConfig(String modeName, String description, boolean enabled,
                      Map<String, Integer> durations) {
        this.modeName    = modeName;
        this.description = description;
        this.enabled     = enabled;
        this.durations   = durations != null ? durations : new LinkedHashMap<>();
    }

    // ── Getters / Setters ─────────────────────────────────────────────────

    public ObjectId getId()                           { return id; }
    public void setId(ObjectId id)                    { this.id = id; }

    public String getModeName()                       { return modeName; }
    public void setModeName(String v)                 { this.modeName = v; }

    public boolean isEnabled()                        { return enabled; }
    public void setEnabled(boolean v)                 { this.enabled = v; }

    public String getDescription()                    { return description; }
    public void setDescription(String v)              { this.description = v; }

    public Map<String, Integer> getDurations()        { return durations; }
    public void setDurations(Map<String, Integer> v)  { this.durations = v; }

    /** Convenience: get duration for a specific difficulty, defaulting to 60. */
    public int getDuration(String difficulty) {
        return durations.getOrDefault(difficulty, 60);
    }

    @Override
    public String toString() {
        return String.format("GameConfig{mode='%s', enabled=%b}", modeName, enabled);
    }
}
