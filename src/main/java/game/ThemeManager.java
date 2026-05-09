package game;

import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.util.List;

/**
 * Singleton that manages the active UI theme and font.
 * Call ThemeManager.bg(), ThemeManager.card(), etc. when building scene inline styles.
 * Call applyTheme(scene) after creating each scene to add the CSS stylesheet overlay.
 */
public class ThemeManager {
    public static final String DEFAULT_DARK_THEME_ID = "dark_violet";
    public static final String PRINT_LIGHT_THEME_ID = "print_light";
    private static final String ORIGINAL_STYLE_KEY = "typemaster.originalStyle";
    private static final String CHILD_LISTENER_KEY = "typemaster.themeChildListener";

    // ── Theme definition ──────────────────────────────────────────────────

    public record ThemeDef(
            String id, String name,
            String bg, String card, String header, String secondary,
            String accent, String accentDark, String accentLight,
            String unlockType, int levelRequired, int coinCost
    ) {
        /** CSS class added to the scene root for this theme. */
        public String cssClass() { return "theme-" + id; }
    }

    public record FontDef(
            String id, String name, String family,
            String unlockType, int levelRequired, int coinCost
    ) {}

    // ── Themes ────────────────────────────────────────────────────────────

    public static final List<ThemeDef> ALL_THEMES = List.of(
        // ── Level-unlocked ──────────────────────────────────────────────
        new ThemeDef(DEFAULT_DARK_THEME_ID,  "Dark Violet",   "#0f0f1a","#1a1a2e","#12122a","#1e293b","#7c3aed","#6d28d9","#a78bfa", "LEVEL",  1, 0),
        new ThemeDef("ocean_blue",   "Ocean Blue",    "#0a1628","#0f2340","#0d1f3c","#1e3a5f","#3b82f6","#2563eb","#60a5fa", "LEVEL",  2, 0),
        new ThemeDef("forest_green", "Forest Green",  "#0a1a0a","#0f2a0f","#0d200d","#1e3a1e","#10b981","#059669","#34d399", "LEVEL",  3, 0),
        new ThemeDef("crimson_red",  "Crimson Red",   "#1a0a0a","#2a0f0f","#200d0d","#3a1e1e","#ef4444","#dc2626","#f87171", "LEVEL",  4, 0),
        new ThemeDef("midnight",     "Midnight Black","#050508","#111118","#0a0a12","#18181f","#6366f1","#4f46e5","#818cf8", "LEVEL",  5, 0),
        new ThemeDef("arctic_ice",   "Arctic Ice",    "#0d1b2a","#0f2030","#0b1520","#1a3040","#67e8f9","#22d3ee","#a5f3fc", "LEVEL",  7, 0),
        new ThemeDef("sunset_orange","Sunset Orange", "#1a0e05","#2a1a08","#200e05","#3a2010","#f97316","#ea580c","#fb923c", "LEVEL", 10, 0),
        new ThemeDef("galaxy_purple","Galaxy Purple", "#120a1f","#1c1030","#100818","#201525","#c084fc","#a855f7","#d8b4fe", "LEVEL", 13, 0),
        new ThemeDef("neon_cyan",    "Neon Cyan",     "#00141a","#00202a","#001018","#003040","#22d3ee","#06b6d4","#67e8f9", "LEVEL", 16, 0),
        new ThemeDef("golden_light", "Golden Light",  "#1a1505","#2a2008","#201505","#3a3010","#eab308","#ca8a04","#facc15", "LEVEL", 20, 0),

        // ── Coin-purchase ───────────────────────────────────────────────
        new ThemeDef("rose_pink",    "Rose Pink",     "#1a0a12","#2a0f1c","#200d15","#3a1e28","#f472b6","#ec4899","#f9a8d4", "COINS",  0, 40),
        new ThemeDef("amber_gold",   "Amber Gold",    "#1a1200","#2a1e00","#201500","#3a2e00","#fbbf24","#f59e0b","#fcd34d", "COINS",  0, 50),
        new ThemeDef("emerald",      "Emerald Glow",  "#011a0e","#022a18","#012012","#033a22","#34d399","#10b981","#6ee7b7", "COINS",  0, 60),
        new ThemeDef("slate_gray",   "Slate Gray",    "#0f1218","#1a1f2e","#141820","#252d3a","#94a3b8","#64748b","#cbd5e1", "COINS",  0, 45),
        new ThemeDef("royal_blue",   "Royal Blue",    "#080e2a","#0f1540","#0a1130","#151e50","#818cf8","#6366f1","#a5b4fc", "COINS",  0, 55),
        new ThemeDef("sakura",       "Sakura",        "#1a0a0d","#2a1015","#200c10","#3a1820","#fb7185","#f43f5e","#fda4af", "COINS",  0, 65),
        new ThemeDef("lava",         "Lava",          "#180800","#280d00","#1e0900","#380e00","#ff4500","#e63900","#ff6a33", "COINS",  0, 75),
        new ThemeDef("matrix",       "Matrix Green",  "#000f00","#001500","#000c00","#002000","#00ff41","#00cc33","#66ff80", "COINS",  0, 80),
        new ThemeDef("pastel",       "Pastel Dream",  "#0f0f1a","#1a1a2e","#12122a","#1e293b","#c4b5fd","#a78bfa","#ddd6fe", "COINS",  0, 90),
        new ThemeDef("cosmic",       "Cosmic Dark",   "#030014","#080025","#050018","#0e0030","#a855f7","#9333ea","#d8b4fe", "COINS",  0, 100),

        // ── Printable/colorful light theme ──────────────────────────────
        new ThemeDef(PRINT_LIGHT_THEME_ID, "Print Light", "#fff7fb","#ffffff","#fff1b8","#e8f3ff","#ec4899","#2563eb","#f59e0b", "LEVEL", 1, 0)
    );

