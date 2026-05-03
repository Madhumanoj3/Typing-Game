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

        int len = Math.min(typed.length(), targetText.length());

        // Count positionally correct characters
        int matched = 0;
        for (int i = 0; i < len; i++) {
            if (typed.charAt(i) == targetText.charAt(i)) matched++;
        }

        // Errors = positional mismatches + extra chars beyond target length
        int mismatches = len - matched;
        int extras     = Math.max(0, typed.length() - targetText.length());
        errorCount     = mismatches + extras;

        // Accuracy = correctly placed chars / total typed chars × 100
        int totalTyped  = typed.length();
        currentAccuracy = totalTyped > 0
                ? Math.max(0, (double) matched / totalTyped * 100)
                : 100.0;

        // WPM = (correct chars / 5) / elapsed minutes  — character-based, smooth
        double elapsedMin = elapsedMinutes();
        currentWpm = (elapsedMin > 0 && matched > 0)
                ? (matched / 5.0) / elapsedMin
                : 0;

        // Words = correctly completed whole words (for Words stat and result screen)
        String[] targetWords = targetText.split("\\s+");
        String[] typedWords  = typed.split("\\s+", -1);
        int correctWords = 0;
        for (int i = 0; i < Math.min(typedWords.length - 1, targetWords.length); i++) {
            if (typedWords[i].equals(targetWords[i])) correctWords++;
        }
        if (typedWords.length == targetWords.length &&
                typedWords[typedWords.length - 1].equals(targetWords[targetWords.length - 1])) {
            correctWords++;
        }
        wordsTyped = correctWords;
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
