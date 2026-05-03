package model;

import java.util.ArrayList;
import java.util.List;

public class UserInventory {

    private String       username;
    private List<String> unlockedThemes;
    private List<String> unlockedFonts;
    private String       activeTheme;
    private String       activeFont;

    public UserInventory() {
        this.unlockedThemes = new ArrayList<>();
        this.unlockedFonts  = new ArrayList<>();
        this.activeTheme    = "dark_violet";
        this.activeFont     = "segoe";
    }

    public UserInventory(String username) {
        this();
        this.username = username;
        // Default unlocks: first theme and first font always available
        this.unlockedThemes.add("dark_violet");
        this.unlockedFonts.add("segoe");
    }

    public String       getUsername()                   { return username; }
    public void         setUsername(String v)           { this.username = v; }

    public List<String> getUnlockedThemes()             { return unlockedThemes; }
    public void         setUnlockedThemes(List<String> v) { this.unlockedThemes = v; }

    public List<String> getUnlockedFonts()              { return unlockedFonts; }
    public void         setUnlockedFonts(List<String> v)  { this.unlockedFonts = v; }

    public String       getActiveTheme()                { return activeTheme; }
    public void         setActiveTheme(String v)        { this.activeTheme = v; }

    public String       getActiveFont()                 { return activeFont; }
    public void         setActiveFont(String v)         { this.activeFont = v; }

    public boolean hasTheme(String id)  { return unlockedThemes.contains(id); }
    public boolean hasFont(String id)   { return unlockedFonts.contains(id); }

    public void unlockTheme(String id)  { if (!hasTheme(id)) unlockedThemes.add(id); }
    public void unlockFont(String id)   { if (!hasFont(id))  unlockedFonts.add(id); }
}