    // ── 20 Fonts ──────────────────────────────────────────────────────────

    public static final List<FontDef> ALL_FONTS = List.of(
        // ── Level-unlocked ──────────────────────────────────────────────
        new FontDef("segoe",       "Segoe UI",            "Segoe UI",           "LEVEL",  1, 0),
        new FontDef("consolas",    "Consolas",            "Consolas",            "LEVEL",  2, 0),
        new FontDef("courier_new", "Courier New",         "Courier New",         "LEVEL",  3, 0),
        new FontDef("arial",       "Arial",               "Arial",               "LEVEL",  4, 0),
        new FontDef("georgia",     "Georgia",             "Georgia",             "LEVEL",  5, 0),
        new FontDef("trebuchet",   "Trebuchet MS",        "Trebuchet MS",        "LEVEL",  7, 0),
        new FontDef("verdana",     "Verdana",             "Verdana",             "LEVEL", 10, 0),
        new FontDef("tahoma",      "Tahoma",              "Tahoma",              "LEVEL", 12, 0),
        new FontDef("calibri",     "Calibri",             "Calibri",             "LEVEL", 15, 0),
        new FontDef("impact",      "Impact",              "Impact",              "LEVEL", 18, 0),

        // ── Coin-purchase ───────────────────────────────────────────────
        new FontDef("comic",       "Comic Sans MS",       "Comic Sans MS",       "COINS",  0, 40),
        new FontDef("palatino",    "Palatino Linotype",   "Palatino Linotype",   "COINS",  0, 45),
        new FontDef("century",     "Century Gothic",      "Century Gothic",      "COINS",  0, 50),
        new FontDef("franklin",    "Franklin Gothic",     "Franklin Gothic Medium","COINS", 0, 55),
        new FontDef("times",       "Times New Roman",     "Times New Roman",     "COINS",  0, 60),
        new FontDef("lucida",      "Lucida Console",      "Lucida Console",      "COINS",  0, 65),
        new FontDef("cambria",     "Cambria",             "Cambria",             "COINS",  0, 70),
        new FontDef("garamond",    "Garamond",            "Garamond",            "COINS",  0, 75),
        new FontDef("rockwell",    "Rockwell",            "Rockwell",            "COINS",  0, 80),
        new FontDef("copperplate", "Copperplate Gothic",  "Copperplate Gothic Bold","COINS", 0, 90)
    );

