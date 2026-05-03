package game;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Plays short sound effects during the game using the standard JDK audio API.
 * Sound files must be placed in src/main/resources/sounds/:
 *   correct.wav  — played when a whole word is typed correctly
 *   wrong.wav    — played when a typing error is introduced
 *   finish.wav   — played when the game ends
 *
 * Sounds fail silently when files are missing.
 */
public class SoundManager {

    private static SoundManager instance;

    private final byte[] correctData;
    private final byte[] wrongData;
    private final byte[] finishData;

    private boolean enabled = true;

    private SoundManager() {
        correctData = loadBytes("/sounds/correct.wav");
        wrongData   = loadBytes("/sounds/wrong.wav");
        finishData  = loadBytes("/sounds/finish.wav");
    }

    public static SoundManager getInstance() {
        if (instance == null) instance = new SoundManager();
        return instance;
    }

    // ── Public API ────────────────────────────────────────────────────────

    public void playCorrect() { play(correctData); }
    public void playWrong()   { play(wrongData); }
    public void playFinish()  { play(finishData); }

    public boolean isEnabled()           { return enabled; }
    public void    setEnabled(boolean v) { this.enabled = v; }

    // ── Internal ──────────────────────────────────────────────────────────

    private void play(byte[] data) {
        if (!enabled || data == null) return;
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(
                    new ByteArrayInputStream(data));
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            clip.addLineListener(e -> {
                if (e.getType() == LineEvent.Type.STOP) clip.close();
            });
            clip.start();
        } catch (Exception ignored) {}
    }

    private byte[] loadBytes(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("[SoundManager] Missing sound file: " + resourcePath +
                    " — place it in src/main/resources/sounds/");
                return null;
            }
            return new BufferedInputStream(is).readAllBytes();
        } catch (Exception e) {
            System.err.println("[SoundManager] Failed to load " + resourcePath + ": " + e.getMessage());
            return null;
        }
    }
}
