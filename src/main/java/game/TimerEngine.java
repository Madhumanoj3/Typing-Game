package game;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 * JavaFX Timeline-based countdown timer.
 *
 * Usage:
 *   TimerEngine timer = new TimerEngine(60, this::onTick, this::onFinished);
 *   timer.start();
 *   // ...
 *   timer.stop();
 */
public class TimerEngine {

    public interface TickCallback    { void onTick(int secondsRemaining); }
    public interface FinishCallback  { void onFinished(); }

    private final int           totalSeconds;
    private final TickCallback  tickCallback;
    private final FinishCallback finishCallback;

    private Timeline timeline;
    private int      remaining;
    private boolean  running;

    /**
     * @param totalSeconds  total countdown duration
     * @param onTick        called every second with seconds remaining
     * @param onFinished    called when the timer reaches zero
     */
    public TimerEngine(int totalSeconds, TickCallback onTick, FinishCallback onFinished) {
        this.totalSeconds   = totalSeconds;
        this.tickCallback   = onTick;
        this.finishCallback = onFinished;
        this.remaining      = totalSeconds;
        this.running        = false;
    }

    /** Starts (or resumes) the countdown. */
    public void start() {
        if (running) return;

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remaining--;
            if (tickCallback != null) tickCallback.onTick(remaining);
            if (remaining <= 0) {
                stop();
                if (finishCallback != null) finishCallback.onFinished();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        running = true;
    }

    /** Pauses the countdown. */
    public void pause() {
        if (timeline != null) timeline.pause();
        running = false;
    }

    /** Stops and resets the timer. */
    public void stop() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
        running = false;
    }

    /** Resets remaining to totalSeconds and stops. */
    public void reset() {
        stop();
        remaining = totalSeconds;
    }

    // ── Queries ───────────────────────────────────────────────────────────

    public int     getRemaining()     { return remaining;     }
    public int     getTotalSeconds()  { return totalSeconds;  }
    public boolean isRunning()        { return running;       }

    /** Returns progress 0.0 (full time) → 1.0 (time expired). */
    public double getProgress() {
        if (totalSeconds == 0) return 1.0;
        return 1.0 - ((double) remaining / totalSeconds);
    }

    /** Formats remaining seconds as MM:SS string. */
    public String getFormattedTime() {
        int m = remaining / 60;
        int s = remaining % 60;
        return String.format("%02d:%02d", m, s);
    }
}
