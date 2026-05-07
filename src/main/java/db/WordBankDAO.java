package db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import model.Content;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class WordBankDAO {

    private static WordBankDAO instance;
    private final MongoCollection<Document> col;

    private WordBankDAO() {
        col = MongoDBManager.getInstance().getDatabase().getCollection("word_bank");
        col.createIndex(Indexes.ascending("type", "difficulty", "active"));
        seedIfEmpty();
    }

    public static WordBankDAO getInstance() {
        if (instance == null) instance = new WordBankDAO();
        return instance;
    }

    public List<String> getWords(String difficulty) {
        return getTexts("WORD", difficulty);
    }

    public List<String> getSentences() {
        return getTexts("SENTENCE", null);
    }

    private List<String> getTexts(String type, String difficulty) {
        List<String> texts = new ArrayList<>();
        List<org.bson.conversions.Bson> filters = new ArrayList<>();
        filters.add(Filters.eq("type", type));
        filters.add(Filters.eq("active", true));
        if (difficulty != null && !difficulty.isBlank()) {
            filters.add(Filters.eq("difficulty", difficulty.toUpperCase()));
        }
        for (Document doc : col.find(Filters.and(filters))) {
            String text = doc.getString("text");
            if (text != null && !text.isBlank()) texts.add(text);
        }
        return texts;
    }

    private void seedIfEmpty() {
        if (col.countDocuments() > 0) return;
        String[] easy = {"cat", "dog", "sun", "moon", "star", "tree", "fish", "bird", "book", "pen"};
        String[] medium = {"computer", "keyboard", "monitor", "software", "hardware", "internet", "database", "network", "program", "system"};
        String[] hard = {"algorithm", "architecture", "implementation", "optimization", "synchronization", "polymorphism", "encapsulation", "inheritance", "abstraction", "interface"};
        for (String word : easy) upsert("WORD", word, "EASY", "seed");
        for (String word : medium) upsert("WORD", word, "MEDIUM", "seed");
        for (String word : hard) upsert("WORD", word, "HARD", "seed");
        upsert("SENTENCE", "The quick brown fox jumps over the lazy dog.", "EASY", "seed");
        upsert("SENTENCE", "Practice makes perfect in typing skills.", "EASY", "seed");
        upsert("SENTENCE", "Java is a powerful programming language.", "MEDIUM", "seed");
        upsert("SENTENCE", "MongoDB is a NoSQL database system.", "MEDIUM", "seed");
        upsert("SENTENCE", "Learning to type faster improves productivity.", "HARD", "seed");
    }

    private void upsert(String type, String text, String difficulty, String category) {
        Document doc = new Document()
                .append("type", type)
                .append("text", text)
                .append("difficulty", difficulty)
                .append("category", category)
                .append("active", true);
        col.replaceOne(Filters.and(Filters.eq("type", type), Filters.eq("text", text)), doc,
                new ReplaceOptions().upsert(true));
    }
}
