package game;

import db.WordBankDAO;

import java.util.List;
import java.util.Random;

public class WordBank {
    private static final String[] EASY_WORDS = {
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

    private static final String[] MEDIUM_WORDS = {
        "computer", "keyboard", "monitor", "software", "hardware", "internet", "database", "network", "program", "system",
        "button", "window", "folder", "screen", "laptop", "tablet", "server", "router", "module", "method",
        "object", "string", "player", "format", "record", "search", "update", "stream", "design", "manage",
        "create", "upload", "deploy", "browse", "secure", "access", "memory", "storage", "backup", "packet",
        "signal", "output", "filter", "decode", "encode", "export", "import", "widget", "layout", "render",
        "nature", "planet", "energy", "garden", "forest", "animal", "family", "friend", "school", "office",
        "market", "bridge", "castle", "island", "museum", "temple", "statue", "valley", "desert", "jungle",
        "health", "mental", "doctor", "patient", "clinic", "vision", "remedy", "repair", "manage", "travel",
        "flight", "ticket", "voyage", "engine", "driver", "police", "safety", "rescue", "trophy", "reward",
        "puzzle", "riddle", "editor", "writer", "artist", "singer", "dancer", "mentor", "leader", "winner"
    };

    private static final String[] HARD_WORDS = {
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
        "parameterization", "generalization", "specialization", "encapsulation", "aggregation",
        "disassembly", "instrumentation", "profiling", "benchmarking", "observability",
        "idempotency", "immutability", "referential", "covariance", "contravariance",
        "deserialization", "denormalization", "disambiguation", "defragmentation", "reconfiguration"
    };

    private static final String[] SENTENCES = {
        // Easy
        "The quick brown fox jumps over the lazy dog.",
        "Practice makes perfect in typing skills.",
        "The sun rises in the east and sets in the west.",
        "A good book is a great friend for life.",
        "Clean code is easy to read and maintain.",
        "Birds sing songs in the early morning light.",
        "She sells seashells by the seashore every day.",
        "The cat sat on the warm window sill.",
        "Hard work and focus lead to great results.",
        "Fresh air and sunlight keep the mind sharp.",
        // Medium
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
        "Data structures are the foundation of efficient programming algorithms.",
        // Hard
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

    public static String[] getWords(String difficulty) {
        try {
            List<String> dbWords = WordBankDAO.getInstance().getWords(difficulty);
            if (!dbWords.isEmpty()) return dbWords.toArray(String[]::new);
        } catch (Exception ex) {
            System.err.println("Word bank DB unavailable, using fallback words: " + ex.getMessage());
        }

        switch (difficulty.toUpperCase()) {
            case "EASY": return EASY_WORDS;
            case "MEDIUM": return MEDIUM_WORDS;
            case "HARD": return HARD_WORDS;
            default: return MEDIUM_WORDS;
        }
    }

    public static String getRandomSentence() {
        Random random = new Random();
        try {
            List<String> dbSentences = WordBankDAO.getInstance().getSentences();
            if (!dbSentences.isEmpty()) return dbSentences.get(random.nextInt(dbSentences.size()));
        } catch (Exception ex) {
            System.err.println("Sentence bank DB unavailable, using fallback sentence: " + ex.getMessage());
        }
        return SENTENCES[random.nextInt(SENTENCES.length)];
    }

    public static String buildPassage(String difficulty, int wordCount) {
        String[] words = getWords(difficulty);
        Random random = new Random();
        StringBuilder passage = new StringBuilder();
        for (int i = 0; i < wordCount; i++) {
            if (i > 0) passage.append(" ");
            passage.append(words[random.nextInt(words.length)]);
        }
        return passage.toString();
    }

    /**
     * Builds a passage that randomly mixes individual words and full sentences.
     * Approximately every 3rd chunk is a sentence; the rest are individual words.
     * The passage stops once the target word count is reached or exceeded.
     */
    public static String buildMixedPassage(String difficulty, int targetWords) {
        String[] words = getWords(difficulty);
        List<String> sentences = getSentenceList(difficulty);
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        int wordsAdded = 0;

        while (wordsAdded < targetWords) {
            if (sb.length() > 0) sb.append(" ");

            // ~1-in-3 chance to insert a full sentence when sentences are available
            if (!sentences.isEmpty() && random.nextInt(3) == 0) {
                String sentence = sentences.get(random.nextInt(sentences.size()));
                sb.append(sentence);
                wordsAdded += sentence.split("\\s+").length;
            } else {
                sb.append(words[random.nextInt(words.length)]);
                wordsAdded++;
            }
        }
        return sb.toString().trim();
    }

    private static List<String> getSentenceList(String difficulty) {
        try {
            List<String> dbSentences = WordBankDAO.getInstance().getSentences(difficulty);
            if (!dbSentences.isEmpty()) return dbSentences;
        } catch (Exception ignored) {}
        return java.util.Arrays.asList(SENTENCES);
    }
}
