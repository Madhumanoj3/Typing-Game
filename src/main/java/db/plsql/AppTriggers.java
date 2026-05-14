package db.plsql;

/*
 * ════════════════════════════════════════════════════════════════════════════
 *  PL/SQL EQUIVALENTS  (Oracle syntax — reference only)
 *
 *  ── TRIGGER 1 ─────────────────────────────────────────────────────────────
 *  CREATE OR REPLACE TRIGGER trg_after_game_insert
 *    AFTER INSERT ON game_results FOR EACH ROW
 *  BEGIN
 *    INSERT INTO audit_log (event, username, detail, occurred_at)
 *    VALUES ('GAME_RESULT_INSERT',
 *            :NEW.username,
 *            'wpm=' || :NEW.wpm || ' acc=' || :NEW.accuracy
 *              || ' mode=' || :NEW.game_mode || ' diff=' || :NEW.difficulty,
 *            SYSDATE);
 *  END trg_after_game_insert;
 *  /
 *
 *  ── TRIGGER 2 ─────────────────────────────────────────────────────────────
 *  CREATE OR REPLACE TRIGGER trg_before_user_register
 *    BEFORE INSERT ON users FOR EACH ROW
 *  BEGIN
 *    -- Validate e-mail format
 *    IF NOT REGEXP_LIKE(:NEW.email,
 *        '^[A-Z0-9._%+\-]+@[A-Z0-9.\-]+\.[A-Z]{2,}$', 'i') THEN
 *      RAISE_APPLICATION_ERROR(-20001,
 *          'Invalid email format: ' || :NEW.email);
 *    END IF;
 *    -- Validate username characters and length
 *    IF NOT REGEXP_LIKE(:NEW.username, '^[a-zA-Z0-9_]{3,20}$') THEN
 *      RAISE_APPLICATION_ERROR(-20002,
 *          'Username must be 3-20 chars: letters, digits or underscores only.');
 *    END IF;
 *    -- Assign :NEW column defaults (equivalent to DEFAULT constraints)
 *    :NEW.subscription_type := 'FREE';
 *    :NEW.blocked            := 0;
 *    :NEW.first_login        := 1;
 *  END trg_before_user_register;
 *  /
 *
 *  ── TRIGGER 3 ─────────────────────────────────────────────────────────────
 *  CREATE OR REPLACE TRIGGER trg_after_user_blocked
 *    AFTER UPDATE OF blocked ON users FOR EACH ROW
 *    WHEN (NEW.blocked = 1)
 *  BEGIN
 *    -- Cascade: suspend any active subscription
 *    UPDATE subscriptions
 *       SET status = 'SUSPENDED'
 *     WHERE username = :NEW.username
 *       AND status   = 'ACTIVE';
 *    -- Audit trail
 *    INSERT INTO audit_log (event, username, detail, occurred_at)
 *    VALUES ('USER_BLOCKED',
 *            :NEW.username,
 *            'Active subscriptions suspended upon block',
 *            SYSDATE);
 *  END trg_after_user_blocked;
 *  /
 * ════════════════════════════════════════════════════════════════════════════
 */

