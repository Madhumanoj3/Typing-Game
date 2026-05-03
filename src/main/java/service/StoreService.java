package service;

import db.UserInventoryDAO;
import db.UserStatsDAO;
import game.ThemeManager;
import game.ThemeManager.ThemeDef;
import game.ThemeManager.FontDef;
import model.UserInventory;
import model.UserStats;

import java.util.List;

/**
 * Handles theme and font purchasing, unlocking, and equipping.
 */
public class StoreService {

    private static StoreService instance;
    private final UserInventoryDAO inventoryDAO;
    private final UserStatsDAO     statsDAO;

    private StoreService() {
        inventoryDAO = UserInventoryDAO.getInstance();
        statsDAO     = UserStatsDAO.getInstance();
    }

    public static StoreService getInstance() {
        if (instance == null) instance = new StoreService();
        return instance;
    }

    // ── Inventory loading ─────────────────────────────────────────────────

    public UserInventory getInventory(String username) {
        UserInventory inv = inventoryDAO.getOrCreate(username);
        syncLevelUnlocks(username, inv);   // auto-unlock level-gated items
        return inv;
    }

    /** Grants all level-unlocked themes/fonts the user has earned but not yet received. */
    private void syncLevelUnlocks(String username, UserInventory inv) {
        UserStats stats = statsDAO.getOrCreate(username);
        int level = stats.getLevel();
        boolean changed = false;

        for (ThemeDef t : ThemeManager.ALL_THEMES) {
            if ("LEVEL".equals(t.unlockType()) && level >= t.levelRequired() && !inv.hasTheme(t.id())) {
                inv.unlockTheme(t.id());
                changed = true;
            }
        }
        for (FontDef f : ThemeManager.ALL_FONTS) {
            if ("LEVEL".equals(f.unlockType()) && level >= f.levelRequired() && !inv.hasFont(f.id())) {
                inv.unlockFont(f.id());
                changed = true;
            }
        }
        if (changed) inventoryDAO.save(inv);
    }

    // ── Theme operations ──────────────────────────────────────────────────

    public enum PurchaseResult { SUCCESS, ALREADY_OWNED, INSUFFICIENT_COINS, LEVEL_REQUIRED, NOT_FOUND }

    public PurchaseResult buyTheme(String username, String themeId) {
        ThemeDef def = ThemeManager.ALL_THEMES.stream()
                .filter(t -> t.id().equals(themeId)).findFirst().orElse(null);
        if (def == null) return PurchaseResult.NOT_FOUND;

        UserInventory inv = inventoryDAO.getOrCreate(username);
        if (inv.hasTheme(themeId)) return PurchaseResult.ALREADY_OWNED;

        if ("LEVEL".equals(def.unlockType())) {
            UserStats stats = statsDAO.getOrCreate(username);
            if (stats.getLevel() < def.levelRequired()) return PurchaseResult.LEVEL_REQUIRED;
            inv.unlockTheme(themeId);
            inventoryDAO.save(inv);
            return PurchaseResult.SUCCESS;
        }

        // COINS purchase
        UserStats stats = statsDAO.getOrCreate(username);
        if (stats.getCoins() < def.coinCost()) return PurchaseResult.INSUFFICIENT_COINS;
        stats.setCoins(stats.getCoins() - def.coinCost());
        statsDAO.save(stats);
        inv.unlockTheme(themeId);
        inventoryDAO.save(inv);
        return PurchaseResult.SUCCESS;
    }

    public PurchaseResult buyFont(String username, String fontId) {
        FontDef def = ThemeManager.ALL_FONTS.stream()
                .filter(f -> f.id().equals(fontId)).findFirst().orElse(null);
        if (def == null) return PurchaseResult.NOT_FOUND;

        UserInventory inv = inventoryDAO.getOrCreate(username);
        if (inv.hasFont(fontId)) return PurchaseResult.ALREADY_OWNED;

        if ("LEVEL".equals(def.unlockType())) {
            UserStats stats = statsDAO.getOrCreate(username);
            if (stats.getLevel() < def.levelRequired()) return PurchaseResult.LEVEL_REQUIRED;
            inv.unlockFont(fontId);
            inventoryDAO.save(inv);
            return PurchaseResult.SUCCESS;
        }

        UserStats stats = statsDAO.getOrCreate(username);
        if (stats.getCoins() < def.coinCost()) return PurchaseResult.INSUFFICIENT_COINS;
        stats.setCoins(stats.getCoins() - def.coinCost());
        statsDAO.save(stats);
        inv.unlockFont(fontId);
        inventoryDAO.save(inv);
        return PurchaseResult.SUCCESS;
    }

    /** Equip a theme the user already owns. Updates DB and ThemeManager. */
    public boolean equipTheme(String username, String themeId) {
        UserInventory inv = inventoryDAO.getOrCreate(username);
        if (!inv.hasTheme(themeId)) return false;
        inv.setActiveTheme(themeId);
        inventoryDAO.save(inv);
        ThemeManager.getInstance().setTheme(themeId);
        return true;
    }

    /** Equip a font the user already owns. Updates DB and ThemeManager. */
    public boolean equipFont(String username, String fontId) {
        UserInventory inv = inventoryDAO.getOrCreate(username);
        if (!inv.hasFont(fontId)) return false;
        inv.setActiveFont(fontId);
        inventoryDAO.save(inv);
        ThemeManager.getInstance().setFont(fontId);
        return true;
    }

    /** Load saved theme+font from DB into ThemeManager at login. */
    public void loadUserPreferences(String username) {
        try {
            UserInventory inv = getInventory(username);
            ThemeManager.getInstance().setTheme(inv.getActiveTheme());
            ThemeManager.getInstance().setFont(inv.getActiveFont());
        } catch (Exception ignored) {}
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    public List<ThemeDef> getAllThemes()  { return ThemeManager.ALL_THEMES; }
    public List<FontDef>  getAllFonts()   { return ThemeManager.ALL_FONTS; }
}
