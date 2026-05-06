package game;

import java.util.Random;

public class WordBank {
    private static final String[] EASY_WORDS = {"cat", "dog", "sun", "moon", "star", "tree", "fish", "bird", "book", "pen"};
    private static final String[] MEDIUM_WORDS = {"computer", "keyboard", "monitor", "software", "hardware", "internet", "database", "network", "program", "system"};
    private static final String[] HARD_WORDS = {"algorithm", "architecture", "implementation", "optimization", "synchronization", "polymorphism", "encapsulation", "inheritance", "abstraction", "interface"};
    
    private static final String[] SENTENCES = {
        "The quick brown fox jumps over the lazy dog.",
        "Practice makes perfect in typing skills.",
        "Java is a powerful programming language.",
        "MongoDB is a NoSQL database system.",
        "Learning to type faster improves productivity."
    };

    public static String[] getWords(String difficulty) {
        switch (difficulty.toUpperCase()) {
            case "EASY": return EASY_WORDS;
            case "MEDIUM": return MEDIUM_WORDS;
            case "HARD": return HARD_WORDS;
            default: return MEDIUM_WORDS;
        }
    }

    public static String getRandomSentence() {
        Random random = new Random();
        return SENTENCES[random.nextInt(SENTENCES.length)];
    }

    public static String buildPassage(String difficulty, int wordCount) {
        String[] words = getWords(difficulty);
        Random random = new Random();
        StringBuilder passage = new StringBuilder();
        
        for (int i = 0; i < wordCount; i++) {
            passage.append(words[random.nextInt(words.length)]);
            if (i < wordCount - 1) {
                passage.append(" ");
            }
        }
        
        return passage.toString();
    }
}