import db.MongoDBManager;
import db.SubscriptionDAO;
import model.GameResult;
import model.User;
import org.bson.Document;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Application-level trigger implementations for the TypeMaster game.
 * <p>
 * Call {@link #registerAll()} once at application startup
 * (see {@code MainUI.start()}).  Afterwards the triggers fire automatically
 * whenever the data-access layer performs the corresponding DML.
 * </p>
 */
public class AppTriggers {

    // ── Input-validation patterns (used by BEFORE trigger) ────────────────
    private static final Pattern EMAIL_RE    = Pattern.compile(
            "^[A-Z0-9._%+\\-]+@[A-Z0-9.\\-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern USERNAME_RE = Pattern.compile(
            "^[a-zA-Z0-9_]{3,20}$");

    private AppTriggers() {}

    // ── Startup registration ──────────────────────────────────────────────

    /**
     * Registers all three triggers with the {@link TriggerEngine}.
     * Must be called once before any DB operations are performed.
     */
    public static void registerAll() {
        TriggerEngine engine = TriggerEngine.getInstance();
        engine.register(TriggerEngine.Event.BEFORE_USER_INSERT, AppTriggers::trgBeforeUserRegister);
        engine.register(TriggerEngine.Event.AFTER_GAME_INSERT,  AppTriggers::trgAfterGameInsert);
        engine.register(TriggerEngine.Event.AFTER_USER_BLOCKED, AppTriggers::trgAfterUserBlocked);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TRIGGER 1 — trg_after_game_insert
    //   AFTER INSERT ON game_results FOR EACH ROW
    //   Writes an audit-log record for every new game session.
    // ─────────────────────────────────────────────────────────────────────────

    private static void trgAfterGameInsert(Object payload) {
        if (!(payload instanceof GameResult result)) return;

        // INSERT INTO audit_log ...
        writeAuditLog("GAME_RESULT_INSERT", result.getUsername(),
                String.format("wpm=%.1f acc=%.1f mode=%s diff=%s",
                        result.getWpm(), result.getAccuracy(),
                        result.getGameMode(), result.getDifficulty()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TRIGGER 2 — trg_before_user_register
    //   BEFORE INSERT ON users FOR EACH ROW
    //   Validates e-mail / username and auto-assigns default field values.
    //   Throws TriggerEngine.TriggerException to abort on validation failure
    //   (equivalent to RAISE_APPLICATION_ERROR in PL/SQL).
    // ─────────────────────────────────────────────────────────────────────────

    private static void trgBeforeUserRegister(Object payload) {
        if (!(payload instanceof User user)) return;

        // Validate e-mail — RAISE_APPLICATION_ERROR(-20001, ...)
        if (user.getEmail() == null || !EMAIL_RE.matcher(user.getEmail()).matches()) {
            throw new TriggerEngine.TriggerException(
                    "Invalid email format: " + user.getEmail());
        }

        // Validate username — RAISE_APPLICATION_ERROR(-20002, ...)
        if (user.getUsername() == null || !USERNAME_RE.matcher(user.getUsername()).matches()) {
            throw new TriggerEngine.TriggerException(
                    "Username must be 3-20 characters: letters, digits, or underscores only.");
        }

        // :NEW column assignments (mirror PL/SQL DEFAULT / :NEW := ... before the INSERT)
        user.setSubscriptionType("FREE");
        user.setBlocked(false);
        user.setFirstLogin(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TRIGGER 3 — trg_after_user_blocked
    //   AFTER UPDATE OF blocked ON users FOR EACH ROW WHEN (NEW.blocked = 1)
    //   Cascades to subscriptions and writes an audit record.
    // ─────────────────────────────────────────────────────────────────────────

    private static void trgAfterUserBlocked(Object payload) {
        if (!(payload instanceof User user)) return;
        if (!user.isBlocked()) return; // guard: WHEN (NEW.blocked = 1)

        // UPDATE subscriptions SET status = 'SUSPENDED' WHERE username = :NEW.username AND status = 'ACTIVE'
        var sub = SubscriptionDAO.getInstance().getLatest(user.getUsername());
        if (sub != null && "ACTIVE".equals(sub.getStatus())) {
            SubscriptionDAO.getInstance().updateStatus(sub.getId(), "SUSPENDED");
        }

        // INSERT INTO audit_log ...
        writeAuditLog("USER_BLOCKED", user.getUsername(),
                "Active subscriptions suspended upon block");
    }

    // ── Shared helper — INSERT INTO audit_log ─────────────────────────────

    static void writeAuditLog(String event, String username, String detail) {
        try {
            MongoDBManager.getInstance().getDatabase()
                    .getCollection("audit_log")
                    .insertOne(new Document()
                            .append("event",      event)
                            .append("username",   username)
                            .append("detail",     detail)
                            .append("occurredAt", Date.from(
                                    LocalDateTime.now()
                                            .atZone(ZoneId.systemDefault())
                                            .toInstant())));
        } catch (Exception ignored) {}
    }
}
