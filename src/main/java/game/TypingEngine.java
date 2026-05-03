package game;


/**
 * Calculates real-time typing statistics:
 *   - Words Per Minute (WPM)
 *   - Raw accuracy (%)
 *   - Error count
 *
 * Usage:
 *   1. Call {@link #start(String)} once with the target passage.
 *   2. Call {@link #update(String)} every time the user's input changes.
 *   3. Query {@link #getWpm()}, {@link #getAccuracy()}, {@link #getErrors()}.
 */
public class TypingEngine {

    // ── State ─────────────────────────────────────────────────────────────
    private String   targetText  = "";
    private long     startTimeMs = 0;
    private boolean  started     = false;

    private double  currentWpm      = 0.0;
    private double  currentAccuracy = 100.0;
    private int     errorCount      = 0;
    private int     wordsTyped      = 0;

    // ── Lifecycle ─────────────────────────────────────────────────────────

    /**
     * Prepares the engine with the target passage.
     * Call this before the user starts typing.
     */
    public void start(String targetText) {
        this.targetText      = targetText;
        this.startTimeMs     = 0;
        this.started         = false;
        this.currentWpm      = 0;
        this.currentAccuracy = 100;
        this.errorCount      = 0;
        this.wordsTyped      = 0;
    }

    /**
     * Updates stats based on the user's current typed input.
     * Should be called on every keystroke.
     *
     * @param typed  the full text the user has typed so far
     */
    public void update(String typed) {
        if (typed == null || typed.isEmpty()) return;

        // Start timer on first character
        if (!started) {
            startTimeMs = System.currentTimeMillis();
            started     = true;
        }

        // Count errors by comparing chars position by position
        int len       = Math.min(typed.length(), targetText.length());
        int errors    = 0;
        for (int i = 0; i < len; i++) {
            if (typed.charAt(i) != targetText.charAt(i)) {
                errors++;
            }
        }
        // Extra characters beyond target also count as errors
        if (typed.length() > targetText.length()) {
            errors += typed.length() - targetText.length();
        }
        errorCount = errors;

        // Words typed = correctly completed words (split by space)
        String[] targetWords = targetText.split("\\s+");
        String[] typedWords  = typed.split("\\s+", -1);

        int correct = 0;
        for (int i = 0; i < Math.min(typedWords.length - 1, targetWords.length); i++) {
            if (typedWords[i].equals(targetWords[i])) correct++;
        }
        // If user has typed the last word and it matches, count it too
        if (typedWords.length == targetWords.length &&
            typedWords[typedWords.length - 1].equals(targetWords[targetWords.length - 1])) {
            correct++;
        }
        wordsTyped = correct;

        // WPM = (correct words / elapsed minutes)
        double elapsedMin = elapsedMinutes();
        currentWpm = elapsedMin > 0 ? correct / elapsedMin : 0;

        // Accuracy = correct chars / total chars typed * 100
        int totalTyped   = typed.length();
        int correctChars = totalTyped - errors;
        currentAccuracy  = totalTyped > 0
                ? Math.max(0, (double) correctChars / totalTyped * 100)
                : 100.0;
    }

    /** Call when the game ends to get final WPM using elapsed time. */
    public void finish() {
        // Nothing extra needed; stats already computed in update()
    }

    // ── Queries ───────────────────────────────────────────────────────────

    public double getWpm()          { return roundOne(currentWpm);      }
    public double getAccuracy()     { return roundOne(currentAccuracy); }
    public int    getErrors()       { return errorCount;                }
    public int    getWordsTyped()   { return wordsTyped;                }

    /** How many characters the user has correctly typed so far (for progress bar). */
    public int getCorrectLength(String typed) {
        if (typed == null) return 0;
        int count = 0;
        int len   = Math.min(typed.length(), targetText.length());
        for (int i = 0; i < len; i++) {
            if (typed.charAt(i) == targetText.charAt(i)) count++;
            else break;   // stop at first error (prefix-correct model)
        }
        return count;
    }

    /** Progress 0.0–1.0 based on typed characters vs total. */
    public double getProgress(String typed) {
        if (targetText.isEmpty()) return 0;
        return Math.min(1.0, (double) typed.length() / targetText.length());
    }

    /** Returns true when the user has typed the full passage length. */
    public boolean isComplete(String typed) {
        return typed != null && typed.length() >= targetText.length();
    }

    public boolean isStarted() { return started; }

    public double elapsedSeconds() {
        if (!started) return 0;
        return (System.currentTimeMillis() - startTimeMs) / 1000.0;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private double elapsedMinutes() {
        return elapsedSeconds() / 60.0;
    }

    private double roundOne(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