    // ── Singleton state ───────────────────────────────────────────────────

    private static ThemeManager instance;

    private String activeThemeId = DEFAULT_DARK_THEME_ID;
    private String lastNonLightThemeId = DEFAULT_DARK_THEME_ID;
    private String activeFontId  = "segoe";

    private ThemeManager() {}

    public static ThemeManager getInstance() {
        if (instance == null) instance = new ThemeManager();
        return instance;
    }

    // ── Active theme / font ───────────────────────────────────────────────

    public void setTheme(String themeId) {
        if (ALL_THEMES.stream().anyMatch(t -> t.id().equals(themeId))) {
            this.activeThemeId = themeId;
            if (!PRINT_LIGHT_THEME_ID.equals(themeId)) {
                rememberNonLightTheme(themeId);
            }
        }
    }

    public void rememberNonLightTheme(String themeId) {
        if (!PRINT_LIGHT_THEME_ID.equals(themeId) &&
                ALL_THEMES.stream().anyMatch(t -> t.id().equals(themeId))) {
            this.lastNonLightThemeId = themeId;
        }
    }

    public void setPrintLightTheme() {
        setTheme(PRINT_LIGHT_THEME_ID);
    }

    public void restoreLastNonLightTheme() {
        setTheme(lastNonLightThemeId != null ? lastNonLightThemeId : DEFAULT_DARK_THEME_ID);
    }

    public boolean isPrintLightTheme() {
        return PRINT_LIGHT_THEME_ID.equals(activeThemeId);
    }

    public void setFont(String fontId) {
        if (ALL_FONTS.stream().anyMatch(f -> f.id().equals(fontId)))
            this.activeFontId = fontId;
    }

    public String getActiveThemeId() { return activeThemeId; }
    public String getActiveFontId()  { return activeFontId; }

    public ThemeDef activeTheme() {
        return ALL_THEMES.stream()
                .filter(t -> t.id().equals(activeThemeId))
                .findFirst().orElse(ALL_THEMES.get(0));
    }

    public FontDef activeFont() {
        return ALL_FONTS.stream()
                .filter(f -> f.id().equals(activeFontId))
                .findFirst().orElse(ALL_FONTS.get(0));
    }

    // ── Color shortcuts (for inline styles in Java) ───────────────────────

    public static String bg()          { return getInstance().activeTheme().bg(); }
    public static String card()        { return getInstance().activeTheme().card(); }
    public static String header()      { return getInstance().activeTheme().header(); }
    public static String secondary()   { return getInstance().activeTheme().secondary(); }
    public static String accent()      { return getInstance().activeTheme().accent(); }
    public static String accentDark()  { return getInstance().activeTheme().accentDark(); }
    public static String accentLight() { return getInstance().activeTheme().accentLight(); }
    public static String fontFamily()  { return getInstance().activeFont().family(); }

    /**
     * Applies the current theme to a scene:
     *  - Adds themes.css as a second stylesheet
     *  - Adds the theme CSS class to the scene root
     *  - Applies current font family to the root inline style
     */
    public static void applyTheme(Scene scene) {
        ThemeManager tm = getInstance();
        ThemeDef theme = tm.activeTheme();

        // Add theme stylesheet if not already present
        String themesCssUrl = ThemeManager.class.getResource("/themes.css") != null
                ? ThemeManager.class.getResource("/themes.css").toExternalForm() : null;
        if (themesCssUrl != null && !scene.getStylesheets().contains(themesCssUrl)) {
            scene.getStylesheets().add(themesCssUrl);
        }

        // Set theme class on root so themed CSS selectors match
        var root = scene.getRoot();
        root.getStyleClass().removeIf(c -> c.startsWith("theme-"));
        root.getStyleClass().add(theme.cssClass());

        // Inline style wins over all CSS — set bg + font here so every screen is covered
        root.setStyle(
            "-fx-background-color: " + theme.bg() + ";" +
            "-fx-font-family: '" + fontFamily() + "';"
        );
        applyInlineThemeOverridesToChildren(root, tm.isPrintLightTheme());
    }

