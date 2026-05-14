package db.plsql;

/*
 * ════════════════════════════════════════════════════════════════════════════
 *  PL/SQL EQUIVALENTS  (Oracle syntax — reference only)
 *
 *  ── FUNCTION 1 ────────────────────────────────────────────────────────────
 *  CREATE OR REPLACE FUNCTION fn_get_user_rank (
 *    p_username IN VARCHAR2
 *  ) RETURN INTEGER AS
 *    v_rank INTEGER;
 *  BEGIN
 *    -- 1-based rank: count users with a strictly higher best_wpm, then add 1
 *    SELECT COUNT(*) + 1 INTO v_rank
 *    FROM   users
 *    WHERE  best_wpm > (SELECT best_wpm FROM users WHERE username = p_username);
 *    RETURN v_rank;
 *  EXCEPTION
 *    WHEN NO_DATA_FOUND THEN RETURN -1;
 *  END fn_get_user_rank;
 *  /
 *
 *  ── FUNCTION 2 ────────────────────────────────────────────────────────────
 *  CREATE OR REPLACE FUNCTION fn_calculate_performance_score (
 *    p_wpm        IN NUMBER,
 *    p_accuracy   IN NUMBER,
 *    p_error_cnt  IN INTEGER
 *  ) RETURN NUMBER AS
 *    v_wpm_score     NUMBER;
 *    v_acc_score     NUMBER;
 *    v_error_penalty NUMBER;
 *    v_raw           NUMBER;
 *  BEGIN
 *    v_wpm_score     := LEAST(p_wpm / 1.5, 100) * 0.50;   -- max  50 pts
 *    v_acc_score     := p_accuracy             * 0.35;     -- max  35 pts
 *    v_error_penalty := LEAST(p_error_cnt * 2, 15);        -- max -15 pts
 *    v_raw           := v_wpm_score + v_acc_score - v_error_penalty;
 *    RETURN ROUND(LEAST(GREATEST(v_raw, 0), 100), 2);
 *  END fn_calculate_performance_score;
 *  /
 *
 *  ── FUNCTION 3 ────────────────────────────────────────────────────────────
 *  CREATE OR REPLACE FUNCTION fn_check_subscription_status (
 *    p_username IN VARCHAR2
 *  ) RETURN VARCHAR2 AS
 *    v_plan   VARCHAR2(20);
 *    v_status VARCHAR2(20);
 *  BEGIN
 *    -- Latest subscription by start_date
 *    SELECT plan, status INTO v_plan, v_status
 *    FROM   subscriptions
 *    WHERE  username   = p_username
 *      AND  start_date = (SELECT MAX(start_date) FROM subscriptions
 *                         WHERE username = p_username);
 *    IF    v_status = 'ACTIVE'    AND v_plan != 'FREE' THEN RETURN 'ACTIVE_PREMIUM';
 *    ELSIF v_status = 'ACTIVE'                         THEN RETURN 'ACTIVE_FREE';
 *    ELSIF v_status = 'SUSPENDED'                      THEN RETURN 'SUSPENDED';
 *    ELSE                                                   RETURN 'EXPIRED';
 *    END IF;
 *  EXCEPTION
 *    WHEN NO_DATA_FOUND THEN
 *      -- User exists but has no subscription record
 *      BEGIN
 *        SELECT COUNT(*) INTO v_plan FROM users WHERE username = p_username;
 *        IF v_plan > 0 THEN RETURN 'ACTIVE_FREE'; END IF;
 *      EXCEPTION WHEN OTHERS THEN NULL; END;
 *      RETURN 'NOT_FOUND';
 *  END fn_check_subscription_status;
 *  /
 * ════════════════════════════════════════════════════════════════════════════
 */

import com.mongodb.client.model.Filters;
import db.MongoDBManager;
import db.SubscriptionDAO;
import model.Subscription;
import model.User;

/**
 * Application-level stored functions for the TypeMaster game.
 * <p>
 * Each static method computes and returns a single value, exactly as a
 * PL/SQL {@code FUNCTION ... RETURN} declaration does.  No side-effects
 * are produced — all methods are pure queries or deterministic calculations.
 * </p>
 */
public class AppFunctions {

    private AppFunctions() {}

