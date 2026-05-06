package db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import model.TypingContent;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Data access for the {@code content} collection.
 * Provides CRUD operations for admin-managed typing content.
 */
public class ContentDAO {

    private static ContentDAO instance;
    private final MongoCollection<Document> col;

    private ContentDAO() {
        col = MongoDBManager.getInstance().getDatabase().getCollection("content");
    }

    public static ContentDAO getInstance() {
        if (instance == null) instance = new ContentDAO();
        return instance;
    }

    // ── Read ──────────────────────────────────────────────────────────────

    public List<TypingContent> getAll() {
        List<TypingContent> list = new ArrayList<>();
        for (Document d : col.find().sort(Sorts.descending("createdAt"))) {
            list.add(toModel(d));
        }
        return list;
    }

    public List<TypingContent> getByDifficulty(String difficulty) {
        List<TypingContent> list = new ArrayList<>();
        for (Document d : col.find(Filters.eq("difficulty", difficulty))
                              .sort(Sorts.descending("createdAt"))) {
            list.add(toModel(d));
        }
        return list;
    }

    // ── Write ─────────────────────────────────────────────────────────────

    public void save(TypingContent content) {
        Document doc = fromModel(content);
        col.insertOne(doc);
        content.setId(doc.getObjectId("_id"));
    }

    public void update(TypingContent content) {
        col.replaceOne(Filters.eq("_id", content.getId()), fromModel(content));
    }

    public void delete(ObjectId id) {
        col.deleteOne(Filters.eq("_id", id));
    }

    // ── Mappers ───────────────────────────────────────────────────────────

    private TypingContent toModel(Document d) {
        TypingContent c = new TypingContent();
        c.setId(d.getObjectId("_id"));
        c.setType(d.getString("type"));
        c.setText(d.getString("text"));
        c.setDifficulty(d.getString("difficulty"));
        Date created = d.getDate("createdAt");
        if (created != null) {
            c.setCreatedAt(created.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        return c;
    }

    private Document fromModel(TypingContent c) {
        Document doc = new Document()
                .append("type",       c.getType())
                .append("text",       c.getText())
                .append("difficulty", c.getDifficulty())
                .append("createdAt",  new Date());
        if (c.getId() != null) doc.append("_id", c.getId());
        return doc;
    }
}