    private static void applyInlineThemeOverridesToChildren(Node root, boolean printLight) {
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyInlineThemeOverrides(child, printLight);
            }
        }
    }

    private static void applyInlineThemeOverrides(Node node, boolean printLight) {
        if (printLight) {
            Object stored = node.getProperties().get(ORIGINAL_STYLE_KEY);
            if (stored == null) {
                node.getProperties().put(ORIGINAL_STYLE_KEY, node.getStyle() == null ? "" : node.getStyle());
            }
            String original = (String) node.getProperties().get(ORIGINAL_STYLE_KEY);
            if (original != null && !original.isBlank()) {
                node.setStyle(toPrintLightStyle(original));
            }
        } else {
            Object stored = node.getProperties().remove(ORIGINAL_STYLE_KEY);
            if (stored instanceof String original) {
                node.setStyle(original);
            }
        }

        if (node instanceof Parent parent) {
            installLightThemeChildListener(parent);
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyInlineThemeOverrides(child, printLight);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void installLightThemeChildListener(Parent parent) {
        if (parent.getProperties().containsKey(CHILD_LISTENER_KEY)) return;

        ListChangeListener<Node> listener = change -> {
            while (change.next()) {
                if (getInstance().isPrintLightTheme()) {
                    for (Node added : change.getAddedSubList()) {
                        applyInlineThemeOverrides(added, true);
                    }
                }
            }
        };
        parent.getChildrenUnmodifiable().addListener(listener);
        parent.getProperties().put(CHILD_LISTENER_KEY, listener);
    }

    private static String toPrintLightStyle(String style) {
        String s = style;

        // Background colors
        s = replaceAll(s, "#0f0f1a", "#fff7fb");
        s = replaceAll(s, "#080818", "#fff7fb");
        s = replaceAll(s, "#030014", "#fff7fb");
        s = replaceAll(s, "#050508", "#fff7fb");
        s = replaceAll(s, "#0c0c1e", "#ffffff");
        s = replaceAll(s, "#1e1040", "#f0e6ff");
        s = replaceAll(s, "#000000", "#111827");

        // Card backgrounds
        s = replaceAll(s, "#1a1a2e", "#ffffff");
        s = replaceAll(s, "#12122b", "#ffffff");
        s = replaceAll(s, "#111128", "#ffffff");
        s = replaceAll(s, "#0e0e1e", "#ffffff");
        s = replaceAll(s, "#0e0e22", "#ffffff");
        s = replaceAll(s, "#0e0e24", "#ffffff");
        s = replaceAll(s, "#111118", "#ffffff");
        s = replaceAll(s, "#080025", "#ffffff");
        s = replaceAll(s, "#050018", "#ffffff");
        s = replaceAll(s, "#0e0030", "#e8f3ff");

        // Secondary backgrounds
        s = replaceAll(s, "#12122a", "#f0f9ff");
        s = replaceAll(s, "#0d0d20", "#fef3c7");
        s = replaceAll(s, "#0b0b1e", "#fff1b8");
        s = replaceAll(s, "#0a0a1c", "#e8f3ff");
        s = replaceAll(s, "#0a0a1a", "#e8f3ff");
        s = replaceAll(s, "#16213e", "#e0f2fe");
        s = replaceAll(s, "#0f172a", "#fef3c7");
        s = replaceAll(s, "#1e293b", "#e0e7ff");
        s = replaceAll(s, "#1e2942", "#fce7f3");

        // Transparent overlays
        s = replaceAll(s, "rgba(255,255,255,0.015)", "rgba(236,72,153,0.06)");
        s = replaceAll(s, "rgba(255,255,255,0.04)", "rgba(59,130,246,0.10)");
        s = replaceAll(s, "rgba(255,255,255,0.05)", "rgba(236,72,153,0.12)");
        s = replaceAll(s, "rgba(255,255,255,0.06)", "rgba(37,99,235,0.15)");
        s = replaceAll(s, "rgba(255,255,255,0.08)", "rgba(236,72,153,0.15)");
        s = replaceAll(s, "rgba(255,255,255,0.1)", "rgba(59,130,246,0.18)");

        // Dark overlays for light theme
        s = replaceAll(s, "rgba(0,0,0,0.4)", "rgba(0,0,0,0.08)");
        s = replaceAll(s, "rgba(0,0,0,0.3)", "rgba(0,0,0,0.06)");

        // Text colors - primary
        s = replaceAll(s, "-fx-text-fill: white", "-fx-text-fill: #111827");
        s = replaceAll(s, "-fx-text-fill:white", "-fx-text-fill:#111827");
        s = replaceAll(s, "-fx-text-fill: #cbd5e1", "-fx-text-fill: #1f2937");
        s = replaceAll(s, "-fx-text-fill:#cbd5e1", "-fx-text-fill:#1f2937");
        
        // Text colors - secondary
        s = replaceAll(s, "-fx-text-fill: #94a3b8", "-fx-text-fill: #374151");
        s = replaceAll(s, "-fx-text-fill:#94a3b8", "-fx-text-fill:#374151");
        s = replaceAll(s, "-fx-text-fill: #64748b", "-fx-text-fill: #4b5563");
        s = replaceAll(s, "-fx-text-fill:#64748b", "-fx-text-fill:#4b5563");
        s = replaceAll(s, "-fx-text-fill: #475569", "-fx-text-fill: #6b7280");
        s = replaceAll(s, "-fx-text-fill:#475569", "-fx-text-fill:#6b7280");
        s = replaceAll(s, "-fx-text-fill: #334155", "-fx-text-fill: #6b7280");
        s = replaceAll(s, "-fx-text-fill:#334155", "-fx-text-fill:#6b7280");

        // Accent colors - keep vibrant but adjust for readability
        s = replaceAll(s, "-fx-text-fill: #fbbf24", "-fx-text-fill: #d97706");
        s = replaceAll(s, "-fx-text-fill: #a78bfa", "-fx-text-fill: #7c3aed");
        s = replaceAll(s, "-fx-text-fill: #38bdf8", "-fx-text-fill: #0284c7");
        s = replaceAll(s, "-fx-text-fill: #34d399", "-fx-text-fill: #059669");
        s = replaceAll(s, "-fx-text-fill: #f472b6", "-fx-text-fill: #db2777");
        s = replaceAll(s, "-fx-text-fill: #10b981", "-fx-text-fill: #047857");
        s = replaceAll(s, "-fx-text-fill: #ef4444", "-fx-text-fill: #dc2626");

        // Border colors
        s = replaceAll(s, "-fx-border-color: rgba(124,58,237,0.5)", "-fx-border-color: rgba(124,58,237,0.3)");
        s = replaceAll(s, "-fx-border-color: rgba(124,58,237,0.35)", "-fx-border-color: rgba(124,58,237,0.25)");
        s = replaceAll(s, "-fx-border-color: rgba(124,58,237,0.25)", "-fx-border-color: rgba(124,58,237,0.2)");

        return s;
    }

    private static String replaceAll(String value, String target, String replacement) {
        return value.replace(target, replacement)
                .replace(target.toUpperCase(), replacement);
    }

    // ── Level title helper (shared across screens) ────────────────────────

    public static String levelTitle(int level) {
        if (level >= 20) return "Grandmaster";
        if (level >= 15) return "Elite";
        if (level >= 10) return "Expert";
        if (level >= 5)  return "Advanced";
        if (level >= 3)  return "Intermediate";
        return "Beginner";
    }
}
