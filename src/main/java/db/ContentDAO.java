package db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import model.Content;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;

public class ContentDAO {
    private static ContentDAO instance;
    private final MongoCollection<Document> collection;

    private ContentDAO() {
        MongoDatabase database = MongoDBManager.getInstance().getDatabase();
        this.collection = database.getCollection("content");
    }

    public static ContentDAO getInstance() {
        if (instance == null) {
            instance = new ContentDAO();
        }
        return instance;
    }

    public void create(Content content) {
        Document doc = new Document()
                .append("type", content.getType())
                .append("text", content.getText())
                .append("difficulty", content.getDifficulty())
                .append("category", content.getCategory())
                .append("active", content.isActive());
        collection.insertOne(doc);
        content.setId(doc.getObjectId("_id"));
    }

    public void save(Content content) {
        create(content);
    }

    public Content findById(ObjectId id) {
        Document doc = collection.find(eq("_id", id)).first();
        return doc != null ? documentToContent(doc) : null;
    }

    public List<Content> getAll() {
        List<Content> contents = new ArrayList<>();
        for (Document doc : collection.find()) {
            contents.add(documentToContent(doc));
        }
        return contents;
    }

    public List<Content> getWordsByDifficulty(String difficulty) {
        List<Content> contents = new ArrayList<>();
        for (Document doc : collection.find(and(eq("type", "WORD"), eq("difficulty", difficulty), eq("active", true)))) {
            contents.add(documentToContent(doc));
        }
        return contents;
    }

    public List<Content> getSentences() {
        List<Content> contents = new ArrayList<>();
        for (Document doc : collection.find(and(eq("type", "SENTENCE"), eq("active", true)))) {
            contents.add(documentToContent(doc));
        }
        return contents;
    }

    public long countByTypeAndDifficulty(String type, String difficulty) {
        return collection.countDocuments(and(eq("type", type), eq("difficulty", difficulty)));
    }

    public List<Content> search(String searchText, String type, String difficulty) {
        List<Content> contents = new ArrayList<>();
        
        var filters = new ArrayList<org.bson.conversions.Bson>();
        if (searchText != null && !searchText.isEmpty()) {
            filters.add(regex("text", searchText, "i"));
        }
        if (type != null && !type.isEmpty()) {
            filters.add(eq("type", type));
        }
        if (difficulty != null && !difficulty.isEmpty()) {
            filters.add(eq("difficulty", difficulty));
        }
        
        var query = filters.isEmpty() ? new Document() : and(filters);
        for (Document doc : collection.find(query)) {
            contents.add(documentToContent(doc));
        }
        return contents;
    }

    public void update(Content content) {
        Document doc = new Document()
                .append("type", content.getType())
                .append("text", content.getText())
                .append("difficulty", content.getDifficulty())
                .append("category", content.getCategory())
                .append("active", content.isActive());
        collection.updateOne(eq("_id", content.getId()), new Document("$set", doc));
    }

    public void delete(ObjectId id) {
        collection.deleteOne(eq("_id", id));
    }

    private Content documentToContent(Document doc) {
        Content content = new Content();
        content.setId(doc.getObjectId("_id"));
        content.setType(doc.getString("type"));
        content.setText(doc.getString("text"));
        content.setDifficulty(doc.getString("difficulty"));
        content.setCategory(doc.getString("category"));
        content.setActive(doc.getBoolean("active", true));
        return content;
    }
}
