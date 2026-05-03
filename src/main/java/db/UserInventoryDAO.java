package db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import model.UserInventory;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class UserInventoryDAO {

    private static UserInventoryDAO instance;
    private final MongoCollection<Document> col;

    private UserInventoryDAO() {
        col = MongoDBManager.getInstance().getDatabase().getCollection("user_inventory");
    }

    public static UserInventoryDAO getInstance() {
        if (instance == null) instance = new UserInventoryDAO();
        return instance;
    }

    public UserInventory getOrCreate(String username) {
        Document doc = col.find(Filters.eq("username", username)).first();
        if (doc == null) {
            UserInventory inv = new UserInventory(username);
            save(inv);
            return inv;
        }
        return docToInv(doc);
    }

    public void save(UserInventory inv) {
        Document doc = new Document()
                .append("username",       inv.getUsername())
                .append("unlockedThemes", inv.getUnlockedThemes())
                .append("unlockedFonts",  inv.getUnlockedFonts())
                .append("activeTheme",    inv.getActiveTheme())
                .append("activeFont",     inv.getActiveFont());
        col.replaceOne(
                Filters.eq("username", inv.getUsername()),
                doc,
                new ReplaceOptions().upsert(true)
        );
    }

    @SuppressWarnings("unchecked")
    private UserInventory docToInv(Document doc) {
        UserInventory inv = new UserInventory();
        inv.setUsername(doc.getString("username"));

        List<String> themes = (List<String>) doc.get("unlockedThemes");
        inv.setUnlockedThemes(themes != null ? new ArrayList<>(themes) : new ArrayList<>(List.of("dark_violet")));

        List<String> fonts = (List<String>) doc.get("unlockedFonts");
        inv.setUnlockedFonts(fonts != null ? new ArrayList<>(fonts) : new ArrayList<>(List.of("segoe")));

        String at = doc.getString("activeTheme");
        inv.setActiveTheme(at != null ? at : "dark_violet");

        String af = doc.getString("activeFont");
        inv.setActiveFont(af != null ? af : "segoe");
        return inv;
    }
}
