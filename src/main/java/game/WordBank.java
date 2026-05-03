package game;

import java.util.*;

/**
 * Provides random word lists and sentences for the typing game.
 * Three difficulty tiers: Easy, Medium, Hard.
 */
public class WordBank {

    private static final Random RANDOM = new Random();

    // ── Easy words (common, short) ────────────────────────────────────────
    private static final String[] EASY = {
        "the", "and", "for", "are", "but", "not", "you", "all", "can",
        "her", "was", "one", "our", "had", "how", "its", "may", "say",
        "she", "two", "who", "got", "let", "put", "too", "use", "way",
        "big", "cat", "dog", "sun", "run", "fun", "hat", "man", "day",
        "old", "new", "out", "see", "ask", "far", "few", "got", "set",
        "sit", "ten", "top", "try", "win", "yes", "ago", "air", "arm",
        "art", "bay", "bed", "box", "boy", "bus", "buy", "car", "cup"
    };

    // ── Medium words ────────────────────────────────────────────────────────
    private static final String[] MEDIUM = {
        "about", "after", "again", "begin", "below", "board", "bring",
        "build", "carry", "chair", "check", "child", "claim", "class",
        "clean", "clear", "close", "color", "could", "court", "cover",
        "cross", "dance", "daily", "drama", "dream", "drink", "drive",
        "earth", "eight", "email", "enter", "event", "every", "first",
        "floor", "focus", "force", "found", "frame", "front", "given",
        "glass", "going", "grace", "grand", "grant", "great", "green",
        "group", "grown", "guest", "guide", "happy", "heart", "heavy",
        "house", "humor", "ideal", "image", "inner", "input", "issue",
        "joint", "judge", "juice", "jumps", "keeps", "known", "large",
        "later", "laugh", "layer", "learn", "legal", "level", "light",
        "limit", "lines", "liver", "local", "logic", "lower", "lunch"
    };

    // ── Hard words (technical / long) ────────────────────────────────────
    private static final String[] HARD = {
        "abstraction", "acceleration", "accomplishment", "acknowledgment",
        "administration", "advertisement", "approximately", "architecture",
        "authentication", "authorization", "bibliographic", "broadcasting",
        "circumstances", "collaboration", "communication", "comprehension",
        "concentration", "configuration", "congratulations", "consciousness",
        "consideration", "demonstration", "determination", "differentiate",
        "documentation", "electromagnetic", "encouragement", "enlightenment",
        "entrepreneurial", "environmental", "establishment", "exaggeration",
        "extraordinary", "flamboyant", "generalization", "implementation",
        "incompatibility", "indiscriminate", "infrastructure", "initialization",
        "interconnected", "internationally", "interpretation", "investigation",
        "malfunction", "manifestation", "microprocessor", "miscellaneous",
        "multidimensional", "nevertheless", "optimization", "organization",
        "overwhelmingly", "participation", "perseverance", "pharmaceutical",
        "professionalism", "qualification", "reconciliation", "rehabilitation",
        "reinforcement", "representation", "responsibility", "sophisticated",
        "synchronization", "transmission", "unambiguously", "uncomfortable",
        "unprecedented", "vulnerability", "workmanship"
    };

    // ── Sentence bank for Practice mode ──────────────────────────────────
    private static final String[] SENTENCES = {
        "The quick brown fox jumps over the lazy dog.",
        "Programming is the art of telling another human what one wants the computer to do.",
        "Success is not final, failure is not fatal: it is the courage to continue that counts.",
        "The only way to do great work is to love what you do.",
        "In the middle of every difficulty lies opportunity.",
        "The best time to plant a tree was twenty years ago. The second best time is now.",
        "Your time is limited, so don't waste it living someone else's life.",
        "It does not matter how slowly you go as long as you do not stop.",
        "Believe you can and you're halfway there.",
        "Everything you've ever wanted is on the other side of fear."
    };

    private WordBank() { }

    // ── Public API ─────────────────────────────────────────────────────────

    /** Returns a shuffled list of {@code count} words for the given difficulty. */
    public static List<String> getWords(String difficulty, int count) {
        String[] pool = switch (difficulty.toLowerCase()) {
            case "hard"   -> HARD;
            case "medium" -> MEDIUM;
            default       -> EASY;
        };

        List<String> full = new ArrayList<>(Arrays.asList(pool));
        Collections.shuffle(full, RANDOM);

        // Repeat if count > pool size
        List<String> result = new ArrayList<>();
        while (result.size() < count) {
            result.addAll(full);
        }
        return result.subList(0, count);
    }

    /** Returns a random sentence (used in Practice mode). */
    public static String getRandomSentence() {
        return SENTENCES[RANDOM.nextInt(SENTENCES.length)];
    }

    /** Returns a multi-sentence paragraph of roughly {@code wordCount} words. */
    public static String buildPassage(String difficulty, int wordCount) {
        List<String> words = getWords(difficulty, wordCount);
        return String.join(" ", words);
    }
}
