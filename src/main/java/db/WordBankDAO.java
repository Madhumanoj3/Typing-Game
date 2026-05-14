package db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class WordBankDAO {

    private static WordBankDAO instance;
    private final MongoCollection<Document> col;

    private static final int SEED_VERSION = 2;

    private WordBankDAO() {
        col = MongoDBManager.getInstance().getDatabase().getCollection("word_bank");
        col.createIndex(Indexes.ascending("type", "difficulty", "active"));
        seedIfOutdated();
    }

    public static WordBankDAO getInstance() {
        if (instance == null) instance = new WordBankDAO();
        return instance;
    }

    public List<String> getWords(String difficulty) {
        return getTexts("WORD", difficulty);
    }

    /** Returns all active sentences regardless of difficulty. */
    public List<String> getSentences() {
        return getTexts("SENTENCE", null);
    }

    /** Returns active sentences matching the given difficulty level. */
    public List<String> getSentences(String difficulty) {
        List<String> byDiff = getTexts("SENTENCE", difficulty);
        // Fall back to all sentences if none exist for this difficulty
        return byDiff.isEmpty() ? getTexts("SENTENCE", null) : byDiff;
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

    private void seedIfOutdated() {
        Document meta = col.find(Filters.eq("type", "META")).first();
        int currentVersion = meta != null ? meta.getInteger("version", 0) : 0;
        if (currentVersion >= SEED_VERSION) return;

        // ── Easy words (100) ─────────────────────────────────────────────
        String[] easy = {
            "cat", "dog", "sun", "moon", "star", "tree", "fish", "bird", "book", "pen",
            "run", "jump", "walk", "swim", "fly", "eat", "see", "say", "get", "use",
            "find", "give", "tell", "work", "call", "feel", "ask", "put", "sit", "try",
            "red", "blue", "big", "old", "new", "hot", "cold", "fast", "slow", "hard",
            "soft", "tall", "long", "good", "best", "read", "play", "sing", "dance", "draw",
            "cup", "hat", "bag", "car", "bus", "map", "key", "bed", "fan", "box",
            "milk", "rain", "road", "rock", "leaf", "sand", "wind", "fire", "snow", "door",
            "hand", "face", "foot", "hair", "eyes", "nose", "name", "home", "love", "life",
            "farm", "lake", "hill", "cave", "wave", "bear", "duck", "frog", "lion", "wolf",
            "game", "song", "film", "idea", "word", "desk", "wall", "bell", "ring", "flag"
        };

        // ── Medium words (100) ───────────────────────────────────────────
        String[] medium = {
            "computer", "keyboard", "monitor", "software", "hardware", "internet", "database", "network", "program", "system",
            "button", "window", "folder", "screen", "laptop", "tablet", "server", "router", "module", "method",
            "object", "string", "player", "format", "record", "search", "update", "stream", "design", "manage",
            "create", "upload", "deploy", "browse", "secure", "access", "memory", "storage", "backup", "packet",
            "signal", "output", "filter", "decode", "encode", "export", "import", "widget", "layout", "render",
            "nature", "planet", "energy", "garden", "forest", "animal", "family", "friend", "school", "office",
            "market", "bridge", "castle", "island", "museum", "temple", "statue", "valley", "desert", "jungle",
            "health", "mental", "doctor", "patient", "clinic", "vision", "remedy", "repair", "travel", "flight",
            "ticket", "voyage", "engine", "driver", "police", "safety", "rescue", "trophy", "reward", "puzzle",
            "riddle", "editor", "writer", "artist", "singer", "dancer", "mentor", "leader", "winner", "career"
        };

        // ── Hard words (65) ──────────────────────────────────────────────
        String[] hard = {
            "algorithm", "architecture", "implementation", "optimization", "synchronization",
            "polymorphism", "encapsulation", "inheritance", "abstraction", "interface",
            "configuration", "authentication", "authorization", "infrastructure", "documentation",
            "visualization", "serialization", "multithreading", "concurrency", "parallelism",
            "containerization", "microservices", "asynchronous", "cryptography", "refactoring",
            "interpolation", "normalization", "fragmentation", "virtualization", "orchestration",
            "scalability", "availability", "reliability", "maintainability", "accessibility",
            "interoperability", "transactional", "declarative", "imperative", "recursion",
            "decomposition", "classification", "specification", "verification", "instantiation",
            "initialization", "compilation", "interpretation", "decompression", "modularization",
            "parameterization", "generalization", "specialization", "aggregation", "instrumentation",
            "profiling", "benchmarking", "observability", "idempotency", "immutability",
            "deserialization", "denormalization", "disambiguation", "defragmentation", "reconfiguration"
        };

        // ── Easy sentences ────────────────────────────────────────────────
        String[] easySentences = {
            "The quick brown fox jumps over the lazy dog.",
            "Practice makes perfect in typing skills.",
            "The sun rises in the east and sets in the west.",
            "A good book is a great friend for life.",
            "Birds sing songs in the early morning light.",
            "She sells seashells by the seashore every day.",
            "The cat sat on the warm window sill.",
            "Hard work and focus lead to great results.",
            "Fresh air and sunlight keep the mind sharp.",
            "Every day is a new chance to learn something."
        };

        // ── Medium sentences ──────────────────────────────────────────────
        String[] mediumSentences = {
            "Java is a powerful and versatile programming language.",
            "MongoDB is a flexible NoSQL database system.",
            "Learning to type faster improves your daily productivity.",
            "Software development requires patience and creative problem solving.",
            "Regular practice helps build muscle memory for the keyboard.",
            "Debugging code is like solving a complex detective mystery.",
            "Open source projects have changed the world of technology.",
            "Cloud computing allows applications to scale on demand easily.",
            "Version control systems help teams collaborate on large projects.",
            "A well-designed user interface improves the overall user experience.",
            "Consistent naming conventions make code easier to understand.",
            "Unit tests catch bugs early and reduce development costs.",
            "Responsive design ensures websites work well on all screen sizes.",
            "APIs allow different software systems to communicate with each other.",
            "Data structures are the foundation of efficient programming algorithms."
        };

        // ── Hard sentences ────────────────────────────────────────────────
        String[] hardSentences = {
            "The implementation of asynchronous programming requires a deep understanding of concurrency.",
            "Polymorphism enables objects of different types to be treated through a common interface.",
            "Encapsulation is one of the fundamental principles of object-oriented programming design.",
            "Microservices architecture decomposes applications into small, independently deployable services.",
            "Cryptographic algorithms ensure the confidentiality and integrity of sensitive data transmissions.",
            "Normalization reduces data redundancy and improves database query performance significantly.",
            "Containerization with Docker allows developers to package and ship applications consistently.",
            "Multithreading enables programs to perform multiple computations simultaneously on modern processors.",
            "The observer pattern decouples the subject from its observers using event notification.",
            "Dependency injection is a technique for achieving inversion of control in software design.",
            "Continuous integration and deployment pipelines automate the testing and release process.",
            "Functional programming treats computation as the evaluation of mathematical functions without side effects.",
            "The garbage collector automatically manages memory allocation and deallocation in managed runtimes.",
            "Binary search trees provide efficient insertion, deletion, and lookup operations in logarithmic time.",
            "Reactive programming is a paradigm oriented around data streams and the propagation of change."
        };

        for (String w : easy)           upsert("WORD",     w, "EASY",   "seed");
        for (String w : medium)         upsert("WORD",     w, "MEDIUM", "seed");
        for (String w : hard)           upsert("WORD",     w, "HARD",   "seed");
        for (String s : easySentences)  upsert("SENTENCE", s, "EASY",   "seed");
        for (String s : mediumSentences)upsert("SENTENCE", s, "MEDIUM", "seed");
        for (String s : hardSentences)  upsert("SENTENCE", s, "HARD",   "seed");

        // Write/update the META version marker
        col.replaceOne(
            Filters.eq("type", "META"),
            new Document("type", "META").append("version", SEED_VERSION),
            new ReplaceOptions().upsert(true));
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
