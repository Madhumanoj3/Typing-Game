package db.plsql;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Application-level trigger dispatcher — mirrors a database trigger engine.
 *
 * PL/SQL analogy:
 *   CREATE OR REPLACE TRIGGER trg_name
 *     BEFORE/AFTER event ON table FOR EACH ROW
 *   BEGIN ... END;
 *
 * Triggers are registered once at startup ({@link db.plsql.AppTriggers#registerAll()})
 * and fired automatically from the data-access layer, just as a DBMS fires
 * row-level triggers during DML.
 */
public class TriggerEngine {

    /**
     * Supported trigger events, each mapping to a specific DML moment.
     *
     * PL/SQL equivalents:
     *   BEFORE_USER_INSERT  → BEFORE INSERT ON users              FOR EACH ROW
     *   AFTER_GAME_INSERT   → AFTER  INSERT ON game_results        FOR EACH ROW
     *   AFTER_USER_BLOCKED  → AFTER  UPDATE OF blocked ON users    FOR EACH ROW
     *                          WHEN (NEW.blocked = 1)
     */
    public enum Event {
        BEFORE_USER_INSERT,
        AFTER_GAME_INSERT,
        AFTER_USER_BLOCKED
    }

    /**
     * A trigger body — equivalent to the BEGIN...END block in PL/SQL.
     * BEFORE-trigger bodies may throw {@link TriggerException} to simulate
     * {@code RAISE_APPLICATION_ERROR}, which aborts the calling operation.
     */
    @FunctionalInterface
    public interface TriggerBody<T> {
        void execute(T payload);
    }

    /**
     * Thrown by a BEFORE trigger to abort the DML operation.
     * Equivalent to {@code RAISE_APPLICATION_ERROR(-20xxx, '...')} in PL/SQL.
     */
    public static class TriggerException extends RuntimeException {
        public TriggerException(String message) { super(message); }
    }

    // ── Singleton ─────────────────────────────────────────────────────────

    private static TriggerEngine instance;
    private final Map<Event, List<TriggerBody<Object>>> registry = new EnumMap<>(Event.class);

    private TriggerEngine() {
        for (Event e : Event.values()) registry.put(e, new ArrayList<>());
    }

    public static TriggerEngine getInstance() {
        if (instance == null) instance = new TriggerEngine();
        return instance;
    }

    // ── Registration ──────────────────────────────────────────────────────

    /** Register a trigger body for the given event (supports multiple per event). */
    @SuppressWarnings("unchecked")
    public <T> void register(Event event, TriggerBody<T> body) {
        registry.get(event).add((TriggerBody<Object>) body);
    }

    // ── Firing ────────────────────────────────────────────────────────────

    /**
     * Fires all trigger bodies registered for {@code event}, passing {@code payload}
     * as the :NEW pseudo-record.  Any {@link TriggerException} propagates to the caller.
     */
    public <T> void fire(Event event, T payload) {
        for (TriggerBody<Object> body : registry.get(event)) {
            body.execute(payload);
        }
    }
}