    // ─────────────────────────────────────────────────────────────────────────
    // FUNCTION 1 — fn_get_user_rank
    //   RETURN INTEGER
    //
    //   Returns the 1-based global rank of the user ordered by bestWpm DESC.
    //   Rank 1 = highest WPM in the entire system.
    //   Returns -1 when the username is not found (equivalent to NO_DATA_FOUND).
    //
    //   SQL logic: SELECT COUNT(*) + 1 FROM users WHERE best_wpm > :target_wpm
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Computes the user's 1-based leaderboard rank by {@code bestWpm}.
     *
     * @param username the player whose rank is requested
     * @return rank (1 = best), or -1 if the user does not exist
     */
    public static int fnGetUserRank(String username) {
        // SELECT best_wpm FROM users WHERE username = p_username — WHEN NO_DATA_FOUND → -1
        User target = MongoDBManager.getInstance().getUserByUsername(username);
        if (target == null) return -1;

        // SELECT COUNT(*) + 1 FROM users WHERE best_wpm > :target_best_wpm
        long higherCount = MongoDBManager.getInstance().getDatabase()
                .getCollection("users")
                .countDocuments(Filters.gt("bestWpm", target.getBestWpm()));

        return (int) higherCount + 1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FUNCTION 2 — fn_calculate_performance_score
    //   RETURN NUMBER (0 – 100)
    //
    //   Deterministic formula — no DB access required.
    //   Weights:
    //     50 pts — WPM component   : min(wpm / 1.5, 100) × 0.50
    //     35 pts — Accuracy        : accuracy × 0.35
    //    -15 pts — Error penalty   : min(errorCount × 2, 15)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Computes a composite performance score (0–100) from one game session.
     *
     * @param wpm        words per minute achieved
     * @param accuracy   accuracy percentage (0–100)
     * @param errorCount number of keystroke errors
     * @return composite score in the range [0, 100], rounded to 2 decimal places
     */
    public static double fnCalculatePerformanceScore(double wpm, double accuracy, int errorCount) {
        double wpmScore     = Math.min(wpm / 1.5, 100.0) * 0.50;   // max 50 pts
        double accScore     = accuracy * 0.35;                       // max 35 pts
        double errorPenalty = Math.min(errorCount * 2.0, 15.0);      // max 15 pts off
        double raw          = wpmScore + accScore - errorPenalty;
        return Math.round(Math.max(0.0, Math.min(100.0, raw)) * 100.0) / 100.0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FUNCTION 3 — fn_check_subscription_status
    //   RETURN VARCHAR2
    //
    //   Queries the latest subscription record and returns a status constant:
    //     "ACTIVE_PREMIUM" — active paid plan (MONTHLY or YEARLY)
    //     "ACTIVE_FREE"    — user exists but no active premium
    //     "SUSPENDED"      — subscription was suspended (e.g. account blocked)
    //     "EXPIRED"        — last subscription expired or was cancelled
    //     "NOT_FOUND"      — username does not exist in the system
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the subscription status for the named user as a descriptive constant.
     *
     * @param username the player to check
     * @return one of {@code "ACTIVE_PREMIUM"}, {@code "ACTIVE_FREE"},
     *         {@code "SUSPENDED"}, {@code "EXPIRED"}, {@code "NOT_FOUND"}
     */
    public static String fnCheckSubscriptionStatus(String username) {
        // WHEN NO_DATA_FOUND (user row missing) → 'NOT_FOUND'
        User user = MongoDBManager.getInstance().getUserByUsername(username);
        if (user == null) return "NOT_FOUND";

        // SELECT plan, status FROM subscriptions WHERE username = p_username ORDER BY start_date DESC
        Subscription sub = SubscriptionDAO.getInstance().getLatest(username);
        if (sub == null) return "ACTIVE_FREE";  // user exists, no subscription row

        return switch (sub.getStatus()) {
            case "ACTIVE"               -> !"FREE".equalsIgnoreCase(sub.getPlan())
                                           ? "ACTIVE_PREMIUM" : "ACTIVE_FREE";
            case "SUSPENDED"            -> "SUSPENDED";
            case "EXPIRED", "CANCELLED" -> "EXPIRED";
            default                     -> "ACTIVE_FREE";
        };
    }
}
